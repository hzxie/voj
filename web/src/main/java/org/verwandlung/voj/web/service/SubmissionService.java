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

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import org.verwandlung.voj.web.mapper.JudgeResultMapper;
import org.verwandlung.voj.web.mapper.LanguageMapper;
import org.verwandlung.voj.web.mapper.OptionMapper;
import org.verwandlung.voj.web.mapper.ProblemMapper;
import org.verwandlung.voj.web.mapper.SubmissionMapper;
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
  /** The judge log of a submission in a language that no judger taking submissions supports. */
  static final String NO_JUDGER_JUDGE_LOG =
      "System Error.\n\nNo running judger that takes submissions supports the language of this"
          + " submission: the judgers that do stopped, or are not ready as their judgings failed"
          + " (see their logs). An administrator can rejudge this submission.\n";

  /** How often (ms) the pending submissions in a language without a judger are looked for. */
  static final long NO_JUDGER_CHECK_INTERVAL_MS = 30000;

  /**
   * [For administrators only] Gets the number of submissions within a specified time period.
   *
   * @param startTime - the start time of the statistics
   * @param endTime - the end time of the statistics
   * @return the number of submissions within the specified time period
   */
  public long getNumberOfSubmissionsUsingDate(Date startTime, Date endTime) {
    return submissionMapper.getNumberOfSubmissionsUsingDate(startTime, endTime);
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
   * Feeds the activity heat-map and the dashboard's daily chart. The days are those of the time zone
   * of the web application (the JVM's default time zone), like the "today" counts of the dashboard:
   * the database counts the submissions per quarter of an hour (the offset of every time zone is a
   * multiple of 15 minutes), and those counts are added up into days here.
   *
   * @param startTime - the start time of the statistics
   * @param endTime - the end time of the statistics
   * @param uid - the unique identifier of the user (only the submissions to published problems are
   *     counted, as in the other statistics of a profile), or 0 for every submission
   * @param isAcceptedOnly - whether to count only accepted submissions
   * @param viewer - the user viewing the statistics (may be null); unless it is the user or an
   *     administrator, the submissions made in contests that have not ended are not counted
   * @return a Map of key-value pairs containing the date and the number of submissions
   */
  public Map<String, Long> getNumberOfSubmissionsGroupByDay(
      Date startTime, Date endTime, long uid, boolean isAcceptedOnly, User viewer) {
    // Build an empty, chronologically ordered slot for every day from the start day (whose
    // submissions the query below includes from midnight on) through the end day.
    ZoneId zone = ZoneId.systemDefault();
    LocalDate startDay = startTime.toInstant().atZone(zone).toLocalDate();
    LocalDate endDay = endTime.toInstant().atZone(zone).toLocalDate();
    Map<String, Long> numberOfSubmissions = new LinkedHashMap<String, Long>();
    for (LocalDate day = startDay; !day.isAfter(endDay); day = day.plusDays(1)) {
      numberOfSubmissions.put(day.format(DAY_FORMAT), 0L);
    }

    // Keep the upper bound at the actual instant so today's (partial-day) submissions are counted.
    List<Map<String, Object>> submissions =
        submissionMapper.getNumberOfSubmissionsGroupByDay(
            Date.from(startDay.atStartOfDay(zone).toInstant()),
            endTime,
            uid,
            isAcceptedOnly,
            getRunningContestsCutoff(uid, viewer));

    for (Map<String, Object> e : submissions) {
      long slotStart = ((Number) e.get("slot")).longValue() * SECONDS_PER_SLOT;
      String day = Instant.ofEpochSecond(slotStart).atZone(zone).toLocalDate().format(DAY_FORMAT);
      numberOfSubmissions.merge(day, ((Number) e.get("submissions")).longValue(), Long::sum);
    }
    return numberOfSubmissions;
  }

  /**
   * The length of the time slots in which SubmissionMapper.getNumberOfSubmissionsGroupByDay counts
   * the submissions, in seconds.
   */
  private static final long SECONDS_PER_SLOT = 15 * 60;

  /** The format of the days in the submission statistics. */
  private static final DateTimeFormatter DAY_FORMAT = DateTimeFormatter.ofPattern("yyyy/MM/dd");

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
    // From the start of the start day (in the web application's time zone), and up to the actual
    // end instant so today's submissions are part of the mix.
    ZoneId zone = ZoneId.systemDefault();
    Date startOfStartDay =
        Date.from(startTime.toInstant().atZone(zone).toLocalDate().atStartOfDay(zone).toInstant());
    List<Map<String, Object>> rows =
        submissionMapper.getNumberOfSubmissionsGroupByJudgeResult(startOfStartDay, endTime);

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
   * Creates a submission, which the judgers take from the database once it is committed.
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
    return saveSubmission(user, problem, languageSlug, code);
  }

  /**
   * Validates and stores a submission. The caller is responsible for having checked that the user
   * may submit to the problem (the normal path only allows published problems, the contest path
   * only problems of a running contest the user attends).
   *
   * <p>A stored submission is pending, which queues it for judging: a judger takes it from the
   * database, which it can only once the transaction that stored it, with everything that belongs
   * to it (e.g. its contest record), has committed.
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
   * Tests whether a judging is the current judging of a submission: a judger claims a pending
   * submission by setting its claim token, which identifies the judging; a rejudge and a later
   * claim of the submission (once the lease of the judging expired) replace or clear it. Used to
   * drop the messages of a superseded judging.
   *
   * @param submissionId - the unique identifier of the submission
   * @param claimToken - the claim token of the judging
   * @return whether the submission still carries that claim token
   */
  public boolean isCurrentJudging(long submissionId, long claimToken) {
    Long currentClaimToken = submissionMapper.getClaimTokenOfSubmission(submissionId);
    return currentClaimToken != null && currentClaimToken == claimToken;
  }

  /**
   * Marks as a System Error the pending submissions in a language that no judger taking submissions
   * supports, and has not for {@code judging.noJudgerTimeout} (5 minutes): the judgers that do
   * stopped, or are not ready as too many of their judgings in a row failed (see the judger's
   * {@code Dispatcher}). With a timeout of 0 or less, a submission waits for a judger as long as it
   * takes. A submission that a judging holds (by a lease that has not expired) is left to it, and
   * so is every submission in a language that a running judger takes submissions in, however long
   * the queue: the judgers judge it in turn.
   *
   * <p>Runs every {@value #NO_JUDGER_CHECK_INTERVAL_MS} ms, on every web application instance. The
   * judgers are told by their heartbeats (see {@link JudgerService}), by the database's clock. How
   * long a language has had no judger is what this instance saw itself, run after run (by its own
   * monotonic clock): a run that fails (e.g. the database is unreachable) starts it over, and so
   * does a start of the instance, so that heartbeats that could not be recorded meanwhile (e.g.
   * while the database was down) never make a running judger look stopped, and the judgers that
   * could not reach this instance while it was down have the timeout to come back.
   *
   * <p>The submissions are found by a plain read, which locks nothing, then marked one at a time,
   * by primary key, each in a statement of its own (outside any transaction, so that no lock is
   * held for longer), which checks again that no judging holds it and no judger taking submissions
   * supports its language: instances running this at the same time, a rejudge and a judger's claim
   * meanwhile neither deadlock with it nor lose their change. Its claim token is cleared, so that
   * the messages of a judging that held it are dropped. The pages of a marked submission read its
   * verdict from the database.
   *
   * @return the number of submissions marked
   */
  @Scheduled(fixedDelay = NO_JUDGER_CHECK_INTERVAL_MS)
  @Transactional(propagation = Propagation.NOT_SUPPORTED)
  public synchronized int failSubmissionsWithoutJudger() {
    if (noJudgerTimeout <= 0) {
      return 0;
    }
    try {
      long now = System.nanoTime();
      Set<Integer> languagesOfReadyJudgers = judgerService.getLanguageIdsOfReadyJudgers();
      Map<Integer, Long> languagesWithoutJudger = new HashMap<>();
      for (int languageId : submissionMapper.getLanguageIdsOfWaitingSubmissions()) {
        if (!languagesOfReadyJudgers.contains(languageId)) {
          languagesWithoutJudger.put(
              languageId, languagesWithoutJudgerSince.getOrDefault(languageId, now));
        }
      }
      languagesWithoutJudgerSince.clear();
      languagesWithoutJudgerSince.putAll(languagesWithoutJudger);
      int failed = 0;
      for (Map.Entry<Integer, Long> language : languagesWithoutJudger.entrySet()) {
        if (now - language.getValue() >= TimeUnit.MILLISECONDS.toNanos(noJudgerTimeout)) {
          failed += failSubmissionsWithoutJudger(language.getKey());
        }
      }
      if (isReconciliationFailing) {
        isReconciliationFailing = false;
        LOGGER.info("The pending submissions in languages without a judger are looked for again.");
      }
      return failed;
    } catch (RuntimeException ex) {
      // What was seen is no longer seen continuously.
      languagesWithoutJudgerSince.clear();
      if (!isReconciliationFailing) {
        isReconciliationFailing = true;
        LOGGER.error(
            "Failed to look for the pending submissions in languages without a judger.", ex);
      }
      return 0;
    }
  }

  /**
   * Marks as a System Error the pending submissions in a language that no judging holds, if no
   * judger taking submissions supports the language still.
   *
   * @param languageId - the unique identifier of the language
   * @return the number of submissions marked
   */
  private int failSubmissionsWithoutJudger(int languageId) {
    int failed = 0;
    for (long submissionId : submissionMapper.getWaitingSubmissionIds(languageId)) {
      if (submissionMapper.failSubmissionWithoutJudger(
              submissionId, languageId, JudgerService.JUDGER_TIMEOUT_SECONDS, NO_JUDGER_JUDGE_LOG)
          == 1) {
        ++failed;
      }
    }
    if (failed > 0) {
      LOGGER.warn(
          String.format(
              "Marked %d pending submission(s) in language #%d, which no running judger taking"
                  + " submissions has supported for %d s, as System Error.",
              failed, languageId, noJudgerTimeout / 1000));
    }
    return failed;
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
   * [For administrators only] Rejudges submissions: resets each of them to pending, at the end of
   * the judging queue (see {@link SubmissionMapper#resetSubmissionToPending}), in one transaction.
   *
   * @param submissionIds - the unique identifiers of the submissions
   */
  public void rejudgeSubmissions(List<Long> submissionIds) {
    for (long submissionId : submissionIds) {
      submissionMapper.resetSubmissionToPending(submissionId);
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

  /** The autowired JudgerService object, which tells the languages of the running judgers. */
  @Autowired private JudgerService judgerService;

  /**
   * How long (ms) the pending submissions in a language wait while no judger taking submissions
   * supports it; 0 or less: as long as it takes.
   */
  @Value("${judging.noJudgerTimeout:300000}")
  private long noJudgerTimeout;

  /**
   * The languages of pending submissions that no judger taking submissions supported at the last
   * runs of {@link #failSubmissionsWithoutJudger()}, and since when (by {@link System#nanoTime()}).
   */
  private final Map<Integer, Long> languagesWithoutJudgerSince = new HashMap<>();

  /** Whether the last run of {@link #failSubmissionsWithoutJudger()} failed. */
  private boolean isReconciliationFailing;

  /** The logger. */
  private static final Logger LOGGER = LogManager.getLogger(SubmissionService.class);
}
