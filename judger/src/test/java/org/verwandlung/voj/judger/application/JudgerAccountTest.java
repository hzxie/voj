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
package org.verwandlung.voj.judger.application;

import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import org.verwandlung.voj.judger.mapper.UserMapper;
import org.verwandlung.voj.judger.model.User;
import org.verwandlung.voj.judger.model.UserGroup;

/**
 * Test class for JudgerAccount.
 *
 * @author Haozhe Xie
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class JudgerAccountTest {
  /**
   * Test case: tests the authenticate and getUid methods. Test data: the judger's account, with a
   * wrong and then the right password, and an account of another group. Expected result: only the
   * right password of a judger's account authenticates it, and its uid is known from then on.
   */
  @Test
  public void testAuthenticate() {
    User judger =
        new User(
            "judger", "{bcrypt}stored-hash", "judger@voj.org", new UserGroup(4, "judgers", "J"));
    judger.setUid(1234);
    User user =
        new User(
            "user", "{bcrypt}stored-hash", "user@voj.org", new UserGroup(2, "users", "U"));
    when(userMapper.getUserUsingUsername("judger")).thenReturn(judger);
    when(userMapper.getUserUsingUsername("user")).thenReturn(user);
    when(passwordEncoder.matches("secret", "{bcrypt}stored-hash")).thenReturn(true);
    ReflectionTestUtils.setField(judgerAccount, "username", "judger");
    ReflectionTestUtils.setField(judgerAccount, "password", "wrong");

    Assertions.assertFalse(judgerAccount.authenticate());
    Assertions.assertThrows(IllegalStateException.class, judgerAccount::getUid);
    ReflectionTestUtils.setField(judgerAccount, "username", "user");
    ReflectionTestUtils.setField(judgerAccount, "password", "secret");
    Assertions.assertFalse(judgerAccount.authenticate());
    ReflectionTestUtils.setField(judgerAccount, "username", "judger");
    Assertions.assertTrue(judgerAccount.authenticate());
    Assertions.assertEquals(1234L, judgerAccount.getUid());
  }

  /** The mocked UserMapper object. */
  @Mock private UserMapper userMapper;

  /** The mocked password encoder. */
  @Mock private PasswordEncoder passwordEncoder;

  /** The JudgerAccount object under test. */
  @InjectMocks private JudgerAccount judgerAccount;
}
