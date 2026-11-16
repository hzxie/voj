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

/**
 * A sandbox backend: carries out a compilation or run built by {@link SandboxPolicy}, enforcing its
 * limits, and measures it. There are two (see {@code SandboxConfiguration}): {@link IsolateRunner}
 * on Linux, and the native {@link Runner} on Linux and Windows. Neither decides a verdict: that is
 * {@link Verdict#of}.
 *
 * <p>Nothing a program does outlives it where a later one could use it: it may write only to its
 * work directory and the output file it is given, which the sandbox creates for the judger, never
 * in a directory the program may write to.
 *
 * @author Haozhe Xie
 */
public interface SandboxRunner {
  /**
   * Runs a compilation or a run of a submission in the sandbox.
   *
   * @param command - what to run, and how
   * @return what the sandbox measured
   * @throws IOException if the sandbox failed to run the program: never a verdict on it
   */
  SandboxResult run(SandboxCommand command) throws IOException;
}
