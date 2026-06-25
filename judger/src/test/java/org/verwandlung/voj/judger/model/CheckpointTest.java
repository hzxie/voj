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
 * Test class for the Checkpoint model.
 *
 * @author Haozhe Xie
 */
public class CheckpointTest {
  /**
   * Test case: tests the parameterized constructor and getters. Expected result: the getters return
   * the values passed to the constructor.
   */
  @Test
  public void testConstructorAndGetters() {
    Checkpoint checkpoint = new Checkpoint(1000L, 1, true, 10, "input", "output");
    Assertions.assertEquals(1000L, checkpoint.getProblemId());
    Assertions.assertEquals(1, checkpoint.getCheckpointId());
    Assertions.assertTrue(checkpoint.isExactlyMatch());
    Assertions.assertEquals(10, checkpoint.getScore());
    Assertions.assertEquals("input", checkpoint.getInput());
    Assertions.assertEquals("output", checkpoint.getOutput());
  }

  /**
   * Test case: tests the default constructor together with the setters. Expected result: the
   * getters return the values set by the setters.
   */
  @Test
  public void testSetters() {
    Checkpoint checkpoint = new Checkpoint();
    checkpoint.setProblemId(1001L);
    checkpoint.setCheckpointId(2);
    checkpoint.setExactlyMatch(false);
    checkpoint.setScore(20);
    checkpoint.setInput("in");
    checkpoint.setOutput("out");

    Assertions.assertEquals(1001L, checkpoint.getProblemId());
    Assertions.assertEquals(2, checkpoint.getCheckpointId());
    Assertions.assertFalse(checkpoint.isExactlyMatch());
    Assertions.assertEquals(20, checkpoint.getScore());
    Assertions.assertEquals("in", checkpoint.getInput());
    Assertions.assertEquals("out", checkpoint.getOutput());
  }

  /** Test case: tests toString. Expected result: a string containing the key identifiers. */
  @Test
  public void testToString() {
    Checkpoint checkpoint = new Checkpoint(1000L, 1, true, 10, "input", "output");
    String value = checkpoint.toString();
    Assertions.assertTrue(value.contains("1000"));
    Assertions.assertTrue(value.contains("CheckPoint"));
  }
}
