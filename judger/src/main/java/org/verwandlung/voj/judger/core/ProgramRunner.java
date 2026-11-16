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
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import org.verwandlung.voj.judger.model.Language;
import org.verwandlung.voj.judger.model.Submission;
import org.verwandlung.voj.judger.util.FileSystemUtils;

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
   * Gets the runtime result of the (user's) program for a single checkpoint. The program, in the
   * program directory of the judging, which the run may only read, runs in a work directory of its
   * own, created empty for the run and removed after it (see {@link Preprocessor}): nothing a run
   * writes reaches a later one.
   *
   * @param submission - the submission object
   * @param workDirectory - the directory of the judging
   * @param inputFilePath - the input file path
   * @param outputFilePath - the output file path, in a directory the run may not write to
   * @return a Map<String, Object> object containing the program's runtime result
   */
  public Map<String, Object> getRuntimeResult(
      Submission submission,
      String workDirectory,
      String inputFilePath,
      String outputFilePath) {
    String commandLine = getCommandLine(submission, workDirectory);
    int timeLimit = getTimeLimit(submission);
    int memoryLimit = getMemoryLimit(submission);

    LOGGER.info(
        String.format(
            "[Submission #%d] Start running with command %s (TimeLimit=%d, MemoryLimit=%s)",
            new Object[] {submission.getSubmissionId(), commandLine, timeLimit, memoryLimit}));
    RunResult runResult;
    String runDirectory = null;
    try {
      runDirectory = preprocessor.createRunDirectory(workDirectory);
      runResult =
          sandboxRunner.run(
              commandLine,
              Preprocessor.getProgramDirectory(workDirectory),
              runDirectory,
              inputFilePath,
              outputFilePath,
              timeLimit,
              memoryLimit,
              false);
    } catch (IOException ex) {
      LOGGER.error("Failed to create the work directory of a run in " + workDirectory, ex);
      runResult = RunResult.internalError();
    } finally {
      removeRunDirectory(runDirectory);
    }
    Verdict verdict = runResult.getVerdict();
    int usedMemory = runResult.getPeakMemoryKb();
    if (JavaOptions.isOutOfMemoryExit(verdict, runResult.getExitCode(), outputFilePath)) {
      // The JVM's heap, sized to the memory limit (see JavaOptions), ran out: the program needed
      // more memory than it is allowed. Like a program stopped at the limit, or one that asked for
      // more than the memory backstop at once, it is reported as having used at least the limit.
      verdict = Verdict.MEMORY_LIMIT_EXCEEDED;
      usedMemory = Math.max(usedMemory, memoryLimit);
    }

    Map<String, Object> result = new HashMap<>(4, 1);
    result.put("runtimeResult", getRuntimeResultSlug(verdict));
    result.put("usedTime", runResult.getCpuTimeMs());
    result.put("usedMemory", usedMemory);
    return result;
  }

  /**
   * Removes the work directory of a run once it is over, with what the run left there.
   *
   * @param runDirectory - the work directory of the run ({@code null} if it was not created)
   */
  private void removeRunDirectory(String runDirectory) {
    if (runDirectory == null) {
      return;
    }
    try {
      FileSystemUtils.deleteRecursively(Paths.get(runDirectory));
    } catch (IOException ex) {
      // Removed before the next run (or with the directory of the judging) all the same.
      LOGGER.warn("Failed to remove the work directory of a run, " + runDirectory, ex);
    }
  }

  /**
   * Gets the command line to execute.
   *
   * @param submission - the submission object
   * @param workDirectory - the directory of the judging
   * @return the command line to execute
   */
  String getCommandLine(Submission submission, String workDirectory) {
    Language language = submission.getLanguage();
    // Literally: a Windows path's backslashes are not escapes.
    return language
        .getRunCommand()
        .replace("{filename}", Preprocessor.getFilename(workDirectory, language));
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
    return (int) Math.ceil(timeLimit * language.getTimeMultiplier());
  }

  /**
   * Gets the maximum memory limit depending on the language.
   *
   * @param submission - the submission object
   * @return the maximum memory limit
   */
  private int getMemoryLimit(Submission submission) {
    Language language = submission.getLanguage();
    int memoryLimit = submission.getProblem().getMemoryLimit();
    return (int) Math.ceil(memoryLimit * language.getMemoryMultiplier());
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
      case OUTPUT_LIMIT_EXCEEDED:
        return "OLE";
      case RUNTIME_ERROR:
        return "RE";
      case INTERNAL_ERROR:
      default:
        return "SE";
    }
  }

  /** The configured sandbox runner (native or isolate); see {@code SandboxConfiguration}. */
  @Autowired private SandboxRunner sandboxRunner;

  /** The preprocessor, which creates the work directory of each run. */
  @Autowired private Preprocessor preprocessor;

  /** The logger. */
  private static final Logger LOGGER = LogManager.getLogger(ProgramRunner.class);
}
