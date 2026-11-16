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
import java.util.Map;

import javax.sql.DataSource;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

import org.verwandlung.voj.judger.application.ApplicationDispatcher;
import org.verwandlung.voj.judger.model.Submission;

/**
 * Test class for SubmissionMapper.
 *
 * @author Haozhe Xie
 */
@ExtendWith(SpringExtension.class)
@Transactional
@ContextConfiguration({"classpath:test-spring-context.xml"})
public class SubmissionMapperTest {
  /**
   * Test case: tests the getSubmission(long) method. Test data: the unique identifier of a
   * submission for Problem#1000. Expected result: returns the expected Submission object.
   */
  @Test
  public void testGetSubmissionExists() {
    Submission submission = submissionMapper.getSubmission(1000);
    Assertions.assertNotNull(submission);

    long problemId = submission.getProblem().getProblemId();
    Assertions.assertEquals(1000, problemId);
  }

  /**
   * Test case: tests the getSubmission(long) method. Test data: a non-existent submission
   * identifier. Expected result: returns a null reference.
   */
  @Test
  public void testGetSubmissionNotExists() {
    Submission submission = submissionMapper.getSubmission(0);
    Assertions.assertNull(submission);
  }

  /**
   * Test case: tests the getNextSubmissionId, claimSubmission and updateSubmissionResult methods.
   * Test data: Submission #1000 (in C++), set back to pending. Expected result: it is the next one
   * in C++, and claimed, with the start time and a lease of a minute by the database's clock,
   * without counting a failed judging; no other judging claims it while it holds the lease. Its verdict is recorded
   * with the result columns only (the code and the rest stay as they were), as often as the judging
   * records it, and a submission that is no longer pending is neither next nor claimed by anybody.
   */
  @Test
  public void testClaimAndRecordVerdict() {
    Submission before = setPending(1000, "2020-01-01 00:00:00");

    Assertions.assertEquals(1000L, submissionMapper.getNextSubmissionId(CPP));
    Assertions.assertEquals(1, submissionMapper.claimSubmission(1000, 7L, 1000L, 60, INTERRUPTED));
    Map<String, Object> claim =
        jdbcTemplate()
            .queryForMap(
                "SELECT submission_judge_attempts AS failed_judgings,"
                    + " TIMESTAMPDIFF(SECOND, submission_execute_time, NOW()) AS started,"
                    + " TIMESTAMPDIFF(SECOND, NOW(), submission_lease_expiry_time) AS lease"
                    + " FROM voj_submissions WHERE submission_id = 1000");
    Assertions.assertEquals(0, ((Number) claim.get("failed_judgings")).intValue());
    Assertions.assertTrue(
        Math.abs(((Number) claim.get("started")).longValue()) <= 5, claim.toString());
    Assertions.assertTrue(((Number) claim.get("lease")).longValue() >= 55, claim.toString());
    Assertions.assertNull(submissionMapper.getNextSubmissionId(CPP));
    Assertions.assertEquals(0, submissionMapper.claimSubmission(1000, 8L, 1000L, 60, INTERRUPTED));

    Assertions.assertEquals(1, submissionMapper.updateSubmissionResult(verdict(1000, 7L)));
    // Again, as when the answer to the first statement was lost: still recorded.
    Assertions.assertEquals(1, submissionMapper.updateSubmissionResult(verdict(1000, 7L)));
    Submission after = submissionMapper.getSubmission(1000);
    Assertions.assertEquals("WA", after.getJudgeResultSlug());
    Assertions.assertEquals("Wrong Answer.", after.getJudgeLog());
    Assertions.assertEquals(12, after.getUsedTime());
    Assertions.assertEquals(34, after.getUsedMemory());
    Assertions.assertEquals(5, after.getJudgeScore());
    Assertions.assertEquals(1000L, after.getJudgerUid());
    Assertions.assertEquals(0, after.getFailedJudgings());
    Assertions.assertEquals(before.getCode(), after.getCode());
    Assertions.assertEquals(before.getUid(), after.getUid());
    Assertions.assertNull(submissionMapper.getNextSubmissionId(CPP));
    expireLease(1000);
    Assertions.assertEquals(0, submissionMapper.claimSubmission(1000, 8L, 1000L, 60, INTERRUPTED));
  }

