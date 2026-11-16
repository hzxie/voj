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
 * Thrown when the web application answered a download of a problem's test data, and the answer is
 * final: the problem has no test data to download (HTTP 404), or the archive is malformed. Unlike
 * other failures of a download (the web application restarting, a network failure, a timeout),
 * asking again, here or on another judger, would not help.
 *
 * @author Haozhe Xie
 */
public class InvalidCheckpointsException extends IOException {
  /**
   * Constructor of the exception.
   *
   * @param message - the error message
   * @param cause - the underlying exception, or {@code null}
   */
  public InvalidCheckpointsException(String message, Throwable cause) {
    super(message, cause);
  }

  /** The unique serialization identifier. */
  private static final long serialVersionUID = 1L;
}
