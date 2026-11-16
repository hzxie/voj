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
package org.verwandlung.voj.web.config;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Property;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.test.util.ReflectionTestUtils;

import org.verwandlung.voj.web.service.UserService;

/**
 * The test class for FirstAdministratorInitializer.
 *
 * @author Haozhe Xie
 */
public class FirstAdministratorInitializerTest {
  /**
   * Test case: tests run(ApplicationArguments). Test data: no administrator exists. Expected: the
   * password of the created administrator is printed on the standard output, and is in no log
   * message.
   */
  @Test
  public void testPrintsThePasswordOnTheStandardOutputOnly() throws Exception {
    UserService userService = mock(UserService.class);
    when(userService.createFirstAdministratorIfNone())
        .thenReturn(new String[] {"root", "s3cret-passw0rd"});
    FirstAdministratorInitializer initializer = new FirstAdministratorInitializer();
    ReflectionTestUtils.setField(initializer, "userService", userService);

    StringBuilder logMessages = new StringBuilder();
    AbstractAppender appender =
        new AbstractAppender("capture", null, null, true, Property.EMPTY_ARRAY) {
          @Override
          public void append(LogEvent event) {
            logMessages.append(event.getMessage().getFormattedMessage()).append('\n');
          }
        };
    appender.start();
    Logger logger = (Logger) LogManager.getLogger(FirstAdministratorInitializer.class);
    Level level = logger.getLevel();
    logger.addAppender(appender);
    logger.setLevel(Level.ALL);
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    PrintStream standardOutput = System.out;
    System.setOut(new PrintStream(output, true, StandardCharsets.UTF_8));
    try {
      initializer.run(new DefaultApplicationArguments());
    } finally {
      System.setOut(standardOutput);
      logger.removeAppender(appender);
      logger.setLevel(level);
    }

    Assertions.assertTrue(output.toString(StandardCharsets.UTF_8).contains("s3cret-passw0rd"));
    Assertions.assertTrue(logMessages.toString().contains("root"));
    Assertions.assertFalse(logMessages.toString().contains("s3cret-passw0rd"));
  }
}
