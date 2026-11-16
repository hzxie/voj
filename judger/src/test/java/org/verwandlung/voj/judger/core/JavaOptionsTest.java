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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for the options of a run's JVM.
 *
 * @author Haozhe Xie
 */
public class JavaOptionsTest {
  /**
   * Test case: the options of runs with memory limits, and without one. Expected result: the JVM
   * takes the limit for the machine's memory, and its heap may take all of it but 64 MB, or half
   * of it under a small limit; without a limit, the heap is left to the JVM's defaults.
   */
  @Test
  public void testOptionsDeriveTheHeapFromTheLimit() {
    Assertions.assertEquals(
        "-XX:MaxRAM=524288k -Xmx458752k"
            + " -XX:+UseSerialGC -XX:ActiveProcessorCount=1 -XX:+ExitOnOutOfMemoryError",
        JavaOptions.forRun(524288));
    Assertions.assertEquals(
        "-XX:MaxRAM=131072k -Xmx65536k"
            + " -XX:+UseSerialGC -XX:ActiveProcessorCount=1 -XX:+ExitOnOutOfMemoryError",
        JavaOptions.forRun(131072));
    Assertions.assertEquals(
        "-XX:MaxRAM=65536k -Xmx32768k"
            + " -XX:+UseSerialGC -XX:ActiveProcessorCount=1 -XX:+ExitOnOutOfMemoryError",
        JavaOptions.forRun(65536));
    Assertions.assertEquals(
        "-XX:+UseSerialGC -XX:ActiveProcessorCount=1 -XX:+ExitOnOutOfMemoryError",
        JavaOptions.forRun(0));
  }

  /**
   * Test case: runs that failed with exit codes. Expected result: only the java launcher (by any
   * path) exiting with 3 ran out of memory; not another program, another exit code, or a run
   * stopped at a limit.
   */
  @Test
  public void testOnlyTheJavaLauncherExitingWith3RanOutOfMemory() {
    Assertions.assertTrue(
        JavaOptions.isOutOfMemoryExit("java -cp /tmp/voj-1 Main", Verdict.RUNTIME_ERROR, 3));
    Assertions.assertTrue(
        JavaOptions.isOutOfMemoryExit(
            "/usr/lib/jvm/java-25-openjdk-amd64/bin/java -jar /tmp/voj-1/Main.jar",
            Verdict.RUNTIME_ERROR,
            3));
    Assertions.assertTrue(
        JavaOptions.isOutOfMemoryExit(
            "\"C:\\Program Files\\Java\\bin\\java.exe\" -jar C:/voj-1/Main.jar",
            Verdict.RUNTIME_ERROR,
            3));
    Assertions.assertFalse(
        JavaOptions.isOutOfMemoryExit("java -cp /tmp/voj-1 Main", Verdict.RUNTIME_ERROR, 1));
    Assertions.assertFalse(
        JavaOptions.isOutOfMemoryExit("java -cp /tmp/voj-1 Main", Verdict.TIME_LIMIT_EXCEEDED, 3));
    Assertions.assertFalse(
        JavaOptions.isOutOfMemoryExit("/tmp/voj-1/random-name.exe", Verdict.RUNTIME_ERROR, 3));
    Assertions.assertFalse(
        JavaOptions.isOutOfMemoryExit("python3 /tmp/voj-1/java", Verdict.RUNTIME_ERROR, 3));
    Assertions.assertFalse(
        JavaOptions.isOutOfMemoryExit("/tmp/voj-1/javac", Verdict.RUNTIME_ERROR, 3));
  }
}
