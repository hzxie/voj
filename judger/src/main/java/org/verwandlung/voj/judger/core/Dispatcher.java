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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Component;

import org.verwandlung.voj.judger.application.ApplicationDispatcher;
import org.verwandlung.voj.judger.exception.CheckpointsRefusedException;
import org.verwandlung.voj.judger.exception.CreateDirectoryException;
import org.verwandlung.voj.judger.exception.InvalidCheckpointsException;
import org.verwandlung.voj.judger.model.Checkpoint;
import org.verwandlung.voj.judger.model.Language;
import org.verwandlung.voj.judger.model.Submission;
import org.verwandlung.voj.judger.util.DigestUtils;
import org.verwandlung.voj.judger.util.FileSystemUtils;

/**
 * The judger dispatcher. Takes the pending submissions from the database, one at a time, and drives
 * their judging. After each stage finishes, a message is pushed to the message queue; after judging
 * completes, the result is written to the database.
 *
 * <p>Who is to blame for a judging that cannot be completed decides what becomes of its submission:
 *
 * <ul>
 *   <li>The submission or its problem: the verdict (e.g. a Compile Error), or a System Error that
 *       says why, when the problem has no usable test data (the web application has none, its
 *       archive is malformed, it has no checkpoints). So is a failure of the judger nobody expected
 *       (a bug), which may as well be the submission's doing.
 *   <li>This judger (see {@link LocalFailure}): its work directory, its copy of the test data,
 *       the download of the test data (refused, e.g. for a wrong API token, or failed), its
 *       sandbox. The submission is released at once, at its place in the queue, for another
 *       judger, and nothing is recorded on it. This judger pauses (longer after each failure in a
 *       row), leaves the problem to the other judgers for a while if it could not get its test
 *       data, and after {@value #MAX_LOCAL_FAILURES} failures in a row is not ready (see {@link
 *       #isReady}) until a judging of it succeeds again: it keeps trying, at most every {@value
 *       #MAX_BACKOFF_MS} ms.
 *   <li>Nobody knows: the judging stopped without an outcome, its judger died, was killed, lost the
 *       database or ran past the time the limits of the submission allow (see {@link Lease}). Its
 *       lease expires, and the judger that takes the submission next counts an interrupted
 *       judging: after {@value #MAX_INTERRUPTIONS} of them, the submission is judged a System Error
 *       that says so. A submission that crashes every judger that takes it ends there too.
 * </ul>
 *
 * @author Haozhe Xie
 */
@Component
public class Dispatcher {
  /** The judge log reason of a System Error with an unexpected cause. */
  static final String INTERNAL_ERROR_REASON =
      "An internal error occurred in the judger while judging this submission.";

  /** The judge log reason of a System Error after interrupted judgings, by their number. */
  static final String INTERRUPTED_REASON =
      "The judging of this submission was interrupted %d times: the judger stopped, crashed or"
          + " lost the database while judging it.";

  /** How long (ms) the lease of a judging lasts; it is renewed every quarter of it. */
  static final long LEASE_TIMEOUT_MS = 60000;

  /** How many judgings of a submission may be interrupted before it is judged a System Error. */
  public static final int MAX_INTERRUPTIONS = 3;

  /** How long (ms) an idle judger waits before it looks for a pending submission again. */
  static final long POLL_INTERVAL_MS = 2000;

  /** After how many failures of its own in a row a judger is not ready. */
  static final int MAX_LOCAL_FAILURES = 3;

  /** How long (ms) a judger pauses after a failure of its own; doubles with each in a row. */
  static final long FIRST_BACKOFF_MS = 5000;

  /** The longest pause (ms) of a judger after failures of its own. */
  static final long MAX_BACKOFF_MS = 120000;

  /** How long (ms) a judger leaves a problem whose test data it could not get to other judgers. */
  static final long PROBLEM_SKIP_MS = 120000;

