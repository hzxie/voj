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

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Predicate;

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
 * <p>The limiter holds at most {@code maxKeys} keys, in the order their windows began: the expired
 * ones are dropped from the front as attempts come in, and when every key is live, the one whose
 * window began first makes room for a new one. Every operation but {@link #resetIf} takes constant
 * (amortised) time, however many keys there are.
 *
 * @author Haozhe Xie
 */
public class FailureLimiter {
  /**
   * Creates a limiter.
   *
   * @param maxFailures - the number of failures after which attempts are refused
   * @param windowMillis - how long failures are counted from the first one, and attempts refused
   * @param maxKeys - the number of keys the limiter holds at most
   */
  public FailureLimiter(int maxFailures, long windowMillis, int maxKeys) {
    this.maxFailures = maxFailures;
    this.windowMillis = windowMillis;
    this.maxKeys = maxKeys;
  }

  /**
   * Takes an attempt for a key, if one is left, counting it as a failure until {@link #release} or
   * {@link #reset} is called for the key.
   *
   * @param key - what the attempts are limited for (an account, an address)
   * @return whether the attempt may be made; false if it must be refused
   */
  public synchronized boolean tryAcquire(Object key) {
    long now = currentTimeMillis();
    removeExpiredKeys(now);
    Failures entry = failures.get(key);
    if (entry == null) {
      if (failures.size() >= maxKeys) {
        Iterator<Failures> eldest = failures.values().iterator();
        eldest.next();
        eldest.remove();
      }
      failures.put(key, new Failures(now));
      return true;
    }
    if (entry.count >= maxFailures) {
      return false;
    }
    ++entry.count;
    return true;
  }

  /**
   * Takes back the attempt last acquired for a key, after it succeeded, keeping the earlier
   * failures.
   *
   * @param key - what the attempts are limited for
   */
  public synchronized void release(Object key) {
    Failures entry = failures.get(key);
    if (entry != null && --entry.count <= 0) {
      failures.remove(key);
    }
  }

  /**
   * Clears the failures of a key, after a successful attempt.
   *
   * @param key - what the attempts are limited for
   */
  public synchronized void reset(Object key) {
    failures.remove(key);
  }

  /**
   * Clears the failures of every key that matches a condition. Takes time in proportion to the
   * number of keys.
   *
   * @param condition - the keys to clear
   */
  public synchronized void resetIf(Predicate<Object> condition) {
    failures.keySet().removeIf(condition);
  }

  /**
   * Drops the keys whose windows ended, which are at the front.
   *
   * @param now - the current time, in milliseconds
   */
  private void removeExpiredKeys(long now) {
    Iterator<Failures> iterator = failures.values().iterator();
    while (iterator.hasNext() && now - iterator.next().firstFailureTime >= windowMillis) {
      iterator.remove();
    }
  }

  /**
   * Gets the time of a monotonic clock, which the windows are measured by.
   *
   * @return the time, in milliseconds
   */
  private static long currentTimeMillis() {
    return System.nanoTime() / 1_000_000;
  }

  /** The failures of a key within its window. */
  private static class Failures {
    Failures(long firstFailureTime) {
      this.firstFailureTime = firstFailureTime;
    }

    /** The time of the first failure, when the window began. */
    final long firstFailureTime;

    /** The number of failures (attempts taken and not released) so far. */
    int count = 1;
  }

  /** The number of failures after which attempts are refused. */
  private final int maxFailures;

  /** How long failures are counted from the first one. */
  private final long windowMillis;

  /** The number of keys the limiter holds at most. */
  private final int maxKeys;

  /** The failures of each key, in the order their windows began. */
  private final Map<Object, Failures> failures = new LinkedHashMap<>();
}
