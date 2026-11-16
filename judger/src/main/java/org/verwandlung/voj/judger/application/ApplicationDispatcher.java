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

import org.verwandlung.voj.judger.core.CompileResult;
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
   * Notifies the user when a system error occurs: the submission could not be judged for a reason
   * that is not its own fault.
   *
   * @param submission - the submission, claimed by {@link #claimSubmission}
   * @param reason - why the submission could not be judged, shown to the user in the judge log
   */
  public void onErrorOccurred(Submission submission, String reason) {
    if (updateSubmission(
        submission, 0, 0, 0, "SE", String.format("System Error.\n\n%s\n", reason))) {
      notifyWebApplication(newMessage("ErrorOccurred", submission));
    }
  }

  /**
   * Notifies the user when the compilation stage finishes.
   *
   * @param submission - the submission, claimed by {@link #claimSubmission}
   * @param result - the compilation result
   */
  public void onCompileFinished(Submission submission, CompileResult result) {
    boolean isSuccessful = result.isSuccessful();
    String log = getJudgeLog(result);

    if (!isSuccessful && !updateSubmission(submission, 0, 0, 0, "CE", log)) {
      return;
    }
    Map<String, Object> mapMessage = newMessage("CompileFinished", submission);
    mapMessage.put("isSuccessful", isSuccessful);
    mapMessage.put("log", log);

    notifyWebApplication(mapMessage);
  }

  /**
   * Returns the judging result in real time.
   *
   * @param submission - the submission, claimed by {@link #claimSubmission}
   * @param checkpointId - the number of the checkpoint
   * @param runtimeResult - the program's runtime result for a checkpoint
   */
  public void onOneTestPointFinished(
      Submission submission, int checkpointId, Map<String, Object> runtimeResult) {
    String runtimeResultSlug = getRuntimeResultSlug(runtimeResult);
    String runtimeResultName = getRuntimeResultName(runtimeResultSlug);
    int usedTime = getUsedTime(runtimeResult);
    int usedMemory = getUsedMemory(runtimeResult);
    int score = getScore(runtimeResult);

    Map<String, Object> mapMessage = newMessage("TestPointFinished", submission);
    mapMessage.put("checkpointId", checkpointId);
    mapMessage.put("runtimeResult", runtimeResultName);
    mapMessage.put("usedTime", usedTime);
    mapMessage.put("usedMemory", usedMemory);
    mapMessage.put("score", score);

    notifyWebApplication(mapMessage);
  }

  /**
   * Persists the program's judging result.
   *
   * @param submission - the submission, claimed by {@link #claimSubmission}
   * @param runtimeResults - the set of judging results for all checkpoints
   */
  public void onAllTestPointsFinished(
      Submission submission, List<Map<String, Object>> runtimeResults) {
    int totalTime = 0;
    int maxMemory = 0;
    int totalScore = 0;
    // Without any checkpoint there is nothing the submission was judged against: never Accepted.
    String runtimeResultSlug = runtimeResults.isEmpty() ? "SE" : "AC";
    String log;

    for (Map<String, Object> runtimeResult : runtimeResults) {
      String currentRuntimeResultSlug = getRuntimeResultSlug(runtimeResult);
      int usedTime = getUsedTime(runtimeResult);
      int usedMemory = getUsedMemory(runtimeResult);
      int score = getScore(runtimeResult);

      totalTime += usedTime;
      if (usedMemory > maxMemory) {
        maxMemory = usedMemory;
      }
      if ("AC".equals(currentRuntimeResultSlug)) {
        totalScore += score;
      }
      // The overall verdict is that of the FIRST failing checkpoint (standard OJ behaviour).
      if ("AC".equals(runtimeResultSlug) && !"AC".equals(currentRuntimeResultSlug)) {
        runtimeResultSlug = currentRuntimeResultSlug;
      }
    }
    log = getJudgeLog(runtimeResults, runtimeResultSlug, totalTime, maxMemory, totalScore);
    if (!updateSubmission(submission, totalTime, maxMemory, totalScore, runtimeResultSlug, log)) {
      return;
    }
    Map<String, Object> mapMessage = newMessage("AllTestPointsFinished", submission);
    mapMessage.put("runtimeResult", getRuntimeResultName(runtimeResultSlug));
    mapMessage.put("totalTime", totalTime);
    mapMessage.put("maxMemory", maxMemory);
    mapMessage.put("totalScore", totalScore);

    notifyWebApplication(mapMessage);
  }

  /**
   * Creates a progress message of a judging. Besides the submission it carries the claim token of
   * the judging (see {@link #claimSubmission}), so that the web application can drop the messages
   * of a judging that was superseded instead of showing them on the submission's page.
   *
   * @param event - the event of the message
   * @param submission - the submission, claimed by {@link #claimSubmission}
   * @return the message
   */
  private static Map<String, Object> newMessage(String event, Submission submission) {
    Map<String, Object> mapMessage = new HashMap<>();
    mapMessage.put("event", event);
    mapMessage.put("submissionId", submission.getSubmissionId());
    mapMessage.put("claimToken", submission.getClaimToken());
    return mapMessage;
  }

  /**
   * Sends a progress message to the web application. The verdict itself is stored in the database,
   * which is what the web application shows once judging is over; the messages only stream the
   * progress live. So a message that cannot be sent (e.g. while the broker restarts) is logged and
   * the judging goes on, rather than failing a judging whose verdict can still be recorded.
   *
   * @param mapMessage - the message
   */
  private void notifyWebApplication(Map<String, Object> mapMessage) {
    try {
      messageSender.sendMessage(mapMessage);
    } catch (RuntimeException ex) {
      LOGGER.warn("Failed to send the " + mapMessage.get("event") + " message", ex);
    }
  }

  /**
   * Gets the unique English abbreviation of the program's judging result from the result set.
   *
   * @param runtimeResult - the program's judging result
   * @return the unique English abbreviation of the program's judging result
   */
  private String getRuntimeResultSlug(Map<String, Object> runtimeResult) {
    Object runtimeResultObject = runtimeResult.get("runtimeResult");

    if (runtimeResultObject == null) {
      return "SE";
    }
    return (String) runtimeResultObject;
  }

  /**
   * Gets the full name of the judging result.
   *
   * @param runtimeResultSlug - the unique English abbreviation of the judging result
   * @return the full name of the judging result
   */
  private String getRuntimeResultName(String runtimeResultSlug) {
    // The judge results are reference data, read once: a judging reads no more from the database
    // than it must, so that a short outage of it does not stop the judging of every checkpoint.
    String runtimeResultName = runtimeResultNames.get(runtimeResultSlug);
    if (runtimeResultName == null) {
      JudgeResult judgeResult = judgeResultMapper.getJudgeResultUsingSlug(runtimeResultSlug);
      if (judgeResult == null) {
        return "System Error";
      }
      runtimeResultName = judgeResult.getJudgeResultName();
      runtimeResultNames.put(runtimeResultSlug, runtimeResultName);
    }
    return runtimeResultName;
  }

  /**
   * Gets the program's running time (ms) from the judging result set.
   *
   * @param runtimeResult - the program's judging result
   * @return the program's running time (ms)
   */
  private int getUsedTime(Map<String, Object> runtimeResult) {
    Object usedTimeObject = runtimeResult.get("usedTime");

    if (usedTimeObject == null) {
      return 0;
    }
    return (Integer) usedTimeObject;
  }

  /**
   * Gets the memory usage (KB) from the judging result set.
   *
   * @param runtimeResult - the program's judging result
   * @return the memory usage (KB)
   */
  private int getUsedMemory(Map<String, Object> runtimeResult) {
    Object usedMemoryObject = runtimeResult.get("usedMemory");

    if (usedMemoryObject == null) {
      return 0;
    }
    return (Integer) usedMemoryObject;
  }

  /**
   * Gets the score corresponding to the checkpoint from the judging result set.
   *
   * @param runtimeResult - the program's judging result
   * @return the score corresponding to the checkpoint
   */
  private int getScore(Map<String, Object> runtimeResult) {
    Object scoreObject = runtimeResult.get("score");

    if (scoreObject == null) {
      return 0;
    }
    return (Integer) scoreObject;
  }

  /**
   * Formats the compile-time log.
   *
   * @param result - the result of the compilation
   * @return the formatted log
   */
  private String getJudgeLog(CompileResult result) {
    boolean isSuccessful = result.isSuccessful();
    String compileLog = result.log();

    StringBuilder formatedLogBuilder = new StringBuilder();
    formatedLogBuilder.append(
        String.format("Compile %s.\n\n", new Object[] {isSuccessful ? "Successful" : "Error"}));
    if (!isSuccessful) {
      // Embed the raw compiler output as a Markdown indented code block (4-space prefix on every
      // line). This keeps the rest of the log as Markdown while rendering the compiler stderr
      // verbatim: line breaks are preserved and angle-bracketed tokens such as
      // "#include <windows.h>" are not stripped as HTML by the front-end Markdown converter.
      formatedLogBuilder.append(compileLog.replaceAll("(?m)^", "    "));
      formatedLogBuilder.append("\n\nCompile Error, Time = 0 ms, Memory = 0 KB, Score = 0.\n");
    }
    return formatedLogBuilder.toString();
  }

  /**
   * Formats the runtime log.
   *
   * @param runtimeResults - the set of judging results for all checkpoints
   * @param runtimeResultSlug - the unique English abbreviation of the overall judging result
   * @param totalTime - the total running time
   * @param maxMemory - the maximum memory used
   * @param totalScore - the total score
   * @return the formatted log
   */
  private String getJudgeLog(
      List<Map<String, Object>> runtimeResults,
      String runtimeResultSlug,
      int totalTime,
      int maxMemory,
      int totalScore) {
    int checkpointId = -1;
    String runtimeResultName = getRuntimeResultName(runtimeResultSlug);

    StringBuilder formatedLogBuilder = new StringBuilder();
    formatedLogBuilder.append("Compile Successfully.\n\n");
    for (Map<String, Object> runtimeResult : runtimeResults) {
      String currentRuntimeResultSlug = getRuntimeResultSlug(runtimeResult);
      String currentRuntimeResultName = getRuntimeResultName(currentRuntimeResultSlug);
      int usedTime = getUsedTime(runtimeResult);
      int usedMemory = getUsedMemory(runtimeResult);
      int score = getScore(runtimeResult);

      if (!"AC".equals(currentRuntimeResultSlug)) {
        score = 0;
      }
      formatedLogBuilder.append(
          String.format(
              "- Test Point #%d: %s, Time = %d ms, Memory = %d KB, Score = %d\n",
              new Object[] {
                ++checkpointId, currentRuntimeResultName, usedTime, usedMemory, score
              }));
    }
    formatedLogBuilder.append(
        String.format(
            "\n%s, Time = %d ms, Memory = %d KB, Score = %d\n",
            new Object[] {runtimeResultName, totalTime, maxMemory, totalScore}));
    return formatedLogBuilder.toString();
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
   * @param usedTime - the running time of the submission (sum of all times)
   * @param usedMemory - the memory used by the submission (maximum memory usage)
   * @param score - the score of the run
   * @param judgeResult - the judging result (JudgeResultSlug)
   * @param log - the judging log
   * @return whether the verdict was recorded; if not, the judging was superseded
   * @throws DataAccessException if the verdict cannot be recorded, as the database is unreachable
   */
  private boolean updateSubmission(
      Submission submission,
      int usedTime,
      int usedMemory,
      int score,
      String judgeResult,
      String log) {
    submission.setUsedTime(usedTime);
    submission.setUsedMemory(usedMemory);
    submission.setJudgeScore(score);
    submission.setJudgeResultSlug(judgeResult);
    submission.setJudgeLog(log);

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
  private final Map<String, String> runtimeResultNames = new ConcurrentHashMap<>();

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
