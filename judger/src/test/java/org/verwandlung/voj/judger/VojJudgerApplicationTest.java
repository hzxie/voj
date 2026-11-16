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
package org.verwandlung.voj.judger;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Database-free tests for VojJudgerApplication.
 *
 * @author Haozhe Xie
 */
public class VojJudgerApplicationTest {
  /**
   * Test case: tests the getCompileProgram(String) method. Test data: compile commands with and
   * without arguments, extra whitespace, and none at all. Expected result: the first word (java for
   * javac, whose version is asked from the runtime); a command without arguments no longer aborts
   * the startup, and an empty one names no program.
   */
  @Test
  public void testGetCompileProgram() {
    Assertions.assertEquals("g++", VojJudgerApplication.getCompileProgram("g++ -o a a.cpp"));
    Assertions.assertEquals("java", VojJudgerApplication.getCompileProgram("javac {filename}.java"));
    Assertions.assertEquals("make", VojJudgerApplication.getCompileProgram("make"));
    Assertions.assertEquals("go", VojJudgerApplication.getCompileProgram("  go\tbuild x.go"));
    Assertions.assertNull(VojJudgerApplication.getCompileProgram("   "));
    Assertions.assertNull(VojJudgerApplication.getCompileProgram(null));
  }
}
