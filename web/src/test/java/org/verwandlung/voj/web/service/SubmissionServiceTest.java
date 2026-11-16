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

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

import org.verwandlung.voj.web.mapper.ContestSubmissionMapper;
import org.verwandlung.voj.web.model.Submission;
import org.verwandlung.voj.web.model.User;
import org.verwandlung.voj.web.model.UserGroup;

/**
 * The test class for SubmissionService.
 *
 * <p>Seed data: user 1000 (zjhzxhz) has 3 submission records (2 AC), user 1001 has 2 submission
 * records (both CE), for a total of 5 submission records (the maximum ID is 1004).
 *
 * @author Haozhe Xie
 */
@ExtendWith(SpringExtension.class)
@Transactional
@ContextConfiguration({"classpath:test-spring-context.xml"})
public class SubmissionServiceTest {
  /** Test case: tests the getNumberOfSubmissionsUsingDate(Date, Date) method. Test data: both the start and end times are null. Expected: the number of all submission records. */
  @Test
  public void testGetNumberOfSubmissionsUsingDate() {
    Assertions.assertEquals(5, submissionService.getNumberOfSubmissionsUsingDate(null, null));
  }

  /** Test case: tests the getNumberOfSubmissionsUsingProblemIdAndUsername(long, String, String) method. Test data: the submissions of user zjhzxhz for problem 1000. Expected: returns 2. */
  @Test
  public void testGetNumberOfSubmissionsUsingProblemIdAndUsername() {
    Assertions.assertEquals(
        2,
        submissionService.getNumberOfSubmissionsUsingProblemIdAndUsername(1000, "zjhzxhz", ""));
  }

  /** Test case: tests the getNumberOfSubmissionsUsingProblemIdAndUsername(long, String, String) method with a verdict filter. Test data: the accepted submissions of user zjhzxhz for problem 1000. Expected: returns 1. */
  @Test
  public void testGetNumberOfSubmissionsUsingProblemIdAndUsernameWithJudgeResult() {
    Assertions.assertEquals(
        1,
        submissionService.getNumberOfSubmissionsUsingProblemIdAndUsername(1000, "zjhzxhz", "AC"));
  }

  /** Test case: tests the getLatestSubmissionId() method. Test data: N/a. Expected: the unique identifier of the latest submission record. */
  @Test
  public void testGetLatestSubmissionId() {
    Assertions.assertEquals(1004, submissionService.getLatestSubmissionId());
  }

  /** Test case: tests the getSubmission(long) method. Test data: an existing submission record identifier. Expected: the corresponding submission record. */
  @Test
  public void testGetSubmissionExists() {
    Submission submission = submissionService.getSubmission(1000);
    Assertions.assertNotNull(submission);
    Assertions.assertEquals(1000, submission.getSubmissionId());
  }

  /** Test case: tests the getSubmission(long) method. Test data: a non-existing submission record identifier. Expected: a null reference. */
  @Test
  public void testGetSubmissionNotExists() {
    Assertions.assertNull(submissionService.getSubmission(0));
  }

  /** Test case: tests the getSubmissions(long, String, int, User) method. Test data: the submissions of user zjhzxhz for problem 1000. Expected: exactly submissions 1000 and 1001, each belonging to problem 1000. */
  @Test
  public void testGetSubmissions() {
    List<Submission> submissions = submissionService.getSubmissions(1000, "zjhzxhz", 10, null);
    Assertions.assertEquals(2, submissions.size());

    // Assert which submissions came back, not just how many: a wrong join or a
    // missing problem/username predicate can return two rows that are the wrong
    // ones, which a size-only check would happily pass.
    java.util.Set<Long> submissionIds = new java.util.HashSet<>();
    for (Submission submission : submissions) {
      submissionIds.add(submission.getSubmissionId());
      Assertions.assertEquals(1000, submission.getProblem().getProblemId());
    }
    Assertions.assertEquals(java.util.Set.of(1000L, 1001L), submissionIds);
  }

