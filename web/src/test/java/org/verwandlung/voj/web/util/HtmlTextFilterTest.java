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
 * The test class for TextFilter.
 *
 * @author Haozhe Xie
 */
public class HtmlTextFilterTest {
  /** Test case: tests the filterHtml() method. Test data: a string with matched HTML. Expected: the HTML-filtered string. */
  @Test
  public void testFilterHtmlUsingMatchedHtml() {
    String text = "XSS <script type=\"text/javascript\">alert('XSS')</script>.";
    Assertions.assertEquals("XSS .", HtmlTextFilter.filter(text));
  }

  /** Test case: tests the filterHtml() method. Test data: a string with unmatched HTML. Expected: the HTML-filtered string. */
  @Test
  public void testFilterHtmlUsingUnmatchedHtml() {
    String text = "XSS <a href=\"http://zjhzxhz.com\">alert('XSS')</script>.";
    Assertions.assertEquals("XSS alert('XSS').", HtmlTextFilter.filter(text));
  }
}