  /**
   * Test case: tests the getNextSubmissionId method. Test data: pending submissions #1000 and #1002
   * in C++, #1001 in Java, queued in the order #1001, #1002, #1000; then #1002 and #1000 queued at
   * the same time. Expected result: the next one is the one queued first in the languages asked
   * for (of those queued at the same time, either); there is none in C.
   */
  @Test
  public void testNextSubmissionIsTheOldestInTheLanguages() {
    setPending(1000, "2020-01-03 00:00:00");
    setPending(1001, "2020-01-01 00:00:00");
    setPending(1002, "2020-01-02 00:00:00");

    Assertions.assertEquals(1002L, submissionMapper.getNextSubmissionId(CPP));
    Assertions.assertEquals(1001L, submissionMapper.getNextSubmissionId(List.of(3)));
    Assertions.assertEquals(1001L, submissionMapper.getNextSubmissionId(List.of(2, 3)));
    Assertions.assertNull(submissionMapper.getNextSubmissionId(List.of(1)));

    setPending(1002, "2020-01-03 00:00:00");
    Assertions.assertTrue(
        List.of(1000L, 1002L).contains(submissionMapper.getNextSubmissionId(CPP)));
  }

  /**
   * Test case: tests the claimSubmission method. Test data: a submission whose judging stopped
   * renewing its lease (e.g. its judger died) until it expired. Expected result: it is the next one
   * again, another judging claims it and counts the first one as failed, with the reason of an
   * interrupted judging, and the first judging neither renews its lease nor records its verdict any
   * more.
   */
  @Test
  public void testExpiredLeaseIsClaimedAgain() {
    setPending(1000, "2020-01-01 00:00:00");
    Assertions.assertEquals(1, submissionMapper.claimSubmission(1000, 7L, null, 60, INTERRUPTED));
    expireLease(1000);

    Assertions.assertEquals(1000L, submissionMapper.getNextSubmissionId(CPP));
    Assertions.assertEquals(1, submissionMapper.claimSubmission(1000, 8L, null, 60, INTERRUPTED));
    Assertions.assertEquals(1, submissionMapper.getSubmission(1000).getFailedJudgings());
    Assertions.assertEquals(INTERRUPTED, submissionMapper.getSubmission(1000).getLastFailure());
    Assertions.assertEquals(0, submissionMapper.renewLease(1000, 7L, 60));
    Assertions.assertEquals(0, submissionMapper.updateSubmissionResult(verdict(1000, 7L)));
    Assertions.assertEquals(1, submissionMapper.renewLease(1000, 8L, 60));
  }

  /**
   * Test case: tests the releaseSubmission method. Test data: a submission claimed by a judging
   * after an interrupted one, which releases it (as its judger shuts down). Expected result: it
   * holds no claim token, lease, start time or judger any more and is the next one again, at its
   * place in the queue; the judging that claims it next counts no further failed judging, and a
   * release by the superseded judging changes nothing.
   */
  @Test
  public void testReleasedSubmissionIsClaimedWithoutCountingAFailedJudging() {
    setPending(1000, "2020-01-01 00:00:00");
    Assertions.assertEquals(1, submissionMapper.claimSubmission(1000, 6L, 1000L, 60, INTERRUPTED));
    expireLease(1000);
    Assertions.assertEquals(1, submissionMapper.claimSubmission(1000, 7L, 1000L, 60, INTERRUPTED));

    Assertions.assertEquals(1, submissionMapper.releaseSubmission(1000, 7L));
    Map<String, Object> released =
        jdbcTemplate()
            .queryForMap(
                "SELECT submission_judge_result, submission_judge_attempts,"
                    + " submission_claim_token, submission_lease_expiry_time,"
                    + " submission_execute_time, judger_uid,"
                    + " submission_queued_time = '2020-01-01 00:00:00' AS is_at_its_place"
                    + " FROM voj_submissions WHERE submission_id = 1000");
    Assertions.assertEquals("PD", released.get("submission_judge_result"));
    Assertions.assertEquals(1, ((Number) released.get("submission_judge_attempts")).intValue());
    Assertions.assertNull(released.get("submission_claim_token"));
    Assertions.assertNull(released.get("submission_lease_expiry_time"));
    Assertions.assertNull(released.get("submission_execute_time"));
    Assertions.assertNull(released.get("judger_uid"));
    Assertions.assertEquals(1, ((Number) released.get("is_at_its_place")).intValue());
    Assertions.assertEquals(1000L, submissionMapper.getNextSubmissionId(CPP));

    Assertions.assertEquals(1, submissionMapper.claimSubmission(1000, 8L, 1000L, 60, INTERRUPTED));
    Assertions.assertEquals(1, submissionMapper.getSubmission(1000).getFailedJudgings());
    Assertions.assertEquals(0, submissionMapper.releaseSubmission(1000, 7L));
    Assertions.assertEquals(1, submissionMapper.renewLease(1000, 8L, 60));
  }

