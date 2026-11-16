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

import org.apache.ibatis.annotations.Param;

import org.verwandlung.voj.web.model.EmailValidation;

/**
 * EmailValidation Data Access Object.
 *
 * @author Haozhe Xie
 */
public interface EmailValidationMapper {
  /**
   * Gets the EmailValidation object corresponding to an email address.
   *
   * @param email - the email address
   * @return the corresponding EmailValidation object
   */
  EmailValidation getEmailValidation(@Param("email") String email);

  /**
   * Creates a new email validation credential.
   *
   * @param emailValidation - the email validation credential
   */
  int createEmailValidation(EmailValidation emailValidation);

  /**
   * Makes the token of an email address unusable, keeping the address's daily email counters.
   *
   * @param email - the email address
   * @param token - the token replacing the current one (a random value nobody was sent)
   * @param now - the current time, which becomes the expiration time
   * @return the number of rows updated (0 when the address has no row)
   */
  int expireEmailValidation(
      @Param("email") String email, @Param("token") String token, @Param("now") Date now);

  /**
   * Takes back one email from the daily count of an address, after the email failed to send.
   *
   * @param email - the email address
   * @return the number of rows updated
   */
  int refundDailyCount(@Param("email") String email);

  /**
   * Deletes an email validation credential.
   *
   * @param email - the email address
   */
  int deleteEmailValidation(@Param("email") String email);
}
