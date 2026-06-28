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

import org.verwandlung.voj.web.model.ProblemDifficulty;

/**
 * The test class for ProblemDifficultyMapper.
 *
 * @author Haozhe Xie
 */
@ExtendWith(SpringExtension.class)
@Transactional
@ContextConfiguration({"classpath:test-spring-context.xml"})
public class ProblemDifficultyMapperTest {
  /** Test case: tests the getProblemDifficulties() method. Test data: N/a. Expected: the three seeded difficulty levels in ascending order. */
  @Test
  public void testGetProblemDifficulties() {
    List<ProblemDifficulty> difficulties = problemDifficultyMapper.getProblemDifficulties();
    Assertions.assertNotNull(difficulties);
    Assertions.assertEquals(3, difficulties.size());
    Assertions.assertEquals("easy", difficulties.get(0).getProblemDifficultySlug());
    Assertions.assertEquals("hard", difficulties.get(2).getProblemDifficultySlug());
  }

  /** Test case: tests the getProblemDifficultyUsingSlug(String) method. Test data: an existing difficulty slug. Expected: the corresponding ProblemDifficulty object. */
  @Test
  public void testGetProblemDifficultyUsingSlugExists() {
    ProblemDifficulty difficulty = problemDifficultyMapper.getProblemDifficultyUsingSlug("medium");
    Assertions.assertNotNull(difficulty);
    Assertions.assertEquals(2, difficulty.getProblemDifficultyId());
    Assertions.assertEquals("Medium", difficulty.getProblemDifficultyName());
  }

  /** Test case: tests the getProblemDifficultyUsingSlug(String) method. Test data: a non-existing difficulty slug. Expected: a null reference. */
  @Test
  public void testGetProblemDifficultyUsingSlugNotExists() {
    Assertions.assertNull(problemDifficultyMapper.getProblemDifficultyUsingSlug("not-exists"));
  }

  /** The ProblemDifficultyMapper object under test. */
  @Autowired private ProblemDifficultyMapper problemDifficultyMapper;
}
