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
 * The limits of a compilation or run, as {@link SandboxPolicy} decides them; every sandbox backend
 * enforces them alike. Every limit is positive: nothing runs without one.
 *
 * @param timeLimitMs - the CPU time limit (ms)
 * @param wallTimeLimitMs - the wall-clock limit (ms), which ends a program that sleeps or blocks
 *     rather than using CPU time
 * @param memoryLimitKb - the memory limit (KB)
 * @param memoryBackstopKb - the committed memory the native sandbox holds a process to (KB; Linux:
 *     RLIMIT_DATA, Windows: the Job's commit limit), which bounds how far a program gets between
 *     two measurements of its memory
 * @param outputLimitKb - the largest file the program may write, its output included (KB)
 * @param processLimit - how many processes the program may have at once; on Linux, whose kernel
 *     counts threads as processes, its threads too
 * @param fileLimit - how many descriptors each process of the program may have open at once
 *     (Linux: RLIMIT_NOFILE, isolate's {@code --open-files}; not on Windows)
 * @param signalQueueLimit - how many signals and POSIX timers the program may have queued at once
 *     (the native sandbox on Linux: RLIMIT_SIGPENDING; isolate charges them to the box's memory)
 * @author Haozhe Xie
 */
public record Limits(
    int timeLimitMs,
    int wallTimeLimitMs,
    int memoryLimitKb,
    int memoryBackstopKb,
    int outputLimitKb,
    int processLimit,
    int fileLimit,
    int signalQueueLimit) {
  /**
   * Creates the limits.
   *
   * @throws IllegalArgumentException if a limit is not positive
   */
  public Limits {
    requirePositive("time limit", timeLimitMs);
    requirePositive("wall-clock limit", wallTimeLimitMs);
    requirePositive("memory limit", memoryLimitKb);
    requirePositive("memory backstop", memoryBackstopKb);
    requirePositive("output limit", outputLimitKb);
    requirePositive("process limit", processLimit);
    requirePositive("descriptor limit", fileLimit);
    requirePositive("signal queue limit", signalQueueLimit);
  }

  /**
   * Checks that a limit is positive.
   *
   * @param name - the name of the limit
   * @param value - the limit
   * @throws IllegalArgumentException if it is not
   */
  private static void requirePositive(String name, int value) {
    if (value <= 0) {
      throw new IllegalArgumentException("The " + name + " must be positive, not " + value);
    }
  }
}
