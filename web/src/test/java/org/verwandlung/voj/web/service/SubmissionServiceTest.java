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

  /** Test case: tests the getJudgeResultsOfProblems(long, Collection) method. Test data: user 1000, problems #1000, #1001 and #1002, and no problem. Expected: AC for #1000 (whose latest WA an earlier AC overrides) and #1001, nothing for #1002, which the user never submitted to. */
  @Test
  public void testGetJudgeResultsOfProblems() {
    Assertions.assertEquals(
        Map.of(1000L, "AC", 1001L, "AC"),
        submissionService.getJudgeResultsOfProblems(1000, List.of(1000L, 1001L, 1002L)));
    Assertions.assertTrue(submissionService.getJudgeResultsOfProblems(1000, List.of()).isEmpty());
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

  /** Test case: tests the getSubmissions methods. Test data: a submission to the hidden problem 1002. Expected: it is listed only when unpublished problems are included. */
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
            submissionService.getSubmissions(0, "", submissionId, null, 100, null), submissionId));
    Assertions.assertTrue(
        containsSubmission(
            submissionService.getSubmissions(0, "", submissionId, null, 100, administrator()),
            submissionId));
    Assertions.assertFalse(
        containsSubmission(
            submissionService.getSubmissions(0, "", null, 0L, 100, null), submissionId));
    Assertions.assertTrue(
        containsSubmission(
            submissionService.getSubmissions(0, "", null, 0L, 100, administrator()), submissionId));
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
    submissionService.deleteSubmission(0);
    Assertions.assertEquals(1L, submissionService.getSolvedCountAndRankOfUser(1002).get("rank"));
    Assertions.assertEquals(1L, submissionService.getSolvedCountAndRankOfUser(1000).get("rank"));
    Assertions.assertEquals(
        submissionService.getSolvedProblemCountForAllUsers().get(1002L),
        submissionService.getSolvedCountAndRankOfUser(1002).get("solvedCount"));
  }

  /**
   * The statement with which a judger claims a pending submission that no judging holds (judger
   * SubmissionMapper), with a lease of a minute: its parameters are the claim token and the
   * submission.
   */
  private static final String JUDGER_CLAIM =
      "UPDATE voj_submissions SET submission_claim_token = ?, submission_execute_time = NOW(),"
          + " submission_lease_expiry_time = NOW() + INTERVAL 60 SECOND,"
          + " submission_judge_attempts = submission_judge_attempts + 1, judger_uid = 1001"
          + " WHERE submission_id = ? AND submission_judge_result = 'PD'"
          + " AND (submission_lease_expiry_time IS NULL OR submission_lease_expiry_time < NOW())";


  /**
   * The statement with which a judger records the verdict of its judging (judger SubmissionMapper):
   * its parameters are the submission and the claim token.
   */
  private static final String JUDGER_RECORD_VERDICT =
      "UPDATE voj_submissions SET submission_used_time = 10, submission_used_memory = 64,"
          + " submission_judge_score = 100, submission_judge_result = 'AC',"
          + " submission_judge_log = 'Accepted.'"
          + " WHERE submission_id = ? AND submission_judge_result = 'PD'"
          + " AND submission_claim_token = ?";

  /**
   * Test case: tests a rejudge (rejudgeSubmissions) together with the judger's claim and verdict
   * statements. Test data: submission 1001, queued long ago, and a judging of it that claimed it
   * (its second attempt) before the rejudge and finishes after it. Expected: the rejudge resets
   * the row to pending, at the end of the queue, without attempts or claim, so the stale judging's
   * verdict is dropped, and a new judging claims the submission and records its verdict.
   */
  @Test
  public void testRejudgeIsCompatibleWithTheJudgersClaim() {
    org.springframework.jdbc.core.JdbcTemplate jdbcTemplate =
        new org.springframework.jdbc.core.JdbcTemplate(dataSource);
    jdbcTemplate.update(
        "UPDATE voj_submissions SET submission_judge_result = 'PD', submission_judge_attempts = 1,"
            + " submission_judge_failure = 'Failed.',"
            + " submission_queued_time = '2014-10-17 23:59:59' WHERE submission_id = 1001");
    Assertions.assertEquals(1, jdbcTemplate.update(JUDGER_CLAIM, 7L, 1001));
    // A judging that holds the submission may not be claimed by another one.
    Assertions.assertEquals(0, jdbcTemplate.update(JUDGER_CLAIM, 8L, 1001));

    submissionService.rejudgeSubmissions(List.of(1001L));

    Map<String, Object> row =
        jdbcTemplate.queryForMap(
            "SELECT submission_judge_result, submission_execute_time, judger_uid,"
                + " submission_judge_log, submission_judge_score, submission_claim_token,"
                + " submission_lease_expiry_time, submission_judge_attempts,"
                + " submission_judge_failure,"
                + " TIMESTAMPDIFF(SECOND, submission_queued_time, NOW()) AS queued_seconds_ago"
                + " FROM voj_submissions WHERE submission_id = 1001");
    Assertions.assertEquals("PD", row.get("submission_judge_result"));
    Assertions.assertNull(row.get("submission_execute_time"));
    Assertions.assertNull(row.get("judger_uid"));
    Assertions.assertNull(row.get("submission_judge_log"));
    Assertions.assertNull(row.get("submission_judge_score"));
    Assertions.assertNull(row.get("submission_claim_token"));
    Assertions.assertNull(row.get("submission_lease_expiry_time"));
    Assertions.assertEquals(0, ((Number) row.get("submission_judge_attempts")).intValue());
    Assertions.assertNull(row.get("submission_judge_failure"));
    Assertions.assertTrue(((Number) row.get("queued_seconds_ago")).longValue() < 60);
    // The judging that started before the rejudge may not record its verdict ...
    Assertions.assertEquals(0, jdbcTemplate.update(JUDGER_RECORD_VERDICT, 1001, 7L));
    // ... and a new judging claims the submission and records its own.
    Assertions.assertEquals(1, jdbcTemplate.update(JUDGER_CLAIM, 9L, 1001));
    Assertions.assertEquals(1, jdbcTemplate.update(JUDGER_RECORD_VERDICT, 1001, 9L));
    Assertions.assertEquals(
        "AC",
        jdbcTemplate.queryForObject(
            "SELECT submission_judge_result FROM voj_submissions WHERE submission_id = 1001",
            String.class));
  }

  /**
   * Test case: tests rejudgeSubmissions(List). Test data: submission 1001, pending since long ago
   * and claimed by no judging, after a failed judging. Expected: it is left alone: it keeps its
   * place in the queue and its failed judging.
   */
  @Test
  public void testRejudgeLeavesAWaitingSubmissionAlone() {
    org.springframework.jdbc.core.JdbcTemplate jdbcTemplate =
        new org.springframework.jdbc.core.JdbcTemplate(dataSource);
    jdbcTemplate.update(
        "UPDATE voj_submissions SET submission_judge_result = 'PD', submission_judge_attempts = 1,"
            + " submission_judge_failure = 'Failed.',"
            + " submission_queued_time = '2014-10-17 23:59:59' WHERE submission_id = 1001");

    submissionService.rejudgeSubmissions(List.of(1001L));

    Map<String, Object> row =
        jdbcTemplate.queryForMap(
            "SELECT submission_judge_result, submission_judge_attempts, submission_judge_failure,"
                + " submission_queued_time = '2014-10-17 23:59:59' AS keeps_its_place"
                + " FROM voj_submissions WHERE submission_id = 1001");
    Assertions.assertEquals("PD", row.get("submission_judge_result"));
    Assertions.assertEquals(1, ((Number) row.get("submission_judge_attempts")).intValue());
    Assertions.assertEquals("Failed.", row.get("submission_judge_failure"));
    Assertions.assertEquals(1, ((Number) row.get("keeps_its_place")).intValue());
  }

  /**
   * Test case: tests isCurrentJudging(long, long). Test data: submission 1001 claimed by a judging
   * with the judger's claim statement, claimed by another judging once its lease expired, then
   * judged by it, then rejudged. Expected: only the claim token the submission carries is
   * current, also once its judging recorded the verdict; after the rejudge none is.
   */
  @Test
  public void testIsCurrentJudging() {
    org.springframework.jdbc.core.JdbcTemplate jdbcTemplate =
        new org.springframework.jdbc.core.JdbcTemplate(dataSource);
    jdbcTemplate.update(
        "UPDATE voj_submissions SET submission_judge_result = 'PD' WHERE submission_id = 1001");
    Assertions.assertFalse(submissionService.isCurrentJudging(1001, 7L));
    // The claims are made behind MyBatis's back: any MyBatis update (deleteSubmission(0) changes no
    // row) clears the query results it cached in this transaction.
    jdbcTemplate.update(JUDGER_CLAIM, 7L, 1001);
    submissionService.deleteSubmission(0);
    Assertions.assertTrue(submissionService.isCurrentJudging(1001, 7L));
    Assertions.assertFalse(submissionService.isCurrentJudging(1001, 8L));
    Assertions.assertFalse(submissionService.isCurrentJudging(0, 7L));

    jdbcTemplate.update(
        "UPDATE voj_submissions SET submission_lease_expiry_time = NOW() - INTERVAL 1 SECOND"
            + " WHERE submission_id = 1001");
    jdbcTemplate.update(JUDGER_CLAIM, 8L, 1001);
    jdbcTemplate.update(JUDGER_RECORD_VERDICT, 1001, 8L);
    submissionService.deleteSubmission(0);
    Assertions.assertFalse(submissionService.isCurrentJudging(1001, 7L));
    Assertions.assertTrue(submissionService.isCurrentJudging(1001, 8L));

    submissionService.rejudgeSubmissions(List.of(1001L));
    Assertions.assertFalse(submissionService.isCurrentJudging(1001, 8L));
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

  /**
   * Test case: tests the getNumberOfSubmissionsGroupByDay(...) method. Test data: user 1002's
   * submissions of today to the published problem 1000 and to the hidden problem 1002. Expected:
   * the user's heat map counts only the one to the published problem, as the other statistics of
   * the profile do (even for an administrator); the site-wide count (uid 0) counts both.
   */
  @Test
  public void testGetNumberOfSubmissionsGroupByDayOfUserCountsPublishedProblemsOnly() {
    org.springframework.jdbc.core.JdbcTemplate jdbcTemplate =
        new org.springframework.jdbc.core.JdbcTemplate(dataSource);
    java.util.Date now = new java.util.Date();
    for (long problemId : new long[] {1000, 1002}) {
      jdbcTemplate.update(
          "INSERT INTO voj_submissions (problem_id, uid, language_id, submission_submit_time,"
              + " submission_judge_result, submission_code) VALUES (?, 1002, 2, ?, 'AC', 'Code')",
          problemId,
          new java.sql.Timestamp(now.getTime()));
    }
    java.util.Date startOfToday =
        java.util.Date.from(
            java.time.LocalDate.now().atStartOfDay(java.time.ZoneId.systemDefault()).toInstant());
    java.util.Date end = new java.util.Date(now.getTime() + 1000);

    for (User viewer : java.util.Arrays.asList(null, administrator())) {
      Assertions.assertEquals(
          List.of(1L),
          List.copyOf(
              submissionService
                  .getNumberOfSubmissionsGroupByDay(startOfToday, end, 1002, false, viewer)
                  .values()));
    }
    Assertions.assertEquals(
        List.of(2L),
        List.copyOf(
            submissionService
                .getNumberOfSubmissionsGroupByDay(startOfToday, end, 0, false, administrator())
                .values()));
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
