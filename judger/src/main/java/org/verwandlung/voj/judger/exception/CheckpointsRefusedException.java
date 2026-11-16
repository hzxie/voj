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
package org.verwandlung.voj.judger.exception;

import java.io.IOException;

/**
 * Thrown when the web application refused a download of a problem's test data for good: it
 * answered with a client error (HTTP 4xx) other than 404 (no test data, see {@link
 * InvalidCheckpointsException}), 408 (Request Timeout) or 429 (Too Many Requests), most likely 403
 * because the judger's {@code judger.apiToken} is not the web application's. Asking again would get
 * the same answer.
 *
 * @author Haozhe Xie
 */
public class CheckpointsRefusedException extends IOException {
  /**
   * Constructor of the exception.
   *
   * @param message - the error message
   * @param statusCode - the HTTP status code of the refusal
   */
  public CheckpointsRefusedException(String message, int statusCode) {
    super(message);
    this.statusCode = statusCode;
  }

  /**
   * Gets the HTTP status code of the refusal.
   *
   * @return the status code
   */
  public int getStatusCode() {
    return statusCode;
  }

  /** The HTTP status code of the refusal. */
  private final int statusCode;

  /** The unique serialization identifier. */
  private static final long serialVersionUID = 1L;
}
