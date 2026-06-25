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
package org.verwandlung.voj.web.model;

import java.util.Collection;
import java.util.List;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * Adapts a {@link User} to Spring Security's {@link UserDetails} so that the authenticated principal
 * carries the full user object. The granted authority is derived from the user group slug as {@code
 * ROLE_<SLUG>} (e.g. the {@code administrators} group maps to {@code ROLE_ADMINISTRATORS}).
 *
 * @author Haozhe Xie
 */
public class VojUserDetails implements UserDetails {
  /**
   * Wraps the given user.
   *
   * @param user - the authenticated user
   */
  public VojUserDetails(User user) {
    this.user = user;
  }

  /**
   * Gets the wrapped user object.
   *
   * @return the wrapped user object
   */
  public User getUser() {
    return user;
  }

  /* (non-Javadoc)
   * @see org.springframework.security.core.userdetails.UserDetails#getAuthorities()
   */
  @Override
  public Collection<? extends GrantedAuthority> getAuthorities() {
    String role = "ROLE_" + user.getUserGroup().getUserGroupSlug().toUpperCase();
    return List.of(new SimpleGrantedAuthority(role));
  }

  /* (non-Javadoc)
   * @see org.springframework.security.core.userdetails.UserDetails#getPassword()
   */
  @Override
  public String getPassword() {
    return user.getPassword();
  }

  /* (non-Javadoc)
   * @see org.springframework.security.core.userdetails.UserDetails#getUsername()
   */
  @Override
  public String getUsername() {
    return user.getUsername();
  }

  /** The wrapped user object. */
  private final User user;

  /** The unique serialization identifier. */
  private static final long serialVersionUID = 7142139570124117910L;
}
