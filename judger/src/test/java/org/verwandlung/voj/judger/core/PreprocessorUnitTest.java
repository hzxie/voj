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
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.util.ReflectionTestUtils;

import org.verwandlung.voj.judger.model.Language;
import org.verwandlung.voj.judger.model.Submission;

/**
 * Database-free unit tests for the preprocessor.
 *
 * @author Haozhe Xie
 */
public class PreprocessorUnitTest {
  /**
   * Test case: tests the createTestCode(Submission, String) method. Test data: a work
   * directory pre-populated with a stale file and a symbolic link pointing outside of it. Expected
   * result: the directory is recreated empty, others may only pass through it, and it holds only
   * the private program directory with the source file in it, and the link's target is untouched.
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
        workDirectory.toString());

    Path programDirectory = workDirectory.resolve("program");
    try (Stream<Path> files = Files.list(workDirectory)) {
      List<String> names =
          files.map(path -> path.getFileName().toString()).collect(Collectors.toList());
      Assertions.assertEquals(List.of("program"), names);
    }
    try (Stream<Path> files = Files.list(programDirectory)) {
      List<String> names =
          files.map(path -> path.getFileName().toString()).collect(Collectors.toList());
      Assertions.assertEquals(List.of("Main.cpp"), names);
    }
    Assertions.assertEquals(
        "int main() {}", Files.readString(programDirectory.resolve("Main.cpp")));
    Assertions.assertEquals("secret", Files.readString(outsideFile));
    Assertions.assertEquals(
        "rwx--x--x",
        PosixFilePermissions.toString(
            Files.getPosixFilePermissions(workDirectory, LinkOption.NOFOLLOW_LINKS)));
    Assertions.assertEquals(
        "rwx------",
        PosixFilePermissions.toString(
            Files.getPosixFilePermissions(programDirectory, LinkOption.NOFOLLOW_LINKS)));
  }

  /**
   * Test case: tests the sealProgramDirectory(String) and createRunDirectory(String) methods. Test
   * data: a program directory with a program (with the set-user-ID bit and writable by everyone),
   * a source file, a subdirectory and a symbolic link, and a run directory a run left a file in.
   * Expected result: nobody but the owner may write to anything in the program directory any more
   * (as root, the judger owns all of it), the program keeps its execute permissions but loses the
   * set-user-ID bit, the link is left alone; and the run directory is created anew, empty and
   * private.
   */
  @Test
  @DisabledOnOs(OS.WINDOWS)
  public void testSealProgramDirectoryAndCreateRunDirectory() throws Exception {
    Path workDirectory = tempDir.resolve("voj-1006");
    preprocessor.createTestCode(
        submission("C++", "g++ -o {filename}.exe {filename}.cpp", "int main() {}"),
        workDirectory.toString());
    Path programDirectory = workDirectory.resolve("program");
    Path program = Files.writeString(programDirectory.resolve("Main.exe"), "binary");
    Files.setPosixFilePermissions(program, PosixFilePermissions.fromString("rwxrwxrwx"));
    Files.setAttribute(program, "unix:mode", 04777);
    Path subdirectory = Files.createDirectory(programDirectory.resolve("sub"));
    Files.setPosixFilePermissions(subdirectory, PosixFilePermissions.fromString("rwxrwxrwx"));
    Files.createSymbolicLink(programDirectory.resolve("link"), tempDir);
    String runDirectory = preprocessor.createRunDirectory(workDirectory.toString());
    Files.writeString(Path.of(runDirectory, "left.txt"), "left");

    preprocessor.sealProgramDirectory(workDirectory.toString());

    Assertions.assertEquals(
        "rwxr-xr-x",
        PosixFilePermissions.toString(Files.getPosixFilePermissions(programDirectory)));
    Assertions.assertEquals(
        "rwxr-xr-x", PosixFilePermissions.toString(Files.getPosixFilePermissions(subdirectory)));
    Assertions.assertEquals(
        0755, (Integer) Files.getAttribute(program, "unix:mode") & 07777);
    Assertions.assertTrue(Files.isSymbolicLink(programDirectory.resolve("link")));
    Assertions.assertEquals(
        System.getProperty("user.name"), Files.getOwner(program).getName());
    Assertions.assertEquals(
        runDirectory, preprocessor.createRunDirectory(workDirectory.toString()));
    try (Stream<Path> files = Files.list(Path.of(runDirectory))) {
      Assertions.assertEquals(0, files.count());
    }
    Assertions.assertEquals(
        "rwx------",
        PosixFilePermissions.toString(Files.getPosixFilePermissions(Path.of(runDirectory))));
  }

