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

import org.apache.ibatis.annotations.Param;
import org.verwandlung.voj.web.model.ContestSubmission;

import java.util.List;

/**
 * ContestSubmission Data Access Object.
 *
 * @author Haozhe Xie
 */
public interface ContestSubmissionMapper {
  /**
   * Gets the submissions of a contest.
   *
   * @param contestId - the unique identifier of the contest
   * @return a List object containing the contest submissions (ContestSubmission)
   */
  List<ContestSubmission> getSubmissionsOfContest(@Param("contestId") long contestId);

  /**
   * Gets the accepted submissions of a contest.
   *
   * @param contestId - the unique identifier of the contest
   * @return a List object containing the accepted contest submissions (ContestSubmission)
   */
  List<ContestSubmission> getAcceptedSubmissionsOfContest(@Param("contestId") long contestId);

  /**
   * Gets the submissions of a contestant in a contest.
   *
   * @param contestId - the unique identifier of the contest
   * @param contestantUid - the unique identifier of the contestant
   * @return a List object containing the contest submissions (ContestSubmission) of the contestant
   */
  List<ContestSubmission> getSubmissionOfContestOfContest(
      @Param("contestId") long contestId, @Param("contestantUid") long contestantUid);

  /**
   * Gets the submissions of a contestant for a problem in a contest.
   *
   * @param contestId - the unique identifier of the contest
   * @param problemId - the unique identifier of the problem
   * @param contestantUid - the unique identifier of the contestant
   * @return a List object containing the contest submissions (ContestSubmission) of the contestant
   */
  List<ContestSubmission> getSubmissionOfContestOfContestProblem(
      @Param("contestId") long contestId,
      @Param("problemId") long problemId,
      @Param("contestantUid") long contestantUid);

  /**
   * Creates a submission.
   *
   * @param contestSubmission - the submission to create
   */
  int createContestSubmission(ContestSubmission contestSubmission);

  /**
   * Deletes a contest submission.
   *
   * @param contestId - the unique identifier of the contest
   * @param submissionId - the unique identifier of the submission
   */
  int deleteContestSubmission(
      @Param("contestId") long contestId, @Param("submissionId") long submissionId);

  /**
   * Deletes all submissions of a contest.
   *
   * @param contestId - the unique identifier of the contest
   * @return the number of submissions deleted
   */
  int deleteSubmissionsOfContest(@Param("contestId") long contestId);
}
