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
package org.verwandlung.voj.web.controller;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.dao.DataAccessException;

/**
 * Runs the bulk actions of the administration console (deleting the selected users, problems,
 * ...).
 *
 * @author Haozhe Xie
 */
final class BulkActions {
  /** Utility class: no instances. */
  private BulkActions() {}

  /**
   * Runs a bulk administration action. Each bulk action is a single service call, i.e. a single
   * transaction, so it either applies to every selected item or - when the database rejects one of
   * them - to none; the failure is logged and reported to the console as unsuccessful instead of
   * surfacing as an error page.
   *
   * @param description - what the action does, for the log
   * @param action - the action
   * @return whether the action completed
   */
  static boolean run(String description, Runnable action) {
    try {
      action.run();
      return true;
    } catch (DataAccessException ex) {
      LOGGER.error(String.format("Failed to %s; nothing was changed.", description), ex);
      return false;
    }
  }

  /** The logger. */
  private static final Logger LOGGER = LogManager.getLogger(BulkActions.class);
}
