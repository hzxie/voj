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
   * #1000, set back to pending. Expected result: it is claimed once, its verdict is recorded with
   * the result columns only (the code and the rest stay as they were), and a submission that is
   * no longer pending can be claimed by nobody.
   */
  @Test
  public void testClaimAndRecordVerdict() {
    Submission before = setPending(1000);
    Date startTime = startTime(0);

    Assertions.assertEquals(1, submissionMapper.claimSubmission(1000, startTime, 1000L));
    Assertions.assertEquals(1, submissionMapper.updateSubmissionResult(verdict(1000, startTime)));

    Submission after = submissionMapper.getSubmission(1000);
    Assertions.assertEquals("WA", after.getJudgeResultSlug());
    Assertions.assertEquals("Wrong Answer.", after.getJudgeLog());
    Assertions.assertEquals(12, after.getUsedTime());
    Assertions.assertEquals(34, after.getUsedMemory());
    Assertions.assertEquals(5, after.getJudgeScore());
    Assertions.assertEquals(startTime.getTime(), after.getExecuteTime().getTime());
    Assertions.assertEquals(1000L, after.getJudgerUid());
    Assertions.assertEquals(before.getCode(), after.getCode());
    Assertions.assertEquals(before.getUid(), after.getUid());
    Assertions.assertEquals(0, submissionMapper.claimSubmission(1000, startTime(1), 1000L));
  }

  /**
   * Test case: tests the updateSubmissionResult method. Test data: a judging whose submission was
   * rejudged (set back to pending) meanwhile, and one whose task was then taken over by another
   * delivery. Expected result: neither stale verdict is recorded; the current judging's is.
   */
  @Test
  public void testStaleVerdictIsDropped() {
    setPending(1000);
    Date firstStartTime = startTime(0);
    Assertions.assertEquals(1, submissionMapper.claimSubmission(1000, firstStartTime, null));

    setPending(1000); // a rejudge, as the web application does it
    Assertions.assertEquals(
        0, submissionMapper.updateSubmissionResult(verdict(1000, firstStartTime)));

    Date secondStartTime = startTime(1);
    Date thirdStartTime = startTime(2);
    Assertions.assertEquals(1, submissionMapper.claimSubmission(1000, secondStartTime, null));
    Assertions.assertEquals(1, submissionMapper.claimSubmission(1000, thirdStartTime, null));
    Assertions.assertEquals(
        0, submissionMapper.updateSubmissionResult(verdict(1000, secondStartTime)));
    Assertions.assertEquals(
        1, submissionMapper.updateSubmissionResult(verdict(1000, thirdStartTime)));
    Assertions.assertEquals("WA", submissionMapper.getSubmission(1000).getJudgeResultSlug());
  }

  /**
   * Sets a submission back to pending, as a rejudge does (resetSubmissionToPending in the web
   * application's SubmissionMapper.xml).
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
                + " judger_uid = NULL WHERE submission_id = ?",
            submissionId);
    return submission;
  }

  /**
   * Gets a distinct start time of a judging, at second precision.
   *
   * @param offsetSeconds - the offset (seconds) from a fixed point in time
   * @return the start time
   */
  private static Date startTime(int offsetSeconds) {
    return new Date((1700000000L + offsetSeconds) * 1000);
  }

  /**
   * Builds the verdict of a judging.
   *
   * @param submissionId - the unique identifier of the submission
   * @param startTime - the start time of the judging
   * @return the submission carrying the verdict
   */
  private static Submission verdict(long submissionId, Date startTime) {
    Submission submission = new Submission();
    submission.setSubmissionId(submissionId);
    submission.setExecuteTime(startTime);
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
