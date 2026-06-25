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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Test class for CreateDirectoryException.
 *
 * @author Haozhe Xie
 */
public class CreateDirectoryExceptionTest {
  /**
   * Test case: tests the constructor. Expected result: the exception carries the supplied message
   * and is a checked exception.
   */
  @Test
  public void testConstructor() {
    CreateDirectoryException exception = new CreateDirectoryException("cannot create directory");
    Assertions.assertEquals("cannot create directory", exception.getMessage());
    Assertions.assertInstanceOf(Exception.class, exception);
  }
}
