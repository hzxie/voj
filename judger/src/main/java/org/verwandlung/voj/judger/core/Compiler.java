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

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import org.verwandlung.voj.judger.model.Language;
import org.verwandlung.voj.judger.model.Submission;
import org.verwandlung.voj.judger.util.FileSystemUtils;

/**
 * The program compiler, used to compile the code submitted by users.
 *
 * @author Haozhe Xie
 */
@Component
public class Compiler {
  /**
   * Gets the compilation result. The compiler runs in the program directory of the judging (see
   * {@link Preprocessor}), the only directory it may write to, and its log goes to the directory
   * of the judging, which it may not write to. Once the program is compiled, the judger takes the
   * program directory, which the runs of the program may then only read (see {@link
   * Preprocessor#sealProgramDirectory}).
   *
   * @param submission - the submission object
   * @param workDirectory - the directory of the judging
   * @return a Map<String, Object> object containing the compilation result
   */
  public Map<String, Object> getCompileResult(Submission submission, String workDirectory) {
    String commandLine = getCompileCommandLine(submission, workDirectory);
    String compileLogPath = workDirectory + "/" + COMPILE_LOG_FILE;
    int memoryLimit = getCompileMemoryLimit(submission.getLanguage());

    Map<String, Object> result =
        getCompileResult(
            commandLine,
            Preprocessor.getProgramDirectory(workDirectory),
            compileLogPath,
            memoryLimit);
    if ((Boolean) result.get("isSuccessful")) {
      try {
        preprocessor.sealProgramDirectory(workDirectory);
      } catch (Exception ex) {
        // The runs would otherwise be able to change the program for the runs after them.
        LOGGER.error("Failed to make the program directory of " + workDirectory + " read-only", ex);
        result.put("verdict", Verdict.INTERNAL_ERROR);
        result.put("isSuccessful", false);
      }
    }
    return result;
  }

  /**
   * Gets the memory limit of a compilation: judger.compileMemoryLimit scaled by the language's
   * memory multiplier, like the memory limit of a run. Languages that run on a memory-hungry
   * runtime have a memory-hungry compiler too: kotlinc is a JVM with a 512 MB heap of its own
   * (its launcher's -Xmx512M) and peaks well above 512 MB in total, while C, C++, Go, Rust and
   * Pascal (multiplier 1) keep the base limit, which bounds a "compiler bomb".
   *
   * @param language - the programming language object
   * @return the memory limit (KB, 0 means no limit)
   */
  int getCompileMemoryLimit(Language language) {
    double multiplier = language == null ? 1.0 : language.getMemoryMultiplier();
    if (compileMemoryLimit <= 0 || multiplier <= 1.0) {
      return compileMemoryLimit;
    }
    return (int) Math.min(Integer.MAX_VALUE, Math.ceil(compileMemoryLimit * multiplier));
  }

  /**
   * Gets the compile command.
   *
   * @param submission - the submission object
   * @param workDirectory - the directory of the judging
   * @return the compile command
   */
  String getCompileCommandLine(Submission submission, String workDirectory) {
    Language language = submission.getLanguage();
    // Literally: a Windows path's backslashes are not escapes.
    return language
        .getCompileCommand()
        .replace("{filename}", Preprocessor.getFilename(workDirectory, language));
  }

  /**
   * Gets the compilation result.
   *
   * @param commandLine - the compile command
   * @param programDirectory - the program directory, where the compiler runs
   * @param compileLogPath - the path of the compilation log output
   * @param memoryLimit - the memory limit (KB, 0 means no limit)
   * @return a Map<String, Object> object containing the compilation result
   */
  private Map<String, Object> getCompileResult(
      String commandLine, String programDirectory, String compileLogPath, int memoryLimit) {
    String inputFilePath = null;

    LOGGER.info("Start compiling with command: " + commandLine);
    RunResult runResult =
        sandboxRunner.run(
            commandLine,
            programDirectory,
            programDirectory,
            inputFilePath,
            compileLogPath,
            compileTimeLimit,
            memoryLimit,
            true);
    Map<String, Object> result = new HashMap<>(4, 1);

    Verdict verdict = runResult.getVerdict();
    result.put("verdict", verdict);
    result.put("isSuccessful", verdict == Verdict.NORMAL);
    result.put("log", getCompileOutput(compileLogPath) + getLimitExceededMessage(verdict));
    return result;
  }