  /** Test case: tests the getSubmissionUsingProblemIdAndUserId(long, long, int) method. Test data: the submissions of user 1000 for problem 1000. Expected: exactly submissions 1000 and 1001. */
  @Test
  public void testGetSubmissionUsingProblemIdAndUserId() {
    List<Submission> submissions =
        submissionService.getSubmissionUsingProblemIdAndUserId(1000, 1000, 10);
    Assertions.assertEquals(2, submissions.size());

    // Assert the identity of the rows, not just the count: the (problemId, userId)
    // filter could return two wrong submissions and still pass a size check.
    java.util.Set<Long> submissionIds = new java.util.HashSet<>();
    for (Submission submission : submissions) {
      submissionIds.add(submission.getSubmissionId());
    }
    Assertions.assertEquals(java.util.Set.of(1000L, 1001L), submissionIds);
  }

  /** Test case: tests the getSubmissionStatsOfUser(long) method. Test data: user 1000. Expected: the correct accepted count, total count and acceptance rate. */
  @Test
  public void testGetSubmissionStatsOfUser() {
    Map<String, Long> stats = submissionService.getSubmissionStatsOfUser(1000, null);
    Assertions.assertEquals(2L, stats.get("acceptedSubmission"));
    Assertions.assertEquals(3L, stats.get("totalSubmission"));
    Assertions.assertEquals(66L, stats.get("acRate"));
  }

  /** Test case: tests the getSubmissionStatsOfUser(long) method. Test data: a user with no submission records. Expected: the acceptance rate is 0 and no division-by-zero exception is thrown. */
  @Test
  public void testGetSubmissionStatsOfUserWithoutSubmissions() {
    Map<String, Long> stats = submissionService.getSubmissionStatsOfUser(9999, null);
    Assertions.assertEquals(0L, stats.get("acceptedSubmission"));
    Assertions.assertEquals(0L, stats.get("totalSubmission"));
    Assertions.assertEquals(0L, stats.get("acRate"));
  }

  /** Test case: tests the getSubmissionOfProblems(long, long, long) method. Test data: user 1000 in the problem range [1000, 1001]. Expected: the AC submission record overrides the latest submission record. */
  @Test
  public void testGetSubmissionOfProblems() {
    Map<Long, Submission> submissions = submissionService.getSubmissionOfProblems(1000, 1000, 1001);
    // Although problem 1000 has a latest WA (1001) submission, it should be overridden by the AC
    // (1000) submission.
    Assertions.assertEquals("AC", submissions.get(1000L).getJudgeResult().getJudgeResultSlug());
  }

  /** Test case: tests the createSubmission(...) method. Test data: valid submission data. Expected: creation succeeds and the submission record identifier is returned. */
  @Test
  public void testCreateSubmissionSuccessfully() {
    Map<String, Object> result =
        submissionService.createSubmission(
            userWithUid(1000), 1000, "text/x-java", "public class Main {}");
    Assertions.assertTrue((Boolean) result.get("isSuccessful"));
    Assertions.assertTrue(result.containsKey("submissionId"));
    Assertions.assertEquals(6, submissionService.getNumberOfSubmissionsUsingDate(null, null));
  }

  /** Test case: tests the createSubmission(...) method. Test data: the user is not logged in. Expected: creation fails. */
  @Test
  public void testCreateSubmissionWithoutLogin() {
    Map<String, Object> result =
        submissionService.createSubmission(null, 1000, "text/x-java", "Code");
    Assertions.assertFalse((Boolean) result.get("isUserLogined"));
    Assertions.assertFalse((Boolean) result.get("isSuccessful"));
  }

  /** Test case: tests the createSubmission(...) method. Test data: the problem does not exist. Expected: creation fails. */
  @Test
  public void testCreateSubmissionWithNonExistentProblem() {
    Map<String, Object> result =
        submissionService.createSubmission(userWithUid(1000), 99999, "text/x-java", "Code");
    Assertions.assertFalse((Boolean) result.get("isProblemExists"));
    Assertions.assertFalse((Boolean) result.get("isSuccessful"));
  }

