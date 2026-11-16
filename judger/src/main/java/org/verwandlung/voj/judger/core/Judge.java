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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import org.verwandlung.voj.judger.application.ApplicationDispatcher;
import org.verwandlung.voj.judger.exception.NoCheckpointsException;
import org.verwandlung.voj.judger.model.Checkpoint;
import org.verwandlung.voj.judger.model.Submission;
import org.verwandlung.voj.judger.util.DigestUtils;
import org.verwandlung.voj.judger.util.FileSystemUtils;

/**
 * Judges a submission that this judger claimed: gets the test data of its problem, writes its
 * source, compiles it and runs it on every checkpoint, reports each step to the web application and
 * records the outcome (see {@link ApplicationDispatcher}).
 *
 * <p>Between its steps (the download of the test data, the compilation, the run of each
 * checkpoint), and before reporting one, the judging stops once it was superseded (see {@link
 * Lease}). A step itself runs to its end, bounded by its own limit: the download timeout, the
 * timeouts of the database statements, or the wall-clock limit of a compilation or run (see {@link
 * Limits#wallTimeLimitMs}).
 *
 * @author Haozhe Xie
 */
@Component
public class Judge {
  /** The judge log reason of a System Error of a problem without test data. */
  static final String NO_TEST_DATA_REASON =
      "This problem has no test data (checkpoints) to judge against.";

  /**
   * Creates the judge.
   *
   * @param applicationDispatcher - records the outcomes and reports the steps
   * @param checkpointStore - the test data of the problems
   * @param preprocessor - writes the source and creates the directories of a judging
   * @param compiler - compiles a submission
   * @param programRunner - runs it on a checkpoint
   * @param comparator - compares its output with the expected one
   */
  @Autowired
  public Judge(
      ApplicationDispatcher applicationDispatcher,
      CheckpointStore checkpointStore,
      Preprocessor preprocessor,
      Compiler compiler,
      ProgramRunner programRunner,
      Comparator comparator) {
    this.applicationDispatcher = applicationDispatcher;
    this.checkpointStore = checkpointStore;
    this.preprocessor = preprocessor;
    this.compiler = compiler;
    this.programRunner = programRunner;
    this.comparator = comparator;
  }

  /**
   * Judges a submission, and records its outcome: a verdict, or a System Error when its problem has
   * no test data. The directory of the judging is removed afterwards, however it ended.
   *
   * @param submission - the submission, claimed by this judger
   * @param lease - the lease of the judging
   * @throws FailedJudging if the judging failed for a known reason
   * @throws Superseded if the judging was superseded
   */
  public void judge(Submission submission, Lease lease) throws FailedJudging, Superseded {
    Path checkpointDirectory;
    List<Checkpoint> checkpoints;
    try {
      checkpointDirectory = checkpointStore.sync(submission.getProblem());
    } catch (NoCheckpointsException ex) {
      onProblemWithoutTestData(submission);
      return;
    } catch (IOException ex) {
      throw FailedJudging.ofJudger(
          "The judger could not download the test data of the problem.", ex);
    }
    try {
      checkpoints = checkpointStore.getCheckpoints(submission.getProblem().getProblemId());
    } catch (IOException ex) {
      throw FailedJudging.ofJudger(
          "The judger could not read its copy of the test data of the problem.", ex);
    }
    if (checkpoints.isEmpty()) {
      // There is nothing to judge a submission against: every one that compiles would be Accepted.
      onProblemWithoutTestData(submission);
      return;
    }
    lease.check();
    // In this judger's own directory, so that a judger that died while judging leaves nothing
    // behind for good (see CheckpointStore#open); unique per judging, in case the directory of a
    // judging of the same submission could not be removed.
    String workDirectory =
        checkpointStore
            .getWorkDirectory()
            .resolve(
                String.format(
                    "voj-%d-%s",
                    submission.getSubmissionId(),
                    DigestUtils.getRandomString(12, DigestUtils.Mode.ALPHANUMERIC)))
            .toString();
    try {
      try {
        preprocessor.createTestCode(submission, workDirectory);
      } catch (IOException ex) {
        throw FailedJudging.ofJudger("The judger could not write the source code to its disk.", ex);
      }
      if (compile(submission, lease, workDirectory)) {
        run(submission, lease, checkpoints, checkpointDirectory, workDirectory);
      }
    } finally {
      // The source, the program and its output, which the sandbox user could read: never left
      // behind, however the judging ended.
      cleanUp(workDirectory);
    }
  }

  /**
   * Judges a submission whose problem has no test data a System Error that says so.
   *
   * @param submission - the submission
   * @throws Superseded if the judging was superseded
   */
  private void onProblemWithoutTestData(Submission submission) throws Superseded {
    LOGGER.error(
        String.format(
            "System Error in submission #%d: problem #%d has no test data.",
            submission.getSubmissionId(), submission.getProblem().getProblemId()));
    applicationDispatcher.onErrorOccurred(submission, NO_TEST_DATA_REASON);
  }

