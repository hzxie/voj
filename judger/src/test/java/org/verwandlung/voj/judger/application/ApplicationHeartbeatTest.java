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

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import org.verwandlung.voj.judger.mapper.UserMapper;
import org.verwandlung.voj.judger.messenger.MessageSender;
import org.verwandlung.voj.judger.model.Language;
import org.verwandlung.voj.judger.model.User;
import org.verwandlung.voj.judger.model.UserGroup;
import org.verwandlung.voj.judger.util.DigestUtils;

/**
 * Test class for ApplicationHeartbeat.
 *
 * @author Haozhe Xie
 */
@ExtendWith(MockitoExtension.class)
public class ApplicationHeartbeatTest {
  /** Injects the configuration values that would normally come from the property files. */
  @BeforeEach
  public void setUp() {
    ReflectionTestUtils.setField(applicationHeartbeat, "judgerUsername", "judger");
    ReflectionTestUtils.setField(applicationHeartbeat, "judgerPassword", "secret");
    ReflectionTestUtils.setField(applicationHeartbeat, "judgerDescription", "Test Judger");
  }

  /**
   * Test case: tests the run method. Test data: a valid judger identity. Expected result: a
   * KeepAlive message carrying the judger's username and description is sent.
   */
  @Test
  @SuppressWarnings("unchecked")
  public void testRunWithValidIdentity() {
    UserGroup userGroup = new UserGroup(4, "judgers", "Judgers");
    Language language = new Language("cpp", "C++", "g++", "./a.out");
    User user =
        new User("judger", DigestUtils.md5Hex("secret"), "judger@voj.org", userGroup, language);
    when(userMapper.getUserUsingUsername("judger")).thenReturn(user);

    applicationHeartbeat.run();

    ArgumentCaptor<Map<String, Object>> messageCaptor = ArgumentCaptor.forClass(Map.class);
    verify(messageSender).sendMessage(messageCaptor.capture());
    Map<String, Object> message = messageCaptor.getValue();
    Assertions.assertEquals("KeepAlive", message.get("event"));
    Assertions.assertEquals("judger", message.get("username"));
    Assertions.assertEquals("Test Judger", message.get("description"));
    Assertions.assertNotNull(message.get("heartbeatTime"));
  }

  /** The mocked MessageSender object. */
  @Mock private MessageSender messageSender;

  /** The mocked UserMapper object. */
  @Mock private UserMapper userMapper;

  /** The ApplicationHeartbeat object under test. */
  @InjectMocks private ApplicationHeartbeat applicationHeartbeat;
}
