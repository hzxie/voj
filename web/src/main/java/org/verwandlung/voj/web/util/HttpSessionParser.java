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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import org.verwandlung.voj.web.model.User;
import org.verwandlung.voj.web.service.UserService;

/**
 * The HttpSession parser.
 *
 * @author Haozhe Xie
 */
@Component
public class HttpSessionParser {
  /**
   * The constructor of HttpSessionParser.
   *
   * @param userService - the autowired UserService object
   */
  @Autowired
  private HttpSessionParser(UserService userService) {
    HttpSessionParser.userService = userService;
  }

  /**
   * Gets the user object in the session.
   *
   * @param session - the HttpSession object
   * @return the user object in the session
   */
  public static User getCurrentUser(HttpSession session) {
    Object isLoggedInAttribute = session.getAttribute("isLoggedIn");
    Object uidAttribute = session.getAttribute("uid");
    User user = null;

    if (isLoggedInAttribute == null || uidAttribute == null) {
      return null;
    }
    boolean isLoggedIn = (Boolean) isLoggedInAttribute;
    long uid = (Long) uidAttribute;

    if (isLoggedIn) {
      user = userService.getUserUsingUid(uid);
    }
    return user;
  }

  /** The autowired UserService object. */
  private static UserService userService;
}
