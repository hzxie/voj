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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.IntConsumer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Component;

import org.verwandlung.voj.judger.application.ApplicationDispatcher;
import org.verwandlung.voj.judger.exception.NoCheckpointsException;
import org.verwandlung.voj.judger.model.Checkpoint;
import org.verwandlung.voj.judger.model.Submission;
import org.verwandlung.voj.judger.util.DigestUtils;
import org.verwandlung.voj.judger.util.FileSystemUtils;

/**
 * The judger dispatcher. Takes the pending submissions from the database, one at a time, and drives
 * their judging. After each stage finishes, a message is pushed to the message queue; after judging
 * completes, the result is written to the database.
 *
 * <p>A judging either reaches an outcome or fails. The outcomes are the verdicts of the submission
 * (e.g. a Compile Error or a Wrong Answer), and a System Error when the web application says that
 * its problem has no test data. Every other way a judging can end without an outcome is a failed
 * judging, whoever is to blame (this judger, the web application, the database, the submission
 * itself, or a bug), as the judger cannot tell for sure: its test data cannot be downloaded or
 * read, its source code cannot be written, the sandbox cannot run the compiler or the program, the
 * outcome cannot be recorded, an unexpected exception, or (see {@link Lease}) its judger stopped
 * while judging it. Two budgets, independent of each other, keep failed judgings from going on for
 * ever:
 *
 * <ul>
 *   <li>The submission's: a failed judging releases it to the end of the queue, so that it holds no
 *       other submission up, and is counted on it, with its reason. The judger that claims it after
 *       {@value #MAX_FAILED_JUDGINGS} failed judgings judges it a System Error that gives the
 *       reason of the last one.
 *   <li>The judger's: after a failed judging it pauses (longer after each one in a row), and after
 *       {@value #MAX_FAILURES_IN_A_ROW} in a row it is not ready (see {@link #isReady}) until a
 *       judging of it reaches an outcome again; it keeps trying, at most every {@value
 *       #MAX_BACKOFF_MS} ms. The count is kept in its slot of the checkpoint directory, and a
 *       judging counts as failed there until it is over, so that a judger that dies while judging
 *       (e.g. one that crashes again after every restart) counts it too.
 * </ul>
 *
 * @author Haozhe Xie
 */
@Component
public class Dispatcher implements DisposableBean {
  /** The judge log reason of a failed judging with an unexpected cause. */
  static final String INTERNAL_ERROR_REASON =
      "An internal error occurred in the judger while judging this submission.";

  /** The judge log reason of a System Error of a problem without test data. */
  static final String NO_TEST_DATA_REASON =
      "This problem has no test data (checkpoints) to judge against.";

  /**
   * The judge log reason of a System Error after too many failed judgings, by their number and the
   * reason of the last one.
   */
  static final String GIVEN_UP_REASON =
      "%d judgings of this submission failed, the last one as follows: %s";

  /** How long (ms) the lease of a judging lasts; it is renewed every quarter of it. */
  static final long LEASE_TIMEOUT_MS = 60000;

  /** How many judgings of a submission may fail before it is judged a System Error. */
  public static final int MAX_FAILED_JUDGINGS = 5;

  /** How long (ms) an idle judger waits before it looks for a pending submission again. */
  static final long POLL_INTERVAL_MS = 2000;

  /** After how many failed judgings in a row a judger is not ready. */
  public static final int MAX_FAILURES_IN_A_ROW = 3;

  /** How long (ms) a judger pauses after a failed judging; doubles with each one in a row. */
  static final long FIRST_BACKOFF_MS = 5000;

  /** The longest pause (ms) of a judger after failed judgings. */
  static final long MAX_BACKOFF_MS = 120000;

  /** The exit status of a judger that stops after an Error (e.g. it ran out of memory). */
  static final int FATAL_ERROR_EXIT_STATUS = 70;

  /**
   * The file, in this judger's cache directory (see {@link CheckpointStore#open}), that holds how
   * many judgings of it failed in a row, including the one it is running: it outlives the process.
   */
  static final String FAILURES_IN_A_ROW_FILE = "failed-judgings";

