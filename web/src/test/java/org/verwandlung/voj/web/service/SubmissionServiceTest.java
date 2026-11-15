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

  /** Test case: tests the getSubmissions(long, String, int, boolean) method. Test data: the submissions of user zjhzxhz for problem 1000. Expected: exactly submissions 1000 and 1001, each belonging to problem 1000. */
  @Test
  public void testGetSubmissions() {
    List<Submission> submissions = submissionService.getSubmissions(1000, "zjhzxhz", 10, true);
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
    Map<String, Long> stats = submissionService.getSubmissionStatsOfUser(1000);
    Assertions.assertEquals(2L, stats.get("acceptedSubmission"));
    Assertions.assertEquals(3L, stats.get("totalSubmission"));
    Assertions.assertEquals(66L, stats.get("acRate"));
  }

  /** Test case: tests the getSubmissionStatsOfUser(long) method. Test data: a user with no submission records. Expected: the acceptance rate is 0 and no division-by-zero exception is thrown. */
  @Test
  public void testGetSubmissionStatsOfUserWithoutSubmissions() {
    Map<String, Long> stats = submissionService.getSubmissionStatsOfUser(9999);
    Assertions.assertEquals(0L, stats.get("acceptedSubmission"));
    Assertions.assertEquals(0L, stats.get("totalSubmission"));
    Assertions.assertEquals(0L, stats.get("acRate"));
  }

  /** Test case: tests the getSubmissionOfUser(long) method. Test data: user 1000. Expected: the latest submission records indexed by problem. */
  @Test
  public void testGetSubmissionOfUser() {
    Map<Long, Submission> submissions = submissionService.getSubmissionOfUser(1000);
    Assertions.assertNotNull(submissions);
    // User 1000 has submission records for both problems 1000 and 1001.
    Assertions.assertTrue(submissions.containsKey(1000L));
    Assertions.assertTrue(submissions.containsKey(1001L));
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
        containsSubmission(submissionService.getSubmissions(0, "", 100, true), submissionId));
    Assertions.assertTrue(
        containsSubmission(submissionService.getSubmissions(0, "", 100, false), submissionId));
    Assertions.assertFalse(
        containsSubmission(
            submissionService.getSubmissions(0, "", submissionId, 100, true), submissionId));
    Assertions.assertTrue(
        containsSubmission(
            submissionService.getSubmissions(0, "", submissionId, 100, false), submissionId));
    Assertions.assertFalse(
        containsSubmission(
            submissionService.getLatestSubmissions(0, "", 0, 100, true), submissionId));
    Assertions.assertTrue(
        containsSubmission(
            submissionService.getLatestSubmissions(0, "", 0, 100, false), submissionId));
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
    // Submission record 1004 is not referenced by voj_contest_submissions and can be safely deleted.
    Assertions.assertTrue(submissionService.deleteSubmission(1004));
    Assertions.assertNull(submissionService.getSubmission(1004));
  }

  /** Test case: tests the createSubmissionTask(long) method. Test data: an arbitrary submission identifier. Expected: sending the message does not throw an exception. */
  @Test
  public void testCreateSubmissionTask() {
    Assertions.assertDoesNotThrow(() -> submissionService.createSubmissionTask(1000));
  }

  /**
   * Test case: tests the getNumberOfSubmissionsGroupByDay(...) method. Test data: a range starting
   * mid-day on 2014-10-01 (the day of user 1000's submission #1000) and ending on 2014-10-03.
   * Expected: one slot per day from the start day through the end day, in chronological order, with
   * the start day's submission counted in its own slot.
   */
  @Test
  public void testGetNumberOfSubmissionsGroupByDayIncludesStartDay() {
    java.util.Calendar calendar = new java.util.GregorianCalendar(2014, 9, 1, 12, 0, 0);
    java.util.Date startTime = calendar.getTime();
    calendar.set(2014, 9, 3, 8, 0, 0);
    java.util.Date endTime = calendar.getTime();

    Map<String, Long> submissions =
        submissionService.getNumberOfSubmissionsGroupByDay(startTime, endTime, 1000, false);

    Assertions.assertEquals(
        List.of("2014/10/01", "2014/10/02", "2014/10/03"), List.copyOf(submissions.keySet()));
    Assertions.assertEquals(1L, submissions.get("2014/10/01"));
    Assertions.assertEquals(0L, submissions.get("2014/10/02"));
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
}
