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
 * The test class for Pagination.
 *
 * @author Haozhe Xie
 */
public class PaginationTest {
  /** Test case: tests getOffset(long, int). Test data: regular, zero, negative and huge page numbers. Expected: the offset of the page, the first page below 1, and no overflow. */
  @Test
  public void testGetOffsetOfPage() {
    Assertions.assertEquals(0, Pagination.getOffset(1, 100));
    Assertions.assertEquals(200, Pagination.getOffset(3, 100));
    Assertions.assertEquals(0, Pagination.getOffset(0, 100));
    Assertions.assertEquals(0, Pagination.getOffset(Long.MIN_VALUE, 100));
    long offset = Pagination.getOffset(Long.MAX_VALUE, Integer.MAX_VALUE);
    Assertions.assertTrue(offset > 0);
  }

  /** Test case: tests getOffset(long). Test data: positive and negative offsets. Expected: negative offsets become 0. */
  @Test
  public void testGetOffset() {
    Assertions.assertEquals(50, Pagination.getOffset(50));
    Assertions.assertEquals(0, Pagination.getOffset(-1));
    Assertions.assertEquals(0, Pagination.getOffset(Long.MIN_VALUE));
  }
}
