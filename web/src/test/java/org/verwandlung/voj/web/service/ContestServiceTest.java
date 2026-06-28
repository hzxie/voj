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

import org.verwandlung.voj.web.model.Contest;
import org.verwandlung.voj.web.model.ContestContestant;
import org.verwandlung.voj.web.model.ContestSubmission;
import org.verwandlung.voj.web.model.Problem;
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
  /** Test case: tests the getContests(String, long, int) method. Test data: an empty keyword. Expected: all contests. */
  @Test
  public void testGetContests() {
    List<Contest> contests = contestService.getContests("", 0, 10);
    Assertions.assertEquals(3, contests.size());
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

  /** Test case: tests the attendContest(...) method. Test data: all conditions are met to attend the not-started Contest #3. Expected: attendance succeeds and the number of participants increases by one. */
  @Test
  public void testAttendContestSuccessfully() {
    Map<String, Boolean> result = contestService.attendContest(3, userWithUid(1000));
    Assertions.assertTrue(result.get("isSuccessful"));
    Assertions.assertEquals(1, contestService.getNumberOfContestantsOfContest(3));
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
    contestService.attendContest(3, userWithUid(1000));

    Map<String, Boolean> result = contestService.attendContest(3, userWithUid(1000));
    Assertions.assertTrue(result.get("isAttendedContest"));
    Assertions.assertFalse(result.get("isSuccessful"));
  }

  /** Test case: tests the getLeaderBoardForOi(long) method. Test data: the OI-mode Contest #1. Expected: the contestants and submission records, with rankings completed. */
  @Test
  @SuppressWarnings("unchecked")
  public void testGetLeaderBoardForOi() {
    Map<String, Object> leaderBoard = contestService.getLeaderBoardForOi(1);
    Assertions.assertTrue(leaderBoard.containsKey("contestants"));
    Assertions.assertTrue(leaderBoard.containsKey("submissions"));

    List<ContestContestant> contestants = (List<ContestContestant>) leaderBoard.get("contestants");
    Assertions.assertEquals(2, contestants.size());
    Assertions.assertEquals(1, contestants.get(0).getRank());
  }

  /** Test case: tests the getLeaderBoardForAcm(long) method. Test data: the ACM-mode Contest #2. Expected: the contestants and submission records, with rankings completed. */
  @Test
  @SuppressWarnings("unchecked")
  public void testGetLeaderBoardForAcm() {
    Map<String, Object> leaderBoard = contestService.getLeaderBoardForAcm(2);
    Assertions.assertTrue(leaderBoard.containsKey("contestants"));
    Assertions.assertTrue(leaderBoard.containsKey("submissions"));

    List<ContestContestant> contestants = (List<ContestContestant>) leaderBoard.get("contestants");
    Assertions.assertEquals(1, contestants.size());
    Assertions.assertEquals(1, contestants.get(0).getRank());
  }

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
}
