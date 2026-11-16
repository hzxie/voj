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

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.IntConsumer;
import java.util.function.Supplier;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Component;

import org.verwandlung.voj.judger.application.ApplicationDispatcher;
import org.verwandlung.voj.judger.model.Submission;

/**
 * The judger dispatcher: takes the pending submissions from the database, one at a time, has them
 * judged (see {@link Judge}) under a lease, and accounts for the judgings that fail.
 *
 * <p>A judging either reaches an outcome or fails. The outcomes are the verdicts of the submission
 * (e.g. a Compile Error or a Wrong Answer), and a System Error when the web application says that
 * its problem has no test data. Every other way a judging can end is a failed judging (see {@link
 * FailedJudging}), of one of two kinds:
 *
 * <ul>
 *   <li>One the submission may have caused: the sandbox failed while running its compiler or its
 *       program, its verdict was refused by the database, an unexpected exception (a bug it may
 *       trigger), or (see {@link RenewedLease}) its judger stopped while judging it. The
 *       submission is released to the end of the queue, so that it holds no other submission up,
 *       and the failed judging is counted on it, with its reason. The judger that claims it after
 *       {@value #MAX_FAILED_JUDGINGS} failed judgings judges it a System Error that gives the
 *       reason of the last one.
 *   <li>One of the judger or its environment: the test data cannot be downloaded or read, the
 *       source code cannot be written, the database is unreachable. The submission is released to
 *       the end of the queue without counting anything on it, so that an outage of the web
 *       application or the database does not use up the budget of a submission, and a submission
 *       that fails this way every time (e.g. its test data is broken) holds no other one up.
 * </ul>
 *
 * <p>Only a judging that stops as its judger shuts down releases its submission at its place in the
 * queue (see {@link #destroy}).
 *
 * <p>Either kind counts on the judger: after a failed judging it pauses (see {@link #getPause}),
 * and after {@value #MAX_FAILURES_IN_A_ROW} in a row it is not ready (see {@link #isReady}) until a
 * judging of it reaches an outcome again. The web application judges a System Error the pending
 * submissions in a language that no ready judger supports for a while, which bounds the failures
 * of the second kind. The count lives in memory: a judger that crashes is restarted by its
 * supervisor, which bounds how often, and the judging it held counts on its submission once its
 * lease expired.
 *
 * @author Haozhe Xie
 */
@Component
public class Dispatcher implements DisposableBean {
  /** The judge log reason of a failed judging with an unexpected cause. */
  static final String INTERNAL_ERROR_REASON =
      "An internal error occurred in the judger while judging this submission.";

  /**
   * The judge log reason of a System Error after too many failed judgings, by their number and the
   * reason of the last one.
   */
  static final String GIVEN_UP_REASON =
      "%d judgings of this submission failed, the last one as follows: %s";

  /** How many judgings of a submission may fail before it is judged a System Error. */
  public static final int MAX_FAILED_JUDGINGS = 5;

  /** After how many failed judgings in a row a judger is not ready. */
  public static final int MAX_FAILURES_IN_A_ROW = 3;

  /** The exit status of a judger that stops after an Error (e.g. it ran out of memory). */
  static final int FATAL_ERROR_EXIT_STATUS = 70;

  /**
   * The times of the judging queue, the same on every judger.
   *
   * @param pollIntervalMs - how long an idle judger waits before it looks for a submission again
   * @param leaseTimeoutMs - how long the lease of a judging lasts; it is renewed every quarter of it
   * @param firstPauseMs - how long a judger pauses after a failed judging; doubles with each one in
   *     a row
   * @param maxPauseMs - the longest pause of a judger after failed judgings
   */
  public record Times(
      long pollIntervalMs, long leaseTimeoutMs, long firstPauseMs, long maxPauseMs) {
    /** The times of a judger: 2 s, 60 s, 5 s and a minute. */
    public static final Times DEFAULT = new Times(2000, 60000, 5000, 60000);
  }

  /**
   * Creates the dispatcher of a judger.
   *
   * @param applicationDispatcher - claims the submissions in the database
   * @param judge - judges a claimed submission
   */
  @Autowired
  public Dispatcher(ApplicationDispatcher applicationDispatcher, Judge judge) {
    this(applicationDispatcher, judge, Times.DEFAULT, System::exit);
  }

  /**
   * Creates a dispatcher.
   *
   * @param applicationDispatcher - claims the submissions in the database
   * @param judge - judges a claimed submission
   * @param times - the times of the judging queue
   * @param processExit - exits the process with a status (see {@link #onFatalError})
   */
  public Dispatcher(
      ApplicationDispatcher applicationDispatcher,
      Judge judge,
      Times times,
      IntConsumer processExit) {
    this.applicationDispatcher = applicationDispatcher;
    this.judge = judge;
    this.times = times;
    this.processExit = processExit;
  }