  /**
   * Test case: tests the requeueSubmission method. Test data: pending submissions #1000 and #1002
   * in C++, #1000 queued first and claimed by a judging that fails. Expected result: #1000 is
   * released to the end of the queue, after #1002, with the failed judging counted and its reason;
   * the judging that claims it next counts nothing more, and a requeue by the superseded judging
   * changes nothing.
   */
  @Test
  public void testRequeuedSubmissionGoesToTheEndOfTheQueue() {
    setPending(1000, "2020-01-01 00:00:00");
    setPending(1002, "2020-01-02 00:00:00");
    Assertions.assertEquals(1, submissionMapper.claimSubmission(1000, 7L, 1000L, 60, INTERRUPTED));

    Assertions.assertEquals(1, submissionMapper.requeueSubmission(1000, 7L, "Failed."));
    Submission requeued = submissionMapper.getSubmission(1000);
    Assertions.assertEquals(1, requeued.getFailedJudgings());
    Assertions.assertEquals("Failed.", requeued.getLastFailure());
    Assertions.assertNull(requeued.getJudgerUid());
    Assertions.assertEquals("PD", requeued.getJudgeResultSlug());
    Assertions.assertEquals(1002L, submissionMapper.getNextSubmissionId(CPP));
    Assertions.assertEquals(
        1L,
        jdbcTemplate()
            .queryForObject(
                "SELECT COUNT(*) FROM voj_submissions WHERE submission_id = 1000"
                    + " AND submission_claim_token IS NULL"
                    + " AND submission_lease_expiry_time IS NULL"
                    + " AND ABS(TIMESTAMPDIFF(SECOND, submission_queued_time, NOW())) <= 5",
                Long.class));

    Assertions.assertEquals(1, submissionMapper.claimSubmission(1000, 8L, 1000L, 60, INTERRUPTED));
    Assertions.assertEquals(1, submissionMapper.getSubmission(1000).getFailedJudgings());
    Assertions.assertEquals("Failed.", submissionMapper.getSubmission(1000).getLastFailure());
    Assertions.assertEquals(0, submissionMapper.requeueSubmission(1000, 7L, "Stale."));
    Assertions.assertEquals(1, submissionMapper.renewLease(1000, 8L, 60));
  }

