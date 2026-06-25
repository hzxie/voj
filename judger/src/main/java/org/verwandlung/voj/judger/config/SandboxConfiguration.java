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
 * Selects which {@link SandboxRunner} the judger uses, based on the {@code judger.sandbox} property:
 *
 * <ul>
 *   <li>{@code native} (default): the legacy JNI {@link Runner} ({@code posix_spawn} /
 *       {@code CreateProcess}). This is the only option on Windows and is also what the test suite
 *       and CI use, since isolate needs kernel privileges that CI containers do not grant.
 *   <li>{@code isolate}: the {@link IsolateRunner} (Linux, cgroups v2 + namespaces + seccomp),
 *       recommended for production Linux deployments.
 * </ul>
 *
 * <p>The selection uses a {@code ${...}} placeholder (rather than a Spring Boot conditional) so that
 * it resolves identically under the Boot runtime and under the XML-based test context.
 *
 * @author Haozhe Xie
 */
@Configuration
public class SandboxConfiguration {
  /**
   * Chooses the active sandbox runner.
   *
   * @param sandbox - the configured sandbox ({@code native} or {@code isolate})
   * @param nativeRunner - the legacy native runner
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
