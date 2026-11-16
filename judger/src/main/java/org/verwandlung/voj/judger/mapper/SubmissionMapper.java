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
package org.verwandlung.voj.judger.mapper;

import java.util.Date;

import org.apache.ibatis.annotations.One;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import org.verwandlung.voj.judger.model.Language;
import org.verwandlung.voj.judger.model.Problem;
import org.verwandlung.voj.judger.model.Submission;

/**
 * Submission Data Access Object.
 *
 * @author Haozhe Xie
 */
public interface SubmissionMapper {
  /**
   * Gets a submission object by its unique identifier.
   *
   * @param submissionId - the unique identifier of the submission
   * @return a submission object
   */
  @Select("SELECT * FROM voj_submissions WHERE submission_id = #{submissionId}")
  @Results({
    @Result(property = "submissionId", column = "submission_id"),
    @Result(
        property = "problem",
        column = "problem_id",
        javaType = Problem.class,
        one = @One(select = "org.verwandlung.voj.judger.mapper.ProblemMapper.getProblem")),
    @Result(property = "uid", column = "uid"),
    @Result(
        property = "language",
        column = "language_id",
        javaType = Language.class,
        one = @One(select = "org.verwandlung.voj.judger.mapper.LanguageMapper.getLanguageUsingId")),
    @Result(property = "submitTime", column = "submission_submit_time"),
    @Result(property = "executeTime", column = "submission_execute_time"),
    @Result(property = "usedTime", column = "submission_used_time"),
    @Result(property = "usedMemory", column = "submission_used_memory"),
    @Result(property = "judgeResultSlug", column = "submission_judge_result"),
    @Result(property = "judgeScore", column = "submission_judge_score"),
    @Result(property = "judgeLog", column = "submission_judge_log"),
    @Result(property = "judgerUid", column = "judger_uid"),
    @Result(property = "code", column = "submission_code"),
  })
  Submission getSubmission(@Param("submissionId") long submissionId);

  /**
   * Claims a pending submission for a judging: records when it started (which identifies the
   * judging) and the judger.
   *
   * @param submissionId - the unique identifier of the submission
   * @param executeTime - the start time of the judging, at second precision (the column's)
   * @param judgerUid - the uid of the judger, or null if unknown
   * @return 1 if the submission was pending and is now claimed, 0 otherwise
   */
  @Update(
      "UPDATE voj_submissions SET submission_execute_time = #{executeTime},"
          + " judger_uid = #{judgerUid}"
          + " WHERE submission_id = #{submissionId} AND submission_judge_result = 'PD'")
  int claimSubmission(
      @Param("submissionId") long submissionId,
      @Param("executeTime") Date executeTime,
      @Param("judgerUid") Long judgerUid);

  /**
   * Records the verdict of a judging: only the result columns (never e.g. the code), and only if
   * the submission is still pending and claimed by that judging.
   *
   * @param submission - the submission, whose execute time is the start time of the judging
   * @return 1 if the verdict was recorded, 0 if the submission was meanwhile rejudged or claimed by
   *     another judging
   */
  @Update(
      "UPDATE voj_submissions SET submission_used_time = #{usedTime},"
          + " submission_used_memory = #{usedMemory}, submission_judge_score = #{judgeScore},"
          + " submission_judge_result = #{judgeResultSlug}, submission_judge_log = #{judgeLog}"
          + " WHERE submission_id = #{submissionId} AND submission_judge_result = 'PD'"
          + " AND submission_execute_time = #{executeTime}")
  int updateSubmissionResult(Submission submission);
}
