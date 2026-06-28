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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import org.verwandlung.voj.web.model.User;
import org.verwandlung.voj.web.model.UserGroup;
import org.verwandlung.voj.web.model.VojUserDetails;

/**
 * The test class for HttpSessionParser.
 *
 * @author Haozhe Xie
 */
public class HttpSessionParserTest {
  /** Clears the security context after every test so cases do not leak authentication state into each other. */
  @AfterEach
  public void tearDown() {
    SecurityContextHolder.clearContext();
  }

  /** Test case: tests getCurrentUser(). Test data: a security context holding an authenticated VojUserDetails principal. Expected: the wrapped user object. */
  @Test
  public void testGetCurrentUserWhenAuthenticated() {
    UserGroup userGroup = new UserGroup(8, "administrators", "Administrators");
    User user = new User(1000, "zjhzxhz", "password", "cshzxie@gmail.com", userGroup, null);
    VojUserDetails userDetails = new VojUserDetails(user);
    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities()));

    User currentUser = HttpSessionParser.getCurrentUser();
    Assertions.assertNotNull(currentUser);
    Assertions.assertEquals("zjhzxhz", currentUser.getUsername());
  }

  /** Test case: tests getCurrentUser(). Test data: an empty security context (anonymous request). Expected: a null reference. */
  @Test
  public void testGetCurrentUserWhenNotAuthenticated() {
    SecurityContextHolder.clearContext();
    Assertions.assertNull(HttpSessionParser.getCurrentUser());
  }

  /** Test case: tests getCurrentUser(). Test data: a principal that is not a VojUserDetails (e.g. the anonymous "anonymousUser" string). Expected: a null reference. */
  @Test
  public void testGetCurrentUserWithNonVojPrincipal() {
    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken("anonymousUser", null, java.util.Collections.emptyList()));

    Assertions.assertNull(HttpSessionParser.getCurrentUser());
  }
}
