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
package org.verwandlung.voj.web.mapper;

import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Param;

import org.verwandlung.voj.web.model.Submission;

/**
 * Submission Data Access Object.
 *
 * @author Haozhe Xie
 */
public interface SubmissionMapper {
  /**
   * [For administrators only] Gets the number of submissions within a specified time period.
   *
   * @param startTime - the start time of the statistics
   * @param endTime - the end time of the statistics
   * @return the number of submissions within the specified time period
   */
  long getNumberOfSubmissionsUsingDate(
      @Param("startTime") String startTime, @Param("endTime") String endTime);

  /**
   * Gets the number of submissions by a user for a problem.
   *
   * @param problemId - the unique identifier of the problem
   * @param username - the username of the user
   * @param judgeResult - the slug of the judge result to filter by, or empty for all
   * @return the number of submissions by the user for the problem
   */
  long getNumberOfSubmissionsUsingProblemIdAndUsername(
      @Param("problemId") long problemId,
      @Param("username") String username,
      @Param("judgeResult") String judgeResult);

  /**
   * [For administrators only] Gets the number of submissions for a programming language.
   *
   * @param languageId - the unique identifier of the programming language
   * @return the number of submissions for the programming language
   */
  long getNumberOfSubmissionsUsingLanguage(@Param("languageId") int languageId);

  /**
   * Gets the number of submissions within a specified time period, grouped by month.
   *
   * @param startTime - the start time of the statistics
   * @param endTime - the end time of the statistics
   * @param uid - the unique identifier of the user
   * @param isAcceptedOnly - whether to count only accepted submissions
   * @return a Map of key-value pairs containing the month and the number of submissions
   */
  List<Map<String, Object>> getNumberOfSubmissionsGroupByMonth(
      @Param("startTime") String startTime,
      @Param("endTime") String endTime,
      @Param("uid") long uid,
      @Param("isAcceptedOnly") boolean isAcceptedOnly);

  /**
   * Gets the number of submissions within a specified time period, grouped by day.
   *
   * @param startTime - the start time of the statistics
   * @param endTime - the end time of the statistics
   * @param uid - the unique identifier of the user
   * @param isAcceptedOnly - whether to count only accepted submissions
   * @param runningContestsAt - when not null, the submissions made in a contest that has not ended
   *     at this instant are left out (they are not shown to other users before the contest ends)
   * @return a Map of key-value pairs containing the date and the number of submissions
   */
  List<Map<String, Object>> getNumberOfSubmissionsGroupByDay(
      @Param("startTime") String startTime,
      @Param("endTime") String endTime,
      @Param("uid") long uid,
      @Param("isAcceptedOnly") boolean isAcceptedOnly,
      @Param("runningContestsAt") Date runningContestsAt);

  /**
   * [For administrators only] Gets the number of submissions within a specified time period, grouped
   * by judge result. Feeds the dashboard's verdict-mix breakdown.
   *
   * @param startTime - the start time of the statistics
   * @param endTime - the end time of the statistics
   * @return a list of {slug, submissions} rows, one per judge-result slug
   */
  List<Map<String, Object>> getNumberOfSubmissionsGroupByJudgeResult(
      @Param("startTime") String startTime, @Param("endTime") String endTime);

  /**
   * [For administrators only] Gets the number of submissions still waiting to be judged (the judge
   * queue depth).
   *
   * @return the number of pending submissions
   */
  long getNumberOfPendingSubmissions();

  /**
   * Gets the unique identifier of the latest submission.
   *
   * @return the unique identifier of the latest submission
   */
  long getLatestSubmissionId();

  /**
   * Gets a submission object by its unique identifier.
   *
   * @param submissionId - the unique identifier of the submission
   * @return a submission object
   */
  Submission getSubmission(@Param("submissionId") long submissionId);

  /**
   * Gets the submissions of a user for a problem.
   *
   * @param problemId - the unique identifier of the problem
   * @param username - the username of the user
   * @param limit - the number of submissions to load each time
   * @param isPublicOnly - when {@code true} (a viewer who is not an administrator), submissions to
   *     unpublished problems are excluded, and so are the submissions made in a contest that has not
   *     ended at {@code now} unless they are the viewer's own
   * @param viewerUid - the unique identifier of the viewer (0 for an anonymous viewer)
   * @param now - the current time
   * @return all submissions within a range
   */
  List<Submission> getSubmissions(
      @Param("problemId") long problemId,
      @Param("username") String username,
      @Param("limit") int limit,
      @Param("isPublicOnly") boolean isPublicOnly,
      @Param("viewerUid") long viewerUid,
      @Param("now") Date now);

