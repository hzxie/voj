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
package org.verwandlung.voj.judger.model;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Test class for the Problem model.
 *
 * @author Haozhe Xie
 */
public class ProblemTest {
  /**
   * Test case: tests the constructor. Expected result: the getters return the values passed to it,
   * and the version of the test data defaults to zero.
   */
  @Test
  public void testConstructor() {
    Problem problem = new Problem(1000L, 1000, 65536);

    Assertions.assertEquals(1000L, problem.getProblemId());
    Assertions.assertEquals(1000, problem.getTimeLimit());
    Assertions.assertEquals(65536, problem.getMemoryLimit());
    Assertions.assertEquals(0, problem.getCheckpointsVersion());
  }

  /**
   * Test case: tests the setters. Expected result: the getters return the values set.
   */
  @Test
  public void testSetters() {
    Problem problem = new Problem();
    problem.setProblemId(1001L);
    problem.setTimeLimit(2000);
    problem.setMemoryLimit(131072);
    problem.setCheckpointsVersion(3);

    Assertions.assertEquals(1001L, problem.getProblemId());
    Assertions.assertEquals(2000, problem.getTimeLimit());
    Assertions.assertEquals(131072, problem.getMemoryLimit());
    Assertions.assertEquals(3, problem.getCheckpointsVersion());
    Assertions.assertTrue(problem.toString().contains("ProblemID=1001"));
  }
}