  /**
   * Explains a compilation that the sandbox stopped at one of its limits, which the compiler itself
   * cannot report (it was killed, often before printing anything).
   *
   * @param verdict - the verdict of the compilation
   * @return the explanation, on a line of its own, or an empty string
   */
  private String getLimitExceededMessage(Verdict verdict) {
    switch (verdict) {
      case TIME_LIMIT_EXCEEDED:
        return String.format(
            "\nCompilation exceeded the time limit (%d ms).\n", new Object[] {compileTimeLimit});
      case MEMORY_LIMIT_EXCEEDED:
        return "\nCompilation exceeded the memory limit.\n";
      case OUTPUT_LIMIT_EXCEEDED:
        return "\nCompilation exceeded the output size limit.\n";
      default:
        return "";
    }
  }

  /**
   * Gets the content of the compilation log, at most its first {@value #MAX_COMPILE_LOG_BYTES}
   * bytes. A compiler can print far more (the output size limit is hundreds of megabytes), which
   * neither belongs in the judger's heap nor fits the judge log's column.
   *
   * @param compileLogPath - the path of the compilation log
   * @return the content of the compilation log
   */
  private String getCompileOutput(String compileLogPath) {
    // The log is created by the sandbox, outside the compiler's directory; as defence in depth,
    // never follow a link at its path, or the log shown to the user could be any file the judger
    // can read.
    try (InputStream inputStream =
        FileSystemUtils.newSandboxFileInputStream(Paths.get(compileLogPath))) {
      byte[] log = inputStream.readNBytes(MAX_COMPILE_LOG_BYTES + 1);
      if (log.length <= MAX_COMPILE_LOG_BYTES) {
        return new String(log, StandardCharsets.UTF_8);
      }
      return new String(log, 0, MAX_COMPILE_LOG_BYTES, StandardCharsets.UTF_8)
          + "\n... (truncated)\n";
    } catch (Exception ex) {
      return "";
    }
  }

  /**
   * The largest part of the compilation log that is kept. The judge log quotes it with every line
   * indented by four spaces, and must fit a TEXT column (64 KB) even for a log of empty lines.
   */
  static final int MAX_COMPILE_LOG_BYTES = 8192;

  /** The name of the compile log, in the directory of the judging. */
  static final String COMPILE_LOG_FILE = "compile.log";

  /** The configured sandbox runner, used to execute the compile command. */
  @Autowired private SandboxRunner sandboxRunner;

  /** The preprocessor, which hands the compiled program directory to the judger. */
  @Autowired private Preprocessor preprocessor;

  /**
   * The CPU time limit (milliseconds) for a compilation. Heavyweight toolchains such as the Go build
   * driver and the JVM-based Kotlin compiler need far more than a few seconds, so this is generous
   * and configurable rather than hardcoded.
   */
  @Value("${judger.compileTimeLimit:20000}")
  private int compileTimeLimit;

  /**
   * The memory limit (kilobytes) for a compilation, before the language's memory multiplier. Without
   * it, a small "compiler bomb" (e.g. C++ template recursion or a giant array initializer) can
   * exhaust host memory and OOM the judger. It must stay generous enough for legitimate compilers
   * (the Go linker, rustc), otherwise a real compile is killed and wrongly reported as a compile
   * error; JVM-based compilers get more through their language's multiplier.
   */
  @Value("${judger.compileMemoryLimit:524288}")
  private int compileMemoryLimit;

  /** The logger. */
  private static final Logger LOGGER = LogManager.getLogger(Compiler.class);
}
