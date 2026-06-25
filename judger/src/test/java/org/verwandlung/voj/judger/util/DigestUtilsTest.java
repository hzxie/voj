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
package org.verwandlung.voj.judger.util;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Test class for DigestUtils.
 *
 * @author Haozhe Xie
 */
public class DigestUtilsTest {
  /**
   * Test case: tests the md5Hex method. Test data: a non-empty string. Expected result: the
   * well-known MD5 digest of the string.
   */
  @Test
  public void testMd5HexValid() {
    Assertions.assertEquals("900150983cd24fb0d6963f7d28e17f72", DigestUtils.md5Hex("abc"));
  }

  /**
   * Test case: tests the md5Hex method. Test data: the same string twice. Expected result: the
   * digest is deterministic and 32 hexadecimal characters long.
   */
  @Test
  public void testMd5HexDeterministicAndFormat() {
    String first = DigestUtils.md5Hex("verwandlung");
    String second = DigestUtils.md5Hex("verwandlung");
    Assertions.assertEquals(first, second);
    Assertions.assertEquals(32, first.length());
    Assertions.assertTrue(first.matches("[0-9a-f]{32}"));
  }

  /**
   * Test case: tests the md5Hex method. Test data: an empty string. Expected result: an empty
   * string.
   */
  @Test
  public void testMd5HexEmpty() {
    Assertions.assertEquals("", DigestUtils.md5Hex(""));
  }

  /**
   * Test case: tests the md5Hex method. Test data: a null reference. Expected result: an empty
   * string.
   */
  @Test
  public void testMd5HexNull() {
    Assertions.assertEquals("", DigestUtils.md5Hex(null));
  }

  /**
   * Test case: tests the getGuid method. Expected result: a well-formed UUID, and two consecutive
   * calls return different values.
   */
  @Test
  public void testGetGuid() {
    String guid = DigestUtils.getGuid();
    Assertions.assertNotNull(guid);
    Assertions.assertTrue(
        guid.matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}"));
    Assertions.assertNotEquals(guid, DigestUtils.getGuid());
  }

  /**
   * Test case: tests the getRandomString method. Test data: the ALPHA mode. Expected result: a
   * string of the requested length containing only letters.
   */
  @Test
  public void testGetRandomStringAlpha() {
    String value = DigestUtils.getRandomString(16, DigestUtils.Mode.ALPHA);
    Assertions.assertEquals(16, value.length());
    Assertions.assertTrue(value.matches("[a-zA-Z]+"));
  }

  /**
   * Test case: tests the getRandomString method. Test data: the ALPHANUMERIC mode. Expected result:
   * a string of the requested length containing only letters and digits.
   */
  @Test
  public void testGetRandomStringAlphanumeric() {
    String value = DigestUtils.getRandomString(24, DigestUtils.Mode.ALPHANUMERIC);
    Assertions.assertEquals(24, value.length());
    Assertions.assertTrue(value.matches("[a-zA-Z0-9]+"));
  }

  /**
   * Test case: tests the getRandomString method. Test data: the NUMERIC mode. Expected result: a
   * string of the requested length containing only digits.
   */
  @Test
  public void testGetRandomStringNumeric() {
    String value = DigestUtils.getRandomString(8, DigestUtils.Mode.NUMERIC);
    Assertions.assertEquals(8, value.length());
    Assertions.assertTrue(value.matches("[0-9]+"));
  }

  /**
   * Test case: tests the getRandomString method. Test data: a length of zero. Expected result: an
   * empty string.
   */
  @Test
  public void testGetRandomStringZeroLength() {
    Assertions.assertEquals("", DigestUtils.getRandomString(0, DigestUtils.Mode.ALPHA));
  }
}
