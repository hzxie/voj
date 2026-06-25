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

import org.apache.ibatis.annotations.Param;

import org.verwandlung.voj.web.model.UserGroup;

/**
 * UserGroup Data Access Object.
 *
 * @author Haozhe Xie
 */
public interface UserGroupMapper {
  /**
   * Gets all user group objects.
   *
   * @return a list of all user group objects
   */
  List<UserGroup> getUserGroups();

  /**
   * Gets a user group object by its unique identifier.
   *
   * @param userGroupId - the unique identifier of the user group
   * @return the expected user group object, or a null reference
   */
  UserGroup getUserGroupUsingId(@Param("userGroupId") int userGroupId);

  /**
   * Gets a user group object by its alias.
   *
   * @param userGroupSlug - the alias of the user group
   * @return the expected user group object, or a null reference
   */
  UserGroup getUserGroupUsingSlug(@Param("userGroupSlug") String userGroupSlug);
}
