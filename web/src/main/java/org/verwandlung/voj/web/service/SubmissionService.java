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
package org.verwandlung.voj.web.service;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.verwandlung.voj.web.mapper.LanguageMapper;
import org.verwandlung.voj.web.mapper.ProblemMapper;
import org.verwandlung.voj.web.mapper.SubmissionMapper;
import org.verwandlung.voj.web.messenger.MessageSender;
import org.verwandlung.voj.web.model.Language;
import org.verwandlung.voj.web.model.Problem;
import org.verwandlung.voj.web.model.Submission;
import org.verwandlung.voj.web.model.User;

/**
 * The business logic layer of the Submission class.
 *
 * @author Haozhe Xie
 */
@Service
@Transactional
public class SubmissionService {
  /**
   * [For administrators only] Gets the number of submissions within a specified time period.
   *
   * @param startTime - the start time of the statistics
   * @param endTime - the end time of the statistics
   * @return the number of submissions within the specified time period
   */
  public long getNumberOfSubmissionsUsingDate(Date startTime, Date endTime) {
    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    String startTimeString = startTime != null ? sdf.format(startTime) : null;
    String endTimeString = endTime != null ? sdf.format(endTime) : null;
    return submissionMapper.getNumberOfSubmissionsUsingDate(startTimeString, endTimeString);
  }

  /**
   * Gets the number of submissions by a user for a problem.
   *
   * @param problemId - the unique identifier of the problem
   * @param username - the username of the user
   * @return the number of submissions by the user for the problem
   */
  public long getNumberOfSubmissionsUsingProblemIdAndUsername(long problemId, String username) {
    return submissionMapper.getNumberOfSubmissionsUsingProblemIdAndUsername(problemId, username);
  }

  /**
   * [For administrators only] Gets the unique identifier of the latest submission.
   *
   * @return the unique identifier of the latest submission
   */
  public long getLatestSubmissionId() {
    return submissionMapper.getLatestSubmissionId();
  }

  /**
   * Gets the number of submissions for each day within a time period (always day-granularity).
   * Feeds the activity heat-map.
   *
   * @param startTime - the start time of the statistics
   * @param endTime - the end time of the statistics
   * @param uid - the unique identifier of the user
   * @param isAcceptedOnly - whether to count only accepted submissions
   * @return a Map of key-value pairs containing the date and the number of submissions
   */
  public Map<String, Long> getNumberOfSubmissionsGroupByDay(
      Date startTime, Date endTime, long uid, boolean isAcceptedOnly) {
    // Build an empty list covering the date range
    Map<String, Long> numberOfSubmissions = new LinkedHashMap<String, Long>();
    Calendar calendar = new GregorianCalendar();
    calendar.setTime(startTime);
    while (calendar.getTime().before(endTime)) {
      SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd");
      calendar.add(Calendar.DATE, 1);
      Date targetDate = calendar.getTime();
      numberOfSubmissions.put(sdf.format(targetDate), (long) 0);
    }

    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd 00:00:00");
    String startTimeString = sdf.format(startTime);
    String endTimeString = sdf.format(endTime);
    List<Map<String, Object>> submissions =
        submissionMapper.getNumberOfSubmissionsGroupByDay(
            startTimeString, endTimeString, uid, isAcceptedOnly);

    for (Map<String, Object> e : submissions) {
      String date = ((java.sql.Date) e.get("date")).toString().replace('-', '/');
      long submissionTimes = (Long) e.get("submissions");
      numberOfSubmissions.put(date, submissionTimes);
    }
    return numberOfSubmissions;
  }

  /**
   * Gets a submission object by its unique identifier.
   *
   * @param submissionId - the unique identifier of the submission
   * @return the submission object
   */
  public Submission getSubmission(long submissionId) {
    return submissionMapper.getSubmission(submissionId);
  }

  /**
   * Gets the list of submissions.
   *
   * @param problemId - the unique identifier of the problem
   * @param username - the username of the user
   * @param limit - the number of submissions to load each time
   * @return the list of submissions (a List<Submission> object)
   */
  public List<Submission> getSubmissions(long problemId, String username, int limit) {
    return submissionMapper.getSubmissions(problemId, username, limit);
  }

  /**
   * Gets the list of submissions. Used to load submissions asynchronously.
   *
   * @param problemId - the unique identifier of the problem
   * @param username - the username of the user
   * @param offset - the starting number of the submission identifier
   * @param limit - the number of submissions to load each time
   * @return the list of submissions (a List<Submission> object)
   */
  public List<Submission> getSubmissions(long problemId, String username, long offset, int limit) {
    return submissionMapper.getSubmissionsUsingOffset(problemId, username, offset, limit);
  }

