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
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

/**
 * File-system helpers for directories that untrusted (sandboxed) code may have written to.
 *
 * @author Haozhe Xie
 */
public class FileSystemUtils {
  /** Utility classes should not have a public constructor. */
  private FileSystemUtils() {}

  /**
   * Recursively deletes a file or directory <em>without following symbolic links</em>. A symbolic
   * link (including one at {@code path} itself) is removed as a link; its target is never touched.
   * This makes it safe to delete a directory whose contents were written by a submission, which
   * could otherwise plant e.g. {@code output -> /etc} so that a recursive delete running as the
   * judger wipes files outside the directory. Does nothing if {@code path} does not exist.
   *
   * @param path - the file or directory to delete
   * @throws IOException if something cannot be deleted
   */
  public static void deleteRecursively(Path path) throws IOException {
    if (!Files.exists(path, LinkOption.NOFOLLOW_LINKS)) {
      return;
    }
    // Files.walkFileTree does not follow links unless FOLLOW_LINKS is requested: a link (to a file
    // or a directory) is reported through visitFile and is deleted as a link.
    Files.walkFileTree(
        path,
        new SimpleFileVisitor<Path>() {
          @Override
          public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
              throws IOException {
            Files.deleteIfExists(file);
            return FileVisitResult.CONTINUE;
          }

          @Override
          public FileVisitResult postVisitDirectory(Path dir, IOException ex) throws IOException {
            if (ex != null) {
              throw ex;
            }
            Files.deleteIfExists(dir);
            return FileVisitResult.CONTINUE;
          }
        });
  }
}
