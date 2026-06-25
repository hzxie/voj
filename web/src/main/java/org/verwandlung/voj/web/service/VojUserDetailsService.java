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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import org.verwandlung.voj.web.model.User;
import org.verwandlung.voj.web.model.VojUserDetails;

/**
 * Loads the user record for Spring Security. Looks the account up by username or email address (the
 * login form accepts either) and wraps it in a {@link VojUserDetails}.
 *
 * <p>Declaring this bean also disables Spring Boot's default in-memory user.
 *
 * @author Haozhe Xie
 */
@Service
public class VojUserDetailsService implements UserDetailsService {
  /* (non-Javadoc)
   * @see org.springframework.security.core.userdetails.UserDetailsService#loadUserByUsername(java.lang.String)
   */
  @Override
  public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
    User user = userService.getUserUsingUsernameOrEmail(username);
    if (user == null) {
      throw new UsernameNotFoundException("No user found with username or email: " + username);
    }
    return new VojUserDetails(user);
  }

  /** The autowired UserService object, used to load the user record. */
  @Autowired private UserService userService;
}
