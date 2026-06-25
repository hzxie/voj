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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.verwandlung.voj.judger.model.Checkpoint;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import org.verwandlung.voj.judger.application.ApplicationDispatcher;
import org.verwandlung.voj.judger.exception.IllgealSubmissionException;
import org.verwandlung.voj.judger.mapper.CheckpointMapper;
import org.verwandlung.voj.judger.mapper.SubmissionMapper;
import org.verwandlung.voj.judger.model.Submission;
import org.verwandlung.voj.judger.util.DigestUtils;

/**
 * The judger dispatcher. Drives the judging workflow of the judger. After each stage finishes, a
 * message is pushed to the message queue; after judging completes, the result is written to the
 * database.
 *
 * @author Haozhe Xie
 */
@Component
public class Dispatcher {
  /**
   * Creates a new judging task. Only one judging task runs at a time.
   *
   * @param submissionId - the unique identifier of the submission
   * @throws IllgealSubmissionException
   * @throws InterruptedException
   */
  public void createNewTask(long submissionId)
      throws IllgealSubmissionException, InterruptedException {
    synchronized (this) {
      String baseDirectory =
          String.format("%s/voj-%s", new Object[] {workBaseDirectory, submissionId});
      String baseFileName = DigestUtils.getRandomString(12, DigestUtils.Mode.ALPHA);

      // Work around the issue where the record cannot be fetched for unknown reasons
      int tryTimes = 0;
      Submission submission = null;
      do {
        Thread.sleep(1000);
        submission = submissionMapper.getSubmission(submissionId);
      } while (submission == null && ++tryTimes <= 3);

      if (submission == null) {
        throw new IllgealSubmissionException(
            String.format("Illegal submission #%s", new Object[] {submissionId}));
      }
      preprocess(submission, baseDirectory, baseFileName);
      if (compile(submission, baseDirectory, baseFileName)) {
        runProgram(submission, baseDirectory, baseFileName);
      }
      cleanUp(baseDirectory);
    }
  }

  /**
   * Performs preprocessing work before judging. Note: the random file name is used to prevent the
   * application from recursively invoking itself.
   *
   * @param submission - the submission object
   * @param workDirectory - the directory used for producing compilation output
   * @param baseFileName - the random file name (without suffix)
   */
  private void preprocess(Submission submission, String workDirectory, String baseFileName) {
    try {
      long problemId = submission.getProblem().getProblemId();
      preprocessor.createTestCode(submission, workDirectory, baseFileName);
      preprocessor.fetchTestPoints(problemId);
    } catch (Exception ex) {
      LOGGER.catching(ex);

      long submissionId = submission.getSubmissionId();
      applicationDispatcher.onErrorOccurred(submissionId);
    }
  }

  /**
   * Creates a compilation task. Note: the random file name is used to prevent the application from
   * recursively invoking itself.
   *
   * @param submission - the submission object
   * @param workDirectory - the directory used for producing compilation output
   * @param baseFileName - the random file name (without suffix)
   */
  private boolean compile(Submission submission, String workDirectory, String baseFileName) {
    long submissionId = submission.getSubmissionId();
    Map<String, Object> result = compiler.getCompileResult(submission, workDirectory, baseFileName);

    applicationDispatcher.onCompileFinished(submissionId, result);
    return (Boolean) result.get("isSuccessful");
  }

  /**
   * Runs the program.
   *
   * @param submission - the submission object
   * @param workDirectory - the directory of the compilation result and the program output
   * @param baseFileName - the file name of the application to run (without file suffix)
   */
  private void runProgram(Submission submission, String workDirectory, String baseFileName) {
    List<Map<String, Object>> runtimeResults = new ArrayList<Map<String, Object>>();
    long submissionId = submission.getSubmissionId();
    long problemId = submission.getProblem().getProblemId();

    List<Checkpoint> checkpoints = checkpointMapper.getCheckpointsUsingProblemId(problemId);
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
              outputFilePath);
      runtimeResult.put("score", checkpointScore);
      runtimeResults.add(runtimeResult);
      applicationDispatcher.onOneTestPointFinished(submissionId, checkpointId, runtimeResult);
    }
    applicationDispatcher.onAllTestPointsFinished(submissionId, runtimeResults);
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
   * @return the Map object containing the program's runtime result
   */
  private Map<String, Object> getRuntimeResult(
      Map<String, Object> result, String standardOutputFilePath, String outputFilePath) {
    String runtimeResultSlug = (String) result.get("runtimeResult");
    int usedTime = (Integer) result.get("usedTime");
    int usedMemory = (Integer) result.get("usedMemory");

    if (runtimeResultSlug.equals("AC")
        && !isOutputTheSame(standardOutputFilePath, outputFilePath)) {
      runtimeResultSlug = "WA";
      result.put("runtimeResult", runtimeResultSlug);
    }
    LOGGER.info(
        String.format(
            "RuntimeResult: [%s, Time: %d ms, Memory: %d KB]",
            new Object[] {runtimeResultSlug, usedTime, usedMemory}));

    return result;
  }

  /**
   * Gets the comparison result between the user's output and the standard output.
   *
   * @param standardOutputFilePath - the path of the standard output file
   * @param outputFilePath - the path of the user's output file
   * @return whether the user's output and the standard output are the same
   */
  private boolean isOutputTheSame(String standardOutputFilePath, String outputFilePath) {
    try {
      return comparator.isOutputTheSame(standardOutputFilePath, outputFilePath);
    } catch (IOException ex) {
      LOGGER.catching(ex);
    }
    return false;
  }

  /**
   * Cleans up the generated files after judging completes.
   *
   * @param baseDirectory - the directory used for producing output results
   */
  private void cleanUp(String baseDirectory) {
    File baseDirFile = new File(baseDirectory);
    if (baseDirFile.exists()) {
      try {
        FileUtils.deleteDirectory(baseDirFile);
      } catch (IOException ex) {
        LOGGER.catching(ex);
      }
    }
  }

  /**
   * The autowired ApplicationDispatcher object, which pushes a message to the message queue after
   * each stage's task completes.
   */
  @Autowired private ApplicationDispatcher applicationDispatcher;

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

  /** The autowired CheckpointMapper object, used to obtain a problem's checkpoints. */
  @Autowired private CheckpointMapper checkpointMapper;

  /** The working directory of the judger, used to store compilation results and program output. */
  @Value("${judger.workDir}")
  private String workBaseDirectory;

  /** The storage directory of checkpoints, used to store the input/output data of checkpoints. */
  @Value("${judger.checkpointDir}")
  private String checkpointDirectory;

  /** The logger. */
  private static final Logger LOGGER = LogManager.getLogger(Dispatcher.class);
}
