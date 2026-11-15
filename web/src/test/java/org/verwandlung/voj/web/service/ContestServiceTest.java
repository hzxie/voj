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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

import org.verwandlung.voj.web.mapper.ContestMapper;
import org.verwandlung.voj.web.model.Contest;
import org.verwandlung.voj.web.model.ContestContestant;
import org.verwandlung.voj.web.model.ContestSubmission;
import org.verwandlung.voj.web.model.Problem;
import org.verwandlung.voj.web.model.PublicationStatus;
import org.verwandlung.voj.web.model.Submission;
import org.verwandlung.voj.web.model.User;

/**
 * The test class for ContestService.
 *
 * <p>The status of the contests in the seed data (relative to the current time): Contest #1 (OI,
 * ended), Contest #2 (ACM, ongoing), Contest #3 (ACM, not started).
 *
 * @author Haozhe Xie
 */
@ExtendWith(SpringExtension.class)
@Transactional
@ContextConfiguration({"classpath:test-spring-context.xml"})
public class ContestServiceTest {
  /** Test case: tests the getContests(String, boolean, long, int) method. Test data: an empty keyword. Expected: all contests. */
  @Test
  public void testGetContests() {
    List<Contest> contests = contestService.getContests("", false, 0, 10);
    Assertions.assertEquals(3, contests.size());
  }

  /** Test case: tests the getContests(String, boolean, long, int) method. Test data: an empty keyword, published contests only. Expected: the two published contests. */
  @Test
  public void testGetContestsPublicOnly() {
    List<Contest> contests = contestService.getContests("", true, 0, 10);
    Assertions.assertEquals(2, contests.size());
  }

  /** Test case: tests the getContest(long) method. Test data: an existing contest identifier. Expected: the corresponding contest object. */
  @Test
  public void testGetContestExists() {
    Contest contest = contestService.getContest(1);
    Assertions.assertNotNull(contest);
    Assertions.assertEquals("OI", contest.getContestMode());
  }

  /** Test case: tests the getContest(long) method. Test data: a non-existing contest identifier. Expected: a null reference. */
  @Test
  public void testGetContestNotExists() {
    Assertions.assertNull(contestService.getContest(0));
  }

  /** Test case: tests the getProblemsOfContests(List) method. Test data: a mix of existing and non-existing problem IDs. Expected: only the existing problems. */
  @Test
  public void testGetProblemsOfContests() {
    List<Problem> problems =
        contestService.getProblemsOfContests(Arrays.asList(1000L, 1001L, 99999L));
    Assertions.assertEquals(2, problems.size());

    // The two existing ids must resolve to their problems and the missing 99999
    // must be dropped - a size-only check would miss returning the wrong problems.
    java.util.Set<Long> problemIds = new java.util.HashSet<>();
    for (Problem problem : problems) {
      problemIds.add(problem.getProblemId());
    }
    Assertions.assertEquals(java.util.Set.of(1000L, 1001L), problemIds);
  }

  /** Test case: tests the getSubmissionsOfContestantOfContest(long, User) method. Test data: the contestant is null. Expected: returns null. */
  @Test
  public void testGetSubmissionsOfContestantOfContestWithNullContestant() {
    Assertions.assertNull(contestService.getSubmissionsOfContestantOfContest(1, null));
  }

  /** Test case: tests the getSubmissionsOfContestantOfContest(long, User) method. Test data: contestant 1000 attended Contest #1. Expected: the submission records grouped by problem. */
  @Test
  public void testGetSubmissionsOfContestantOfContest() {
    Map<Long, ContestSubmission> submissions =
        contestService.getSubmissionsOfContestantOfContest(1, userWithUid(1000));
    // Contestant 1000 has submission records for problems 1000 and 1001 in Contest #1.
    Assertions.assertEquals(2, submissions.size());
    Assertions.assertTrue(submissions.containsKey(1000L));
    Assertions.assertTrue(submissions.containsKey(1001L));
  }

