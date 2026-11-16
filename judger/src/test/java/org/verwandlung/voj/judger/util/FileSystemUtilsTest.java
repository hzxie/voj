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
package org.verwandlung.voj.judger.util;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.attribute.AclEntry;
import java.nio.file.attribute.AclFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;

/**
 * Test class for FileSystemUtils.
 *
 * @author Haozhe Xie
 */
public class FileSystemUtilsTest {
  /**
   * Test case: tests the deleteRecursively(Path) method. Test data: a nested directory tree.
   * Expected result: the whole tree is removed.
   */
  @Test
  public void testDeleteRecursively() throws Exception {
    Path root = tempDir.resolve("work");
    Files.createDirectories(root.resolve("a/b"));
    Files.writeString(root.resolve("a/b/file.txt"), "data");
    Files.writeString(root.resolve("top.txt"), "data");

    FileSystemUtils.deleteRecursively(root);
    Assertions.assertFalse(Files.exists(root, LinkOption.NOFOLLOW_LINKS));
  }

  /**
   * Test case: tests the deleteRecursively(Path) method. Test data: a missing path. Expected
   * result: nothing happens.
   */
  @Test
  public void testDeleteRecursivelyMissingPath() throws Exception {
    FileSystemUtils.deleteRecursively(tempDir.resolve("missing"));
  }

  /**
   * Test case: tests the deleteRecursively(Path) method. Test data: a directory containing symbolic
   * links to a file and a directory outside it, and a symbolic link passed as the root. Expected
   * result: only the links are removed; their targets are left untouched.
   */
  @Test
  @DisabledOnOs(OS.WINDOWS)
  public void testDeleteRecursivelyDoesNotFollowSymbolicLinks() throws Exception {
    Path outside = Files.createDirectory(tempDir.resolve("outside"));
    Path outsideFile = Files.writeString(outside.resolve("secret.txt"), "secret");

    Path root = Files.createDirectory(tempDir.resolve("work"));
    Files.createSymbolicLink(root.resolve("output#1.txt"), outsideFile);
    Files.createSymbolicLink(root.resolve("dir-link"), outside);

    FileSystemUtils.deleteRecursively(root);
    Assertions.assertFalse(Files.exists(root, LinkOption.NOFOLLOW_LINKS));
    Assertions.assertEquals("secret", Files.readString(outsideFile));

    Path rootLink = Files.createSymbolicLink(tempDir.resolve("root-link"), outside);
    FileSystemUtils.deleteRecursively(rootLink);
    Assertions.assertFalse(Files.exists(rootLink, LinkOption.NOFOLLOW_LINKS));
    Assertions.assertEquals("secret", Files.readString(outsideFile));
  }

  /**
   * Test case: tests the deleteRecursively(Path) method. Test data: a chain of directories nested
   * deeper than a path may be long (as a submission builds it by creating a directory and entering
   * it again and again), with a file at the bottom and a symbolic link to a directory outside it
   * halfway down. Expected result: the whole tree is removed, the link's target is left untouched.
   */
  @Test
  @DisabledOnOs(OS.WINDOWS)
  public void testDeleteRecursivelyRemovesATreeDeeperThanPathMax() throws Exception {
    Path outside = Files.createDirectory(tempDir.resolve("outside"));
    Path outsideFile = Files.writeString(outside.resolve("secret.txt"), "secret");
    Path root = Files.createDirectory(tempDir.resolve("work"));
    // Built bottom-up by short paths only: the chain is moved into a new directory, which then
    // takes the chain's place, one level at a time.
    Path chain = Files.createDirectory(root.resolve("d"));
    Files.writeString(chain.resolve("deepest.txt"), "data");
    int depth = 3000;
    for (int level = 1; level < depth; ++level) {
      Path parent = Files.createDirectory(root.resolve("n"));
      if (level == depth / 2) {
        Files.createSymbolicLink(parent.resolve("link"), outside);
      }
      Files.move(chain, parent.resolve("d"));
      Files.move(parent, chain);
    }
    // "/d" per level: the deepest directory's path is longer than PATH_MAX (4096 on Linux).
    Assertions.assertTrue(root.toString().length() + 2 * depth > 4096);

    FileSystemUtils.deleteRecursively(root);

    Assertions.assertFalse(Files.exists(root, LinkOption.NOFOLLOW_LINKS));
    Assertions.assertEquals("secret", Files.readString(outsideFile));
  }

