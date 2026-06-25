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

import org.verwandlung.voj.web.model.ProblemTag;
import org.verwandlung.voj.web.model.ProblemTagRelationship;

/**
 * The test class for ProblemTagMapper.
 *
 * @author Haozhe Xie
 */
@ExtendWith(SpringExtension.class)
@Transactional
@ContextConfiguration({"classpath:test-spring-context.xml"})
public class ProblemTagMapperTest {
  /** Test case: tests the getProblemTags() method. Test data: N/a. Expected: the list of all problem tags. */
  @Test
  public void testGetProblemTags() {
    List<ProblemTag> problemTags = problemTagMapper.getProblemTags();
    Assertions.assertEquals(2, problemTags.size());

    ProblemTag firstTag = problemTags.get(0);
    String tagSlug = firstTag.getProblemTagSlug();
    Assertions.assertEquals("greedy", tagSlug);
  }

  /** Test case: tests the getProblemTagUsingId(int) method. Test data: the unique identifier of a default tag. Expected: the problem tag object of the default tag. */
  @Test
  public void testGetProblemTagUsingIdExists() {
    ProblemTag problemTag = problemTagMapper.getProblemTagUsingTagId(1);
    Assertions.assertNotNull(problemTag);

    String problemTagName = problemTag.getProblemTagName();
    Assertions.assertEquals("Greedy", problemTagName);
  }

  /** Test case: tests the getProblemTagUsingId(int) method. Test data: a non-existing unique identifier. Expected: a null reference. */
  @Test
  public void testGetProblemTagUsingIdNotExists() {
    ProblemTag problemTag = problemTagMapper.getProblemTagUsingTagId(0);
    Assertions.assertNull(problemTag);
  }

  /** Test case: tests the getProblemTagsUsingProblemId(long) method. Test data: an existing problem ID. Expected: the problem tags of the problem. */
  @Test
  public void testGetProblemTagUsingProblemIdExists() {
    List<ProblemTag> problemTags = problemTagMapper.getProblemTagsUsingProblemId(1001);
    Assertions.assertNotNull(problemTags);
    Assertions.assertEquals(2, problemTags.size());

    ProblemTag problemTag = problemTags.get(0);
    String problemTagName = problemTag.getProblemTagName();
    Assertions.assertEquals("Greedy", problemTagName);
  }

  /** Test case: tests the getProblemTagsUsingProblemId(long) method. Test data: a non-existing unique identifier. Expected: an empty problem tag list. */
  @Test
  public void testGetProblemTagUsingProblemIdNotExists() {
    List<ProblemTag> problemTags = problemTagMapper.getProblemTagsUsingProblemId(0);
    Assertions.assertEquals(0, problemTags.size());
  }

  /** Test case: tests the getProblemTagsOfProblems() method. Test data: a problem ID range from 1000 to 1002. Expected: all the problem tag information (4 records in total). */
  @Test
  public void testGetProblemTagsOfProblemsFrom1000WithLimit3() {
    List<ProblemTagRelationship> problemTagRelationships =
        problemTagMapper.getProblemTagsOfProblems(1000, 1002);
    Assertions.assertEquals(4, problemTagRelationships.size());

    ProblemTagRelationship ptr = problemTagRelationships.get(1);
    Assertions.assertEquals(1001, ptr.getProblemId());
    Assertions.assertEquals(1, ptr.getProblemTagId());
  }

  /** Test case: tests the getProblemTagsOfProblems() method. Test data: a problem ID range from 999 to 1000. Expected: all the problem tag information (1 record in total). */
  @Test
  public void testGetProblemTagsOfProblemsFrom999WithLimit2() {
    List<ProblemTagRelationship> problemTagRelationships =
        problemTagMapper.getProblemTagsOfProblems(999, 1000);
    Assertions.assertEquals(1, problemTagRelationships.size());

    ProblemTagRelationship ptr = problemTagRelationships.get(0);
    Assertions.assertEquals(1000, ptr.getProblemId());
    Assertions.assertEquals(1, ptr.getProblemTagId());
  }

  /** Test case: tests the getProblemTagsOfProblems() method. Test data: a problem ID range from 999 to 999. Expected: all the problem tag information (0 records in total). */
  @Test
  public void testGetProblemTagsOfProblemsFrom999WithLimit1() {
    List<ProblemTagRelationship> problemTagRelationships =
        problemTagMapper.getProblemTagsOfProblems(999, 999);
    Assertions.assertEquals(0, problemTagRelationships.size());
  }

  /** Test case: tests the getProblemTagUsingSlug(String) method. Test data: the slug of a default tag. Expected: the problem tag object of the default tag. */
  @Test
  public void testGetProblemTagUsingTagSlugExists() {
    ProblemTag problemTag = problemTagMapper.getProblemTagUsingTagSlug("greedy");
    Assertions.assertNotNull(problemTag);

    String problemTagName = problemTag.getProblemTagName();
    Assertions.assertEquals("Greedy", problemTagName);
  }

  /** Test case: tests the getProblemTagUsingSlug(String) method. Test data: a non-existing slug. Expected: a null reference. */
  @Test
  public void testGetProblemTagUsingTagSlugNotExists() {
    ProblemTag problemTag = problemTagMapper.getProblemTagUsingTagSlug("not-exists");
    Assertions.assertNull(problemTag);
  }

  /** Test case: tests the getProblemTagsOfProblems(long, long) method. Test data: problem IDs 1000-1010. Expected: the problem tags of the corresponding problems. */
  @Test
  public void testGetProblemTagsOfProblems() {
    List<ProblemTagRelationship> problemTagRelationships =
        problemTagMapper.getProblemTagsOfProblems(1000, 1010);
    Assertions.assertEquals(5, problemTagRelationships.size());

    ProblemTagRelationship ptr = problemTagRelationships.get(0);
    Assertions.assertEquals(1000, ptr.getProblemId());
    Assertions.assertEquals(1, ptr.getProblemTagId());
  }