  /** Test case: tests the createSubmission(...) method. Test data: the programming language does not exist. Expected: creation fails. */
  @Test
  public void testCreateSubmissionWithNonExistentLanguage() {
    Map<String, Object> result =
        submissionService.createSubmission(userWithUid(1000), 1000, "not-a-language", "Code");
    Assertions.assertFalse((Boolean) result.get("isLanguageExists"));
    Assertions.assertFalse((Boolean) result.get("isSuccessful"));
  }

  /** Test case: tests the createSubmission(...) method. Test data: the code is empty. Expected: creation fails. */
  @Test
  public void testCreateSubmissionWithEmptyCode() {
    Map<String, Object> result =
        submissionService.createSubmission(userWithUid(1000), 1000, "text/x-java", "");
    Assertions.assertTrue((Boolean) result.get("isCodeEmpty"));
    Assertions.assertFalse((Boolean) result.get("isSuccessful"));
  }

  /**
   * Test case: tests the createSubmission(...) method. Test data: email verification is required
   * and the submitter's address is not verified (a banned user, by contrast, never reaches the
   * service: CurrentUserRefreshFilter logs them out). Expected: creation fails with isEmailVerified
   * false; it succeeds once the address is verified.
   */
  @Test
  public void testCreateSubmissionRequiresVerifiedEmail() {
    new org.springframework.jdbc.core.JdbcTemplate(dataSource)
        .update(
            "UPDATE voj_options SET option_value = '1'"
                + " WHERE option_name = 'requireEmailVerification'");
    User user = userWithUid(1002);
    user.setEmailVerified(false);

    Map<String, Object> result =
        submissionService.createSubmission(user, 1000, "text/x-java", "Code");
    Assertions.assertFalse((Boolean) result.get("isEmailVerified"));
    Assertions.assertFalse((Boolean) result.get("isSuccessful"));

    user.setEmailVerified(true);
    Assertions.assertTrue(
        (Boolean)
            submissionService.createSubmission(user, 1000, "text/x-java", "Code").get("isSuccessful"));
  }

  /** Test case: tests the createSubmission(...) method. Test data: a submission in Java after Java has been disabled. Expected: the language is rejected and creation fails. */
  @Test
  public void testCreateSubmissionWithDisabledLanguage() {
    new org.springframework.jdbc.core.JdbcTemplate(dataSource)
        .update("UPDATE voj_languages SET language_enabled = 0 WHERE language_slug = 'text/x-java'");

    Map<String, Object> result =
        submissionService.createSubmission(userWithUid(1000), 1000, "text/x-java", "Code");
    Assertions.assertFalse((Boolean) result.get("isLanguageExists"));
    Assertions.assertFalse((Boolean) result.get("isSuccessful"));
  }

  /** Test case: tests the createSubmission(...) method. Test data: a regular user submitting to the hidden problem 1002. Expected: the problem is reported as missing and creation fails. */
  @Test
  public void testCreateSubmissionToUnpublishedProblemByUser() {
    Map<String, Object> result =
        submissionService.createSubmission(userWithUid(1002), 1002, "text/x-java", "Code");
    Assertions.assertFalse((Boolean) result.get("isProblemExists"));
    Assertions.assertFalse((Boolean) result.get("isSuccessful"));
  }

  /** Test case: tests the createSubmission(...) method. Test data: an administrator submitting to the hidden problem 1002. Expected: creation succeeds. */
  @Test
  public void testCreateSubmissionToUnpublishedProblemByAdministrator() {
    Map<String, Object> result =
        submissionService.createSubmission(administrator(), 1002, "text/x-java", "Code");
    Assertions.assertTrue((Boolean) result.get("isSuccessful"));
  }