  /**
   * Judges the pending submissions in some languages, one after the other, until the thread is
   * interrupted or the judger stops. When there is none, or none can be taken (e.g. the database is
   * unreachable), it looks again every {@value #POLL_INTERVAL_MS} ms; after a failed judging, it
   * pauses (see {@link #getBackoff}). It starts with the failed judgings in a row of its previous
   * run, the one that run was busy with when it ended included, and pauses first if there are any.
   *
   * <p>An Error (e.g. an OutOfMemoryError, or a StackOverflowError) leaves the judger in no state
   * to judge: see {@link #onFatalError}.
   *
   * @param languageIds - the unique identifiers of the languages this judger supports, not empty
   */
  public void judgeSubmissions(List<Integer> languageIds) {
    localFailures = readFailuresInARow();
    isJudging = true;
    try {
      long pause = 0;
      if (localFailures > 0) {
        pause = getBackoff();
        LOGGER.warn(
            String.format(
                "The last %d judgings of this judger failed (or its previous run stopped while"
                    + " judging): it waits %d s before it takes a submission%s.",
                localFailures,
                pause / 1000,
                localFailures >= maxFailuresInARow
                    ? ", and is not ready until a judging of it reaches an outcome"
                    : ""));
      }
      while (!Thread.currentThread().isInterrupted() && !isStopping) {
        if (pause > 0) {
          try {
            Thread.sleep(pause);
          } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            break;
          }
        }
        try {
          boolean isClaimed = judgeNextSubmission(languageIds);
          if (isClaimFailing) {
            isClaimFailing = false;
            LOGGER.info("This judger takes submissions from the database again.");
          }
          pause = localFailures > 0 ? getBackoff() : isClaimed ? 0 : pollInterval;
        } catch (RuntimeException ex) {
          if (!isClaimFailing) {
            isClaimFailing = true;
            LOGGER.error(
                "Failed to take a submission to judge (is the database reachable?); trying again"
                    + " every "
                    + pollInterval
                    + " ms.",
                ex);
          }
          pause = pollInterval;
        } catch (Error error) {
          onFatalError(error);
          return;
        }
      }
    } finally {
      isJudging = false;
    }
  }

  /**
   * Stops the judger after an Error, which leaves it in no state to judge: logs it and exits the
   * process (status {@value #FATAL_ERROR_EXIT_STATUS}), so that its supervisor (e.g. Docker's
   * restart policy) starts it again. The submission it was judging is neither released nor judged
   * (the judging may be in any state): it is held until its lease expired, and the judging is then
   * counted as a failed one, as is the failed judging in a row of this judger (see {@link
   * #judgeSubmissions}).
   *
   * @param error - the error
   */
  void onFatalError(Error error) {
    // Nothing more is claimed, recorded or released (see #destroy).
    isStopping = true;
    isJudging = false;
    try {
      LOGGER.fatal(
          "The judger stops after an error, and exits so that it is started again: the submission"
              + " it was judging is taken again once its lease expired.",
          error);
    } finally {
      processExit.accept(FATAL_ERROR_EXIT_STATUS);
    }
  }

  /**
   * Stops the judger as it shuts down (e.g. it was asked to stop, by its supervisor or in a rolling
   * update): it claims no more submissions, and releases the submission it is judging, at its place
   * in the queue, without counting the judging as a failed one, neither on the submission nor on
   * this judger, so that another judger takes it at once. The judging itself runs on until the
   * process exits, but cannot record anything any more. Nothing is released after a fatal error
   * (see {@link #onFatalError}).
   */
  @Override
  public void destroy() {
    if (isStopping) {
      return;
    }
    isStopping = true;
    Submission submission = claimedSubmission;
    if (submission == null) {
      return;
    }
    try {
      if (applicationDispatcher.releaseSubmission(submission)) {
        writeFailuresInARow(localFailures);
        LOGGER.info(
            String.format(
                "Released submission #%d, whose judging stops as this judger shuts down.",
                submission.getSubmissionId()));
      }
    } catch (RuntimeException ex) {
      LOGGER.warn(
          String.format(
              "Failed to release submission #%d as this judger shuts down: it is taken again once"
                  + " the lease of its judging expired.",
              submission.getSubmissionId()),
          ex);
    }
  }

  /**
   * Claims the next pending submission in some languages (the one queued first) and judges it. Only
   * one judging runs at a time.
   *
   * <p>A judging renews its lease in the background while it runs (see {@link Lease}). A judging
   * that fails releases the submission to the end of the queue and is counted (see {@link
   * Dispatcher}); a submission with {@value #MAX_FAILED_JUDGINGS} failed judgings is judged a
   * System Error instead.
   *
   * @param languageIds - the unique identifiers of the languages this judger supports, not empty
   * @return whether a submission was claimed
   * @throws DataAccessException if no submission can be claimed as the database is unreachable
   */
  public boolean judgeNextSubmission(List<Integer> languageIds) {
    synchronized (this) {
      if (isStopping) {
        return false;
      }
      // In this judger's own directory, so that a judger that died while judging leaves nothing
      // behind for good (see CheckpointStore#getWorkDirectory). A judger without one claims nothing.
      Path workDirectory;
      try {
        workDirectory = checkpointStore.getWorkDirectory();
      } catch (IOException ex) {
        onFailedJudging(null, "This judger has no usable work directory.", ex);
        return false;
      }
      Submission submission = applicationDispatcher.claimSubmission(languageIds, getLeaseSeconds());
      if (submission == null) {
        return false;
      }
      claimedSubmission = submission;
      try {
        if (isStopping) {
          // The judger started to shut down while it claimed the submission (see #destroy).
          applicationDispatcher.releaseSubmission(submission);
        } else {
          judgeSubmission(submission, workDirectory);
        }
      } finally {
        claimedSubmission = null;
      }
      return true;
    }
  }

  /**
   * Judges a submission this judger claimed, or judges it a System Error after too many failed
   * judgings, and counts the outcome on this judger.
   *
   * @param submission - the submission, claimed by this judger
   * @param workDirectory - this judger's work directory
   */
  private void judgeSubmission(Submission submission, Path workDirectory) {
    long submissionId = submission.getSubmissionId();
    int failedJudgings = submission.getFailedJudgings();
    if (failedJudgings >= maxFailedJudgings) {
      String reason =
          String.format(
              GIVEN_UP_REASON,
              failedJudgings,
              submission.getLastFailure() == null
                  ? INTERNAL_ERROR_REASON
                  : submission.getLastFailure());
      LOGGER.error(String.format("System Error in submission #%d: %s", submissionId, reason));
      applicationDispatcher.onErrorOccurred(submission, reason);
      return;
    }
    // Counted as failed until it is over, in case this process ends with it.
    recordFailuresInARow(localFailures + 1);
    // Unique per judging, in case the directory of a judging of the same submission could not be
    // removed.
    String baseDirectory =
        workDirectory
            .resolve(
                String.format(
                    "voj-%d-%s",
                    submissionId, DigestUtils.getRandomString(12, DigestUtils.Mode.ALPHANUMERIC)))
            .toString();
    try (Lease lease = new Lease(submission)) {
      judge(submission, lease, baseDirectory);
      onJudged();
    } catch (FailedJudging ex) {
      onFailedJudging(submission, ex.getMessage(), ex.getCause());
    } catch (Superseded ex) {
      LOGGER.info(
          String.format(
              "Stopped judging submission #%d: it was rejudged, or claimed by another judging"
                  + " meanwhile.",
              submissionId));
      recordFailuresInARow(localFailures);
    } catch (DataAccessException ex) {
      onFailedJudging(
          submission, "The outcome of the judging could not be recorded in the database.", ex);
    } catch (Exception ex) {
      onFailedJudging(submission, INTERNAL_ERROR_REASON, ex);
    } finally {
      // Always remove the work directory (source, binary and program output), even when a stage
      // throws; otherwise it lingers in a shared location, readable by later submissions that
      // run as the same sandbox user.
      cleanUp(baseDirectory);
    }
  }

  /**
   * Tells whether this judger takes submissions: it judges, and fewer than {@value
   * #MAX_FAILURES_IN_A_ROW} of its judgings in a row failed. Recorded with its heartbeat, for the
   * web application, which judges a System Error the pending submissions in a language that no
   * ready judger supports for a while.
   *
   * @return whether this judger takes submissions
   */
  public boolean isReady() {
    return isJudging && !isStopping && localFailures < maxFailuresInARow;
  }

  /** Records a judging that reached an outcome: this judger's failed judgings in a row are over. */
  private void onJudged() {
    if (localFailures >= maxFailuresInARow) {
      LOGGER.info("A judging of this judger reached an outcome again: it is ready.");
    }
    localFailures = 0;
    recordFailuresInARow(0);
  }

  /**
   * Records a failed judging: releases its submission to the end of the queue, counting the
   * failure on it, counts it on this judger, which pauses (see {@link #getBackoff}), and logs it,
   * once the judger is not ready at most once until it is again.
   *
   * @param submission - the submission of the judging, or {@code null} if none was claimed
   * @param reason - why the judging failed, shown to the user if the submission is given up on
   * @param cause - the underlying exception, or {@code null}
   */
  private void onFailedJudging(Submission submission, String reason, Throwable cause) {
    int failures = ++localFailures;
    recordFailuresInARow(failures);
    String message =
        submission == null
            ? reason
            : String.format(
                "The judging of submission #%d failed (it is judged again later, or a System"
                    + " Error after %d failed judgings): %s",
                submission.getSubmissionId(), maxFailedJudgings, reason);
    if (submission != null) {
      try {
        applicationDispatcher.requeueSubmission(submission, reason);
      } catch (DataAccessException ex) {
        LOGGER.warn(
            String.format(
                "Failed to release submission #%d: it is taken again once the lease of its"
                    + " judging expired.",
                submission.getSubmissionId()),
            ex);
      }
    }
    if (failures < maxFailuresInARow) {
      LOGGER.warn(message, cause);
    } else if (failures == maxFailuresInARow) {
      LOGGER.error(
          String.format(
              "%d judgings of this judger failed in a row, the last one: %s This judger is not"
                  + " ready until a judging of it reaches an outcome again, which it tries at"
                  + " most every %d s.",
              failures, message, maxBackoff / 1000),
          cause);
    } else {
      LOGGER.debug(message, cause);
    }
  }

  /**
   * Gets how long this judger pauses after its failed judgings in a row: {@value
   * #FIRST_BACKOFF_MS} ms after the first one, twice as long after each next one, at most {@value
   * #MAX_BACKOFF_MS} ms.
   *
   * @return how long (ms) this judger pauses
   */
  long getBackoff() {
    int doublings = Math.min(localFailures - 1, 20);
    return Math.min(firstBackoff << Math.max(doublings, 0), maxBackoff);
  }

  /**
   * Records how many judgings of this judger failed in a row (see {@link #FAILURES_IN_A_ROW_FILE}),
   * unless the judger stops: then the count is the one it stops with (see {@link #destroy} and
   * {@link #onFatalError}).
   *
   * @param failures - the number of failed judgings in a row
   */
  private void recordFailuresInARow(int failures) {
    if (!isStopping) {
      writeFailuresInARow(failures);
    }
  }

  /**
   * Writes how many judgings of this judger failed in a row to {@link #FAILURES_IN_A_ROW_FILE}. A
   * failure to write it is logged: it only matters if this judger stops while judging.
   *
   * @param failures - the number of failed judgings in a row
   */
  private void writeFailuresInARow(int failures) {
    try {
      Files.writeString(getFailuresInARowFile(), String.valueOf(failures));
    } catch (IOException | RuntimeException ex) {
      LOGGER.warn("Failed to record the number of failed judgings in a row of this judger.", ex);
    }
  }

  /**
   * Reads how many judgings of this judger failed in a row, as its previous run left the count.
   *
   * @return the number of failed judgings in a row, 0 if none was recorded
   */
  private int readFailuresInARow() {
    try {
      return Math.max(
          Integer.parseInt(
              Files.readString(getFailuresInARowFile(), StandardCharsets.UTF_8).trim()),
          0);
    } catch (NoSuchFileException ex) {
      return 0;
    } catch (IOException | RuntimeException ex) {
      LOGGER.warn("Failed to read the number of failed judgings in a row of this judger.", ex);
      return 0;
    }
  }

  /**
   * Gets the file that holds how many judgings of this judger failed in a row.
   *
   * @return the file, in this judger's cache directory
   * @throws IOException if the cache directory cannot be opened
   */
  private Path getFailuresInARowFile() throws IOException {
    return checkpointStore.open().resolve(FAILURES_IN_A_ROW_FILE);
  }

  /**
   * Judges a submission: loads the problem's test data, compiles the submission and runs it against
   * every checkpoint. Note: the random file name is used to prevent the application from
   * recursively invoking itself.
   *
   * <p>Between its steps (the download of the test data, the compilation, the run of each
   * checkpoint), and before reporting one, the judging stops once it was superseded (see {@link
   * Lease}). A step itself runs to its end, bounded by its own limit: the download timeout, the
   * timeouts of the database statements, or the wall-clock limit of a compilation or run (see
   * {@link Limits#wallTimeLimitMs}).
   *
   * @param submission - the submission object
   * @param lease - the lease of the judging
   * @param workDirectory - the directory of the compilation result and the program output
   * @throws FailedJudging if the judging failed for a known reason
   * @throws Superseded if the judging was superseded
   * @throws Exception if the judging failed for an unexpected reason
   */
  private void judge(Submission submission, Lease lease, String workDirectory) throws Exception {
    long problemId = submission.getProblem().getProblemId();
    Path checkpointDirectory;
    List<Checkpoint> checkpoints;
    try {
      checkpointDirectory = checkpointStore.sync(submission.getProblem());
    } catch (NoCheckpointsException ex) {
      onProblemWithoutTestData(submission);
      return;
    } catch (IOException ex) {
      throw new FailedJudging("The judger could not download the test data of the problem.", ex);
    }
    try {
      checkpoints = checkpointStore.getCheckpoints(problemId);
    } catch (IOException ex) {
      throw new FailedJudging(
          "The judger could not read its copy of the test data of the problem.", ex);
    }
    if (checkpoints.isEmpty()) {
      // There is nothing to judge a submission against: every one that compiles would be Accepted.
      onProblemWithoutTestData(submission);
      return;
    }
    lease.check();
    try {
      preprocessor.createTestCode(submission, workDirectory);
    } catch (Exception ex) {
      throw new FailedJudging("The judger could not write the source code to its disk.", ex);
    }
    if (compile(submission, lease, workDirectory)) {
      runProgram(submission, lease, checkpoints, checkpointDirectory, workDirectory);
    }
  }

  /**
   * Judges a submission whose problem has no test data a System Error that says so.
   *
   * @param submission - the submission object
   */
  private void onProblemWithoutTestData(Submission submission) {
    LOGGER.error(
        String.format(
            "System Error in submission #%d: problem #%d has no test data.",
            submission.getSubmissionId(), submission.getProblem().getProblemId()));
    applicationDispatcher.onErrorOccurred(submission, NO_TEST_DATA_REASON);
  }

  /**
   * Creates a compilation task.
   *
   * @param submission - the submission object
   * @param lease - the lease of the judging
   * @param workDirectory - the directory used for producing compilation output
   * @return whether the compilation succeeded
   * @throws FailedJudging if the sandbox failed to run the compiler
   * @throws Superseded if the judging was superseded
   */
  private boolean compile(Submission submission, Lease lease, String workDirectory)
      throws FailedJudging, Superseded {
    CompileResult result = compiler.getCompileResult(submission, workDirectory);
    if (result.verdict() == Verdict.INTERNAL_ERROR) {
      throw new FailedJudging("The sandbox of the judger failed to run the compiler.", null);
    }
    lease.check();
    applicationDispatcher.onCompileFinished(submission, result);
    return result.isSuccessful();
  }

  /**
   * Runs the program.
   *
   * @param submission - the submission object
   * @param lease - the lease of the judging
   * @param checkpoints - the problem's checkpoints
   * @param checkpointDirectory - the directory holding the problem's test data
   * @param workDirectory - the directory of the compilation result and the program output
   * @throws FailedJudging if the sandbox failed to run the program, or the test data cannot be read
   * @throws Superseded if the judging was superseded
   */
  private void runProgram(
      Submission submission,
      Lease lease,
      List<Checkpoint> checkpoints,
      Path checkpointDirectory,
      String workDirectory)
      throws FailedJudging, Superseded {
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
                  submission, workDirectory, inputFilePath, outputFilePath),
              stdOutputFilePath,
              outputFilePath,
              checkpoint.isExactlyMatch());
      runtimeResult.put("score", checkpointScore);
      runtimeResults.add(runtimeResult);
      lease.check();
      applicationDispatcher.onOneTestPointFinished(submission, checkpointId, runtimeResult);
    }
    applicationDispatcher.onAllTestPointsFinished(submission, runtimeResults);
  }

  /**
   * The lease of a judging: renewed in the background every quarter of the lease timeout while the
   * judging runs, however long its steps take, so that no other judging claims the submission
   * meanwhile. A renewal that finds the judging superseded (the submission was rejudged, or
   * claimed by another judging as the lease had expired, e.g. while the database was unreachable)
   * marks it so, and the judging stops at its next step (see {@link #check}). Closing the lease
   * stops the renewals; the lease then expires, which matters unless the judging recorded its
   * outcome or released the submission.
   *
   * <p>An Error in a renewal stops the judging, as one in the judging itself does (see {@link
   * #onFatalError}): without its renewals, the lease would expire under a judging that goes on.
   */
  final class Lease implements AutoCloseable {
    /**
     * Starts renewing the lease of a judging.
     *
     * @param submission - the submission, claimed by the judging
     */
    Lease(Submission submission) {
      this.submission = submission;
      long renewalInterval = Math.max(1, leaseTimeout / 4);
      renewal =
          leaseRenewer.scheduleWithFixedDelay(
              this::renew, renewalInterval, renewalInterval, TimeUnit.MILLISECONDS);
    }

    /**
     * Renews the lease; a failure (e.g. the database is unreachable) is retried next time. An
     * Error marks the judging superseded, so that it stops without recording anything, and stops
     * the judger.
     */
    private void renew() {
      try {
        if (!applicationDispatcher.renewLease(submission, getLeaseSeconds())) {
          isSuperseded = true;
        }
      } catch (RuntimeException ex) {
        LOGGER.warn(
            String.format(
                "Failed to renew the lease of the judging of submission #%d.",
                submission.getSubmissionId()),
            ex);
      } catch (Error error) {
        isSuperseded = true;
        renewal.cancel(false);
        onFatalError(error);
      }
    }

    /**
     * Stops the judging if it was superseded.
     *
     * @throws Superseded if the judging was superseded
     */
    void check() throws Superseded {
      if (isSuperseded) {
        throw new Superseded();
      }
    }

    /** Stops renewing the lease. */
    @Override
    public void close() {
      renewal.cancel(false);
    }

    /** The submission, claimed by the judging. */
    private final Submission submission;

    /** The scheduled renewals. */
    private final ScheduledFuture<?> renewal;

    /** Whether a renewal found the judging superseded. */
    private volatile boolean isSuperseded;
  }

  /**
   * Gets how long the lease of a judging lasts, in seconds, rounded up.
   *
   * @return how long the lease lasts, in seconds
   */
  private long getLeaseSeconds() {
    return (leaseTimeout + 999) / 1000;
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
   * @param result - the outcome of the run
   * @param standardOutputFilePath - the path of the standard output file
   * @param outputFilePath - the path of the user's output file
   * @param exactlyMatch - whether the checkpoint requires an exact match
   * @return the Map object containing the program's runtime result
   * @throws FailedJudging if the sandbox failed to run the program, or the standard output cannot
   *     be read
   */
  private Map<String, Object> getRuntimeResult(
      RunResult result, String standardOutputFilePath, String outputFilePath, boolean exactlyMatch)
      throws FailedJudging {
    String runtimeResultSlug = result.verdict().getSlug();
    int usedTime = result.usedTime();
    int usedMemory = result.usedMemory();

    if (result.verdict() == Verdict.INTERNAL_ERROR) {
      throw new FailedJudging("The sandbox of the judger failed to run the program.", null);
    }
    if (result.verdict() == Verdict.NORMAL) {
      try {
        runtimeResultSlug =
            comparator.getComparisonResult(standardOutputFilePath, outputFilePath, exactlyMatch);
      } catch (IOException ex) {
        // The comparator judges an output it may not read (e.g. a planted link) a Wrong Answer
        // itself, so this is the judger failing to read its copy of the test data.
        throw new FailedJudging(
            "The judger could not read its copy of the test data of the problem.", ex);
      }
    }
    LOGGER.info(
        String.format(
            "RuntimeResult: [%s, Time: %d ms, Memory: %d KB]",
            new Object[] {runtimeResultSlug, usedTime, usedMemory}));

    Map<String, Object> runtimeResult = new HashMap<>();
    runtimeResult.put("runtimeResult", runtimeResultSlug);
    runtimeResult.put("usedTime", usedTime);
    runtimeResult.put("usedMemory", usedMemory);
    return runtimeResult;
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
   * A judging that failed for a known reason (see {@link Dispatcher}). Its message is the reason,
   * shown to the user in the System Error of the submission if it is the last of too many failed
   * judgings.
   */
  static final class FailedJudging extends Exception {
    /**
     * Constructor.
     *
     * @param reason - the reason
     * @param cause - the underlying exception, or {@code null}
     */
    FailedJudging(String reason, Throwable cause) {
      super(reason, cause);
    }

    /** The unique serialization identifier. */
    private static final long serialVersionUID = 1L;
  }

  /**
   * A judging that was superseded: the submission was rejudged, or claimed by another judging as
   * the lease of the judging had expired.
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
  @Autowired private ApplicationDispatcher applicationDispatcher;

  /** The autowired Preprocessor object, which performs the preparation work before compilation. */
  @Autowired private Preprocessor preprocessor;

  /** The autowired Compiler object, which performs the compilation work. */
  @Autowired private Compiler compiler;

  /** The autowired ProgramRunner object, which performs the program execution work. */
  @Autowired private ProgramRunner programRunner;

  /** The autowired Comparator object, which performs the output comparison work. */
  @Autowired private Comparator comparator;

  /** The autowired CheckpointStore, which caches a problem's checkpoints locally. */
  @Autowired private CheckpointStore checkpointStore;

  /*
   * The policy of the judging queue: the same on every judger, so not configurable. Fields rather
   * than constants only so that the tests can shorten the times.
   */

  /** How long (ms) an idle judger waits before it looks for a submission again. */
  private long pollInterval = POLL_INTERVAL_MS;

  /** How long (ms) the lease of a judging lasts; it is renewed every quarter of it. */
  private long leaseTimeout = LEASE_TIMEOUT_MS;

  /** How many judgings of a submission may fail before it is a System Error. */
  private int maxFailedJudgings = MAX_FAILED_JUDGINGS;

  /** After how many failed judgings in a row this judger is not ready. */
  private int maxFailuresInARow = MAX_FAILURES_IN_A_ROW;

  /** How long (ms) this judger pauses after a failed judging. */
  private long firstBackoff = FIRST_BACKOFF_MS;

  /** The longest pause (ms) of this judger after failed judgings. */
  private long maxBackoff = MAX_BACKOFF_MS;

  /** Exits the process with a status; replaced by the tests. */
  private IntConsumer processExit = System::exit;

  /** Whether the judging loop runs. */
  private volatile boolean isJudging;

  /** Whether the judger stops: it shuts down, or ran into an Error. */
  private volatile boolean isStopping;

  /** How many judgings (or attempts to judge) of this judger failed in a row. */
  private volatile int localFailures;

  /** Whether the last attempt to take a submission failed (e.g. the database is unreachable). */
  private boolean isClaimFailing;

  /** The submission this judger claimed and judges, or {@code null}. */
  private volatile Submission claimedSubmission;

  /** Renews the leases of the judgings, on a daemon thread, which never keeps the judger alive. */
  private final ScheduledExecutorService leaseRenewer =
      Executors.newSingleThreadScheduledExecutor(
          runnable -> {
            Thread thread = new Thread(runnable, "voj-lease-renewal");
            thread.setDaemon(true);
            return thread;
          });

  /** The logger. */
  private static final Logger LOGGER = LogManager.getLogger(Dispatcher.class);
}
