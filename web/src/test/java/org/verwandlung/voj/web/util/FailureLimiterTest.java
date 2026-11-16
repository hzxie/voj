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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * The test class for FailureLimiter.
 *
 * @author Haozhe Xie
 */
public class FailureLimiterTest {
  /** Test case: tests tryAcquire(Object). Test data: 3 attempts allowed, 4 taken for one key, 1 for another. Expected: the fourth is refused; the other key is unaffected. */
  @Test
  public void testRefusesAttemptsOverTheLimit() {
    FailureLimiter limiter = new FailureLimiter(3, 60000);
    Assertions.assertTrue(limiter.tryAcquire("alice"));
    Assertions.assertTrue(limiter.tryAcquire("alice"));
    Assertions.assertTrue(limiter.tryAcquire("alice"));
    Assertions.assertFalse(limiter.tryAcquire("alice"));
    Assertions.assertTrue(limiter.tryAcquire("bob"));
  }

  /** Test case: tests reset(Object) and release(Object). Test data: the limit reached, then a reset; then an attempt released. Expected: a reset clears every failure; a release takes back only the last attempt. */
  @Test
  public void testResetAndRelease() {
    FailureLimiter limiter = new FailureLimiter(2, 60000);
    limiter.tryAcquire("alice");
    limiter.tryAcquire("alice");
    limiter.reset("alice");
    Assertions.assertTrue(limiter.tryAcquire("alice"));
    Assertions.assertTrue(limiter.tryAcquire("alice"));
    limiter.release("alice");
    Assertions.assertTrue(limiter.tryAcquire("alice"));
    Assertions.assertFalse(limiter.tryAcquire("alice"));
  }

  /** Test case: tests the window. Test data: a 1 ms window. Expected: the failures expire with it. */
  @Test
  public void testFailuresExpire() throws InterruptedException {
    FailureLimiter limiter = new FailureLimiter(1, 1);
    Assertions.assertTrue(limiter.tryAcquire("alice"));
    Thread.sleep(5);
    Assertions.assertTrue(limiter.tryAcquire("alice"));
  }
}
