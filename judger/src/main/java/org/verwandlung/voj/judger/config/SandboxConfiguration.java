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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import org.verwandlung.voj.judger.core.IsolateRunner;
import org.verwandlung.voj.judger.core.Runner;
import org.verwandlung.voj.judger.core.SandboxRunner;

/**
 * Selects the {@link SandboxRunner} by {@code judger.sandbox}. Both backends carry out the same
 * commands and limits (see {@code SandboxPolicy}), and the judger judges what they measure alike:
 *
 * <ul>
 *   <li>{@code isolate}: the {@link IsolateRunner} (namespaces and cgroups), recommended for
 *       Linux in production; it needs isolate and a judger running as root.
 *   <li>{@code native} (default): the JNI {@link Runner}, the backend on Windows, and on Linux for
 *       a judger that is not root (development, the tests) or a host without isolate.
 * </ul>
 *
 * @author Haozhe Xie
 */
@Configuration
public class SandboxConfiguration {
  /**
   * Chooses the active sandbox runner.
   *
   * @param sandbox - the configured sandbox ({@code native} or {@code isolate})
   * @param nativeRunner - the native runner
   * @param isolateRunner - the isolate runner
   * @return the selected sandbox runner
   */
  @Bean
  @Primary
  public SandboxRunner sandboxRunner(
      @Value("${judger.sandbox:native}") String sandbox,
      Runner nativeRunner,
      IsolateRunner isolateRunner) {
    if ("isolate".equalsIgnoreCase(sandbox)) {
      LOGGER.info("Using the isolate sandbox runner.");
      return isolateRunner;
    }
    LOGGER.info("Using the native sandbox runner.");
    return nativeRunner;
  }

  /** The logger. */
  private static final Logger LOGGER = LogManager.getLogger(SandboxConfiguration.class);
}
