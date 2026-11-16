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
package org.verwandlung.voj.judger.application;

import java.security.SecureRandom;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.IntSupplier;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.NonTransientDataAccessException;
import org.springframework.stereotype.Component;

import org.verwandlung.voj.judger.core.CheckpointResult;
import org.verwandlung.voj.judger.core.CompileResult;
import org.verwandlung.voj.judger.core.Outcome;
import org.verwandlung.voj.judger.core.Superseded;
import org.verwandlung.voj.judger.mapper.JudgeResultMapper;
import org.verwandlung.voj.judger.mapper.SubmissionMapper;
import org.verwandlung.voj.judger.messenger.MessageSender;
import org.verwandlung.voj.judger.model.JudgeResult;
import org.verwandlung.voj.judger.model.Submission;

/**
 * The application dispatcher: claims the submissions to judge in the database, records the outcome
 * of their judgings there, and streams their progress to the web application.
 *
 * @author Haozhe Xie
 */
@Component
public class ApplicationDispatcher {
  /**
   * Why a judging whose lease expired failed, as the judger that claims its submission next records
   * it (see {@link #claimSubmission}).
   */
  public static final String INTERRUPTED_REASON =
      "Its judger stopped, crashed or lost the database while judging it.";

  /** How long (ms) a failed statement waits before it is tried again; doubles each time. */
  static final long RETRY_DELAY_MS = 1000;

  /**
   * Creates the application dispatcher of a judger.
   *
   * @param submissionMapper - the submissions
   * @param judgeResultMapper - the judge results
   * @param messageSender - sends the progress of the judgings to the web application
   * @param judgerAccount - the account of the judger
   */
  @Autowired
  public ApplicationDispatcher(
      SubmissionMapper submissionMapper,
      JudgeResultMapper judgeResultMapper,
      MessageSender messageSender,
      JudgerAccount judgerAccount) {
    this(submissionMapper, judgeResultMapper, messageSender, judgerAccount, RETRY_DELAY_MS);
  }

  /**
   * Creates an application dispatcher.
   *
   * @param submissionMapper - the submissions
   * @param judgeResultMapper - the judge results
   * @param messageSender - sends the progress of the judgings to the web application
   * @param judgerAccount - the account of the judger
   * @param retryDelay - how long (ms) a failed statement waits before it is tried again
   */
  ApplicationDispatcher(
      SubmissionMapper submissionMapper,
      JudgeResultMapper judgeResultMapper,
      MessageSender messageSender,
      JudgerAccount judgerAccount,
      long retryDelay) {
    this.submissionMapper = submissionMapper;
    this.judgeResultMapper = judgeResultMapper;
    this.messageSender = messageSender;
    this.judgerAccount = judgerAccount;
    this.retryDelay = retryDelay;
  }

  /**
   * Claims the next submission to judge in one of some languages (see {@code
   * SubmissionMapper#getNextSubmissionId}) for a new judging, and reads it once claimed: an
   * administrator may edit its problem or test data and rejudge it until then, and the judging
   * must be of what was rejudged. A submission whose previous judging held it until its lease
   * expired is claimed with that judging counted as a failed one (see {@link
   * #INTERRUPTED_REASON}). A claimed submission that cannot be read (e.g. the database went away
   * meanwhile) is released at once if the database lets it (see {@link #releaseSubmission}), rather
   * than held until its lease expired, which would count a failed judging on it.
   *
   * <p>The claim token, a random number, identifies the judging: its lease is renewed (see {@link
   * #renewLease}) and its verdict recorded only while the submission still carries it, and its
   * messages carry it, so that the web application shows the messages of the current judging only.
   * A rejudge clears it, and a later claim (once the lease of this one expired) replaces it: the
   * judging they supersede stops, without overwriting the new verdict with a stale one.
   *
   * @param languageIds - the unique identifiers of the languages this judger supports, not empty
   * @param leaseSeconds - how long the lease of the judging lasts, in seconds
   * @return the submission, with the claim token of the judging, or {@code null} if there is none
   *     to judge
   * @throws DataAccessException if no submission can be claimed as the database is unreachable
   */
  public Submission claimSubmission(List<Integer> languageIds, long leaseSeconds) {
    for (Long submissionId = submissionMapper.getNextSubmissionId(languageIds);
        submissionId != null;
        submissionId = submissionMapper.getNextSubmissionId(languageIds)) {
      // Another judger may have claimed the submission since it was read: then the next one.
      long claimToken = RANDOM.nextLong();
      if (submissionMapper.claimSubmission(
              submissionId, claimToken, judgerAccount.getUid(), leaseSeconds, INTERRUPTED_REASON)
          == 1) {
        Submission submission;
        try {
          submission = submissionMapper.getSubmission(submissionId);
        } catch (RuntimeException ex) {
          try {
            release(submissionId, claimToken, false);
          } catch (RuntimeException releaseFailure) {
            ex.addSuppressed(releaseFailure);
          }
          throw ex;
        }
        if (submission != null) {
          submission.setClaimToken(claimToken);
          return submission;
        }
      }
    }
    return null;
  }

