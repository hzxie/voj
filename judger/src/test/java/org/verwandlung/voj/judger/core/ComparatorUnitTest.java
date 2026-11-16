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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;

/**
 * Database-free unit tests for the output comparator.
 *
 * @author Haozhe Xie
 */
public class ComparatorUnitTest {
  /** Test case: identical outputs are accepted in both modes. */
  @Test
  public void testIdentical() throws Exception {
    assertResult("1 2\n3\n", "1 2\n3\n", "AC", "AC");
  }

  /** Test case: trailing spaces AND tabs on a line are ignored leniently, PE when exact. */
  @Test
  public void testTrailingWhitespace() throws Exception {
    assertResult("3\n", "3 \n", "AC", "PE");
    assertResult("3\n", "3\t\n", "AC", "PE");
    assertResult("3 \t\n", "3\n", "AC", "PE");
  }

  /** Test case: trailing blank lines are ignored leniently, PE when exact. */
  @Test
  public void testTrailingBlankLines() throws Exception {
    assertResult("3\n", "3\n\n \n\t\n", "AC", "PE");
    assertResult("3\n\n\n", "3", "AC", "PE");
  }

  /** Test case: a missing or extra final newline, and CRLF line endings, are an exact match. */
  @Test
  public void testFinalNewlineAndCrlf() throws Exception {
    assertResult("1\n2\n", "1\n2", "AC", "AC");
    assertResult("1\n2", "1\n2\n", "AC", "AC");
    assertResult("1\r\n2\r\n", "1\n2\n", "AC", "AC");
    assertResult("1\n2\n", "1\r\n2", "AC", "AC");
    assertResult("", "\n", "AC", "AC");
  }

  /** Test case: a second trailing newline is not part of the exact-match tolerance. */
  @Test
  public void testDoubleTrailingNewline() throws Exception {
    assertResult("1\n\n", "1\n", "AC", "PE");
    assertResult("1\n", "1\n\n", "AC", "PE");
  }

  /** Test case: leading whitespace, inner blank lines and different content are wrong answers. */
  @Test
  public void testWrongAnswer() throws Exception {
    assertResult("45652  \n\n", "24334", "WA", "WA");
    assertResult("45652\n", "\n", "WA", "WA");
    assertResult("1\n2\n", "1\n\n2\n", "WA", "WA");
    assertResult("1 2\n", " 1 2\n", "WA", "WA");
    assertResult("1 2\n", "1  2\n", "WA", "WA");
    assertResult("12\n", "1\n", "WA", "WA");
  }

  /**
   * Test case: a lone CR. Expected result: it ends a line when comparing leniently, but is content
   * in an exact match (only CRLF counts as LF there).
   */
  @Test
  public void testLoneCarriageReturn() throws Exception {
    assertResult("1\n2\n", "1\r2\r", "AC", "PE");
    assertResult("1\r\r\n2", "1\n\n2\n", "AC", "PE");
    assertResult("1\n2", "1\r\n\r2", "WA", "WA");
    assertResult("1\r", "1", "AC", "PE");
  }

  /**
   * Test case: whitespace differences where the lines diverge. Expected result: trailing spaces and
   * tabs of any mix are ignored leniently, but any content after them is not.
   */
  @Test
  public void testWhitespaceAtTheDivergence() throws Exception {
    assertResult("1 \t \n2\n", "1\t\n2\n", "AC", "PE");
    assertResult("1 2\n", "1 \t2\n", "WA", "WA");
    assertResult("1  \n", "1  x\n", "WA", "WA");
    assertResult(" \n", "", "AC", "PE");
    assertResult("", " \t\n\n\t", "AC", "PE");
    assertResult("1\n \n2\n", "1\n\n2\n", "AC", "PE");
    assertResult("1\n\n", "1\n \n", "AC", "PE");
    assertResult("1\n\n2", "1\n2", "WA", "WA");
    assertResult("1\f\n", "1\n", "WA", "WA");
  }

  /** Test case: non-ASCII bytes are compared byte-for-byte. */
  @Test
  public void testNonAsciiBytes() throws Exception {
    Path std = tempDir.resolve("std-bytes.txt");
    Path out = tempDir.resolve("out-bytes.txt");
    Files.write(std, new byte[] {(byte) 0xff, '\n'});
    Files.write(out, new byte[] {(byte) 0xfe, '\n'});
    Assertions.assertEquals(
        "WA", comparator.getComparisonResult(std.toString(), out.toString(), false));
    Assertions.assertEquals(
        "WA", comparator.getComparisonResult(std.toString(), out.toString(), true));
  }

  /** Test case: outputs larger than the read buffer, differing only in the last byte. */
  @Test
  public void testLargeOutput() throws Exception {
    String body = "1234567\r\n".repeat(5000);
    assertResult(body + "8\n", body.replace("\r\n", "\n") + "8", "AC", "AC");
    assertResult(body + "8\n", body + "9\n", "WA", "WA");
  }

