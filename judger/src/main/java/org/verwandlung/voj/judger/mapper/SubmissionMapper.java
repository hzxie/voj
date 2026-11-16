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
   * Claims a pending submission for a judging: sets the claim token, which identifies the judging,
   * the start time of the judging, and the judger. The start time and the lease, which the judging
   * renews (see {@link #renewLease}), are those of the database's clock.
   *
   * <p>The web application counts the judgings asked for in {@code submission_judge_generation}:
   * a rejudge increments it, and every judging task carries the generation it was queued with. A
   * task whose generation is no longer the submission's is stale (a rejudge queued a newer one),
   * so it claims nothing. A task delivered again (e.g. its judger died) claims the submission
   * anew, which supersedes the judging of the earlier delivery.
   *
   * @param submissionId - the unique identifier of the submission
   * @param generation - the judging generation of the task
   * @param claimToken - the claim token of the judging, a random number
   * @param judgerUid - the uid of the judger, or null if unknown
   * @return 1 if the submission was pending (at that generation) and is now claimed, 0 otherwise
   */
  @Update(
      "UPDATE voj_submissions SET submission_claim_token = #{claimToken},"
          + " submission_execute_time = NOW(), submission_lease_time = NOW(),"
          + " judger_uid = #{judgerUid}"
          + " WHERE submission_id = #{submissionId} AND submission_judge_result = 'PD'"
          + " AND submission_judge_generation = #{generation}")
  int claimSubmission(
      @Param("submissionId") long submissionId,
      @Param("generation") long generation,
      @Param("claimToken") long claimToken,
      @Param("judgerUid") Long judgerUid);

  /**
   * Renews the lease of a judging, which tells the web application that the judging is still
   * going on: a pending submission whose lease is not renewed in time is a System Error. The count
   * is of the rows found, not of those changed (the default of the MySQL driver), so a renewal
   * within the same second as the previous one counts too.
   *
   * @param submissionId - the unique identifier of the submission
   * @param claimToken - the claim token of the judging
   * @return 1 if the submission is still pending and claimed by the judging, 0 if it was rejudged,
   *     claimed by another judging or failed meanwhile: the judging was superseded
   */
  @Update(
      "UPDATE voj_submissions SET submission_lease_time = NOW()"
          + " WHERE submission_id = #{submissionId} AND submission_judge_result = 'PD'"
          + " AND submission_claim_token = #{claimToken}")
  int renewLease(@Param("submissionId") long submissionId, @Param("claimToken") long claimToken);

  /**
   * Releases the claim of a judging that hands its task back to be delivered again: the
   * submission waits for a judger again, as before the claim.
   *
   * @param submissionId - the unique identifier of the submission
   * @param claimToken - the claim token of the judging
   * @return 1 if the claim was released, 0 if the judging was superseded
   */
  @Update(
      "UPDATE voj_submissions SET submission_claim_token = NULL"
          + " WHERE submission_id = #{submissionId} AND submission_judge_result = 'PD'"
          + " AND submission_claim_token = #{claimToken}")
  int releaseSubmission(
      @Param("submissionId") long submissionId, @Param("claimToken") long claimToken);

  /**
   * Records the verdict of a judging: only the result columns (never e.g. the code), and only if
   * the submission is still pending and claimed by that judging.
   *
   * @param submission - the submission, with the claim token of the judging
   * @return 1 if the verdict was recorded, 0 if the judging was superseded
   */
  @Update(
      "UPDATE voj_submissions SET submission_used_time = #{usedTime},"
          + " submission_used_memory = #{usedMemory}, submission_judge_score = #{judgeScore},"
          + " submission_judge_result = #{judgeResultSlug}, submission_judge_log = #{judgeLog}"
          + " WHERE submission_id = #{submissionId} AND submission_judge_result = 'PD'"
          + " AND submission_claim_token = #{claimToken}")
  int updateSubmissionResult(Submission submission);
}
