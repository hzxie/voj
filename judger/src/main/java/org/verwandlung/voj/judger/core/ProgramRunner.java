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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import org.verwandlung.voj.judger.model.Submission;
import org.verwandlung.voj.judger.util.FileSystemUtils;

/**
 * Runs a user's compiled program for a single checkpoint: the run is built by {@link
 * SandboxPolicy}, carried out by the configured {@link SandboxRunner} and judged by {@link
 * Verdict#of}.
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
   * @return the outcome of the run
   */
  public RunResult getRuntimeResult(
      Submission submission, String workDirectory, String inputFilePath, String outputFilePath) {
    String runDirectory = null;
    try {
      runDirectory = preprocessor.createRunDirectory(workDirectory);
      SandboxCommand command =
          sandboxPolicy.run(
              submission.getLanguage(),
              submission.getProblem(),
              Preprocessor.getProgramDirectory(workDirectory),
              runDirectory,
              inputFilePath,
              outputFilePath);
      Limits limits = command.limits();
      LOGGER.info(
          String.format(
              "[Submission #%d] Start running with command %s (TimeLimit=%d, MemoryLimit=%s)",
              submission.getSubmissionId(),
              String.join(" ", command.arguments()),
              limits.timeLimitMs(),
              limits.memoryLimitKb()));
      SandboxResult result = sandboxRunner.run(command);
      Verdict verdict = Verdict.of(command, result);
      // A program judged over a limit is reported as having used at least that much.
      return new RunResult(
          verdict,
          verdict == Verdict.TIME_LIMIT_EXCEEDED
              ? Math.max(result.cpuTimeMs(), limits.timeLimitMs())
              : result.cpuTimeMs(),
          verdict == Verdict.MEMORY_LIMIT_EXCEEDED
              ? Math.max(result.peakMemoryKb(), limits.memoryLimitKb())
              : result.peakMemoryKb());
    } catch (IOException ex) {
      LOGGER.error("The sandbox failed to run the program in " + workDirectory, ex);
      return new RunResult(Verdict.INTERNAL_ERROR, 0, 0);
    } finally {
      removeRunDirectory(runDirectory);
    }
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

  /** The policy, which builds the run. */
  @Autowired private SandboxPolicy sandboxPolicy;

  /** The configured sandbox runner (native or isolate); see {@code SandboxConfiguration}. */
  @Autowired private SandboxRunner sandboxRunner;

  /** The preprocessor, which creates the work directory of each run. */
  @Autowired private Preprocessor preprocessor;

  /** The logger. */
  private static final Logger LOGGER = LogManager.getLogger(ProgramRunner.class);
}
