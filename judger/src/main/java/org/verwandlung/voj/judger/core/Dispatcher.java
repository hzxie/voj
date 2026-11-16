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
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Component;

import org.verwandlung.voj.judger.application.ApplicationDispatcher;
import org.verwandlung.voj.judger.exception.CheckpointsRefusedException;
import org.verwandlung.voj.judger.exception.InvalidCheckpointsException;
import org.verwandlung.voj.judger.exception.RetryableJudgingException;
import org.verwandlung.voj.judger.model.Checkpoint;
import org.verwandlung.voj.judger.model.Submission;
import org.verwandlung.voj.judger.util.DigestUtils;
import org.verwandlung.voj.judger.util.FileSystemUtils;

/**
 * The judger dispatcher. Takes the pending submissions from the database, one at a time, and drives
 * their judging. After each stage finishes, a message is pushed to the message queue; after judging
 * completes, the result is written to the database.
 *
 * <p>A judging that cannot be completed for a reason other than the submission itself (no test
 * data, a sandbox failure, unreadable test data, ...) is judged a System Error, never a verdict
 * that blames the submission.
 *
 * @author Haozhe Xie
 */
@Component
public class Dispatcher implements InitializingBean {
  /** The judge log reason of a System Error with an unexpected cause. */
  static final String INTERNAL_ERROR_REASON =
      "An internal error occurred in the judger while judging this submission.";

  /** The judge log reason of a System Error after a judging that stopped without an outcome. */
  static final String ABANDONED_REASON =
      "A judger took this submission but stopped judging it: the judger stopped, or could not"
          + " reach the database.";

  /**
   * Refuses a configuration of the judging queue that could not work: a lease, a poll interval or a
   * number of attempts that is not positive.
   */
  @Override
  public void afterPropertiesSet() {
    if (pollInterval <= 0 || leaseTimeout <= 0 || maxAttempts <= 0) {
      throw new IllegalArgumentException(
          "judging.pollInterval, judging.leaseTimeout and judging.maxAttempts must be positive");
    }
  }

  /**
   * Judges the pending submissions in some languages, one after the other, until the thread is
   * interrupted. When there is none, or it cannot be taken (e.g. the database is unreachable), it
   * looks again every {@code judging.pollInterval} ms.
   *
   * @param languageIds - the unique identifiers of the languages this judger supports, not empty
   */
  public void judgeSubmissions(List<Integer> languageIds) {
    while (!Thread.currentThread().isInterrupted()) {
      boolean isJudged = false;
      try {
        isJudged = judgeNextSubmission(languageIds);
      } catch (RuntimeException ex) {
        LOGGER.error("Failed to take a submission to judge.", ex);
      }
      if (!isJudged) {
        try {
          Thread.sleep(pollInterval);
        } catch (InterruptedException ex) {
          Thread.currentThread().interrupt();
        }
      }
    }
  }

