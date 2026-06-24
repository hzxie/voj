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
 * HttpSession解析器.
 *
 * @author Haozhe Xie
 */
@Component
public class HttpSessionParser {
  /**
   * HttpSessionParser的构造函数.
   *
   * @param userService - 自动注入的UserService对象
   */
  @Autowired
  private HttpSessionParser(UserService userService) {
    HttpSessionParser.userService = userService;
  }

  /**
   * 获取Session中的用户对象.
   *
   * @param session - HttpSession对象
   * @return Session中的用户对象
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

  /** 自动注入的UserService对象. */
  private static UserService userService;
}