  /**
   * [For administrators only] Gets a page of the submissions matching the filters, newest first.
   * Uses the same filters as {@link #getNumberOfSubmissionsUsingProblemIdAndUsername}, so a pager
   * built on the two stays consistent.
   *
   * @param problemId - the unique identifier of the problem (0 = no filter)
   * @param username - the username of the user (empty = no filter)
   * @param judgeResult - the slug of the judge result to filter by, or empty for all
   * @param offset - the number of matching submissions to skip (zero-based)
   * @param limit - the number of submissions to fetch
   * @return the submissions on the requested page
   */
  List<Submission> getSubmissionsUsingFilters(
      @Param("problemId") long problemId,
      @Param("username") String username,
      @Param("judgeResult") String judgeResult,
      @Param("offset") long offset,
      @Param("limit") int limit);

  /**
   * Gets the submissions of a user for a problem.
   *
   * @param problemId - the unique identifier of the problem
   * @param username - the username of the user
   * @param judgeResult - the slug of the judge result to filter by, or empty for all
   * @param offset - the starting number of the submission identifier
   * @param limit - the number of submissions to load each time
   * @param isPublicOnly - when {@code true} (a viewer who is not an administrator), submissions to
   *     unpublished problems are excluded, and so are the submissions made in a contest that has not
   *     ended at {@code now} unless they are the viewer's own
   * @param viewerUid - the unique identifier of the viewer (0 for an anonymous viewer)
   * @param now - the current time
   * @return all submissions within a range
   */
  List<Submission> getSubmissionsUsingOffset(
      @Param("problemId") long problemId,
      @Param("username") String username,
      @Param("judgeResult") String judgeResult,
      @Param("submissionId") long offset,
      @Param("limit") int limit,
      @Param("isPublicOnly") boolean isPublicOnly,
      @Param("viewerUid") long viewerUid,
      @Param("now") Date now);

  /**
   * Gets the submissions of a user for a problem.
   *
   * @param problemId - the unique identifier of the problem
   * @param username - the username of the user
   * @param offset - the starting number of the submission identifier
   * @param limit - the number of submissions to load each time
   * @param isPublicOnly - when {@code true} (a viewer who is not an administrator), submissions to
   *     unpublished problems are excluded, and so are the submissions made in a contest that has not
   *     ended at {@code now} unless they are the viewer's own
   * @param viewerUid - the unique identifier of the viewer (0 for an anonymous viewer)
   * @param now - the current time
   * @return all submissions within a range
   */
  List<Submission> getLatestSubmissionsUsingOffset(
      @Param("problemId") long problemId,
      @Param("username") String username,
      @Param("submissionId") long offset,
      @Param("limit") int limit,
      @Param("isPublicOnly") boolean isPublicOnly,
      @Param("viewerUid") long viewerUid,
      @Param("now") Date now);

  /**
   * Gets the submissions of a user for a problem.
   *
   * @param problemId - the unique identifier of the problem
   * @param uid - the unique identifier of the user
   * @param limit - the number of submissions to load each time
   * @return the submissions of the user for the problem
   */
  List<Submission> getSubmissionUsingProblemIdAndUserId(
      @Param("problemId") long problemId, @Param("uid") long uid, @Param("limit") int limit);

  /**
   * Gets the latest judging results of a user within a problem ID range.
   *
   * @param uid - the unique identifier of the user
   * @param problemIdLowerBound - the lower bound of the problem ID range
   * @param problemIdUpperBound - the upper bound of the problem ID range
   * @return the latest judging results within the problem ID range
   */
  List<Submission> getLatestSubmissionOfProblems(
      @Param("uid") long uid,
      @Param("problemIdLowerBound") long problemIdLowerBound,
      @Param("problemIdUpperBound") long problemIdUpperBound);

  /**
   * Gets the accepted judging results of a user within a problem ID range.
   *
   * @param uid - the unique identifier of the user
   * @param problemIdLowerBound - the lower bound of the problem ID range
   * @param problemIdUpperBound - the upper bound of the problem ID range
   * @return the accepted judging results within the problem ID range
   */
  List<Submission> getAcceptedSubmissionOfProblems(
      @Param("uid") long uid,
      @Param("problemIdLowerBound") long problemIdLowerBound,
      @Param("problemIdUpperBound") long problemIdUpperBound);

