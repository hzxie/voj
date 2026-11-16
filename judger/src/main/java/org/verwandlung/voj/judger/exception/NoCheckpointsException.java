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
 * Thrown when the web application says that it has no test data for a problem: it answered the
 * download with HTTP 404 and marked the answer as its own (see {@code HttpCheckpointProvider}).
 * Every other failure of a download, an unmarked 404 included (e.g. from a wrong {@code
 * judger.web.baseUrl}), is a plain {@link IOException}: the judging failed, not the problem.
 *
 * @author Haozhe Xie
 */
public class NoCheckpointsException extends IOException {
  /**
   * Constructor of the exception.
   *
   * @param message - the error message
   */
  public NoCheckpointsException(String message) {
    super(message);
  }

  /** The unique serialization identifier. */
  private static final long serialVersionUID = 1L;
}
