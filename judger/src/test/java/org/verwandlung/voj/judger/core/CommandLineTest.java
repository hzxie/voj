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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.verwandlung.voj.judger.model.Language;
import org.verwandlung.voj.judger.model.Submission;

/**
 * Tests how a language's commands are completed with the path of a submission's files.
 *
 * @author Haozhe Xie
 */
public class CommandLineTest {
  /** A work directory as Windows writes it, with backslashes. */
  private static final String WINDOWS_WORK_DIRECTORY =
      "C:\\ProgramData\\voj-judger\\work-0\\voj-1000-abc";

  /**
   * Test case: tests Compiler.getCompileCommandLine(). Test data: a Windows work directory.
   * Expected: {filename} is the path of the language's source file in the program directory,
   * without its extension, inserted literally, its backslashes kept.
   */
  @Test
  public void testCompileCommandKeepsBackslashes() {
    Submission submission = submissionOf("g++ -o {filename}.exe {filename}.cpp", "{filename}.exe");

    Assertions.assertEquals(
        "g++ -o "
            + WINDOWS_WORK_DIRECTORY
            + "/program/Main.exe "
            + WINDOWS_WORK_DIRECTORY
            + "/program/Main.cpp",
        new Compiler().getCompileCommandLine(submission, WINDOWS_WORK_DIRECTORY));
  }

  /**
   * Test case: tests ProgramRunner.getCommandLine(). Test data: a Windows work directory and a
   * directory with a dollar sign. Expected: both are inserted literally.
   */
  @Test
  public void testRunCommandKeepsBackslashesAndDollarSigns() {
    Submission submission = submissionOf("g++ {filename}.cpp", "{filename}.exe");

    Assertions.assertEquals(
        WINDOWS_WORK_DIRECTORY + "/program/Main.exe",
        new ProgramRunner().getCommandLine(submission, WINDOWS_WORK_DIRECTORY));
    Assertions.assertEquals(
        "/srv/$1/program/Main.exe", new ProgramRunner().getCommandLine(submission, "/srv/$1"));
  }

  /**
   * Test case: tests ProgramRunner.getCommandLine(). Test data: Java's commands. Expected: the run
   * command is used as it is, the class directory named after the source file.
   */
  @Test
  public void testJavaRunsItsMainClass() {
    Submission submission =
        submissionOf("javac -d {filename} {filename}.java", "java -cp {filename} Main");
    submission.getLanguage().setSourceFilename("Main.java");
    Assertions.assertEquals(
        "java -cp /srv/voj-1/program/Main Main",
        new ProgramRunner().getCommandLine(submission, "/srv/voj-1"));
  }

  /** Creates a submission in a language with the given commands. */
  private static Submission submissionOf(String compileCommand, String runCommand) {
    Submission submission = new Submission();
    Language language = new Language("cpp", "C++", compileCommand, runCommand);
    language.setSourceFilename("Main.cpp");
    submission.setLanguage(language);
    return submission;
  }
}