  /** The exit status of a judger that stops after an Error (e.g. it ran out of memory). */
  static final int FATAL_ERROR_EXIT_STATUS = 70;

  /**
   * Judges the pending submissions in some languages, one after the other, until the thread is
   * interrupted. When there is none, or none can be taken (e.g. the database is unreachable), it
   * looks again every {@value #POLL_INTERVAL_MS} ms; after a failure of its own, it pauses (see
   * {@link #getBackoff}).
   *
   * <p>An Error (e.g. an OutOfMemoryError, or a StackOverflowError) leaves the judger in no state
   * to judge: it is logged and the process exits (status {@value #FATAL_ERROR_EXIT_STATUS}), so
   * that its supervisor (e.g. Docker's restart policy) starts it again. The submission it was
   * judging is held until its lease expired, and is then counted an interrupted judging.
   *
   * @param languageIds - the unique identifiers of the languages this judger supports, not empty
   */
  public void judgeSubmissions(List<Integer> languageIds) {
    isJudging = true;
    try {
      while (!Thread.currentThread().isInterrupted()) {
        long pause;
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
        if (pause > 0) {
          try {
            Thread.sleep(pause);
          } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
          }
        }
      }
    } finally {
      isJudging = false;
    }
  }

  /**
   * Stops the judger after an Error, which leaves it in no state to judge: logs it and exits the
   * process, so that its supervisor starts it again.
   *
   * @param error - the error
   */
  void onFatalError(Error error) {
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
   * Claims the next pending submission in some languages (the one queued first) and judges it. Only
   * one judging runs at a time.
   *
   * <p>A judging renews its lease in the background while it runs (see {@link Lease}). A judging
   * that cannot be completed for a reason of this judger's own (see {@link LocalFailure}) releases
   * the submission at once for another judger; one that cannot record its outcome as the database
   * is unreachable releases it if it can, or else leaves it to the lease (which is not counted as a
   * failure of this judger's own: its heartbeats cannot be recorded either meanwhile). A submission whose judgings
   * were interrupted {@value #MAX_INTERRUPTIONS} times is judged a System Error instead.
   *
   * @param languageIds - the unique identifiers of the languages this judger supports, not empty
   * @return whether a submission was claimed
   * @throws DataAccessException if no submission can be claimed as the database is unreachable
   */
  public boolean judgeNextSubmission(List<Integer> languageIds) {
    synchronized (this) {
      // In this judger's own directory, so that a judger that died while judging leaves nothing
      // behind for good (see CheckpointStore#getWorkDirectory). A judger without one claims nothing.
      Path workDirectory;
      try {
        workDirectory = checkpointStore.getWorkDirectory();
      } catch (IOException ex) {
        onLocalFailure(new LocalFailure(null, "This judger has no usable work directory.", ex));
        return false;
      }
      Submission submission =
          applicationDispatcher.claimSubmission(
              languageIds, getSkippedProblemIds(), getLeaseSeconds());
      if (submission == null) {
        return false;
      }
      long submissionId = submission.getSubmissionId();
      if (submission.getInterruptions() >= maxInterruptions) {
        LOGGER.error(
            String.format(
                "The judgings of submission #%d were interrupted %d times: System Error.",
                submissionId, submission.getInterruptions()));
        applicationDispatcher.onErrorOccurred(
            submission, String.format(INTERRUPTED_REASON, submission.getInterruptions()));
        return true;
      }
      // Unique per judging, in case the directory of a judging of the same submission could not be
      // removed.
      String baseDirectory =
          workDirectory
              .resolve(
                  String.format(
                      "voj-%d-%s",
                      submissionId,
                      DigestUtils.getRandomString(12, DigestUtils.Mode.ALPHANUMERIC)))
              .toString();
      String baseFileName = DigestUtils.getRandomString(12, DigestUtils.Mode.ALPHA);
      try (Lease lease = new Lease(submission)) {
        currentLease = lease;
        judge(submission, lease, baseDirectory, baseFileName);
        onJudged();
      } catch (LocalFailure ex) {
        release(submission);
        onLocalFailure(ex);
      } catch (DataAccessException ex) {
        if (!ApplicationDispatcher.isUnreachable(ex)) {
          // The database refused what was to be recorded, which may be the submission's doing:
          // judged like any unexpected failure, rather than by every judger in turn.
          LOGGER.error(String.format("Failed to judge submission #%d", submissionId), ex);
          applicationDispatcher.onErrorOccurred(submission, INTERNAL_ERROR_REASON);
        } else {
          // Neither the verdict nor a System Error can be recorded now.
          LOGGER.error(
              String.format(
                  "Failed to record the outcome of submission #%d: it is released for another"
                      + " judging, or taken again once the lease of this one expired.",
                  submissionId),
              ex);
          release(submission);
        }
      } catch (Superseded ex) {
        LOGGER.info(
            String.format(
                "Stopped judging submission #%d: it was rejudged, or claimed by another judging"
                    + " meanwhile.",
                submissionId));
      } catch (SystemError ex) {
        LOGGER.error(String.format("System Error in submission #%d", submissionId), ex);
        applicationDispatcher.onErrorOccurred(submission, ex.getMessage());
        onJudged();
      } catch (Exception ex) {
        LOGGER.error(String.format("Failed to judge submission #%d", submissionId), ex);
        applicationDispatcher.onErrorOccurred(submission, INTERNAL_ERROR_REASON);
      } finally {
        currentLease = null;
        // Always remove the work directory (source, binary and program output), even when a stage
        // throws; otherwise it lingers in a shared location, readable by later submissions that
        // run as the same sandbox user.
        cleanUp(baseDirectory);
      }
      return true;
    }
  }

  /**
   * Tells whether this judger takes submissions: it judges, fewer than {@value
   * #MAX_LOCAL_FAILURES} of its judgings in a row failed for a reason of its own, and its judging
   * (if any) has not run past the time the limits of its submission allow. Recorded with its
   * heartbeat, for the web application, which judges a System Error the pending submissions in a
   * language that no ready judger supports for a while.
   *
   * @return whether this judger takes submissions
   */
  public boolean isReady() {
    Lease lease = currentLease;
    return isJudging && localFailures < maxLocalFailures && (lease == null || !lease.isOverdue);
  }

  /** Records a judging that reached an outcome: this judger's failures in a row are over. */
  private void onJudged() {
    if (localFailures >= maxLocalFailures) {
      LOGGER.info("This judger judged a submission again: it is ready.");
    }
    localFailures = 0;
  }

  /**
   * Records a failure of this judger's own: pauses the judger (see {@link #getBackoff}), leaves
   * the problem whose test data it could not get to the other judgers for a while, and logs it,
   * once the judger is not ready at most once until it is again.
   *
   * @param failure - the failure
   */
  private void onLocalFailure(LocalFailure failure) {
    int failures = ++localFailures;
    if (failure.problemId != null) {
      skippedProblems.put(failure.problemId, System.nanoTime() + problemSkipTime * 1_000_000);
    }
    if (failures < maxLocalFailures) {
      LOGGER.warn(failure.getMessage() + " It is left to another judger.", failure.getCause());
    } else if (failures == maxLocalFailures) {
      LOGGER.error(
          String.format(
              "%d judgings in a row failed for reasons of this judger's own, the last one: %s"
                  + " This judger is not ready until it judged a submission again, which it tries"
                  + " at most every %d s.",
              failures, failure.getMessage(), maxBackoff / 1000),
          failure.getCause());
    } else {
      LOGGER.debug(failure.getMessage(), failure.getCause());
    }
  }

  /**
   * Gets how long this judger pauses after its failures in a row: {@value #FIRST_BACKOFF_MS} ms
   * after the first one, twice as long after each next one, at most {@value #MAX_BACKOFF_MS} ms.
   *
   * @return how long (ms) this judger pauses
   */
  long getBackoff() {
    int doublings = Math.min(localFailures - 1, 20);
    return Math.min(firstBackoff << Math.max(doublings, 0), maxBackoff);
  }

  /**
   * Gets the problems this judger could not get the test data of lately, which it leaves to the
   * other judgers for now.
   *
   * @return the unique identifiers of the problems
   */
  private List<Long> getSkippedProblemIds() {
    long now = System.nanoTime();
    skippedProblems.values().removeIf(until -> now - until >= 0);
    return new ArrayList<>(skippedProblems.keySet());
  }

  /**
   * Releases a submission this judging could not judge for another judging. If the database is
   * unreachable, the submission is taken again once the lease of this judging expired.
   *
   * @param submission - the submission, claimed by the judging
   */
  private void release(Submission submission) {
    try {
      applicationDispatcher.releaseSubmission(submission);
    } catch (DataAccessException ex) {
      LOGGER.warn(
          String.format(
              "Failed to release submission #%d: it is taken again once the lease of its judging"
                  + " expired.",
              submission.getSubmissionId()),
          ex);
    }
  }

  /**
   * Judges a submission: loads the problem's test data, compiles the submission and runs it against
   * every checkpoint. Note: the random file name is used to prevent the application from
   * recursively invoking itself.
   *
   * <p>Between its steps (the download of the test data, the compilation, the run of each
   * checkpoint), and before reporting one, the judging stops once it was superseded (see {@link
   * Lease}). A step itself runs to its end: its time is bounded by the sandbox's limits (e.g. at
   * most three times the time limit of a run, plus a second, of wall-clock time).
   *
   * @param submission - the submission object
   * @param lease - the lease of the judging
   * @param workDirectory - the directory of the compilation result and the program output
   * @param baseFileName - the random file name (without suffix)
   * @throws SystemError if the submission cannot be judged for a reason of its problem's
   * @throws LocalFailure if the submission cannot be judged for a reason of this judger's own
   * @throws Superseded if the judging was superseded
   * @throws Exception if the submission cannot be judged for an unexpected reason
   */
  private void judge(
      Submission submission, Lease lease, String workDirectory, String baseFileName)
      throws Exception {
    Path checkpointDirectory = syncCheckpoints(submission);
    List<Checkpoint> checkpoints = getCheckpoints(submission);
    lease.setDeadline(checkpoints.size(), getRunTimeLimit(submission));
    lease.check();
    try {
      preprocessor.createTestCode(submission, workDirectory, baseFileName);
    } catch (IOException | CreateDirectoryException ex) {
      throw new LocalFailure(null, "The source code could not be written to the disk.", ex);
    }
    if (compile(submission, lease, workDirectory, baseFileName)) {
      runProgram(submission, lease, checkpoints, checkpointDirectory, workDirectory, baseFileName);
    }
  }

  /**
   * Makes the problem's test data available locally.
   *
   * <p>Only a download whose answer is final for the problem (the web application has no test data
   * for it, or its archive is malformed) is a System Error. A download that the web application
   * refuses this judger (e.g. for a wrong API token) or that fails otherwise (the web application
   * restarting or overloaded, a network failure or timeout, this judger's own disk) is no fault of
   * the submission or its problem, and likely not one of another judger, so it is left to one.
   *
   * @param submission - the submission object
   * @return the directory holding the problem's test data
   * @throws SystemError if the problem has no usable test data
   * @throws LocalFailure if this judger cannot get the test data
   */
  private Path syncCheckpoints(Submission submission) throws SystemError, LocalFailure {
    long problemId = submission.getProblem().getProblemId();
    try {
      return checkpointStore.sync(submission.getProblem());
    } catch (InvalidCheckpointsException ex) {
      throw new SystemError("The test data of this problem could not be loaded.", ex);
    } catch (CheckpointsRefusedException ex) {
      int statusCode = ex.getStatusCode();
      String reason =
          String.format(
              "The web application refused this judger the test data of problem #%d (HTTP %d).",
              problemId, statusCode);
      if (statusCode == 401 || statusCode == 403) {
        reason +=
            " Check that the judger's judger.apiToken (VOJ_JUDGER_API_TOKEN) is the web"
                + " application's.";
      }
      throw new LocalFailure(problemId, reason, ex);
    } catch (IOException ex) {
      throw new LocalFailure(
          problemId,
          String.format("The test data of problem #%d could not be downloaded.", problemId),
          ex);
    }
  }

  /**
   * Reads the problem's checkpoints. There is nothing to judge a submission against without them,
   * so a problem without checkpoints is a System Error: every submission that compiles would be
   * Accepted otherwise.
   *
   * @param submission - the submission object
   * @return the problem's checkpoints (never empty)
   * @throws SystemError if there is no checkpoint
   * @throws LocalFailure if this judger cannot read its copy of the test data
   */
  private List<Checkpoint> getCheckpoints(Submission submission)
      throws SystemError, LocalFailure {
    long problemId = submission.getProblem().getProblemId();
    List<Checkpoint> checkpoints;
    try {
      checkpoints = checkpointStore.getCheckpoints(problemId);
    } catch (IOException ex) {
      throw new LocalFailure(
          problemId,
          String.format("This judger could not read the test data of problem #%d.", problemId),
          ex);
    }
    if (checkpoints.isEmpty()) {
      throw new SystemError("This problem has no test data (checkpoints) to judge against.", null);
    }
    return checkpoints;
  }

  /**
   * Gets the time limit of a run of a submission, as the program runner applies it: the problem's,
   * scaled by the language's time multiplier.
   *
   * @param submission - the submission object
   * @return the time limit of a run, in milliseconds
   */
  private static long getRunTimeLimit(Submission submission) {
    Language language = submission.getLanguage();
    double multiplier = language == null ? 1.0 : language.getTimeMultiplier();
    return (long) Math.ceil(submission.getProblem().getTimeLimit() * Math.max(multiplier, 1.0));
  }

  /**
   * Creates a compilation task. Note: the random file name is used to prevent the application from
   * recursively invoking itself.
   *
   * @param submission - the submission object
   * @param lease - the lease of the judging
   * @param workDirectory - the directory used for producing compilation output
   * @param baseFileName - the random file name (without suffix)
   * @return whether the compilation succeeded
   * @throws LocalFailure if the sandbox failed to run the compiler
   * @throws Superseded if the judging was superseded
   */
  private boolean compile(
      Submission submission, Lease lease, String workDirectory, String baseFileName)
      throws LocalFailure, Superseded {
    Map<String, Object> result = compiler.getCompileResult(submission, workDirectory, baseFileName);
    if (result.get("verdict") == Verdict.INTERNAL_ERROR) {
      throw new LocalFailure(null, "The sandbox of this judger failed to run the compiler.", null);
    }
    lease.check();
    applicationDispatcher.onCompileFinished(submission, result);
    return (Boolean) result.get("isSuccessful");
  }

  /**
   * Runs the program.
   *
   * @param submission - the submission object
   * @param lease - the lease of the judging
   * @param checkpoints - the problem's checkpoints
   * @param checkpointDirectory - the directory holding the problem's test data
   * @param workDirectory - the directory of the compilation result and the program output
   * @param baseFileName - the file name of the application to run (without file suffix)
   * @throws LocalFailure if the sandbox failed to run the program, or the test data cannot be read
   * @throws Superseded if the judging was superseded
   */
  private void runProgram(
      Submission submission,
      Lease lease,
      List<Checkpoint> checkpoints,
      Path checkpointDirectory,
      String workDirectory,
      String baseFileName)
      throws LocalFailure, Superseded {
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
              submission,
              programRunner.getRuntimeResult(
                  submission, workDirectory, baseFileName, inputFilePath, outputFilePath),
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
   * <p>The renewals stop too once the judging runs past its deadline, the longest a judging of the
   * submission can take: the download of its test data, its compilation and its runs, each bounded
   * by its timeout or the sandbox's wall-clock limit (three times the time limit, plus a second),
   * plus a lease timeout. A judging past it is stuck (e.g. on a connection that went silent): the
   * submission is taken by another judger once its lease expired, and this judger is not ready
   * meanwhile (see {@link #isReady}).
   */
  final class Lease implements AutoCloseable {
    /**
     * Starts renewing the lease of a judging. Until its checkpoints are known (see {@link
     * #setDeadline}), the deadline of the judging allows for the download of its test data.
     *
     * @param submission - the submission, claimed by the judging
     */
    Lease(Submission submission) {
      this.submission = submission;
      this.startTime = System.nanoTime();
      this.deadline = startTime + (downloadTimeout * 1000 + leaseTimeout) * 1_000_000;
      long renewalInterval = Math.max(1, leaseTimeout / 4);
      renewal =
          leaseRenewer.scheduleWithFixedDelay(
              this::renew, renewalInterval, renewalInterval, TimeUnit.MILLISECONDS);
    }

    /**
     * Sets the deadline of the judging, from the limits of its steps.
     *
     * @param numberOfCheckpoints - the number of checkpoints of the problem
     * @param runTimeLimit - the time limit (ms) of a run of the submission
     */
    void setDeadline(int numberOfCheckpoints, long runTimeLimit) {
      long budget =
          downloadTimeout * 1000
              + getWallTimeLimit(compileTimeLimit)
              + numberOfCheckpoints * getWallTimeLimit(runTimeLimit)
              + leaseTimeout;
      deadline = startTime + budget * 1_000_000;
    }

    /** Renews the lease; a failure (e.g. the database is unreachable) is retried next time. */
    private void renew() {
      if (System.nanoTime() - deadline > 0) {
        isOverdue = true;
        renewal.cancel(false);
        LOGGER.error(
            String.format(
                "The judging of submission #%d still runs %d s after it started, longer than the"
                    + " limits of the submission allow: its lease is no longer renewed, so that"
                    + " another judger takes it, and this judger is not ready until the judging is"
                    + " over.",
                submission.getSubmissionId(),
                (System.nanoTime() - startTime) / 1_000_000_000));
        return;
      }
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

    /** When the judging started, in {@link System#nanoTime()} terms. */
    private final long startTime;

    /** The deadline of the judging, in {@link System#nanoTime()} terms. */
    private volatile long deadline;

    /** The scheduled renewals. */
    private final ScheduledFuture<?> renewal;

    /** Whether a renewal found the judging superseded. */
    private volatile boolean isSuperseded;

    /** Whether the judging ran past its deadline. */
    private volatile boolean isOverdue;
  }

  /**
   * Gets the longest a step of a judging with a time limit may take: the sandbox stops it after
   * three times its time limit, plus a second, of wall-clock time.
   *
   * @param timeLimit - the time limit (ms) of the step
   * @return its wall-clock limit (ms)
   */
  private static long getWallTimeLimit(long timeLimit) {
    return timeLimit * 3 + 1000;
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
   * @param submission - the submission object
   * @param result - the Map object containing the program's runtime result
   * @param standardOutputFilePath - the path of the standard output file
   * @param outputFilePath - the path of the user's output file
   * @param exactlyMatch - whether the checkpoint requires an exact match
   * @return the Map object containing the program's runtime result
   * @throws LocalFailure if the sandbox failed to run the program, or the standard output cannot be
   *     read
   */
  private Map<String, Object> getRuntimeResult(
      Submission submission,
      Map<String, Object> result,
      String standardOutputFilePath,
      String outputFilePath,
      boolean exactlyMatch)
      throws LocalFailure {
    String runtimeResultSlug = (String) result.get("runtimeResult");
    int usedTime = (Integer) result.get("usedTime");
    int usedMemory = (Integer) result.get("usedMemory");

    if ("SE".equals(runtimeResultSlug)) {
      throw new LocalFailure(null, "The sandbox of this judger failed to run the program.", null);
    }
    if (runtimeResultSlug.equals("AC")) {
      long problemId = submission.getProblem().getProblemId();
      try {
        runtimeResultSlug =
            comparator.getComparisonResult(standardOutputFilePath, outputFilePath, exactlyMatch);
      } catch (IOException ex) {
        // The comparator judges an output it may not read (e.g. a planted link) a Wrong Answer
        // itself, so this is the judger failing to read its copy of the test data.
        throw new LocalFailure(
            problemId,
            String.format("This judger could not read the test data of problem #%d.", problemId),
            ex);
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
   * A judging that cannot be completed for a known reason of the submission's problem. Its message
   * is shown to the user in the judge log of the resulting System Error.
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
   * A judging that cannot be completed for a reason of this judger's own, which another judger
   * likely does not have: its work directory or its copy of the test data cannot be used, the
   * test data cannot be downloaded (the web application refuses this judger, or cannot be
   * reached), or its sandbox cannot run the compiler or the program. Nothing is recorded on the
   * submission: it is released for another judger.
   */
  static final class LocalFailure extends Exception {
    /**
     * Constructor.
     *
     * @param problemId - the unique identifier of the problem whose test data this judger cannot
     *     get, or {@code null} if the failure is not one of the test data
     * @param reason - the reason, logged
     * @param cause - the underlying exception, or {@code null}
     */
    LocalFailure(Long problemId, String reason, Throwable cause) {
      super(reason, cause);
      this.problemId = problemId;
    }

    /** The problem whose test data this judger cannot get, or {@code null}. */
    private final Long problemId;

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

  /** How long (seconds) a download of a problem's test data may take. */
  @Value("${judger.web.downloadTimeout:300}")
  private long downloadTimeout;

  /** The time limit (ms) of a compilation. */
  @Value("${judger.compileTimeLimit:20000}")
  private long compileTimeLimit;

  /*
   * The policy of the judging queue: the same on every judger, so not configurable. Fields rather
   * than constants only so that the tests can shorten the times.
   */

  /** How long (ms) an idle judger waits before it looks for a submission again. */
  private long pollInterval = POLL_INTERVAL_MS;

  /** How long (ms) the lease of a judging lasts; it is renewed every quarter of it. */
  private long leaseTimeout = LEASE_TIMEOUT_MS;

  /** How many judgings of a submission may be interrupted before it is a System Error. */
  private int maxInterruptions = MAX_INTERRUPTIONS;

  /** After how many failures of its own in a row this judger is not ready. */
  private int maxLocalFailures = MAX_LOCAL_FAILURES;

  /** How long (ms) this judger pauses after a failure of its own. */
  private long firstBackoff = FIRST_BACKOFF_MS;

  /** The longest pause (ms) of this judger after failures of its own. */
  private long maxBackoff = MAX_BACKOFF_MS;

  /** How long (ms) this judger leaves a problem whose test data it could not get to others. */
  private long problemSkipTime = PROBLEM_SKIP_MS;

  /** Exits the process with a status; replaced by the tests. */
  private IntConsumer processExit = System::exit;

  /** Whether the judging loop runs. */
  private volatile boolean isJudging;

  /** How many judgings (or attempts to judge) in a row failed for a reason of this judger's own. */
  private volatile int localFailures;

  /** Whether the last attempt to take a submission failed (e.g. the database is unreachable). */
  private boolean isClaimFailing;

  /** The problems this judger could not get the test data of, and until when it skips them. */
  private final Map<Long, Long> skippedProblems = new HashMap<>();

  /** The lease of the current judging, or {@code null}. */
  private volatile Lease currentLease;

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