  /** Test case: tests the getSubmissionsOfContestantOfContestProblem(...) method. Test data: the contest or the contestant is null. Expected: returns null. */
  @Test
  public void testGetSubmissionsOfContestantOfContestProblemWithNullArguments() {
    Assertions.assertNull(
        contestService.getSubmissionsOfContestantOfContestProblem(null, 1000, userWithUid(1000)));
    Assertions.assertNull(
        contestService.getSubmissionsOfContestantOfContestProblem(
            contestService.getContest(1), 1000, null));
  }

  /** Test case: tests the getSubmissionsOfContestantOfContestProblem(...) method. Test data: the ended Contest #1. Expected: the contestant's submission records for the problem. */
  @Test
  public void testGetSubmissionsOfContestantOfContestProblem() {
    List<Submission> submissions =
        contestService.getSubmissionsOfContestantOfContestProblem(
            contestService.getContest(1), 1000, userWithUid(1000));
    Assertions.assertEquals(1, submissions.size());

    // Pin down which submission came back: the (contest, problem, contestant)
    // lookup is a three-way join, so a wrong predicate could return one wrong
    // row and still pass a size-only check.
    Submission submission = submissions.get(0);
    Assertions.assertEquals(1000, submission.getSubmissionId());
    Assertions.assertEquals(1000, submission.getProblem().getProblemId());
  }

  /** Test case: tests the getSubmissionsOfContestantOfContestProblem(...) method. Test data: the not-started Contest #3. Expected: an empty list. */
  @Test
  public void testGetSubmissionsOfContestantOfContestProblemForReadyContest() {
    List<Submission> submissions =
        contestService.getSubmissionsOfContestantOfContestProblem(
            contestService.getContest(3), 1003, userWithUid(1000));
    Assertions.assertTrue(submissions.isEmpty());
  }

  /** Test case: tests the getCodeSnippetOfContestProblem(...) method. Test data: the contest is null or is not an ongoing OI contest. Expected: returns null. */
  @Test
  public void testGetCodeSnippetOfContestProblemReturnsNull() {
    Assertions.assertNull(
        contestService.getCodeSnippetOfContestProblem(null, 1000, userWithUid(1000)));
    // Contest #2 uses the ACM mode, so no code snapshot is returned.
    Assertions.assertNull(
        contestService.getCodeSnippetOfContestProblem(
            contestService.getContest(2), 1001, userWithUid(1000)));
  }

  /** Test case: tests the getNumberOfContestantsOfContest(long) method. Test data: each contest. Expected: the correct number of participants. */
  @Test
  public void testGetNumberOfContestantsOfContest() {
    Assertions.assertEquals(2, contestService.getNumberOfContestantsOfContest(1));
    Assertions.assertEquals(1, contestService.getNumberOfContestantsOfContest(2));
    Assertions.assertEquals(0, contestService.getNumberOfContestantsOfContest(3));
  }

  /** Test case: tests the isAttendContest(long, User) method. Test data: attended / not attended / not logged in. Expected: the correct attendance status. */
  @Test
  public void testIsAttendContest() {
    Assertions.assertTrue(contestService.isAttendContest(1, userWithUid(1000)));
    Assertions.assertFalse(contestService.isAttendContest(1, userWithUid(9999)));
    Assertions.assertFalse(contestService.isAttendContest(1, null));
  }

  /** Test case: tests the attendContest(...) method. Test data: all conditions are met to attend the not-started Contest #3 once it is published. Expected: attendance succeeds and the number of participants increases by one. */
  @Test
  public void testAttendContestSuccessfully() {
    publishContest(3);
    Map<String, Boolean> result = contestService.attendContest(3, userWithUid(1000));
    Assertions.assertTrue(result.get("isSuccessful"));
    Assertions.assertEquals(1, contestService.getNumberOfContestantsOfContest(3));
  }

  /** Test case: tests the attendContest(...) method. Test data: the not-started but hidden Contest #3. Expected: attendance fails as if the contest did not exist. */
  @Test
  public void testAttendContestThatIsNotPublished() {
    Map<String, Boolean> result = contestService.attendContest(3, userWithUid(1000));
    Assertions.assertFalse(result.get("isContestExists"));
    Assertions.assertFalse(result.get("isSuccessful"));
  }

