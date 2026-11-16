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
import java.nio.channels.FileLock;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.FileTime;
import java.nio.file.attribute.PosixFilePermissions;

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
   * is released; the file holds the process id of its holder, and only its owner may open it.
   */
  @Test
  public void testLockIsExclusive() throws Exception {
    Path lockFile = tempDir.resolve("judger-0.lock");

    try (ExclusiveFileLock lock = ExclusiveFileLock.tryAcquire(lockFile)) {
      Assertions.assertNotNull(lock);
      Assertions.assertTrue(lock.isValid());
      Assertions.assertNull(ExclusiveFileLock.tryAcquire(lockFile));
    }
    Assertions.assertEquals(ProcessHandle.current().pid() + "\n", Files.readString(lockFile));
    Assertions.assertEquals(
        "rw-------", PosixFilePermissions.toString(Files.getPosixFilePermissions(lockFile)));
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

  /**
   * Test case: a lock file locked by this process and checked (which marks it as just used), then
   * released, while another process (another judger on the host) tries to lock it. Expected
   * result: the other process cannot lock it until it is released: checking the lock never opens
   * (and closes) the file again, which would release the lock.
   */
  @Test
  public void testCheckedLockIsExclusiveAcrossProcesses() throws Exception {
    Path lockFile = tempDir.resolve("judger-0.lock");

    try (ExclusiveFileLock lock = ExclusiveFileLock.tryAcquire(lockFile)) {
      Assertions.assertFalse(isFreeForAnotherProcess(lockFile));
      Assertions.assertTrue(lock.isValid());
      Assertions.assertFalse(isFreeForAnotherProcess(lockFile));
    }
    Assertions.assertTrue(isFreeForAnotherProcess(lockFile));
  }

  /**
   * Test case: a lock file locked by this process is locked again through another path of the same
   * file (a link to its directory), while another process tries to lock it. Expected result: the
   * second lock is not granted, and the other process still cannot lock the file: the failed
   * attempt never opens (and closes) the file again, which would release the lock.
   */
  @Test
  public void testLockTakenAgainIsExclusiveAcrossProcesses() throws Exception {
    Path lockFile = tempDir.resolve("judger-0.lock");
    Path link = Files.createSymbolicLink(tempDir.resolve("link"), tempDir);

    try (ExclusiveFileLock lock = ExclusiveFileLock.tryAcquire(lockFile)) {
      Assertions.assertNull(ExclusiveFileLock.tryAcquire(link.resolve("judger-0.lock")));
      Assertions.assertFalse(isFreeForAnotherProcess(lockFile));
    }
    Assertions.assertTrue(isFreeForAnotherProcess(lockFile));
  }

  /**
   * Tries to lock a lock file in another process (a JVM running {@link OtherProcess}), and
   * releases it.
   *
   * @param lockFile - the lock file
   * @return whether the other process could lock it
   */
  private static boolean isFreeForAnotherProcess(Path lockFile) throws Exception {
    Process process =
        new ProcessBuilder(
                Paths.get(System.getProperty("java.home"), "bin", "java").toString(),
                "-cp",
                System.getProperty("java.class.path"),
                OtherProcess.class.getName(),
                lockFile.toString())
            .redirectErrorStream(true)
            .start();
    String output = new String(process.getInputStream().readAllBytes()).trim();
    Assertions.assertEquals(0, process.waitFor(), output);
    return Boolean.parseBoolean(output);
  }

  /** Another process that tries to lock a lock file. */
  public static final class OtherProcess {
    /**
     * Tries to lock a lock file, and prints whether it could.
     *
     * @param args - the lock file
     */
    public static void main(String[] args) throws Exception {
      try (FileChannel channel = FileChannel.open(Paths.get(args[0]), StandardOpenOption.WRITE);
          FileLock lock = channel.tryLock()) {
        System.out.println(lock != null);
      }
    }
  }

  /** A temporary directory managed by JUnit. */
  @TempDir Path tempDir;
}
