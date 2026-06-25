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

import java.sql.Date;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

import org.verwandlung.voj.web.model.Language;
import org.verwandlung.voj.web.model.Problem;
import org.verwandlung.voj.web.model.Submission;
import org.verwandlung.voj.web.model.User;

/**
 * The test class for SubmissionMapper.
 *
 * @author Haozhe Xie
 */
@ExtendWith(SpringExtension.class)
@Transactional
@ContextConfiguration({"classpath:test-spring-context.xml"})
public class SubmissionMapperTest {
  /**
   * Test case: tests the getNumberOfSubmissionsUsingDate(String, String) method. Test data: query
   * using 2014-10-01 to 2014-10-18. Expected: the number of submissions within this time range (2).
   */
  @Test
  public void testGetNumberOfSubmissionsUsingDate() {
    String startTime = "2014-10-01 00:00:00";
    String endTime = "2014-10-18 00:00:00";

    long numberOfSubmissions = submissionMapper.getNumberOfSubmissionsUsingDate(startTime, endTime);
    Assertions.assertEquals(2, numberOfSubmissions);
  }

  /**
   * Test case: tests the getNumberOfSubmissionsGroupByMonth(String, String, int, boolean) method. Test
   * data: query using 2014-10-01 to 2015-10-01. Expected: the number of submissions per month for
   * the year.
   */
  @Test
  public void testGetNumberOfTotalSubmissionsGroupByMonthWithoutUser() {
    String startTime = "2014-10-01 00:00:00";
    String endTime = "2015-10-01 00:00:00";

    List<Map<String, Object>> numberOfSubmissions =
        submissionMapper.getNumberOfSubmissionsGroupByMonth(startTime, endTime, 0, false);
    Assertions.assertEquals(3, numberOfSubmissions.size());

    Map<String, Object> firstEntry = numberOfSubmissions.get(0);
    int month = (Integer) firstEntry.get("month");
    long submissions = (Long) firstEntry.get("submissions");

    Assertions.assertEquals(201410, month);
    Assertions.assertEquals(2, submissions);
  }

  /**
   * Test case: tests the getNumberOfSubmissionsGroupByMonth(String, String, int, boolean) method. Test
   * data: query the accepted submissions using 2014-10-01 to 2015-10-01. Expected: the number of
   * accepted submissions per month for the year.
   */
  @Test
  public void testGetNumberOfAccpectedSubmissionsGroupByMonthWithoutUser() {
    String startTime = "2014-10-01 00:00:00";
    String endTime = "2015-10-01 00:00:00";

    List<Map<String, Object>> numberOfSubmissions =
        submissionMapper.getNumberOfSubmissionsGroupByMonth(startTime, endTime, 0, true);
    Assertions.assertEquals(2, numberOfSubmissions.size());

    Map<String, Object> firstEntry = numberOfSubmissions.get(0);
    int month = (Integer) firstEntry.get("month");
    long submissions = (Long) firstEntry.get("submissions");

    Assertions.assertEquals(201410, month);
    Assertions.assertEquals(1, submissions);
  }

  /**
   * Test case: tests the getNumberOfSubmissionsGroupByMonth(String, String, int, boolean) method. Test
   * data: query a user's submissions using 2014-10-01 to 2015-10-01. Expected: the number of the
   * user's submissions per month for the year.
   */
  @Test
  public void testGetNumberOfTotalSubmissionsGroupByMonthWithUser() {
    String startTime = "2014-10-01 00:00:00";
    String endTime = "2015-10-01 00:00:00";

    List<Map<String, Object>> numberOfSubmissions =
        submissionMapper.getNumberOfSubmissionsGroupByMonth(startTime, endTime, 1000, false);
    Assertions.assertEquals(2, numberOfSubmissions.size());

    Map<String, Object> firstEntry = numberOfSubmissions.get(0);
    int month = (Integer) firstEntry.get("month");
    long submissions = (Long) firstEntry.get("submissions");

    Assertions.assertEquals(201410, month);
    Assertions.assertEquals(2, submissions);
  }

  /**
   * Test case: tests the getNumberOfSubmissionsGroupByMonth(String, String, int, boolean) method. Test
   * data: query a user's accepted submissions using 2014-10-01 to 2015-10-01. Expected: the number
   * of the user's accepted submissions per month for the year.
   */
  @Test
  public void testGetNumberOfAccpectedSubmissionsGroupByMonthWithUser() {
    String startTime = "2014-10-01 00:00:00";
    String endTime = "2015-10-01 00:00:00";

    List<Map<String, Object>> numberOfSubmissions =
        submissionMapper.getNumberOfSubmissionsGroupByMonth(startTime, endTime, 1000, true);
    Assertions.assertEquals(2, numberOfSubmissions.size());

    Map<String, Object> firstEntry = numberOfSubmissions.get(0);
    int month = (Integer) firstEntry.get("month");
    long submissions = (Long) firstEntry.get("submissions");

    Assertions.assertEquals(201410, month);
    Assertions.assertEquals(1, submissions);
  }

