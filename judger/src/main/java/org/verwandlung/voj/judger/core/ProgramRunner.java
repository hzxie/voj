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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import org.verwandlung.voj.judger.model.Language;
import org.verwandlung.voj.judger.model.Submission;

/**
 * Runs a user's compiled program for a single checkpoint.
 *
 * <p>This is the operating-system-independent orchestration layer: it builds the command line and
 * the time/memory limits from the submission and its programming language, delegates the actual
 * (sandboxed) execution to the configured {@link SandboxRunner}, and maps the resulting
 * {@link Verdict} to the unique English abbreviation used by the rest of the system.
 *
 * @author Haozhe Xie
 */
@Component
public class ProgramRunner {
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
    RunResult runResult =
        sandboxRunner.run(commandLine, inputFilePath, outputFilePath, timeLimit, memoryLimit);

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

  /** The configured sandbox runner (native or isolate); see {@code SandboxConfiguration}. */
  @Autowired private SandboxRunner sandboxRunner;

  /** The logger. */
  private static final Logger LOGGER = LogManager.getLogger(ProgramRunner.class);
}
