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
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Component;

import org.verwandlung.voj.judger.application.ApplicationDispatcher;
import org.verwandlung.voj.judger.exception.CheckpointsRefusedException;
import org.verwandlung.voj.judger.exception.IllgealSubmissionException;
import org.verwandlung.voj.judger.exception.InvalidCheckpointsException;
import org.verwandlung.voj.judger.exception.RetryableJudgingException;
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
   * <p>A judging that cannot be completed now, but likely later (see {@link
   * RetryableJudgingException}), is handed back for another delivery, unless this is the last
   * delivery of the task: the submission is then judged a System Error with the actual reason.
   *
   * <p>The judging renews its lease after each of its steps (see {@link #renewLease}), and stops
   * once it finds that it was superseded, without recording or sending anything.
   *
   * @param submissionId - the unique identifier of the submission
   * @param generation - the judging generation of the task
   * @param isLastDelivery - whether the broker will not deliver the task again
   * @throws IllgealSubmissionException if the submission does not exist
   * @throws DataAccessException if the submission or the outcome of its judging cannot be read or
   *     written
   * @throws RetryableJudgingException if the submission cannot be judged now, but likely later
   */
  public void createNewTask(long submissionId, long generation, boolean isLastDelivery)
      throws IllgealSubmissionException {
    synchronized (this) {
      // In this judger's own directory, so that a judger that died while judging leaves nothing
      // behind for good (see CheckpointStore#getWorkDirectory); unique per judging, in case the
      // directory of a judging of the same submission could not be removed.
      Path workDirectory;
      try {
        workDirectory = checkpointStore.getWorkDirectory();
      } catch (IOException ex) {
        throw new RetryableJudgingException("This judger has no usable work directory.", ex);
      }
      String baseDirectory =
          workDirectory
              .resolve(
                  String.format(
                      "voj-%d-%s",
                      submissionId,
                      DigestUtils.getRandomString(12, DigestUtils.Mode.ALPHANUMERIC)))
              .toString();
      String baseFileName = DigestUtils.getRandomString(12, DigestUtils.Mode.ALPHA);

      Long claimToken = applicationDispatcher.onJudgingStarted(submissionId, generation);
      if (claimToken == null) {
        LOGGER.info(
            String.format(
                "Skipping submission #%d: it is not pending (e.g. its task was delivered twice),"
                    + " was rejudged since its task (generation %d) was queued, or does not exist.",
                submissionId, generation));
        return;
      }
      // Read only once claimed, never before: an administrator may edit the submission's problem
      // or test data and rejudge it in between, and the judging must be of what was rejudged. (The
      // web application queues the task only once the submission is committed, so it exists.)
      Submission submission = submissionMapper.getSubmission(submissionId);
      if (submission == null) {
        throw new IllgealSubmissionException(
            String.format("Illegal submission #%s", new Object[] {submissionId}));
      }
      submission.setClaimToken(claimToken);
      try {
        judge(submission, baseDirectory, baseFileName);
      } catch (DataAccessException ex) {
        // The database cannot be reached: neither the verdict nor a System Error can be recorded.
        // The task is delivered again later (see MessageReceiver).
        throw ex;
      } catch (RetryableJudgingException ex) {
        if (isLastDelivery) {
          LOGGER.error(
              String.format(
                  "Cannot judge submission #%d now, and its task is not delivered again: System"
                      + " Error.",
                  submissionId),
              ex);
          applicationDispatcher.onErrorOccurred(submission, ex.getMessage());
          return;
        }
        LOGGER.warn(
            String.format(
                "Cannot judge submission #%d now; its task is delivered again.", submissionId),
            ex);
        applicationDispatcher.onJudgingHandedBack(submission);
        throw ex;
      } catch (Superseded ex) {
        LOGGER.info(
            String.format(
                "Stopped judging submission #%d: it was rejudged, claimed by a later delivery of"
                    + " its task, or judged a System Error meanwhile.",
                submissionId));
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
   * @throws Superseded if the judging was superseded
   * @throws Exception if the submission cannot be judged for an unexpected reason
   */
  private void judge(Submission submission, String workDirectory, String baseFileName)
      throws Exception {
    Path checkpointDirectory = syncCheckpoints(submission);
    List<Checkpoint> checkpoints = getCheckpoints(submission);
    renewLease(submission);
    preprocessor.createTestCode(submission, workDirectory, baseFileName);
    if (compile(submission, workDirectory, baseFileName)) {
      runProgram(submission, checkpoints, checkpointDirectory, workDirectory, baseFileName);
    }
  }

  /**
   * Makes the problem's test data available locally.
   *
   * <p>Only a download whose answer is final (the problem has no test data, its archive is
   * malformed, or the web application refuses this judger, e.g. for a wrong API token) is a System
   * Error. Any other failure (the web application restarting or overloaded, a network failure or
   * timeout, this judger's own disk) is no fault of the submission or its problem and likely gone
   * later or on another judger, so the task is delivered again instead.
   *
   * @param submission - the submission object
   * @return the directory holding the problem's test data
   * @throws SystemError if the problem has no usable test data, or this judger may not get it
   * @throws RetryableJudgingException if the test data cannot be downloaded now
   */
  private Path syncCheckpoints(Submission submission) throws SystemError {
    try {
      return checkpointStore.sync(submission.getProblem());
    } catch (InvalidCheckpointsException ex) {
      throw new SystemError("The test data of this problem could not be loaded.", ex);
    } catch (CheckpointsRefusedException ex) {
      int statusCode = ex.getStatusCode();
      String reason =
          String.format(
              "The web application refused to hand this judger the test data of this problem"
                  + " (HTTP %d).",
              statusCode);
      if (statusCode == 401 || statusCode == 403) {
        reason +=
            " Check that the judger's judger.apiToken (VOJ_JUDGER_API_TOKEN) is the web"
                + " application's.";
      }
      throw new SystemError(reason, ex);
    } catch (IOException ex) {
      throw new RetryableJudgingException("The test data could not be downloaded.", ex);
    }
  }

  /**
   * Reads the problem's checkpoints. There is nothing to judge a submission against without them,
   * so a problem without checkpoints is a System Error: every submission that compiles would be
   * Accepted otherwise.
   *
   * @param submission - the submission object
   * @return the problem's checkpoints (never empty)
   * @throws SystemError if the test data cannot be read or there is none
   */
  private List<Checkpoint> getCheckpoints(Submission submission) throws SystemError {
    long problemId = submission.getProblem().getProblemId();
    List<Checkpoint> checkpoints;
    try {
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
   * @throws Superseded if the judging was superseded
   */
  private boolean compile(Submission submission, String workDirectory, String baseFileName)
      throws SystemError, Superseded {
    Map<String, Object> result = compiler.getCompileResult(submission, workDirectory, baseFileName);
    if (result.get("verdict") == Verdict.INTERNAL_ERROR) {
      throw new SystemError("The sandbox failed to run the compiler.", null);
    }
    renewLease(submission);
    applicationDispatcher.onCompileFinished(submission, result);
    return (Boolean) result.get("isSuccessful");
  }

  /**
   * Runs the program.
   *
   * @param submission - the submission object
   * @param checkpoints - the problem's checkpoints
   * @param checkpointDirectory - the directory holding the problem's test data
   * @param workDirectory - the directory of the compilation result and the program output
   * @param baseFileName - the file name of the application to run (without file suffix)
   * @throws SystemError if the test data cannot be read
   * @throws Superseded if the judging was superseded
   */
  private void runProgram(
      Submission submission,
      List<Checkpoint> checkpoints,
      Path checkpointDirectory,
      String workDirectory,
      String baseFileName)
      throws SystemError, Superseded {
    List<Map<String, Object>> runtimeResults = new ArrayList<Map<String, Object>>();

    for (Checkpoint checkpoint : checkpoints) {
      int checkpointId = checkpoint.getCheckpointId();
      int checkpointScore = checkpoint.getScore();
      String inputFilePath =
          checkpointDirectory.resolve("input#" + checkpointId + ".txt").toString();
      String stdOutputFilePath =
          checkpointDirectory.resolve("output#" + checkpointId + ".txt").toString();
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
      renewLease(submission);
      applicationDispatcher.onOneTestPointFinished(submission, checkpointId, runtimeResult);
    }
    applicationDispatcher.onAllTestPointsFinished(submission, runtimeResults);
  }

  /**
   * Renews the lease of the judging after one of its steps (see {@link
   * ApplicationDispatcher#renewLease}), and before the step is reported: after the download of the
   * test data, after the compilation and after the run of each checkpoint. So the time between two
   * renewals is bounded by the longest of these steps (see {@code judging.leaseTimeout} in the
   * web application's configuration).
   *
   * @param submission - the submission object
   * @throws Superseded if the judging was superseded
   */
  private void renewLease(Submission submission) throws Superseded {
    if (!applicationDispatcher.renewLease(submission)) {
      throw new Superseded();
    }
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
   * A judging that was superseded: the submission was rejudged, claimed by a later delivery of its
   * task, or judged a System Error as the lease of the judging had expired.
   */
  static final class Superseded extends Exception {
    /** Constructor. */
    Superseded() {
      super(null, null, false, false);
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

  /** The logger. */
  private static final Logger LOGGER = LogManager.getLogger(Dispatcher.class);
}
