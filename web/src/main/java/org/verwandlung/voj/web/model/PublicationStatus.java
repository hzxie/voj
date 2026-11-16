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
 * The publication status of problems, contests and bulletin board messages, stored as a short string
 * in their {@code *_status} column. Only {@link #PUBLISHED} content is public; {@link #DRAFT} and
 * {@link #HIDDEN} content is visible to administrators only. The two behave the same and differ only
 * in the label the administration console shows (not published yet / withdrawn).
 *
 * @author Haozhe Xie
 */
public final class PublicationStatus {
  /** Visible to everyone. */
  public static final String PUBLISHED = "PUBLISHED";

  /** Not published yet: visible to administrators only. */
  public static final String DRAFT = "DRAFT";

  /** Withdrawn after publication: visible to administrators only. */
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

  /**
   * Tests whether content with the given status is visible to a user: published content is visible
   * to everyone, while unpublished (draft / hidden) content is visible to administrators only.
   *
   * @param status - the status string of the content
   * @param user - the user viewing the content (may be null for anonymous visitors)
   * @return {@code true} if the user may see the content
   */
  public static boolean isVisibleTo(String status, User user) {
    return isPublic(status) || isAdministrator(user);
  }

  /**
   * Tests whether a user belongs to the administrators user group.
   *
   * @param user - the user to test (may be null)
   * @return {@code true} if the user is an administrator
   */
  public static boolean isAdministrator(User user) {
    return user != null
        && user.getUserGroup() != null
        && "administrators".equals(user.getUserGroup().getUserGroupSlug());
  }

  /** Utility class: no instances. */
  private PublicationStatus() {}
}
