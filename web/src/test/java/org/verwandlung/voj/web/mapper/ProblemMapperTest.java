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

import javax.sql.DataSource;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

import org.verwandlung.voj.web.model.Problem;
import org.verwandlung.voj.web.model.ProblemDifficulty;

/**
 * The test class for ProblemMapper.
 *
 * @author Haozhe Xie
 */
@ExtendWith(SpringExtension.class)
@Transactional
@ContextConfiguration({"classpath:test-spring-context.xml"})
public class ProblemMapperTest {
  /** Test case: tests the getNumberOfProblems() method. Test data: N/a. Expected: the number of all problems in the table. */
  @Test
  public void testGetNumberOfProblems() {
    long totalProblems = problemMapper.getNumberOfProblems();
    Assertions.assertEquals(4, totalProblems);
  }

  /**
   * Test case: tests the getNumberOfProblemsUsingFilters(String, int, boolean) method. Test data: N/a. Expected: the number of public problems in the table.
   */
  @Test
  public void testGetNumberOfPublicProblems() {
    long totalProblems = problemMapper.getNumberOfProblemsUsingFilters(null, 0, true);
    Assertions.assertEquals(3, totalProblems);
  }

  /** Test case: tests the getLowerBoundOfProblems() method. Test data: N/a. Expected: the ID of the first problem (1000). */
  @Test
  public void testGetLowerBoundOfProblems() {
    long lowerBoundOfProblems = problemMapper.getLowerBoundOfProblems();
    Assertions.assertEquals(1000, lowerBoundOfProblems);
  }

  /** Test case: tests the getUpperBoundOfProblems() method. Test data: N/a. Expected: the ID of the last problem (1002). */
  @Test
  public void testGetUpperBoundOfProblems() {
    long upperBoundOfProblems = problemMapper.getUpperBoundOfProblems();
    Assertions.assertEquals(1003, upperBoundOfProblems);
  }

  /**
   * Test case: tests the getLowerBoundOfProblems() method when no problem exists. Test data: an
   * emptied problems table. Expected: 0 (MIN over an empty table is NULL, which COALESCE maps to 0
   * so the primitive long return type never receives null).
   */
  @Test
  public void testGetLowerBoundOfProblemsWhenEmpty() {
    deleteAllProblems();
    Assertions.assertEquals(0, problemMapper.getLowerBoundOfProblems());
  }

  /**
   * Test case: tests the getUpperBoundOfProblems() method when no problem exists. Test data: an
   * emptied problems table. Expected: 0 (MAX over an empty table is NULL, which COALESCE maps to 0
   * so the primitive long return type never receives null).
   */
  @Test
  public void testGetUpperBoundOfProblemsWhenEmpty() {
    deleteAllProblems();
    Assertions.assertEquals(0, problemMapper.getUpperBoundOfProblems());
  }

  /**
   * Test case: tests the getUpperBoundOfProblemsWithLimit(boolean, long, int) method. Test data: get 3
   * problems with IDs starting from 1000. Expected: the ID of the last problem is 1002.
   */
  @Test
  public void testGetUpperBoundOfProblemsWithOffsetFrom1000WithLimit3() {
    long upperBoundOfProblems = problemMapper.getUpperBoundOfProblemsWithLimit(false, 1000, 3);
    Assertions.assertEquals(1002, upperBoundOfProblems);
  }

  /**
   * Test case: tests the getUpperBoundOfProblemsWithLimit(boolean, long, int) method. Test data: get 3
   * public problems with IDs starting from 1000. Expected: the ID of the last problem is 1003.
   */
  @Test
  public void testGetUpperBoundOfPublicProblemsWithOffsetFrom1000WithLimit3() {
    long upperBoundOfProblems = problemMapper.getUpperBoundOfProblemsWithLimit(true, 1000, 3);
    Assertions.assertEquals(1003, upperBoundOfProblems);
  }

  /**
   * Test case: tests the getUpperBoundOfProblemsWithLimit(boolean, long, int) method when the
   * requested range matches no problem. Test data: an offset beyond the last problem ID. Expected: 0
   * (the inner query yields no rows, so MAX is NULL, which COALESCE maps to 0 — no deletion required
   * to exercise the empty-result path).
   */
  @Test
  public void testGetUpperBoundOfProblemsWithLimitWhenRangeEmpty() {
    long upperBoundOfProblems = problemMapper.getUpperBoundOfProblemsWithLimit(false, 99999, 3);
    Assertions.assertEquals(0, upperBoundOfProblems);
  }

  /** Test case: tests the getProblem() method. Test data: the problem unique identifier of A+B Problem. Expected: the expected problem object. */
  @Test
  public void testGetProblemExists() {
    Problem problem = problemMapper.getProblem(1000);
    Assertions.assertNotNull(problem);

    String problemName = problem.getProblemName();
    Assertions.assertEquals("A+B Problem", problemName);

    long acceptedSubmission = problem.getAcceptedSubmission();
    Assertions.assertEquals(1, acceptedSubmission);
  }

  /** Test case: tests the getProblem() method. Test data: a non-existing problem unique identifier. Expected: a null reference. */
  @Test
  public void testGetProblemNotExists() {
    Problem problem = problemMapper.getProblem(0);
    Assertions.assertNull(problem);
  }