  /** Test case: tests the getSubmissions / getLatestSubmissions methods. Test data: a submission to the hidden problem 1002. Expected: it is listed only when unpublished problems are included. */
  @Test
  public void testGetSubmissionsExcludesUnpublishedProblems() {
    long submissionId =
        (Long)
            submissionService
                .createSubmission(administrator(), 1002, "text/x-java", "Code")
                .get("submissionId");

    Assertions.assertFalse(
        containsSubmission(submissionService.getSubmissions(0, "", 100, null), submissionId));
    Assertions.assertTrue(
        containsSubmission(
            submissionService.getSubmissions(0, "", 100, administrator()), submissionId));
    Assertions.assertFalse(
        containsSubmission(
            submissionService.getSubmissions(0, "", submissionId, 100, null), submissionId));
    Assertions.assertTrue(
        containsSubmission(
            submissionService.getSubmissions(0, "", submissionId, 100, administrator()),
            submissionId));
    Assertions.assertFalse(
        containsSubmission(
            submissionService.getLatestSubmissions(0, "", 0, 100, null), submissionId));
    Assertions.assertTrue(
        containsSubmission(
            submissionService.getLatestSubmissions(0, "", 0, 100, administrator()), submissionId));
  }

  /**
   * Test case: tests getSolvedCountAndRankOfUser(long). Test data: user 1000 solved 2 problems,
   * user 1001 none; then user 1002 solves one problem, then a second one. Expected: the counts, and
   * the same standard competition ranks as the user list computes from everyone's counts (ties
   * share a rank; users who solved nothing are not ranked).
   */
  @Test
  public void testGetSolvedCountAndRankOfUser() {
    Assertions.assertEquals(
        2L, submissionService.getSolvedCountAndRankOfUser(1000).get("solvedCount"));
    Assertions.assertEquals(1L, submissionService.getSolvedCountAndRankOfUser(1000).get("rank"));
    Assertions.assertEquals(
        0L, submissionService.getSolvedCountAndRankOfUser(1001).get("solvedCount"));
    Assertions.assertNull(submissionService.getSolvedCountAndRankOfUser(1001).get("rank"));

    org.springframework.jdbc.core.JdbcTemplate jdbcTemplate =
        new org.springframework.jdbc.core.JdbcTemplate(dataSource);
    String insertAccepted =
        "INSERT INTO voj_submissions (problem_id, uid, language_id, submission_judge_result,"
            + " submission_code) VALUES (?, 1002, 2, 'AC', 'Code')";
    jdbcTemplate.update(insertAccepted, 1000);
    Assertions.assertEquals(2L, submissionService.getSolvedCountAndRankOfUser(1002).get("rank"));
    jdbcTemplate.update(insertAccepted, 1003);
    // The rows were inserted behind MyBatis's back: any MyBatis update (this one changes no row)
    // clears the query results it cached in this transaction.
    submissionService.markSubmissionUnjudgeable(
        0, null, SubmissionService.JudgingTaskFailure.UNKNOWN);
    Assertions.assertEquals(1L, submissionService.getSolvedCountAndRankOfUser(1002).get("rank"));
    Assertions.assertEquals(1L, submissionService.getSolvedCountAndRankOfUser(1000).get("rank"));
    Assertions.assertEquals(
        submissionService.getSolvedProblemCountForAllUsers().get(1002L),
        submissionService.getSolvedCountAndRankOfUser(1002).get("solvedCount"));
  }

  /**
   * The statement with which a judger claims a pending submission (judger SubmissionMapper).
   */
  private static final String JUDGER_CLAIM =
      "UPDATE voj_submissions SET submission_execute_time = ?, judger_uid = 1001"
          + " WHERE submission_id = ? AND submission_judge_result = 'PD'";

  /**
   * The statement with which a judger records the verdict of its judging (judger SubmissionMapper).
   */
  private static final String JUDGER_RECORD_VERDICT =
      "UPDATE voj_submissions SET submission_used_time = 10, submission_used_memory = 64,"
          + " submission_judge_score = 100, submission_judge_result = 'AC',"
          + " submission_judge_log = 'Accepted.'"
          + " WHERE submission_id = ? AND submission_judge_result = 'PD'"
          + " AND submission_execute_time = ?";

