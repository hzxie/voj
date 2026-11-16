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
import org.verwandlung.voj.web.model.ContestContestant;
import org.verwandlung.voj.web.model.User;

import java.util.List;

/**
 * The test class for ContestContestantMapper.
 *
 * @author Haozhe Xie
 */
@ExtendWith(SpringExtension.class)
@Transactional
@ContextConfiguration({"classpath:test-spring-context.xml"})
public class ContestContestantMapperTest {
  /** Test case: tests the getNumberOfContestantsOfContest(long) method. Test data: an existing contest unique identifier. Expected: the number of participants of the corresponding contest. */
  @Test
  public void testGetNumberOfContestantsOfContestUsingExistingContest() {
    long numberOfContestants = contestContestantMapper.getNumberOfContestantsOfContest(1);
    Assertions.assertEquals(2, numberOfContestants);
  }

  /** Test case: tests the getNumberOfContestantsOfContest(long) method. Test data: a non-existing contest unique identifier. Expected: returns 0. */
  @Test
  public void testGetNumberOfContestantsOfContestUsingNotExistingContest() {
    long numberOfContestants = contestContestantMapper.getNumberOfContestantsOfContest(0);
    Assertions.assertEquals(0, numberOfContestants);
  }

  /**
   * Test case: tests the getContestantOfContest(long, long) method. Test data: ContestID = 1,
   * ContestantUid = 1000. Expected: the ContestContestant object of the user with UID = 1000 in the
   * 1st contest.
   */
  @Test
  public void testGetContestantOfContestExists() {
    ContestContestant cc = contestContestantMapper.getContestantOfContest(1, 1000);
    Assertions.assertNotNull(cc);

    String usernameOfFirstContestant = cc.getContestant().getUsername();
    Assertions.assertEquals("zjhzxhz", usernameOfFirstContestant);
  }

  /**
   * Test case: tests the getContestantsOfContestForOi(long, Date, long, int) method. Test data: ContestID =
   * 1, offset = 0, limit = 2. Expected: 2 contestants of the 1st contest starting from 0.
   */
  @Test
  public void testGetContestantsOfFirstContestForOiFrom0WithLimit2() {
    List<ContestContestant> contestants =
        contestContestantMapper.getContestantsOfContestForOi(1, null, 0, 2);
    Assertions.assertEquals(2, contestants.size());

    ContestContestant firstContestant = contestants.get(0);
    String usernameOfFirstContestant = firstContestant.getContestant().getUsername();
    Assertions.assertEquals("zjhzxhz", usernameOfFirstContestant);

    int score = firstContestant.getScore();
    Assertions.assertEquals(200, score);
    long time = firstContestant.getTime();
    Assertions.assertEquals(60, time);
  }

  /**
   * Test case: tests the getContestantsOfContestForOi(long, Date, long, int) method. Test data: ContestID =
   * 2, offset = 0, limit = 1. Expected: 1 contestant of the 2nd contest starting from 0.
   */
  @Test
  public void testGetContestantsOfSecondContestForOiFrom1WithLimit1() {
    List<ContestContestant> contestants =
        contestContestantMapper.getContestantsOfContestForOi(2, null, 0, 1);
    Assertions.assertEquals(1, contestants.size());

    ContestContestant firstContestant = contestants.get(0);
    String usernameOfFirstContestant = firstContestant.getContestant().getUsername();
    Assertions.assertEquals("zjhzxhz", usernameOfFirstContestant);
  }

  /**
   * Test case: tests the getContestantsOfContestForOi(long, Date, long, int) method. Test data: the
   * 2nd contest with three contestants, none of whom submitted anything (all tied at score 0 and
   * time 0). Expected: all three contestants, in UID order; tied rows must not be merged into one.
   */
  @Test
  public void testGetContestantsOfContestForOiKeepsTiedContestants() {
    addContestantsToSecondContest();
    List<ContestContestant> contestants =
        contestContestantMapper.getContestantsOfContestForOi(2, null, 0, 10);
    Assertions.assertEquals(List.of(1000L, 1001L, 1002L), uidsOf(contestants));
    Assertions.assertEquals("voj@judger", contestants.get(1).getContestant().getUsername());
    for (ContestContestant cc : contestants) {
      Assertions.assertEquals(0, cc.getScore());
      Assertions.assertEquals(0, cc.getTime());
    }
  }

