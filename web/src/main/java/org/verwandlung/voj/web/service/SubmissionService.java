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
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import org.verwandlung.voj.web.mapper.JudgeResultMapper;
import org.verwandlung.voj.web.mapper.LanguageMapper;
import org.verwandlung.voj.web.mapper.OptionMapper;
import org.verwandlung.voj.web.mapper.ProblemMapper;
import org.verwandlung.voj.web.mapper.SubmissionMapper;
import org.verwandlung.voj.web.messenger.MessageSender;
import org.verwandlung.voj.web.model.JudgeResult;
import org.verwandlung.voj.web.model.Language;
import org.verwandlung.voj.web.model.Option;
import org.verwandlung.voj.web.model.Problem;
import org.verwandlung.voj.web.model.PublicationStatus;
import org.verwandlung.voj.web.model.Submission;
import org.verwandlung.voj.web.model.User;
import org.verwandlung.voj.web.util.Pagination;

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
   * @param judgeResult - the slug of the judge result to filter by, or empty for all
   * @return the number of submissions by the user for the problem
   */
  public long getNumberOfSubmissionsUsingProblemIdAndUsername(
      long problemId, String username, String judgeResult) {
    return submissionMapper.getNumberOfSubmissionsUsingProblemIdAndUsername(
        problemId, username, judgeResult);
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
   * @param viewer - the user viewing the statistics (may be null); unless it is the user or an
   *     administrator, the submissions made in contests that have not ended are not counted
   * @return a Map of key-value pairs containing the date and the number of submissions
   */
  public Map<String, Long> getNumberOfSubmissionsGroupByDay(
      Date startTime, Date endTime, long uid, boolean isAcceptedOnly, User viewer) {
    // Build an empty, chronologically ordered slot for every day from the start day (whose
    // submissions the query below includes from midnight on) through the end day.
    Map<String, Long> numberOfSubmissions = new LinkedHashMap<String, Long>();
    SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd");
    Calendar calendar = new GregorianCalendar();
    calendar.setTime(startTime);
    calendar.set(Calendar.HOUR_OF_DAY, 0);
    calendar.set(Calendar.MINUTE, 0);
    calendar.set(Calendar.SECOND, 0);
    calendar.set(Calendar.MILLISECOND, 0);
    while (!calendar.getTime().after(endTime)) {
      numberOfSubmissions.put(sdf.format(calendar.getTime()), (long) 0);
      calendar.add(Calendar.DATE, 1);
    }

    String startTimeString = new SimpleDateFormat("yyyy-MM-dd 00:00:00").format(startTime);
    // Keep the upper bound at the actual instant so today's (partial-day) submissions are counted.
    String endTimeString = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(endTime);
    List<Map<String, Object>> submissions =
        submissionMapper.getNumberOfSubmissionsGroupByDay(
            startTimeString,
            endTimeString,
            uid,
            isAcceptedOnly,
            getRunningContestsCutoff(uid, viewer));

    for (Map<String, Object> e : submissions) {
      String date = ((java.sql.Date) e.get("date")).toString().replace('-', '/');
      long submissionTimes = (Long) e.get("submissions");
      numberOfSubmissions.put(date, submissionTimes);
    }
    return numberOfSubmissions;
  }

  /**
   * [For administrators only] Gets the number of submissions within a time period grouped by judge
   * result, feeding the dashboard's verdict-mix breakdown. The pending ("PD") bucket is dropped so
   * the mix reflects completed judgements only.
   *
   * @param startTime - the start time of the statistics
   * @param endTime - the end time of the statistics
   * @return an ordered map of judge-result slug to submission count, highest count first
   */
  public Map<String, Long> getNumberOfSubmissionsGroupByJudgeResult(Date startTime, Date endTime) {
    String startTimeString = new SimpleDateFormat("yyyy-MM-dd 00:00:00").format(startTime);
    // Keep the upper bound at the actual instant so today's submissions are part of the mix.
    String endTimeString = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(endTime);
    List<Map<String, Object>> rows =
        submissionMapper.getNumberOfSubmissionsGroupByJudgeResult(startTimeString, endTimeString);

    List<Map.Entry<String, Long>> entries = new ArrayList<>();
    for (Map<String, Object> row : rows) {
      String slug = (String) row.get("slug");
      if (slug == null || "PD".equals(slug)) {
        continue;
      }
      entries.add(new AbstractMap.SimpleEntry<>(slug, ((Number) row.get("submissions")).longValue()));
    }
    entries.sort((a, b) -> Long.compare(b.getValue(), a.getValue()));

    Map<String, Long> result = new LinkedHashMap<>();
    for (Map.Entry<String, Long> entry : entries) {
      result.put(entry.getKey(), entry.getValue());
    }
    return result;
  }

  /**
   * [For administrators only] Gets the judge-queue depth, i.e. the number of submissions still
   * waiting to be judged.
   *
   * @return the number of pending submissions
   */
  public long getNumberOfPendingSubmissions() {
    return submissionMapper.getNumberOfPendingSubmissions();
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
   * Gets the list of submissions a viewer may see: an administrator sees every submission; anybody
   * else neither sees the submissions to unpublished problems nor, until the contest ends, the
   * submissions other users made in a contest (see {@link ContestService#isSubmissionVisible}).
   *
   * @param problemId - the unique identifier of the problem
   * @param username - the username of the user
   * @param limit - the number of submissions to load each time
   * @param viewer - the user viewing the list (may be null)
   * @return the list of submissions (a List<Submission> object)
   */
  public List<Submission> getSubmissions(
      long problemId, String username, int limit, User viewer) {
    return submissionMapper.getSubmissions(
        problemId,
        username,
        limit,
        !PublicationStatus.isAdministrator(viewer),
        getUid(viewer),
        new Date());
  }

  /**
   * Gets the list of submissions a viewer may see (see {@link #getSubmissions(long, String, int,
   * User)}). Used to load submissions asynchronously.
   *
   * @param problemId - the unique identifier of the problem
   * @param username - the username of the user
   * @param offset - the starting number of the submission identifier
   * @param limit - the number of submissions to load each time
   * @param viewer - the user viewing the list (may be null)
   * @return the list of submissions (a List<Submission> object)
   */
  public List<Submission> getSubmissions(
      long problemId, String username, long offset, int limit, User viewer) {
    return submissionMapper.getSubmissionsUsingOffset(
        problemId,
        username,
        "",
        offset,
        limit,
        !PublicationStatus.isAdministrator(viewer),
        getUid(viewer),
        new Date());
  }

  /**
   * Gets the unique identifier of a viewer.
   *
   * @param viewer - the viewer (may be null)
   * @return the unique identifier of the viewer, or 0 for an anonymous viewer
   */
  private static long getUid(User viewer) {
    return viewer == null ? 0 : viewer.getUid();
  }

  /**
   * [For administrators only] Gets a page of the submissions filtered by problem, user and judge
   * result (newest first), including the submissions to unpublished problems. The filters match
   * {@link #getNumberOfSubmissionsUsingProblemIdAndUsername}, so the two can back one pager.
   *
   * @param problemId - the unique identifier of the problem (0 = no filter)
   * @param username - the username of the user (empty = no filter)
   * @param judgeResult - the slug of the judge result to filter by, or empty for all
   * @param pageNumber - the 1-based page number
   * @param pageSize - the number of submissions per page
   * @return the submissions on the requested page
   */
  public List<Submission> getSubmissionsByPage(
      long problemId, String username, String judgeResult, long pageNumber, int pageSize) {
    long offset = Pagination.getOffset(pageNumber, pageSize);
    return submissionMapper.getSubmissionsUsingFilters(
        problemId, username, judgeResult, offset, pageSize);
  }

  /**
   * Gets all judge results. Used to populate the verdict filter on the submission list.
   *
   * @return the list of all judge results
   */
  public List<JudgeResult> getJudgeResults() {
    return judgeResultMapper.getJudgeResults();
  }

  /**
   * Gets the list of latest submissions a viewer may see (see {@link #getSubmissions(long, String,
   * int, User)}). Used to periodically fetch the latest submissions.
   *
   * @param problemId - the unique identifier of the problem
   * @param username - the username of the user
   * @param offset - the starting number of the submission identifier
   * @param limit - the number of submissions to load each time
   * @param viewer - the user viewing the list (may be null)
   * @return the list of submissions (a List<Submission> object)
   */
  public List<Submission> getLatestSubmissions(
      long problemId, String username, long offset, int limit, User viewer) {
    return submissionMapper.getLatestSubmissionsUsingOffset(
        problemId,
        username,
        offset,
        limit,
        !PublicationStatus.isAdministrator(viewer),
        getUid(viewer),
        new Date());
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
   * Gets the instant from which a viewer may not see a user's contest submissions in aggregates:
   * until a contest ends, its submissions are only visible to their owner and administrators (see
   * ContestService#isSubmissionVisible), so they are not counted in the statistics other users see.
   *
   * @param uid - the unique identifier of the user the statistics are about (0 for all users)
   * @param viewer - the user viewing the statistics (may be null)
   * @return the current time when running contests must be left out, otherwise null
   */
  private static Date getRunningContestsCutoff(long uid, User viewer) {
    boolean isOwnStatistics = viewer != null && uid != 0 && viewer.getUid() == uid;
    return isOwnStatistics || PublicationStatus.isAdministrator(viewer) ? null : new Date();
  }

  /**
   * Gets the submission statistics overview of a user.
   *
   * @param userId - the unique identifier of the user
   * @param viewer - the user viewing the statistics (may be null); unless it is the user or an
   *     administrator, the submissions made in contests that have not ended are not counted
   * @return a HashMap containing the user's submission statistics overview
   */
  public Map<String, Long> getSubmissionStatsOfUser(long userId, User viewer) {
    Date runningContestsAt = getRunningContestsCutoff(userId, viewer);
    long acceptedSubmission =
        submissionMapper.getAcceptedSubmissionUsingUserId(userId, runningContestsAt);
    long totalSubmission = submissionMapper.getTotalSubmissionUsingUserId(userId, runningContestsAt);
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
   * @param viewer - the user viewing the statistics (may be null); unless it is the user or an
   *     administrator, the submissions made in contests that have not ended are not counted
   * @return an ordered list of per-difficulty stat maps
   */
  public List<Map<String, Object>> getSolvedProblemsByDifficulty(long userId, User viewer) {
    List<Map<String, Object>> difficulties =
        submissionMapper.getNumberOfSolvedProblemsByDifficulty(
            userId, getRunningContestsCutoff(userId, viewer));
    for (Map<String, Object> difficulty : difficulties) {
      long total = ((Number) difficulty.get("total")).longValue();
      long solved = ((Number) difficulty.get("solved")).longValue();
      difficulty.put("percentage", total == 0 ? 0 : solved * 100 / total);
    }
    return difficulties;
  }

  /**
   * Gets the number of distinct solved (accepted) problems for every user with at least one accepted
   * submission, keyed by user identifier. Backs the admin user list's SOLVED and RANK columns; users
   * absent from the map have solved no problems.
   *
   * @return a map from user identifier to the number of distinct solved problems
   */
  public Map<Long, Long> getSolvedProblemCountForAllUsers() {
    List<Map<String, Object>> rows = submissionMapper.getSolvedProblemCountForAllUsers();
    Map<Long, Long> solvedCounts = new HashMap<>(rows.size() * 4 / 3 + 1);
    for (Map<String, Object> row : rows) {
      long uid = ((Number) row.get("uid")).longValue();
      long solved = ((Number) row.get("solved")).longValue();
      solvedCounts.put(uid, solved);
    }
    return solvedCounts;
  }

  /**
   * Gets the number of distinct problems a user has solved and the user's global rank by it, with
   * the same standard competition ranking as the administration's user list (ties share the
   * lowest rank, e.g. 1, 1, 3), but with two targeted queries instead of the counts of every user.
   *
   * @param userId - the unique identifier of the user
   * @return a Map with {@code solvedCount} and {@code rank} (null when the user has solved nothing,
   *     as such users are not ranked)
   */
  public Map<String, Long> getSolvedCountAndRankOfUser(long userId) {
    long solvedCount = submissionMapper.getNumberOfSolvedProblemsOfUser(userId);
    Map<String, Long> result = new HashMap<>(4, 1);
    result.put("solvedCount", solvedCount);
    result.put(
        "rank",
        solvedCount == 0
            ? null
            : 1 + submissionMapper.getNumberOfUsersSolvedMoreProblems(solvedCount));
    return result;
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
    // An unpublished (draft / hidden) problem only accepts submissions from administrators. For
    // everyone else it is reported as missing, matching the problem page, so this endpoint does not
    // reveal which unpublished problem ids exist. (Contest problems, which are often hidden, are
    // submitted through ContestService.createSubmission instead.)
    if (problem != null && !PublicationStatus.isVisibleTo(problem.getStatus(), user)) {
      problem = null;
    }
    Map<String, Object> result = saveSubmission(user, problem, languageSlug, code);
    if ((Boolean) result.get("isSuccessful")) {
      createSubmissionTask((Long) result.get("submissionId"), languageSlug);
    }
    return result;
  }

  /**
   * Validates and stores a submission without enqueuing its judging task. The caller is
   * responsible for having checked that the user may submit to the problem (the normal path only
   * allows published problems, the contest path only problems of a running contest the user
   * attends) and for calling {@link #createSubmissionTask(long, String)} once everything that
   * belongs to the submission has been stored.
   *
   * @param user - the logged-in user object
   * @param problem - the problem to submit to, or null when it does not exist / may not be used
   * @param languageSlug - the alias of the programming language
   * @param code - the code
   * @return a Map<String, Object> object containing the validation result and, on success, the
   *     unique identifier of the created submission ({@code submissionId})
   */
  public Map<String, Object> saveSubmission(
      User user, Problem problem, String languageSlug, String code) {
    Language language = languageMapper.getLanguageUsingSlug(languageSlug);
    // A disabled language is not offered on the problem page and must not be accepted either.
    if (language != null && !language.isEnabled()) {
      language = null;
    }

    Submission submission = new Submission(problem, user, language, code);

    @SuppressWarnings("unchecked")
    Map<String, Object> result =
        (Map<String, Object>) getSubmissionCreationResult(submission);
    boolean isSuccessful = (Boolean) result.get("isSuccessful");
    if (isSuccessful) {
      submissionMapper.createSubmission(submission);
      result.put("submissionId", submission.getSubmissionId());
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
    Map<String, Boolean> result = new HashMap<>(8, 1);
    String code = submission.getCode();
    result.put("isUserLogined", submission.getUser() != null);
    result.put("isProblemExists", submission.getProblem() != null);
    result.put("isLanguageExists", submission.getLanguage() != null);
    result.put("isCodeEmpty", code == null || code.length() == 0);
    result.put("isEmailVerified", isSubmitterEmailVerified(submission.getUser()));

    boolean isSuccessful =
        result.get("isUserLogined")
            && result.get("isProblemExists")
            && result.get("isLanguageExists")
            && !result.get("isCodeEmpty")
            && result.get("isEmailVerified");
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
    // Rejudge path: clear the previous verdict/log so the record returns to pending before the new
    // task is enqueued. This makes the detail pages stream the live judging result again and lets
    // the expired-task guard fall back to marking it unjudgeable if no judger ever claims it.
    submissionMapper.resetSubmissionToPending(submissionId);
    createSubmissionTask(submissionId, languageSlug);
  }

  /**
   * Creates a judging task and submits the submission information to the message queue.
   *
   * <p>Inside a transaction (the normal case, as this service is transactional) the task is only
   * sent once that transaction has committed: a judger must not be able to fetch a submission that
   * is not visible yet, and a transaction that rolls back must not leave a task behind. Outside a
   * transaction the task is sent immediately. See {@link #sendSubmissionTask(long, String)} for what
   * happens when sending fails.
   *
   * @param submissionId - the unique identifier of the submission
   * @param languageSlug - the slug of the submission's language, used by judgers to claim only the
   *     tasks they can build
   */
  public void createSubmissionTask(long submissionId, String languageSlug) {
    if (TransactionSynchronizationManager.isSynchronizationActive()) {
      TransactionSynchronizationManager.registerSynchronization(
          new TransactionSynchronization() {
            @Override
            public void afterCommit() {
              sendSubmissionTask(submissionId, languageSlug);
            }
          });
    } else {
      sendSubmissionTask(submissionId, languageSlug);
    }
  }

  /**
   * Sends the judging task of a stored submission to the message queue.
   *
   * <p>The submission is already committed at this point, so a failure cannot be rolled back and
   * must not reach the user either: the submission was accepted. Instead of leaving it pending
   * forever (no judger will ever see it, and the expired-task guard only covers tasks that reached
   * the queue), it is marked as a system error in a new transaction, which tells the user what
   * happened and lets an administrator rejudge it once the message queue is back. Retrying here is
   * deliberately avoided: it would hold the request thread while the broker is down, and a retry
   * that only lives in memory is lost on restart anyway.
   *
   * @param submissionId - the unique identifier of the submission
   * @param languageSlug - the slug of the submission's language
   */
  private void sendSubmissionTask(long submissionId, String languageSlug) {
    Map<String, Object> mapMessage = new HashMap<>();
    mapMessage.put("event", "SubmissionCreated");
    mapMessage.put("submissionId", submissionId);

    try {
      messageSender.sendMessage(mapMessage, languageSlug);
    } catch (RuntimeException ex) {
      LOGGER.error(
          String.format(
              "Failed to queue the judging task of submission #%d; marking it as a system error.",
              submissionId),
          ex);
      markSubmissionTaskNotQueued(submissionId);
    }
  }

  /**
   * Marks a still-pending submission as a system error because its judging task could not be sent
   * to the message queue. Runs in a new transaction, as Spring requires for writes made after a
   * commit: the committed transaction still holds its connection at that point, so this briefly
   * takes a second one from the pool. Only this failure path does, and if no connection can be had
   * the error is logged and the submission stays pending, as before.
   *
   * @param submissionId - the unique identifier of the submission
   */
  private void markSubmissionTaskNotQueued(long submissionId) {
    String judgeLog =
        "System Error.\n\nThe judging task could not be sent to the message queue. "
            + "An administrator can rejudge this submission.\n";
    try {
      TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
      transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
      transactionTemplate.executeWithoutResult(
          status -> submissionMapper.markSubmissionUnjudgeable(submissionId, "SE", judgeLog));
    } catch (RuntimeException ex) {
      LOGGER.error(
          String.format("Failed to mark submission #%d as a system error.", submissionId), ex);
    }
  }

  /** Why a judging task ended on the dead-letter queue. */
  public enum JudgingTaskFailure {
    /** No judger took the task before it expired: none supports the submission's language. */
    NO_JUDGER_AVAILABLE(
        "No available judger supports this submission's language, so the judging task expired"
            + " before it could be processed."),

    /** Judgers took the task, but judging it failed every time until the redelivery limit. */
    JUDGING_FAILED_REPEATEDLY(
        "Judging this submission failed repeatedly, so its judging task was given up."
            + " An administrator can rejudge this submission."),

    /** The broker gave the task up for another (or an unrecorded) reason. */
    UNKNOWN(
        "The judging task could not be completed. An administrator can rejudge this submission.");

    /** The explanation shown in the submission's judge log. */
    private final String explanation;

    JudgingTaskFailure(String explanation) {
      this.explanation = explanation;
    }

    /**
     * Gets the judge log of a submission whose judging task failed this way.
     *
     * @return the judge log
     */
    public String getJudgeLog() {
      return "System Error.\n\n" + explanation + "\n";
    }
  }

  /**
   * Marks a still-pending submission as a terminal failure because its judging task ended on the
   * dead-letter queue, with a judge log that says why. Invoked from the dead-letter listener. The
   * update only touches submissions that are still pending, so it never overwrites a real verdict
   * (and is safe to apply more than once); a judging that is still running cannot record its
   * verdict afterwards, as the judger only records a verdict over a pending submission.
   *
   * @param submissionId - the unique identifier of the submission whose task was dead-lettered
   * @param failure - why the task was dead-lettered
   * @return whether the submission was marked (false when it was already judged or does not exist)
   */
  public boolean markSubmissionUnjudgeable(long submissionId, JudgingTaskFailure failure) {
    return submissionMapper.markSubmissionUnjudgeable(submissionId, "SE", failure.getJudgeLog())
        > 0;
  }

  /**
   * Deletes a submission by its unique identifier. A contest submission also leaves its contest
   * (the contest record cascades).
   *
   * @param submissionId - the unique identifier of the submission
   * @return whether the submission existed and was deleted
   */
  public boolean deleteSubmission(long submissionId) {
    return submissionMapper.deleteSubmission(submissionId) > 0;
  }

  /**
   * [For administrators only] Deletes submissions in one transaction: either all of them are
   * deleted or, if one deletion fails, none.
   *
   * @param submissionIds - the unique identifiers of the submissions
   * @return the identifiers of the submissions that existed and were deleted
   */
  public List<Long> deleteSubmissions(List<Long> submissionIds) {
    List<Long> deletedSubmissions = new ArrayList<>();
    for (long submissionId : submissionIds) {
      if (deleteSubmission(submissionId)) {
        deletedSubmissions.add(submissionId);
      }
    }
    return deletedSubmissions;
  }

  /**
   * [For administrators only] Rejudges submissions: resets each of them to pending and, once the
   * single transaction that does so has committed, queues their judging tasks.
   *
   * @param submissionIds - the unique identifiers of the submissions
   */
  public void createSubmissionTasks(List<Long> submissionIds) {
    for (long submissionId : submissionIds) {
      createSubmissionTask(submissionId);
    }
  }

  /**
   * Checks whether the submitter is allowed to submit with respect to email verification. When the
   * {@code requireEmailVerification} option is enabled, only users whose email address has been
   * verified may submit; otherwise everyone may submit.
   *
   * <p>The user is the one of the current request, which CurrentUserRefreshFilter reloaded from the
   * database, so its verification status is current. A banned user never gets here: the filter
   * logs them out, so they submit as nobody ({@code isUserLogined} false).
   *
   * @param user - the submitting user, or null
   * @return whether the submitter satisfies the email-verification requirement
   */
  private boolean isSubmitterEmailVerified(User user) {
    Option option = optionMapper.getOption("requireEmailVerification");
    boolean isRequired = option != null && "1".equals(option.getOptionValue());
    if (!isRequired) {
      return true;
    }
    return user != null && user.isEmailVerified();
  }

  /** The autowired SubmissionMapper object. */
  @Autowired private SubmissionMapper submissionMapper;

  /** The autowired JudgeResultMapper object. Used to populate the verdict filter. */
  @Autowired private JudgeResultMapper judgeResultMapper;

  /** The autowired OptionMapper object. Used to resolve the email-verification requirement. */
  @Autowired private OptionMapper optionMapper;

  /** The autowired ProblemMapper object. */
  @Autowired private ProblemMapper problemMapper;

  /** The autowired LanguageMapper object. */
  @Autowired private LanguageMapper languageMapper;

  /** The autowired MessageSender object. */
  @Autowired private MessageSender messageSender;

  /**
   * The autowired transaction manager. Used to record a failed task submission in a transaction of
   * its own.
   */
  @Autowired private PlatformTransactionManager transactionManager;

  /** The logger. */
  private static final Logger LOGGER = LogManager.getLogger(SubmissionService.class);
}
