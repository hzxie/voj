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
import org.junit.jupiter.api.function.Executable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

import org.verwandlung.voj.web.model.ProblemCategory;
import org.verwandlung.voj.web.model.ProblemCategoryRelationship;

/**
 * The test class for ProblemCategoryMapper.
 *
 * @author Haozhe Xie
 */
@ExtendWith(SpringExtension.class)
@Transactional
@ContextConfiguration({"classpath:test-spring-context.xml"})
public class ProblemCategoryMapperTest {
  /** Test case: tests the getProblemCategories() method. Test data: N/a. Expected: the list of all problem categories, with the default category first. */
  @Test
  public void testGetProblemCategories() {
    List<ProblemCategory> problemCategories = problemCategoryMapper.getProblemCategories();
    Assertions.assertNotNull(problemCategories);
    Assertions.assertEquals(2, problemCategories.size());

    ProblemCategory firstProblemCategory = problemCategories.get(0);
    String problemCategoryName = firstProblemCategory.getProblemCategoryName();
    Assertions.assertEquals("Uncategorized", problemCategoryName);
  }

  /** Test case: tests the getProblemCategoryUsingCategoryId(int) method. Test data: the unique identifier of the default category. Expected: the problem category object of the default category. */
  @Test
  public void testGetProblemCategoryUsingIdExists() {
    ProblemCategory problemCategory = problemCategoryMapper.getProblemCategoryUsingCategoryId(1);
    Assertions.assertNotNull(problemCategory);

    String problemCategoryName = problemCategory.getProblemCategoryName();
    Assertions.assertEquals("Uncategorized", problemCategoryName);
  }

  /** Test case: tests the getProblemCategoryUsingCategoryId(int) method. Test data: a non-existing unique identifier. Expected: a null reference. */
  @Test
  public void testGetProblemCategoryUsingIdNotExists() {
    ProblemCategory problemCategory = problemCategoryMapper.getProblemCategoryUsingCategoryId(0);
    Assertions.assertNull(problemCategory);
  }

  /** Test case: tests the getProblemCategoriesUsingProblemId(long) method. Test data: the unique identifier of an existing problem. Expected: the list of all problem categories of the problem. */
  @Test
  public void testGetProblemCategoriesUsingProblemIdExists() {
    List<ProblemCategory> problemCategories =
        problemCategoryMapper.getProblemCategoriesUsingProblemId(1000);
    Assertions.assertEquals(2, problemCategories.size());

    ProblemCategory problemCategory = problemCategories.get(0);
    String problemCategorySlug = problemCategory.getProblemCategorySlug();
    Assertions.assertEquals("uncategorized", problemCategorySlug);
  }

  /** Test case: tests the getProblemCategoriesUsingProblemId(long) method. Test data: a non-existing problem unique identifier. Expected: an empty problem category list. */
  @Test
  public void testGetProblemCategoriesUsingProblemIdNotExists() {
    List<ProblemCategory> problemCategories =
        problemCategoryMapper.getProblemCategoriesUsingProblemId(0);
    Assertions.assertEquals(0, problemCategories.size());
  }

  /**
   * Test case: tests the getProblemCategoriesOfProblems() method. Test data: a problem ID range from 1000 to 1002. Expected: all the problem category information (4 records in total).
   */
  @Test
  public void testGetProblemCategoriesOfProblemsFrom1000WithLimit3() {
    List<ProblemCategoryRelationship> problemCategoryRelationships =
        problemCategoryMapper.getProblemCategoriesOfProblems(1000, 1002);
    Assertions.assertEquals(4, problemCategoryRelationships.size());

    ProblemCategoryRelationship pcr = problemCategoryRelationships.get(0);
    Assertions.assertEquals(1000, pcr.getProblemId());
    Assertions.assertEquals(1, pcr.getProblemCategoryId());
  }

  /** Test case: tests the getProblemCategoriesOfProblems() method. Test data: a problem ID range from 999 to 1000. Expected: all the problem category information (2 records in total). */
  @Test
  public void testGetProblemCategoriesOfProblemsFrom999WithLimit2() {
    List<ProblemCategoryRelationship> problemCategoryRelationships =
        problemCategoryMapper.getProblemCategoriesOfProblems(999, 1000);
    Assertions.assertEquals(2, problemCategoryRelationships.size());

    ProblemCategoryRelationship pcr = problemCategoryRelationships.get(0);
    Assertions.assertEquals(1000, pcr.getProblemId());
    Assertions.assertEquals(1, pcr.getProblemCategoryId());
  }