  /** Test case: tests the attendContest(...) method. Test data: the contest has ended (not in the not-started state). Expected: attendance fails. */
  @Test
  public void testAttendContestThatIsNotReady() {
    Map<String, Boolean> result = contestService.attendContest(1, userWithUid(1000));
    Assertions.assertFalse(result.get("isContestReady"));
    Assertions.assertFalse(result.get("isSuccessful"));
  }

  /** Test case: tests the attendContest(...) method. Test data: the user is not logged in. Expected: attendance fails. */
  @Test
  public void testAttendContestWithoutLogin() {
    Map<String, Boolean> result = contestService.attendContest(3, null);
    Assertions.assertFalse(result.get("isUserLogin"));
    Assertions.assertFalse(result.get("isSuccessful"));
  }

  /** Test case: tests the attendContest(...) method. Test data: attending the same contest again. Expected: the second attendance fails. */
  @Test
  public void testAttendContestThatIsAlreadyAttended() {
    publishContest(3);
    contestService.attendContest(3, userWithUid(1000));

    Map<String, Boolean> result = contestService.attendContest(3, userWithUid(1000));
    Assertions.assertTrue(result.get("isAttendedContest"));
    Assertions.assertFalse(result.get("isSuccessful"));
  }

  /** Test case: tests the getLeaderBoardForOi(long, User) method. Test data: the OI-mode Contest #1. Expected: the contestants and submission records, with rankings completed. */
  @Test
  @SuppressWarnings("unchecked")
  public void testGetLeaderBoardForOi() {
    Map<String, Object> leaderBoard = contestService.getLeaderBoardForOi(1, null);
    Assertions.assertTrue(leaderBoard.containsKey("contestants"));
    Assertions.assertTrue(leaderBoard.containsKey("submissions"));

    List<ContestContestant> contestants = (List<ContestContestant>) leaderBoard.get("contestants");
    Assertions.assertEquals(2, contestants.size());
    Assertions.assertEquals(1, contestants.get(0).getRank());
  }

  /** Test case: tests the getLeaderBoardForAcm(long, User) method. Test data: the ACM-mode Contest #2. Expected: the contestants and submission records, with rankings completed. */
  @Test
  @SuppressWarnings("unchecked")
  public void testGetLeaderBoardForAcm() {
    Map<String, Object> leaderBoard = contestService.getLeaderBoardForAcm(2, null);
    Assertions.assertTrue(leaderBoard.containsKey("contestants"));
    Assertions.assertTrue(leaderBoard.containsKey("submissions"));

    List<ContestContestant> contestants = (List<ContestContestant>) leaderBoard.get("contestants");
    Assertions.assertEquals(1, contestants.size());
    Assertions.assertEquals(1, contestants.get(0).getRank());
  }

