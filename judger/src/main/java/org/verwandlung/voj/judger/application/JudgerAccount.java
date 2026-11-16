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
package org.verwandlung.voj.judger.application;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import org.verwandlung.voj.judger.mapper.UserMapper;
import org.verwandlung.voj.judger.model.User;

/**
 * The account of this judger (judger.username, in the {@code judgers} group), which its claims and
 * heartbeats are recorded by. It is checked once, at startup (see {@link #authenticate}), before
 * the judger takes any submission.
 *
 * @author Haozhe Xie
 */
@Component
public class JudgerAccount {
  /**
   * Checks the judger's credentials against the users of the web application, and remembers the
   * uid of its account.
   *
   * @return whether the account exists, is a judger's, and the password is its own
   */
  public boolean authenticate() {
    User user = userMapper.getUserUsingUsername(username);
    if (user == null
        || !"judgers".equals(user.getUserGroup().getUserGroupSlug())
        || !passwordEncoder.matches(password, user.getPassword())) {
      return false;
    }
    uid = user.getUid();
    return true;
  }

  /**
   * Gets the uid of the judger's account.
   *
   * @return the uid
   * @throws IllegalStateException if the account was not authenticated
   */
  public long getUid() {
    Long accountUid = uid;
    if (accountUid == null) {
      throw new IllegalStateException("The judger's account was not authenticated");
    }
    return accountUid;
  }

  /** The name of the judger's account. */
  @Value("${judger.username}")
  private String username;

  /** The password of the judger's account. */
  @Value("${judger.password}")
  private String password;

  /** The autowired UserMapper object, used to look the account up. */
  @Autowired private UserMapper userMapper;

  /** The autowired password encoder, used to verify the password against the stored hash. */
  @Autowired private PasswordEncoder passwordEncoder;

  /** The uid of the account, once authenticated. */
  private volatile Long uid;
}