  /**
   * Test case: tests the createTestCode(Submission, String) method. Test data: the work
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
        workDirectory.toString());

    Assertions.assertFalse(Files.isSymbolicLink(workDirectory));
    Assertions.assertTrue(Files.isRegularFile(workDirectory.resolve("program/Main.cpp")));
    try (Stream<Path> files = Files.list(target)) {
      Assertions.assertEquals(0, files.count());
    }
  }

  /**
   * Test case: tests the createTestCode(Submission, String) method. Test data: Java code that
   * prints "class Main". Expected result: the code is saved as it is, as the language's source
   * file.
   */
  @Test
  public void testCreateTestCodeSavesTheCodeAsTheLanguagesSourceFile() throws Exception {
    String code =
        "public class Main {\r\n"
            + "  public static void main(String[] args) { System.out.println(\"class Main\"); }\r\n"
            + "}\r\n";
    Path workDirectory = tempDir.resolve("voj-1002");

    preprocessor.createTestCode(
        submission("Java", "javac -d {filename} {filename}.java", code), workDirectory.toString());

    Assertions.assertEquals(code, Files.readString(workDirectory.resolve("program/Main.java")));
  }

  /**
   * Test case: tests the createTestCode(Submission, String) method. Test data: languages whose
   * source file name is missing or not a plain file name. Expected result: no code is written.
   */
  @Test
  public void testCreateTestCodeRefusesASourceFileNameThatIsNotAPlainName() {
    for (String name : new String[] {"", "..", "../Main.c", "sub/Main.c", "sub\\Main.c"}) {
      Submission submission = submission("C", "gcc {filename}.c", "int main() {}");
      submission.getLanguage().setSourceFilename(name);
      Assertions.assertThrows(
          IOException.class,
          () -> preprocessor.createTestCode(submission, tempDir.resolve("voj-1003").toString()),
          name);
    }
  }