  /**
   * Test case: tests a rejudge (createSubmissionTasks) together with the judger's claim and verdict
   * statements. Test data: submission 1001 (judged WA, with an execute time), and a judging of it
   * that claimed it before the rejudge and finishes after it. Expected: the rejudge resets the row
   * to pending and clears the execute time and judger, so the stale judging's verdict is dropped
   * and a new judging can claim the submission and record its verdict.
   */
  @Test
  public void testRejudgeIsCompatibleWithTheJudgersClaim() {
    org.springframework.jdbc.core.JdbcTemplate jdbcTemplate =
        new org.springframework.jdbc.core.JdbcTemplate(dataSource);
    java.sql.Timestamp staleJudging = java.sql.Timestamp.valueOf("2026-01-01 10:00:00");
    jdbcTemplate.update(
        "UPDATE voj_submissions SET submission_judge_result = 'PD' WHERE submission_id = 1001");
    Assertions.assertEquals(1, jdbcTemplate.update(JUDGER_CLAIM, staleJudging, 1001));

    submissionService.createSubmissionTasks(List.of(1001L));

    Map<String, Object> row =
        jdbcTemplate.queryForMap(
            "SELECT submission_judge_result, submission_execute_time, judger_uid,"
                + " submission_judge_log, submission_judge_score FROM voj_submissions"
                + " WHERE submission_id = 1001");
    Assertions.assertEquals("PD", row.get("submission_judge_result"));
    Assertions.assertNull(row.get("submission_execute_time"));
    Assertions.assertNull(row.get("judger_uid"));
    Assertions.assertNull(row.get("submission_judge_log"));
    Assertions.assertNull(row.get("submission_judge_score"));
    // The judging that started before the rejudge may not record its verdict ...
    Assertions.assertEquals(0, jdbcTemplate.update(JUDGER_RECORD_VERDICT, 1001, staleJudging));
    // ... and the judging of the new task claims the submission and records its own.
    java.sql.Timestamp newJudging = java.sql.Timestamp.valueOf("2026-01-01 10:05:00");
    Assertions.assertEquals(1, jdbcTemplate.update(JUDGER_CLAIM, newJudging, 1001));
    Assertions.assertEquals(1, jdbcTemplate.update(JUDGER_RECORD_VERDICT, 1001, newJudging));
    Assertions.assertEquals(
        "AC",
        jdbcTemplate.queryForObject(
            "SELECT submission_judge_result FROM voj_submissions WHERE submission_id = 1001",
            String.class));
  }

  /**
   * Test case: tests isCurrentJudging(long, Long, Long). Test data: submission 1001 (judge
   * generation 0) claimed by a judging with the judger's claim statement, then rejudged. Expected:
   * only that judging's start time and generation are current, and after the rejudge neither is;
   * an unknown start time or generation is not compared.
   */
  @Test
  public void testIsCurrentJudging() {
    org.springframework.jdbc.core.JdbcTemplate jdbcTemplate =
        new org.springframework.jdbc.core.JdbcTemplate(dataSource);
    java.sql.Timestamp judging = java.sql.Timestamp.valueOf("2026-01-01 10:00:00");
    jdbcTemplate.update(
        "UPDATE voj_submissions SET submission_judge_result = 'PD' WHERE submission_id = 1001");
    jdbcTemplate.update(JUDGER_CLAIM, judging, 1001);

    Assertions.assertTrue(submissionService.isCurrentJudging(1001, judging.getTime(), 0L));
    Assertions.assertTrue(submissionService.isCurrentJudging(1001, judging.getTime(), null));
    Assertions.assertTrue(submissionService.isCurrentJudging(1001, null, 0L));
    Assertions.assertFalse(
        submissionService.isCurrentJudging(1001, judging.getTime() - 1000, 0L));
    Assertions.assertFalse(submissionService.isCurrentJudging(1001, judging.getTime(), 1L));
    Assertions.assertFalse(submissionService.isCurrentJudging(0, null, 0L));

    submissionService.createSubmissionTasks(List.of(1001L));
    Assertions.assertFalse(submissionService.isCurrentJudging(1001, judging.getTime(), null));
    Assertions.assertFalse(submissionService.isCurrentJudging(1001, null, 0L));
    // The judging of the rejudge's task (generation 1) claims the submission in the same second.
    jdbcTemplate.update(JUDGER_CLAIM, judging, 1001);
    // The claim was made behind MyBatis's back: any MyBatis update (this one changes no row) clears
    // the query results it cached in this transaction.
    submissionService.markSubmissionUnjudgeable(
        0, null, SubmissionService.JudgingTaskFailure.UNKNOWN);
    Assertions.assertFalse(submissionService.isCurrentJudging(1001, judging.getTime(), 0L));
    Assertions.assertTrue(submissionService.isCurrentJudging(1001, judging.getTime(), 1L));
  }

