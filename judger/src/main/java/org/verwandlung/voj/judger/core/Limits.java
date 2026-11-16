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

import java.lang.management.ManagementFactory;

import com.sun.management.OperatingSystemMXBean;

/**
 * The limits of a compilation or run, which every sandbox backend enforces alike.
 *
 * @param timeLimitMs - the CPU time limit (ms, 0 means none)
 * @param memoryLimitKb - the memory limit (KB, 0 means none)
 * @param outputLimitKb - the largest file the program may write, its output included (KB)
 * @param processLimit - how many processes the program may have at once; on Linux, whose kernel
 *     counts threads as processes, its threads too
 * @author Haozhe Xie
 */
public record Limits(int timeLimitMs, int memoryLimitKb, int outputLimitKb, int processLimit) {
  /**
   * Gets the wall-clock limit, which ends a program that sleeps or blocks rather than using CPU
   * time: {@value #WALL_TIME_FACTOR} times the time limit plus {@value #WALL_TIME_SLACK_MS} ms, so
   * that a program on a busy host is not stopped short of its CPU time.
   *
   * @return the wall-clock limit (ms, 0 means none)
   */
  public int wallTimeLimitMs() {
    return saturate(wallTimeLimitMs(timeLimitMs));
  }

  /**
   * Gets the wall-clock limit of a time limit (see {@link #wallTimeLimitMs()}).
   *
   * @param timeLimitMs - the time limit (ms, 0 means none)
   * @return the wall-clock limit (ms, 0 means none)
   */
  static long wallTimeLimitMs(long timeLimitMs) {
    return timeLimitMs <= 0 ? 0 : timeLimitMs * WALL_TIME_FACTOR + WALL_TIME_SLACK_MS;
  }

  /**
   * Gets the backstop the native sandbox holds the committed memory of a process to (Linux:
   * RLIMIT_DATA, Windows: the Job's commit limit): twice the memory limit plus max(256 MB, RAM /
   * 32). The memory limit itself is measured on the peak resident set; the backstop only bounds
   * how far a program gets between two measurements, and is generous because a JVM commits far
   * more than it touches.
   *
   * @return the backstop (KB, 0 means none)
   */
  public int memoryBackstopKb() {
    if (memoryLimitKb <= 0) {
      return 0;
    }
    return saturate(2L * memoryLimitKb + Math.max(MIN_BACKSTOP_HEADROOM_KB, TOTAL_MEMORY_KB / 32));
  }

  /**
   * Caps a figure at the largest int.
   *
   * @param value - the figure
   * @return the figure, at most {@link Integer#MAX_VALUE}
   */
  private static int saturate(long value) {
    return (int) Math.min(Integer.MAX_VALUE, value);
  }

  /** The wall-clock limit is this many times the time limit, ... */
  static final int WALL_TIME_FACTOR = 2;

  /** ... plus this much (ms). */
  static final int WALL_TIME_SLACK_MS = 1000;

  /** The least headroom of the memory backstop (KB). */
  private static final long MIN_BACKSTOP_HEADROOM_KB = 256 * 1024;

  /** The host's physical memory (KB). */
  private static final long TOTAL_MEMORY_KB =
      ((OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean()).getTotalMemorySize()
          / 1024;
}
