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
package org.verwandlung.voj.web.model;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * The test class for Contest.
 *
 * @author Haozhe Xie
 */
public class ContestTest {
  /**
   * Test case: tests equals(Object) and hashCode(). Test data: contests 1 and 37038, whose old hash
   * codes (id % 37 + id % 1001) collided and therefore compared equal, and two objects of contest
   * 1. Expected: only contests with the same identifier are equal, with equal hash codes.
   */
  @Test
  public void testEqualsComparesIdentifiers() {
    Contest first = contest(1);
    Contest sameAsFirst = contest(1);
    Contest collidingWithFirst = contest(37038);

    Assertions.assertEquals(first, sameAsFirst);
    Assertions.assertEquals(first.hashCode(), sameAsFirst.hashCode());
    Assertions.assertNotEquals(first, collidingWithFirst);
    Assertions.assertNotEquals(first, null);
    Assertions.assertNotEquals(first, "1");
  }

  /** Builds a contest with the given unique identifier. */
  private static Contest contest(long contestId) {
    Contest contest = new Contest();
    contest.setContestId(contestId);
    return contest;
  }
}