  /**
   * Test case: tests the createTestCode(Submission, String) method. Test data: a Go
   * submission, a root-owned read-only build cache seed holding an entry, a group-writable entry
   * and a stale trim file. Expected result: the submission gets its own cache in its program
   * directory whose directories belong to the sandbox user, the trusted entry is a link to the
   * seed's file that stays root-owned and read-only, the untrusted entry is left out, and the
   * cache is marked as just trimmed; once the program directory is made read-only, the seed's file
   * is still unchanged. Needs root (it hands files to another user).
   */
  @Test
  @DisabledOnOs(OS.WINDOWS)
  public void testCreateTestCodeSeedsAPrivateGoBuildCache() throws Exception {
    Assumptions.assumeTrue("root".equals(System.getProperty("user.name")), "Needs root.");
    Path seed = Files.createDirectories(tempDir.resolve("seed/ab"));
    Path entry = Files.writeString(seed.resolve("0123-d"), "compiled");
    Path untrusted = Files.writeString(seed.resolve("4567-d"), "untrusted");
    Files.writeString(seed.getParent().resolve("trim.txt"), "0");
    Files.setPosixFilePermissions(entry, PosixFilePermissions.fromString("r--r--r--"));
    Files.setPosixFilePermissions(untrusted, PosixFilePermissions.fromString("rw-rw-rw-"));
    Files.setPosixFilePermissions(tempDir, PosixFilePermissions.fromString("rwxr-xr-x"));
    ReflectionTestUtils.setField(preprocessor, "goBuildCacheSeed", seed.getParent().toString());
    ReflectionTestUtils.setField(preprocessor, "sandboxUsername", "nobody");
    Path workDirectory = tempDir.resolve("voj-1004");

    preprocessor.createTestCode(
        submission("Go", "go build -o {filename}.exe {filename}.go", "package main"),
        workDirectory.toString());

    Path cache = workDirectory.resolve("program/.cache/go-build");
    Assertions.assertEquals("nobody", Files.getOwner(cache.resolve("ab")).getName());
    Assertions.assertEquals("nobody", Files.getOwner(cache.getParent()).getName());
    Path linked = cache.resolve("ab/0123-d");
    Assertions.assertTrue(Files.isSameFile(entry, linked));
    Assertions.assertEquals("root", Files.getOwner(linked).getName());
    Assertions.assertEquals(
        "r--r--r--", PosixFilePermissions.toString(Files.getPosixFilePermissions(linked)));
    Assertions.assertFalse(Files.exists(cache.resolve("ab/4567-d")));
    long trimmed = Long.parseLong(Files.readString(cache.resolve("trim.txt")));
    Assertions.assertTrue(Math.abs(System.currentTimeMillis() / 1000 - trimmed) < 60);

    preprocessor.sealProgramDirectory(workDirectory.toString());

    Assertions.assertEquals("root", Files.getOwner(cache.resolve("ab")).getName());
    Assertions.assertEquals("root", Files.getOwner(entry).getName());
    Assertions.assertEquals(
        "r--r--r--", PosixFilePermissions.toString(Files.getPosixFilePermissions(entry)));
  }

  /**
   * Test case: tests the createTestCode(Submission, String) method. Test data: the isolate
   * backend with box 3. Expected result: the private program directory belongs to isolate's box
   * user, UID first_uid + 3, the directory of the judging to the judger. Needs root.
   */
  @Test
  @DisabledOnOs(OS.WINDOWS)
  public void testCreateTestCodeHandsWorkDirectoryToIsolateBoxUser() throws Exception {
    Assumptions.assumeTrue("root".equals(System.getProperty("user.name")), "Needs root.");
    ReflectionTestUtils.setField(preprocessor, "sandbox", "isolate");
    ReflectionTestUtils.setField(preprocessor, "isolateFirstUid", 60000);
    ReflectionTestUtils.setField(preprocessor, "isolateBoxId", 3);
    ReflectionTestUtils.setField(preprocessor, "sandboxUsername", "nobody");
    Path workDirectory = tempDir.resolve("voj-1005");

    preprocessor.createTestCode(
        submission("C++", "g++ -o {filename}.exe {filename}.cpp", "int main() {}"),
        workDirectory.toString());

    Path programDirectory = workDirectory.resolve("program");
    Assertions.assertEquals(
        60003, Files.getAttribute(programDirectory, "unix:uid", LinkOption.NOFOLLOW_LINKS));
    Assertions.assertEquals(
        0, Files.getAttribute(workDirectory, "unix:uid", LinkOption.NOFOLLOW_LINKS));
    Assertions.assertEquals(
        "rwx------",
        PosixFilePermissions.toString(
            Files.getPosixFilePermissions(programDirectory, LinkOption.NOFOLLOW_LINKS)));
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
    Language language = new Language("text/x-test", languageName, compileCommand, "");
    String extension = compileCommand.substring(compileCommand.lastIndexOf("{filename}.") + 11);
    language.setSourceFilename("Main." + extension.split(" ")[0]);
    Submission submission = new Submission();
    submission.setLanguage(language);
    submission.setCode(code);
    return submission;
  }

  /** The Preprocessor object under test (no Spring context: system.username stays unset). */
  private final Preprocessor preprocessor = new Preprocessor();

  /** A temporary directory managed by JUnit. */
  @TempDir Path tempDir;
}
