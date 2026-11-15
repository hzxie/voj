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

import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
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

  /** A temporary directory managed by JUnit. */
  @TempDir Path tempDir;
}
