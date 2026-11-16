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

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.test.util.ReflectionTestUtils;

import org.verwandlung.voj.web.service.UserService;

/**
 * The test class for JudgerAccountInitializer.
 *
 * @author Haozhe Xie
 */
public class JudgerAccountInitializerTest {
  /**
   * Test case: tests run(ApplicationArguments). Test data: judger.password set, then empty.
   * Expected: the judger account is provisioned with the configured name and password only when
   * the password is set.
   */
  @Test
  public void testProvisionsTheJudgerAccountWhenAPasswordIsSet() {
    UserService userService = mock(UserService.class);
    when(userService.provisionJudgerAccount(anyString(), anyString()))
        .thenReturn(UserService.JudgerAccountProvision.CREATED);
    JudgerAccountInitializer initializer = new JudgerAccountInitializer();
    ReflectionTestUtils.setField(initializer, "userService", userService);
    ReflectionTestUtils.setField(initializer, "username", "judger");

    ReflectionTestUtils.setField(initializer, "password", "");
    initializer.run(new DefaultApplicationArguments());
    verify(userService, never()).provisionJudgerAccount(anyString(), anyString());

    ReflectionTestUtils.setField(initializer, "password", "judger-password");
    initializer.run(new DefaultApplicationArguments());
    verify(userService).provisionJudgerAccount("judger", "judger-password");
  }
}
