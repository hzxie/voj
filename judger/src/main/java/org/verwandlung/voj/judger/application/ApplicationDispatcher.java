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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.NonTransientDataAccessException;
import org.springframework.stereotype.Component;

import org.verwandlung.voj.judger.core.CheckpointResult;
import org.verwandlung.voj.judger.core.CompileResult;
import org.verwandlung.voj.judger.core.Outcome;
import org.verwandlung.voj.judger.mapper.JudgeResultMapper;
import org.verwandlung.voj.judger.mapper.SubmissionMapper;
import org.verwandlung.voj.judger.mapper.UserMapper;
import org.verwandlung.voj.judger.messenger.MessageSender;
import org.verwandlung.voj.judger.model.JudgeResult;
import org.verwandlung.voj.judger.model.Submission;
import org.verwandlung.voj.judger.model.User;

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

  /**
   * Claims the next submission to judge in one of some languages (see {@code
   * SubmissionMapper#getNextSubmissionId}) for a new judging, and reads it once claimed: an
   * administrator may edit its problem or test data and rejudge it until then, and the judging
   * must be of what was rejudged. A submission whose previous judging held it until its lease
   * expired is claimed with that judging counted as a failed one (see {@link
   * #INTERRUPTED_REASON}).
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
   */
  public Submission claimSubmission(List<Integer> languageIds, long leaseSeconds) {
    for (Long submissionId = submissionMapper.getNextSubmissionId(languageIds);
        submissionId != null;
        submissionId = submissionMapper.getNextSubmissionId(languageIds)) {
      // Another judger may have claimed the submission since it was read: then the next one.
      long claimToken = RANDOM.nextLong();
      if (submissionMapper.claimSubmission(
              submissionId, claimToken, getJudgerUid(), leaseSeconds, INTERRUPTED_REASON)
          == 1) {
        Submission submission = submissionMapper.getSubmission(submissionId);
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
   * Releases a submission at its place in the queue, without counting a failed judging, so that
   * another judger takes it at once: its judging stops as this judger shuts down, or failed for a
   * reason of the judger or its environment (see {@code Dispatcher}). A judging that was superseded
   * meanwhile releases nothing.
   *
   * @param submission - the submission, claimed by {@link #claimSubmission}
   * @return whether the submission was released
   */
  public boolean releaseSubmission(Submission submission) {
    return submissionMapper.releaseSubmission(
            submission.getSubmissionId(), submission.getClaimToken())
        == 1;
  }

  /**
   * Releases a submission whose judging failed in a way it may have caused (see {@code
   * Dispatcher}), at the end of the queue, and counts the failed judging, with its reason, which the System Error of the submission tells
   * once too many of its judgings failed. A judging that was superseded meanwhile releases nothing.
   *
   * @param submission - the submission, claimed by {@link #claimSubmission}
   * @param reason - why the judging failed, shown to the user (cut to {@value #MAX_REASON_LENGTH}
   *     characters)
   * @return whether the submission was released
   */
  public boolean requeueSubmission(Submission submission, String reason) {
    String storedReason =
        reason.length() <= MAX_REASON_LENGTH ? reason : reason.substring(0, MAX_REASON_LENGTH);
    return submissionMapper.requeueSubmission(
            submission.getSubmissionId(), submission.getClaimToken(), storedReason)
        == 1;
  }

  /**
   * Records a System Error: the submission could not be judged for a reason that is not its own.
   *
   * @param submission - the submission, claimed by {@link #claimSubmission}
   * @param reason - why the submission could not be judged, shown to the user in the judge log
   */
  public void onErrorOccurred(Submission submission, String reason) {
    if (updateSubmission(submission, Outcome.systemError(reason))) {
      messageSender.sendMessage(newMessage("ErrorOccurred", submission));
    }
  }

  /**
   * Reports the compilation of a submission, and records a Compile Error.
   *
   * @param submission - the submission, claimed by {@link #claimSubmission}
   * @param result - the compilation result
   */
  public void onCompileFinished(Submission submission, CompileResult result) {
    Map<String, Object> message = newMessage("CompileFinished", submission);
    message.put("isSuccessful", result.isSuccessful());
    if (!result.isSuccessful()) {
      Outcome outcome = Outcome.compileError(result.log());
      if (!updateSubmission(submission, outcome)) {
        return;
      }
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
   */
  public void onAllTestPointsFinished(Submission submission, List<CheckpointResult> results) {
    Outcome outcome = Outcome.of(results, this::getJudgeResultName);
    if (!updateSubmission(submission, outcome)) {
      return;
    }
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
   * Records the verdict of a judging: only the result columns, and only if the submission is still
   * claimed by this judging (see {@link #claimSubmission}); otherwise the verdict is stale and
   * dropped.
   *
   * <p>A statement that fails for now (e.g. while the database fails over, or a connection broke,
   * see {@link #isUnreachable}) is tried again, {@value #UPDATE_TRIES} times in all, a little later each time, which is still well within
   * the lease of the judging: a verdict is not lost to a short outage. (A statement that did change
   * the submission though its answer was lost finds it judged when tried again, and reports the
   * judging superseded.)
   *
   * @param submission - the submission, claimed by {@link #claimSubmission}
   * @param outcome - the outcome of the judging
   * @return whether the verdict was recorded; if not, the judging was superseded
   * @throws DataAccessException if the verdict cannot be recorded, as the database is unreachable
   */
  private boolean updateSubmission(Submission submission, Outcome outcome) {
    String judgeResult = outcome.judgeResult();
    submission.setUsedTime(outcome.usedTime());
    submission.setUsedMemory(outcome.usedMemory());
    submission.setJudgeScore(outcome.score());
    submission.setJudgeResultSlug(judgeResult);
    submission.setJudgeLog(outcome.log());

    if (updateSubmissionResult(submission) == 0) {
      LOGGER.warn(
          String.format(
              "Dropped the verdict %s of submission #%d: its judging was superseded.",
              judgeResult, submission.getSubmissionId()));
      return false;
    }
    return true;
  }

  /**
   * Updates the result columns of a submission, trying a failed statement again.
   *
   * @param submission - the submission, with its verdict
   * @return the number of submissions updated
   * @throws DataAccessException if no try succeeded
   */
  private int updateSubmissionResult(Submission submission) {
    for (int tries = 1; ; ++tries) {
      try {
        return submissionMapper.updateSubmissionResult(submission);
      } catch (DataAccessException ex) {
        if (tries == UPDATE_TRIES || !isUnreachable(ex)) {
          throw ex;
        }
        LOGGER.warn(
            String.format(
                "Failed to record the verdict of submission #%d; trying again.",
                submission.getSubmissionId()),
            ex);
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

  /**
   * Resolves and caches the uid of this judger, so each judged submission records which judger
   * processed it. The judger is a regular user account (in the {@code judgers} group) identified by
   * the configured username.
   *
   * @return the uid of this judger, or null if the account cannot be resolved
   */
  private Long getJudgerUid() {
    if (judgerUid == null && judgerUsername != null && !judgerUsername.isEmpty()) {
      User judger = userMapper.getUserUsingUsername(judgerUsername);
      if (judger != null) {
        judgerUid = judger.getUid();
      }
    }
    return judgerUid;
  }

  /** The autowired MessageSender object, used to send messages to the message queue. */
  @Autowired private MessageSender messageSender;

  /** The autowired SubmissionMapper object, used to query/update submission information. */
  @Autowired private SubmissionMapper submissionMapper;

  /** The autowired JudgeResultMapper object, used to access information about all judge results. */
  @Autowired private JudgeResultMapper judgeResultMapper;

  /** The autowired UserMapper object, used to resolve this judger's own user account. */
  @Autowired private UserMapper userMapper;

  /** The judger's username, used to resolve which judger processed each submission. */
  @Value("${judger.username}")
  private String judgerUsername;

  /** The lazily-resolved, cached uid of this judger (null until resolved). */
  private Long judgerUid;

  /** The names of the judge results, by their slug, as read so far. */
  private final Map<String, String> judgeResultNames = new ConcurrentHashMap<>();

  /** The longest reason of a failed judging the database takes. */
  static final int MAX_REASON_LENGTH = 255;

  /** How many times a verdict is recorded before a failure to do so is given up. */
  static final int UPDATE_TRIES = 3;

  /** How long (ms) a failed statement waits before it is tried again; doubles each time. */
  private long retryDelay = 1000;

  /** The source of the claim tokens. */
  private static final SecureRandom RANDOM = new SecureRandom();

  /** The logger. */
  private static final Logger LOGGER = LogManager.getLogger(ApplicationDispatcher.class);
}
