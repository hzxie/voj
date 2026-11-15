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
