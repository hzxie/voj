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

/**
 * Turns the page numbers and offsets that requests carry into values a {@code LIMIT offset, n}
 * clause accepts. MySQL rejects a negative offset, and a large page number overflows when it is
 * multiplied by the page size, so without this a crafted request ends in a 500.
 *
 * @author Haozhe Xie
 */
public final class Pagination {
  /** Utility classes should not have a public constructor. */
  private Pagination() {}

  /**
   * Gets the offset of the first row of a page.
   *
   * @param pageNumber - the 1-based page number (values below 1 mean the first page)
   * @param pageSize - the number of rows per page (positive)
   * @return the offset of the first row of the page, never negative
   */
  public static long getOffset(long pageNumber, int pageSize) {
    long page = Math.min(Math.max(pageNumber, 1), Integer.MAX_VALUE);
    return (page - 1) * Math.max(pageSize, 0);
  }

  /**
   * Clamps an offset taken from a request.
   *
   * @param offset - the requested offset
   * @return the offset, or 0 when it is negative
   */
  public static long getOffset(long offset) {
    return Math.max(offset, 0);
  }
}
