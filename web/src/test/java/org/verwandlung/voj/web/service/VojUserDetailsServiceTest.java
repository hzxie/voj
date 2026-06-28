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
package org.verwandlung.voj.web.service;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.test.util.ReflectionTestUtils;

import org.verwandlung.voj.web.model.User;
import org.verwandlung.voj.web.model.UserGroup;

/**
 * The test class for VojUserDetailsService.
 *
 * @author Haozhe Xie
 */
public class VojUserDetailsServiceTest {
  /** Wires a mocked UserService into a fresh service before every test. */
  @BeforeEach
  public void setUp() {
    userService = mock(UserService.class);
    vojUserDetailsService = new VojUserDetailsService();
    ReflectionTestUtils.setField(vojUserDetailsService, "userService", userService);
  }

  /** Test case: tests loadUserByUsername(String). Test data: a username matching an existing user. Expected: a VojUserDetails wrapping that user. */
  @Test
  public void testLoadUserByUsernameExists() {
    UserGroup userGroup = new UserGroup(8, "administrators", "Administrators");
    User user = new User(1000, "zjhzxhz", "password", "cshzxie@gmail.com", userGroup, null);
    when(userService.getUserUsingUsernameOrEmail("zjhzxhz")).thenReturn(user);

    UserDetails userDetails = vojUserDetailsService.loadUserByUsername("zjhzxhz");
    Assertions.assertNotNull(userDetails);
    Assertions.assertEquals("zjhzxhz", userDetails.getUsername());
  }

  /** Test case: tests loadUserByUsername(String). Test data: a username (or email) that matches no user. Expected: a UsernameNotFoundException is thrown. */
  @Test
  public void testLoadUserByUsernameNotExists() {
    when(userService.getUserUsingUsernameOrEmail("ghost")).thenReturn(null);

    Assertions.assertThrows(
        UsernameNotFoundException.class, () -> vojUserDetailsService.loadUserByUsername("ghost"));
  }

  /** The mocked UserService the service under test delegates to. */
  private UserService userService;

  /** The VojUserDetailsService object under test. */
  private VojUserDetailsService vojUserDetailsService;
}
