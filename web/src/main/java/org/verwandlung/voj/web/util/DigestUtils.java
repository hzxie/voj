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

import java.util.UUID;

/**
 * The cryptographic functions class.
 *
 * @author Haozhe Xie
 */
public class DigestUtils {
  /** Utility classes should not have a public constructor. */
  private DigestUtils() {}

  /**
   * Generates a GUID.
   *
   * @return the GUID
   */
  public static String getGuid() {
    return UUID.randomUUID().toString();
  }
}
