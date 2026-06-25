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

import java.io.FileInputStream;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import org.verwandlung.voj.judger.model.Submission;

/**
 * The program compiler, used to compile the code submitted by users.
 *
 * @author Haozhe Xie
 */
@Component
public class Compiler {
  /**
   * Gets the compilation result.
   *
   * @param submission - the submission object
   * @param workDirectory - the compilation output directory
   * @param baseFileName - the compilation output file name
   * @return a Map<String, Object> object containing the compilation result
   */
  public Map<String, Object> getCompileResult(
      Submission submission, String workDirectory, String baseFileName) {
    String commandLine = getCompileCommandLine(submission, workDirectory, baseFileName);
    String compileLogPath = getCompileLogPath(workDirectory, baseFileName);

    return getCompileResult(commandLine, compileLogPath);
  }

  /**
   * Gets the compile command.
   *
   * @param submission - the submission object
   * @param workDirectory - the compilation output directory
   * @param baseFileName - the compilation output file name
   * @return the compile command
   */
  private String getCompileCommandLine(
      Submission submission, String workDirectory, String baseFileName) {
    String filePathWithoutExtension =
        String.format("%s/%s", new Object[] {workDirectory, baseFileName});
    String compileCommand =
        submission
            .getLanguage()
            .getCompileCommand()
            .replaceAll("\\{filename\\}", filePathWithoutExtension);
    return compileCommand;
  }

  /**
   * Gets the file path of the compilation log output.
   *
   * @param workDirectory - the compilation output directory
   * @param baseFileName - the compilation output file name
   * @return the file path of the compilation log output
   */
  private String getCompileLogPath(String workDirectory, String baseFileName) {
    return String.format("%s/%s-compile.log", new Object[] {workDirectory, baseFileName});
  }

  /**
   * Gets the compilation result.
   *
   * @param commandLine - the compile command
   * @param compileLogPath - the path of the compilation log output
   * @return a Map<String, Object> object containing the compilation result
   */
  private Map<String, Object> getCompileResult(String commandLine, String compileLogPath) {
    String inputFilePath = null;
    int timeLimit = 5000;
    int memoryLimit = 0;

    LOGGER.info("Start compiling with command: " + commandLine);
    RunResult runResult =
        sandboxRunner.run(commandLine, inputFilePath, compileLogPath, timeLimit, memoryLimit);
    Map<String, Object> result = new HashMap<>(3, 1);

    boolean isSuccessful = runResult.getVerdict() == Verdict.NORMAL;
    result.put("isSuccessful", isSuccessful);
    result.put("log", getCompileOutput(compileLogPath));
    return result;
  }

  /**
   * Gets the content of the compilation log.
   *
   * @param compileLogPath - the path of the compilation log
   * @return the content of the compilation log
   */
  private String getCompileOutput(String compileLogPath) {
    FileInputStream inputStream = null;
    String compileLog = "";
    try {
      inputStream = new FileInputStream(compileLogPath);
      compileLog = IOUtils.toString(inputStream);
      inputStream.close();
    } catch (Exception ex) {
      // Do nothing
    }
    return compileLog;
  }

  /** The configured sandbox runner, used to execute the compile command. */
  @Autowired private SandboxRunner sandboxRunner;

  /** The logger. */
  private static final Logger LOGGER = LogManager.getLogger(Compiler.class);
}
