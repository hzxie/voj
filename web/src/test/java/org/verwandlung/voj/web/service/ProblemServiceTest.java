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

import javax.sql.DataSource;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

import org.verwandlung.voj.web.mapper.ProblemMapper;
import org.verwandlung.voj.web.model.Checkpoint;
import org.verwandlung.voj.web.model.Problem;
import org.verwandlung.voj.web.model.ProblemDifficulty;
import org.verwandlung.voj.web.model.ProblemCategory;
import org.verwandlung.voj.web.model.ProblemTag;

/**
 * The test class for ProblemService.
 *
 * <p>Seed data: 4 problems (1000, 1001, 1003 public, 1002 private); 2 problem categories
 * (uncategorized default/non-editable, dynamic-programming); 2 tags; problems 1000 and 1001 each
 * have 10 checkpoints (20 in total).
 *
 * @author Haozhe Xie
 */
@ExtendWith(SpringExtension.class)
@Transactional
@ContextConfiguration({"classpath:test-spring-context.xml"})
public class ProblemServiceTest {
  private static final String TEST_CASES = "[{\"input\": \"1 2\", \"output\": \"3\"}]";
  private static final String CATEGORIES = "[\"uncategorized\"]";
  private static final String TAGS = "[\"Greedy\"]";

  /** Test case: tests the getFirstIndexOfProblems() method. Test data: N/a. Expected: the lower bound of the problem IDs. */
  @Test
  public void testGetFirstIndexOfProblems() {
    Assertions.assertEquals(1000, problemService.getFirstIndexOfProblems());
  }

  /**
   * Test case: tests the getFirstIndexOfProblems() method when no problem exists. Test data: an
   * emptied problems table. Expected: 0, rather than a BindingException. Regression test for the
   * all-problems administration view, which crashed with "attempted to return null from a method
   * with a primitive return type (long)" when the problems table was empty.
   */
  @Test
  public void testGetFirstIndexOfProblemsWhenEmpty() {
    deleteAllProblems();
    Assertions.assertEquals(0, problemService.getFirstIndexOfProblems());
  }

  /** Test case: tests the getNumberOfProblems() method. Test data: N/a. Expected: the number of all problems. */
  @Test
  public void testGetNumberOfProblems() {
    Assertions.assertEquals(4, problemService.getNumberOfProblems());
  }

  /** Test case: tests the getProblem(long) method. Test data: an existing / non-existing problem identifier. Expected: the corresponding problem or a null reference. */
  @Test
  public void testGetProblem() {
    Assertions.assertNotNull(problemService.getProblem(1000));
    Assertions.assertNull(problemService.getProblem(0));
  }

  /** Test case: tests the getProblemsUsingFilters(...) method. Test data: without filter conditions. Expected: all problems. */
  @Test
  public void testGetProblemsUsingFiltersWithoutFilters() {
    List<Problem> problems = problemService.getProblemsUsingFilters(0, "", "", "", false, 100);
    Assertions.assertEquals(4, problems.size());
  }

  /** Test case: tests the getProblemsUsingFilters(...) method. Test data: public problems only. Expected: the public problems. */
  @Test
  public void testGetProblemsUsingFiltersPublicOnly() {
    List<Problem> problems = problemService.getProblemsUsingFilters(0, "", "", "", true, 100);
    Assertions.assertEquals(3, problems.size());
  }

  /** Test case: tests the getProblemsUsingFilters(...) method. Test data: filtering by category. Expected: only the problems under that category. */
  @Test
  public void testGetProblemsUsingFiltersByCategory() {
    List<Problem> problems =
        problemService.getProblemsUsingFilters(0, "", "dynamic-programming", "", false, 100);
    // Only problem 1000 belongs to the dynamic-programming category.
    Assertions.assertEquals(1, problems.size());
    Assertions.assertEquals(1000, problems.get(0).getProblemId());
  }

  /** Test case: tests the getNumberOfProblemsUsingFilters(...) method. Test data: all / public only. Expected: the corresponding count. */
  @Test
  public void testGetNumberOfProblemsUsingFilters() {
    Assertions.assertEquals(4, problemService.getNumberOfProblemsUsingFilters("", "", false));
    Assertions.assertEquals(3, problemService.getNumberOfProblemsUsingFilters("", "", true));
  }

  /** Test case: tests the getProblemCategories() method. Test data: N/a. Expected: all problem categories. */
  @Test
  public void testGetProblemCategories() {
    Assertions.assertEquals(2, problemService.getProblemCategories().size());
  }

  /** Test case: tests the getProblemCategoriesWithHierarchy() method. Test data: N/a. Expected: 2 top-level categories, each with empty subcategories. */
  @Test
  public void testGetProblemCategoriesWithHierarchy() {
    Map<ProblemCategory, List<ProblemCategory>> hierarchy =
        problemService.getProblemCategoriesWithHierarchy();
    Assertions.assertEquals(2, hierarchy.size());
    for (List<ProblemCategory> subCategories : hierarchy.values()) {
      Assertions.assertTrue(subCategories.isEmpty());
    }
  }