  /**
   * Renews the lease of a judging, so that no other judging claims its submission meanwhile.
   *
   * @param submission - the submission, claimed by {@link #claimSubmission}
   * @param leaseSeconds - how long the lease lasts from now, in seconds
   * @return whether the judging is still the current one; if not, it was superseded (the
   *     submission was rejudged, or claimed by another judging as its lease had expired) and must
   *     stop without recording or sending anything
   */
  public boolean renewLease(Submission submission, long leaseSeconds) {
    return submissionMapper.renewLease(
            submission.getSubmissionId(), submission.getClaimToken(), leaseSeconds)
        == 1;
  }

  /**
   * Releases a submission whose judging failed for a reason of the judger or its environment (see
   * {@code Dispatcher}), to the end of the queue, without counting a failed judging on it: a
   * submission whose judgings fail that way every time holds no other submission up. A judging that
   * was superseded meanwhile releases nothing.
   *
   * @param submission - the submission, claimed by {@link #claimSubmission}
   * @return whether the submission was released
   * @throws DataAccessException if the database could not be written (see {@link #write})
   */
  public boolean releaseSubmission(Submission submission) {
    return release(submission.getSubmissionId(), submission.getClaimToken(), false);
  }

  /**
   * Releases a submission whose judging stops as this judger shuts down, at its place in the queue,
   * without counting a failed judging on it, so that another judger takes it at once. A judging
   * that was superseded meanwhile releases nothing.
   *
   * @param submission - the submission, claimed by {@link #claimSubmission}
   * @return whether the submission was released
   * @throws DataAccessException if the database could not be written (see {@link #write})
   */
  public boolean handBackSubmission(Submission submission) {
    return release(submission.getSubmissionId(), submission.getClaimToken(), true);
  }

  /**
   * Releases a submission that a judging claimed, without counting a failed judging on it.
   *
   * @param submissionId - the unique identifier of the submission
   * @param claimToken - the claim token of the judging
   * @param keepsPlace - whether it keeps its place in the queue, rather than going to the end
   * @return whether the submission was released
   * @throws DataAccessException if the database could not be written (see {@link #write})
   */
  private boolean release(long submissionId, long claimToken, boolean keepsPlace) {
    return write(
            "release",
            submissionId,
            () -> submissionMapper.releaseSubmission(submissionId, claimToken, keepsPlace))
        == 1;
  }

  /**
   * Releases a submission whose judging failed in a way it may have caused (see {@code
   * Dispatcher}), to the end of the queue, and counts the failed judging, with its reason, which
   * the System Error of the submission tells once too many of its judgings failed. A judging that
   * was superseded meanwhile releases nothing.
   *
   * @param submission - the submission, claimed by {@link #claimSubmission}
   * @param reason - why the judging failed, shown to the user, at most 255 characters
   * @return whether the submission was released
   * @throws DataAccessException if the database could not be written (see {@link #write})
   */
  public boolean requeueSubmission(Submission submission, String reason) {
    return write(
            "requeue",
            submission.getSubmissionId(),
            () ->
                submissionMapper.requeueSubmission(
                    submission.getSubmissionId(), submission.getClaimToken(), reason))
        == 1;
  }

