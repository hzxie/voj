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

import java.util.Map;

import jakarta.servlet.http.HttpSession;

/**
 * The CSRF protection service. Note: the service integrated in Spring Security is too complex, so it
 * is not used.
 *
 * @author Haozhe Xie
 */
public class CsrfProtector {
  /** Utility classes should not have a public constructor. */
  private CsrfProtector() {}

  /**
   * Generates a CSRF token, and stores it in the session for verification.
   *
   * @param session - the HttpSession object
   * @return the generated CSRF token
   */
  public static String getCsrfToken(HttpSession session) {
    String csrfToken = DigestUtils.getGuid();
    session.setAttribute("CsrfToken", csrfToken);

    return csrfToken;
  }

  /**
   * Verifies whether the CSRF token is correct.
   *
   * @param csrfToken - the CSRF token to verify
   * @param session - the HttpSession object
   * @return whether the CSRF token is correct
   */
  public static boolean isCsrfTokenValid(String csrfToken, HttpSession session) {
    Object csrfTokenAttribute = session.getAttribute("CsrfToken");
    return isCsrfTokenValid(csrfToken, csrfTokenAttribute);
  }

  /**
   * Verifies whether the CSRF token is correct.
   *
   * @param csrfToken - the CSRF token to verify
   * @param sessionAttributes - all the attributes in the session
   * @return whether the CSRF token is correct
   */
  public static boolean isCsrfTokenValid(String csrfToken, Map<String, Object> sessionAttributes) {
    Object csrfTokenAttribute = sessionAttributes.get("CsrfToken");
    return isCsrfTokenValid(csrfToken, csrfTokenAttribute);
  }

  /**
   * Verifies whether the CSRF token is correct.
   *
   * @param csrfToken - the CSRF token to verify
   * @param csrfTokenAttribute - the value of the CSRF token attribute in the session
   * @return whether the CSRF token is correct
   */
  private static boolean isCsrfTokenValid(String csrfToken, Object csrfTokenAttribute) {
    if (csrfTokenAttribute == null) {
      return false;
    }
    String generatedToken = (String) csrfTokenAttribute;

    if (!generatedToken.equals(csrfToken)) {
      return false;
    }
    return true;
  }
}
