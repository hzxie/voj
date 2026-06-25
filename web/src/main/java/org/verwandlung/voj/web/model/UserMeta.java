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
 * The user meta information.
 *
 * @author Haozhe Xie
 */
public class UserMeta implements Serializable {
  /** Default constructor of the UserMeta class. */
  public UserMeta() {}

  /**
   * Constructor of the UserMeta class.
   *
   * @param user - the user object
   * @param metaKey - the key of the meta information
   * @param metaValue - the value of the meta information
   */
  public UserMeta(User user, String metaKey, String metaValue) {
    this.user = user;
    this.metaKey = metaKey;
    this.metaValue = metaValue;
  }

  /**
   * Constructor of the UserMeta class.
   *
   * @param metaId - the unique identifier of the user meta information
   * @param user - the user object
   * @param metaKey - the key of the meta information
   * @param metaValue - the value of the meta information
   */
  public UserMeta(long metaId, User user, String metaKey, String metaValue) {
    this(user, metaKey, metaValue);
    this.metaId = metaId;
  }

  /**
   * Gets the unique identifier of the user meta information.
   *
   * @return the unique identifier of the user meta information
   */
  public long getMetaId() {
    return metaId;
  }

  /**
   * Sets the unique identifier of the user meta information.
   *
   * @param metaId - the unique identifier of the user meta information
   */
  public void setMetaId(long metaId) {
    this.metaId = metaId;
  }

  /**
   * Gets the user object the meta information corresponds to.
   *
   * @return the user object the meta information corresponds to
   */
  public User getUser() {
    return user;
  }

  /**
   * Sets the user object the meta information corresponds to.
   *
   * @param user - the user object the meta information corresponds to
   */
  public void setUser(User user) {
    this.user = user;
  }

  /**
   * Gets the key of the meta information.
   *
   * @return the key of the meta information
   */
  public String getMetaKey() {
    return metaKey;
  }

  /**
   * Sets the key of the meta information.
   *
   * @param metaKey - the key of the meta information
   */
  public void setMetaKey(String metaKey) {
    this.metaKey = metaKey;
  }

  /**
   * Gets the value of the meta information.
   *
   * @return the value of the meta information
   */
  public String getMetaValue() {
    return metaValue;
  }

  /**
   * Sets the value of the meta information.
   *
   * @param metaValue - the value of the meta information
   */
  public void setMetaValue(String metaValue) {
    this.metaValue = metaValue;
  }

  /* (non-Javadoc)
   * @see java.lang.Object#toString()
   */
  public String toString() {
    return String.format(
        "UserMeta: [ID=%s, User={%s}, Key=%s, Value=%s]",
        new Object[] {metaId, user, metaKey, metaValue});
  }

  /** The unique identifier of the user meta information. */
  private long metaId;

  /** The user object the meta information corresponds to. */
  private User user;

  /** The key of the meta information. */
  private String metaKey;

  /** The value of the meta information. */
  private String metaValue;

  /** The unique serialization identifier. */
  private static final long serialVersionUID = 1362748371508853051L;
}
