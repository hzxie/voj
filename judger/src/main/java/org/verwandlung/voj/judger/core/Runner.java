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

import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import org.verwandlung.voj.judger.util.NativeLibraryLoader;

/**
 * The legacy native {@link SandboxRunner}, backed by the JNI runner ({@code fork}/{@code exec}
 * under a seccomp filter on Linux, {@code CreateProcess} on Windows).
 *
 * <p>This implementation does not have first-class resource accounting, so it <em>infers</em> the
 * {@link Verdict} from the exit code and the measured time/memory. That inference is best-effort and
 * is superseded, on platforms that have proper isolation, by {@link IsolateRunner} (Linux) and the
 * future Job Object based runner (Windows).
 *
 * <p>This class is selected as the active {@code SandboxRunner} when {@code judger.sandbox=native}
 * (the default); see {@code SandboxConfiguration}. Note that the class name must stay
 * {@code Runner} because the JNI symbol of {@link #getRuntimeResult} is derived from it.
 *
 * @author Haozhe Xie
 */
@Component
public class Runner implements SandboxRunner {
  /**
   * Runs a command under the legacy native sandbox. A compilation captures standard output and
   * standard error into {@code outputFilePath} (the compile log); a run of the submission captures
   * only standard output, and its standard error is discarded so it never reaches the judged output.
   *
   * @param commandLine - the command line to execute
   * @param inputFilePath - the file to redirect to standard input, or {@code null} for none
   * @param outputFilePath - the file to capture the output into, or {@code null} for none
   * @param timeLimitMs - the time limit in milliseconds (0 means no limit)
   * @param memoryLimitKb - the memory limit in kilobytes (0 means no limit)
   * @param isCompilation - whether this is a compilation (as opposed to a run of the submission)
   * @return the structured run result; never {@code null}
   */
  @Override
  public RunResult run(
      String commandLine,
      String inputFilePath,
      String outputFilePath,
      int timeLimitMs,
      int memoryLimitKb,
      boolean isCompilation) {
    if (LIBRARY_LOAD_FAILURE != null) {
      LOGGER.error(
          "The native sandbox library is not loaded, so {} cannot run: {}",
          commandLine,
          LIBRARY_LOAD_FAILURE.toString());
      return RunResult.internalError();
    }
    try {
      Map<String, Object> nativeResult =
          getRuntimeResult(
              commandLine,
              systemUsername,
              systemPassword,
              inputFilePath,
              outputFilePath,
              timeLimitMs,
              memoryLimitKb,
              isCompilation,
              isCompilation ? compileProcessLimit : processLimit);
      return toRunResult(nativeResult, timeLimitMs, memoryLimitKb);
    } catch (Throwable ex) {
      // The native runner throws when the sandbox cannot be set up (no sandbox user, a failed
      // privilege drop or seccomp filter, an executable that cannot be run, ...). That is a system
      // error, never a verdict on the submission, and must not escape: the caller would leave the
      // submission pending.
      LOGGER.error("The native sandbox failed to run " + commandLine, ex);
      return RunResult.internalError();
    }
  }

  /**
   * Converts the native runner's result map into a run result.
   *
   * @param nativeResult - the map returned by {@link #getRuntimeResult}
   * @param timeLimitMs - the time limit (ms, 0 means no limit)
   * @param memoryLimitKb - the memory limit (KB, 0 means no limit)
   * @return the run result ({@link Verdict#INTERNAL_ERROR} for a missing map)
   */
  static RunResult toRunResult(
      Map<String, Object> nativeResult, int timeLimitMs, int memoryLimitKb) {
    if (nativeResult == null) {
      return RunResult.internalError();
    }
    int exitCode = (Integer) nativeResult.get("exitCode");
    int usedTime = (Integer) nativeResult.get("usedTime");
    int usedMemory = (Integer) nativeResult.get("usedMemory");
    // Absent from a native library that predates it.
    Object limitExceeded = nativeResult.get("limitExceeded");
    Verdict verdict =
        inferVerdict(
            limitExceeded == null ? LIMIT_NONE : (Integer) limitExceeded,
            exitCode,
            usedTime,
            usedMemory,
            timeLimitMs,
            memoryLimitKb);
    return new RunResult(verdict, usedTime, usedMemory, exitCode);
  }

