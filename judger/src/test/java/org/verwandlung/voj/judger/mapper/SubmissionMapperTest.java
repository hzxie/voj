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

import javax.sql.DataSource;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

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
   * Test case: tests the claimSubmission and updateSubmissionResult methods. Test data: Submission
   * #1000, set back to pending. Expected result: it is claimed, with the start time and the lease
   * of the database's clock; its verdict is recorded with the result columns only (the code and
   * the rest stay as they were), and a submission that is no longer pending can be claimed by
   * nobody.
   */
  @Test
  public void testClaimAndRecordVerdict() {
    Submission before = setPending(1000);
    long generation = generationOf(1000);

    Assertions.assertEquals(1, submissionMapper.claimSubmission(1000, generation, 7L, 1000L));
    Assertions.assertEquals(
        0,
        new JdbcTemplate(dataSource)
            .queryForObject(
                "SELECT COUNT(*) FROM voj_submissions WHERE submission_id = 1000"
                    + " AND (ABS(TIMESTAMPDIFF(SECOND, submission_execute_time, NOW())) > 60"
                    + " OR submission_lease_time <> submission_execute_time)",
                Integer.class));
    Assertions.assertEquals(1, submissionMapper.updateSubmissionResult(verdict(1000, 7L)));

    Submission after = submissionMapper.getSubmission(1000);
    Assertions.assertEquals("WA", after.getJudgeResultSlug());
    Assertions.assertEquals("Wrong Answer.", after.getJudgeLog());
    Assertions.assertEquals(12, after.getUsedTime());
    Assertions.assertEquals(34, after.getUsedMemory());
    Assertions.assertEquals(5, after.getJudgeScore());
    Assertions.assertEquals(1000L, after.getJudgerUid());
    Assertions.assertEquals(before.getCode(), after.getCode());
    Assertions.assertEquals(before.getUid(), after.getUid());
    Assertions.assertEquals(0, submissionMapper.claimSubmission(1000, generation, 8L, 1000L));
  }

  /**
   * Test case: tests the renewLease and updateSubmissionResult methods. Test data: a judging whose
   * submission was rejudged meanwhile, and one whose task was then claimed by another delivery.
   * Expected result: neither superseded judging renews its lease nor records its verdict; the
   * current judging does both.
   */
  @Test
  public void testSupersededJudgingIsStopped() {
    setPending(1000);
    long staleGeneration = generationOf(1000);
    Assertions.assertEquals(1, submissionMapper.claimSubmission(1000, staleGeneration, 7L, null));
    Assertions.assertEquals(1, submissionMapper.renewLease(1000, 7L));
    // A renewal within the same second as the previous one counts too.
    Assertions.assertEquals(1, submissionMapper.renewLease(1000, 7L));

    setPending(1000); // a rejudge, as the web application does it
    Assertions.assertEquals(0, submissionMapper.renewLease(1000, 7L));
    Assertions.assertEquals(0, submissionMapper.updateSubmissionResult(verdict(1000, 7L)));

    long generation = generationOf(1000);
    Assertions.assertEquals(1, submissionMapper.claimSubmission(1000, generation, 8L, null));
    Assertions.assertEquals(1, submissionMapper.claimSubmission(1000, generation, 9L, null));
    Assertions.assertEquals(0, submissionMapper.renewLease(1000, 8L));
    Assertions.assertEquals(0, submissionMapper.updateSubmissionResult(verdict(1000, 8L)));
    Assertions.assertEquals(1, submissionMapper.renewLease(1000, 9L));
    Assertions.assertEquals(1, submissionMapper.updateSubmissionResult(verdict(1000, 9L)));
    Assertions.assertEquals("WA", submissionMapper.getSubmission(1000).getJudgeResultSlug());
    // A judged submission has no lease to renew.
    Assertions.assertEquals(0, submissionMapper.renewLease(1000, 9L));
  }

  /**
   * Test case: tests the releaseSubmission method. Test data: a claimed submission, released by a
   * judging that was superseded, then by its own. Expected result: only the current judging
   * releases it, after which its verdict is not recorded, and the next delivery of the task claims
   * the submission again.
   */
  @Test
  public void testReleaseSubmission() {
    setPending(1000);
    long generation = generationOf(1000);
    Assertions.assertEquals(1, submissionMapper.claimSubmission(1000, generation, 7L, null));

    Assertions.assertEquals(0, submissionMapper.releaseSubmission(1000, 8L));
    Assertions.assertEquals(1, submissionMapper.releaseSubmission(1000, 7L));
    Assertions.assertEquals(0, submissionMapper.updateSubmissionResult(verdict(1000, 7L)));
    Assertions.assertEquals(1, submissionMapper.claimSubmission(1000, generation, 8L, null));
  }

  /**
   * Test case: tests the claimSubmission method. Test data: a task queued before the submission was
   * rejudged, i.e. of an older generation, and the rejudge's task. Expected result: only the task
   * of the current generation claims it.
   */
  @Test
  public void testStaleGenerationIsNotClaimed() {
    setPending(1000);
    long staleGeneration = generationOf(1000);
    setPending(1000); // a rejudge, as the web application does it
    long generation = generationOf(1000);
    Assertions.assertEquals(staleGeneration + 1, generation);

    Assertions.assertEquals(0, submissionMapper.claimSubmission(1000, staleGeneration, 7L, null));
    Assertions.assertEquals(1, submissionMapper.claimSubmission(1000, generation, 8L, null));
  }

  /**
   * Gets the judging generation of a submission.
   *
   * @param submissionId - the unique identifier of the submission
   * @return its judging generation
   */
  private long generationOf(long submissionId) {
    return new JdbcTemplate(dataSource)
        .queryForObject(
            "SELECT submission_judge_generation FROM voj_submissions WHERE submission_id = ?",
            Long.class,
            submissionId);
  }

  /**
   * Sets a submission back to pending, as a rejudge does (resetSubmissionToPending in the web
   * application's SubmissionMapper.xml), which also counts the judging asked for.
   *
   * @param submissionId - the unique identifier of the submission
   * @return the submission as it was before
   */
  private Submission setPending(long submissionId) {
    Submission submission = submissionMapper.getSubmission(submissionId);
    new JdbcTemplate(dataSource)
        .update(
            "UPDATE voj_submissions SET submission_judge_result = 'PD', submission_judge_log ="
                + " NULL, submission_judge_score = NULL, submission_used_time = NULL,"
                + " submission_used_memory = NULL, submission_execute_time = NULL,"
                + " judger_uid = NULL,"
                + " submission_judge_generation = submission_judge_generation + 1,"
                + " submission_claim_token = NULL, submission_lease_time = NULL,"
                + " submission_queued_time = NOW()"
                + " WHERE submission_id = ?",
            submissionId);
    return submission;
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

  /** The SubmissionMapper object under test. */
  @Autowired private SubmissionMapper submissionMapper;

  /** The data source, to reset a submission as the web application does. */
  @Autowired private DataSource dataSource;
}
