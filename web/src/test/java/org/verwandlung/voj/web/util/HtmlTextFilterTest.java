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
package org.verwandlung.voj.web.util;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * The test class for HtmlTextFilter. User text is stored as plain text, so the filter must keep it
 * verbatim (escaping is the job of every output path).
 *
 * @author Haozhe Xie
 */
public class HtmlTextFilterTest {
  /** Test case: tests the filter() method. Test data: null. Expected: null. */
  @Test
  public void testFilterNull() {
    Assertions.assertNull(HtmlTextFilter.filter(null));
  }

  /** Test case: tests the filter() method. Test data: C++ snippets with angle brackets. Expected: kept verbatim instead of being parsed as tags and dropped. */
  @Test
  public void testFilterKeepsCodeWithAngleBrackets() {
    String code = "#include <iostream>\nvector<int> v;\nif (a<b && b>c) {}";
    Assertions.assertEquals(code, HtmlTextFilter.filter(code));
  }

  /** Test case: tests the filter() method. Test data: text with ampersands, quotes and entity-like sequences. Expected: kept verbatim and not entity-encoded. */
  @Test
  public void testFilterDoesNotEncodeEntities() {
    Assertions.assertEquals("A & B", HtmlTextFilter.filter("A & B"));
    Assertions.assertEquals("\"quoted\" 'text'", HtmlTextFilter.filter("\"quoted\" 'text'"));
    Assertions.assertEquals("&amp; &lt;", HtmlTextFilter.filter("&amp; &lt;"));
  }

  /** Test case: tests the filter() method. Test data: markup that would be dangerous if rendered unescaped. Expected: kept verbatim as text (the output paths escape it). */
  @Test
  public void testFilterKeepsMarkupAsPlainText() {
    String text = "XSS <script type=\"text/javascript\">alert('XSS')</script>. <img src=x onerror=alert(1)>";
    Assertions.assertEquals(text, HtmlTextFilter.filter(text));
  }

  /** Test case: tests the filter() method. Test data: Markdown with whitespace and line breaks. Expected: whitespace, tabs and line breaks are preserved, so code blocks keep their indentation. */
  @Test
  public void testFilterKeepsWhitespace() {
    String text = "  leading\r\n\tcode block\n\n    indented  ";
    Assertions.assertEquals(text, HtmlTextFilter.filter(text));
  }

  /** Test case: tests the filter() method. Test data: text with NUL, escape, DEL and C1 control characters. Expected: those control characters are removed. */
  @Test
  public void testFilterRemovesControlCharacters() {
    Assertions.assertEquals("abcd", HtmlTextFilter.filter("a\u0000b\u001bc\u007fd\u0085"));
    Assertions.assertEquals("", HtmlTextFilter.filter("\u0000"));
  }

  /** Test case: tests the filter() method. Test data: non-ASCII text. Expected: kept verbatim, so the length checks count the real characters. */
  @Test
  public void testFilterKeepsUnicode() {
    String text = "你好, 世界   😀";
    Assertions.assertEquals(text, HtmlTextFilter.filter(text));
    Assertions.assertEquals(5, HtmlTextFilter.filter("a & b").length());
  }
}
