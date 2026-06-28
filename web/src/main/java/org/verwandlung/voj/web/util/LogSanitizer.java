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

/**
 * Neutralizes user-controlled values before they are written to the application log, preventing log
 * forging (CWE-117) by stripping the carriage-return and line-feed characters an attacker could use
 * to inject forged log records.
 *
 * @author Haozhe Xie
 */
public class LogSanitizer {
  /** Utility classes should not have a public constructor. */
  private LogSanitizer() {}

  /**
   * Returns a single-line, log-safe representation of the given value with all line breaks replaced
   * by underscores.
   *
   * @param value - the (possibly user-controlled) value to be logged
   * @return a representation of the value that is safe to embed in a log record
   */
  public static String forLog(Object value) {
    if (value == null) {
      return "null";
    }
    return value.toString().replaceAll("[\r\n]", "_");
  }
}
