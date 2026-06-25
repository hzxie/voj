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
package org.verwandlung.voj.judger.mapper;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

import org.verwandlung.voj.judger.model.Problem;

/**
 * Test class for ProblemMapper.
 *
 * @author Haozhe Xie
 */
@ExtendWith(SpringExtension.class)
@Transactional
@ContextConfiguration({"classpath:test-spring-context.xml"})
public class ProblemMapperTest {
  /**
   * Test case: tests the getProblem() method. Test data: the unique identifier of the "A+B Problem".
   * Expected result: returns the expected problem object.
   */
  @Test
  public void testGetProblemExists() {
    Problem problem = problemMapper.getProblem(1000);
    Assertions.assertNotNull(problem);

    String problemName = problem.getProblemName();
    Assertions.assertEquals("A+B Problem", problemName);
  }

  /**
   * Test case: tests the getProblem() method. Test data: a non-existent problem identifier.
   * Expected result: returns a null reference.
   */
  @Test
  public void testGetProblemNotExists() {
    Problem problem = problemMapper.getProblem(0);
    Assertions.assertNull(problem);
  }

  /** The ProblemMapper object under test. */
  @Autowired private ProblemMapper problemMapper;
}