  /**
   * Test case: tests the getLeaderBoardForAcm(long, User) method on a running contest (penalty 20
   * minutes). Test data: user 1000 gets WA (+10 min), CE (+11), AC (+20) and WA (+30) on P1000, AC
   * (+40) on P1001 and an unsolved WA (+5) on P1003; user 1001 gets AC (+15) and AC (+50) on P1000;
   * user 1002 submits nothing. Expected: scores count distinct solved problems (2 / 1 / 0); only the
   * WA before the first AC is penalised (CE, the WA after the AC and the unsolved problem are not);
   * the time of a problem is that of its first AC.
   */
  @Test
  @SuppressWarnings("unchecked")
  public void testGetLeaderBoardForAcmScoresAndPenalty() {
    long start = createAcmContestFixture(0);

    List<ContestContestant> raw =
        contestContestantMapper.getContestantsOfContestForAcm(FIXTURE_CONTEST_ID, null, 0, 10);
    Map<Long, ContestContestant> rawByUid = new java.util.HashMap<>();
    raw.forEach(cc -> rawByUid.put(cc.getContestant().getUid(), cc));
    Assertions.assertEquals(2, rawByUid.get(1000L).getScore());
    Assertions.assertEquals(1, rawByUid.get(1000L).getTime());
    Assertions.assertEquals(1, rawByUid.get(1001L).getScore());
    Assertions.assertEquals(0, rawByUid.get(1001L).getTime());
    Assertions.assertEquals(0, rawByUid.get(1002L).getScore());

    Map<String, Object> leaderBoard = contestService.getLeaderBoardForAcm(FIXTURE_CONTEST_ID, null);
    List<ContestContestant> contestants = (List<ContestContestant>) leaderBoard.get("contestants");
    Assertions.assertEquals(List.of(1000L, 1001L, 1002L), uidsOf(contestants));
    // 1 rejected attempt * 20 min + 20 min (P1000) + 40 min (P1001)
    Assertions.assertEquals(20 * 60 + 20 * 60 + 40 * 60, contestants.get(0).getTime());
    Assertions.assertEquals(15 * 60, contestants.get(1).getTime());
    Assertions.assertEquals(0, contestants.get(2).getTime());
    Assertions.assertEquals(List.of(1L, 2L, 3L), ranksOf(contestants));

    Map<Long, Map<Long, Submission>> submissions =
        (Map<Long, Map<Long, Submission>>) leaderBoard.get("submissions");
    Submission firstAc = submissions.get(1001L).get(1000L);
    Assertions.assertEquals(start + 15 * 60_000L, firstAc.getSubmitTime().getTime());
    Assertions.assertNull(leaderBoard.get("frozenAt"));
  }

  /**
   * Test case: tests the getLeaderBoardForAcm(long, User) method with a scoreboard freeze. Test
   * data: the fixture above with a 150-minute freeze, i.e. frozen from 30 minutes after the start.
   * Expected: for a regular viewer, user 1000's AC on P1001 (+40 min) is hidden so user 1001 (one
   * problem, less penalty) leads; an administrator still sees the full leaderboard.
   */
  @Test
  @SuppressWarnings("unchecked")
  public void testGetLeaderBoardForAcmHonoursFreeze() {
    createAcmContestFixture(150);

    Map<String, Object> leaderBoard =
        contestService.getLeaderBoardForAcm(FIXTURE_CONTEST_ID, userWithUid(1002));
    Assertions.assertNotNull(leaderBoard.get("frozenAt"));
    List<ContestContestant> contestants = (List<ContestContestant>) leaderBoard.get("contestants");
    Assertions.assertEquals(List.of(1001L, 1000L, 1002L), uidsOf(contestants));
    Assertions.assertEquals(1, contestants.get(1).getScore());
    Assertions.assertEquals(20 * 60 + 20 * 60, contestants.get(1).getTime());
    Map<Long, Map<Long, Submission>> submissions =
        (Map<Long, Map<Long, Submission>>) leaderBoard.get("submissions");
    Assertions.assertFalse(submissions.get(1000L).containsKey(1001L));

    User administrator = userWithUid(1000);
    administrator.setUserGroup(
        new org.verwandlung.voj.web.model.UserGroup(8, "administrators", "Administrators"));
    leaderBoard = contestService.getLeaderBoardForAcm(FIXTURE_CONTEST_ID, administrator);
    Assertions.assertNull(leaderBoard.get("frozenAt"));
    contestants = (List<ContestContestant>) leaderBoard.get("contestants");
    Assertions.assertEquals(List.of(1000L, 1001L, 1002L), uidsOf(contestants));
  }