  /** Test case: tests the getProblemCategoriesOfProblems() method. Test data: a problem ID range from 999 to 999. Expected: all the problem category information (0 records in total). */
  @Test
  public void testGetProblemCategoriesOfProblemsFrom999WithLimit1() {
    List<ProblemCategoryRelationship> problemCategoryRelationships =
        problemCategoryMapper.getProblemCategoriesOfProblems(999, 999);
    Assertions.assertEquals(0, problemCategoryRelationships.size());
  }

  /** Test case: tests the getProblemCategoryUsingCategorySlug(String) method. Test data: the slug of the default category. Expected: the problem category object of the default category. */
  @Test
  public void testGetProblemCategoryUsingSlugExists() {
    ProblemCategory problemCategory =
        problemCategoryMapper.getProblemCategoryUsingCategorySlug("uncategorized");
    Assertions.assertNotNull(problemCategory);

    String problemCategoryName = problemCategory.getProblemCategoryName();
    Assertions.assertEquals("Uncategorized", problemCategoryName);
  }

  /** Test case: tests the getProblemCategoryUsingCategorySlug(String) method. Test data: a non-existing slug. Expected: a null reference. */
  @Test
  public void testGetProblemCategoryUsingSlugNotExists() {
    ProblemCategory problemCategory =
        problemCategoryMapper.getProblemCategoryUsingCategorySlug("not-exists");
    Assertions.assertNull(problemCategory);
  }

  /**
   * Test case: tests the createProblemCategory(ProblemCategory) method. Test data: a valid data set,
   * and no identical slug exists in the table. Expected: the data insertion operation completes
   * successfully.
   */
  @Test
  public void testCreateProblemCategoryNormally() {
    ProblemCategory problemCategory = new ProblemCategory("new-category", "New Category", 0);
    int numberOfRowsAffected = problemCategoryMapper.createProblemCategory(problemCategory);
    Assertions.assertEquals(1, numberOfRowsAffected);
  }

  /**
   * Test case: tests the createProblemCategory(ProblemCategory) method. Test data: an invalid data set
   * (a category name that is too long). Expected: a DataIntegrityViolationException is thrown.
   */
  @Test
  public void testCreateProblemCategoryUsingTooLongCategoryName() {
    ProblemCategory problemCategory =
        new ProblemCategory("new-category", "New Category Very Very Very Long Name", 0);
    Executable e =
        () -> {
          problemCategoryMapper.createProblemCategory(problemCategory);
        };
    Assertions.assertThrows(org.springframework.dao.DataIntegrityViolationException.class, e);
  }

  /**
   * Test case: tests the createProblemCategory(ProblemCategory) method. Test data: a valid data set,
   * but an identical slug exists in the table. Expected: a DuplicateKeyException is thrown.
   */
  @Test
  public void testCreateProblemCategoryUsingExistingSlug() {
    ProblemCategory problemCategory = new ProblemCategory("uncategorized", "New Category", 0);
    Executable e =
        () -> {
          problemCategoryMapper.createProblemCategory(problemCategory);
        };
    Assertions.assertThrows(org.springframework.dao.DuplicateKeyException.class, e);
  }

  /**
   * Test case: tests the createProblemCategoryRelationship(long, ProblemCategory) method. Test data:
   * an existing problem ID and a problem category object. Expected: the relationship between the
   * corresponding problem and problem category is created successfully.
   */
  @Test
  public void testCreateProblemCategoryRelationshipNormally() {
    ProblemCategory problemCategory = problemCategoryMapper.getProblemCategoryUsingCategoryId(2);
    int numberOfRowsAffected =
        problemCategoryMapper.createProblemCategoryRelationship(1002, problemCategory);
    Assertions.assertEquals(1, numberOfRowsAffected);
  }

  /**
   * Test case: tests the createProblemCategoryRelationship(long, ProblemCategory) method. Test data: a
   * non-existing problem ID and an existing problem category object. Expected: a
   * DataIntegrityViolationException is thrown.
   */
  @Test
  public void testCreateProblemCategoryRelationshipUsingNotExistingProblemId() {
    ProblemCategory problemCategory = problemCategoryMapper.getProblemCategoryUsingCategoryId(1);
    Executable e =
        () -> {
          problemCategoryMapper.createProblemCategoryRelationship(0, problemCategory);
        };
    Assertions.assertThrows(org.springframework.dao.DataIntegrityViolationException.class, e);
  }