  /**
   * Test case: tests the getContestantsOfContestForAcm(long, Date, long, int) method. Test data: the
   * 2nd contest with three contestants, none of whom submitted anything (all tied at 0 solved and 0
   * rejected). Expected: all three contestants, in UID order; tied rows must not be merged into one.
   */
  @Test
  public void testGetContestantsOfContestForAcmKeepsTiedContestants() {
    addContestantsToSecondContest();
    List<ContestContestant> contestants =
        contestContestantMapper.getContestantsOfContestForAcm(2, null, 0, 10);
    Assertions.assertEquals(List.of(1000L, 1001L, 1002L), uidsOf(contestants));
    Assertions.assertEquals("another-user", contestants.get(2).getContestant().getUsername());
  }

  /** Registers users 1001 and 1002 for the 2nd contest (user 1000 is registered already). */
  private void addContestantsToSecondContest() {
    Contest contest = contestMapper.getContest(2);
    contestContestantMapper.createContestContestant(
        new ContestContestant(contest, userMapper.getUserUsingUid(1001)));
    contestContestantMapper.createContestContestant(
        new ContestContestant(contest, userMapper.getUserUsingUid(1002)));
  }

  /** Collects the contestant UIDs of a leaderboard, in order. */
  private static List<Long> uidsOf(List<ContestContestant> contestants) {
    return contestants.stream().map(cc -> cc.getContestant().getUid()).toList();
  }

  /**
   * Test case: tests the createContestContestant(Contest, User) method. Test data: valid data, and no
   * identical record exists in the database. Expected: the participation record is created
   * successfully.
   */
  @Test
  public void testCreateContestContestantNormally() {
    Contest contest = contestMapper.getContest(2);
    User contestant = userMapper.getUserUsingUid(1002);

    ContestContestant cc = new ContestContestant(contest, contestant);
    int numberOfRowsAffected = contestContestantMapper.createContestContestant(cc);
    Assertions.assertEquals(1, numberOfRowsAffected);
  }

  /**
   * Test case: tests the createContestContestant(Contest, User) method. Test data: valid data, but an
   * identical record exists in the database. Expected: a DuplicateKeyException is thrown.
   */
  @Test
  public void testCreateContestContestantWithExistingRecord() {
    Contest contest = contestMapper.getContest(1);
    User contestant = userMapper.getUserUsingUid(1001);

    ContestContestant cc = new ContestContestant(contest, contestant);
    Executable e =
        () -> {
          contestContestantMapper.createContestContestant(cc);
        };
    Assertions.assertThrows(org.springframework.dao.DuplicateKeyException.class, e);
  }

  /**
   * Test case: tests the createContestContestant(Contest, User) method. Test data: a non-existing
   * contest ID. Expected: a DataIntegrityViolationException is thrown.
   */
  @Test
  public void testCreateContestContestantWithContestContestantNotExistingContest() {
    Contest contest = contestMapper.getContest(0);
    User contestant = userMapper.getUserUsingUid(1001);

    ContestContestant cc = new ContestContestant(contest, contestant);
    Executable e =
        () -> {
          contestContestantMapper.createContestContestant(cc);
        };
    Assertions.assertThrows(org.springframework.dao.DataIntegrityViolationException.class, e);
  }

  /**
   * Test case: tests the createContestContestant(Contest, User) method. Test data: a non-existing user
   * ID. Expected: a DataIntegrityViolationException is thrown.
   */
  @Test
  public void testCreateContestContestantWithNotExistingContestant() {
    Contest contest = contestMapper.getContest(1);
    User contestant = userMapper.getUserUsingUid(0);

    ContestContestant cc = new ContestContestant(contest, contestant);
    Executable e =
        () -> {
          contestContestantMapper.createContestContestant(cc);
        };
    Assertions.assertThrows(org.springframework.dao.DataIntegrityViolationException.class, e);
  }

  /**
   * Test case: tests the deleteContestContestant(long, long) method. Test data: ContestID = 2, UserID
   * = 1000. Expected: the ContestContestant is deleted successfully.
   */
  @Test
  public void testDeleteContestContestantExists() {
    ContestContestant cc = contestContestantMapper.getContestantOfContest(2, 1000);
    Assertions.assertNotNull(cc);

    int numberOfRowsAffected = contestContestantMapper.deleteContestContestant(2, 1000);
    Assertions.assertEquals(1, numberOfRowsAffected);
    cc = contestContestantMapper.getContestantOfContest(2, 1000);
    Assertions.assertNull(cc);
  }

  /** The ContestContestantMapper object under test. */
  @Autowired private ContestContestantMapper contestContestantMapper;

  /** The autowired ContestMapper object. Used to build test cases. */
  @Autowired private ContestMapper contestMapper;

  /** The autowired UserMapper object. Used to build test cases. */
  @Autowired private UserMapper userMapper;
}
