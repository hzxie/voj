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

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;

/**
 * Utility class for generating random strings.
 *
 * @author Haozhe Xie
 */
public class DigestUtils {
  /** Utility classes should not have a public constructor. */
  private DigestUtils() {}

  /**
   * Generates a GUID.
   *
   * @return a GUID
   */
  public static String getGuid() {
    return UUID.randomUUID().toString();
  }

  /**
   * Gets the MD5-encrypted password.
   *
   * @param password - the password before MD5 encryption
   * @return the MD5-encrypted password
   */
  public static String md5Hex(String password) {
    String md5 = "";
    if (password == null || password.isEmpty()) {
      return "";
    }
    try {
      MessageDigest md = MessageDigest.getInstance("MD5");
      md.update(password.getBytes());
      byte byteData[] = md.digest();

      StringBuilder sb = new StringBuilder();
      for (int i = 0; i < byteData.length; i++) {
        sb.append(Integer.toString((byteData[i] & 0xff) + 0x100, 16).substring(1));
      }
      md5 = sb.toString();
    } catch (NoSuchAlgorithmException e) {
      e.printStackTrace();
    }
    return md5;
  }

  /**
   * Generates a random string.
   *
   * @param length - the length of the string
   * @param mode - the string generation mode
   * @return a random string
   */
  public static String getRandomString(int length, Mode mode) {
    StringBuffer buffer = new StringBuffer();
    String characters = "";

    switch (mode) {
      case ALPHA:
        characters = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
        break;
      case ALPHANUMERIC:
        characters = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890";
        break;
      case NUMERIC:
        characters = "1234567890";
        break;
    }

    int charactersLength = characters.length();
    for (int i = 0; i < length; ++i) {
      double index = Math.random() * charactersLength;
      buffer.append(characters.charAt((int) index));
    }
    return buffer.toString();
  }

  /** Enumeration of string generation modes. */
  public static enum Mode {
    ALPHA,
    ALPHANUMERIC,
    NUMERIC
  }
}
