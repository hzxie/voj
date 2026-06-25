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
 * The structured result of running a single command inside a {@link SandboxRunner}.
 *
 * <p>This replaces the previous untyped {@code Map<String, Object>} contract between the runner and
 * the dispatcher. It carries an explicit {@link Verdict} (so the judging outcome no longer has to be
 * re-derived from a raw exit code) together with the measured resource usage.
 *
 * <p>Instances are immutable.
 *
 * @author Haozhe Xie
 */
public final class RunResult {
  /**
   * Creates a run result.
   *
   * @param verdict - why the process terminated
   * @param cpuTimeMs - the CPU time used by the process (ms). Implementations that cannot measure
   *     CPU time may report wall-clock time instead; this should be documented by the implementation
   * @param peakMemoryKb - the peak memory used by the process (KB)
   * @param exitCode - the raw exit code reported by the platform (for diagnostics/logging only)
   */
  public RunResult(Verdict verdict, int cpuTimeMs, int peakMemoryKb, int exitCode) {
    this.verdict = verdict;
    this.cpuTimeMs = cpuTimeMs;
    this.peakMemoryKb = peakMemoryKb;
    this.exitCode = exitCode;
  }

  /**
   * Creates a result representing a sandbox-level failure (the process could not be run or measured).
   *
   * @return a run result with verdict {@link Verdict#INTERNAL_ERROR} and zeroed measurements
   */
  public static RunResult internalError() {
    return new RunResult(Verdict.INTERNAL_ERROR, 0, 0, -1);
  }

  /**
   * Gets the reason the process terminated.
   *
   * @return the verdict
   */
  public Verdict getVerdict() {
    return verdict;
  }

  /**
   * Gets the CPU time used by the process (ms).
   *
   * @return the CPU time in milliseconds
   */
  public int getCpuTimeMs() {
    return cpuTimeMs;
  }

  /**
   * Gets the peak memory used by the process (KB).
   *
   * @return the peak memory in kilobytes
   */
  public int getPeakMemoryKb() {
    return peakMemoryKb;
  }

  /**
   * Gets the raw platform exit code (for diagnostics only).
   *
   * @return the exit code
   */
  public int getExitCode() {
    return exitCode;
  }

  /** Why the process terminated. */
  private final Verdict verdict;

  /** The CPU time used by the process (ms). */
  private final int cpuTimeMs;

  /** The peak memory used by the process (KB). */
  private final int peakMemoryKb;

  /** The raw platform exit code (diagnostics only). */
  private final int exitCode;
}
