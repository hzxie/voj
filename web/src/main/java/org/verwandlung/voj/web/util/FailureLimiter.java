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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Limits failed attempts (of a password, for instance) per key: after {@code maxFailures} failures
 * within {@code windowMillis} of the first one, further attempts are refused until that window
 * ends. Kept in memory (per web application instance, and lost on restart), which is enough to
 * make guessing impractical.
 *
 * <p>An attempt is counted as a failure when it is taken, atomically with testing the limit, and
 * the count is cleared only when the attempt succeeds. Testing first and counting after a (slow)
 * password hash would let any number of concurrent attempts through.
 *
 * @author Haozhe Xie
 */
public class FailureLimiter {
  /**
   * Creates a limiter.
   *
   * @param maxFailures - the number of failures after which attempts are refused
   * @param windowMillis - how long failures are counted from the first one, and attempts refused
   */
  public FailureLimiter(int maxFailures, long windowMillis) {
    this.maxFailures = maxFailures;
    this.windowMillis = windowMillis;
  }

  /**
   * Takes an attempt for a key, if one is left, counting it as a failure until {@link #reset} is
   * called for the key.
   *
   * @param key - what the attempts are limited for (an account, an address)
   * @return whether the attempt may be made; false if it must be refused
   */
  public boolean tryAcquire(Object key) {
    long now = System.currentTimeMillis();
    if (failures.size() > MAX_KEYS) {
      failures.values().removeIf(entry -> now - entry[1] >= windowMillis);
    }
    boolean[] isAcquired = {false};
    failures.compute(
        key,
        (k, entry) -> {
          if (entry == null || now - entry[1] >= windowMillis) {
            isAcquired[0] = true;
            return new long[] {1, now};
          }
          if (entry[0] >= maxFailures) {
            return entry;
          }
          isAcquired[0] = true;
          return new long[] {entry[0] + 1, entry[1]};
        });
    return isAcquired[0];
  }

  /**
   * Takes back the attempt last acquired for a key, after it succeeded, keeping the earlier
   * failures.
   *
   * @param key - what the attempts are limited for
   */
  public void release(Object key) {
    failures.computeIfPresent(
        key, (k, entry) -> entry[0] <= 1 ? null : new long[] {entry[0] - 1, entry[1]});
  }

  /**
   * Clears the failures of a key, after a successful attempt.
   *
   * @param key - what the attempts are limited for
   */
  public void reset(Object key) {
    failures.remove(key);
  }

  /** The number of keys above which expired entries are purged. */
  private static final int MAX_KEYS = 10000;

  /** The number of failures after which attempts are refused. */
  private final int maxFailures;

  /** How long failures are counted from the first one. */
  private final long windowMillis;

  /** The failures of each key: {number of failures, time of the first one}. */
  private final Map<Object, long[]> failures = new ConcurrentHashMap<>();
}
