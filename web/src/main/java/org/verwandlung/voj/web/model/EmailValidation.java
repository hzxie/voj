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
import java.util.Date;

/**
 * The email validation model.
 *
 * @author Haozhe Xie
 */
public class EmailValidation implements Serializable {
  /** Default constructor of the EmailValidation class. */
  public EmailValidation() {}

  /**
   * Constructor of the EmailValidation class.
   *
   * @param email - the email address to be validated
   * @param token - the token used for validation
   * @param expireTime - the expiration time of the token
   */
  public EmailValidation(String email, String token, Date expireTime) {
    this.email = email;
    this.token = token;
    this.expireTime = expireTime;
  }

  /**
   * Gets the email address to be validated.
   *
   * @return the email address to be validated
   */
  public String getEmail() {
    return email;
  }

  /**
   * Sets the email address to be validated.
   *
   * @param email - the email address to be validated
   */
  public void setEmail(String email) {
    this.email = email;
  }

  /**
   * Gets the token used for validation.
   *
   * @return the token used for validation
   */
  public String getToken() {
    return token;
  }

  /**
   * Sets the token used for validation.
   *
   * @param token - the token used for validation
   */
  public void setToken(String token) {
    this.token = token;
  }

  /**
   * Gets the expiration time of the token.
   *
   * @return the expiration time of the token
   */
  public Date getExpireTime() {
    return expireTime;
  }

  /**
   * Sets the expiration time of the token.
   *
   * @param expireTime - the expiration time of the token
   */
  public void setExpireTime(Date expireTime) {
    this.expireTime = expireTime;
  }

  /* (non-Javadoc)
   * @see java.lang.Object#toString()
   */
  public String toString() {
    return String.format(
        "EmailValidation [Email=%s, Token=%s, ExpireTime=%s]",
        new Object[] {email, token, expireTime});
  }

  /** The email address to be validated. */
  private String email;

  /** The token used for validation. */
  private String token;

  /** The expiration time of the token. */
  private Date expireTime;

  /** The unique serialization identifier. */
  private static final long serialVersionUID = -2452767046421078474L;
}
