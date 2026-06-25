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

import java.util.Date;
import java.util.List;

import org.apache.ibatis.annotations.Param;

import org.verwandlung.voj.web.model.User;
import org.verwandlung.voj.web.model.UserMeta;

/**
 * UserMeta Data Access Object.
 *
 * @author Haozhe Xie
 */
public interface UserMetaMapper {
  /**
   * [For administrators only] Gets the total number of users registered in the system within a time
   * period.
   *
   * @param startTime - the start time of the statistics (inclusive)
   * @param endTime - the end time of the statistics (inclusive)
   * @return the total number of users registered in the system within the time period
   */
  long getNumberOfUserRegistered(
      @Param("startTime") Date startTime, @Param("endTime") Date endTime);

  /**
   * Gets all meta information of a user.
   *
   * @param user - the user object
   * @return a list containing the meta information
   */
  List<UserMeta> getUserMetaUsingUser(@Param("user") User user);

  /**
   * Gets a specific meta information entry of a user.
   *
   * @param user - the user object
   * @param metaKey - the key of the meta information
   * @return the expected meta information object
   */
  UserMeta getUserMetaUsingUserAndMetaKey(
      @Param("user") User user, @Param("metaKey") String metaKey);

  /**
   * Inserts new user meta data.
   *
   * @param userMeta - the user meta data to insert
   */
  int createUserMeta(UserMeta userMeta);

  /**
   * Updates a user's meta data.
   *
   * @param userMeta - the meta data to update
   */
  int updateUserMeta(UserMeta userMeta);

  /**
   * Deletes all meta data of a user.
   *
   * @param uid - the unique identifier of the user
   */
  int deleteUserMetaUsingUser(@Param("uid") long uid);
}