  /**
   * Test case: tests the renewLease and updateSubmissionResult methods. Test data: a judging whose
   * submission was rejudged meanwhile, and the judging that claimed it then. Expected result: the
   * superseded judging neither renews its lease nor records its verdict; the current judging does
   * both, and a renewal by it extends the lease.
   */
  @Test
  public void testSupersededJudgingIsStopped() {
    setPending(1000, "2020-01-01 00:00:00");
    Assertions.assertEquals(1, submissionMapper.claimSubmission(1000, 7L, null, 60, INTERRUPTED));
    Assertions.assertEquals(1, submissionMapper.renewLease(1000, 7L, 60));
    // A renewal within the same second as the previous one counts too.
    Assertions.assertEquals(1, submissionMapper.renewLease(1000, 7L, 60));

    setPending(1000, "2020-01-01 00:00:00"); // a rejudge, as the web application does it
    Assertions.assertEquals(0, submissionMapper.renewLease(1000, 7L, 60));
    Assertions.assertEquals(0, submissionMapper.updateSubmissionResult(verdict(1000, 7L)));

    Assertions.assertEquals(1, submissionMapper.claimSubmission(1000, 9L, null, 60, INTERRUPTED));
    Assertions.assertEquals(0, submissionMapper.getSubmission(1000).getFailedJudgings());
    Assertions.assertEquals(1, submissionMapper.renewLease(1000, 9L, 3600));
    Assertions.assertTrue(
        jdbcTemplate()
                .queryForObject(
                    "SELECT TIMESTAMPDIFF(SECOND, NOW(), submission_lease_expiry_time)"
                        + " FROM voj_submissions WHERE submission_id = 1000",
                    Long.class)
            > 3000);
    Assertions.assertEquals(1, submissionMapper.updateSubmissionResult(verdict(1000, 9L)));
    Assertions.assertEquals("WA", submissionMapper.getSubmission(1000).getJudgeResultSlug());
    // A judged submission has no lease to renew.
    Assertions.assertEquals(0, submissionMapper.renewLease(1000, 9L, 60));
  }

  /**
   * Sets a submission back to pending, as a rejudge does (resetSubmissionToPending in the web
   * application's SubmissionMapper.xml), but queued at a given time.
   *
   * @param submissionId - the unique identifier of the submission
   * @param queuedTime - when it was queued
   * @return the submission as it was before
   */
  private Submission setPending(long submissionId, String queuedTime) {
    Submission submission = submissionMapper.getSubmission(submissionId);
    jdbcTemplate()
        .update(
            "UPDATE voj_submissions SET submission_judge_result = 'PD', submission_judge_log ="
                + " NULL, submission_judge_score = NULL, submission_used_time = NULL,"
                + " submission_used_memory = NULL, submission_execute_time = NULL,"
                + " judger_uid = NULL, submission_queued_time = ?, submission_judge_attempts = 0,"
                + " submission_judge_failure = NULL, submission_claim_token = NULL,"
                + " submission_lease_expiry_time = NULL"
                + " WHERE submission_id = ?",
            queuedTime,
            submissionId);
    clearCachedQueries();
    return submission;
  }

  /**
   * Lets the lease of the judging of a submission expire.
   *
   * @param submissionId - the unique identifier of the submission
   */
  private void expireLease(long submissionId) {
    jdbcTemplate()
        .update(
            "UPDATE voj_submissions SET submission_lease_expiry_time = NOW() - INTERVAL 1 SECOND"
                + " WHERE submission_id = ?",
            submissionId);
    clearCachedQueries();
  }

  /**
   * Clears the query results MyBatis cached in the transaction of the test, after a change made
   * behind its back: any MyBatis update does (this one changes no row).
   */
  private void clearCachedQueries() {
    submissionMapper.renewLease(0, 0, 1);
  }

  /** Gets a JdbcTemplate on the data source, which runs in the transaction of the test. */
  private JdbcTemplate jdbcTemplate() {
    return new JdbcTemplate(dataSource);
  }

  /**
   * Builds the verdict of a judging.
   *
   * @param submissionId - the unique identifier of the submission
   * @param claimToken - the claim token of the judging
   * @return the submission carrying the verdict
   */
  private static Submission verdict(long submissionId, long claimToken) {
    Submission submission = new Submission();
    submission.setSubmissionId(submissionId);
    submission.setClaimToken(claimToken);
    submission.setJudgeResultSlug("WA");
    submission.setJudgeLog("Wrong Answer.");
    submission.setUsedTime(12);
    submission.setUsedMemory(34);
    submission.setJudgeScore(5);
    return submission;
  }

  /** The languages of the tests: C++ only. */
  private static final List<Integer> CPP = List.of(2);

  /** The reason of a judging whose lease expired, as the judgers record it. */
  private static final String INTERRUPTED = ApplicationDispatcher.INTERRUPTED_REASON;

  /** The SubmissionMapper object under test. */
  @Autowired private SubmissionMapper submissionMapper;

  /** The data source, to reset a submission as the web application does. */
  @Autowired private DataSource dataSource;
}