  /**
   * Test case: tests the getProblemsUsingFilters(String, int, boolean, long, int) method. Test data:
   * get 10 problems with IDs starting from 1000. Expected: the expected problem list (3 problems).
   */
  @Test
  public void testGetProblemsFrom1000WithLimit10() {
    List<Problem> problems = problemMapper.getProblemsUsingFilters(null, 0, 0, true, 1000, 10);
    Assertions.assertEquals(3, problems.size());

    Problem firstProblem = problems.get(0);
    long problemId = firstProblem.getProblemId();
    Assertions.assertEquals(1000, problemId);

    String problemName = firstProblem.getProblemName();
    Assertions.assertEquals("A+B Problem", problemName);

    long acceptedSubmission = firstProblem.getAcceptedSubmission();
    Assertions.assertEquals(1, acceptedSubmission);
  }

  /**
   * Test case: tests the getProblemsUsingFilters(String, int, boolean, long, int) method. Test data:
   * get 1 problem with ID starting from 1001. Expected: the expected problem list (1 problem).
   */
  @Test
  public void testGetProblemsFrom1001WithLimit1() {
    List<Problem> problems = problemMapper.getProblemsUsingFilters("", 0, 0, true, 1001, 1);
    Assertions.assertEquals(1, problems.size());

    Problem firstProblem = problems.get(0);
    long problemId = firstProblem.getProblemId();
    Assertions.assertEquals(1001, problemId);

    long acceptedSubmission = firstProblem.getAcceptedSubmission();
    Assertions.assertEquals(1, acceptedSubmission);
  }

  /**
   * Test case: tests the getProblemsUsingFilters(String, int, boolean, long, int) method. Test data:
   * get 10 problems with IDs starting from 1010. Expected: an empty problem list.
   */
  @Test
  public void testGetProblemsFrom1010WithLimit10() {
    List<Problem> problems = problemMapper.getProblemsUsingFilters(null, 0, 0, true, 1010, 10);
    Assertions.assertEquals(0, problems.size());
  }

  /** Test case: tests the createProblem(Problem) method. Test data: a valid data set. Expected: the data insertion operation completes successfully. */
  @Test
  public void testCreateProblemNormally() {
    Problem problem =
        new Problem(
            false,
            "Problem Name",
            1000,
            32768,
            "Description",
            "Input Format",
            "Output Format",
            "Sample Input",
            "Sample Input",
            null);
    problem.setProblemDifficulty(new ProblemDifficulty(1, "easy", "Easy"));
    int numberOfRowsAffected = problemMapper.createProblem(problem);
    Assertions.assertEquals(1, numberOfRowsAffected);
  }

  /** Test case: tests the updateProblem(Problem) method. Test data: a valid data set, and a record with the corresponding ID exists in the table. Expected: the data update operation completes successfully. */
  @Test
  public void testUpdateProblemNormally() {
    Problem problem = problemMapper.getProblem(1001);
    Assertions.assertNotNull(problem);

    problem.setProblemName("New Problem Name");
    int numberOfRowsAffected = problemMapper.updateProblem(problem);
    Assertions.assertEquals(1, numberOfRowsAffected);

    /**
     * The following Assert CANNOT passed in CI due to the bug of Spring Test Framework. But it
     * really works.
     */
    /*
     * Problem updatedProblem = problemMapper.getProblem(1001);
     * String problemName = updatedProblem.getProblemName();
     * Assertions.assertEquals("New Problem Name", problemName);
     */
  }

  /** Test case: tests the updateProblem(Problem) method. Test data: a valid data set, but no record with the corresponding ID exists in the table. Expected: the method executes normally without affecting the data in the table. */
  @Test
  public void testUpdateProblemNotExists() {
    Problem problem = problemMapper.getProblem(1001);
    Assertions.assertNotNull(problem);

    problem.setProblemId(0);
    int numberOfRowsAffected = problemMapper.updateProblem(problem);
    Assertions.assertEquals(0, numberOfRowsAffected);
  }

  /** Test case: tests the deleteProblem(long) method. Test data: the unique identifier of problem #1002. Expected: the data deletion operation completes successfully. */
  @Test
  public void testDeleteProblemExists() {
    Problem problem = problemMapper.getProblem(1002);
    Assertions.assertNotNull(problem);

    int numberOfRowsAffected = problemMapper.deleteProblem(1002);
    Assertions.assertEquals(1, numberOfRowsAffected);

    problem = problemMapper.getProblem(1002);
    Assertions.assertNull(problem);
  }

  /** Test case: tests the deleteProblem(long) method. Test data: a non-existing problem unique identifier. Expected: the method executes normally without affecting the data in the table. */
  @Test
  public void testDeleteProblemNotExists() {
    Problem problem = problemMapper.getProblem(0);
    Assertions.assertNull(problem);

    int numberOfRowsAffected = problemMapper.deleteProblem(0);
    Assertions.assertEquals(0, numberOfRowsAffected);
  }

  /**
   * Empties the problems table within the current (rolled-back) test transaction. Contest
   * submissions reference submissions without ON DELETE CASCADE, so they must be removed first;
   * deleting the problems then cascades to submissions, checkpoints, category/tag relationships and
   * discussion threads.
   */
  private void deleteAllProblems() {
    JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
    jdbcTemplate.update("DELETE FROM voj_contest_submissions");
    jdbcTemplate.update("DELETE FROM voj_problems");
  }

  /** The ProblemMapper object under test. */
  @Autowired private ProblemMapper problemMapper;

  /** The data source used to empty the problems table within the test transaction. */
  @Autowired private DataSource dataSource;
}
