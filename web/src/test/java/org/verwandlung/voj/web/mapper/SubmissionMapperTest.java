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

import java.util.Date;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
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
   * Test case: tests the getNumberOfSubmissionsUsingDate(Date, Date) method. Test data: query
   * using 2014-10-01 to 2014-10-18 (UTC). Expected: the number of submissions within this time range
   * (2).
   */
  @Test
  public void testGetNumberOfSubmissionsUsingDate() {
    Date startTime = utc("2014-10-01T00:00:00Z");
    Date endTime = utc("2014-10-18T00:00:00Z");

    long numberOfSubmissions = submissionMapper.getNumberOfSubmissionsUsingDate(startTime, endTime);
    Assertions.assertEquals(2, numberOfSubmissions);
  }

  /**
   * Test case: tests the getNumberOfSubmissionsGroupByDay(Date, Date, long, boolean, Date) method. Test
   * data: query using 2014-10 (UTC). Expected: the number of submissions per quarter of an hour of the
   * month.
   */
  @Test
  public void testGetNumberOfTotalSubmissionsGroupByDayWithoutUser() {
    Date startTime = utc("2014-10-01T00:00:00Z");
    Date endTime = utc("2014-11-01T00:00:00Z");

    List<Map<String, Object>> numberOfSubmissions =
        submissionMapper.getNumberOfSubmissionsGroupByDay(startTime, endTime, 0, false, null);
    Assertions.assertEquals(2, numberOfSubmissions.size());

    // Submission #1000 was made at 2014-10-01 00:00:00 UTC: the first quarter of an hour of the day.
    Map<String, Object> firstEntry = numberOfSubmissions.get(0);
    Assertions.assertEquals(
        startTime.getTime() / 900_000, ((Number) firstEntry.get("slot")).longValue());
    Assertions.assertEquals(1L, ((Number) firstEntry.get("submissions")).longValue());
  }

  /**
   * Test case: tests the getNumberOfSubmissionsGroupByDay(Date, Date, long, boolean, Date) method. Test
   * data: query the accepted submissions using 2014-10 (UTC). Expected: the number of
   * accepted submissions per quarter of an hour of the month.
   */
  @Test
  public void testGetNumberOfAccpectedSubmissionsGroupByDayWithoutUser() {
    Date startTime = utc("2014-10-01T00:00:00Z");
    Date endTime = utc("2014-11-01T00:00:00Z");

    List<Map<String, Object>> numberOfSubmissions =
        submissionMapper.getNumberOfSubmissionsGroupByDay(startTime, endTime, 0, true, null);
    Assertions.assertEquals(1, numberOfSubmissions.size());

    // Submission #1000 was made at 2014-10-01 00:00:00 UTC: the first quarter of an hour of the day.
    Map<String, Object> firstEntry = numberOfSubmissions.get(0);
    Assertions.assertEquals(
        startTime.getTime() / 900_000, ((Number) firstEntry.get("slot")).longValue());
    Assertions.assertEquals(1L, ((Number) firstEntry.get("submissions")).longValue());
  }

  /**
   * Test case: tests the getNumberOfSubmissionsGroupByDay(Date, Date, long, boolean, Date) method. Test
   * data: query a user's submissions using 2014-10 (UTC). Expected: the number of the
   * user's submissions per quarter of an hour of the month.
   */
  @Test
  public void testGetNumberOfTotalSubmissionsGroupByDayWithUser() {
    Date startTime = utc("2014-10-01T00:00:00Z");
    Date endTime = utc("2014-11-01T00:00:00Z");

    List<Map<String, Object>> numberOfSubmissions =
        submissionMapper.getNumberOfSubmissionsGroupByDay(startTime, endTime, 1000, false, null);
    Assertions.assertEquals(2, numberOfSubmissions.size());

    // Submission #1000 was made at 2014-10-01 00:00:00 UTC: the first quarter of an hour of the day.
    Map<String, Object> firstEntry = numberOfSubmissions.get(0);
    Assertions.assertEquals(
        startTime.getTime() / 900_000, ((Number) firstEntry.get("slot")).longValue());
    Assertions.assertEquals(1L, ((Number) firstEntry.get("submissions")).longValue());
  }

  /**
   * Test case: tests the getNumberOfSubmissionsGroupByDay(Date, Date, long, boolean, Date) method. Test
   * data: query a user's accepted submissions using 2014-10 (UTC). Expected: the number
   * of the user's accepted submissions per quarter of an hour of the month.
   */
  @Test
  public void testGetNumberOfAccpectedSubmissionsGroupByDayWithUser() {
    Date startTime = utc("2014-10-01T00:00:00Z");
    Date endTime = utc("2014-11-01T00:00:00Z");

    List<Map<String, Object>> numberOfSubmissions =
        submissionMapper.getNumberOfSubmissionsGroupByDay(startTime, endTime, 1000, true, null);
    Assertions.assertEquals(1, numberOfSubmissions.size());

    // Submission #1000 was made at 2014-10-01 00:00:00 UTC: the first quarter of an hour of the day.
    Map<String, Object> firstEntry = numberOfSubmissions.get(0);
    Assertions.assertEquals(
        startTime.getTime() / 900_000, ((Number) firstEntry.get("slot")).longValue());
    Assertions.assertEquals(1L, ((Number) firstEntry.get("submissions")).longValue());
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
   * Test case: tests the getSubmissions(long, String, String, Long, Long, long, int, boolean, long, Date) method. Test data: get 10
   * submissions with IDs starting from 1010 (ID From 1010 to 1000). Expected: the submission list.
   */
  @Test
  public void testGetSubmissionsFrom1010WithLimit10() {
    List<Submission> submissions = submissionMapper.getSubmissions(0, "", "", 1010L, null, 0, 10, false, 0, null);
    Assertions.assertEquals(5, submissions.size());

    Submission firstSubmission = submissions.get(0);
    long submissionId = firstSubmission.getSubmissionId();
    Assertions.assertEquals(1004, submissionId);
  }

  /**
   * Test case: tests the getSubmissions(long, String, String, Long, Long, long, int, boolean, long, Date) method. Test data: get 2
   * submissions with IDs starting from 1003. Expected: the submission list (2 submissions).
   */
  @Test
  public void testGetSubmissionsFrom1003WithLimit2() {
    List<Submission> submissions = submissionMapper.getSubmissions(0, "", "", 1003L, null, 0, 2, false, 0, null);
    Assertions.assertEquals(2, submissions.size());

    Submission firstSubmission = submissions.get(0);
    long submissionId = firstSubmission.getSubmissionId();
    Assertions.assertEquals(1003, submissionId);
  }

  /**
   * Test case: tests the getSubmissions(long, String, String, Long, Long, long, int, boolean, long, Date) method. Test data: get 10
   * submissions with IDs starting from 100 (ID From 100 to 90). Expected: an empty submission list.
   */
  @Test
  public void testGetSubmissionsFrom100WithLimit10() {
    List<Submission> submissions = submissionMapper.getSubmissions(0, "", "", 100L, null, 0, 10, false, 0, null);
    Assertions.assertEquals(0, submissions.size());
  }

  /**
   * Test case: tests the getSubmissions(long, String, String, Long, Long, long, int, boolean, long,
   * Date) method. Test data: the submissions of user zjhzxhz from #1001 on, and those that are
   * Compile Errors, the second page of one. Expected: the first in ascending order, the second the
   * older Compile Error; neither carries a judge log or code.
   */
  @Test
  public void testGetSubmissionsUsingFilters() {
    List<Submission> latest =
        submissionMapper.getSubmissions(0, "zjhzxhz", "", null, 1001L, 0, 10, false, 0, null);
    Assertions.assertEquals(List.of(1001L, 1003L), latest.stream().map(Submission::getSubmissionId).toList());

    List<Submission> compileErrors =
        submissionMapper.getSubmissions(0, "", "CE", null, null, 1, 1, false, 0, null);
    Assertions.assertEquals(1, compileErrors.size());
    Submission submission = compileErrors.get(0);
    Assertions.assertEquals(1002, submission.getSubmissionId());
    Assertions.assertEquals("CE", submission.getJudgeResult().getJudgeResultSlug());
    Assertions.assertEquals(1001, submission.getUser().getUid());
    Assertions.assertNull(submission.getJudgeLog());
    Assertions.assertNull(submission.getCode());
  }

  /**
   * Test case: tests the getSubmissionWithoutCode(long) method. Test data: submission #1000.
   * Expected: the submission with its judge log but without its code.
   */
  @Test
  public void testGetSubmissionWithoutCode() {
    Submission submission = submissionMapper.getSubmissionWithoutCode(1000);
    Assertions.assertEquals(1000, submission.getProblem().getProblemId());
    Assertions.assertEquals("AC", submission.getJudgeResult().getJudgeResultSlug());
    Assertions.assertNotNull(submission.getJudgeLog());
    Assertions.assertNull(submission.getCode());
    Assertions.assertNotNull(submissionMapper.getSubmission(1000).getCode());
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
    long acceptedSubmission = submissionMapper.getAcceptedSubmissionUsingUserId(1000, null);
    Assertions.assertEquals(2, acceptedSubmission);
  }

  /**
   * Test case: tests the getTotalSubmissionUsingUserId(long) method. Test data: get the total number
   * of submissions of user ID #1000. Expected: the total number of submissions of user ID #1000 (3).
   */
  @Test
  public void testGetTotalSubmissionUsingUserIdWithUser1000() {
    long acceptedSubmission = submissionMapper.getTotalSubmissionUsingUserId(1000, null);
    Assertions.assertEquals(3, acceptedSubmission);
  }

  /**
   * Test case: tests the getAcceptedSubmissionUsingUserId(long) and getTotalSubmissionUsingUserId
   * (long) methods. Test data: user ID #1000 gets an accepted submission to the hidden problem #1002.
   * Expected: neither count changes, because the profile statistics only cover published problems.
   */
  @Test
  public void testSubmissionStatsIgnoreUnpublishedProblems() {
    new JdbcTemplate(dataSource)
        .update(
            "INSERT INTO voj_submissions (problem_id, uid, language_id, submission_judge_result,"
                + " submission_code) VALUES (1002, 1000, 2, 'AC', 'Code')");

    Assertions.assertEquals(2, submissionMapper.getAcceptedSubmissionUsingUserId(1000, null));
    Assertions.assertEquals(3, submissionMapper.getTotalSubmissionUsingUserId(1000, null));
  }

  /**
   * Test case: tests the getSolvedProblemCountForAllUsers() method. Test data: the seeded
   * submissions. Expected: user ID #1000 appears with a solved-problem count of 2 (problems #1000 and
   * #1001), matching getAcceptedSubmissionUsingUserId.
   */
  @Test
  public void testGetSolvedProblemCountForAllUsers() {
    List<Map<String, Object>> rows = submissionMapper.getSolvedProblemCountForAllUsers();
    Assertions.assertNotNull(rows);

    boolean foundUser1000 = false;
    for (Map<String, Object> row : rows) {
      long uid = ((Number) row.get("uid")).longValue();
      long solved = ((Number) row.get("solved")).longValue();
      if (uid == 1000) {
        foundUser1000 = true;
        Assertions.assertEquals(2, solved);
      }
    }
    Assertions.assertTrue(foundUser1000);
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

  /** The DataSource of the test database, used to insert fixtures the mappers cannot create. */
  @Autowired private javax.sql.DataSource dataSource;

  /** The ProblemMapper object, used to build the unit test cases. */
  @Autowired private ProblemMapper problemMapper;

  /** The UserMapper object, used to build the unit test cases. */
  @Autowired private UserMapper userMapper;

  /** Parses an instant, e.g. {@code 2014-10-01T00:00:00Z}, into a Date. */
  private static Date utc(String instant) {
    return Date.from(java.time.Instant.parse(instant));
  }

  /** The LanguageMapper object, used to build the unit test cases. */
  @Autowired private LanguageMapper languageMapper;
}
