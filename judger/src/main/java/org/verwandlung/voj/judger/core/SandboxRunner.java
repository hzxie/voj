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
 * Runs a single command under an operating-system sandbox and reports how it terminated.
 *
 * <p>This is the seam that lets each platform plug in its own isolation mechanism behind a common
 * contract:
 *
 * <ul>
 *   <li>Linux: a wrapper around <a href="https://github.com/ioi/isolate">isolate</a> (cgroups v2 +
 *       namespaces + seccomp).
 *   <li>Windows: a Job Object based runner via JNI.
 *   <li>(transitional) the legacy JNI {@code posix_spawn}/{@code CreateProcess} runner.
 * </ul>
 *
 * <p>Implementations that have first-class isolation (isolate, Job Objects) should report the
 * {@link Verdict} <em>directly</em> from the sandbox's own accounting rather than inferring it from
 * the exit code. The transitional legacy runner does not have such accounting and therefore falls
 * back to best-effort inference; that inaccuracy goes away once the legacy runner is removed.
 *
 * @author Haozhe Xie
 */
public interface SandboxRunner {
  /**
   * Runs a command under the sandbox, telling a compilation apart from a run of the submission.
   *
   * <ul>
   *   <li>A compilation ({@code isCompilation == true}) runs in the program directory, the only
   *       directory it may write to (the compiler writes the program there), and captures standard
   *       output <em>and</em> standard error into {@code outputFilePath}, so the compile log keeps
   *       the diagnostics.
   *   <li>A run of the submission ({@code isCompilation == false}) may only read the program
   *       directory: it runs in its work directory, a fresh, empty directory of its own that is the
   *       only one it may write to, and captures only standard output into {@code outputFilePath},
   *       which is compared against the expected answer; standard error must be kept out of it
   *       (sent elsewhere or discarded). So nothing a run does changes what a later run of the same
   *       program finds.
   * </ul>
   *
   * <p>The output file is never in a directory the program may write to: the sandbox creates it,
   * and the program only gets it as its standard output (or error).
   *
   * @param commandLine - the command line to execute
   * @param programDirectory - the directory of the program (its source and what the compiler made
   *     of it), or {@code null} for none
   * @param workDirectory - the directory the program runs in, its home and temporary directory and
   *     the only one it may write to: the program directory for a compilation, a fresh one for a
   *     run ({@code null} for none: the program may write nowhere)
   * @param inputFilePath - the file to redirect to standard input, or {@code null} for none
   * @param outputFilePath - the file to capture the output into, or {@code null} for none
   * @param timeLimitMs - the time limit in milliseconds (0 means no limit)
   * @param memoryLimitKb - the memory limit in kilobytes (0 means no limit)
   * @param isCompilation - whether this is a compilation (as opposed to a run of the submission)
   * @return the structured run result; never {@code null} (use {@link RunResult#internalError()} on
   *     failure)
   */
  RunResult run(
      String commandLine,
      String programDirectory,
      String workDirectory,
      String inputFilePath,
      String outputFilePath,
      int timeLimitMs,
      int memoryLimitKb,
      boolean isCompilation);
}
