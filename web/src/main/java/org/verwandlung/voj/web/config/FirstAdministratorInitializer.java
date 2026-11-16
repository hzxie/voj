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
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import org.verwandlung.voj.web.service.UserService;

/**
 * Creates the first administrator of a new installation when the application starts and no
 * administrator exists, and logs its random password once: there is no default account to guess.
 *
 * @author Haozhe Xie
 */
@Component
public class FirstAdministratorInitializer implements ApplicationRunner {
  /* (non-Javadoc)
   * @see org.springframework.boot.ApplicationRunner#run(org.springframework.boot.ApplicationArguments)
   */
  @Override
  public void run(ApplicationArguments args) {
    String[] credentials = userService.createFirstAdministratorIfNone();
    if (credentials != null) {
      LOGGER.warn(
          "No administrator existed, so one was created: username '{}', password '{}'. Sign in and"
              + " change the password; it is not shown again.",
          credentials[0],
          credentials[1]);
    }
  }

  /** The autowired UserService object. */
  @Autowired private UserService userService;

  /** The logger. */
  private static final Logger LOGGER = LogManager.getLogger(FirstAdministratorInitializer.class);
}
