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
 * The test class for SeoUtils.
 *
 * @author Haozhe Xie
 */
public class SeoUtilsTest {
  /** Test case: tests the getDescription(String) method. Test data: Markdown with a heading, emphasis, a link, an image and inline math. Expected: the plain text with the markup removed and the whitespace collapsed. */
  @Test
  public void testGetDescriptionWithMarkdown() {
    Assertions.assertEquals(
        "Sum Given two integers a and b, see the docs. Output a+b.",
        SeoUtils.getDescription(
            "# Sum\n\nGiven **two** integers `a` and `b`, see [the docs](https://x.y).\n"
                + "![figure](a.png)\nOutput $a+b$."));
  }

  /** Test case: tests the getDescription(String) method. Test data: HTML with tags and entities. Expected: the plain text with the tags removed and the entities decoded. */
  @Test
  public void testGetDescriptionWithHtml() {
    Assertions.assertEquals(
        "1 < n < 100 lines", SeoUtils.getDescription("<p>1 &lt; n &lt; 100</p><br/><p>lines</p>"));
  }

  /** Test case: tests the getDescription(String) method. Test data: a 200-character text. Expected: a 160-character text ending with an ellipsis. */
  @Test
  public void testGetDescriptionTruncatesLongText() {
    String description = SeoUtils.getDescription("中".repeat(200));
    Assertions.assertEquals("中".repeat(159) + "…", description);
  }

  /** Test case: tests the getDescription(String) method. Test data: null, blank text and markup only. Expected: null, so the page falls back to the site-wide description. */
  @Test
  public void testGetDescriptionWithoutText() {
    Assertions.assertNull(SeoUtils.getDescription(null));
    Assertions.assertNull(SeoUtils.getDescription("  \n "));
    Assertions.assertNull(SeoUtils.getDescription("<p></p> ![x](y.png)"));
  }
}
