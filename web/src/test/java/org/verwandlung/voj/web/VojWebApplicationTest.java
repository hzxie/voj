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
package org.verwandlung.voj.web;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;

import org.verwandlung.voj.web.util.MailSender;

/**
 * The test class for VojWebApplication: the application as it starts in production, every bean of
 * it, against the test database (the other tests assemble only some of them, from
 * test-spring-context.xml). The application reads the database as it starts, so it provisions the
 * database first, with the scripts test-spring-context.xml runs, whichever test runs first. Its
 * context is closed after the test, so that its scheduled jobs do not change the data of the tests
 * that follow.
 *
 * @author Haozhe Xie
 */
@SpringBootTest(
    classes = VojWebApplication.class,
    properties = {
      "spring.sql.init.mode=always",
      "spring.sql.init.encoding=UTF-8",
      "spring.sql.init.schema-locations=classpath:db/schema.sql",
      "spring.sql.init.data-locations=classpath:db/seed.sql,classpath:db/test-data.sql"
    })
@TestPropertySource(locations = "classpath:voj-test.properties")
@DirtiesContext
public class VojWebApplicationTest {
  /**
   * Test case: the application starts. Expected result: every bean is created, which a cycle among
   * the beans would prevent (e.g. a service the interceptors use needing the email sender, which
   * needs the template engine, which needs a bean of the MVC configuration).
   */
  @Test
  public void testTheApplicationStarts() {
    Assertions.assertNotNull(applicationContext.getBean(MailSender.class));
  }

  /** The application context of the application. */
  @Autowired private ApplicationContext applicationContext;
}
