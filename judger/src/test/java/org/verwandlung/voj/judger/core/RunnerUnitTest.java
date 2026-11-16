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

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for the native runner's verdict mapping, which need neither the native library nor a
 * database.
 *
 * @author Haozhe Xie
 */
public class RunnerUnitTest {
  /**
   * Test case: the native runner reports the limit the program exceeded. Expected result: that
   * limit's verdict, even when the program exited with 0.
   */
  @Test
  public void testReportedLimitComesFirst() {
    Assertions.assertEquals(Verdict.TIME_LIMIT_EXCEEDED, verdict(Runner.LIMIT_TIME, 0, 10, 10));
    Assertions.assertEquals(Verdict.MEMORY_LIMIT_EXCEEDED, verdict(Runner.LIMIT_MEMORY, 0, 10, 10));
    Assertions.assertEquals(Verdict.OUTPUT_LIMIT_EXCEEDED, verdict(Runner.LIMIT_OUTPUT, 0, 10, 10));
    Assertions.assertEquals(Verdict.OUTPUT_LIMIT_EXCEEDED, verdict(Runner.LIMIT_OUTPUT, 153, 10, 10));
  }

  /**
   * Test case: a program that exited with 0 but used more than a limit. Expected result: never a
   * normal run, even without a reported limit.
   */
  @Test
  public void testExitZeroOverALimitIsNotNormal() {
    Assertions.assertEquals(Verdict.TIME_LIMIT_EXCEEDED, verdict(Runner.LIMIT_NONE, 0, 1001, 10));
    Assertions.assertEquals(Verdict.MEMORY_LIMIT_EXCEEDED, verdict(Runner.LIMIT_NONE, 0, 10, 65537));
    Assertions.assertEquals(Verdict.TIME_LIMIT_EXCEEDED, verdict(null, 0, 1001, 10));
  }

  /**
   * Test case: runs within their limits. Expected result: an exit code of 0 is a normal run (also
   * exactly at a limit, and with a native library that does not report limits), anything else a
   * runtime error.
   */
  @Test
  public void testRunsWithinTheLimits() {
    Assertions.assertEquals(Verdict.NORMAL, verdict(Runner.LIMIT_NONE, 0, 1000, 65536));
    Assertions.assertEquals(Verdict.NORMAL, verdict(null, 0, 10, 10));
    Assertions.assertEquals(Verdict.RUNTIME_ERROR, verdict(Runner.LIMIT_NONE, 1, 10, 10));
    Assertions.assertEquals(Verdict.RUNTIME_ERROR, verdict(Runner.LIMIT_NONE, 134, 10, 10));
  }

  /**
   * Test case: a failed run at a limit, reported without a limit. Expected result: the historical
   * heuristic attributes it to the limit it reached.
   */
  @Test
  public void testFailedRunAtALimit() {
    Assertions.assertEquals(Verdict.TIME_LIMIT_EXCEEDED, verdict(Runner.LIMIT_NONE, 137, 1000, 10));
    Assertions.assertEquals(
        Verdict.MEMORY_LIMIT_EXCEEDED, verdict(Runner.LIMIT_NONE, 137, 10, 65536));
  }

  /**
   * Test case: no result map. Expected result: a system error.
   */
  @Test
  public void testMissingResultIsASystemError() {
    Assertions.assertEquals(
        Verdict.INTERNAL_ERROR, Runner.toRunResult(null, 1000, 65536).getVerdict());
  }

  /**
   * Maps a native result under a 1000 ms / 65536 KB limit.
   *
   * @param limitExceeded - the reported limit, or {@code null} to leave the key out
   * @param exitCode - the exit code
   * @param usedTime - the CPU time (ms)
   * @param usedMemory - the peak memory (KB)
   * @return the verdict
   */
  private static Verdict verdict(Integer limitExceeded, int exitCode, int usedTime, int usedMemory) {
    Map<String, Object> nativeResult = new HashMap<>();
    nativeResult.put("exitCode", exitCode);
    nativeResult.put("usedTime", usedTime);
    nativeResult.put("usedMemory", usedMemory);
    if (limitExceeded != null) {
      nativeResult.put("limitExceeded", limitExceeded);
    }
    RunResult result = Runner.toRunResult(nativeResult, 1000, 65536);
    Assertions.assertEquals(usedTime, result.getCpuTimeMs());
    return result.getVerdict();
  }
}
