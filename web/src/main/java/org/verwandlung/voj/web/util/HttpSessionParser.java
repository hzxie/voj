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

import jakarta.servlet.http.HttpSession;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import org.verwandlung.voj.web.model.User;
import org.verwandlung.voj.web.model.VojUserDetails;

/**
 * The HttpSession parser. Resolves the currently authenticated user from the Spring Security
 * context.
 *
 * @author Haozhe Xie
 */
public final class HttpSessionParser {
  /** Utility classes should not have a public constructor. */
  private HttpSessionParser() {}

  /**
   * Gets the currently authenticated user.
   *
   * <p>The {@code session} argument is retained for source compatibility with existing callers; the
   * user is now resolved from the {@link SecurityContextHolder} rather than session attributes.
   *
   * @param session - the HttpSession object (unused)
   * @return the authenticated user object, or a {@code null} reference if no user is logged in
   */
  public static User getCurrentUser(HttpSession session) {
    return getCurrentUser();
  }

  /**
   * Gets the currently authenticated user from the Spring Security context.
   *
   * @return the authenticated user object, or a {@code null} reference if no user is logged in
   */
  public static User getCurrentUser() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null) {
      return null;
    }
    Object principal = authentication.getPrincipal();
    if (principal instanceof VojUserDetails) {
      return ((VojUserDetails) principal).getUser();
    }
    return null;
  }
}