  /**
   * Test case: tests a rejudge (createSubmissionTasks) followed by the dead-lettering of the task
   * queued before it. Test data: submission 1001 (judge generation 0) rejudged twice. Expected:
   * each rejudge increments the judge generation; the dead-lettered task of an older generation
   * leaves the pending submission alone, the task of the current generation marks it, and a task
   * without a generation (queued before generations existed) marks any pending submission.
   */
  @Test
  public void testStaleDeadLetteredTaskDoesNotFailARejudge() {
    org.springframework.jdbc.core.JdbcTemplate jdbcTemplate =
        new org.springframework.jdbc.core.JdbcTemplate(dataSource);
    Assertions.assertEquals(0, submissionService.getSubmission(1001).getJudgeGeneration());
    submissionService.createSubmissionTasks(List.of(1001L));
    submissionService.createSubmissionTasks(List.of(1001L));
    Assertions.assertEquals(2, submissionService.getSubmission(1001).getJudgeGeneration());

    for (long staleGeneration : new long[] {0, 1}) {
      Assertions.assertFalse(
          submissionService.markSubmissionUnjudgeable(
              1001, staleGeneration, SubmissionService.JudgingTaskFailure.NO_JUDGER_AVAILABLE));
    }
    Assertions.assertEquals(
        "PD",
        jdbcTemplate.queryForObject(
            "SELECT submission_judge_result FROM voj_submissions WHERE submission_id = 1001",
            String.class));

    Assertions.assertTrue(
        submissionService.markSubmissionUnjudgeable(
            1001, 2L, SubmissionService.JudgingTaskFailure.NO_JUDGER_AVAILABLE));
    Assertions.assertEquals(
        "SE", submissionService.getSubmission(1001).getJudgeResult().getJudgeResultSlug());

    submissionService.createSubmissionTasks(List.of(1001L));
    Assertions.assertTrue(
        submissionService.markSubmissionUnjudgeable(
            1001, null, SubmissionService.JudgingTaskFailure.UNKNOWN));
  }

  /**
   * Test case: tests markSubmissionUnjudgeable(long, Long, JudgingTaskFailure). Test data: a pending
   * submission a judging claimed, which then failed repeatedly; a judged submission. Expected: the
   * pending one becomes SE with the log of that failure and keeps the claim's execute time, and the
   * judging can no longer record a verdict; the judged one is left alone.
   */
  @Test
  public void testMarkSubmissionUnjudgeable() {
    org.springframework.jdbc.core.JdbcTemplate jdbcTemplate =
        new org.springframework.jdbc.core.JdbcTemplate(dataSource);
    java.sql.Timestamp judging = java.sql.Timestamp.valueOf("2026-01-01 10:00:00");
    jdbcTemplate.update(
        "UPDATE voj_submissions SET submission_judge_result = 'PD' WHERE submission_id = 1001");
    jdbcTemplate.update(JUDGER_CLAIM, judging, 1001);

    Assertions.assertTrue(
        submissionService.markSubmissionUnjudgeable(
            1001, 0L, SubmissionService.JudgingTaskFailure.JUDGING_FAILED_REPEATEDLY));
    Map<String, Object> row =
        jdbcTemplate.queryForMap(
            "SELECT submission_judge_result, submission_judge_log, submission_execute_time"
                + " FROM voj_submissions WHERE submission_id = 1001");
    Assertions.assertEquals("SE", row.get("submission_judge_result"));
    Assertions.assertEquals(
        SubmissionService.JudgingTaskFailure.JUDGING_FAILED_REPEATEDLY.getJudgeLog(),
        row.get("submission_judge_log"));
    Assertions.assertEquals(judging, row.get("submission_execute_time"));
    Assertions.assertEquals(0, jdbcTemplate.update(JUDGER_RECORD_VERDICT, 1001, judging));

    Assertions.assertFalse(
        submissionService.markSubmissionUnjudgeable(
            1000, null, SubmissionService.JudgingTaskFailure.NO_JUDGER_AVAILABLE));
  }

