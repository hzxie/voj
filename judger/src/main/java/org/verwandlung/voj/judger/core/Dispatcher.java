/* Verwandlung Online Judge - A cross-platform judge online system
 * Copyright (C) 2014-2026 Haozhe Xie <root@haozhexie.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.verwandlung.voj.judger.core;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Component;

import org.verwandlung.voj.judger.application.ApplicationDispatcher;
import org.verwandlung.voj.judger.exception.IllgealSubmissionException;
import org.verwandlung.voj.judger.mapper.SubmissionMapper;
import org.verwandlung.voj.judger.model.Checkpoint;
import org.verwandlung.voj.judger.model.Submission;
import org.verwandlung.voj.judger.util.DigestUtils;
import org.verwandlung.voj.judger.util.FileSystemUtils;

/**
 * The judger dispatcher. Drives the judging workflow of the judger. After each stage finishes, a
 * message is pushed to the message queue; after judging completes, the result is written to the
 * database.
 *
 * <p>A judging that cannot be completed for a reason other than the submission itself (no test
 * data, a sandbox failure, unreadable test data, ...) is judged a System Error, never a verdict
 * that blames the submission.
 *
 * @author Haozhe Xie
 */
@Component
public class Dispatcher {
  /** The judge log reason of a System Error with an unexpected cause. */
  static final String INTERNAL_ERROR_REASON =
      "An internal error occurred in the judger while judging this submission.";

  /**
   * Creates a new judging task. Only one judging task runs at a time.
   *
   * @param submissionId - the unique identifier of the submission
   * @throws IllgealSubmissionException if the submission does not exist
   * @throws DataAccessException if the submission or the outcome of its judging cannot be read or
   *     written
   */
  public void createNewTask(long submissionId) throws IllgealSubmissionException {
    synchronized (this) {
      String baseDirectory =
          String.format("%s/voj-%s", new Object[] {workBaseDirectory, submissionId});
      String baseFileName = DigestUtils.getRandomString(12, DigestUtils.Mode.ALPHA);

      // The web application queues the task only once the submission is committed (after the
      // transaction that stores it), so it can be read right away.
      Submission submission = submissionMapper.getSubmission(submissionId);
      if (submission == null) {
        throw new IllgealSubmissionException(
            String.format("Illegal submission #%s", new Object[] {submissionId}));
      }
      if (!applicationDispatcher.onJudgingStarted(submission)) {
        LOGGER.info(
            String.format(
                "Skipping submission #%d: it is not pending (e.g. its task was delivered twice).",
                submissionId));
        return;
      }
      try {
        judge(submission, baseDirectory, baseFileName);
      } catch (DataAccessException ex) {
        // The database cannot be reached: neither the verdict nor a System Error can be recorded.
        // The task is delivered again later (see MessageReceiver).
        throw ex;
      } catch (SystemError ex) {
        LOGGER.error(String.format("System Error in submission #%d", submissionId), ex);
        applicationDispatcher.onErrorOccurred(submission, ex.getMessage());
      } catch (Exception ex) {
        LOGGER.error(String.format("Failed to judge submission #%d", submissionId), ex);
        applicationDispatcher.onErrorOccurred(submission, INTERNAL_ERROR_REASON);
      } finally {
        // Always remove the work directory (source, binary and program output), even when a stage
        // throws; otherwise it lingers in a shared location, readable by later submissions that
        // run as the same sandbox user.
        cleanUp(baseDirectory);
      }
    }
  }

  /**
   * Judges a submission: loads the problem's test data, compiles the submission and runs it against
   * every checkpoint. Note: the random file name is used to prevent the application from
   * recursively invoking itself.
   *
   * @param submission - the submission object
   * @param workDirectory - the directory of the compilation result and the program output
   * @param baseFileName - the random file name (without suffix)
   * @throws SystemError if the submission cannot be judged for a known reason
   * @throws Exception if the submission cannot be judged for an unexpected reason
   */
  private void judge(Submission submission, String workDirectory, String baseFileName)
      throws Exception {
    List<Checkpoint> checkpoints = getCheckpoints(submission);
    preprocessor.createTestCode(submission, workDirectory, baseFileName);
    if (compile(submission, workDirectory, baseFileName)) {
      runProgram(submission, checkpoints, workDirectory, baseFileName);
    }
  }

