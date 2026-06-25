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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;
import org.verwandlung.voj.web.model.Contest;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * The test class for ContestMapper.
 *
 * @author Haozhe Xie
 */
@ExtendWith(SpringExtension.class)
@Transactional
@ContextConfiguration({"classpath:test-spring-context.xml"})
public class ContestMapperTest {
  /** Test case: tests the getNumberOfContests(String) method. Test data: Keyword = null. Expected: the total number of contests in the database (3). */
  @Test
  public void testGetNumberOfContests() {
    long numberOfContests = contestMapper.getNumberOfContests(null);
    Assertions.assertEquals(3, numberOfContests);
  }

  /**
   * Test case: tests the getNumberOfContests(String) method. Test data: Keyword = "#1". Expected: the number of matching contests in the database ("Contest #1").
   */
  @Test
  public void testGetNumberOfContestsUsingKeywordSharp1() {
    long numberOfContests = contestMapper.getNumberOfContests("#1");
    Assertions.assertEquals(1, numberOfContests);
  }

  /**
   * Test case: tests the getNumberOfContests(String) method. Test data: Keyword = "test #". Expected:
   * the number of matching contests in the database ("Contest #{1, 2, 3}").
   */
  @Test
  public void testGetNumberOfContestsUsingKeywordTestSharp() {
    long numberOfContests = contestMapper.getNumberOfContests("test #");
    Assertions.assertEquals(3, numberOfContests);
  }

  /** Test case: tests the getContests(String, long, int) method. Test data: get 2 contests starting from item 1. Expected: a List in the form [contest 2, contest 1]. */
  @Test
  public void testGetContestsFrom0WithLimit2() {
    List<Contest> contests = contestMapper.getContests("test #", 0, 2);
    Assertions.assertEquals(2, contests.size());

    Contest firstContest = contests.get(0);
    String contestName = firstContest.getContestName();
    Assertions.assertEquals("Contest #3", contestName);
  }

  /** Test case: tests the getContests(String, long, int) method. Test data: get 1 contest starting from item 2. Expected: a List in the form [contest 1]. */
  @Test
  public void testGetContestsFrom1WithLimit1() {
    List<Contest> contests = contestMapper.getContests(null, 1, 1);
    Assertions.assertEquals(1, contests.size());

    Contest contest = contests.get(0);
    String contestName = contest.getContestName();
    Assertions.assertEquals("Contest #2", contestName);
  }

  /** Test case: tests the getContests(String, long, int) method. Test data: get 1 contest starting from item 4. Expected: a List in the form []. */
  @Test
  public void testGetContestsFrom2WithLimit1() {
    List<Contest> contests = contestMapper.getContests(null, 3, 1);
    Assertions.assertEquals(0, contests.size());
  }

  /** Test case: tests the getContests(String, long, int) method. Test data: get 2 contests starting from item 1. Expected: a List in the form [contest 2, contest 1]. */
  @Test
  public void testGetContestsFrom0WithLimit2WithKeywordSharp2() {
    List<Contest> contests = contestMapper.getContests("#2", 0, 2);
    Assertions.assertEquals(1, contests.size());

    Contest firstContest = contests.get(0);
    String contestName = firstContest.getContestName();
    Assertions.assertEquals("Contest #2", contestName);
  }

  /** Test case: tests the getContest(long) method. Test data: get the 1st contest object. Expected: the 1st contest object. */
  @Test
  public void testGetContestsUsingIdExists() {
    Contest contest = contestMapper.getContest(1);
    Assertions.assertNotNull(contest);

    String contestName = contest.getContestName();
    Assertions.assertEquals("Contest #1", contestName);
  }

  /** Test case: tests the getContest(long) method. Test data: get the 0th contest object. Expected: a null reference. */
  @Test
  public void testGetContestsUsingIdNotExists() {
    Contest contest = contestMapper.getContest(0);
    Assertions.assertNull(contest);
  }

  /** Test case: tests the createContest(Contest) method. Test data: a Contest object with normal data values. Expected: the contest is created successfully. */
  @Test
  public void testCreateContestNormally() {
    Calendar calendar = Calendar.getInstance();
    calendar.set(2016, Calendar.MAY, 7, 18, 0, 0);
    Date startTime = calendar.getTime();

    calendar.set(2016, Calendar.MAY, 7, 20, 0, 0);
    Date endTime = calendar.getTime();

    Contest contest = new Contest("Contest", "Contest Notes", startTime, endTime, "OI", "[]");
    int numberOfRowsAffected = contestMapper.createContest(contest);
    Assertions.assertEquals(1, numberOfRowsAffected);
  }

  /**
   * Test case: tests the createContest(Contest) method. Test data: a string with a contest mode that is too long. Expected: a DataIntegrityViolationException is thrown.
   */
  @Test
  public void testCreateContestUsingTooLongContestMode() {
    Calendar calendar = Calendar.getInstance();
    calendar.set(2016, Calendar.MAY, 7, 18, 0, 0);
    Date startTime = calendar.getTime();

    calendar.set(2016, Calendar.MAY, 7, 20, 0, 0);
    Date endTime = calendar.getTime();

    Contest contest =
        new Contest("Contest", "Contest Notes", startTime, endTime, "OOOOI", "[1000]");
    Executable e =
        () -> {
          contestMapper.createContest(contest);
        };
    Assertions.assertThrows(org.springframework.dao.DataIntegrityViolationException.class, e);
  }

  /** Test case: tests the updateContest(Contest) method. Test data: a Contest object with normal data, updating the contest name. Expected: the contest object is updated successfully. */
  @Test
  public void testUpdateContestsNormally() {
    Contest contest = contestMapper.getContest(2);
    Assertions.assertNotNull(contest);

    contest.setContestName("New Contest Name");
    int numberOfRowsAffected = contestMapper.updateContest(contest);
    Assertions.assertEquals(1, numberOfRowsAffected);

    contest = contestMapper.getContest(2);
    String contestName = contest.getContestName();
    Assertions.assertEquals("New Contest Name", contestName);
  }

  /** Test case: tests the deleteContest(long) method. Test data: delete a contest object. Expected: the contest object is deleted successfully. */
  @Test
  public void testDeleteContestsExists() {
    Contest contest = contestMapper.getContest(3);
    Assertions.assertNotNull(contest);

    int numberOfRowsAffected = contestMapper.deleteContest(3);
    Assertions.assertEquals(1, numberOfRowsAffected);

    contest = contestMapper.getContest(3);
    Assertions.assertNull(contest);
  }

  /** Test case: tests the deleteContest(long) method. Test data: delete the 0th contest object. Expected: the program executes normally and no contest is deleted. */
  @Test
  public void testDeleteContestsNotExists() {
    Contest contest = contestMapper.getContest(0);
    Assertions.assertNull(contest);

    int numberOfRowsAffected = contestMapper.deleteContest(0);
    Assertions.assertEquals(0, numberOfRowsAffected);
  }

  /** The ContestMapper object under test. */
  @Autowired private ContestMapper contestMapper;
}
