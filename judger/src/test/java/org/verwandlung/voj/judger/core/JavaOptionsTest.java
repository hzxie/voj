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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

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
   * Test case: runs that failed, with the exit code and the end of the output of a JVM that ran
   * out of memory, or not. Expected result: only a run that exited with 3 and whose last line of
   * output holds the JVM's report ran out of memory, whatever the line starts with and however it
   * ends; not one whose report is followed by more output, that exited otherwise, or that was
   * stopped at a limit.
   */
  @Test
  public void testOnlyARunEndingWithTheJvmsReportRanOutOfMemory(@TempDir Path directory)
      throws Exception {
    String report = "Terminating due to java.lang.OutOfMemoryError: Java heap space";
    String output = write(directory, "output", "1\n2\n" + report + "\n");
    Assertions.assertTrue(JavaOptions.isOutOfMemoryExit(Verdict.RUNTIME_ERROR, 3, output));
    Assertions.assertTrue(
        JavaOptions.isOutOfMemoryExit(
            Verdict.RUNTIME_ERROR, 3, write(directory, "partial", "1\n2 3" + report + "\n")));
    Assertions.assertTrue(
        JavaOptions.isOutOfMemoryExit(
            Verdict.RUNTIME_ERROR, 3, write(directory, "crlf", report + "\r\n")));
    Assertions.assertTrue(
        JavaOptions.isOutOfMemoryExit(
            Verdict.RUNTIME_ERROR,
            3,
            write(directory, "long", "x".repeat(100000) + "\n" + report + "\n")));
    Assertions.assertTrue(
        JavaOptions.isOutOfMemoryExit(
            Verdict.RUNTIME_ERROR,
            3,
            write(
                directory,
                "array",
                "Terminating due to java.lang.OutOfMemoryError: Requested array size exceeds VM"
                    + " limit\n")));

    Assertions.assertFalse(JavaOptions.isOutOfMemoryExit(Verdict.RUNTIME_ERROR, 1, output));
    Assertions.assertFalse(JavaOptions.isOutOfMemoryExit(Verdict.TIME_LIMIT_EXCEEDED, 3, output));
    Assertions.assertFalse(JavaOptions.isOutOfMemoryExit(Verdict.NORMAL, 0, output));
    Assertions.assertFalse(
        JavaOptions.isOutOfMemoryExit(
            Verdict.RUNTIME_ERROR, 3, write(directory, "followed", report + "\n4\n")));
    Assertions.assertFalse(
        JavaOptions.isOutOfMemoryExit(
            Verdict.RUNTIME_ERROR, 3, write(directory, "other", "1\n2\n")));
    Assertions.assertFalse(
        JavaOptions.isOutOfMemoryExit(Verdict.RUNTIME_ERROR, 3, write(directory, "empty", "")));
    Assertions.assertFalse(
        JavaOptions.isOutOfMemoryExit(
            Verdict.RUNTIME_ERROR, 3, directory.resolve("missing").toString()));
    Assertions.assertFalse(JavaOptions.isOutOfMemoryExit(Verdict.RUNTIME_ERROR, 3, null));
  }

  /**
   * Test case: a run whose output file was replaced by a link to a file that ends with the JVM's
   * report. Expected result: the link is not followed, so the run did not run out of memory.
   */
  @Test
  public void testALinkInPlaceOfTheOutputIsNotFollowed(@TempDir Path directory) throws Exception {
    Path target =
        Path.of(
            write(
                directory, "target", "Terminating due to java.lang.OutOfMemoryError: Metaspace\n"));
    Path link = directory.resolve("output");
    try {
      Files.createSymbolicLink(link, target);
    } catch (UnsupportedOperationException | IOException ex) {
      Assumptions.abort("Symbolic links cannot be created here: " + ex);
    }
    Assertions.assertTrue(
        JavaOptions.isOutOfMemoryExit(Verdict.RUNTIME_ERROR, 3, target.toString()));
    Assertions.assertFalse(
        JavaOptions.isOutOfMemoryExit(Verdict.RUNTIME_ERROR, 3, link.toString()));
  }

  /**
   * Writes a file.
   *
   * @param directory - the directory to write it in
   * @param name - its name
   * @param content - its content
   * @return its path
   * @throws IOException if it cannot be written
   */
  private static String write(Path directory, String name, String content) throws IOException {
    return Files.writeString(directory.resolve(name), content, StandardCharsets.UTF_8).toString();
  }
}