  /**
   * Compiles a submission, and reports it (a Compile Error is its outcome).
   *
   * @param submission - the submission
   * @param lease - the lease of the judging
   * @param workDirectory - the directory of the judging
   * @return whether the compilation succeeded
   * @throws FailedJudging if the sandbox failed to run the compiler
   * @throws Superseded if the judging was superseded
   */
  private boolean compile(Submission submission, Lease lease, String workDirectory)
      throws FailedJudging, Superseded {
    CompileResult result = compiler.getCompileResult(submission, workDirectory);
    if (result.verdict() == Verdict.INTERNAL_ERROR) {
      throw FailedJudging.ofSubmission(
          "The sandbox of the judger failed to run the compiler.", null);
    }
    lease.check();
    applicationDispatcher.onCompileFinished(submission, result);
    return result.isSuccessful();
  }

  /**
   * Runs a compiled submission on every checkpoint, reports each run, and records the verdict.
   *
   * @param submission - the submission
   * @param lease - the lease of the judging
   * @param checkpoints - the checkpoints of its problem
   * @param checkpointDirectory - the directory of the test data of its problem
   * @param workDirectory - the directory of the judging
   * @throws FailedJudging if the sandbox failed to run the program, or the test data cannot be read
   * @throws Superseded if the judging was superseded
   */
  private void run(
      Submission submission,
      Lease lease,
      List<Checkpoint> checkpoints,
      Path checkpointDirectory,
      String workDirectory)
      throws FailedJudging, Superseded {
    List<CheckpointResult> results = new ArrayList<>();
    for (Checkpoint checkpoint : checkpoints) {
      int checkpointId = checkpoint.getCheckpointId();
      String inputFilePath =
          checkpointDirectory.resolve("input#" + checkpointId + ".txt").toString();
      String outputFilePath = workDirectory + "/output#" + checkpointId + ".txt";
      RunResult run =
          programRunner.getRuntimeResult(submission, workDirectory, inputFilePath, outputFilePath);
      CheckpointResult result =
          getCheckpointResult(
              checkpoint,
              run,
              checkpointDirectory.resolve("output#" + checkpointId + ".txt").toString(),
              outputFilePath);
      results.add(result);
      lease.check();
      applicationDispatcher.onOneTestPointFinished(submission, result);
    }
    applicationDispatcher.onAllTestPointsFinished(submission, results);
  }

  /**
   * Judges a run of the program on a checkpoint: by how it ended, or, if it exited normally, by
   * its output.
   *
   * @param checkpoint - the checkpoint
   * @param run - how the run ended
   * @param standardOutputFilePath - the path of the expected output
   * @param outputFilePath - the path of the program's output
   * @return the verdict on the checkpoint
   * @throws FailedJudging if the sandbox failed to run the program, or the expected output cannot
   *     be read
   */
  private CheckpointResult getCheckpointResult(
      Checkpoint checkpoint, RunResult run, String standardOutputFilePath, String outputFilePath)
      throws FailedJudging {
    String judgeResult;
    switch (run.verdict()) {
      case INTERNAL_ERROR:
        throw FailedJudging.ofSubmission(
            "The sandbox of the judger failed to run the program.", null);
      case NORMAL:
        try {
          judgeResult =
              comparator.getComparisonResult(
                  standardOutputFilePath, outputFilePath, checkpoint.isExactlyMatch());
        } catch (IOException ex) {
          // The comparator judges an output it may not read (e.g. a planted link) a Wrong Answer
          // itself, so this is the judger failing to read its copy of the test data.
          throw FailedJudging.ofJudger(
              "The judger could not read its copy of the test data of the problem.", ex);
        }
        break;
      default:
        judgeResult = run.verdict().getSlug();
    }
    LOGGER.info(
        String.format(
            "RuntimeResult: [%s, Time: %d ms, Memory: %d KB]",
            judgeResult, run.usedTime(), run.usedMemory()));
    return new CheckpointResult(
        checkpoint.getCheckpointId(),
        judgeResult,
        run.usedTime(),
        run.usedMemory(),
        CheckpointResult.ACCEPTED.equals(judgeResult) ? checkpoint.getScore() : 0);
  }

  /**
   * Removes the directory of a judging.
   *
   * @param workDirectory - the directory of the judging
   */
  private void cleanUp(String workDirectory) {
    try {
      // The directory was writable by the submission, so never follow links planted inside it.
      FileSystemUtils.deleteRecursively(Paths.get(workDirectory));
    } catch (IOException ex) {
      LOGGER.catching(ex);
    }
  }

  /** Records the outcomes and reports the steps. */
  private final ApplicationDispatcher applicationDispatcher;

  /** The test data of the problems. */
  private final CheckpointStore checkpointStore;

  /** Writes the source and creates the directories of a judging. */
  private final Preprocessor preprocessor;

  /** Compiles a submission. */
  private final Compiler compiler;

  /** Runs a submission on a checkpoint. */
  private final ProgramRunner programRunner;

  /** Compares the output of a run with the expected one. */
  private final Comparator comparator;

  /** The logger. */
  private static final Logger LOGGER = LogManager.getLogger(Judge.class);
}
