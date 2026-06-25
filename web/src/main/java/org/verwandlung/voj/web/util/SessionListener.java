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

import jakarta.servlet.http.HttpSessionEvent;
import jakarta.servlet.http.HttpSessionListener;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * The session listener. Used to count online user data.
 *
 * @author Haozhe Xie
 */
public class SessionListener implements HttpSessionListener {
  /**
   * Gets the number of online users.
   *
   * @return the number of online users
   */
  public static long getTotalSessions() {
    return totalSessions;
  }

  /* (non-Javadoc)
   * @see jakarta.servlet.http.HttpSessionListener#sessionCreated(jakarta.servlet.http.HttpSessionEvent)
   */
  @Override
  public void sessionCreated(HttpSessionEvent se) {
    synchronized (this) {
      ++totalSessions;
    }
    LOGGER.debug("Session Created: " + se.getSession().getId());
    LOGGER.debug("Total Sessions: " + totalSessions);
  }

  /* (non-Javadoc)
   * @see jakarta.servlet.http.HttpSessionListener#sessionDestroyed(jakarta.servlet.http.HttpSessionEvent)
   */
  @Override
  public void sessionDestroyed(HttpSessionEvent se) {
    synchronized (this) {
      --totalSessions;

      if (totalSessions < 0) {
        totalSessions = 0;
      }
    }
    LOGGER.debug("Session Destroyed: " + se.getSession().getId());
    LOGGER.debug("Total Sessions: " + totalSessions);
  }

  /** The number of active sessions. */
  private static long totalSessions = 0;

  /** The logger. */
  private static final Logger LOGGER = LogManager.getLogger(SessionListener.class);
}
