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

/**
 * The shared publication status for content that can be published, drafted, or withdrawn (problems,
 * contests, bulletin board messages). Stored as a short string in the corresponding {@code *_status}
 * column; only {@link #PUBLISHED} content is visible to the public.
 *
 * @author Haozhe Xie
 */
public final class PublicationStatus {
  /** Visible to everyone. */
  public static final String PUBLISHED = "PUBLISHED";

  /** Not yet published, visible only to administrators. */
  public static final String DRAFT = "DRAFT";

  /** Withdrawn, hidden from everyone. */
  public static final String HIDDEN = "HIDDEN";

  /**
   * Normalizes a status string, falling back to {@link #PUBLISHED} for unknown / null values.
   *
   * @param status - the raw status string supplied by a caller
   * @return one of {@link #PUBLISHED}, {@link #DRAFT}, or {@link #HIDDEN}
   */
  public static String normalize(String status) {
    if (DRAFT.equals(status) || HIDDEN.equals(status)) {
      return status;
    }
    return PUBLISHED;
  }

  /**
   * Tests whether a status string denotes publicly visible content.
   *
   * @param status - the status string to test
   * @return {@code true} if the status is {@link #PUBLISHED}
   */
  public static boolean isPublic(String status) {
    return PUBLISHED.equals(status);
  }

  /** Utility class: no instances. */
  private PublicationStatus() {}
}
