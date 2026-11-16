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
 * Test class for RetryableJudgingException.
 *
 * @author Haozhe Xie
 */
public class RetryableJudgingExceptionTest {
  /**
   * Test case: tests the constructor. Expected result: the exception carries the supplied message
   * and cause, and is unchecked, so that it reaches the listener container.
   */
  @Test
  public void testConstructor() {
    Exception cause = new Exception("cause");
    RetryableJudgingException exception = new RetryableJudgingException("later", cause);
    Assertions.assertEquals("later", exception.getMessage());
    Assertions.assertSame(cause, exception.getCause());
    Assertions.assertInstanceOf(RuntimeException.class, exception);
  }
}
