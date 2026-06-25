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
package org.verwandlung.voj.web.mapper;

import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

import org.verwandlung.voj.web.model.UserGroup;

/**
 * The test class for UserGroupMapper.
 *
 * @author Haozhe Xie
 */
@ExtendWith(SpringExtension.class)
@Transactional
@ContextConfiguration({"classpath:test-spring-context.xml"})
public class UserGroupMapperTest {
  /** Test case: tests the getUserGroups() method. Test data: N/a. Expected: all user group objects (4 user group objects). */
  public void testGetUserGroups() {
    List<UserGroup> userGroups = userGroupMapper.getUserGroups();
    Assertions.assertEquals(4, userGroups.size());

    UserGroup firstUserGroup = userGroups.get(0);
    String firstUserGroupSlug = firstUserGroup.getUserGroupSlug();
    Assertions.assertEquals("forbidden", firstUserGroupSlug);
  }

  /**
   * Test case: tests the getUserGroupUsingId(int) method. Test data: the unique identifier of a
   * normal user group (UserGroup). Expected: the corresponding user group (UserGroup) object.
   */
  @Test
  public void testGetUserGroupUsingIdExists() {
    UserGroup userGroup = userGroupMapper.getUserGroupUsingId(2);
    Assertions.assertNotNull(userGroup);

    String userGroupSlug = userGroup.getUserGroupSlug();
    Assertions.assertEquals("users", userGroupSlug);
  }

  /** Test case: tests the getUserGroupUsingId(int) method. Test data: a non-existing user group unique identifier. Expected: a null reference. */
  @Test
  public void testGetUserGroupUsingIdNotExists() {
    UserGroup userGroup = userGroupMapper.getUserGroupUsingId(0);
    Assertions.assertNull(userGroup);
  }

  /**
   * Test case: tests the getUserGroupUsingSlug(String) method. Test data: the slug of a normal user
   * group (UserGroup). Expected: the corresponding user group (UserGroup) object.
   */
  @Test
  public void testGetUserGroupUsingSlugExists() {
    UserGroup userGroup = userGroupMapper.getUserGroupUsingSlug("users");
    Assertions.assertNotNull(userGroup);

    int userGroupId = userGroup.getUserGroupId();
    Assertions.assertEquals(2, userGroupId);
  }

  /** Test case: tests the getUserGroupUsingSlug(String) method. Test data: a non-existing user group slug. Expected: a null reference. */
  @Test
  public void testGetUserGroupUsingSlugNotExists() {
    UserGroup userGroup = userGroupMapper.getUserGroupUsingSlug("Not-Exists");
    Assertions.assertNull(userGroup);
  }

  /** The UserGroupMapper object under test. */
  @Autowired private UserGroupMapper userGroupMapper;
}
