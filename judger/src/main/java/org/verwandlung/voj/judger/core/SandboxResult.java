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
 * What a sandbox measured of a program, and the limit it stopped the program for, if it did. The
 * verdict is not the sandbox's to decide (see {@link Verdict#of}).
 *
 * @param cpuTimeMs - the CPU time of the program, every process and thread of it included (ms)
 * @param peakMemoryKb - the peak memory of the program (KB; docs/deployment.md tells what each
 *     backend measures)
 * @param exitCode - the exit code of the program (on Linux, 128 + the signal that ended it)
 * @param stoppedFor - the limit the sandbox stopped the program for
 * @author Haozhe Xie
 */
public record SandboxResult(int cpuTimeMs, int peakMemoryKb, int exitCode, Stop stoppedFor) {
  /** The limit a sandbox stopped a program for, numbered as the native sandbox reports it. */
  public enum Stop {
    /** None: the program exited on its own. */
    NONE,

    /** The time limit, of CPU or wall-clock time. */
    TIME,

    /** The memory limit, or the memory backstop. */
    MEMORY,

    /** The output limit. */
    OUTPUT
  }
}
