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

import jakarta.servlet.http.HttpServletRequest;

/**
 * The HTTP request header parsing service.
 *
 * @author Haozhe Xie
 */
public class HttpRequestParser {
  /** Utility classes should not have a public constructor. */
  private HttpRequestParser() {}

  /**
   * Parses the user's real IP when a reverse proxy is used.
   *
   * @param request - the HttpRequest object
   * @return the user's real IP
   */
  public static String getRemoteAddr(HttpServletRequest request) {
    if (request.getHeader("X-Real-IP") != null) {
      return request.getHeader("X-Real-IP");
    }
    return request.getRemoteAddr();
  }
}
