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

import java.nio.channels.FileChannel;
import java.nio.channels.OverlappingFileLockException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.FileTime;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;

/**
 * Test class for ExclusiveFileLock.
 *
 * @author Haozhe Xie
 */
@DisabledOnOs(OS.WINDOWS)
public class ExclusiveFileLockTest {
  /**
   * Test case: a lock file locked twice. Expected result: only the first lock is granted, until it
   * is released.
   */
  @Test
  public void testLockIsExclusive() throws Exception {
    Path lockFile = tempDir.resolve("judger-0.lock");

    try (ExclusiveFileLock lock = ExclusiveFileLock.tryAcquire(lockFile)) {
      Assertions.assertNotNull(lock);
      Assertions.assertTrue(lock.isValid());
      Assertions.assertNull(ExclusiveFileLock.tryAcquire(lockFile));
    }
    try (ExclusiveFileLock lock = ExclusiveFileLock.tryAcquire(lockFile)) {
      Assertions.assertNotNull(lock);
    }
  }

  /**
   * Test case: the lock file is removed while it is held (e.g. by a clean-up of old files in /tmp),
   * or replaced by another file. Expected result: the lock no longer counts, as another process
   * could lock the file now at the path.
   */
  @Test
  public void testLockOfARemovedOrReplacedFileIsNotValid() throws Exception {
    Path lockFile = tempDir.resolve("judger-0.lock");

    try (ExclusiveFileLock lock = ExclusiveFileLock.tryAcquire(lockFile)) {
      Files.delete(lockFile);
      Assertions.assertFalse(lock.isValid());
      Files.createFile(lockFile);
      Assertions.assertFalse(lock.isValid());
    }
    try (ExclusiveFileLock lock = ExclusiveFileLock.tryAcquire(lockFile)) {
      Path other = Files.createFile(tempDir.resolve("other"));
      Files.move(other, lockFile, StandardCopyOption.REPLACE_EXISTING);
      Assertions.assertFalse(lock.isValid());
    }
  }

  /**
   * Test case: the lock file is removed between its opening and its locking (e.g. by a clean-up of
   * old files in /tmp). Expected result: no failure; the file now at the path is locked instead.
   */
  @Test
  public void testLockFileRemovedWhileBeingLockedIsLockedAgain() throws Exception {
    Path lockFile = tempDir.resolve("judger-0.lock");
    boolean[] removed = new boolean[1];

    try (ExclusiveFileLock lock =
        ExclusiveFileLock.tryAcquire(
            lockFile,
            path -> {
              if (!removed[0]) {
                removed[0] = true;
                deleteQuietly(path);
              }
            })) {
      Assertions.assertTrue(removed[0]);
      Assertions.assertNotNull(lock);
      Assertions.assertTrue(lock.isValid());
      assertLocked(lockFile);
    }
  }

  /**
   * Test case: the lock file is replaced by another file between its opening and its locking (as
   * another judger may create one after a removal). Expected result: the lock is taken on the file
   * now at the path, not on the replaced one, which guards nothing.
   */
  @Test
  public void testLockFileReplacedWhileBeingLockedIsLockedAgain() throws Exception {
    Path lockFile = tempDir.resolve("judger-0.lock");
    boolean[] replaced = new boolean[1];

    try (ExclusiveFileLock lock =
        ExclusiveFileLock.tryAcquire(
            lockFile,
            path -> {
              if (!replaced[0]) {
                replaced[0] = true;
                try {
                  Path other = Files.createFile(tempDir.resolve("other"));
                  Files.move(other, path, StandardCopyOption.REPLACE_EXISTING);
                } catch (java.io.IOException ex) {
                  throw new java.io.UncheckedIOException(ex);
                }
              }
            })) {
      Assertions.assertTrue(replaced[0]);
      Assertions.assertTrue(lock.isValid());
      assertLocked(lockFile);
    }
  }

  /**
   * Asserts that this process holds the lock of the file at a path. (The probe's channel is closed
   * afterwards, which may release that lock: only for the end of a test.)
   *
   * @param lockFile - the path of the lock file
   */
  private static void assertLocked(Path lockFile) throws Exception {
    try (FileChannel probe = FileChannel.open(lockFile, StandardOpenOption.WRITE)) {
      Assertions.assertThrows(OverlappingFileLockException.class, probe::tryLock);
    }
  }

  /**
   * Deletes a file.
   *
   * @param file - the file
   */
  private static void deleteQuietly(Path file) {
    try {
      Files.delete(file);
    } catch (java.io.IOException ex) {
      throw new java.io.UncheckedIOException(ex);
    }
  }

  /**
   * Test case: a lock file that is still the one locked. Expected result: checking it marks it as
   * just used, so that a clean-up of old files leaves it alone.
   */
  @Test
  public void testCheckingTheLockMarksTheFileAsUsed() throws Exception {
    Path lockFile = tempDir.resolve("judger-0.lock");

    try (ExclusiveFileLock lock = ExclusiveFileLock.tryAcquire(lockFile)) {
      Files.setLastModifiedTime(lockFile, FileTime.fromMillis(0));
      Assertions.assertTrue(lock.isValid());
      Assertions.assertTrue(Files.getLastModifiedTime(lockFile).toMillis() > 0);
    }
  }

  /** A temporary directory managed by JUnit. */
  @TempDir Path tempDir;
}
