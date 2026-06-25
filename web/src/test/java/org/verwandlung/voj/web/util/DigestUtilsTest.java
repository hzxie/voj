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
 * The test class for DigestUtils.
 *
 * @author Haozhe Xie
 */
public class DigestUtilsTest {
  /** Test case: tests the md5Hex method. Test data: a valid password. Expected: the MD5-encrypted password. */
  @Test
  public void testMd5HexCase1() {
    String password = "zjhzxhz";
    String runtimeValue = DigestUtils.md5Hex(password);
    String expectedValue = "785ee107c11dfe36de668b1ae7baacbb";

    Assertions.assertEquals(expectedValue, runtimeValue);
  }

  /** Test case: tests the md5Hex method. Test data: a valid password. Expected: the MD5-encrypted password. */
  @Test
  public void testMd5HexCase2() {
    String password = "meigaofei0926";
    String runtimeValue = DigestUtils.md5Hex(password);
    String expectedValue = "0adb82835139278edb78d1e8c4548600";

    Assertions.assertEquals(expectedValue, runtimeValue);
  }

  /** Test case: tests the md5Hex method. Test data: an empty password. Expected: an empty string. */
  @Test
  public void testMd5HexEmpty() {
    String password = "";
    String runtimeValue = DigestUtils.md5Hex(password);
    String expectedValue = "";

    Assertions.assertEquals(expectedValue, runtimeValue);
  }
}