  /**
   * Test case: tests the getLeaderBoardForOi(long, User) method. Test data: user 1000 scores 100 then
   * 40 on P1000 and 60 on P1001; user 1001 scores 70 on P1000. Expected: only the last submission per
   * problem counts, for the score (40 + 60) and the time (50 + 10 ms), and the leaderboard cells show
   * that last submission.
   */
  @Test
  @SuppressWarnings("unchecked")
  public void testGetLeaderBoardForOiCountsLastSubmissionPerProblem() {
    long now = System.currentTimeMillis() / 1000 * 1000;
    long start = now - 2 * 3600_000L;
    insertContest(FIXTURE_CONTEST_ID, "OI", "IOI", start, now + 3600_000L, 0);
    insertContestant(FIXTURE_CONTEST_ID, 1000);
    insertContestant(FIXTURE_CONTEST_ID, 1001);
    insertSubmission(1000, 1000, "AC", 100, 30, start + 10 * 60_000L);
    insertSubmission(1001, 1000, "WA", 70, 5, start + 12 * 60_000L);
    long last = insertSubmission(1000, 1000, "WA", 40, 50, start + 20 * 60_000L);
    insertSubmission(1000, 1001, "WA", 60, 10, start + 30 * 60_000L);

    Map<String, Object> leaderBoard = contestService.getLeaderBoardForOi(FIXTURE_CONTEST_ID, null);
    List<ContestContestant> contestants = (List<ContestContestant>) leaderBoard.get("contestants");
    Assertions.assertEquals(List.of(1000L, 1001L), uidsOf(contestants));
    Assertions.assertEquals(100, contestants.get(0).getScore());
    Assertions.assertEquals(60, contestants.get(0).getTime());
    Assertions.assertEquals(70, contestants.get(1).getScore());
    Map<Long, Map<Long, Submission>> submissions =
        (Map<Long, Map<Long, Submission>>) leaderBoard.get("submissions");
    Assertions.assertEquals(last, submissions.get(1000L).get(1000L).getSubmissionId());
  }

  /** Test case: tests ContestContestant.compareTo(Object). Test data: two contestants with equal score and time. Expected: a consistent order (sgn(a.compareTo(b)) == -sgn(b.compareTo(a))), and 0 against itself. */
  @Test
  public void testCompareToIsConsistentOnFullTies() {
    ContestContestant a = contestantWithTime(10);
    a.setContestant(userWithUid(1000));
    ContestContestant b = contestantWithTime(10);
    b.setContestant(userWithUid(1001));

    Assertions.assertEquals(
        Integer.signum(a.compareTo(b)), -Integer.signum(b.compareTo(a)));
    Assertions.assertNotEquals(0, a.compareTo(b));
    Assertions.assertEquals(0, a.compareTo(a));
  }

  /**
   * Test case: tests createSubmission(long, long, User, String, String). Test data: a running
   * contest attended by user 1000 whose problem list holds the hidden problem 1002. Expected: the
   * submission and its contest record are created, and the submission is viewable by the
   * contestant through the contest but not by a user who did not attend.
   */
  @Test
  public void testCreateContestSubmissionToHiddenContestProblem() {
    createRunningContestWithHiddenProblem();

    Map<String, Object> result =
        contestService.createSubmission(
            FIXTURE_CONTEST_ID, 1002, userWithUid(1000), "text/x-java", "Code");
    Assertions.assertEquals(Boolean.TRUE, result.get("isSuccessful"), result.toString());
    long submissionId = (Long) result.get("submissionId");
    Assertions.assertEquals(
        List.of(FIXTURE_CONTEST_ID),
        contestSubmissionMapper.getContestIdsOfSubmission(submissionId));

    Submission submission = submissionService.getSubmission(submissionId);
    Assertions.assertTrue(
        contestService.isSubmissionViewableThroughContest(submission, userWithUid(1000)));
    Assertions.assertFalse(
        contestService.isSubmissionViewableThroughContest(submission, userWithUid(1002)));
    Assertions.assertFalse(contestService.isSubmissionViewableThroughContest(submission, null));

    // The normal submission path still refuses the hidden problem.
    Assertions.assertEquals(
        Boolean.FALSE,
        submissionService
            .createSubmission(userWithUid(1000), 1002, "text/x-java", "Code")
            .get("isProblemExists"));
  }

