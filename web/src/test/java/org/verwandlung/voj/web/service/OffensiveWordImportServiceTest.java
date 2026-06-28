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
package org.verwandlung.voj.web.service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * The test class for OffensiveWordImportService.
 *
 * <p>These tests exercise the two pure {@code private static} helpers in isolation (invoked via
 * {@link ReflectionTestUtils}); the live-HTTP {@code fetch} / {@code importFromSources} paths are
 * not unit-tested because they {@code new} a {@code HttpClient} and reach out over the network.
 *
 * @author Haozhe Xie
 */
public class OffensiveWordImportServiceTest {
  /** Test case: tests parseSourceUrls(String). Test data: several URLs across separate lines. Expected: every URL is returned in order. */
  @Test
  public void testParseSourceUrlsMultipleLines() {
    List<String> urls =
        parseSourceUrls("https://example.org/a.txt\nhttps://example.org/b.txt\nhttps://example.org/c.txt");
    Assertions.assertEquals(
        List.of(
            "https://example.org/a.txt",
            "https://example.org/b.txt",
            "https://example.org/c.txt"),
        urls);
  }

  /** Test case: tests parseSourceUrls(String). Test data: a list with blank lines, surrounding whitespace and a CRLF separator. Expected: blanks are dropped and each URL is trimmed. */
  @Test
  public void testParseSourceUrlsSkipsBlankLinesAndTrims() {
    List<String> urls =
        parseSourceUrls("  https://example.org/a.txt  \r\n\r\n\t\nhttps://example.org/b.txt\n   \n");
    Assertions.assertEquals(
        List.of("https://example.org/a.txt", "https://example.org/b.txt"), urls);
  }

  /** Test case: tests parseSourceUrls(String). Test data: lines beginning with '#'. Expected: comment lines are ignored. */
  @Test
  public void testParseSourceUrlsSkipsComments() {
    List<String> urls =
        parseSourceUrls("# a comment\nhttps://example.org/a.txt\n#another\nhttps://example.org/b.txt");
    Assertions.assertEquals(
        List.of("https://example.org/a.txt", "https://example.org/b.txt"), urls);
  }

  /** Test case: tests parseSourceUrls(String). Test data: a null source string. Expected: an empty list (no NullPointerException). */
  @Test
  public void testParseSourceUrlsNull() {
    Assertions.assertTrue(parseSourceUrls(null).isEmpty());
  }

  /** Test case: tests parseSourceUrls(String). Test data: an empty / whitespace-only string. Expected: an empty list. */
  @Test
  public void testParseSourceUrlsEmpty() {
    Assertions.assertTrue(parseSourceUrls("   \n\t\r\n").isEmpty());
  }

  /** Test case: tests readBoundedUtf8(InputStream). Test data: a small UTF-8 body. Expected: the body is decoded verbatim. */
  @Test
  public void testReadBoundedUtf8DecodesUtf8() throws IOException {
    String body = "bad\nwords\n敏感词";
    Assertions.assertEquals(body, readBoundedUtf8(streamOf(body)));
  }

  /** Test case: tests readBoundedUtf8(InputStream). Test data: an empty stream. Expected: an empty string. */
  @Test
  public void testReadBoundedUtf8Empty() throws IOException {
    Assertions.assertEquals("", readBoundedUtf8(streamOf("")));
  }

  /** Test case: tests readBoundedUtf8(InputStream). Test data: a body just over the 16 MiB cap. Expected: an IOException is thrown rather than buffering it. */
  @Test
  public void testReadBoundedUtf8RejectsOversizedBody() {
    int overCap = 16 * 1024 * 1024 + 1;
    InputStream oversized =
        new InputStream() {
          private int remaining = overCap;

          @Override
          public int read() {
            if (remaining <= 0) {
              return -1;
            }
            remaining--;
            return 'a';
          }

          @Override
          public int read(byte[] b, int off, int len) {
            if (remaining <= 0) {
              return -1;
            }
            int n = Math.min(len, remaining);
            for (int i = 0; i < n; i++) {
              b[off + i] = 'a';
            }
            remaining -= n;
            return n;
          }
        };
    Assertions.assertThrows(IOException.class, () -> readBoundedUtf8(oversized));
  }

  /** Test case: tests isPlainTextContentType(String). Test data: text/plain with a charset parameter. Expected: accepted (the charset parameter is ignored). */
  @Test
  public void testIsPlainTextContentTypeAcceptsTextPlain() {
    Assertions.assertTrue(isPlainTextContentType("text/plain; charset=utf-8"));
  }

  /** Test case: tests isPlainTextContentType(String). Test data: application/octet-stream, as raw.githubusercontent.com serves raw files. Expected: accepted. */
  @Test
  public void testIsPlainTextContentTypeAcceptsOctetStream() {
    Assertions.assertTrue(isPlainTextContentType("application/octet-stream"));
  }

  /** Test case: tests isPlainTextContentType(String). Test data: an absent (empty) Content-Type. Expected: accepted, since some static hosts omit it for .txt files. */
  @Test
  public void testIsPlainTextContentTypeAcceptsEmpty() {
    Assertions.assertTrue(isPlainTextContentType(""));
  }

  /** Test case: tests isPlainTextContentType(String). Test data: text/html, as returned by a file-view page or a soft-404. Expected: rejected so the HTML is not stored as words. */
  @Test
  public void testIsPlainTextContentTypeRejectsHtml() {
    Assertions.assertFalse(isPlainTextContentType("text/html; charset=utf-8"));
  }

  /** Test case: tests isPlainTextContentType(String). Test data: application/json. Expected: rejected. */
  @Test
  public void testIsPlainTextContentTypeRejectsJson() {
    Assertions.assertFalse(isPlainTextContentType("application/json"));
  }

  /** Wraps a string as a UTF-8 input stream. */
  private static InputStream streamOf(String body) {
    return new ByteArrayInputStream(body.getBytes(StandardCharsets.UTF_8));
  }

  /** Invokes the private static isPlainTextContentType helper. */
  private static boolean isPlainTextContentType(String contentType) {
    try {
      Method method =
          OffensiveWordImportService.class.getDeclaredMethod(
              "isPlainTextContentType", String.class);
      method.setAccessible(true);
      return (boolean) method.invoke(null, contentType);
    } catch (ReflectiveOperationException ex) {
      throw new RuntimeException(ex);
    }
  }

  /** Invokes the private static parseSourceUrls helper. */
  @SuppressWarnings("unchecked")
  private static List<String> parseSourceUrls(String sources) {
    try {
      Method method =
          OffensiveWordImportService.class.getDeclaredMethod("parseSourceUrls", String.class);
      method.setAccessible(true);
      return (List<String>) method.invoke(null, sources);
    } catch (ReflectiveOperationException ex) {
      throw new RuntimeException(ex);
    }
  }

  /** Invokes the private static readBoundedUtf8 helper, re-throwing its IOException unwrapped. */
  private static String readBoundedUtf8(InputStream in) throws IOException {
    try {
      Method method =
          OffensiveWordImportService.class.getDeclaredMethod("readBoundedUtf8", InputStream.class);
      method.setAccessible(true);
      return (String) method.invoke(null, in);
    } catch (InvocationTargetException ex) {
      if (ex.getCause() instanceof IOException) {
        throw (IOException) ex.getCause();
      }
      throw new RuntimeException(ex.getCause());
    } catch (ReflectiveOperationException ex) {
      throw new RuntimeException(ex);
    }
  }
}