  /**
   * Makes the problem's test data available locally and reads its checkpoints. There is nothing to
   * judge a submission against without them, so a problem without checkpoints is a System Error:
   * every submission that compiles would be Accepted otherwise.
   *
   * @param submission - the submission object
   * @return the problem's checkpoints (never empty)
   * @throws SystemError if the test data cannot be loaded or there is none
   */
  private List<Checkpoint> getCheckpoints(Submission submission) throws SystemError {
    long problemId = submission.getProblem().getProblemId();
    List<Checkpoint> checkpoints;
    try {
      checkpointStore.sync(submission.getProblem());
      checkpoints = checkpointStore.getCheckpoints(problemId);
    } catch (IOException ex) {
      throw new SystemError("The test data of this problem could not be loaded.", ex);
    }
    if (checkpoints.isEmpty()) {
      throw new SystemError("This problem has no test data (checkpoints) to judge against.", null);
    }
    return checkpoints;
  }

  /**
   * Creates a compilation task. Note: the random file name is used to prevent the application from
   * recursively invoking itself.
   *
   * @param submission - the submission object
   * @param workDirectory - the directory used for producing compilation output
   * @param baseFileName - the random file name (without suffix)
   * @return whether the compilation succeeded
   * @throws SystemError if the sandbox failed to run the compiler
   */
  private boolean compile(Submission submission, String workDirectory, String baseFileName)
      throws SystemError {
    Map<String, Object> result = compiler.getCompileResult(submission, workDirectory, baseFileName);
    if (result.get("verdict") == Verdict.INTERNAL_ERROR) {
      throw new SystemError("The sandbox failed to run the compiler.", null);
    }
    applicationDispatcher.onCompileFinished(submission, result);
    return (Boolean) result.get("isSuccessful");
  }

  /**
   * Runs the program.
   *
   * @param submission - the submission object
   * @param checkpoints - the problem's checkpoints
   * @param workDirectory - the directory of the compilation result and the program output
   * @param baseFileName - the file name of the application to run (without file suffix)
   * @throws SystemError if the test data cannot be read
   */
  private void runProgram(
      Submission submission,
      List<Checkpoint> checkpoints,
      String workDirectory,
      String baseFileName)
      throws SystemError {
    List<Map<String, Object>> runtimeResults = new ArrayList<Map<String, Object>>();
    long submissionId = submission.getSubmissionId();
    long problemId = submission.getProblem().getProblemId();

    for (Checkpoint checkpoint : checkpoints) {
      int checkpointId = checkpoint.getCheckpointId();
      int checkpointScore = checkpoint.getScore();
      String inputFilePath =
          String.format(
              "%s/%s/input#%s.txt", new Object[] {checkpointDirectory, problemId, checkpointId});
      String stdOutputFilePath =
          String.format(
              "%s/%s/output#%s.txt", new Object[] {checkpointDirectory, problemId, checkpointId});
      String outputFilePath = getOutputFilePath(workDirectory, checkpointId);

      Map<String, Object> runtimeResult =
          getRuntimeResult(
              programRunner.getRuntimeResult(
                  submission, workDirectory, baseFileName, inputFilePath, outputFilePath),
              stdOutputFilePath,
              outputFilePath,
              checkpoint.isExactlyMatch());
      runtimeResult.put("score", checkpointScore);
      runtimeResults.add(runtimeResult);
      applicationDispatcher.onOneTestPointFinished(submissionId, checkpointId, runtimeResult);
    }
    applicationDispatcher.onAllTestPointsFinished(submission, runtimeResults);
  }

