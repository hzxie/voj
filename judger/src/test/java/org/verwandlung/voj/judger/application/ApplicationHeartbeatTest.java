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

import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
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
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import org.verwandlung.voj.judger.core.Dispatcher;
import org.verwandlung.voj.judger.mapper.JudgerMapper;
import org.verwandlung.voj.judger.mapper.UserMapper;
import org.verwandlung.voj.judger.model.Language;
import org.verwandlung.voj.judger.model.User;
import org.verwandlung.voj.judger.model.UserGroup;

/**
 * Test class for ApplicationHeartbeat.
 *
 * @author Haozhe Xie
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class ApplicationHeartbeatTest {
  /** Injects the configuration values that would normally come from the property files. */
  @BeforeEach
  public void setUp() {
    ReflectionTestUtils.setField(applicationHeartbeat, "judgerUsername", "judger");
    ReflectionTestUtils.setField(applicationHeartbeat, "judgerPassword", "secret");
    ReflectionTestUtils.setField(applicationHeartbeat, "judgerDescription", "Test Judger");
    ReflectionTestUtils.setField(applicationHeartbeat, "judgerUid", 1001L);
    when(dispatcher.isReady()).thenReturn(true);
  }

  /**
   * Test case: tests the run method. Test data: a ready judger that supports C++ and Java.
   * Expected result: its heartbeats record, as one process, its account, description, languages
   * and readiness; the first one also removes the entries of the processes that stopped long ago.
   */
  @Test
  public void testRun() {
    applicationHeartbeat.setLanguageIds(List.of(2, 3));

    applicationHeartbeat.run();
    applicationHeartbeat.run();

    ArgumentCaptor<String> instances = ArgumentCaptor.forClass(String.class);
    verify(judgerMapper, times(2))
        .recordHeartbeat(
            instances.capture(),
            eq(1001L),
            eq("Test Judger"),
            eq("2,3"),
            eq(true),
            anyInt(),
            anyInt());
    Assertions.assertEquals(instances.getAllValues().get(0), instances.getAllValues().get(1));
    verify(judgerMapper, times(1)).deleteStoppedJudgers();
  }

  /**
   * Test case: tests the run method. Test data: a judger that supports no language, and one whose
   * dispatcher is not ready (e.g. too many of its judgings in a row failed). Expected result:
   * neither is recorded ready.
   */
  @Test
  public void testRunRecordsAJudgerThatTakesNothingAsNotReady() {
    applicationHeartbeat.run();
    applicationHeartbeat.setLanguageIds(List.of(2));
    when(dispatcher.isReady()).thenReturn(false);
    applicationHeartbeat.run();

    verify(judgerMapper)
        .recordHeartbeat(anyString(), eq(1001L), anyString(), eq(""), eq(false), anyInt(), anyInt());
    verify(judgerMapper)
        .recordHeartbeat(
            anyString(), eq(1001L), anyString(), eq("2"), eq(false), anyInt(), anyInt());
  }

  /**
   * Test case: the heartbeat runs on a schedule, as at startup, and a heartbeat fails (e.g. the
   * database is restarting). Expected result: later heartbeats are still recorded, and the first
   * one recorded removes the entries of the processes that stopped long ago; an exception escaping
   * a run would cancel all of them.
   */
  @Test
  public void testHeartbeatSurvivesFailures() {
    doThrow(new DataAccessResourceFailureException("database down"))
        .doNothing()
        .when(judgerMapper)
        .recordHeartbeat(
            anyString(), anyLong(), anyString(), anyString(), anyBoolean(), anyInt(), anyInt());
    ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    try {
      scheduler.scheduleWithFixedDelay(applicationHeartbeat, 0, 10, TimeUnit.MILLISECONDS);
      verify(judgerMapper, timeout(10000).atLeast(3))
          .recordHeartbeat(
              anyString(), anyLong(), anyString(), anyString(), anyBoolean(), anyInt(), anyInt());
    } finally {
      scheduler.shutdownNow();
    }
    verify(judgerMapper, times(2)).deleteStoppedJudgers();
  }

  /**
   * Test case: tests the isIdentityValid method. Test data: the judger's account, with the right
   * and a wrong password. Expected result: only the right password is valid, and the heartbeats
   * are recorded by the uid of the account.
   */
  @Test
  public void testIsIdentityValid() {
    UserGroup userGroup = new UserGroup(4, "judgers", "Judgers");
    Language language = new Language("cpp", "C++", "g++", "./a.out");
    User user = new User("judger", "{bcrypt}stored-hash", "judger@voj.org", userGroup, language);
    user.setUid(1234);
    when(userMapper.getUserUsingUsername("judger")).thenReturn(user);
    when(passwordEncoder.matches("secret", "{bcrypt}stored-hash")).thenReturn(true);

    Assertions.assertTrue(applicationHeartbeat.isIdentityValid());
    Assertions.assertEquals(1234L, ReflectionTestUtils.getField(applicationHeartbeat, "judgerUid"));
    ReflectionTestUtils.setField(applicationHeartbeat, "judgerPassword", "wrong");
    Assertions.assertFalse(applicationHeartbeat.isIdentityValid());
  }

  /** The mocked JudgerMapper object. */
  @Mock private JudgerMapper judgerMapper;

  /** The mocked Dispatcher object. */
  @Mock private Dispatcher dispatcher;

  /** The mocked UserMapper object. */
  @Mock private UserMapper userMapper;

  /** The mocked password encoder. */
  @Mock private PasswordEncoder passwordEncoder;

  /** The ApplicationHeartbeat object under test. */
  @InjectMocks private ApplicationHeartbeat applicationHeartbeat;
}
