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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jms.UncategorizedJmsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import org.verwandlung.voj.judger.mapper.UserMapper;
import org.verwandlung.voj.judger.messenger.MessageSender;
import org.verwandlung.voj.judger.model.Language;
import org.verwandlung.voj.judger.model.User;
import org.verwandlung.voj.judger.model.UserGroup;

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
   * Test case: tests the run method. Expected result: a KeepAlive message carrying the judger's
   * username and description is sent, without checking the identity again (that is done once, at
   * startup).
   */
  @Test
  @SuppressWarnings("unchecked")
  public void testRun() {
    applicationHeartbeat.run();

    ArgumentCaptor<Map<String, Object>> messageCaptor = ArgumentCaptor.forClass(Map.class);
    verify(messageSender).sendMessage(messageCaptor.capture());
    Map<String, Object> message = messageCaptor.getValue();
    Assertions.assertEquals("KeepAlive", message.get("event"));
    Assertions.assertEquals("judger", message.get("username"));
    Assertions.assertEquals("Test Judger", message.get("description"));
    Assertions.assertNotNull(message.get("heartbeatTime"));
    verifyNoInteractions(userMapper);
  }

  /**
   * Test case: the heartbeat runs on a fixed-rate schedule, as at startup, and a heartbeat fails
   * (e.g. the broker is restarting). Expected result: later heartbeats are still sent; an
   * exception escaping a run would cancel all of them.
   */
  @Test
  public void testHeartbeatSurvivesFailures() {
    doThrow(new UncategorizedJmsException("broker down"))
        .doNothing()
        .when(messageSender)
        .sendMessage(any());
    ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    try {
      scheduler.scheduleAtFixedRate(applicationHeartbeat, 0, 10, TimeUnit.MILLISECONDS);
      verify(messageSender, timeout(10000).atLeast(2)).sendMessage(any());
    } finally {
      scheduler.shutdownNow();
    }
  }

  /**
   * Test case: tests the isIdentityValid method. Test data: the judger's account, with the right
   * and a wrong password. Expected result: only the right password is valid.
   */
  @Test
  public void testIsIdentityValid() {
    UserGroup userGroup = new UserGroup(4, "judgers", "Judgers");
    Language language = new Language("cpp", "C++", "g++", "./a.out");
    User user = new User("judger", "{bcrypt}stored-hash", "judger@voj.org", userGroup, language);
    when(userMapper.getUserUsingUsername("judger")).thenReturn(user);
    when(passwordEncoder.matches("secret", "{bcrypt}stored-hash")).thenReturn(true);

    Assertions.assertTrue(applicationHeartbeat.isIdentityValid());
    ReflectionTestUtils.setField(applicationHeartbeat, "judgerPassword", "wrong");
    Assertions.assertFalse(applicationHeartbeat.isIdentityValid());
  }

  /** The mocked MessageSender object. */
  @Mock private MessageSender messageSender;

  /** The mocked UserMapper object. */
  @Mock private UserMapper userMapper;

  /** The mocked password encoder. */
  @Mock private PasswordEncoder passwordEncoder;

  /** The ApplicationHeartbeat object under test. */
  @InjectMocks private ApplicationHeartbeat applicationHeartbeat;
}
