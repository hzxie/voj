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
 * The IO exception thrown when creating a directory fails. Thrown when java.io.File.mkdirs() returns
 * false.
 *
 * @author Haozhe Xie
 */
public class CreateDirectoryException extends Exception {
  /**
   * Constructor of the exception.
   *
   * @param message - the error message
   */
  public CreateDirectoryException(String message) {
    super(message);
  }

  /** The unique serialization identifier. */
  private static final long serialVersionUID = 7430055519184434330L;
}
