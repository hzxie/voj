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

import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

import org.verwandlung.voj.web.model.Contest;
import org.verwandlung.voj.web.model.ContestSubmission;
import org.verwandlung.voj.web.model.Submission;

/**
 * The test class for ContestSubmissionMapper.
 *
 * @author Haozhe Xie
 */
@ExtendWith(SpringExtension.class)
@Transactional
@ContextConfiguration({"classpath:test-spring-context.xml"})
public class ContestSubmissionMapperTest {
  /** Test case: tests the getSubmissionsOfContest(long) method. Test data: the unique identifier of a contest that has submission records. Expected: the list of all submission records of the contest. */
  @Test
  public void testGetSubmissionsOfContestExists() {
    List<ContestSubmission> submissions =
        contestSubmissionMapper.getSubmissionsOfContest(1);
    Assertions.assertEquals(3, submissions.size());

    ContestSubmission firstSubmission = submissions.get(0);
    Assertions.assertEquals(1, firstSubmission.getContest().getContestId());
    Assertions.assertNotNull(firstSubmission.getSubmission());
  }

  /** Test case: tests the getSubmissionsOfContest(long) method. Test data: the unique identifier of a contest that has no submission records. Expected: an empty list. */
  @Test
  public void testGetSubmissionsOfContestNotExists() {
    List<ContestSubmission> submissions =
        contestSubmissionMapper.getSubmissionsOfContest(0);
    Assertions.assertTrue(submissions.isEmpty());
  }

  /** Test case: tests the getAcceptedSubmissionsOfContest(long) method. Test data: the unique identifier of a contest that has AC submission records. Expected: only the submissions judged as AC. */
  @Test
  public void testGetAcceptedSubmissionsOfContestExists() {
    List<ContestSubmission> submissions =
        contestSubmissionMapper.getAcceptedSubmissionsOfContest(1);
    Assertions.assertEquals(2, submissions.size());

    for (ContestSubmission cs : submissions) {
      Assertions.assertEquals("AC", cs.getSubmission().getJudgeResult().getJudgeResultSlug());
    }
  }

  /** Test case: tests the getAcceptedSubmissionsOfContest(long) method. Test data: a non-existing contest unique identifier. Expected: an empty list. */
  @Test
  public void testGetAcceptedSubmissionsOfContestNotExists() {
    List<ContestSubmission> submissions =
        contestSubmissionMapper.getAcceptedSubmissionsOfContest(0);
    Assertions.assertTrue(submissions.isEmpty());
  }

  /** Test case: tests the getSubmissionOfContestOfContest(long, long) method. Test data: a contestant who has made submissions. Expected: only the submission records of this contestant in this contest. */
  @Test
  public void testGetSubmissionOfContestOfContestForContestant() {
    List<ContestSubmission> submissions =
        contestSubmissionMapper.getSubmissionOfContestOfContest(1, 1000);
    Assertions.assertEquals(2, submissions.size());

    for (ContestSubmission cs : submissions) {
      Assertions.assertEquals(1000, cs.getSubmission().getUser().getUid());
    }
  }

  /** Test case: tests the getSubmissionOfContestOfContest(long, long) method. Test data: another contestant. Expected: only the submission records of this contestant. */
  @Test
  public void testGetSubmissionOfContestOfContestForAnotherContestant() {
    List<ContestSubmission> submissions =
        contestSubmissionMapper.getSubmissionOfContestOfContest(1, 1001);
    Assertions.assertEquals(1, submissions.size());
    Assertions.assertEquals(1001, submissions.get(0).getSubmission().getUser().getUid());
  }

  /** Test case: tests the getSubmissionOfContestOfContest(long, long) method. Test data: a contestant who has not made any submission. Expected: an empty list. */
  @Test
  public void testGetSubmissionOfContestOfContestForNonContestant() {
    List<ContestSubmission> submissions =
        contestSubmissionMapper.getSubmissionOfContestOfContest(1, 9999);
    Assertions.assertTrue(submissions.isEmpty());
  }

  /** Test case: tests the getSubmissionOfContestOfContestProblem(long, long, long) method. Test data: the contestant has submitted this problem. Expected: the contestant's submission records for this problem. */
  @Test
  public void testGetSubmissionOfContestOfContestProblemExists() {
    List<ContestSubmission> submissions =
        contestSubmissionMapper.getSubmissionOfContestOfContestProblem(1, 1000, 1000);
    Assertions.assertEquals(1, submissions.size());

    Submission submission = submissions.get(0).getSubmission();
    Assertions.assertEquals(1000, submission.getProblem().getProblemId());
    Assertions.assertEquals(1000, submission.getUser().getUid());
  }

  /** Test case: tests the getSubmissionOfContestOfContestProblem(long, long, long) method. Test data: the contestant has not submitted this problem. Expected: an empty list. */
  @Test
  public void testGetSubmissionOfContestOfContestProblemNotExists() {
    List<ContestSubmission> submissions =
        contestSubmissionMapper.getSubmissionOfContestOfContestProblem(1, 1001, 1001);
    Assertions.assertTrue(submissions.isEmpty());
  }

  /** Test case: tests the createContestSubmission(ContestSubmission) method. Test data: a valid contest submission record. Expected: the data creation operation completes successfully. */
  @Test
  public void testCreateContestSubmission() {
    Contest contest = new Contest();
    contest.setContestId(1);
    Submission submission = new Submission();
    submission.setSubmissionId(1001);

    int numberOfRowsAffected =
        contestSubmissionMapper.createContestSubmission(new ContestSubmission(contest, submission));
    Assertions.assertEquals(1, numberOfRowsAffected);
    Assertions.assertEquals(4, contestSubmissionMapper.getSubmissionsOfContest(1).size());
  }

  /** Test case: tests the deleteContestSubmission(long, long) method. Test data: an existing contest submission record. Expected: the data deletion operation completes successfully. */
  @Test
  public void testDeleteContestSubmissionExists() {
    int numberOfRowsAffected = contestSubmissionMapper.deleteContestSubmission(1, 1000);
    Assertions.assertEquals(1, numberOfRowsAffected);
    Assertions.assertEquals(2, contestSubmissionMapper.getSubmissionsOfContest(1).size());
  }

  /** Test case: tests the deleteContestSubmission(long, long) method. Test data: a non-existing contest submission record. Expected: the method executes normally without affecting the data in the table. */
  @Test
  public void testDeleteContestSubmissionNotExists() {
    int numberOfRowsAffected = contestSubmissionMapper.deleteContestSubmission(1, 9999);
    Assertions.assertEquals(0, numberOfRowsAffected);
  }

  /** The ContestSubmissionMapper object under test. */
  @Autowired private ContestSubmissionMapper contestSubmissionMapper;
}
