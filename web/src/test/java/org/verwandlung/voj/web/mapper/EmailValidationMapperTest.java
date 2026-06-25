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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

import org.verwandlung.voj.web.model.EmailValidation;

import java.util.Date;

/**
 * The test class for EmailValidationMapper.
 *
 * @author Haozhe Xie
 */
@ExtendWith(SpringExtension.class)
@Transactional
@ContextConfiguration({"classpath:test-spring-context.xml"})
public class EmailValidationMapperTest {
  /** Test case: tests the getEmailValidation(String) method. Test data: an existing email address. Expected: the expected EmailValidation object. */
  @Test
  public void testGetEmailValidationExists() {
    EmailValidation emailValidation =
        emailValidationMapper.getEmailValidation("support@verwandlung.org");
    Assertions.assertNotNull(emailValidation);

    String token = emailValidation.getToken();
    Assertions.assertEquals("Random-String-Generated", token);
  }

  /** Test case: tests the getEmailValidation(String) method. Test data: a non-existing email address. Expected: a null reference. */
  @Test
  public void testGetEmailValidationNotExists() {
    EmailValidation emailValidation =
        emailValidationMapper.getEmailValidation("not-exists@verwandlung.org");
    Assertions.assertNull(emailValidation);
  }

  /**
   * Test case: tests the createEmailValidation(EmailValidation) method. Test data: a valid data set,
   * and no record exists for this email address. Expected: the data insertion operation completes
   * successfully.
   */
  @Test
  public void testCreateEmailValidationNormally() {
    EmailValidation emailValidation =
        new EmailValidation("cshzxie@gmail.com", "RandomToken", new Date());
    int numberOfRowsAffected = emailValidationMapper.createEmailValidation(emailValidation);
    Assertions.assertEquals(1, numberOfRowsAffected);

    EmailValidation insertedEmailValidation =
        emailValidationMapper.getEmailValidation("cshzxie@gmail.com");
    Assertions.assertNotNull(insertedEmailValidation);

    String token = emailValidation.getToken();
    Assertions.assertEquals("RandomToken", token);
  }

  /**
   * Test case: tests the createEmailValidation(EmailValidation) method. Test data: a valid data set,
   * but a record already exists for this email address. Expected: a DuplicateKeyException is thrown.
   */
  @Test
  public void testCreateEmailValidationUsingExistingEmail() {
    EmailValidation emailValidation =
        new EmailValidation("support@verwandlung.org", "RandomToken", new Date());
    Executable e =
        () -> {
          emailValidationMapper.createEmailValidation(emailValidation);
        };
    Assertions.assertThrows(org.springframework.dao.DuplicateKeyException.class, e);
  }

  /**
   * Test case: tests the createEmailValidation(EmailValidation) method. Test data: a valid data set,
   * but no user uses this email address (violating foreign-key referential integrity). Expected: a
   * DataIntegrityViolationException is thrown.
   */
  @Test
  public void testCreateEmailValidationUsingNotExistingEmail() {
    EmailValidation emailValidation =
        new EmailValidation("not-exists@verwandlung.org", "RandomToken", new Date());
    Executable e =
        () -> {
          emailValidationMapper.createEmailValidation(emailValidation);
        };
    Assertions.assertThrows(org.springframework.dao.DataIntegrityViolationException.class, e);
  }

  /** Test case: tests the deleteEmailValidation(String) method. Test data: an existing email address. Expected: the data deletion operation completes successfully. */
  @Test
  public void testDeleteEmailValidationExists() {
    EmailValidation emailValidation =
        emailValidationMapper.getEmailValidation("support@verwandlung.org");
    Assertions.assertNotNull(emailValidation);

    int numberOfRowsAffected =
        emailValidationMapper.deleteEmailValidation("support@verwandlung.org");
    Assertions.assertEquals(1, numberOfRowsAffected);

    emailValidation = emailValidationMapper.getEmailValidation("support@verwandlung.org");
    Assertions.assertNull(emailValidation);
  }

  /** Test case: tests the deleteEmailValidation(String) method. Test data: a non-existing email address. Expected: the method executes normally without affecting the data in the table. */
  @Test
  public void testDeleteEmailValidationNotExists() {
    int numberOfRowsAffected =
        emailValidationMapper.deleteEmailValidation("not-exist@verwandlung.org");
    Assertions.assertEquals(0, numberOfRowsAffected);
  }

  /** The EmailValidationMapper object under test. */
  @Autowired private EmailValidationMapper emailValidationMapper;
}