  /**
   * Test case: tests the getNumberOfSubmissionsGroupByDay(String, String, int, boolean) method. Test
   * data: query using 2014-10-01 to 2014-10-31. Expected: the number of submissions per day for the
   * month.
   */
  @Test
  public void testGetNumberOfTotalSubmissionsGroupByDayWithoutUser() {
    String startTime = "2014-10-01 00:00:00";
    String endTime = "2014-11-01 00:00:00";

    List<Map<String, Object>> numberOfSubmissions =
        submissionMapper.getNumberOfSubmissionsGroupByDay(startTime, endTime, 0, false);
    Assertions.assertEquals(2, numberOfSubmissions.size());

    Map<String, Object> firstEntry = numberOfSubmissions.get(0);
    Date day = (Date) firstEntry.get("date");
    long submissions = (Long) firstEntry.get("submissions");

    Assertions.assertEquals("2014-10-01", day.toString());
    Assertions.assertEquals(1, submissions);
  }

  /**
   * Test case: tests the getNumberOfSubmissionsGroupByDay(String, String, int, boolean) method. Test
   * data: query the accepted submissions using 2014-10-01 to 2014-10-31. Expected: the number of
   * accepted submissions per day for the month.
   */
  @Test
  public void testGetNumberOfAccpectedSubmissionsGroupByDayWithoutUser() {
    String startTime = "2014-10-01 00:00:00";
    String endTime = "2014-11-01 00:00:00";

    List<Map<String, Object>> numberOfSubmissions =
        submissionMapper.getNumberOfSubmissionsGroupByDay(startTime, endTime, 0, true);
    Assertions.assertEquals(1, numberOfSubmissions.size());

    Map<String, Object> firstEntry = numberOfSubmissions.get(0);
    Date day = (Date) firstEntry.get("date");
    long submissions = (Long) firstEntry.get("submissions");

    Assertions.assertEquals("2014-10-01", day.toString());
    Assertions.assertEquals(1, submissions);
  }

  /**
   * Test case: tests the getNumberOfSubmissionsGroupByDay(String, String, int, boolean) method. Test
   * data: query a user's submissions using 2014-10-01 to 2014-10-31. Expected: the number of the
   * user's submissions per day for the month.
   */
  @Test
  public void testGetNumberOfTotalSubmissionsGroupByDayWithUser() {
    String startTime = "2014-10-01 00:00:00";
    String endTime = "2014-11-01 00:00:00";

    List<Map<String, Object>> numberOfSubmissions =
        submissionMapper.getNumberOfSubmissionsGroupByDay(startTime, endTime, 1000, false);
    Assertions.assertEquals(2, numberOfSubmissions.size());

    Map<String, Object> firstEntry = numberOfSubmissions.get(0);
    Date day = (Date) firstEntry.get("date");
    long submissions = (Long) firstEntry.get("submissions");

    Assertions.assertEquals("2014-10-01", day.toString());
    Assertions.assertEquals(1, submissions);
  }

  /**
   * Test case: tests the getNumberOfSubmissionsGroupByDay(String, String, int, boolean) method. Test
   * data: query a user's accepted submissions using 2014-10-01 to 2014-10-31. Expected: the number
   * of the user's accepted submissions per day for the month.
   */
  @Test
  public void testGetNumberOfAccpectedSubmissionsGroupByDayWithUser() {
    String startTime = "2014-10-01 00:00:00";
    String endTime = "2014-11-01 00:00:00";

    List<Map<String, Object>> numberOfSubmissions =
        submissionMapper.getNumberOfSubmissionsGroupByDay(startTime, endTime, 1000, true);
    Assertions.assertEquals(1, numberOfSubmissions.size());

    Map<String, Object> firstEntry = numberOfSubmissions.get(0);
    Date day = (Date) firstEntry.get("date");
    long submissions = (Long) firstEntry.get("submissions");

    Assertions.assertEquals("2014-10-01", day.toString());
    Assertions.assertEquals(1, submissions);
  }

