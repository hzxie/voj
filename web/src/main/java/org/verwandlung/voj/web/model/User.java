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

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * The model of a user. Maps to the voj_users table in the database.
 *
 * @author Haozhe Xie
 */
public class User implements Serializable {
  /** Default constructor of the user. */
  public User() {}

  /**
   * Constructor of the user.
   *
   * @param username - the username
   * @param password - the password
   * @param email - the email address
   * @param userGroup - the user group
   * @param preferLanguage - the user's preferred language
   */
  public User(
      String username,
      String password,
      String email,
      UserGroup userGroup,
      Language preferLanguage) {
    this.username = username;
    this.password = password;
    this.email = email;
    this.userGroup = userGroup;
    this.preferLanguage = preferLanguage;
  }

  /**
   * Constructor of the User class.
   *
   * @param uid - the unique identifier of the user
   * @param username - the username
   * @param password - the password
   * @param email - the email address
   * @param userGroup - the user group
   * @param preferLanguage - the user's preferred language
   */
  public User(
      int uid,
      String username,
      String password,
      String email,
      UserGroup userGroup,
      Language preferLanguage) {
    this(username, password, email, userGroup, preferLanguage);
    this.uid = uid;
  }

  /**
   * Gets the unique identifier of the user.
   *
   * @return the unique identifier of the user
   */
  public long getUid() {
    return uid;
  }

  /**
   * Sets the unique identifier of the user.
   *
   * @param uid - the unique identifier of the user
   */
  public void setUid(long uid) {
    this.uid = uid;
  }

  /**
   * Gets the username.
   *
   * @return the username
   */
  public String getUsername() {
    return username;
  }

  /**
   * Sets the username.
   *
   * @param username - the username
   */
  public void setUsername(String username) {
    this.username = username;
  }

  /**
   * Gets the password (encrypted with MD5).
   *
   * @return the password
   */
  public String getPassword() {
    return password;
  }

  /**
   * Sets the password.
   *
   * @param password - the password
   */
  public void setPassword(String password) {
    this.password = password;
  }

  /**
   * Gets the email address.
   *
   * @return the email address
   */
  public String getEmail() {
    return email;
  }

  /**
   * Sets the email address.
   *
   * @param email - the email address
   */
  public void setEmail(String email) {
    this.email = email;
  }

  /**
   * Gets the user group.
   *
   * @return the user group object
   */
  public UserGroup getUserGroup() {
    return userGroup;
  }

  /**
   * Sets the user group.
   *
   * @param userGroup - the user group object
   */
  public void setUserGroup(UserGroup userGroup) {
    this.userGroup = userGroup;
  }

  /**
   * Gets the user's preferred language object.
   *
   * @return the user's preferred language object
   */
  public Language getPreferLanguage() {
    return preferLanguage;
  }

  /**
   * Sets the user's preferred language object.
   *
   * @param preferLanguage - the user's preferred language object
   */
  public void setPreferLanguage(Language preferLanguage) {
    this.preferLanguage = preferLanguage;
  }

  /**
   * Gets whether the user's email address has been verified.
   *
   * @return whether the user's email address has been verified
   */
  public boolean isEmailVerified() {
    return isEmailVerified;
  }

  /**
   * Sets whether the user's email address has been verified.
   *
   * @param isEmailVerified - whether the user's email address has been verified
   */
  public void setEmailVerified(boolean isEmailVerified) {
    this.isEmailVerified = isEmailVerified;
  }

  /* (non-Javadoc)
   * @see java.lang.Object#hashCode()
   */
  public int hashCode() {
    return (int) uid;
  }

  /* (non-Javadoc)
   * @see java.lang.Object#equals(java.lang.Object)
   */
  public boolean equals(Object obj) {
    if (obj instanceof User) {
      User anotherUser = (User) obj;
      return anotherUser.getUid() == uid;
    }
    return false;
  }

  /* (non-Javadoc)
   * @see java.lang.Object#toString()
   */
  public String toString() {
    return String.format(
        "User: [Uid=%s, Username=%s, Email=%s, UserGroup={%s}]",
        new Object[] {uid, username, email, userGroup});
  }

  /** The unique identifier of the user. */
  private long uid;

  /** The username. */
  private String username;

  /** The password (encrypted with MD5). */
  @JsonIgnore private String password;

  /** The email address. */
  private String email;

  /** The user group object. */
  @JsonIgnore private UserGroup userGroup;

  /** The user's preferred language object. */
  @JsonIgnore private Language preferLanguage;

  /** Whether the user's email address has been verified. */
  @JsonIgnore private boolean isEmailVerified = true;

  /** The unique serialization identifier. */
  private static final long serialVersionUID = 2264535351943252507L;
}