  /**
   * Gets the list of latest submissions. Used to periodically fetch the latest submissions.
   *
   * @param problemId - the unique identifier of the problem
   * @param username - the username of the user
   * @param offset - the starting number of the submission identifier
   * @param limit - the number of submissions to load each time
   * @return the list of submissions (a List<Submission> object)
   */
  public List<Submission> getLatestSubmissions(
      long problemId, String username, long offset, int limit) {
    return submissionMapper.getLatestSubmissionsUsingOffset(problemId, username, offset, limit);
  }

  /**
   * Gets the submissions of a user for a problem.
   *
   * @param problemId - the unique identifier of the problem
   * @param userId - the unique identifier of the user
   * @param limit - the number of submissions to load each time
   * @return the submissions of the user for the problem
   */
  public List<Submission> getSubmissionUsingProblemIdAndUserId(
      long problemId, long userId, int limit) {
    return submissionMapper.getSubmissionUsingProblemIdAndUserId(problemId, userId, limit);
  }

  /**
   * Gets the judging results of all problems for a user.
   *
   * @param userId - the unique identifier of the user
   * @return the judging results of all problems for the user
   */
  public Map<Long, Submission> getSubmissionOfUser(long userId) {
    long problemIdLowerBound = problemMapper.getLowerBoundOfProblems();
    long problemIdUpperBound = problemMapper.getUpperBoundOfProblems();

    return getSubmissionOfProblems(userId, problemIdLowerBound, problemIdUpperBound);
  }

  /**
   * Gets the judging results of a user within a problem ID range.
   *
   * @param userId - the unique identifier of the user
   * @param problemIdLowerBound - the lower bound of the problem ID range
   * @param problemIdUpperBound - the upper bound of the problem ID range
   * @return the judging results within the problem ID range
   */
  public Map<Long, Submission> getSubmissionOfProblems(
      long userId, long problemIdLowerBound, long problemIdUpperBound) {
    Map<Long, Submission> submissionOfProblems = new HashMap<>();
    List<Submission> latestSubmission =
        submissionMapper.getLatestSubmissionOfProblems(
            userId, problemIdLowerBound, problemIdUpperBound);
    List<Submission> acceptedSubmission =
        submissionMapper.getAcceptedSubmissionOfProblems(
            userId, problemIdLowerBound, problemIdUpperBound);

    for (Submission s : latestSubmission) {
      long problemId = s.getProblem().getProblemId();
      submissionOfProblems.put(problemId, s);
    }
    // Update some values with the accepted submissions
    for (Submission s : acceptedSubmission) {
      long problemId = s.getProblem().getProblemId();
      submissionOfProblems.put(problemId, s);
    }
    return submissionOfProblems;
  }

  /**
   * Gets the submission statistics overview of a user.
   *
   * @param userId - the unique identifier of the user
   * @return a HashMap containing the user's submission statistics overview
   */
  public Map<String, Long> getSubmissionStatsOfUser(long userId) {
    long acceptedSubmission = submissionMapper.getAcceptedSubmissionUsingUserId(userId);
    long totalSubmission = submissionMapper.getTotalSubmissionUsingUserId(userId);
    long acRate = 0;
    if (totalSubmission != 0) {
      acRate = acceptedSubmission * 100 / totalSubmission;
    }

    Map<String, Long> submissionStats = new HashMap<>(4, 1);
    submissionStats.put("acceptedSubmission", acceptedSubmission);
    submissionStats.put("totalSubmission", totalSubmission);
    submissionStats.put("acRate", acRate);
    return submissionStats;
  }

  /**
   * Gets a user's solved-problem counts grouped by difficulty level, feeding the profile's
   * "solved by difficulty" breakdown. Each entry carries the difficulty slug/name, the number of
   * public problems at that difficulty (total), the number the user has solved (solved) and the
   * completion percentage.
   *
   * @param userId - the unique identifier of the user
   * @return an ordered list of per-difficulty stat maps
   */
  public List<Map<String, Object>> getSolvedProblemsByDifficulty(long userId) {
    List<Map<String, Object>> difficulties =
        submissionMapper.getNumberOfSolvedProblemsByDifficulty(userId);
    for (Map<String, Object> difficulty : difficulties) {
      long total = ((Number) difficulty.get("total")).longValue();
      long solved = ((Number) difficulty.get("solved")).longValue();
      difficulty.put("percentage", total == 0 ? 0 : solved * 100 / total);
    }
    return difficulties;
  }