  /**
   * Records a System Error: the submission could not be judged for a reason that is not its own.
   *
   * @param submission - the submission, claimed by {@link #claimSubmission}
   * @param reason - why the submission could not be judged, shown to the user in the judge log
   * @throws Superseded if the judging was superseded
   */
  public void onErrorOccurred(Submission submission, String reason) throws Superseded {
    record(submission, Outcome.systemError(reason));
    messageSender.sendMessage(newMessage("ErrorOccurred", submission));
  }

  /**
   * Reports the compilation of a submission, and records a Compile Error.
   *
   * @param submission - the submission, claimed by {@link #claimSubmission}
   * @param result - the compilation result
   * @throws Superseded if the judging was superseded
   */
  public void onCompileFinished(Submission submission, CompileResult result) throws Superseded {
    Map<String, Object> message = newMessage("CompileFinished", submission);
    message.put("isSuccessful", result.isSuccessful());
    if (!result.isSuccessful()) {
      Outcome outcome = Outcome.compileError(result.log());
      record(submission, outcome);
      message.put("log", outcome.log());
    }
    messageSender.sendMessage(message);
  }

  /**
   * Reports the verdict of a submission on a checkpoint.
   *
   * @param submission - the submission, claimed by {@link #claimSubmission}
   * @param result - its verdict on the checkpoint
   */
  public void onOneTestPointFinished(Submission submission, CheckpointResult result) {
    Map<String, Object> message = newMessage("TestPointFinished", submission);
    message.put("checkpointId", result.checkpointId());
    message.put("runtimeResult", getJudgeResultName(result.judgeResult()));
    message.put("usedTime", result.usedTime());
    message.put("usedMemory", result.usedMemory());
    message.put("score", result.score());
    messageSender.sendMessage(message);
  }

  /**
   * Records and reports the verdict of a submission that ran on every checkpoint.
   *
   * @param submission - the submission, claimed by {@link #claimSubmission}
   * @param results - its verdicts on the checkpoints, in their order, not empty
   * @throws Superseded if the judging was superseded
   */
  public void onAllTestPointsFinished(Submission submission, List<CheckpointResult> results)
      throws Superseded {
    Outcome outcome = Outcome.of(results, this::getJudgeResultName);
    record(submission, outcome);
    Map<String, Object> message = newMessage("AllTestPointsFinished", submission);
    message.put("runtimeResult", getJudgeResultName(outcome.judgeResult()));
    message.put("totalTime", outcome.usedTime());
    message.put("maxMemory", outcome.usedMemory());
    message.put("totalScore", outcome.score());
    messageSender.sendMessage(message);
  }

  /**
   * Creates a progress message of a judging. Besides the submission it carries the claim token of
   * the judging (see {@link #claimSubmission}), so that the web application can drop the messages
   * of a judging that was superseded instead of showing them on the submission's page. The
   * messages only stream the progress live (the verdicts are in the database), and are sent in the
   * background (see {@link MessageSender}).
   *
   * @param event - the event of the message
   * @param submission - the submission, claimed by {@link #claimSubmission}
   * @return the message
   */
  private static Map<String, Object> newMessage(String event, Submission submission) {
    Map<String, Object> message = new HashMap<>();
    message.put("event", event);
    message.put("submissionId", submission.getSubmissionId());
    message.put("claimToken", submission.getClaimToken());
    return message;
  }

  /**
   * Gets the name of a judge result, which the users see.
   *
   * @param judgeResult - the slug of the judge result
   * @return its name, or the slug if there is no such judge result
   */
  private String getJudgeResultName(String judgeResult) {
    // The judge results are reference data, read once: a judging reads no more from the database
    // than it must, so that a short outage of it does not stop the judging of every checkpoint.
    return judgeResultNames.computeIfAbsent(
        judgeResult,
        slug -> {
          JudgeResult result = judgeResultMapper.getJudgeResultUsingSlug(slug);
          return result == null ? slug : result.getJudgeResultName();
        });
  }

