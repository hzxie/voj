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
 * The test class for PublicationStatus.
 *
 * @author Haozhe Xie
 */
public class PublicationStatusTest {
  /** Test case: tests normalize(String). Test data: the three recognized statuses. Expected: each status is returned unchanged. */
  @Test
  public void testNormalizeKeepsRecognizedStatuses() {
    Assertions.assertEquals("PUBLISHED", PublicationStatus.normalize("PUBLISHED"));
    Assertions.assertEquals("DRAFT", PublicationStatus.normalize("DRAFT"));
    Assertions.assertEquals("HIDDEN", PublicationStatus.normalize("HIDDEN"));
  }

  /** Test case: tests normalize(String). Test data: an unknown status string. Expected: it falls back to PUBLISHED. */
  @Test
  public void testNormalizeFallsBackForUnknownStatus() {
    Assertions.assertEquals("PUBLISHED", PublicationStatus.normalize("SOMETHING_ELSE"));
  }

  /** Test case: tests normalize(String). Test data: a null status. Expected: it falls back to PUBLISHED. */
  @Test
  public void testNormalizeFallsBackForNull() {
    Assertions.assertEquals("PUBLISHED", PublicationStatus.normalize(null));
  }

  /** Test case: tests isPublic(String). Test data: the PUBLISHED status. Expected: true. */
  @Test
  public void testIsPublicForPublished() {
    Assertions.assertTrue(PublicationStatus.isPublic("PUBLISHED"));
  }

  /** Test case: tests isPublic(String). Test data: the non-public statuses and null. Expected: false for each. */
  @Test
  public void testIsPublicForNonPublic() {
    Assertions.assertFalse(PublicationStatus.isPublic("DRAFT"));
    Assertions.assertFalse(PublicationStatus.isPublic("HIDDEN"));
    Assertions.assertFalse(PublicationStatus.isPublic(null));
  }
}