  /**
   * Test case: 48 MB outputs on a single line, without a newline, compared through the files.
   * Expected result: the right verdicts, while the comparison allocates a small fraction of a line
   * (it streams the files instead of reading whole lines).
   */
  @Test
  public void testVeryLongLineIsComparedInBoundedMemory() throws Exception {
    long length = 48L << 20;
    Path std = writeLine(tempDir.resolve("std-long.txt"), length, "");
    Path trailingWhitespace = writeLine(tempDir.resolve("out-long-ws.txt"), length, " \t\r\n");
    Path different = writeLine(tempDir.resolve("out-long-diff.txt"), length - 1, "y");

    long allocated = allocatedBytes();
    Assertions.assertEquals(
        "PE", comparator.getComparisonResult(std.toString(), trailingWhitespace.toString(), true));
    Assertions.assertEquals(
        "AC", comparator.getComparisonResult(std.toString(), trailingWhitespace.toString(), false));
    Assertions.assertEquals(
        "WA", comparator.getComparisonResult(std.toString(), different.toString(), true));
    allocated = allocatedBytes() - allocated;
    Assertions.assertTrue(allocated < (4L << 20), "Allocated " + allocated + " bytes");
  }

  /**
   * Gets the number of bytes the current thread has allocated so far.
   *
   * @return the allocated bytes
   */
  private static long allocatedBytes() {
    return ((com.sun.management.ThreadMXBean)
            java.lang.management.ManagementFactory.getThreadMXBean())
        .getCurrentThreadAllocatedBytes();
  }

  /**
   * Writes a file holding a single line of {@code length} 'x' characters, followed by a suffix.
   *
   * @param path - the file to write
   * @param length - the number of 'x' characters
   * @param suffix - the bytes written after them
   * @return the path of the file
   */
  private static Path writeLine(Path path, long length, String suffix) throws Exception {
    try (java.io.OutputStream outputStream = Files.newOutputStream(path)) {
      longLine(length, suffix).transferTo(outputStream);
    }
    return path;
  }

  /**
   * Generates a single line of {@code length} 'x' characters followed by a suffix, on the fly.
   *
   * @param length - the number of 'x' characters
   * @param suffix - the bytes that follow them
   * @return a stream of the generated bytes
   */
  private static java.io.InputStream longLine(long length, String suffix) {
    byte[] suffixBytes = suffix.getBytes(java.nio.charset.StandardCharsets.ISO_8859_1);
    return new java.io.InputStream() {
      @Override
      public int read() {
        byte[] single = new byte[1];
        return read(single, 0, 1) == -1 ? -1 : single[0] & 0xFF;
      }

      @Override
      public int read(byte[] buffer, int offset, int count) {
        long total = length + suffixBytes.length;
        if (position >= total) {
          return -1;
        }
        int n = (int) Math.min(count, total - position);
        for (int i = 0; i < n; ++i, ++position) {
          buffer[offset + i] =
              position < length ? (byte) 'x' : suffixBytes[(int) (position - length)];
        }
        return n;
      }

      private long position;
    };
  }

  /**
   * Test case: the user's output is replaced by a symbolic link to the answer itself. Expected
   * result: the link is not followed, so the submission does not get AC for "printing" the answer.
   */
  @Test
  @DisabledOnOs(OS.WINDOWS)
  public void testSymlinkedOutputIsNotFollowed() throws Exception {
    Path std = Files.writeString(tempDir.resolve("std-link.txt"), "42\n");
    Path out = Files.createSymbolicLink(tempDir.resolve("out-link.txt"), std);
    Assertions.assertThrows(
        java.io.IOException.class,
        () -> comparator.getComparisonResult(std.toString(), out.toString(), false));
    Assertions.assertThrows(
        java.io.IOException.class,
        () -> comparator.getComparisonResult(std.toString(), out.toString(), true));
  }

  /**
   * Asserts the lenient and the exact comparison results of two outputs.
   *
   * @param expected - the standard output
   * @param actual - the user's output
   * @param lenientResult - the expected result when an exact match is not required
   * @param exactResult - the expected result when an exact match is required
   */
  private void assertResult(String expected, String actual, String lenientResult, String exactResult)
      throws Exception {
    Path std = Files.writeString(tempDir.resolve("std.txt"), expected);
    Path out = Files.writeString(tempDir.resolve("out.txt"), actual);
    Assertions.assertEquals(
        lenientResult,
        comparator.getComparisonResult(std.toString(), out.toString(), false),
        "lenient: " + expected.replace("\n", "\\n") + " vs " + actual.replace("\n", "\\n"));
    Assertions.assertEquals(
        exactResult,
        comparator.getComparisonResult(std.toString(), out.toString(), true),
        "exact: " + expected.replace("\n", "\\n") + " vs " + actual.replace("\n", "\\n"));
  }

  /** The Comparator object under test. */
  private final Comparator comparator = new Comparator();

  /** A temporary directory managed by JUnit. */
  @TempDir Path tempDir;
}