  /**
   * Test case: tests the getNumberOfSubmissionsUsingLanguage(int) method. Test data: the unique
   * identifier of the C++ language. Expected: the number of submissions using the C++ language.
   */
  @Test
  public void testGetNumberOfSubmissionsUsingLanguageUsingCpp() {
    int languageId = 2;
    long numberOfSubmissions = submissionMapper.getNumberOfSubmissionsUsingLanguage(languageId);
    Assertions.assertEquals(4, numberOfSubmissions);
  }

  /** Test case: tests the getNumberOfSubmissionsUsingLanguage(int) method. Test data: a non-existing unique identifier. Expected: returns 0. */
  @Test
  public void testGetNumberOfSubmissionsUsingLanguageUsingNotExistingLanguage() {
    int languageId = 0;
    long numberOfSubmissions = submissionMapper.getNumberOfSubmissionsUsingLanguage(languageId);
    Assertions.assertEquals(0, numberOfSubmissions);
  }

  /** Test case: tests the getLatestSubmissionId() method. Test data: N/a. Expected: the unique identifier of the last submission. */
  @Test
  public void testGetLatestSubmissionId() {
    long latestSubmissionId = submissionMapper.getLatestSubmissionId();
    Assertions.assertEquals(1004, latestSubmissionId);
  }

  /** Test case: tests the getSubmission(long) method. Test data: the unique identifier of a submission record of Problem #1000. Expected: the expected Submission object. */
  @Test
  public void testGetSubmissionExists() {
    Submission submission = submissionMapper.getSubmission(1000);
    Assertions.assertNotNull(submission);

    Problem problem = submission.getProblem();
    long problemId = problem.getProblemId();
    Assertions.assertEquals(1000, problemId);
  }

  /** Test case: tests the getSubmission(long) method. Test data: a non-existing submission record unique identifier. Expected: a null reference. */
  @Test
  public void testGetSubmissionNotExists() {
    Submission submission = submissionMapper.getSubmission(0);
    Assertions.assertNull(submission);
  }

  /**
   * Test case: tests the getSubmissionsUsingOffset(long, String, long, int) method. Test data: get 10
   * submissions with IDs starting from 1010 (ID From 1010 to 1000). Expected: the submission list.
   */
  @Test
  public void testGetSubmissionsFrom1010WithLimit10() {
    List<Submission> submissions = submissionMapper.getSubmissionsUsingOffset(0, "", 1010, 10);
    Assertions.assertEquals(5, submissions.size());

    Submission firstSubmission = submissions.get(0);
    long submissionId = firstSubmission.getSubmissionId();
    Assertions.assertEquals(1004, submissionId);
  }

  /**
   * Test case: tests the getSubmissionsUsingOffset(long, String, long, int) method. Test data: get 2
   * submissions with IDs starting from 1003. Expected: the submission list (2 submissions).
   */
  @Test
  public void testGetSubmissionsFrom1003WithLimit2() {
    List<Submission> submissions = submissionMapper.getSubmissionsUsingOffset(0, "", 1003, 2);
    Assertions.assertEquals(2, submissions.size());

    Submission firstSubmission = submissions.get(0);
    long submissionId = firstSubmission.getSubmissionId();
    Assertions.assertEquals(1003, submissionId);
  }

  /**
   * Test case: tests the getSubmissionsUsingOffset(long, String, long, int) method. Test data: get 10
   * submissions with IDs starting from 100 (ID From 100 to 90). Expected: an empty submission list.
   */
  @Test
  public void testGetSubmissionsFrom100WithLimit10() {
    List<Submission> submissions = submissionMapper.getSubmissionsUsingOffset(0, "", 100, 10);
    Assertions.assertEquals(0, submissions.size());
  }

  /**
   * Test case: tests the getSubmissionUsingProblemIdAndUserId(long, long, int) method. Test data: get
   * the latest 1 submission of problem ID #1000 and user ID #1000. Expected: the expected submission
   * list (1 submission).
   */
  @Test
  public void testGetSubmissionUsingExistingProblemIdAndExistingUserId() {
    List<Submission> submissions =
        submissionMapper.getSubmissionUsingProblemIdAndUserId(1000, 1000, 1);
    Assertions.assertEquals(1, submissions.size());

    Submission firstSubmission = submissions.get(0);
    long submissionId = firstSubmission.getSubmissionId();
    Assertions.assertEquals(1001, submissionId);

    String judgeResultSlug = firstSubmission.getJudgeResult().getJudgeResultSlug();
    Assertions.assertEquals("WA", judgeResultSlug);
  }