  /**
   * Infers the verdict from the native runner's output. The limit the runner reports the program
   * exceeded comes first. Then, whatever the exit code, a run that used more than a limit is never
   * a normal one: the runners poll, so a program can finish just past a limit before it is
   * stopped. Otherwise an exit code of 0 is a normal run, and a failed run is attributed to the
   * limit it reached, falling back to a runtime error.
   *
   * @param limitExceeded - the limit the native runner reports exceeded (LIMIT_*)
   * @param exitCode - the raw exit code reported by the native runner
   * @param usedTime - the measured time used (ms)
   * @param usedMemory - the measured memory used (KB)
   * @param timeLimitMs - the time limit (ms, 0 means no limit)
   * @param memoryLimitKb - the memory limit (KB, 0 means no limit)
   * @return the inferred verdict
   */
  private static Verdict inferVerdict(
      int limitExceeded,
      int exitCode,
      int usedTime,
      int usedMemory,
      int timeLimitMs,
      int memoryLimitKb) {
    switch (limitExceeded) {
      case LIMIT_TIME:
        return Verdict.TIME_LIMIT_EXCEEDED;
      case LIMIT_MEMORY:
        return Verdict.MEMORY_LIMIT_EXCEEDED;
      case LIMIT_OUTPUT:
        return Verdict.OUTPUT_LIMIT_EXCEEDED;
      default:
        break;
    }
    if (timeLimitMs != 0 && usedTime > timeLimitMs) {
      return Verdict.TIME_LIMIT_EXCEEDED;
    }
    if (memoryLimitKb != 0 && usedMemory > memoryLimitKb) {
      return Verdict.MEMORY_LIMIT_EXCEEDED;
    }
    if (exitCode == 0) {
      return Verdict.NORMAL;
    }
    if (timeLimitMs != 0 && usedTime >= timeLimitMs) {
      return Verdict.TIME_LIMIT_EXCEEDED;
    }
    if (memoryLimitKb != 0 && usedMemory >= memoryLimitKb) {
      return Verdict.MEMORY_LIMIT_EXCEEDED;
    }
    return Verdict.RUNTIME_ERROR;
  }

  /**
   * Gets the program's runtime result via JNI.
   *
   * @param commandLine - the command line of the program to execute
   * @param systemUsername - the username for logging into the operating system
   * @param systemPassword - the password for logging into the operating system
   * @param inputFilePath - the input file path (may be NULL)
   * @param outputFilePath - the output file path (may be NULL)
   * @param timeLimit - the time limit (in ms, 0 means no limit)
   * @param memoryLimit - the memory limit (in KB, 0 means no limit)
   * @param isCompilation - whether this is a compilation (stderr is then captured with stdout)
   * @param processLimit - the maximum number of processes (Linux: processes and threads) the
   *     sandbox may have at once (0 means the native default)
   * @return a Map<String, Object> object containing the program's runtime result
   */
  public native Map<String, Object> getRuntimeResult(
      String commandLine,
      String systemUsername,
      String systemPassword,
      String inputFilePath,
      String outputFilePath,
      int timeLimit,
      int memoryLimit,
      boolean isCompilation,
      int processLimit);

  /**
   * The username for logging into the operating system. For security, we recommend running the
   * judging program as a low-privilege user.
   */
  @Value("${system.username}")
  private String systemUsername;

  /**
   * The password for logging into the operating system. For security, we recommend running the
   * judging program as a low-privilege user.
   */
  @Value("${system.password}")
  private String systemPassword;

  /**
   * The maximum number of processes a submission may have at once. On Linux, where a run of the
   * submission cannot create processes, this is the sandbox user's RLIMIT_NPROC, which caps its
   * threads.
   */
  @Value("${judger.processLimit:64}")
  private int processLimit;

  /**
   * The maximum number of processes a compilation may have at once. Higher than {@link
   * #processLimit}: the compilers are trusted programs, and the Go toolchain runs one compiler per
   * CPU, each with up to a thread per CPU, which exhausts a small limit on a many-core host.
   */
  @Value("${judger.compileProcessLimit:1024}")
  private int compileProcessLimit;

  /** The "limitExceeded" values of the native result: no limit was exceeded. */
  static final int LIMIT_NONE = 0;

  /** The "limitExceeded" values of the native result: the time limit. */
  static final int LIMIT_TIME = 1;

  /** The "limitExceeded" values of the native result: the memory limit. */
  static final int LIMIT_MEMORY = 2;

  /** The "limitExceeded" values of the native result: the output limit. */
  static final int LIMIT_OUTPUT = 3;

  /** The logger. */
  private static final Logger LOGGER = LogManager.getLogger(Runner.class);

  /**
   * Why the native library could not be loaded, or {@code null} once it is. The judger still
   * starts without it (e.g. with {@code judger.sandbox=isolate}), but every native run is then a
   * system error.
   */
  private static final Throwable LIBRARY_LOAD_FAILURE = loadNativeLibrary();

  /**
   * Loads the native library.
   *
   * @return why it could not be loaded, or {@code null} if it was
   */
  private static Throwable loadNativeLibrary() {
    try {
      NativeLibraryLoader.loadLibrary("JudgerCore");
      return null;
    } catch (Throwable ex) {
      LOGGER.error("Failed to load the native sandbox library JudgerCore", ex);
      return ex;
    }
  }
}