  /**
   * Gets the number of distinct published problems a user has solved. Problems that are not
   * published (draft / hidden, e.g. contest-only problems) are not counted, so the figure can be
   * shown on public pages such as the user profile.
   *
   * @param uid - the unique identifier of the user
   * @param runningContestsAt - when not null, the submissions made in a contest that has not ended
   *     at this instant are left out (they are not shown to other users before the contest ends)
   * @return the number of distinct published problems the user has solved
   */
  long getAcceptedSubmissionUsingUserId(
      @Param("uid") long uid, @Param("runningContestsAt") Date runningContestsAt);

  /**
   * Gets the total number of judged submissions of a user to published problems (pending and
   * system-error submissions, and submissions to draft / hidden problems, are not counted).
   *
   * @param uid - the unique identifier of the user
   * @param runningContestsAt - when not null, the submissions made in a contest that has not ended
   *     at this instant are left out (they are not shown to other users before the contest ends)
   * @return the total number of judged submissions of the user to published problems
   */
  long getTotalSubmissionUsingUserId(
      @Param("uid") long uid, @Param("runningContestsAt") Date runningContestsAt);

  /**
   * Gets a user's public-problem counts broken down by difficulty level. Each row carries the
   * difficulty's slug and name, the number of public problems at that difficulty (total) and the
   * number the user has an accepted submission for (solved).
   *
   * @param uid - the unique identifier of the user
   * @param runningContestsAt - when not null, the submissions made in a contest that has not ended
   *     at this instant are left out (they are not shown to other users before the contest ends)
   * @return an ordered list of per-difficulty rows (slug, name, total, solved)
   */
  List<Map<String, Object>> getNumberOfSolvedProblemsByDifficulty(
      @Param("uid") long uid, @Param("runningContestsAt") Date runningContestsAt);

  /**
   * Gets the number of distinct solved (accepted) problems for every user who has at least one
   * accepted submission. Used to populate the admin user list's SOLVED and RANK columns in a single
   * query (rather than one query per user).
   *
   * @return a list of rows, each carrying the user identifier (uid) and the number of distinct
   *     solved problems (solved), ordered by solved count descending
   */
  List<Map<String, Object>> getSolvedProblemCountForAllUsers();

  /**
   * Gets the number of distinct problems a user has solved (any problem, like {@link
   * #getSolvedProblemCountForAllUsers}).
   *
   * @param uid - the unique identifier of the user
   * @return the number of distinct problems the user has solved
   */
  long getNumberOfSolvedProblemsOfUser(@Param("uid") long uid);

  /**
   * Gets the number of users who have solved more distinct problems than the given number.
   *
   * @param numberOfSolvedProblems - the number of solved problems to compare with
   * @return the number of users who solved more problems
   */
  long getNumberOfUsersSolvedMoreProblems(
      @Param("numberOfSolvedProblems") long numberOfSolvedProblems);

  /**
   * Creates a submission.
   *
   * @param submission - the submission object to create
   */
  int createSubmission(Submission submission);

  /**
   * Updates a submission.
   *
   * @param submission - the submission object to update
   */
  int updateSubmission(Submission submission);

  /**
   * Marks a still-pending submission as a terminal failure (used when a judging task expires with no
   * available judger). Only rows whose result is still pending are updated, so a real verdict is
   * never overwritten.
   *
   * @param submissionId - the unique identifier of the submission
   * @param judgeResultSlug - the terminal judge-result slug to set
   * @param judgeLog - the judge log explaining the failure
   * @return the number of rows updated (1 when the submission was still pending, 0 otherwise)
   */
  int markSubmissionUnjudgeable(
      @Param("submissionId") long submissionId,
      @Param("judgeResultSlug") String judgeResultSlug,
      @Param("judgeLog") String judgeLog);

  /**
   * Resets a submission back to the pending state so it can be re-judged, clearing the previous
   * verdict, judge log, resource usage, score, executing judger and execute time. Used by the
   * rejudge flow: without this, the record would keep its old verdict, the detail pages would not
   * show the live judging stream, and the expired-task guard (which only touches pending rows) could
   * never mark it as unjudgeable.
   *
   * @param submissionId - the unique identifier of the submission
   * @return the number of rows updated
   */
  int resetSubmissionToPending(@Param("submissionId") long submissionId);

  /**
   * Deletes a submission by its unique identifier.
   *
   * @param submissionId - the unique identifier of the submission
   */
  int deleteSubmission(@Param("submissionId") long submissionId);
}
