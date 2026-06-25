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

import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import org.verwandlung.voj.judger.model.Language;
import org.verwandlung.voj.judger.model.Submission;
import org.verwandlung.voj.judger.util.NativeLibraryLoader;

/**
 * The legacy native program runner, used to execute native applications, including the compiler
 * (gcc) and the program compiled from the code submitted by users.
 *
 * <p>This is the transitional {@link SandboxRunner} implementation backed by the existing JNI runner
 * ({@code posix_spawn} on Unix, {@code CreateProcess} on Windows). It does not have first-class
 * resource accounting, so it <em>infers</em> the {@link Verdict} from the exit code and the measured
 * time/memory. That inference is best-effort and goes away once the platform-native runners
 * (isolate on Linux, Job Objects on Windows) replace it.
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
   * Gets the runtime result of the (user's) program for a single checkpoint.
   *
   * @param submission - the submission object
   * @param workDirectory - the directory of the compilation result and the program output
   * @param baseFileName - the file name of the application to run (without file suffix)
   * @param inputFilePath - the input file path
   * @param outputFilePath - the output file path
   * @return a Map<String, Object> object containing the program's runtime result
   */
  public Map<String, Object> getRuntimeResult(
      Submission submission,
      String workDirectory,
      String baseFileName,
      String inputFilePath,
      String outputFilePath) {
    String commandLine = getCommandLine(submission, workDirectory, baseFileName);
    int timeLimit = getTimeLimit(submission);
    int memoryLimit = getMemoryLimit(submission);

    LOGGER.info(
        String.format(
            "[Submission #%d] Start running with command %s (TimeLimit=%d, MemoryLimit=%s)",
            new Object[] {submission.getSubmissionId(), commandLine, timeLimit, memoryLimit}));
    RunResult runResult = run(commandLine, inputFilePath, outputFilePath, timeLimit, memoryLimit);

    Map<String, Object> result = new HashMap<>(4, 1);
    result.put("runtimeResult", getRuntimeResultSlug(runResult.getVerdict()));
    result.put("usedTime", runResult.getCpuTimeMs());
    result.put("usedMemory", runResult.getPeakMemoryKb());
    return result;
  }

  /**
   * Gets the command line to execute.
   *
   * @param submission - the submission object
   * @param workDirectory - the directory of the compilation result and the program output
   * @param baseFileName - the file name of the application to run (without file suffix)
   * @return the command line to execute
   */
  private String getCommandLine(Submission submission, String workDirectory, String baseFileName) {
    Language language = submission.getLanguage();
    String filePathWithoutExtension =
        String.format("%s/%s", new Object[] {workDirectory, baseFileName});
    StringBuilder runCommand =
        new StringBuilder(
            language.getRunCommand().replaceAll("\\{filename\\}", filePathWithoutExtension));

    if (language.getLanguageName().equalsIgnoreCase("Java")) {
      int lastIndexOfSpace = runCommand.lastIndexOf("/");
      runCommand.setCharAt(lastIndexOfSpace, ' ');
    }
    return runCommand.toString();
  }

  /**
   * Gets the maximum time limit depending on the language.
   *
   * @param submission - the submission object
   * @return the maximum time limit
   */
  private int getTimeLimit(Submission submission) {
    Language language = submission.getLanguage();
    int timeLimit = submission.getProblem().getTimeLimit();

    if (language.getLanguageName().equalsIgnoreCase("Java")) {
      timeLimit *= 2;
    }
    return timeLimit;
  }

  /**
   * Gets the maximum memory limit depending on the language.
   *
   * @param submission - the submission object
   * @return the maximum memory limit
   */
  private int getMemoryLimit(Submission submission) {
    int memoryLimit = submission.getProblem().getMemoryLimit();
    return memoryLimit;
  }

  /**
   * Maps a {@link Verdict} to the unique English abbreviation used by the rest of the system.
   *
   * <p>{@link Verdict#NORMAL} maps to {@code "AC"}; whether the run is really Accepted is decided
   * later by the output comparison (a normal run with mismatching output becomes {@code "WA"}).
   *
   * @param verdict - the verdict to map
   * @return the unique English abbreviation of the runtime result
   */
  private String getRuntimeResultSlug(Verdict verdict) {
    switch (verdict) {
      case NORMAL:
        return "AC";
      case TIME_LIMIT_EXCEEDED:
        return "TLE";
      case MEMORY_LIMIT_EXCEEDED:
        return "MLE";
      case RUNTIME_ERROR:
        return "RE";
      case INTERNAL_ERROR:
      default:
        return "SE";
    }
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
