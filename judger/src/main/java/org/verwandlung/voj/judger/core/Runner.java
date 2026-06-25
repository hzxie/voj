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
 * The legacy native {@link SandboxRunner}, backed by the JNI runner ({@code posix_spawn} on Unix,
 * {@code CreateProcess} on Windows).
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
   * Runs a command under the legacy native sandbox.
   *
   * @param commandLine - the command line to execute
   * @param inputFilePath - the file to redirect to standard input, or {@code null} for none
   * @param outputFilePath - the file to capture standard output/error into, or {@code null} for none
   * @param timeLimitMs - the time limit in milliseconds (0 means no limit)
   * @param memoryLimitKb - the memory limit in kilobytes (0 means no limit)
   * @return the structured run result; never {@code null}
   */
  @Override
  public RunResult run(
      String commandLine,
      String inputFilePath,
      String outputFilePath,
      int timeLimitMs,
      int memoryLimitKb) {
    try {
      Map<String, Object> nativeResult =
          getRuntimeResult(
              commandLine,
              systemUsername,
              systemPassword,
              inputFilePath,
              outputFilePath,
              timeLimitMs,
              memoryLimitKb);
      if (nativeResult == null) {
        return RunResult.internalError();
      }

      int exitCode = (Integer) nativeResult.get("exitCode");
      int usedTime = (Integer) nativeResult.get("usedTime");
      int usedMemory = (Integer) nativeResult.get("usedMemory");
      Verdict verdict = inferVerdict(exitCode, usedTime, usedMemory, timeLimitMs, memoryLimitKb);
      return new RunResult(verdict, usedTime, usedMemory, exitCode);
    } catch (Exception ex) {
      LOGGER.catching(ex);
      return RunResult.internalError();
    }
  }

  /**
   * Infers the verdict from the legacy runner's output. The native runner only reports an exit code
   * and the measured time/memory, so this reproduces the historical heuristic: an exit code of 0 is
   * a normal run; otherwise we attribute the failure to the limit that was reached, falling back to
   * a runtime error.
   *
   * @param exitCode - the raw exit code reported by the native runner
   * @param usedTime - the measured time used (ms)
   * @param usedMemory - the measured memory used (KB)
   * @param timeLimitMs - the time limit (ms, 0 means no limit)
   * @param memoryLimitKb - the memory limit (KB, 0 means no limit)
   * @return the inferred verdict
   */
  private Verdict inferVerdict(
      int exitCode, int usedTime, int usedMemory, int timeLimitMs, int memoryLimitKb) {
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
   * @return a Map<String, Object> object containing the program's runtime result
   */
  public native Map<String, Object> getRuntimeResult(
      String commandLine,
      String systemUsername,
      String systemPassword,
      String inputFilePath,
      String outputFilePath,
      int timeLimit,
      int memoryLimit);

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

  /** The logger. */
  private static final Logger LOGGER = LogManager.getLogger(Runner.class);

  /** Load Native Library. */
  static {
    try {
      NativeLibraryLoader.loadLibrary("JudgerCore");
    } catch (Exception ex) {
      ex.printStackTrace();
      LOGGER.catching(ex);
    }
  }
}