  /**
   * Test case: tests the deleteRecursively(Path) method. Test data: a tree whose directories had
   * their permissions taken away (as a submission running as the judger's own account can do): the
   * directory itself, a subdirectory and its subdirectory without any, another one without write
   * permission. Expected result: the whole tree is removed.
   */
  @Test
  @DisabledOnOs(OS.WINDOWS)
  public void testDeleteRecursivelyRemovesDirectoriesWithoutPermissions() throws Exception {
    Assumptions.assumeFalse(
        "root".equals(System.getProperty("user.name")), "root is never denied access");
    Path root = Files.createDirectory(tempDir.resolve("work"));
    Path locked = Files.createDirectories(root.resolve("locked/inner"));
    Files.writeString(locked.resolve("file.txt"), "data");
    Path readOnly = Files.createDirectory(root.resolve("read-only"));
    Files.writeString(readOnly.resolve("file.txt"), "data");
    Files.setPosixFilePermissions(locked, PosixFilePermissions.fromString("---------"));
    Files.setPosixFilePermissions(locked.getParent(), PosixFilePermissions.fromString("---------"));
    Files.setPosixFilePermissions(readOnly, PosixFilePermissions.fromString("r-x------"));
    Files.setPosixFilePermissions(root, PosixFilePermissions.fromString("---------"));

    FileSystemUtils.deleteRecursively(root);

    Assertions.assertFalse(Files.exists(root, LinkOption.NOFOLLOW_LINKS));
  }

  /**
   * Test case: tests the deleteRecursively(Path) method on Windows. Test data: a directory holding
   * a junction to a directory outside it, a junction whose target is gone, and a junction to a
   * directory the judger may not list; and a junction passed as the root. Expected result: only
   * the junctions are removed, never listed nor followed; their targets are left untouched.
   */
  @Test
  @EnabledOnOs(OS.WINDOWS)
  public void testDeleteRecursivelyRemovesJunctionsAsLinksOnWindows() throws Exception {
    Path outside = Files.createDirectory(tempDir.resolve("outside"));
    Path outsideFile = Files.writeString(outside.resolve("secret.txt"), "secret");
    Path gone = Files.createDirectory(tempDir.resolve("gone"));
    Path denied = Files.createDirectory(tempDir.resolve("denied"));
    Path root = Files.createDirectory(tempDir.resolve("work"));
    createJunction(root.resolve("outside"), outside);
    createJunction(root.resolve("gone"), gone);
    Files.delete(gone);
    createJunction(root.resolve("denied"), denied);
    List<AclEntry> deniedAcl = getAcl(denied);
    setAcl(denied, List.of());

    try {
      FileSystemUtils.deleteRecursively(root);
      Assertions.assertFalse(Files.exists(root, LinkOption.NOFOLLOW_LINKS));
      Assertions.assertEquals(List.of(), getAcl(denied));
    } finally {
      setAcl(denied, deniedAcl);
    }
    Assertions.assertEquals("secret", Files.readString(outsideFile));

    Path rootJunction = createJunction(tempDir.resolve("root-junction"), outside);
    FileSystemUtils.deleteRecursively(rootJunction);
    Assertions.assertFalse(Files.exists(rootJunction, LinkOption.NOFOLLOW_LINKS));
    Assertions.assertEquals("secret", Files.readString(outsideFile));
  }

