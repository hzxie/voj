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
 * The test class for SlugifyUtils.
 *
 * @author Haozhe Xie
 */
public class SlugifyUtilsTest {
  /** Test case: tests the getSlug(String) method. Test data: a string composed of Latin characters. Expected: a string with the Latin characters lowercased and the whitespace removed. */
  @Test
  public void testGetSlugWithAsciiChars() {
    Assertions.assertEquals("a-b--c", SlugifyUtils.getSlug("a b -C"));
  }

  /** Test case: tests the getSlug(String) method. Test data: a string composed of Latin and non-Latin characters. Expected: a string with the non-Latin characters escaped and the whitespace removed. */
  @Test
  public void testGetSlugWithChineseCharacters() {
    Assertions.assertEquals("a%e4%b8%ad%e6%96%87-b", SlugifyUtils.getSlug("a中文 b"));
  }
}
