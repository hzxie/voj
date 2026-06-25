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
   * Test case: tests the constructor without an identifier. Expected result: the getters return the
   * values passed to the constructor and the identifier defaults to zero.
   */
  @Test
  public void testConstructorWithoutId() {
    Problem problem =
        new Problem(
            true, "A+B", 1000, 65536, "desc", "input", "output", "1 2", "3", "hint");

    Assertions.assertEquals(0, problem.getProblemId());
    Assertions.assertTrue(problem.isPublic());
    Assertions.assertEquals("A+B", problem.getProblemName());
    Assertions.assertEquals(1000, problem.getTimeLimit());
    Assertions.assertEquals(65536, problem.getMemoryLimit());
    Assertions.assertEquals("desc", problem.getDescription());
    Assertions.assertEquals("input", problem.getInputFormat());
    Assertions.assertEquals("output", problem.getOutputFormat());
    Assertions.assertEquals("1 2", problem.getSampleInput());
    Assertions.assertEquals("3", problem.getSampleOutput());
    Assertions.assertEquals("hint", problem.getHint());
  }

  /**
   * Test case: tests the constructor with an identifier. Expected result: the getters return the
   * values passed to the constructor, including the identifier.
   */
  @Test
  public void testConstructorWithId() {
    Problem problem =
        new Problem(
            1000L, true, "A+B", 1000, 65536, "desc", "input", "output", "1 2", "3", "hint");

    Assertions.assertEquals(1000L, problem.getProblemId());
    Assertions.assertEquals("A+B", problem.getProblemName());
  }

  /**
   * Test case: tests the default constructor together with the setters. Expected result: the
   * getters return the values set by the setters.
   */
  @Test
  public void testSetters() {
    Problem problem = new Problem();
    problem.setProblemId(1001L);
    problem.setPublic(false);
    problem.setProblemName("Hello");
    problem.setTimeLimit(2000);
    problem.setMemoryLimit(131072);
    problem.setDescription("d");
    problem.setInpuFormatt("i");
    problem.setOutputFormat("o");
    problem.setSampleInput("si");
    problem.setSampleOutput("so");
    problem.setHint("h");

    Assertions.assertEquals(1001L, problem.getProblemId());
    Assertions.assertFalse(problem.isPublic());
    Assertions.assertEquals("Hello", problem.getProblemName());
    Assertions.assertEquals(2000, problem.getTimeLimit());
    Assertions.assertEquals(131072, problem.getMemoryLimit());
    Assertions.assertEquals("d", problem.getDescription());
    Assertions.assertEquals("i", problem.getInputFormat());
    Assertions.assertEquals("o", problem.getOutputFormat());
    Assertions.assertEquals("si", problem.getSampleInput());
    Assertions.assertEquals("so", problem.getSampleOutput());
    Assertions.assertEquals("h", problem.getHint());
  }

  /** Test case: tests toString. Expected result: a string containing the problem name. */
  @Test
  public void testToString() {
    Problem problem =
        new Problem(
            1000L, true, "A+B", 1000, 65536, "desc", "input", "output", "1 2", "3", "hint");
    Assertions.assertTrue(problem.toString().contains("A+B"));
  }
}
