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

/**
 * The reason a sandboxed process terminated, as reported (or inferred) by a {@link SandboxRunner}.
 *
 * <p>This is intentionally a small, platform-neutral vocabulary: every {@code SandboxRunner}
 * implementation (isolate on Linux, Job Objects on Windows, or the legacy JNI runner) is responsible
 * for translating its platform-specific exit information into one of these values, so that the
 * judging verdict no longer depends on guessing from a raw process exit code.
 *
 * <p>Note that {@link #NORMAL} only means "the process finished within its limits". Whether the run
 * is actually <em>Accepted</em> is decided later by comparing the program output against the
 * expected output (see {@code Comparator}); a {@code NORMAL} run with mismatching output becomes a
 * Wrong Answer.
 *
 * @author Haozhe Xie
 */
public enum Verdict {
  /** The process exited normally and stayed within the time and memory limits. */
  NORMAL,

  /** The process exceeded the time limit and was killed. */
  TIME_LIMIT_EXCEEDED,

  /** The process exceeded the memory limit and was killed. */
  MEMORY_LIMIT_EXCEEDED,

  /** The process exited abnormally (non-zero exit code or terminated by a signal). */
  RUNTIME_ERROR,

  /** The sandbox itself failed to run the process (e.g. it could not be launched). */
  INTERNAL_ERROR;
}