  /**
   * Records the outcome of a judging: only the result columns, and only if the submission is still
   * claimed by this judging (see {@link #claimSubmission}).
   *
   * @param submission - the submission, claimed by {@link #claimSubmission}
   * @param outcome - the outcome of the judging
   * @throws Superseded if the judging was superseded: the verdict is stale, and not recorded
   * @throws DataAccessException if the verdict cannot be recorded (see {@link #write})
   */
  private void record(Submission submission, Outcome outcome) throws Superseded {
    submission.setUsedTime(outcome.usedTime());
    submission.setUsedMemory(outcome.usedMemory());
    submission.setJudgeScore(outcome.score());
    submission.setJudgeResultSlug(outcome.judgeResult());
    submission.setJudgeLog(outcome.log());
    int recorded =
        write(
            "record the verdict of",
            submission.getSubmissionId(),
            () -> submissionMapper.updateSubmissionResult(submission));
    if (recorded == 0) {
      throw new Superseded();
    }
  }

  /**
   * Runs a write of a judging to its submission, which changes the submission only while the
   * judging still holds it (by its claim token): records its verdict, or releases it.
   *
   * <p>A statement that fails for now (e.g. while the database fails over, or a connection broke,
   * see {@link #isUnreachable}) is tried again, {@value #WRITE_TRIES} times in all, a second after
   * the first try and two after the second: what a judging found is not lost to a short outage. A
   * try fails once it got no connection within {@code jdbc.maxWait} or no answer within the {@code
   * socketTimeout} of the {@code jdbc.url}. Each write may be tried again after it was done though
   * its answer was lost: a verdict is recorded once more (the claim token stays), and a release
   * finds the claim token gone and changes nothing.
   *
   * @param action - what the statement does to the submission, for the log
   * @param submissionId - the unique identifier of the submission
   * @param statement - the statement, which returns the number of submissions it changed
   * @return the number of submissions changed
   * @throws DataAccessException if the database refused the statement, or no try succeeded
   */
  private int write(String action, long submissionId, IntSupplier statement) {
    for (int tries = 1; ; ++tries) {
      try {
        return statement.getAsInt();
      } catch (DataAccessException ex) {
        if (tries == WRITE_TRIES || !isUnreachable(ex)) {
          throw ex;
        }
        LOGGER.warn(
            String.format("Failed to %s submission #%d; trying again.", action, submissionId), ex);
        try {
          Thread.sleep(retryDelay << (tries - 1));
        } catch (InterruptedException interruption) {
          Thread.currentThread().interrupt();
          throw ex;
        }
      }
    }
  }

  /**
   * Tells a failure of the database that a statement may get over if tried again (the database is
   * unreachable, a connection broke or timed out, a lock could not be had) from one it may not (e.g.
   * a value the column does not take), which may be the doing of what is recorded.
   *
   * @param ex - the failure
   * @return whether the database failed for now
   */
  public static boolean isUnreachable(DataAccessException ex) {
    return !(ex instanceof NonTransientDataAccessException)
        || ex instanceof DataAccessResourceFailureException;
  }

  /** Sends the progress of the judgings to the web application. */
  private final MessageSender messageSender;

  /** The submissions. */
  private final SubmissionMapper submissionMapper;

  /** The judge results, whose names the users see. */
  private final JudgeResultMapper judgeResultMapper;

  /** The account of the judger, which its claims are recorded by. */
  private final JudgerAccount judgerAccount;

  /** The names of the judge results, by their slug, as read so far. */
  private final Map<String, String> judgeResultNames = new ConcurrentHashMap<>();

  /** How many times a write of a judging is tried before a failure to do it is given up. */
  static final int WRITE_TRIES = 3;

  /** How long (ms) a failed statement waits before it is tried again; doubles each time. */
  private final long retryDelay;

  /** The source of the claim tokens. */
  private static final SecureRandom RANDOM = new SecureRandom();

  /** The logger. */
  private static final Logger LOGGER = LogManager.getLogger(ApplicationDispatcher.class);
}
