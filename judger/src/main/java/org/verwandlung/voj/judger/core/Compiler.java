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

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

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
   * Compiles a submission. The compiler runs in the program directory of the judging (see {@link
   * Preprocessor}), the only directory it may write to, and its log goes to the directory of the
   * judging, which it may not write to. Once the program is compiled, the judger takes the program
   * directory, which the runs of the program may then only read (see {@link
   * Preprocessor#sealProgramDirectory}).
   *
   * @param submission - the submission object
   * @param workDirectory - the directory of the judging
   * @return the result of the compilation
   */
  public CompileResult getCompileResult(Submission submission, String workDirectory) {
    String compileLogPath = workDirectory + "/" + COMPILE_LOG_FILE;
    SandboxCommand command =
        sandboxPolicy.compilation(
            submission.getLanguage(),
            Preprocessor.getProgramDirectory(workDirectory),
            compileLogPath);
    LOGGER.info("Start compiling with command: " + String.join(" ", command.arguments()));
    Verdict verdict;
    try {
      verdict = Verdict.of(command, sandboxRunner.run(command));
    } catch (IOException ex) {
      LOGGER.error("The sandbox failed to run the compiler", ex);
      return new CompileResult(Verdict.INTERNAL_ERROR, "");
    }
    if (verdict == Verdict.NORMAL) {
      try {
        preprocessor.sealProgramDirectory(workDirectory);
      } catch (IOException ex) {
        // The runs would otherwise be able to change the program for the runs after them.
        LOGGER.error("Failed to make the program directory of " + workDirectory + " read-only", ex);
        return new CompileResult(Verdict.INTERNAL_ERROR, "");
      }
    }
    return new CompileResult(
        verdict,
        getCompileOutput(compileLogPath) + getLimitExceededMessage(verdict, command.limits()));
  }

  /**
   * Explains a compilation that the sandbox stopped at one of its limits, which the compiler itself
   * cannot report (it was killed, often before printing anything).
   *
   * @param verdict - the verdict of the compilation
   * @param limits - the limits of the compilation
   * @return the explanation, on a line of its own, or an empty string
   */
  private String getLimitExceededMessage(Verdict verdict, Limits limits) {
    switch (verdict) {
      case TIME_LIMIT_EXCEEDED:
        return String.format(
            "\nCompilation exceeded the time limit (%d ms).\n", limits.timeLimitMs());
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

  /** The policy, which builds the compilation. */
  @Autowired private SandboxPolicy sandboxPolicy;

  /** The configured sandbox runner, used to execute the compile command. */
  @Autowired private SandboxRunner sandboxRunner;

  /** The preprocessor, which hands the compiled program directory to the judger. */
  @Autowired private Preprocessor preprocessor;

  /** The logger. */
  private static final Logger LOGGER = LogManager.getLogger(Compiler.class);
}
