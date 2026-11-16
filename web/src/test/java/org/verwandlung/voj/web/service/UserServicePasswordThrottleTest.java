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
package org.verwandlung.voj.web.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import org.verwandlung.voj.web.model.User;

/**
 * Tests the throttling of the current-password checks of UserService under concurrent requests.
 *
 * <p>The test pool of the shared application context has a single database connection, which
 * serializes the transactional service calls, so this test builds a UserService of its own: a
 * wrong current password is refused before anything touches the database.
 *
 * @author Haozhe Xie
 */
public class UserServicePasswordThrottleTest {
  /** The number of concurrent requests. */
  private static final int NUMBER_OF_REQUESTS = 64;

  /**
   * Test case: tests the throttling of the current-password checks under concurrent requests. Test
   * data: 64 requests of one user, started at once, change the password with a wrong current
   * password, while every password check takes 100 ms. Expected: only 5 of them check the password
   * (and are told it is wrong); the others are refused with isPasswordCheckThrottled, and so is the
   * right password afterwards.
   */
  @Test
  public void testConcurrentWrongPasswordsAreThrottled() throws Exception {
    AtomicInteger numberOfPasswordChecks = new AtomicInteger();
    UserService userService = new UserService();
    ReflectionTestUtils.setField(
        userService, "passwordEncoder", new SlowPasswordEncoder(numberOfPasswordChecks));
    User user = new User();
    user.setUid(1000);
    user.setPassword("right-password");

    ExecutorService executor = Executors.newFixedThreadPool(NUMBER_OF_REQUESTS);
    try {
      CountDownLatch start = new CountDownLatch(1);
      List<Future<Map<String, Boolean>>> results = new ArrayList<>();
      for (int i = 0; i < NUMBER_OF_REQUESTS; ++i) {
        String password = "wrong-" + i;
        results.add(
            executor.submit(
                () -> {
                  start.await();
                  return userService.changePassword(user, password, "new-password", "new-password");
                }));
      }
      start.countDown();

      int numberOfCheckedRequests = 0;
      for (Future<Map<String, Boolean>> future : results) {
        Map<String, Boolean> result = future.get();
        Assertions.assertFalse(result.get("isOldPasswordCorrect"));
        Assertions.assertFalse(result.get("isSuccessful"));
        if (!result.get("isPasswordCheckThrottled")) {
          ++numberOfCheckedRequests;
        }
      }
      Assertions.assertEquals(5, numberOfCheckedRequests);
      Assertions.assertEquals(5, numberOfPasswordChecks.get());
    } finally {
      executor.shutdownNow();
    }

    Map<String, Boolean> result =
        userService.changePassword(user, "right-password", "new-password", "new-password");
    Assertions.assertTrue(result.get("isPasswordCheckThrottled"));
    Assertions.assertFalse(result.get("isSuccessful"));
    Assertions.assertEquals(5, numberOfPasswordChecks.get());
  }

  /** A password encoder that counts the password checks and makes each take 100 ms. */
  private static class SlowPasswordEncoder implements PasswordEncoder {
    SlowPasswordEncoder(AtomicInteger numberOfPasswordChecks) {
      this.numberOfPasswordChecks = numberOfPasswordChecks;
    }

    @Override
    public String encode(CharSequence rawPassword) {
      return rawPassword.toString();
    }

    @Override
    public boolean matches(CharSequence rawPassword, String encodedPassword) {
      numberOfPasswordChecks.incrementAndGet();
      try {
        Thread.sleep(100);
      } catch (InterruptedException ex) {
        Thread.currentThread().interrupt();
      }
      return encodedPassword.equals(rawPassword.toString());
    }

    /** The number of password checks made so far. */
    private final AtomicInteger numberOfPasswordChecks;
  }
}