  /** Test case: tests the createProblemTag(ProblemTag) method. Test data: a valid data set, and no identical slug exists in the table. Expected: the data insertion operation completes successfully. */
  @Test
  public void testCreateProblemTagNormally() {
    ProblemTag problemTag = new ProblemTag("new- tag", "New Tag");
    int numberOfRowsAffected = problemTagMapper.createProblemTag(problemTag);
    Assertions.assertEquals(1, numberOfRowsAffected);
  }

  /**
   * Test case: tests the createProblemTag(ProblemTag) method. Test data: an invalid data set (a tag
   * name that is too long). Expected: a DataIntegrityViolationException is thrown.
   */
  @Test
  public void testCreateProblemTagUsingTooLongTagName() {
    ProblemTag problemTag = new ProblemTag("new-tag", "New Tag Very Very Very Very Long Name");
    Executable e =
        () -> {
          problemTagMapper.createProblemTag(problemTag);
        };
    Assertions.assertThrows(org.springframework.dao.DataIntegrityViolationException.class, e);
  }

  /**
   * Test case: tests the createProblemTag(ProblemTag) method. Test data: a valid data set, but an
   * identical slug exists in the table. Expected: a DuplicateKeyException is thrown.
   */
  @Test
  public void testCreateProblemTagUsingExistingSlug() {
    ProblemTag problemTag = new ProblemTag("greedy", "New Tag");
    Executable e =
        () -> {
          problemTagMapper.createProblemTag(problemTag);
        };
    Assertions.assertThrows(org.springframework.dao.DuplicateKeyException.class, e);
  }

  /** Test case: tests the updateProblemTag(ProblemTag) method. Test data: a valid data set, and a corresponding record exists in the database. Expected: the data update operation completes successfully. */
  @Test
  public void testUpdateProblemTagNormally() {
    ProblemTag problemTag = problemTagMapper.getProblemTagUsingTagSlug("greedy");
    Assertions.assertNotNull(problemTag);

    problemTag.setProblemTagName("New Tag Name");
    int numberOfRowsAffected = problemTagMapper.updateProblemTag(problemTag);
    Assertions.assertEquals(1, numberOfRowsAffected);

    problemTag = problemTagMapper.getProblemTagUsingTagSlug("greedy");
    String newTagName = problemTag.getProblemTagName();
    Assertions.assertEquals("New Tag Name", newTagName);
  }

  /**
   * Test case: tests the updateProblemTag(ProblemTag) method. Test data: an invalid data set (a tag
   * name that is too long). Expected: a DataIntegrityViolationException is thrown.
   */
  @Test
  public void testUpdateProblemTagUsingTooLongTagName() {
    ProblemTag problemTag = problemTagMapper.getProblemTagUsingTagSlug("greedy");
    Assertions.assertNotNull(problemTag);

    problemTag.setProblemTagName("New Tag Very Very Very Very Long Name");
    Executable e =
        () -> {
          problemTagMapper.updateProblemTag(problemTag);
        };
    Assertions.assertThrows(org.springframework.dao.DataIntegrityViolationException.class, e);
  }

  /** Test case: tests the deleteProblemTag(int) method. Test data: an existing problem tag unique identifier. Expected: the data deletion operation completes successfully. */
  @Test
  public void testDeleteProblemTagUsingTagIdExists() {
    ProblemTag problemTag = problemTagMapper.getProblemTagUsingTagId(1);
    Assertions.assertNotNull(problemTag);

    int numberOfRowsAffected = problemTagMapper.deleteProblemTagUsingTagId(1);
    Assertions.assertEquals(1, numberOfRowsAffected);

    problemTag = problemTagMapper.getProblemTagUsingTagId(1);
    Assertions.assertNull(problemTag);
  }

  /** Test case: tests the deleteProblemTag(int) method. Test data: a non-existing problem tag unique identifier. Expected: the method executes normally without affecting the data in the table. */
  @Test
  public void testDeleteProblemTagUsingTagIdNotExists() {
    ProblemTag problemTag = problemTagMapper.getProblemTagUsingTagId(0);
    Assertions.assertNull(problemTag);

    int numberOfRowsAffected = problemTagMapper.deleteProblemTagUsingTagId(0);
    Assertions.assertEquals(0, numberOfRowsAffected);
  }

  /** Test case: tests the deleteProblemTagRelationship(int) method. Test data: an existing problem tag unique identifier. Expected: the data deletion operation completes successfully. */
  @Test
  public void testDeleteProblemTagUsingProblemIdExists() {
    List<ProblemTag> problemTags = problemTagMapper.getProblemTagsUsingProblemId(1001);
    Assertions.assertEquals(2, problemTags.size());

    int numberOfRowsAffected = problemTagMapper.deleteProblemTagRelationship(1001);
    Assertions.assertEquals(2, numberOfRowsAffected);
  }

  /** Test case: tests the deleteProblemTagRelationship(int) method. Test data: a non-existing problem tag unique identifier. Expected: the method executes normally without affecting the data in the table. */
  @Test
  public void testDeleteProblemTagUsingProblemIdNotExists() {
    List<ProblemTag> problemTags = problemTagMapper.getProblemTagsUsingProblemId(0);
    Assertions.assertEquals(0, problemTags.size());

    int numberOfRowsAffected = problemTagMapper.deleteProblemTagRelationship(0);
    Assertions.assertEquals(0, numberOfRowsAffected);
  }

  /** The ProblemTagMapper object under test. */
  @Autowired private ProblemTagMapper problemTagMapper;
}