  /**
   * Gets the output path of the current checkpoint.
   *
   * @param workDirectory - the directory of the compilation result and the program output
   * @param checkpointId - the number of the current checkpoint
   * @return the output path of the current checkpoint
   */
  private String getOutputFilePath(String workDirectory, int checkpointId) {
    return String.format("%s/output#%s.txt", new Object[] {workDirectory, checkpointId});
  }

  /**
   * Gets the program's runtime result (and the answer comparison result).
   *
   * @param result - the Map object containing the program's runtime result
   * @param standardOutputFilePath - the path of the standard output file
   * @param outputFilePath - the path of the user's output file
   * @param exactlyMatch - whether the checkpoint requires an exact match
   * @return the Map object containing the program's runtime result
   * @throws SystemError if the standard output cannot be read
   */
  private Map<String, Object> getRuntimeResult(
      Map<String, Object> result,
      String standardOutputFilePath,
      String outputFilePath,
      boolean exactlyMatch)
      throws SystemError {
    String runtimeResultSlug = (String) result.get("runtimeResult");
    int usedTime = (Integer) result.get("usedTime");
    int usedMemory = (Integer) result.get("usedMemory");

    if (runtimeResultSlug.equals("AC")) {
      try {
        runtimeResultSlug =
            comparator.getComparisonResult(standardOutputFilePath, outputFilePath, exactlyMatch);
      } catch (IOException ex) {
        // The comparator judges an output it may not read (e.g. a planted link) a Wrong Answer
        // itself, so this is the judger failing to read the test data or the output.
        throw new SystemError("The test data of this problem could not be read.", ex);
      }
      result.put("runtimeResult", runtimeResultSlug);
    }
    LOGGER.info(
        String.format(
            "RuntimeResult: [%s, Time: %d ms, Memory: %d KB]",
            new Object[] {runtimeResultSlug, usedTime, usedMemory}));

    return result;
  }

  /**
   * Cleans up the generated files after judging completes.
   *
   * @param baseDirectory - the directory used for producing output results
   */
  private void cleanUp(String baseDirectory) {
    try {
      // The directory was writable by the submission, so never follow links planted inside it.
      FileSystemUtils.deleteRecursively(Paths.get(baseDirectory));
    } catch (IOException ex) {
      LOGGER.catching(ex);
    }
  }

  /**
   * A judging that cannot be completed for a known reason that is not the submission's fault. Its
   * message is shown to the user in the judge log of the resulting System Error.
   */
  static final class SystemError extends Exception {
    /**
     * Constructor.
     *
     * @param reason - the reason, shown to the user
     * @param cause - the underlying exception, or {@code null}
     */
    SystemError(String reason, Throwable cause) {
      super(reason, cause);
    }

    /** The unique serialization identifier. */
    private static final long serialVersionUID = 1L;
  }

  /**
   * The autowired ApplicationDispatcher object, which pushes a message to the message queue after
   * each stage's task completes.
   */
  @Lazy @Autowired private ApplicationDispatcher applicationDispatcher;

  /** The autowired Preprocessor object, which performs the preparation work before compilation. */
  @Autowired private Preprocessor preprocessor;

  /** The autowired Compiler object, which performs the compilation work. */
  @Autowired private Compiler compiler;

  /** The autowired ProgramRunner object, which performs the program execution work. */
  @Autowired private ProgramRunner programRunner;

  /** The autowired Comparator object, which performs the output comparison work. */
  @Autowired private Comparator comparator;

  /** The autowired SubmissionMapper object. */
  @Autowired private SubmissionMapper submissionMapper;

  /** The autowired CheckpointStore, which caches a problem's checkpoints locally. */
  @Autowired private CheckpointStore checkpointStore;

  /** The working directory of the judger, used to store compilation results and program output. */
  @Value("${judger.workDir}")
  private String workBaseDirectory;

  /** The storage directory of checkpoints, used to store the input/output data of checkpoints. */
  @Value("${judger.checkpointDir}")
  private String checkpointDirectory;

  /** The logger. */
  private static final Logger LOGGER = LogManager.getLogger(Dispatcher.class);
}
