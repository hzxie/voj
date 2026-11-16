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

/**
 * Thrown when a submission cannot be judged now, for a reason that is neither its fault nor its
 * problem's and is likely gone later or on another judger (e.g. its test data could not be
 * downloaded while the web application restarts). Rather than being judged a System Error, its
 * judging task is delivered again (see {@code JmsConfig#submissionTaskListenerContainer}).
 *
 * @author Haozhe Xie
 */
public class RetryableJudgingException extends RuntimeException {
  /**
   * Constructor of the exception.
   *
   * @param message - the error message
   * @param cause - the underlying exception
   */
  public RetryableJudgingException(String message, Throwable cause) {
    super(message, cause);
  }

  /** The unique serialization identifier. */
  private static final long serialVersionUID = 1L;
}
