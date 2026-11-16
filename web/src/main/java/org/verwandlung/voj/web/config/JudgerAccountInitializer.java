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
package org.verwandlung.voj.web.config;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import org.verwandlung.voj.web.service.UserService;

/**
 * Makes the account the judgers sign in as (judger.username) a judger account with the password
 * judger.password, when the application starts and that password is set: the judgers of a new
 * installation (e.g. the Docker image's, which generates the password) then need no account to be
 * created by hand. See {@link UserService#provisionJudgerAccount}.
 *
 * @author Haozhe Xie
 */
@Component
public class JudgerAccountInitializer implements ApplicationRunner {
  /* (non-Javadoc)
   * @see org.springframework.boot.ApplicationRunner#run(org.springframework.boot.ApplicationArguments)
   */
  @Override
  public void run(ApplicationArguments args) {
    if (password.isEmpty()) {
      return;
    }
    if (username.isEmpty()) {
      LOGGER.error("judger.password is set, but judger.username is empty: no judger account made.");
      return;
    }
    UserService.JudgerAccountProvision provision;
    try {
      provision = userService.provisionJudgerAccount(username, password);
    } catch (RuntimeException ex) {
      // E.g. another instance of the web application created the account at the same time.
      LOGGER.error("Failed to make the judger account " + username + ".", ex);
      return;
    }
    switch (provision) {
      case CREATED -> LOGGER.info("Created the judger account {}.", username);
      case PASSWORD_SET -> LOGGER.info("Set the password of the judger account {}.", username);
      case NOT_A_JUDGER ->
          LOGGER.error(
              "The user {} is not a judger, so the judgers cannot sign in as it: set"
                  + " judger.username (VOJ_JUDGER_USERNAME) of the web application and of the"
                  + " judgers to another name.",
              username);
      case PASSWORD_ILLEGAL ->
          LOGGER.error(
              "judger.password (VOJ_JUDGER_PASSWORD) must have 6 to 64 characters: the judger"
                  + " account {} is left as it is.",
              username);
      case UNCHANGED -> {}
    }
  }

  /** The username of the account the judgers sign in as. */
  @Value("${judger.username:}")
  private String username;

  /** The password of the account the judgers sign in as; empty to leave the accounts alone. */
  @Value("${judger.password:}")
  private String password;

  /** The autowired UserService object. */
  @Autowired private UserService userService;

  /** The logger. */
  private static final Logger LOGGER = LogManager.getLogger(JudgerAccountInitializer.class);
}