  /** Test case: tests createSubmission(long, long, User, String, String). Test data: user 1002, who did not attend the running contest. Expected: rejected with isAttended = false. */
  @Test
  public void testCreateContestSubmissionWithoutAttending() {
    createRunningContestWithHiddenProblem();

    Map<String, Object> result =
        contestService.createSubmission(
            FIXTURE_CONTEST_ID, 1002, userWithUid(1002), "text/x-java", "Code");
    Assertions.assertEquals(Boolean.FALSE, result.get("isAttended"));
    Assertions.assertEquals(Boolean.FALSE, result.get("isSuccessful"));
  }

  /** Test case: tests createSubmission(long, long, User, String, String). Test data: an anonymous user. Expected: rejected with isUserLogined = false. */
  @Test
  public void testCreateContestSubmissionWithoutLogin() {
    createRunningContestWithHiddenProblem();

    Map<String, Object> result =
        contestService.createSubmission(FIXTURE_CONTEST_ID, 1002, null, "text/x-java", "Code");
    Assertions.assertEquals(Boolean.FALSE, result.get("isUserLogined"));
    Assertions.assertEquals(Boolean.FALSE, result.get("isSuccessful"));
  }

  /** Test case: tests createSubmission(long, long, User, String, String). Test data: a problem that is not in the running Contest #2. Expected: rejected with isProblemExists = false. */
  @Test
  public void testCreateContestSubmissionToProblemOutsideContest() {
    Map<String, Object> result =
        contestService.createSubmission(2, 1000, userWithUid(1000), "text/x-java", "Code");
    Assertions.assertEquals(Boolean.TRUE, result.get("isContestLive"));
    Assertions.assertEquals(Boolean.FALSE, result.get("isProblemExists"));
    Assertions.assertEquals(Boolean.FALSE, result.get("isSuccessful"));
  }

  /** Test case: tests createSubmission(long, long, User, String, String). Test data: the ended Contest #1, attended by user 1000. Expected: rejected with isContestLive = false. */
  @Test
  public void testCreateContestSubmissionToEndedContest() {
    Map<String, Object> result =
        contestService.createSubmission(1, 1000, userWithUid(1000), "text/x-java", "Code");
    Assertions.assertEquals(Boolean.TRUE, result.get("isContestExists"));
    Assertions.assertEquals(Boolean.FALSE, result.get("isContestLive"));
    Assertions.assertEquals(Boolean.FALSE, result.get("isSuccessful"));
  }

  /** Test case: tests createSubmission(long, long, User, String, String). Test data: the hidden Contest #3. Expected: reported as missing to a regular user. */
  @Test
  public void testCreateContestSubmissionToHiddenContest() {
    Map<String, Object> result =
        contestService.createSubmission(3, 1000, userWithUid(1000), "text/x-java", "Code");
    Assertions.assertEquals(Boolean.FALSE, result.get("isContestExists"));
    Assertions.assertEquals(Boolean.FALSE, result.get("isSuccessful"));
  }

  /** Test case: tests isContestProblemsViewable(Contest, User). Test data: not-started, running and ended contests. Expected: nobody but administrators before the start (even attendees), attendees only while running, everyone after the end. */
  @Test
  public void testIsContestProblemsViewable() {
    User administrator = userWithUid(1001);
    administrator.setUserGroup(
        new org.verwandlung.voj.web.model.UserGroup(8, "administrators", "Administrators"));

    // Contest #3 has not started; user 1000 attends it.
    publishContest(3);
    contestService.attendContest(3, userWithUid(1000));
    Contest notStarted = contestService.getContest(3);
    Assertions.assertFalse(contestService.isContestProblemsViewable(notStarted, userWithUid(1000)));
    Assertions.assertTrue(contestService.isContestProblemsViewable(notStarted, administrator));

    // Contest #2 is running; only user 1000 attends it.
    Contest running = contestService.getContest(2);
    Assertions.assertTrue(contestService.isContestProblemsViewable(running, userWithUid(1000)));
    Assertions.assertFalse(contestService.isContestProblemsViewable(running, userWithUid(1002)));
    Assertions.assertFalse(contestService.isContestProblemsViewable(running, null));

    // Contest #1 has ended.
    Assertions.assertTrue(contestService.isContestProblemsViewable(contestService.getContest(1), null));
  }