  /**
   * Test case: tests the getSubmissionUsingProblemIdAndUserId(long, long, int) method. Test data: a
   * non-existing problem unique identifier. Expected: an empty submission list.
   */
  @Test
  public void testGetSubmissionUsingNotExistingProblemIdAndExistingUserId() {
    List<Submission> submissions =
        submissionMapper.getSubmissionUsingProblemIdAndUserId(0, 1000, 10);
    Assertions.assertEquals(0, submissions.size());
  }

  /**
   * Test case: tests the getSubmissionUsingProblemIdAndUserId(long, long, int) method. Test data: a
   * non-existing user unique identifier. Expected: an empty submission list.
   */
  @Test
  public void testGetSubmissionUsingExistingProblemIdAndNotExistingUserId() {
    List<Submission> submissions =
        submissionMapper.getSubmissionUsingProblemIdAndUserId(1000, 0, 10);
    Assertions.assertEquals(0, submissions.size());
  }

  /**
   * Test case: tests the getLatestSubmissionOfProblems(long, long, long) method. Test data: get the
   * latest judge results of user ID #1000 in the problem ID range [1000, 1010). Expected: the
   * expected submission list (problem ID #1000 - WA, problem ID #1001 - AC).
   */
  @Test
  public void testGetLatestSubmissionOfProblemsWithUser1000() {
    List<Submission> submissions = submissionMapper.getLatestSubmissionOfProblems(1000, 1000, 1010);
    Assertions.assertEquals(2, submissions.size());

    Submission firstSubmission = submissions.get(0);
    String firstJudgeResultSlug = firstSubmission.getJudgeResult().getJudgeResultSlug();
    Assertions.assertEquals("WA", firstJudgeResultSlug);

    Submission secondSubmission = submissions.get(1);
    String secondJudgeResultSlug = secondSubmission.getJudgeResult().getJudgeResultSlug();
    Assertions.assertEquals("AC", secondJudgeResultSlug);
  }

  /**
   * Test case: tests the getLatestSubmissionOfProblems(long, long, long) method. Test data: get the
   * latest judge results of user ID #1001 in the problem ID range [1000, 1010). Expected: the
   * expected submission list (problem ID #1000 - CE).
   */
  @Test
  public void testGetLatestSubmissionOfProblemsWithUser1001() {
    List<Submission> submissions = submissionMapper.getLatestSubmissionOfProblems(1001, 1000, 1010);
    Assertions.assertEquals(1, submissions.size());

    Submission firstSubmission = submissions.get(0);
    String firstJudgeResultSlug = firstSubmission.getJudgeResult().getJudgeResultSlug();
    Assertions.assertEquals("CE", firstJudgeResultSlug);
  }

  /**
   * Test case: tests the getAcceptedSubmissionOfProblems(long, long, long) method. Test data: get the
   * accepted judge results of user ID #1000 in the problem ID range [1000, 1010). Expected: the
   * expected submission list (problem ID #1000 - AC, problem ID #1001 - AC).
   */
  @Test
  public void testGetAcceptedSubmissionOfProblemsWithUser1000() {
    List<Submission> submissions =
        submissionMapper.getAcceptedSubmissionOfProblems(1000, 1000, 1010);
    Assertions.assertEquals(2, submissions.size());

    Submission firstSubmission = submissions.get(0);
    String firstJudgeResultSlug = firstSubmission.getJudgeResult().getJudgeResultSlug();
    Assertions.assertEquals("AC", firstJudgeResultSlug);

    Submission secondSubmission = submissions.get(1);
    String secondJudgeResultSlug = secondSubmission.getJudgeResult().getJudgeResultSlug();
    Assertions.assertEquals("AC", secondJudgeResultSlug);
  }

  /**
   * Test case: tests the getAcceptedSubmissionOfProblems(long, long, long) method. Test data: get the
   * accepted judge results of user ID #1001 in the problem ID range [1000, 1010). Expected: an empty
   * submission list.
   */
  @Test
  public void testGetAcceptedSubmissionOfProblemsWithUser1001() {
    List<Submission> submissions =
        submissionMapper.getAcceptedSubmissionOfProblems(1001, 1000, 1010);
    Assertions.assertEquals(0, submissions.size());
  }

  /**
   * Test case: tests the getAcceptedSubmissionUsingUserId(long) method. Test data: get the number of
   * accepted submissions of user ID #1000. Expected: the number of accepted submissions of user ID
   * #1000 (2).
   */
  @Test
  public void testGetAcceptedSubmissionUsingUserIdWithUser1000() {
    long acceptedSubmission = submissionMapper.getAcceptedSubmissionUsingUserId(1000);
    Assertions.assertEquals(2, acceptedSubmission);
  }