  /** Test case: tests the getProblemCategoriesUsingProblemId(long) method. Test data: problem 1000. Expected: all categories of the problem. */
  @Test
  public void testGetProblemCategoriesUsingProblemId() {
    List<ProblemCategory> categories = problemService.getProblemCategoriesUsingProblemId(1000);
    Assertions.assertEquals(2, categories.size());
  }

  /** Test case: tests the getProblemCategoriesOfProblems(long, long) method. Test data: the problem range [1000, 1003]. Expected: the categories indexed by problem. */
  @Test
  public void testGetProblemCategoriesOfProblems() {
    Map<Long, List<ProblemCategory>> categories =
        problemService.getProblemCategoriesOfProblems(1000, 1003);
    Assertions.assertEquals(2, categories.get(1000L).size());
  }

  /** Test case: tests the getProblemTagsUsingProblemId(long) method. Test data: problem 1001. Expected: all tags of the problem. */
  @Test
  public void testGetProblemTagsUsingProblemId() {
    List<ProblemTag> tags = problemService.getProblemTagsUsingProblemId(1001);
    Assertions.assertEquals(2, tags.size());
  }

  /** Test case: tests the getProblemTagsOfProblems(long, long) method. Test data: the problem range [1000, 1003]. Expected: the tags indexed by problem. */
  @Test
  public void testGetProblemTagsOfProblems() {
    Map<Long, List<ProblemTag>> tags = problemService.getProblemTagsOfProblems(1000, 1003);
    Assertions.assertEquals(2, tags.get(1001L).size());
  }

  /** Test case: tests the getNumberOfCheckpoints() method. Test data: N/a. Expected: the number of all checkpoints. */
  @Test
  public void testGetNumberOfCheckpoints() {
    Assertions.assertEquals(20, problemService.getNumberOfCheckpoints());
  }

  /** Test case: tests the getCheckpointsUsingProblemId(long) method. Test data: problem 1000 and problem 1002. Expected: the corresponding checkpoint lists. */
  @Test
  public void testGetCheckpointsUsingProblemId() {
    List<Checkpoint> checkpoints = problemService.getCheckpointsUsingProblemId(1000);
    Assertions.assertEquals(10, checkpoints.size());
    Assertions.assertTrue(problemService.getCheckpointsUsingProblemId(1002).isEmpty());
  }

  /** Test case: tests the createProblem(...) method. Test data: valid problem data. Expected: creation succeeds and the problem identifier is returned. */
  @Test
  public void testCreateProblemSuccessfully() {
    Map<String, Object> result =
        problemService.createProblem(
            "A New Problem", 1000, 65536, "Description", "Hint", "Input Format",
            "Output Format", "1 2", "3", TEST_CASES, CATEGORIES, TAGS, true, true, "easy");
    Assertions.assertTrue((Boolean) result.get("isSuccessful"));
    Assertions.assertTrue(result.containsKey("problemId"));
    Assertions.assertEquals(5, problemService.getNumberOfProblems());
  }

  /** Test case: tests the createProblem(...) method. Test data: the problem name is empty. Expected: creation fails. */
  @Test
  public void testCreateProblemWithEmptyName() {
    Map<String, Object> result =
        problemService.createProblem(
            "", 1000, 65536, "Description", "Hint", "Input Format", "Output Format",
            "1 2", "3", TEST_CASES, CATEGORIES, TAGS, true, true, "easy");
    Assertions.assertTrue((Boolean) result.get("isProblemNameEmpty"));
    Assertions.assertFalse((Boolean) result.get("isSuccessful"));
  }

  /** Test case: tests the createProblem(...) method. Test data: an illegal time limit. Expected: creation fails. */
  @Test
  public void testCreateProblemWithIllegalTimeLimit() {
    Map<String, Object> result =
        problemService.createProblem(
            "A New Problem", 0, 65536, "Description", "Hint", "Input Format", "Output Format",
            "1 2", "3", TEST_CASES, CATEGORIES, TAGS, true, true, "easy");
    Assertions.assertFalse((Boolean) result.get("isTimeLimitLegal"));
    Assertions.assertFalse((Boolean) result.get("isSuccessful"));
  }

  /** Test case: tests the editProblem(...) method. Test data: an existing problem. Expected: editing succeeds. */
  @Test
  public void testEditProblemSuccessfully() {
    Map<String, Boolean> result =
        problemService.editProblem(
            1000, "A+B Problem (Edited)", 2000, 131072, "New Description", "Hint",
            "Input Format", "Output Format", "1 2", "3", TEST_CASES, CATEGORIES, TAGS, true, true, "easy");
    Assertions.assertTrue(result.get("isSuccessful"));
    Assertions.assertEquals("A+B Problem (Edited)", problemService.getProblem(1000).getProblemName());
  }