  /** Creates a running contest (problems 1001 and the hidden 1002) attended by user 1000. */
  private void createRunningContestWithHiddenProblem() {
    long now = System.currentTimeMillis() / 1000 * 1000;
    insertContest(FIXTURE_CONTEST_ID, "ACM", "ICPC", now - 3600_000L, now + 3600_000L, 0);
    jdbcTemplate()
        .update(
            "UPDATE voj_contests SET contest_problems = '[1001, 1002]' WHERE contest_id = ?",
            FIXTURE_CONTEST_ID);
    insertContestant(FIXTURE_CONTEST_ID, 1000);
  }

  /**
   * Creates a running ACM contest (started two hours ago, ending in one hour, 20-minute penalty)
   * with the submissions described in testGetLeaderBoardForAcmScoresAndPenalty.
   *
   * @param freeze - the scoreboard freeze in minutes
   * @return the start time of the contest in milliseconds
   */
  private long createAcmContestFixture(int freeze) {
    long now = System.currentTimeMillis() / 1000 * 1000;
    long start = now - 2 * 3600_000L;
    insertContest(FIXTURE_CONTEST_ID, "ACM", "ICPC", start, now + 3600_000L, freeze);
    insertContestant(FIXTURE_CONTEST_ID, 1000);
    insertContestant(FIXTURE_CONTEST_ID, 1001);
    insertContestant(FIXTURE_CONTEST_ID, 1002);
    // Inserted in chronological order so that submission ids follow the submit time.
    insertSubmission(1000, 1003, "WA", 0, 10, start + 5 * 60_000L);
    insertSubmission(1000, 1000, "WA", 0, 10, start + 10 * 60_000L);
    insertSubmission(1000, 1000, "CE", 0, 0, start + 11 * 60_000L);
    insertSubmission(1001, 1000, "AC", 100, 10, start + 15 * 60_000L);
    insertSubmission(1000, 1000, "AC", 100, 10, start + 20 * 60_000L);
    insertSubmission(1000, 1000, "WA", 0, 10, start + 30 * 60_000L);
    insertSubmission(1000, 1001, "AC", 100, 10, start + 40 * 60_000L);
    insertSubmission(1001, 1000, "AC", 100, 10, start + 50 * 60_000L);
    return start;
  }

  /** Inserts a published contest over problems 1000, 1001 and 1003. */
  private void insertContest(
      long contestId, String mode, String scoring, long start, long end, int freeze) {
    jdbcTemplate()
        .update(
            "INSERT INTO voj_contests (contest_id, contest_name, contest_notes, contest_start_time,"
                + " contest_end_time, contest_mode, contest_problems, contest_status,"
                + " contest_scoring, contest_penalty, contest_freeze) VALUES (?, 'Fixture', '', ?,"
                + " ?, ?, '[1000, 1001, 1003]', 'PUBLISHED', ?, 20, ?)",
            contestId,
            new java.sql.Timestamp(start),
            new java.sql.Timestamp(end),
            mode,
            scoring,
            freeze);
  }

  /** Registers a user as a contestant. */
  private void insertContestant(long contestId, long uid) {
    jdbcTemplate()
        .update(
            "INSERT INTO voj_contest_contestants (contest_id, contestant_uid) VALUES (?, ?)",
            contestId,
            uid);
  }