  /**
   * Claims the next pending submission in some languages (the one queued first) and judges it. Only
   * one judging runs at a time.
   *
   * <p>A judging renews its lease in the background while it runs (see {@link Lease}). Each claim
   * counts as an attempt, and a submission gets at most {@code judging.maxAttempts} of them. A
   * judging that cannot be completed now, but likely later (see {@link RetryableJudgingException}),
   * or whose outcome cannot be recorded (the database is unreachable), stops renewing its lease:
   * once the lease expired, the submission is claimed again, by any judger, which gives what failed
   * (e.g. the web application restarting) a lease timeout to recover. So is the submission of a
   * judger that dies while judging. On the last attempt, a judging that cannot be completed now
   * judges the submission a System Error with the actual reason instead; a submission whose last
   * attempt ended without an outcome is judged a System Error by the judging that claims it next.
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
        LOGGER.error("This judger has no usable work directory, so it takes no submission.", ex);
        return false;
      }
      Submission submission = applicationDispatcher.claimSubmission(languageIds, getLeaseSeconds());
      if (submission == null) {
        return false;
      }
      long submissionId = submission.getSubmissionId();
      if (submission.getJudgeAttempts() > maxAttempts) {
        LOGGER.error(
            String.format(
                "The last attempt to judge submission #%d stopped without an outcome: System"
                    + " Error.",
                submissionId));
        applicationDispatcher.onErrorOccurred(submission, getLastAttemptReason(ABANDONED_REASON));
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
        judge(submission, lease, baseDirectory, baseFileName);
      } catch (DataAccessException ex) {
        // Neither the verdict nor a System Error can be recorded.
        LOGGER.error(
            String.format(
                "Failed to record the outcome of submission #%d; it is judged again once the lease"
                    + " of this judging expired.",
                submissionId),
            ex);
      } catch (RetryableJudgingException ex) {
        if (submission.getJudgeAttempts() < maxAttempts) {
          LOGGER.warn(
              String.format(
                  "Cannot judge submission #%d now; it is judged again once the lease of this"
                      + " judging expired.",
                  submissionId),
              ex);
        } else {
          LOGGER.error(
              String.format(
                  "Cannot judge submission #%d on its last attempt: System Error.", submissionId),
              ex);
          applicationDispatcher.onErrorOccurred(
              submission, getLastAttemptReason(ex.getMessage()));
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
      } catch (Exception ex) {
        LOGGER.error(String.format("Failed to judge submission #%d", submissionId), ex);
        applicationDispatcher.onErrorOccurred(submission, INTERNAL_ERROR_REASON);
      } finally {
        // Always remove the work directory (source, binary and program output), even when a stage
        // throws; otherwise it lingers in a shared location, readable by later submissions that
        // run as the same sandbox user.
        cleanUp(baseDirectory);
      }
      return true;
    }
  }

  /**
   * Gets the judge log reason of a submission that was given up on after its last attempt.
   *
   * @param reason - why its last attempt failed
   * @return the reason, with the number of attempts
   */
  private String getLastAttemptReason(String reason) {
    return String.format(
        "%s\n\nThe judgers gave up on this submission after %d attempts to judge it.",
        reason, maxAttempts);
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
   * @throws SystemError if the submission cannot be judged for a known reason
   * @throws Superseded if the judging was superseded
   * @throws Exception if the submission cannot be judged for an unexpected reason
   */
  private void judge(
      Submission submission, Lease lease, String workDirectory, String baseFileName)
      throws Exception {
    Path checkpointDirectory = syncCheckpoints(submission);
    List<Checkpoint> checkpoints = getCheckpoints(submission);
    lease.check();
    preprocessor.createTestCode(submission, workDirectory, baseFileName);
    if (compile(submission, lease, workDirectory, baseFileName)) {
      runProgram(submission, lease, checkpoints, checkpointDirectory, workDirectory, baseFileName);
    }
  }

  /**
   * Makes the problem's test data available locally.
   *
   * <p>Only a download whose answer is final (the problem has no test data, its archive is
   * malformed, or the web application refuses this judger, e.g. for a wrong API token) is a System
   * Error. Any other failure (the web application restarting or overloaded, a network failure or
   * timeout, this judger's own disk) is no fault of the submission or its problem and likely gone
   * later or on another judger, so the submission is judged again instead.
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
   * @param lease - the lease of the judging
   * @param workDirectory - the directory used for producing compilation output
   * @param baseFileName - the random file name (without suffix)
   * @return whether the compilation succeeded
   * @throws SystemError if the sandbox failed to run the compiler
   * @throws Superseded if the judging was superseded
   */
  private boolean compile(
      Submission submission, Lease lease, String workDirectory, String baseFileName)
      throws SystemError, Superseded {
    Map<String, Object> result = compiler.getCompileResult(submission, workDirectory, baseFileName);
    if (result.get("verdict") == Verdict.INTERNAL_ERROR) {
      throw new SystemError("The sandbox failed to run the compiler.", null);
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
   * @throws SystemError if the test data cannot be read
   * @throws Superseded if the judging was superseded
   */
  private void runProgram(
      Submission submission,
      Lease lease,
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
      lease.check();
      applicationDispatcher.onOneTestPointFinished(submission, checkpointId, runtimeResult);
    }
    applicationDispatcher.onAllTestPointsFinished(submission, runtimeResults);
  }

  /**
   * The lease of a judging: renewed in the background every quarter of {@code
   * judging.leaseTimeout} while the judging runs, however long its steps take, so that no other
   * judging claims the submission meanwhile. A renewal that finds the judging superseded (the
   * submission was rejudged, or claimed by another judging as the lease had expired, e.g. while the
   * database was unreachable) marks it so, and the judging stops at its next step (see {@link
   * #check}). Closing the lease stops the renewals; the lease then expires, which matters unless
   * the judging recorded its outcome.
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

    /** Renews the lease; a failure (e.g. the database is unreachable) is retried next time. */
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

  /** How long (ms) a judger waits before it looks for a submission again when there was none. */
  @Value("${judging.pollInterval:2000}")
  private long pollInterval;

  /** How long (ms) the lease of a judging lasts; it is renewed every quarter of it. */
  @Value("${judging.leaseTimeout:60000}")
  private long leaseTimeout;

  /** How many judgings may take a submission before it is judged a System Error. */
  @Value("${judging.maxAttempts:3}")
  private int maxAttempts;

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
