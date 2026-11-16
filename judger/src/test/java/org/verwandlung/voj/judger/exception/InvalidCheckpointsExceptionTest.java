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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Test class for InvalidCheckpointsException.
 *
 * @author Haozhe Xie
 */
public class InvalidCheckpointsExceptionTest {
  /**
   * Test case: tests the constructor. Expected result: the exception carries the supplied message
   * and cause, and is an IOException.
   */
  @Test
  public void testConstructor() {
    Exception cause = new Exception("cause");
    InvalidCheckpointsException exception = new InvalidCheckpointsException("HTTP 404", cause);
    Assertions.assertEquals("HTTP 404", exception.getMessage());
    Assertions.assertSame(cause, exception.getCause());
    Assertions.assertInstanceOf(IOException.class, exception);
  }
}