  /** Tests whether a list of submissions contains the submission with the given identifier. */
  private static boolean containsSubmission(List<Submission> submissions, long submissionId) {
    return submissions.stream().anyMatch(s -> s.getSubmissionId() == submissionId);
  }

  /** Constructs a User object in the administrators user group. */
  private User administrator() {
    User user = userWithUid(1000);
    user.setUserGroup(new UserGroup(8, "administrators", "Administrators"));
    return user;
  }

  /** Test case: tests the deleteSubmission(long) method. Test data: a submission record not referenced by any contest. Expected: deletion succeeds and the submission record no longer exists. */
  @Test
  public void testDeleteSubmission() {
    Assertions.assertTrue(submissionService.deleteSubmission(1004));
    Assertions.assertNull(submissionService.getSubmission(1004));
  }

  /**
   * Test case: tests the deleteSubmission(long) method. Test data: submission 1000, which was made
   * in contest #1. Expected: deletion succeeds and the submission leaves the contest.
   */
  @Test
  public void testDeleteContestSubmission() {
    Assertions.assertTrue(submissionService.deleteSubmission(1000));
    Assertions.assertNull(submissionService.getSubmission(1000));
    Assertions.assertTrue(contestSubmissionMapper.getContestIdsOfSubmission(1000).isEmpty());
  }

  /** Test case: tests the deleteSubmission(long) method. Test data: a submission that does not exist. Expected: returns false. */
  @Test
  public void testDeleteSubmissionNotExists() {
    Assertions.assertFalse(submissionService.deleteSubmission(0));
  }

  /**
   * Test case: tests the deleteSubmissions(List) method. Test data: a contest submission, a plain
   * submission and a missing identifier. Expected: the two existing submissions are deleted and
   * reported, the missing one is not.
   */
  @Test
  public void testDeleteSubmissions() {
    Assertions.assertEquals(
        List.of(1002L, 1004L), submissionService.deleteSubmissions(List.of(1002L, 1004L, 0L)));
    Assertions.assertNull(submissionService.getSubmission(1002));
    Assertions.assertNull(submissionService.getSubmission(1004));
  }

  /** Test case: tests the createSubmissionTask(long) method. Test data: an arbitrary submission identifier. Expected: sending the message does not throw an exception. */
  @Test
  public void testCreateSubmissionTask() {
    Assertions.assertDoesNotThrow(() -> submissionService.createSubmissionTask(1000));
  }