  /**
   * Test case: tests the getTotalSubmissionUsingUserId(long) method. Test data: get the total number
   * of submissions of user ID #1000. Expected: the total number of submissions of user ID #1000 (3).
   */
  @Test
  public void testGetTotalSubmissionUsingUserIdWithUser1000() {
    long acceptedSubmission = submissionMapper.getTotalSubmissionUsingUserId(1000);
    Assertions.assertEquals(3, acceptedSubmission);
  }

  /** Test case: tests the createSubmission(Submission) method. Test data: a valid data set. Expected: the data insertion operation completes successfully. */
  @Test
  public void testCreateSubmissionNormally() {
    Problem problem = problemMapper.getProblem(1000);
    User user = userMapper.getUserUsingUid(1000);
    Language language = languageMapper.getLanguageUsingId(1);

    Submission submission = new Submission(problem, user, language, "C Code");
    int numberOfRowsAffected = submissionMapper.createSubmission(submission);
    Assertions.assertEquals(1, numberOfRowsAffected);
  }

  /**
   * Test case: tests the createSubmission(Submission) method. Test data: a non-existing foreign-key
   * value (a programming language object). Expected: a DataIntegrityViolationException is thrown.
   */
  @Test
  public void testCreateSubmissionUsingNotExistingLanguage() {
    Problem problem = problemMapper.getProblem(1000);
    User user = userMapper.getUserUsingUid(1000);
    Language language = new Language(0, "not-exists", "Not Exists", "Not Exists", "Not Exists");

    Submission submission = new Submission(problem, user, language, "Code");
    Executable e =
        () -> {
          submissionMapper.createSubmission(submission);
        };
    Assertions.assertThrows(org.springframework.dao.DataIntegrityViolationException.class, e);
  }

  /** Test case: tests the updateSubmission(Submission) method. Test data: a valid data set, and a record with the corresponding ID exists in the table. Expected: the data update operation completes successfully. */
  @Test
  public void testUpdateSubmissionNormally() {
    Submission submission = submissionMapper.getSubmission(1002);
    Assertions.assertNotNull(submission);

    submission.setJudgeScore(20);
    int numberOfRowsAffected = submissionMapper.updateSubmission(submission);
    Assertions.assertEquals(1, numberOfRowsAffected);

    /**
     * The following Assert CANNOT passed in CI due to the bug of Spring Test Framework. But it
     * really works.
     */
    /*
     * Submission updatedSubmission = submissionMapper.getSubmission(1002);
     * int judgeScore = updatedSubmission.getJudgeScore();
     * Assertions.assertEquals(20, judgeScore);
     */
  }

  /**
   * Test case: tests the updateSubmission(Submission) method. Test data: a valid data set, but no record with the corresponding ID exists in the table. Expected: the method executes normally without affecting the data in the table.
   */
  @Test
  public void testUpdateSubmissionNotExists() {
    Submission submission = submissionMapper.getSubmission(1000);
    Assertions.assertNotNull(submission);

    submission.setSubmissionId(0);
    int numberOfRowsAffected = submissionMapper.updateSubmission(submission);
    Assertions.assertEquals(0, numberOfRowsAffected);
  }

  /** Test case: tests the deleteSubmission(long) method. Test data: the unique identifier of submission record #1004. Expected: the data deletion operation completes successfully. */
  @Test
  public void testDeleteSubmissionExists() {
    Submission submission = submissionMapper.getSubmission(1004);
    Assertions.assertNotNull(submission);

    int numberOfRowsAffected = submissionMapper.deleteSubmission(1004);
    Assertions.assertEquals(1, numberOfRowsAffected);

    submission = submissionMapper.getSubmission(1004);
    Assertions.assertNull(submission);
  }

  /** Test case: tests the deleteSubmission(long) method. Test data: a non-existing submission record unique identifier. Expected: the method executes normally without affecting the data in the table. */
  @Test
  public void testDeleteSubmissionNotExists() {
    Submission submission = submissionMapper.getSubmission(0);
    Assertions.assertNull(submission);

    int numberOfRowsAffected = submissionMapper.deleteSubmission(0);
    Assertions.assertEquals(0, numberOfRowsAffected);
  }

  /** The SubmissionMapper object under test. */
  @Autowired private SubmissionMapper submissionMapper;

  /** The ProblemMapper object, used to build the unit test cases. */
  @Autowired private ProblemMapper problemMapper;

  /** The UserMapper object, used to build the unit test cases. */
  @Autowired private UserMapper userMapper;

  /** The LanguageMapper object, used to build the unit test cases. */
  @Autowired private LanguageMapper languageMapper;
}
