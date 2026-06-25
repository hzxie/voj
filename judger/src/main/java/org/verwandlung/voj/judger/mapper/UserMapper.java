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

import org.apache.ibatis.annotations.One;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.Select;

import org.verwandlung.voj.judger.model.Language;
import org.verwandlung.voj.judger.model.User;
import org.verwandlung.voj.judger.model.UserGroup;

/**
 * User Data Access Object.
 *
 * @author Haozhe Xie
 */
public interface UserMapper {
  /**
   * Gets a user object by username.
   *
   * @param username - the username
   * @return the expected user object, or a null reference
   */
  @Select("SELECT * FROM voj_users WHERE username = #{username}")
  @Options(useCache = false)
  @Results(
      value = {
        @Result(
            property = "userGroup",
            column = "user_group_id",
            javaType = UserGroup.class,
            one =
                @One(
                    select =
                        "org.verwandlung.voj.judger.mapper.UserGroupMapper.getUserGroupUsingId")),
        @Result(
            property = "preferLanguage",
            column = "prefer_language_id",
            javaType = Language.class,
            one =
                @One(
                    select = "org.verwandlung.voj.judger.mapper.LanguageMapper.getLanguageUsingId"))
      })
  User getUserUsingUsername(@Param("username") String username);
}