  /**
   * Test case: tests the getNumberOfSubmissionsGroupByDay(...) method in a time zone 5:30 hours
   * ahead of UTC (the web application's days are those of its time zone, not of the database
   * session's UTC). Test data: user 1002's submissions at 00:10 and 23:50 on 2026-03-10, at 00:10 on
   * 2026-03-11 and at 23:50 on 2026-03-09 (local times); a range from noon on 2026-03-10 to 08:00
   * on 2026-03-12. Expected: one slot per local day from the start day through the end day, in
   * chronological order; the submissions are counted on their local days (in UTC, 00:10 on
   * 2026-03-10 is still 2026-03-09, and 00:10 on 2026-03-11 still 2026-03-10), including the start
   * day's before noon, and the one before the start day is not counted.
   */
  @Test
  public void testGetNumberOfSubmissionsGroupByDayUsesTheLocalDays() {
    java.util.TimeZone defaultTimeZone = java.util.TimeZone.getDefault();
    java.time.ZoneId zone = java.time.ZoneId.of("Asia/Kolkata");
    try {
      java.util.TimeZone.setDefault(java.util.TimeZone.getTimeZone(zone));
      org.springframework.jdbc.core.JdbcTemplate jdbcTemplate =
          new org.springframework.jdbc.core.JdbcTemplate(dataSource);
      for (String localTime :
          List.of(
              "2026-03-09T23:50", "2026-03-10T00:10", "2026-03-10T23:50", "2026-03-11T00:10")) {
        jdbcTemplate.update(
            "INSERT INTO voj_submissions (problem_id, uid, language_id, submission_submit_time,"
                + " submission_judge_result, submission_code) VALUES (1000, 1002, 2, ?, 'AC', 'C')",
            localTimestamp(localTime, zone));
      }

      Map<String, Long> submissions =
          submissionService.getNumberOfSubmissionsGroupByDay(
              localTimestamp("2026-03-10T12:00", zone),
              localTimestamp("2026-03-12T08:00", zone),
              1002,
              false,
              null);

      Assertions.assertEquals(
          List.of("2026/03/10", "2026/03/11", "2026/03/12"), List.copyOf(submissions.keySet()));
      Assertions.assertEquals(List.of(2L, 1L, 0L), List.copyOf(submissions.values()));
    } finally {
      java.util.TimeZone.setDefault(defaultTimeZone);
    }
  }

  /** Gets the instant of a local date and time in a time zone. */
  private static java.sql.Timestamp localTimestamp(String localDateTime, java.time.ZoneId zone) {
    return java.sql.Timestamp.from(
        java.time.LocalDateTime.parse(localDateTime).atZone(zone).toInstant());
  }

  /**
   * Test case: tests the getSubmissionsByPage(...) method. Test data: user zjhzxhz's three
   * submissions (1003, 1001, 1000 newest first) paged two at a time, and the CE filter. Expected:
   * the pages partition exactly the filtered rows counted by
   * getNumberOfSubmissionsUsingProblemIdAndUsername, without repeating or skipping any.
   */
  @Test
  public void testGetSubmissionsByPageFollowsFilteredCount() {
    Assertions.assertEquals(
        3, submissionService.getNumberOfSubmissionsUsingProblemIdAndUsername(0, "zjhzxhz", ""));
    Assertions.assertEquals(
        List.of(1003L, 1001L),
        submissionIdsOf(submissionService.getSubmissionsByPage(0, "zjhzxhz", "", 1, 2)));
    Assertions.assertEquals(
        List.of(1000L), submissionIdsOf(submissionService.getSubmissionsByPage(0, "zjhzxhz", "", 2, 2)));

    // Submissions 1002 and 1004 are the only CE ones; their ids are not adjacent to the latest id.
    Assertions.assertEquals(
        List.of(1004L), submissionIdsOf(submissionService.getSubmissionsByPage(0, "", "CE", 1, 1)));
    Assertions.assertEquals(
        List.of(1002L), submissionIdsOf(submissionService.getSubmissionsByPage(0, "", "CE", 2, 1)));
    Assertions.assertTrue(submissionService.getSubmissionsByPage(0, "", "CE", 3, 1).isEmpty());
  }

  /** Collects the ids of a list of submissions, preserving their order. */
  private static List<Long> submissionIdsOf(List<Submission> submissions) {
    return submissions.stream().map(Submission::getSubmissionId).toList();
  }

  /** Constructs a User object with only the UID set, used as the submitting user argument. */
  private User userWithUid(long uid) {
    User user = new User();
    user.setUid(uid);
    return user;
  }

  /** The SubmissionService object under test. */
  @Autowired private SubmissionService submissionService;

  /** The ContestSubmissionMapper object, used to check the contest records of a submission. */
  @Autowired private ContestSubmissionMapper contestSubmissionMapper;

  /** The DataSource of the test database, used to set up fixtures the services cannot create. */
  @Autowired private javax.sql.DataSource dataSource;
}
