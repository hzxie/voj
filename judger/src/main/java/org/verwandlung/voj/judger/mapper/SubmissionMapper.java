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

import java.util.List;

import org.apache.ibatis.annotations.One;
import org.apache.ibatis.annotations.Options;
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
   * How long (seconds) a statement of a judging that holds a submission may take. The JDBC
   * driver's socket timeout ends a statement on a connection that went silent (e.g. a half-open
   * one); this ends one that waits on a lock, so that neither the renewals of the lease nor the
   * outcome of a judging wait for longer than its lease.
   */
  int STATEMENT_TIMEOUT_SECONDS = 10;

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
    @Result(property = "failedJudgings", column = "submission_judge_attempts"),
    @Result(property = "lastFailure", column = "submission_judge_failure"),
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
   * Gets the next submission to judge in one of some languages: the pending submission that was
   * queued first (submitted, rejudged, or released after a failed judging) and that no judging
   * holds, i.e. none claimed it, or the lease of the one that did expired (by the database's
   * clock). A plain read, which locks nothing: several judgers may get the same submission, and the
   * first to claim it judges it. The index of the judging queue (judge result, queued time,
   * language, lease) answers it alone: the pending submissions in other languages, or held by a
   * judging, are skipped in the index, without reading their rows.
   *
   * @param languageIds - the unique identifiers of the languages, not empty
   * @return the unique identifier of the submission, or null if there is none
   */
  @Select({
    "<script>",
    "SELECT submission_id FROM voj_submissions WHERE submission_judge_result = 'PD'",
    " AND language_id IN",
    " <foreach item='languageId' collection='languageIds' open='(' separator=',' close=')'>",
    "#{languageId}</foreach>",
    " AND (submission_lease_expiry_time IS NULL OR submission_lease_expiry_time &lt; NOW())",
    " ORDER BY submission_queued_time LIMIT 1",
    "</script>"
  })
  Long getNextSubmissionId(@Param("languageIds") List<Integer> languageIds);

  /**
   * Claims a pending submission that no judging holds (see {@link #getNextSubmissionId}) for a
   * judging: sets the claim token, which identifies the judging, the start time of the judging,
   * the expiry of its lease, which it renews while it runs (see {@link #renewLease}), and the
   * judger. The times are those of the database's clock. One statement, by primary key, that
   * checks the submission again: of the judgers that try to claim a submission at the same time,
   * one does.
   *
   * <p>A submission that still has a claim token was held by a judging whose lease expired: its
   * judger stopped (or was killed, or lost the database) while judging it. That judging failed, which is counted here, with its reason (both are set first,
   * while the claim token is still the old one). A submission a judging released (see {@link
   * #releaseSubmission} and {@link #requeueSubmission}), or that was rejudged, has none.
   *
   * @param submissionId - the unique identifier of the submission
   * @param claimToken - the claim token of the judging, a random number
   * @param judgerUid - the uid of the judger, or null if unknown
   * @param leaseSeconds - how long the lease lasts, in seconds
   * @param interruptedReason - the reason of a failed judging whose lease expired
   * @return 1 if the submission is now claimed by the judging, 0 if it was not to be claimed (any
   *     more)
   */
  @Update(
      "UPDATE voj_submissions SET"
          + " submission_judge_failure = IF(submission_claim_token IS NULL,"
          + " submission_judge_failure, #{interruptedReason}),"
          + " submission_judge_attempts ="
          + " submission_judge_attempts + (submission_claim_token IS NOT NULL),"
          + " submission_claim_token = #{claimToken},"
          + " submission_execute_time = NOW(),"
          + " submission_lease_expiry_time = NOW() + INTERVAL #{leaseSeconds} SECOND,"
          + " judger_uid = #{judgerUid}"
          + " WHERE submission_id = #{submissionId} AND submission_judge_result = 'PD'"
          + " AND (submission_lease_expiry_time IS NULL OR submission_lease_expiry_time < NOW())")
  int claimSubmission(
      @Param("submissionId") long submissionId,
      @Param("claimToken") long claimToken,
      @Param("judgerUid") Long judgerUid,
      @Param("leaseSeconds") long leaseSeconds,
      @Param("interruptedReason") String interruptedReason);

  /**
   * Renews the lease of a judging, so that no other judging claims the submission while it runs.
   * The count is of the rows found, not of those changed (the default of the MySQL driver), so a
   * renewal within the same second as the previous one counts too.
   *
   * @param submissionId - the unique identifier of the submission
   * @param claimToken - the claim token of the judging
   * @param leaseSeconds - how long the lease lasts from now, in seconds
   * @return 1 if the submission is still pending and claimed by the judging, 0 if it was rejudged
   *     or claimed by another judging meanwhile: the judging was superseded
   */
  @Update(
      "UPDATE voj_submissions"
          + " SET submission_lease_expiry_time = NOW() + INTERVAL #{leaseSeconds} SECOND"
          + " WHERE submission_id = #{submissionId} AND submission_judge_result = 'PD'"
          + " AND submission_claim_token = #{claimToken}")
  @Options(timeout = STATEMENT_TIMEOUT_SECONDS)
  int renewLease(
      @Param("submissionId") long submissionId,
      @Param("claimToken") long claimToken,
      @Param("leaseSeconds") long leaseSeconds);

  /**
   * Releases a submission whose judging stops as its judger shuts down, or failed for a reason of
   * the judger or its environment: it is pending and held by no judging again, at its place in the
   * queue, so that the next judger that looks takes it. The judging is not counted as a failed one.
   *
   * @param submissionId - the unique identifier of the submission
   * @param claimToken - the claim token of the judging
   * @return 1 if the submission was released, 0 if the judging was superseded meanwhile
   */
  @Update(
      "UPDATE voj_submissions SET submission_claim_token = NULL,"
          + " submission_lease_expiry_time = NULL, submission_execute_time = NULL,"
          + " judger_uid = NULL"
          + " WHERE submission_id = #{submissionId} AND submission_judge_result = 'PD'"
          + " AND submission_claim_token = #{claimToken}")
  @Options(timeout = STATEMENT_TIMEOUT_SECONDS)
  int releaseSubmission(
      @Param("submissionId") long submissionId, @Param("claimToken") long claimToken);

  /**
   * Releases a submission whose judging failed in a way it may have caused (see {@code
   * Dispatcher}): it is pending and held by no judging again, at the end of the queue (its queued time is now), so that it holds no other
   * submission up, and the failed judging is counted, with its reason.
   *
   * @param submissionId - the unique identifier of the submission
   * @param claimToken - the claim token of the judging
   * @param reason - why the judging failed, at most 255 characters
   * @return 1 if the submission was released, 0 if the judging was superseded meanwhile
   */
  @Update(
      "UPDATE voj_submissions SET submission_judge_attempts = submission_judge_attempts + 1,"
          + " submission_judge_failure = #{reason}, submission_queued_time = NOW(),"
          + " submission_claim_token = NULL, submission_lease_expiry_time = NULL,"
          + " submission_execute_time = NULL, judger_uid = NULL"
          + " WHERE submission_id = #{submissionId} AND submission_judge_result = 'PD'"
          + " AND submission_claim_token = #{claimToken}")
  @Options(timeout = STATEMENT_TIMEOUT_SECONDS)
  int requeueSubmission(
      @Param("submissionId") long submissionId,
      @Param("claimToken") long claimToken,
      @Param("reason") String reason);

  /**
   * Records the verdict of a judging: only the result columns (never e.g. the code), and only if
   * the submission is still claimed by that judging. The claim token stays, so recording the same
   * verdict again (e.g. as the answer to the first statement was lost) finds the submission and
   * changes nothing; a rejudge, and the claim of another judging, replace the token. The count is
   * of the rows found (see {@link #renewLease}).
   *
   * @param submission - the submission, with the claim token of the judging
   * @return 1 if the verdict was recorded, 0 if the judging was superseded
   */
  @Update(
      "UPDATE voj_submissions SET submission_used_time = #{usedTime},"
          + " submission_used_memory = #{usedMemory}, submission_judge_score = #{judgeScore},"
          + " submission_judge_result = #{judgeResultSlug}, submission_judge_log = #{judgeLog}"
          + " WHERE submission_id = #{submissionId} AND submission_claim_token = #{claimToken}")
  @Options(timeout = STATEMENT_TIMEOUT_SECONDS)
  int updateSubmissionResult(Submission submission);
}