  /** Test case: tests the editProblem(...) method. Test data: a non-existing problem. Expected: editing fails. */
  @Test
  public void testEditProblemNotExists() {
    Map<String, Boolean> result =
        problemService.editProblem(
            99999, "Nonexistent", 1000, 65536, "Description", "Hint", "Input Format",
            "Output Format", "1 2", "3", TEST_CASES, CATEGORIES, TAGS, true, true, "easy");
    Assertions.assertFalse(result.get("isProblemExists"));
    Assertions.assertFalse(result.get("isSuccessful"));
  }

  /** Test case: tests the deleteProblem(long) method. Test data: a problem with no foreign-key references. Expected: the problem is deleted. */
  @Test
  public void testDeleteProblem() {
    // Insert a problem with no associated data directly through the Mapper, to avoid foreign-key
    // constraints.
    Problem problem =
        new Problem(
            true, "Throwaway Problem", 1000, 65536, "Description", "Input Format",
            "Output Format", "1 2", "3", "Hint");
    problem.setProblemDifficulty(new ProblemDifficulty(1, "easy", "Easy"));
    problemMapper.createProblem(problem);
    long problemId = problem.getProblemId();

    problemService.deleteProblem(problemId);
    Assertions.assertNull(problemService.getProblem(problemId));
  }

  /** Test case: tests the createProblemCategory(...) method. Test data: valid category data. Expected: creation succeeds and the category identifier is returned. */
  @Test
  public void testCreateProblemCategorySuccessfully() {
    Map<String, Object> result =
        problemService.createProblemCategory("graph-theory", "Graph Theory", "");
    Assertions.assertTrue((Boolean) result.get("isSuccessful"));
    Assertions.assertTrue(result.containsKey("problemCategoryId"));
    Assertions.assertEquals(3, problemService.getProblemCategories().size());
  }

  /** Test case: tests the createProblemCategory(...) method. Test data: the category slug already exists. Expected: creation fails. */
  @Test
  public void testCreateProblemCategoryWithDuplicatedSlug() {
    Map<String, Object> result =
        problemService.createProblemCategory("uncategorized", "Duplicated", "");
    Assertions.assertTrue((Boolean) result.get("isProblemCategorySlugExists"));
    Assertions.assertFalse((Boolean) result.get("isSuccessful"));
  }

  /** Test case: tests the createProblemCategory(...) method. Test data: the category slug is empty. Expected: creation fails. */
  @Test
  public void testCreateProblemCategoryWithEmptySlug() {
    Map<String, Object> result = problemService.createProblemCategory("", "Empty Slug", "");
    Assertions.assertTrue((Boolean) result.get("isProblemCategorySlugEmpty"));
    Assertions.assertFalse((Boolean) result.get("isSuccessful"));
  }

  /** Test case: tests the editProblemCategory(...) method. Test data: an editable category. Expected: editing succeeds. */
  @Test
  public void testEditProblemCategorySuccessfully() {
    Map<String, Boolean> result =
        problemService.editProblemCategory(2, "dynamic-programming", "DP (Updated)", "");
    Assertions.assertTrue(result.get("isSuccessful"));
  }

  /** Test case: tests the editProblemCategory(...) method. Test data: the default category (ID 1, non-editable). Expected: editing fails. */
  @Test
  public void testEditProblemCategoryNotEditable() {
    Map<String, Boolean> result =
        problemService.editProblemCategory(1, "uncategorized", "Uncategorized", "");
    Assertions.assertFalse(result.get("isProblemCategoryEditable"));
    Assertions.assertFalse(result.get("isSuccessful"));
  }

  /** Test case: tests the editProblemCategory(...) method. Test data: a non-existing category. Expected: editing fails. */
  @Test
  public void testEditProblemCategoryNotExists() {
    Map<String, Boolean> result = problemService.editProblemCategory(9999, "slug", "Name", "");
    Assertions.assertFalse(result.get("isProblemCategoryExists"));
    Assertions.assertFalse(result.get("isSuccessful"));
  }

  /** Test case: tests the deleteProblemCategory(int) method. Test data: the default category (ID 1). Expected: deletion fails. */
  @Test
  public void testDeleteProblemCategoryNotEditable() {
    Assertions.assertFalse(problemService.deleteProblemCategory(1));
  }

  /** Test case: tests the deleteProblemCategory(int) method. Test data: an editable category with no associations. Expected: deletion succeeds. */
  @Test
  public void testDeleteProblemCategorySuccessfully() {
    @SuppressWarnings("unchecked")
    Map<String, Object> created =
        problemService.createProblemCategory("graph-theory", "Graph Theory", "");
    int problemCategoryId = (int) (long) (Long) created.get("problemCategoryId");
    Assertions.assertTrue(problemService.deleteProblemCategory(problemCategoryId));
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

  /** The ProblemService object under test. */
  @Autowired private ProblemService problemService;

  /** The Mapper used to construct unassociated problem data within the test transaction. */
  @Autowired private ProblemMapper problemMapper;

  /** The data source used to empty the problems table within the test transaction. */
  @Autowired private DataSource dataSource;
}