  /**
   * Inserts a judged submission and attaches it to the fixture contest.
   *
   * @return the identifier of the new submission
   */
  private long insertSubmission(
      long uid, long problemId, String judgeResult, int score, int usedTime, long submitTime) {
    org.springframework.jdbc.core.JdbcTemplate jdbcTemplate = jdbcTemplate();
    jdbcTemplate.update(
        "INSERT INTO voj_submissions (problem_id, uid, language_id, submission_submit_time,"
            + " submission_used_time, submission_used_memory, submission_judge_result,"
            + " submission_judge_score, submission_code) VALUES (?, ?, 2, ?, ?, 0, ?, ?, 'Code')",
        problemId,
        uid,
        new java.sql.Timestamp(submitTime),
        usedTime,
        judgeResult,
        score);
    long submissionId = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    jdbcTemplate.update(
        "INSERT INTO voj_contest_submissions (contest_id, submission_id) VALUES (?, ?)",
        FIXTURE_CONTEST_ID,
        submissionId);
    return submissionId;
  }

  /** Creates a JdbcTemplate on the test DataSource (it joins the test transaction). */
  private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate() {
    return new org.springframework.jdbc.core.JdbcTemplate(dataSource);
  }

  /** Collects the contestant UIDs of a leaderboard, in order. */
  private static List<Long> uidsOf(List<ContestContestant> contestants) {
    return contestants.stream().map(cc -> cc.getContestant().getUid()).toList();
  }

  /** Collects the ranks of a leaderboard, in order. */
  private static List<Long> ranksOf(List<ContestContestant> contestants) {
    return contestants.stream().map(ContestContestant::getRank).toList();
  }

  /** The identifier of the contest created by the leaderboard fixtures. */
  private static final long FIXTURE_CONTEST_ID = 100;

  /** Test case: tests the rankingContestants(List) method. Test data: a list of contestants with tied ranks. Expected: equal scores are tied, and the subsequent ranks are skipped. */
  @Test
  public void testRankingContestants() {
    List<ContestContestant> contestants = new ArrayList<>();
    // Scores all default to 0; ranks are distinguished by penalty time: the first two have the
    // same penalty time (tied for 1st), and the third has a different penalty time (3rd).
    contestants.add(contestantWithTime(10));
    contestants.add(contestantWithTime(10));
    contestants.add(contestantWithTime(20));

    contestService.rankingContestants(contestants);
    Assertions.assertEquals(1, contestants.get(0).getRank());
    Assertions.assertEquals(1, contestants.get(1).getRank());
    Assertions.assertEquals(3, contestants.get(2).getRank());
  }

  /** Test case: tests the rankingContestants(List) method. Test data: an empty list. Expected: the method returns normally without throwing an exception. */
  @Test
  public void testRankingContestantsWithEmptyList() {
    List<ContestContestant> contestants = new ArrayList<>();
    Assertions.assertDoesNotThrow(() -> contestService.rankingContestants(contestants));
  }

  /** Publishes a contest (the hidden Contest #3 is the only not-started one in the test data). */
  private void publishContest(long contestId) {
    Contest contest = contestService.getContest(contestId);
    contest.setStatus(PublicationStatus.PUBLISHED);
    contestMapper.updateContest(contest);
  }

  /** Constructs a User object with only the UID set, used as a contestant argument. */
  private User userWithUid(long uid) {
    User user = new User();
    user.setUid(uid);
    return user;
  }

  /** Constructs a ContestContestant object with only the penalty time set, used for ranking tests. */
  private ContestContestant contestantWithTime(long time) {
    ContestContestant contestant = new ContestContestant();
    contestant.setTime(time);
    return contestant;
  }

  /** The ContestService object under test. */
  @Autowired private ContestService contestService;

  /** The ContestMapper object, used to publish a contest in the test data. */
  @Autowired private ContestMapper contestMapper;

  /** The ContestContestantMapper object, used to check the raw leaderboard query results. */
  @Autowired private org.verwandlung.voj.web.mapper.ContestContestantMapper contestContestantMapper;

  /** The SubmissionService object, used to read back contest submissions. */
  @Autowired private SubmissionService submissionService;

  /** The ContestSubmissionMapper object, used to check the stored contest records. */
  @Autowired
  private org.verwandlung.voj.web.mapper.ContestSubmissionMapper contestSubmissionMapper;

  /** The DataSource of the test database, used to insert leaderboard fixtures. */
  @Autowired private javax.sql.DataSource dataSource;
}
