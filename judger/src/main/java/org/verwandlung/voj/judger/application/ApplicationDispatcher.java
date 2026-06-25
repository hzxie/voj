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

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import org.verwandlung.voj.judger.core.Dispatcher;
import org.verwandlung.voj.judger.mapper.JudgeResultMapper;
import org.verwandlung.voj.judger.mapper.SubmissionMapper;
import org.verwandlung.voj.judger.messenger.MessageSender;
import org.verwandlung.voj.judger.model.JudgeResult;
import org.verwandlung.voj.judger.model.Submission;

/**
 * The application dispatcher.
 *
 * @author Haozhe Xie
 */
@Component
public class ApplicationDispatcher {
  /**
   * Callback invoked when a new judging request is received from the message queue.
   *
   * @param submissionId - the unique identifier of the submission
   */
  public void onSubmissionCreated(long submissionId) {
    try {
      judgerDispatcher.createNewTask(submissionId);
    } catch (Exception ex) {
      LOGGER.catching(ex);
    }
  }

  /**
   * Notifies the user when a system error occurs.
   *
   * @param submissionId - the unique identifier of the submission
   */
  public void onErrorOccurred(long submissionId) {
    updateSubmission(submissionId, 0, 0, 0, "SE", "Internal error occured.");

    Map<String, Object> mapMessage = new HashMap<>();
    mapMessage.put("event", "ErrorOccurred");
    mapMessage.put("submissionId", submissionId);

    messageSender.sendMessage(mapMessage);
  }

  /**
   * Notifies the user when the compilation stage finishes.
   *
   * @param submissionId - the unique identifier of the submission
   * @param result - the compilation result
   */
  public void onCompileFinished(long submissionId, Map<String, Object> result) {
    boolean isSuccessful = (Boolean) result.get("isSuccessful");
    String log = log = getJudgeLog(result);

    if (!isSuccessful) {
      updateSubmission(submissionId, 0, 0, 0, "CE", log);
    }
    Map<String, Object> mapMessage = new HashMap<>();
    mapMessage.put("event", "CompileFinished");
    mapMessage.put("submissionId", submissionId);
    mapMessage.put("isSuccessful", isSuccessful);
    mapMessage.put("log", log);

    messageSender.sendMessage(mapMessage);
  }

  /**
   * Returns the judging result in real time.
   *
   * @param submissionId - the number of the submission
   * @param checkpointId - the number of the checkpoint
   * @param runtimeResult - the program's runtime result for a checkpoint
   */
  public void onOneTestPointFinished(
      long submissionId, int checkpointId, Map<String, Object> runtimeResult) {
    String runtimeResultSlug = getRuntimeResultSlug(runtimeResult);
    String runtimeResultName = getRuntimeResultName(runtimeResultSlug);
    int usedTime = getUsedTime(runtimeResult);
    int usedMemory = getUsedMemory(runtimeResult);
    int score = getScore(runtimeResult);

    Map<String, Object> mapMessage = new HashMap<>();
    mapMessage.put("event", "TestPointFinished");
    mapMessage.put("submissionId", submissionId);
    mapMessage.put("checkpointId", checkpointId);
    mapMessage.put("runtimeResult", runtimeResultName);
    mapMessage.put("usedTime", usedTime);
    mapMessage.put("usedMemory", usedMemory);
    mapMessage.put("score", score);

    messageSender.sendMessage(mapMessage);
  }

  /**
   * Persists the program's judging result.
   *
   * @param submissionId - the number of the submission
   * @param runtimeResults - the set of judging results for all checkpoints
   */
  public void onAllTestPointsFinished(long submissionId, List<Map<String, Object>> runtimeResults) {
    int totalTime = 0;
    int maxMemory = 0;
    int totalScore = 0;
    String runtimeResultSlug = "AC";
    String log = "System Error.";

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
      if (!"AC".equals(currentRuntimeResultSlug)) {
        runtimeResultSlug = currentRuntimeResultSlug;
      }
    }
    log = getJudgeLog(runtimeResults, runtimeResultSlug, totalTime, maxMemory, totalScore);
    updateSubmission(submissionId, totalTime, maxMemory, totalScore, runtimeResultSlug, log);

    Map<String, Object> mapMessage = new HashMap<>();
    mapMessage.put("event", "AllTestPointsFinished");
    mapMessage.put("submissionId", submissionId);
    mapMessage.put("runtimeResult", getRuntimeResultName(runtimeResultSlug));
    mapMessage.put("totalTime", totalTime);
    mapMessage.put("maxMemory", maxMemory);
    mapMessage.put("totalScore", totalScore);

    messageSender.sendMessage(mapMessage);
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
    JudgeResult judgeResult = judgeResultMapper.getJudgeResultUsingSlug(runtimeResultSlug);

    if (judgeResult == null) {
      return "System Error";
    }
    return judgeResult.getJudgeResultName();
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
   * @param result - the Map<String, Object> object containing the compilation status
   * @return the formatted log
   */
  private String getJudgeLog(Map<String, Object> result) {
    boolean isSuccessful = (Boolean) result.get("isSuccessful");
    String compileLog = (String) result.get("log");

    StringBuilder formatedLogBuilder = new StringBuilder();
    formatedLogBuilder.append(
        String.format("Compile %s.\n\n", new Object[] {isSuccessful ? "Successful" : "Error"}));
    if (!isSuccessful) {
      formatedLogBuilder.append(compileLog.replace("\n", "\n\n"));
      formatedLogBuilder.append("\nCompile Error, Time = 0 ms, Memory = 0 KB, Score = 0.\n");
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
   * Updates the submission information.
   *
   * @param submissionId - the unique identifier of the submission
   * @param usedTime - the running time of the submission (sum of all times)
   * @param usedMemory - the memory used by the submission (maximum memory usage)
   * @param score - the score of the run
   * @param judgeResult - the judging result (JudgeResultSlug)
   * @param log - the judging log
   */
  private void updateSubmission(
      long submissionId, int usedTime, int usedMemory, int score, String judgeResult, String log) {
    Submission submission = submissionMapper.getSubmission(submissionId);
    submission.setExecuteTime(new Date());
    submission.setUsedTime(usedTime);
    submission.setUsedMemory(usedMemory);
    submission.setJudgeScore(score);
    submission.setJudgeResultSlug(judgeResult);
    submission.setJudgeLog(log);

    submissionMapper.updateSubmission(submission);
  }

  /** The autowired Dispatcher object, used to schedule judging jobs. */
  @Autowired private Dispatcher judgerDispatcher;

  /** The autowired MessageSender object, used to send messages to the message queue. */
  @Autowired private MessageSender messageSender;

  /** The autowired SubmissionMapper object, used to query/update submission information. */
  @Autowired private SubmissionMapper submissionMapper;

  /** The autowired JudgeResultMapper object, used to access information about all judge results. */
  @Autowired private JudgeResultMapper judgeResultMapper;

  /** The logger. */
  private static final Logger LOGGER = LogManager.getLogger(ApplicationDispatcher.class);
}
