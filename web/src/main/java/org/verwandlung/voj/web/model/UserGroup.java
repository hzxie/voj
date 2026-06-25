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

import java.io.Serializable;

/**
 * The model of a user group. Maps to the voj_user_groups table in the database.
 *
 * @author Haozhe Xie
 */
public class UserGroup implements Serializable {
  /** Default constructor of the user group. */
  public UserGroup() {}

  /**
   * Constructor of the user group.
   *
   * @param userGroupId - the unique identifier of the user group
   * @param userGroupSlug - the alias of the user group
   * @param userGroupName - the name of the user group
   */
  public UserGroup(int userGroupId, String userGroupSlug, String userGroupName) {
    this.userGroupId = userGroupId;
    this.userGroupSlug = userGroupSlug;
    this.userGroupName = userGroupName;
  }

  /**
   * Gets the unique identifier of the user group.
   *
   * @return the unique identifier of the user group
   */
  public int getUserGroupId() {
    return userGroupId;
  }

  /**
   * Sets the unique identifier of the user group.
   *
   * @param userGroupId - the unique identifier of the user group
   */
  public void setUserGroupId(int userGroupId) {
    this.userGroupId = userGroupId;
  }

  /**
   * Gets the alias of the user group.
   *
   * @return the alias of the user group
   */
  public String getUserGroupSlug() {
    return userGroupSlug;
  }

  /**
   * Sets the alias of the user group.
   *
   * @param userGroupSlug - the alias of the user group
   */
  public void setUserGroupSlug(String userGroupSlug) {
    this.userGroupSlug = userGroupSlug;
  }

  /**
   * Gets the name of the user group.
   *
   * @return the name of the user group
   */
  public String getUserGroupName() {
    return userGroupName;
  }

  /**
   * Sets the name of the user group.
   *
   * @param userGroupName - the name of the user group
   */
  public void setUserGroupName(String userGroupName) {
    this.userGroupName = userGroupName;
  }

  /* (non-Javadoc)
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    return String.format(
        "UserGroup [ID=%d, Slug=%s, Name=%s]",
        new Object[] {userGroupId, userGroupSlug, userGroupName});
  }

  /** The unique identifier of the user group. */
  private int userGroupId;

  /** The alias of the user group. */
  private String userGroupSlug;

  /** The name of the user group. */
  private String userGroupName;

  /** The unique serialization identifier. */
  private static final long serialVersionUID = -4267702283058634035L;
}