  /**
   * Creates a submission and adds the judging task to the message queue.
   *
   * @param user - the logged-in user object
   * @param problemId - the unique identifier of the problem
   * @param languageSlug - the alias of the programming language
   * @param code - the code
   * @return a Map<String, Object> object containing the submission creation result, including the
   *     unique identifier of the created submission.
   */
  public Map<String, Object> createSubmission(
      User user, long problemId, String languageSlug, String code) {
    Problem problem = problemMapper.getProblem(problemId);
    Language language = languageMapper.getLanguageUsingSlug(languageSlug);

    Submission submission = new Submission(problem, user, language, code);

    @SuppressWarnings("unchecked")
    Map<String, Object> result =
        (Map<String, Object>) getSubmissionCreationResult(submission);
    boolean isSuccessful = (Boolean) result.get("isSuccessful");
    if (isSuccessful) {
      submissionMapper.createSubmission(submission);

      long submissionId = submission.getSubmissionId();
      createSubmissionTask(submissionId, language.getLanguageSlug());
      result.put("submissionId", submissionId);
    }
    return result;
  }

  /**
   * Validates the submission data.
   *
   * @param submission - the submission object to create
   * @return a Map<String, Boolean> object containing the validation result of the submission
   */
  private Map<String, ? extends Object> getSubmissionCreationResult(Submission submission) {
    Map<String, Boolean> result = new HashMap<>(6, 1);
    String code = submission.getCode();
    result.put("isUserLogined", submission.getUser() != null);
    result.put("isProblemExists", submission.getProblem() != null);
    result.put("isLanguageExists", submission.getLanguage() != null);
    result.put("isCodeEmpty", code == null || code.length() == 0);

    boolean isSuccessful =
        result.get("isUserLogined")
            && result.get("isProblemExists")
            && result.get("isLanguageExists")
            && !result.get("isCodeEmpty");
    result.put("isSuccessful", isSuccessful);
    return result;
  }

  /**
   * Creates a judging task and submits the submission information to the message queue. Looks up the
   * submission's language so the task can be tagged for language-aware routing; used by the rejudge
   * flow, which only knows the submission's unique identifier.
   *
   * @param submissionId - the unique identifier of the submission
   */
  public void createSubmissionTask(long submissionId) {
    Submission submission = submissionMapper.getSubmission(submissionId);
    String languageSlug =
        (submission != null && submission.getLanguage() != null)
            ? submission.getLanguage().getLanguageSlug()
            : null;
    createSubmissionTask(submissionId, languageSlug);
  }

  /**
   * Creates a judging task and submits the submission information to the message queue.
   *
   * @param submissionId - the unique identifier of the submission
   * @param languageSlug - the slug of the submission's language, used by judgers to claim only the
   *     tasks they can build
   */
  public void createSubmissionTask(long submissionId, String languageSlug) {
    Map<String, Object> mapMessage = new HashMap<>();
    mapMessage.put("event", "SubmissionCreated");
    mapMessage.put("submissionId", submissionId);

    messageSender.sendMessage(mapMessage, languageSlug);
  }

  /**
   * Marks a still-pending submission as a terminal failure because no judger was available to handle
   * it before its judging task expired in the queue. Invoked from the dead-letter listener. The
   * update only touches submissions that are still pending, so it never overwrites a real verdict
   * (and is safe to apply more than once).
   *
   * @param submissionId - the unique identifier of the submission whose task expired
   * @return whether the submission was marked (false when it was already judged or does not exist)
   */
  public boolean markSubmissionUnjudgeable(long submissionId) {
    String judgeLog =
        "System Error.\n\nNo available judger supports this submission's language, so the judging "
            + "task expired before it could be processed.\n";
    return submissionMapper.markSubmissionUnjudgeable(submissionId, "SE", judgeLog) > 0;
  }

  /**
   * Deletes a submission by its unique identifier.
   *
   * @param submissionId - the unique identifier of the submission
   * @return whether the submission was deleted
   */
  public boolean deleteSubmission(long submissionId) {
    submissionMapper.deleteSubmission(submissionId);
    return true;
  }

  /** The autowired SubmissionMapper object. */
  @Autowired private SubmissionMapper submissionMapper;

  /** The autowired ProblemMapper object. */
  @Autowired private ProblemMapper problemMapper;

  /** The autowired LanguageMapper object. */
  @Autowired private LanguageMapper languageMapper;

  /** The autowired MessageSender object. */
  @Autowired private MessageSender messageSender;
}