  /**
   * Judges the pending submissions in the languages this judger supports, one after the other,
   * until the thread is interrupted or the judger stops. When there is none, or none can be taken
   * (e.g. the database is unreachable, or this judger supports no language for now), it looks again
   * after the poll interval; after a failed judging, it pauses (see {@link #getPause}).
   *
   * <p>An Error (e.g. an OutOfMemoryError, or a StackOverflowError) leaves the judger in no state
   * to judge: see {@link #onFatalError}.
   *
   * @param languageIds - gives the unique identifiers of the languages this judger supports now
   */
  public void judgeSubmissions(Supplier<List<Integer>> languageIds) {
    isJudging = true;
    try {
      while (!Thread.currentThread().isInterrupted() && !isStopping) {
        long pause;
        try {
          List<Integer> languages = languageIds.get();
          boolean isClaimed = !languages.isEmpty() && judgeNextSubmission(languages);
          if (isClaimFailing) {
            isClaimFailing = false;
            LOGGER.info("This judger takes submissions from the database again.");
          }
          pause = !isClaimed ? times.pollIntervalMs() : failuresInARow > 0 ? getPause() : 0;
        } catch (RuntimeException ex) {
          if (!isClaimFailing) {
            isClaimFailing = true;
            LOGGER.error(
                String.format(
                    "Failed to take a submission to judge (is the database reachable?); trying"
                        + " again every %d ms.",
                    times.pollIntervalMs()),
                ex);
          }
          pause = times.pollIntervalMs();
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
   * process (status {@value #FATAL_ERROR_EXIT_STATUS}), so that its supervisor (e.g. Docker's
   * restart policy) starts it again. The submission it was judging is neither released nor judged
   * (the judging may be in any state): it is held until its lease expired, and the judging is then
   * counted as a failed one on it.
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
   * in the queue, without counting the judging as a failed one, so that another judger takes it at
   * once. The judging itself runs on until the process exits, but cannot record anything any more.
   * Nothing is released after a fatal error (see {@link #onFatalError}).
   *
   * <p>A claim in progress ends first (see {@link #claimLock}): then either it took nothing, or
   * the submission it took is the one released here.
   */
  @Override
  public void destroy() {
    Submission submission;
    synchronized (claimLock) {
      if (isStopping) {
        return;
      }
      isStopping = true;
      submission = claimedSubmission;
    }
    if (submission == null) {
      return;
    }
    try {
      if (applicationDispatcher.handBackSubmission(submission)) {
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
   * <p>A judging renews its lease in the background while it runs (see {@link RenewedLease}). A
   * judging that fails releases the submission (see {@link Dispatcher}); a submission with {@value
   * #MAX_FAILED_JUDGINGS} failed judgings is judged a System Error instead.
   *
   * @param languageIds - the unique identifiers of the languages this judger supports, not empty
   * @return whether a submission was claimed
   * @throws DataAccessException if no submission can be claimed as the database is unreachable
   */
  public boolean judgeNextSubmission(List<Integer> languageIds) {
    Submission submission;
    synchronized (claimLock) {
      if (isStopping) {
        return false;
      }
      submission = applicationDispatcher.claimSubmission(languageIds, getLeaseSeconds());
      if (submission == null) {
        return false;
      }
      claimedSubmission = submission;
    }
    try {
      judgeSubmission(submission);
    } finally {
      claimedSubmission = null;
    }
    return true;
  }

  /**
   * Judges a submission this judger claimed, or judges it a System Error after too many failed
   * judgings, and counts the outcome on this judger.
   *
   * @param submission - the submission, claimed by this judger
   */
  private void judgeSubmission(Submission submission) {
    try (RenewedLease lease = new RenewedLease(submission)) {
      if (submission.getFailedJudgings() >= MAX_FAILED_JUDGINGS) {
        // Neither an outcome of this judger's judging nor a failure of it.
        giveUp(submission);
        return;
      }
      judge.judge(submission, lease);
      onJudged();
    } catch (FailedJudging ex) {
      onFailedJudging(submission, ex);
    } catch (Superseded ex) {
      LOGGER.info(
          String.format(
              "Stopped judging submission #%d: it was rejudged, or claimed by another judging"
                  + " meanwhile.",
              submission.getSubmissionId()));
    } catch (DataAccessException ex) {
      String reason = "The outcome of the judging could not be recorded in the database.";
      onFailedJudging(
          submission,
          ApplicationDispatcher.isUnreachable(ex)
              ? FailedJudging.ofJudger(reason, ex)
              : FailedJudging.ofSubmission(reason, ex));
    } catch (RuntimeException ex) {
      onFailedJudging(submission, FailedJudging.ofSubmission(INTERNAL_ERROR_REASON, ex));
    }
  }

  /**
   * Judges a submission with {@value #MAX_FAILED_JUDGINGS} failed judgings a System Error that
   * gives their number and the reason of the last one.
   *
   * @param submission - the submission, claimed by this judger
   * @throws Superseded if the judging was superseded
   */
  private void giveUp(Submission submission) throws Superseded {
    String reason =
        String.format(
            GIVEN_UP_REASON,
            submission.getFailedJudgings(),
            submission.getLastFailure() == null
                ? INTERNAL_ERROR_REASON
                : submission.getLastFailure());
    LOGGER.error(
        String.format("System Error in submission #%d: %s", submission.getSubmissionId(), reason));
    applicationDispatcher.onErrorOccurred(submission, reason);
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
    return isJudging && !isStopping && failuresInARow < MAX_FAILURES_IN_A_ROW;
  }

  /** Records a judging that reached an outcome: this judger's failed judgings in a row are over. */
  private void onJudged() {
    if (failuresInARow >= MAX_FAILURES_IN_A_ROW) {
      LOGGER.info("A judging of this judger reached an outcome again: it is ready.");
    }
    failuresInARow = 0;
  }

  /**
   * Records a failed judging (see {@link Dispatcher}): releases its submission to the end of the
   * queue, counting the failure on it if the submission may have caused it, and counts it on this
   * judger, which pauses (see {@link #getPause}).
   *
   * @param submission - the submission of the judging
   * @param failure - why the judging failed
   */
  private void onFailedJudging(Submission submission, FailedJudging failure) {
    int failures = ++failuresInARow;
    long submissionId = submission.getSubmissionId();
    try {
      if (failure.countsOnSubmission()) {
        applicationDispatcher.requeueSubmission(submission, failure.getMessage());
      } else {
        applicationDispatcher.releaseSubmission(submission);
      }
    } catch (DataAccessException ex) {
      LOGGER.warn(
          String.format(
              "Failed to release submission #%d: it is taken again once the lease of its"
                  + " judging expired.",
              submissionId),
          ex);
    }
    LOGGER.warn(
        failure.countsOnSubmission()
            ? String.format(
                "The judging of submission #%d failed (it is judged again later, or a System Error"
                    + " after %d failed judgings): %s",
                submissionId, MAX_FAILED_JUDGINGS, failure.getMessage())
            : String.format(
                "The judging of submission #%d failed, not for anything of the submission's (it"
                    + " is judged again later): %s",
                submissionId, failure.getMessage()),
        failure.getCause());
    if (failures == MAX_FAILURES_IN_A_ROW) {
      LOGGER.error(
          String.format(
              "%d judgings of this judger failed in a row: it is not ready until a judging of it"
                  + " reaches an outcome again, which it tries after a pause of %d s, twice as long"
                  + " after each failure more, at most %d s.",
              failures, getPause() / 1000, times.maxPauseMs() / 1000));
    }
  }

  /**
   * Gets how long this judger pauses after its failed judgings in a row: the first pause after the
   * first one, twice as long after each next one, at most the longest pause.
   *
   * @return how long (ms) this judger pauses
   */
  long getPause() {
    int doublings = Math.min(Math.max(failuresInARow - 1, 0), 20);
    return Math.min(times.firstPauseMs() << doublings, times.maxPauseMs());
  }

  /**
   * Gets how long the lease of a judging lasts, in seconds, rounded up.
   *
   * @return how long the lease lasts, in seconds
   */
  private long getLeaseSeconds() {
    return (times.leaseTimeoutMs() + 999) / 1000;
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
  private final class RenewedLease implements Lease, AutoCloseable {
    /**
     * Starts renewing the lease of a judging.
     *
     * @param submission - the submission, claimed by the judging
     */
    RenewedLease(Submission submission) {
      this.submission = submission;
      long renewalInterval = Math.max(1, times.leaseTimeoutMs() / 4);
      renewal =
          LEASE_RENEWER.scheduleWithFixedDelay(
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

    @Override
    public void check() throws Superseded {
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

  /** Claims the submissions, and records what becomes of them, in the database. */
  private final ApplicationDispatcher applicationDispatcher;

  /** Judges a claimed submission. */
  private final Judge judge;

  /** The times of the judging queue. */
  private final Times times;

  /** Exits the process with a status. */
  private final IntConsumer processExit;

  /** Whether the judging loop runs. */
  private volatile boolean isJudging;

  /** Whether the judger stops: it shuts down, or ran into an Error. */
  private volatile boolean isStopping;

  /** How many judgings of this judger failed in a row. */
  private volatile int failuresInARow;

  /** Whether the last attempt to take a submission failed (e.g. the database is unreachable). */
  private boolean isClaimFailing;

  /** The submission this judger claimed and judges, or {@code null}. */
  private volatile Submission claimedSubmission;

  /**
   * Held while a submission is claimed and set as the claimed one, and while the judger starts to
   * shut down (see {@link #destroy}), never while a submission is judged: a shutdown either comes
   * before a claim, which then takes nothing, or after it, and releases what it took.
   */
  private final Object claimLock = new Object();

  /** Renews the leases of the judgings, on a daemon thread, which never keeps the judger alive. */
  private static final ScheduledExecutorService LEASE_RENEWER =
      Executors.newSingleThreadScheduledExecutor(
          runnable -> {
            Thread thread = new Thread(runnable, "voj-lease-renewal");
            thread.setDaemon(true);
            return thread;
          });

  /** The logger. */
  private static final Logger LOGGER = LogManager.getLogger(Dispatcher.class);
}