  /**
   * Test case: tests the createProblemCategoryRelationship(long, ProblemCategory) method. Test data:
   * an existing problem ID and a non-existing problem category object. Expected: a
   * DataIntegrityViolationException is thrown.
   */
  @Test
  public void testCreateProblemCategoryRelationshipUsingNotExistingProblemCategory() {
    Executable e =
        () -> {
          problemCategoryMapper.createProblemCategoryRelationship(1000, null);
        };
    Assertions.assertThrows(org.springframework.dao.DataIntegrityViolationException.class, e);
  }

  /**
   * Test case: tests the createProblemCategoryRelationship(long, ProblemCategory) method. Test data:
   * an existing problem ID and an existing problem category object. Expected: a
   * DuplicateKeyException is thrown.
   */
  @Test
  public void
      testCreateProblemCategoryRelationshipUsingExistingProblemIdAndExistingProblemCategory() {
    ProblemCategory problemCategory = problemCategoryMapper.getProblemCategoryUsingCategoryId(1);
    Executable e =
        () -> {
          problemCategoryMapper.createProblemCategoryRelationship(1000, problemCategory);
        };
    Assertions.assertThrows(org.springframework.dao.DuplicateKeyException.class, e);
  }

  /**
   * Test case: tests the updateProblemCategory(ProblemCategory) method. Test data: a valid data set, and a corresponding record exists in the database. Expected: the data update operation completes successfully.
   */
  @Test
  public void testUpdateProblemCategoryNormally() {
    ProblemCategory problemCategory =
        problemCategoryMapper.getProblemCategoryUsingCategorySlug("uncategorized");
    Assertions.assertNotNull(problemCategory);

    problemCategory.setProblemCategoryName("New Category Name");
    int numberOfRowsAffected = problemCategoryMapper.updateProblemCategory(problemCategory);
    Assertions.assertEquals(1, numberOfRowsAffected);

    problemCategory = problemCategoryMapper.getProblemCategoryUsingCategorySlug("uncategorized");
    String newCategoryName = problemCategory.getProblemCategoryName();
    Assertions.assertEquals("New Category Name", newCategoryName);
  }

  /**
   * Test case: tests the updateProblemCategory(ProblemCategory) method. Test data: an invalid data set
   * (a category name that is too long). Expected: a DataIntegrityViolationException is thrown.
   */
  @Test
  public void testUpdateProblemCategoryUsingTooLongCategoryName() {
    ProblemCategory problemCategory =
        problemCategoryMapper.getProblemCategoryUsingCategorySlug("uncategorized");
    Assertions.assertNotNull(problemCategory);

    problemCategory.setProblemCategoryName("New Category Very Very Very Long Name");
    Executable e =
        () -> {
          problemCategoryMapper.updateProblemCategory(problemCategory);
        };
    Assertions.assertThrows(org.springframework.dao.DataIntegrityViolationException.class, e);
  }

  /** Test case: tests the deleteProblemCategory(int) method. Test data: an existing problem category unique identifier. Expected: the data deletion operation completes successfully. */
  @Test
  public void testDeleteProblemCategoryExists() {
    ProblemCategory problemCategory = problemCategoryMapper.getProblemCategoryUsingCategoryId(1);
    Assertions.assertNotNull(problemCategory);

    int numberOfRowsAffected = problemCategoryMapper.deleteProblemCategory(1);
    Assertions.assertEquals(1, numberOfRowsAffected);

    problemCategory = problemCategoryMapper.getProblemCategoryUsingCategoryId(1);
    Assertions.assertNull(problemCategory);
  }

  /** Test case: tests the deleteProblemCategory(int) method. Test data: a non-existing problem category unique identifier. Expected: the method executes normally without affecting the data in the table. */
  @Test
  public void testDeleteProblemCategoryNotExists() {
    ProblemCategory problemCategory = problemCategoryMapper.getProblemCategoryUsingCategoryId(0);
    Assertions.assertNull(problemCategory);

    int numberOfRowsAffected = problemCategoryMapper.deleteProblemCategory(0);
    Assertions.assertEquals(0, numberOfRowsAffected);
  }

  /** The ProblemCategoryMapper object under test. */
  @Autowired private ProblemCategoryMapper problemCategoryMapper;
}
