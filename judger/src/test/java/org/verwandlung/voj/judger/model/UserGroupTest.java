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
package org.verwandlung.voj.judger.model;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Test class for the UserGroup model.
 *
 * @author Haozhe Xie
 */
public class UserGroupTest {
  /**
   * Test case: tests the parameterized constructor and getters. Expected result: the getters return
   * the values passed to the constructor.
   */
  @Test
  public void testConstructorAndGetters() {
    UserGroup userGroup = new UserGroup(4, "judgers", "Judgers");
    Assertions.assertEquals(4, userGroup.getUserGroupId());
    Assertions.assertEquals("judgers", userGroup.getUserGroupSlug());
    Assertions.assertEquals("Judgers", userGroup.getUserGroupName());
  }

  /**
   * Test case: tests the default constructor together with the setters. Expected result: the
   * getters return the values set by the setters.
   */
  @Test
  public void testSetters() {
    UserGroup userGroup = new UserGroup();
    userGroup.setUserGroupId(1);
    userGroup.setUserGroupSlug("users");
    userGroup.setUserGroupName("Users");

    Assertions.assertEquals(1, userGroup.getUserGroupId());
    Assertions.assertEquals("users", userGroup.getUserGroupSlug());
    Assertions.assertEquals("Users", userGroup.getUserGroupName());
  }

  /** Test case: tests toString. Expected result: a string containing the slug. */
  @Test
  public void testToString() {
    UserGroup userGroup = new UserGroup(4, "judgers", "Judgers");
    Assertions.assertTrue(userGroup.toString().contains("judgers"));
  }
}
