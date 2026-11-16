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
package org.verwandlung.voj.judger.config;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import org.verwandlung.voj.judger.core.IsolateRunner;
import org.verwandlung.voj.judger.core.Runner;
import org.verwandlung.voj.judger.util.FileSystemUtils;

/**
 * Tests {@link SandboxConfiguration}, which selects the sandbox backend.
 *
 * @author Haozhe Xie
 */
public class SandboxConfigurationTest {
  /**
   * Test case: each value of judger.sandbox. Expected result: native selects the native runner;
   * isolate selects the isolate runner for a judger running as root, and keeps any other judger
   * from starting (it could not hand the box's user its directories); any other value keeps the
   * judger from starting.
   */
  @Test
  public void testTheSandboxIsNativeOrIsolateAsRoot() {
    SandboxConfiguration configuration = new SandboxConfiguration();
    Runner nativeRunner = new Runner();
    IsolateRunner isolateRunner = new IsolateRunner();

    Assertions.assertSame(
        nativeRunner, configuration.sandboxRunner("native", nativeRunner, isolateRunner));
    if (FileSystemUtils.isRunningAsRoot()) {
      Assertions.assertSame(
          isolateRunner, configuration.sandboxRunner("isolate", nativeRunner, isolateRunner));
    } else {
      Assertions.assertThrows(
          IllegalStateException.class,
          () -> configuration.sandboxRunner("isolate", nativeRunner, isolateRunner));
    }
    Assertions.assertThrows(
        IllegalStateException.class,
        () -> configuration.sandboxRunner("nsjail", nativeRunner, isolateRunner));
  }
}
