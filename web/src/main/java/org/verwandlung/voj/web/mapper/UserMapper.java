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

import org.verwandlung.voj.web.model.User;
import org.verwandlung.voj.web.model.UserGroup;

/**
 * User Data Access Object.
 *
 * @author Haozhe Xie
 */
public interface UserMapper {
  /**
   * [For administrators only] Gets the total number of users in a user group in the system.
   *
   * @param userGroup - the user group object the users belong to
   * @return the total number of users in the user group in the system
   */
  long getNumberOfUsersUsingUserGroup(@Param("userGroup") UserGroup userGroup);

  /**
   * [For administrators only] Gets the total number of matching users by user group and username.
   *
   * @param userGroup - the user group object
   * @param username - part or all of the username
   * @return the total number of users in the user group whose username contains the given string
   */
  long getNumberOfUsersUsingUserGroupAndUsername(
      @Param("userGroup") UserGroup userGroup, @Param("username") String username);

  /**
   * [For administrators only] Gets the total number of users who selected a certain preferred
   * language.
   *
   * @param languageId - the unique identifier of the programming language
   * @return the total number of users who selected the preferred language
   */
  long getNumberOfUsersUsingLanguage(@Param("languageId") int languageId);

  /**
   * Gets a user object by its unique identifier.
   *
   * @param uid - the unique identifier of the user
   * @return the expected user object, or a null reference
   */
  User getUserUsingUid(@Param("uid") long uid);

  /**
   * Gets a user object by username.
   *
   * @param username - the username
   * @return the expected user object, or a null reference
   */
  User getUserUsingUsername(@Param("username") String username);

  /**
   * Gets a user object by email address.
   *
   * @param email - the email address
   * @return the expected user object, or a null reference
   */
  User getUserUsingEmail(@Param("email") String email);

  /**
   * Gets the list of users in a user group.
   *
   * @param userGroup - the user group object the users belong to
   * @param offset - the starting number of the user identifier
   * @param limit - the number of users to fetch
   * @return the list of matching users
   */
  List<User> getUserUsingUserGroup(
      @Param("userGroup") UserGroup userGroup,
      @Param("uid") long offset,
      @Param("limit") int limit);

  /**
   * [For administrators only] Filters user objects by user group and username.
   *
   * @param userGroup - the user group object
   * @param username - part or all of the username
   * @param offset - the starting number of the user identifier
   * @param limit - the number of users to fetch
   * @return the list of matching users
   */
  List<User> getUserUsingUserGroupAndUsername(
      @Param("userGroup") UserGroup userGroup,
      @Param("username") String username,
      @Param("offset") long offset,
      @Param("limit") int limit);

  /**
   * Creates a new user object.
   *
   * @param user - the user object to create
   */
  int createUser(User user);

  /**
   * Updates every column of a user (username, password, email, user group and preferred language).
   * Only for the administrator's user editor, which edits all of them: an operation that changes
   * one of them uses its own update, so that it cannot revert what another request changed in the
   * meantime (e.g. a ban or a password change).
   *
   * @param user - the user object whose information is to be updated
   */
  int updateUser(User user);

  /**
   * Updates the password (hash) of a user.
   *
   * @param uid - the unique identifier of the user
   * @param password - the new password hash
   * @return the number of rows updated
   */
  int updatePassword(@Param("uid") long uid, @Param("password") String password);

  /**
   * Updates the password (hash) of a user only while it is still a given one, i.e. unless another
   * request changed it in the meantime.
   *
   * @param uid - the unique identifier of the user
   * @param oldPassword - the password hash the user is expected to have
   * @param newPassword - the new password hash
   * @return the number of rows updated (0 when the password was changed in the meantime)
   */
  int updatePasswordIfUnchanged(
      @Param("uid") long uid,
      @Param("oldPassword") String oldPassword,
      @Param("newPassword") String newPassword);

  /**
   * Updates the email address of a user.
   *
   * @param uid - the unique identifier of the user
   * @param email - the new email address
   * @return the number of rows updated
   */
  int updateEmail(@Param("uid") long uid, @Param("email") String email);

  /**
   * Updates the user group of a user.
   *
   * @param uid - the unique identifier of the user
   * @param userGroupId - the unique identifier of the new user group
   * @return the number of rows updated
   */
  int updateUserGroup(@Param("uid") long uid, @Param("userGroupId") int userGroupId);

  /**
   * Updates the preferred programming language of a user.
   *
   * @param uid - the unique identifier of the user
   * @param languageId - the unique identifier of the language
   * @return the number of rows updated
   */
  int updatePreferLanguage(@Param("uid") long uid, @Param("languageId") int languageId);

  /**
   * Updates the email-verified flag of a user.
   *
   * @param uid - the unique identifier of the user
   * @param isEmailVerified - whether the user's email address has been verified
   */
  int updateEmailVerified(
      @Param("uid") long uid, @Param("isEmailVerified") boolean isEmailVerified);

  /**
   * Deletes a user object.
   *
   * @param uid - the unique identifier of the user to delete
   */
  int deleteUser(@Param("uid") long uid);
}