  /**
   * Test case: tests the deleteRecursively(Path) method on Windows. Test data: a tree with a
   * read-only file, a read-only directory and a directory whose ACL a submission emptied, which
   * denies everyone (the judger, its owner, but changing its ACL) any access. Expected result: the
   * whole tree is removed.
   */
  @Test
  @EnabledOnOs(OS.WINDOWS)
  public void testDeleteRecursivelyRemovesReadOnlyAndDeniedFilesOnWindows() throws Exception {
    Path root = Files.createDirectory(tempDir.resolve("work"));
    Path readOnlyFile = Files.writeString(root.resolve("a.exe"), "binary");
    Files.setAttribute(readOnlyFile, "dos:readonly", true);
    Path readOnlyDirectory = Files.createDirectory(root.resolve("read-only"));
    Files.writeString(readOnlyDirectory.resolve("file.txt"), "data");
    Files.setAttribute(readOnlyDirectory, "dos:readonly", true);
    Path denied = Files.createDirectory(root.resolve("denied"));
    Files.writeString(Files.createDirectory(denied.resolve("inner")).resolve("file.txt"), "data");
    setAcl(denied, List.of());
    Assertions.assertThrows(IOException.class, () -> Files.list(denied).close());

    FileSystemUtils.deleteRecursively(root);

    Assertions.assertFalse(Files.exists(root, LinkOption.NOFOLLOW_LINKS));
  }

  /**
   * Creates a directory junction, as a submission may without any privilege.
   *
   * @param link - the junction
   * @param target - the directory it points to
   * @return the junction
   */
  public static Path createJunction(Path link, Path target) throws Exception {
    Process process =
        new ProcessBuilder("cmd", "/c", "mklink", "/J", link.toString(), target.toString())
            .redirectErrorStream(true)
            .start();
    String output = new String(process.getInputStream().readAllBytes());
    Assertions.assertEquals(0, process.waitFor(), output);
    Assertions.assertTrue(
        Files.readAttributes(link, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS).isOther());
    return link;
  }

  /**
   * Reads the ACL of a file (Windows).
   *
   * @param file - the file
   * @return its ACL
   */
  private static List<AclEntry> getAcl(Path file) throws IOException {
    return Files.getFileAttributeView(file, AclFileAttributeView.class).getAcl();
  }

  /**
   * Replaces the ACL of a file (Windows).
   *
   * @param file - the file
   * @param acl - its new ACL
   */
  private static void setAcl(Path file, List<AclEntry> acl) throws IOException {
    Files.getFileAttributeView(file, AclFileAttributeView.class).setAcl(acl);
  }

  /**
   * Test case: tests the newSandboxFileInputStream(Path) method. Test data: a plain regular file.
   * Expected result: its content is read.
   */
  @Test
  public void testNewSandboxFileInputStreamReadsRegularFile() throws Exception {
    Path file = Files.writeString(tempDir.resolve("output#0.txt"), "42\n");
    try (InputStream inputStream = FileSystemUtils.newSandboxFileInputStream(file)) {
      Assertions.assertEquals("42\n", new String(inputStream.readAllBytes()));
    }
  }

  /**
   * Test case: tests the newSandboxFileInputStream(Path) method. Test data: a symbolic link, a hard
   * link, a FIFO and a missing file planted in place of the output. Expected result: each is
   * refused instead of being followed (or blocking).
   */
  @Test
  @DisabledOnOs(OS.WINDOWS)
  public void testNewSandboxFileInputStreamRefusesPlantedFiles() throws Exception {
    Path secret = Files.writeString(tempDir.resolve("secret.txt"), "secret");

    Path symlink = Files.createSymbolicLink(tempDir.resolve("output#1.txt"), secret);
    Assertions.assertThrows(
        IOException.class, () -> FileSystemUtils.newSandboxFileInputStream(symlink));

    Path hardLink = Files.createLink(tempDir.resolve("output#2.txt"), secret);
    Assertions.assertThrows(
        IOException.class, () -> FileSystemUtils.newSandboxFileInputStream(hardLink));

    Path fifo = tempDir.resolve("output#3.txt");
    Process mkfifo = new ProcessBuilder("mkfifo", fifo.toString()).start();
    Assertions.assertEquals(0, mkfifo.waitFor());
    Assertions.assertThrows(
        IOException.class, () -> FileSystemUtils.newSandboxFileInputStream(fifo));

    Assertions.assertThrows(
        IOException.class,
        () -> FileSystemUtils.newSandboxFileInputStream(tempDir.resolve("missing.txt")));
  }

  /** A temporary directory managed by JUnit. */
  @TempDir Path tempDir;
}
