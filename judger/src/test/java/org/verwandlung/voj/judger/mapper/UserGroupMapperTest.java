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
package org.verwandlung.voj.judger.mapper;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

import org.verwandlung.voj.judger.model.UserGroup;

/**
 * Test class for UserGroupMapper.
 *
 * @author Haozhe Xie
 */
@ExtendWith(SpringExtension.class)
@Transactional
@ContextConfiguration({"classpath:test-spring-context.xml"})
public class UserGroupMapperTest {
  /**
   * Test case: tests the getUserGroupUsingId(int) method. Test data: the unique identifier of a
   * user's user group. Expected result: returns the user's user group object.
   */
  @Test
  public void testGetUserGroupUsingIdExists() {
    UserGroup userGroup = userGroupMapper.getUserGroupUsingId(1);
    Assertions.assertNotNull(userGroup);

    String userGroupSlug = userGroup.getUserGroupSlug();
    Assertions.assertEquals("forbidden", userGroupSlug);
  }

  /**
   * Test case: tests the getUserGroupUsingId(int) method. Test data: a non-existent user group
   * identifier. Expected result: returns a null reference.
   */
  @Test
  public void testGetUserGroupUsingIdNotExists() {
    UserGroup userGroup = userGroupMapper.getUserGroupUsingId(0);
    Assertions.assertNull(userGroup);
  }

  /** The UserGroupMapper object under test. */
  @Autowired private UserGroupMapper userGroupMapper;
}
