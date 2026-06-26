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
   * @return the number of submissions by the user for the problem
   */
  long getNumberOfSubmissionsUsingProblemIdAndUsername(
      @Param("problemId") long problemId, @Param("username") String username);

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
   * @return a Map of key-value pairs containing the date and the number of submissions
   */
  List<Map<String, Object>> getNumberOfSubmissionsGroupByDay(
      @Param("startTime") String startTime,
      @Param("endTime") String endTime,
      @Param("uid") long uid,
      @Param("isAcceptedOnly") boolean isAcceptedOnly);

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
   * @return all submissions within a range
   */
  List<Submission> getSubmissions(
      @Param("problemId") long problemId,
      @Param("username") String username,
      @Param("limit") int limit);

  /**
   * Gets the submissions of a user for a problem.
   *
   * @param problemId - the unique identifier of the problem
   * @param username - the username of the user
   * @param offset - the starting number of the submission identifier
   * @param limit - the number of submissions to load each time
   * @return all submissions within a range
   */
  List<Submission> getSubmissionsUsingOffset(
      @Param("problemId") long problemId,
      @Param("username") String username,
      @Param("submissionId") long offset,
      @Param("limit") int limit);

  /**
   * Gets the submissions of a user for a problem.
   *
   * @param problemId - the unique identifier of the problem
   * @param username - the username of the user
   * @param offset - the starting number of the submission identifier
   * @param limit - the number of submissions to load each time
   * @return all submissions within a range
   */
  List<Submission> getLatestSubmissionsUsingOffset(
      @Param("problemId") long problemId,
      @Param("username") String username,
      @Param("submissionId") long offset,
      @Param("limit") int limit);

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
   * Gets the number of accepted submissions of a user.
   *
   * @param uid - the unique identifier of the user
   * @return the number of accepted submissions of the user
   */
  long getAcceptedSubmissionUsingUserId(@Param("uid") long uid);

  /**
   * Gets the total number of submissions of a user.
   *
   * @param uid - the unique identifier of the user
   * @return the total number of submissions of the user
   */
  long getTotalSubmissionUsingUserId(@Param("uid") long uid);

  /**
   * Gets a user's public-problem counts broken down by difficulty level. Each row carries the
   * difficulty's slug and name, the number of public problems at that difficulty (total) and the
   * number the user has an accepted submission for (solved).
   *
   * @param uid - the unique identifier of the user
   * @return an ordered list of per-difficulty rows (slug, name, total, solved)
   */
  List<Map<String, Object>> getNumberOfSolvedProblemsByDifficulty(@Param("uid") long uid);

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
   * Deletes a submission by its unique identifier.
   *
   * @param submissionId - the unique identifier of the submission
   */
  int deleteSubmission(@Param("submissionId") long submissionId);
}
