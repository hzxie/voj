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
 * The native program runner, used to execute native applications, including the compiler (gcc) and
 * the program compiled from the code submitted by users.
 *
 * @author Haozhe Xie
 */
@Component
public class Runner {
  /**
   * Gets the runtime result of the (user's) program.
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

    Map<String, Object> result = new HashMap<>(4, 1);
    String runtimeResultSlug = "SE";
    int usedTime = 0;
    int usedMemory = 0;

    try {
      LOGGER.info(
          String.format(
              "[Submission #%d] Start running with command %s (TimeLimit=%d, MemoryLimit=%s)",
              new Object[] {submission.getSubmissionId(), commandLine, timeLimit, memoryLimit}));
      Map<String, Object> runtimeResult =
          getRuntimeResult(
              commandLine,
              systemUsername,
              systemPassword,
              inputFilePath,
              outputFilePath,
              timeLimit,
              memoryLimit);

      int exitCode = (Integer) runtimeResult.get("exitCode");
      usedTime = (Integer) runtimeResult.get("usedTime");
      usedMemory = (Integer) runtimeResult.get("usedMemory");
      runtimeResultSlug =
          getRuntimeResultSlug(exitCode, timeLimit, usedTime, memoryLimit, usedMemory);
    } catch (Exception ex) {
      LOGGER.catching(ex);
    }

    result.put("runtimeResult", runtimeResultSlug);
    result.put("usedTime", usedTime);
    result.put("usedMemory", usedMemory);
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
   * Wraps the judging result based on the result returned by JNI.
   *
   * @param exitCode - the program's exit status code
   * @param timeLimit - the maximum time limit
   * @param timeUsed - the time used by the program
   * @param memoryLimit - the maximum memory limit
   * @param memoryUsed - the memory used by the program (maximum)
   * @return the unique English abbreviation of the program's runtime result
   */
  private String getRuntimeResultSlug(
      int exitCode, int timeLimit, int timeUsed, int memoryLimit, int memoryUsed) {
    if (exitCode == 0) {
      // Output will be compared in next stage
      return "AC";
    }
    if (timeUsed >= timeLimit) {
      return "TLE";
    }
    if (memoryUsed >= memoryLimit) {
      return "MLE";
    }
    return "RE";
  }

  /**
   * Gets the runtime result of the (compilation) program.
   *
   * @param commandLine - the command line of the program to execute
   * @param inputFilePath - the input file path (may be NULL)
   * @param outputFilePath - the output file path (may be NULL)
   * @param timeLimit - the time limit (in ms, 0 means no limit)
   * @param memoryLimit - the memory limit (in KB, 0 means no limit)
   * @return a Map<String, Object> object containing the program's runtime result
   */
  public Map<String, Object> getRuntimeResult(
      String commandLine,
      String inputFilePath,
      String outputFilePath,
      int timeLimit,
      int memoryLimit) {
    Map<String, Object> result = null;
    try {
      result =
          getRuntimeResult(
              commandLine,
              systemUsername,
              systemPassword,
              inputFilePath,
              outputFilePath,
              timeLimit,
              memoryLimit);
    } catch (Exception ex) {
      LOGGER.catching(ex);
    }
    return result;
  }

  /**
   * Gets the program's runtime result.
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
