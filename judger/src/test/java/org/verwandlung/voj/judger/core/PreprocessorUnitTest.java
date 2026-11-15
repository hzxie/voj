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

import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;

import org.verwandlung.voj.judger.model.Language;
import org.verwandlung.voj.judger.model.Submission;

/**
 * Database-free unit tests for the preprocessor.
 *
 * @author Haozhe Xie
 */
public class PreprocessorUnitTest {
  /**
   * Test case: tests the createTestCode(Submission, String, String) method. Test data: a work
   * directory pre-populated with a stale file and a symbolic link pointing outside of it. Expected
   * result: the directory is recreated empty and private, holds only the source file, and the
   * link's target is untouched.
   */
  @Test
  @DisabledOnOs(OS.WINDOWS)
  public void testCreateTestCodeRecreatesExistingWorkDirectory() throws Exception {
    Path outsideFile = Files.writeString(tempDir.resolve("secret.txt"), "secret");
    Path workDirectory = Files.createDirectory(tempDir.resolve("voj-1000"));
    Files.writeString(workDirectory.resolve("stale.txt"), "stale");
    Files.createSymbolicLink(workDirectory.resolve("output#1.txt"), outsideFile);

    preprocessor.createTestCode(
        submission("C++", "g++ -o {filename}.exe {filename}.cpp", "int main() {}"),
        workDirectory.toString(),
        "random");

    try (Stream<Path> files = Files.list(workDirectory)) {
      List<String> names =
          files.map(path -> path.getFileName().toString()).collect(Collectors.toList());
      Assertions.assertEquals(List.of("random.cpp"), names);
    }
    Assertions.assertEquals("int main() {}", Files.readString(workDirectory.resolve("random.cpp")));
    Assertions.assertEquals("secret", Files.readString(outsideFile));
    Assertions.assertEquals(
        "rwx------",
        PosixFilePermissions.toString(
            Files.getPosixFilePermissions(workDirectory, LinkOption.NOFOLLOW_LINKS)));
  }

  /**
   * Test case: tests the createTestCode(Submission, String, String) method. Test data: the work
   * directory path is a symbolic link to another directory. Expected result: the link is replaced
   * by a real directory and nothing is written into the link's target.
   */
  @Test
  @DisabledOnOs(OS.WINDOWS)
  public void testCreateTestCodeReplacesSymbolicLinkedWorkDirectory() throws Exception {
    Path target = Files.createDirectory(tempDir.resolve("target"));
    Path workDirectory = Files.createSymbolicLink(tempDir.resolve("voj-1001"), target);

    preprocessor.createTestCode(
        submission("C++", "g++ -o {filename}.exe {filename}.cpp", "int main() {}"),
        workDirectory.toString(),
        "random");

    Assertions.assertFalse(Files.isSymbolicLink(workDirectory));
    Assertions.assertTrue(Files.isRegularFile(workDirectory.resolve("random.cpp")));
    try (Stream<Path> files = Files.list(target)) {
      Assertions.assertEquals(0, files.count());
    }
  }

  /**
   * Test case: tests the createTestCode(Submission, String, String) method. Test data: Java code
   * declaring Main (separated by a tab / CRLF) alongside a MainHelper class. Expected result: only
   * the Main declarations are renamed.
   */
  @Test
  public void testCreateTestCodeRenamesOnlyJavaMainClass() throws Exception {
    String code =
        "public class\tMain {\r\n"
            + "  public static void main(String[] args) { MainHelper.run(); }\r\n"
            + "}\r\n"
            + "class MainHelper { static void run() {} }\n"
            + "final class\r\nMain2 {}\n";
    Path workDirectory = tempDir.resolve("voj-1002");

    preprocessor.createTestCode(
        submission("Java", "javac {filename}.java", code), workDirectory.toString(), "Random");

    String expected =
        "public class Random {\r\n"
            + "  public static void main(String[] args) { MainHelper.run(); }\r\n"
            + "}\r\n"
            + "class MainHelper { static void run() {} }\n"
            + "final class\r\nMain2 {}\n";
    Assertions.assertEquals(expected, Files.readString(workDirectory.resolve("Random.java")));
  }

  /**
   * Test case: tests the createTestCode(Submission, String, String) method. Test data: Java code
   * whose Main class is declared on the line after the keyword. Expected result: it is renamed.
   */
  @Test
  public void testCreateTestCodeRenamesJavaMainClassAcrossLines() throws Exception {
    Path workDirectory = tempDir.resolve("voj-1003");

    preprocessor.createTestCode(
        submission("Java", "javac {filename}.java", "class\r\n  Main<T> {}"),
        workDirectory.toString(),
        "Random");

    Assertions.assertEquals(
        "class Random<T> {}", Files.readString(workDirectory.resolve("Random.java")));
  }

  /**
   * Builds a submission in the given language.
   *
   * @param languageName - the name of the programming language
   * @param compileCommand - the compile command of the programming language
   * @param code - the submitted code
   * @return a submission object
   */
  private Submission submission(String languageName, String compileCommand, String code) {
    Submission submission = new Submission();
    submission.setLanguage(new Language("text/x-test", languageName, compileCommand, ""));
    submission.setCode(code);
    return submission;
  }

  /** The Preprocessor object under test (no Spring context: system.username stays unset). */
  private final Preprocessor preprocessor = new Preprocessor();

  /** A temporary directory managed by JUnit. */
  @TempDir Path tempDir;
}
