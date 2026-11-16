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

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Tests {@link Verdict#of}, which judges what every sandbox backend measures.
 *
 * @author Haozhe Xie
 */
public class VerdictTest {
  /**
   * Test case: the sandbox stopped the program for a limit. Expected result: that limit's verdict,
   * whatever the exit code and the figures.
   */
  @Test
  public void testTheLimitTheSandboxStoppedTheProgramForComesFirst() {
    Assertions.assertEquals(
        Verdict.TIME_LIMIT_EXCEEDED, verdict(SandboxResult.Stop.TIME, 0, 10, 10));
    Assertions.assertEquals(
        Verdict.MEMORY_LIMIT_EXCEEDED, verdict(SandboxResult.Stop.MEMORY, 0, 10, 10));
    Assertions.assertEquals(
        Verdict.OUTPUT_LIMIT_EXCEEDED, verdict(SandboxResult.Stop.OUTPUT, 153, 10, 10));
  }

  /**
   * Test case: a program that exited, with 0 or not, just past a limit before the sandbox stopped
   * it. Expected result: that limit's verdict; exactly at a limit is within it.
   */
  @Test
  public void testFiguresPastALimitAreNeverNormal() {
    Assertions.assertEquals(
        Verdict.TIME_LIMIT_EXCEEDED, verdict(SandboxResult.Stop.NONE, 0, 1001, 10));
    Assertions.assertEquals(
        Verdict.MEMORY_LIMIT_EXCEEDED, verdict(SandboxResult.Stop.NONE, 137, 10, 65537));
    Assertions.assertEquals(Verdict.NORMAL, verdict(SandboxResult.Stop.NONE, 0, 1000, 65536));
  }

  /**
   * Test case: programs that exited on their own within their limits. Expected result: 0 is a
   * normal run, anything else a runtime error.
   */
  @Test
  public void testTheExitCodeDecidesWithinTheLimits() {
    Assertions.assertEquals(Verdict.NORMAL, verdict(SandboxResult.Stop.NONE, 0, 10, 10));
    Assertions.assertEquals(Verdict.RUNTIME_ERROR, verdict(SandboxResult.Stop.NONE, 1, 10, 10));
    Assertions.assertEquals(Verdict.RUNTIME_ERROR, verdict(SandboxResult.Stop.NONE, 134, 999, 10));
  }

  /**
   * Test case: outputs of the output limit and one byte less, of programs that exited on their own
   * (e.g. ignoring SIGXFSZ, so that their writes past the limit failed). Expected result: an
   * output that reached the limit exceeded it, whatever the exit code; one below it did not.
   */
  @Test
  public void testAnOutputThatReachedTheLimitExceededIt(@TempDir Path directory) throws Exception {
    Path full = Files.write(directory.resolve("full"), new byte[1024]);
    Path below = Files.write(directory.resolve("below"), new byte[1023]);

    Assertions.assertEquals(Verdict.OUTPUT_LIMIT_EXCEEDED, verdict(full, false, 0));
    Assertions.assertEquals(Verdict.OUTPUT_LIMIT_EXCEEDED, verdict(full, false, 1));
    Assertions.assertEquals(Verdict.OUTPUT_LIMIT_EXCEEDED, verdict(full, true, 0));
    Assertions.assertEquals(Verdict.NORMAL, verdict(below, false, 0));
  }

  /**
   * Test case: runs and a compilation that exited with 3 after a JVM's out-of-memory report.
   * Expected result: a run's JVM ran out of memory; a compilation (a trusted compiler's) and a
   * report without the exit code are runtime errors.
   */
  @Test
  public void testARunWhoseJvmRanOutOfMemoryExceededTheMemoryLimit(@TempDir Path directory)
      throws Exception {
    Path output =
        Files.writeString(
            directory.resolve("output"), JavaOptions.OUT_OF_MEMORY_REPORT + "Java heap space\n");

    Assertions.assertEquals(Verdict.MEMORY_LIMIT_EXCEEDED, verdict(output, false, 3));
    Assertions.assertEquals(Verdict.RUNTIME_ERROR, verdict(output, true, 3));
    Assertions.assertEquals(Verdict.RUNTIME_ERROR, verdict(output, false, 1));
  }

  /**
   * Test case: a command without an output file. Expected result: refused, as every compilation
   * and run is judged by its output.
   */
  @Test
  public void testEveryCommandHasAnOutputFile() {
    Assertions.assertThrows(
        NullPointerException.class,
        () ->
            new SandboxCommand(
                List.of("/bin/true"),
                List.of(),
                "/tmp",
                null,
                null,
                false,
                SandboxPolicy.limits(false, 1000, 65536, 1, 1)));
  }

  /**
   * Judges a program under a 1000 ms / 65536 KB limit, whose output file does not exist.
   *
   * @param stop - the limit the sandbox stopped it for
   * @param exitCode - its exit code
   * @param cpuTime - its CPU time (ms)
   * @param peakMemory - its peak memory (KB)
   * @return the verdict
   */
  private static Verdict verdict(
      SandboxResult.Stop stop, int exitCode, int cpuTime, int peakMemory) {
    SandboxCommand command =
        new SandboxCommand(
            List.of("/bin/true"),
            List.of(),
            "/tmp",
            null,
            "/voj-no-such-directory/output.txt",
            false,
            SandboxPolicy.limits(false, 1000, 65536, 1, 1));
    return Verdict.of(command, new SandboxResult(cpuTime, peakMemory, exitCode, stop));
  }

  /**
   * Judges a program that exited on its own within its time and memory limits, with an output
   * limit of 1 KB.
   *
   * @param outputFile - its output
   * @param compilation - whether it is a compilation
   * @param exitCode - its exit code
   * @return the verdict
   */
  private static Verdict verdict(Path outputFile, boolean compilation, int exitCode) {
    SandboxCommand command =
        new SandboxCommand(
            List.of("/bin/true"),
            List.of(),
            "/tmp",
            null,
            outputFile.toString(),
            compilation,
            SandboxPolicy.limits(false, 1000, 65536, 1, 1));
    return Verdict.of(command, new SandboxResult(10, 10, exitCode, SandboxResult.Stop.NONE));
  }
}
