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

import java.util.Locale;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * The test class for LocaleUtils.
 *
 * @author Haozhe Xie
 */
public class LocaleUtilsTest {
  /** Test case: tests the getLocaleOfLanguage(String) method. Test data: the en_US language slug. Expected: a Locale with the English language and the US country. */
  @Test
  public void testGetLocaleOfLanguageForEnglish() {
    Locale locale = LocaleUtils.getLocaleOfLanguage("en_US");
    Assertions.assertEquals("en", locale.getLanguage());
    Assertions.assertEquals("US", locale.getCountry());
  }

  /** Test case: tests the getLocaleOfLanguage(String) method. Test data: the zh_CN language slug. Expected: a Locale with the Chinese language and the China country. */
  @Test
  public void testGetLocaleOfLanguageForChinese() {
    Locale locale = LocaleUtils.getLocaleOfLanguage("zh_CN");
    Assertions.assertEquals("zh", locale.getLanguage());
    Assertions.assertEquals("CN", locale.getCountry());
  }

  /** Test case: tests the getLocaleOfLanguage(String) method. Test data: a slug without a country part. Expected: an ArrayIndexOutOfBoundsException, because a country part is required. */
  @Test
  public void testGetLocaleOfLanguageWithoutCountry() {
    Assertions.assertThrows(
        ArrayIndexOutOfBoundsException.class, () -> LocaleUtils.getLocaleOfLanguage("en"));
  }
}
