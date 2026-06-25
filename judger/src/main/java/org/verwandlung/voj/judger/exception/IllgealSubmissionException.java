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
 * The illegal submission exception. Thrown when the getSubmission(long) operation returns null.
 *
 * @author Haozhe Xie
 */
public class IllgealSubmissionException extends Exception {
  /**
   * Constructor of IllgealSubmissionException.
   *
   * @param message - the error message
   */
  public IllgealSubmissionException(String message) {
    super(message);
  }

  /** The unique serialization identifier. */
  private static final long serialVersionUID = -9019235951964656553L;
}
