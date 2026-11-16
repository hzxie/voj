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
package org.verwandlung.voj.judger.core;

import java.util.List;
import java.util.Map;
import java.util.function.UnaryOperator;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Test class for Outcome.
 *
 * @author Haozhe Xie
 */
public class OutcomeTest {
  /**
   * Test case: tests the of method. Test data: four checkpoints, the second and third failed.
   * Expected result: the judge result of the first failed one, the time of all runs, the most
   * memory of one, the score earned, and a log with a line per checkpoint and the summary.
   */
  @Test
  public void testOfReportsTheFirstFailure() {
    Outcome outcome =
        Outcome.of(
            List.of(
                new CheckpointResult(0, "AC", 10, 128, 25),
                new CheckpointResult(1, "TLE", 1000, 256, 0),
                new CheckpointResult(2, "WA", 20, 64, 0),
                new CheckpointResult(3, "AC", 30, 64, 25)),
            NAMES);

    Assertions.assertEquals("TLE", outcome.judgeResult());
    Assertions.assertEquals(1060, outcome.usedTime());
    Assertions.assertEquals(256, outcome.usedMemory());
    Assertions.assertEquals(50, outcome.score());
    Assertions.assertEquals(
        "Compile Successfully.\n\n"
            + "- Test Point #0: Accepted, Time = 10 ms, Memory = 128 KB, Score = 25\n"
            + "- Test Point #1: Time Limit Exceeded, Time = 1000 ms, Memory = 256 KB, Score = 0\n"
            + "- Test Point #2: Wrong Answer, Time = 20 ms, Memory = 64 KB, Score = 0\n"
            + "- Test Point #3: Accepted, Time = 30 ms, Memory = 64 KB, Score = 25\n"
            + "\nTime Limit Exceeded, Time = 1060 ms, Memory = 256 KB, Score = 50\n",
        outcome.log());
  }

  /**
   * Test case: tests the of method. Test data: no checkpoint. Expected result: refused, never an
   * Accepted with nothing checked.
   */
  @Test
  public void testOfWithoutCheckpointsIsRefused() {
    Assertions.assertThrows(IllegalArgumentException.class, () -> Outcome.of(List.of(), NAMES));
  }

  /**
   * Test case: tests the compileError and systemError methods. Expected result: the compiler's
   * output quoted as a code block, and the reason of a System Error.
   */
  @Test
  public void testCompileAndSystemErrors() {
    Outcome compileError = Outcome.compileError("a.cpp:1: error\n#include <windows.h>");
    Assertions.assertEquals("CE", compileError.judgeResult());
    Assertions.assertEquals(
        "Compile Error.\n\n    a.cpp:1: error\n    #include <windows.h>\n\n"
            + "Compile Error, Time = 0 ms, Memory = 0 KB, Score = 0.\n",
        compileError.log());

    Outcome systemError = Outcome.systemError("No test data.");
    Assertions.assertEquals("SE", systemError.judgeResult());
    Assertions.assertEquals("System Error.\n\nNo test data.\n", systemError.log());
  }

  /** The names of the judge results of the tests. */
  private static final UnaryOperator<String> NAMES =
      Map.of("AC", "Accepted", "WA", "Wrong Answer", "TLE", "Time Limit Exceeded")::get;
}
