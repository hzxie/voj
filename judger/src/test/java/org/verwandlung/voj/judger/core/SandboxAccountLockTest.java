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
package org.verwandlung.voj.judger.core;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Test class for SandboxAccountLock.
 *
 * @author Haozhe Xie
 */
@DisabledOnOs(OS.WINDOWS)
public class SandboxAccountLockTest {
  /**
   * Test case: two judgers on one host (two locks in one JVM here; across processes the lock file
   * does the same) with the same sandbox account, then with different ones. Expected result: the
   * second judger may not use the account the first one uses, but may use another one.
   */
  @Test
  public void testJudgersOnOneHostNeedAccountsOfTheirOwn() throws Exception {
    SandboxAccountLock first = newLock("root");
    SandboxAccountLock second = newLock("root");
    SandboxAccountLock third = newLock("nobody");

    first.acquire(tempDir);

    IOException ex = Assertions.assertThrows(IOException.class, () -> second.acquire(tempDir));
    Assertions.assertTrue(ex.getMessage().contains("system.username"), ex.getMessage());
    Assertions.assertTrue(Files.exists(tempDir.resolve("voj-sandbox-0.lock")));
    Assertions.assertDoesNotThrow(() -> third.acquire(tempDir));
  }

  /**
   * Test case: the sandbox account does not exist. Expected result: the judger does not start.
   */
  @Test
  public void testMissingAccountIsRefused() {
    SandboxAccountLock lock = newLock("voj-no-such-account");

    Assertions.assertThrows(IOException.class, () -> lock.acquire(tempDir));
  }

  /**
   * Test case: tests the uidOf method. Expected result: the uid of the account.
   */
  @Test
  public void testUidOf() throws Exception {
    Assertions.assertEquals("0", SandboxAccountLock.uidOf("root"));
  }

  /**
   * Test case: a judger that uses isolate, which gives each judger a box (and uid) of its own.
   * Expected result: no lock is taken.
   */
  @Test
  public void testIsolateNeedsNoLock() throws Exception {
    SandboxAccountLock lock = newLock("voj-no-such-account");
    ReflectionTestUtils.setField(lock, "sandbox", "isolate");

    lock.acquire();

    Assertions.assertNull(ReflectionTestUtils.getField(lock, "lock"));
  }

  /**
   * Creates the lock of a judger.
   *
   * @param sandboxUsername - the sandbox account of the judger
   * @return the lock
   */
  private static SandboxAccountLock newLock(String sandboxUsername) {
    SandboxAccountLock lock = new SandboxAccountLock();
    ReflectionTestUtils.setField(lock, "sandbox", "native");
    ReflectionTestUtils.setField(lock, "sandboxUsername", sandboxUsername);
    return lock;
  }

  /** A temporary directory managed by JUnit. */
  @TempDir Path tempDir;
}
