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

import java.io.Closeable;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.OverlappingFileLockException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;

/**
 * An exclusive lock on a lock file, held by this process until it is closed or the process exits
 * (however it exits), which tells judgers on one host apart.
 *
 * <p>The lock belongs to the file, not to its path: if the file is removed (e.g. by a clean-up of
 * old files in {@code /tmp}), another process creates a new file at the same path and locks that
 * one. So the holder checks with {@link #isValid} that the file at the path is still the one it
 * locked, before it relies on the lock.
 *
 * @author Haozhe Xie
 */
public final class ExclusiveFileLock implements Closeable {
  /**
   * Locks a lock file, which is created if needed.
   *
   * <p>The file may be removed or replaced between its opening and its locking, and what was
   * locked then guards nothing: another process may lock the file now at the path. So once the
   * file is locked, the file at the path must be the one locked, i.e. open in this process (Java
   * cannot tell the identity of the file a channel has open, but Linux lists the files a process
   * has open in {@code /proc/self/fd}, and this process opens a lock file through its lock only);
   * if it is not, or there is none, the file at the path is opened and locked again.
   *
   * @param file - the lock file
   * @return the lock, or {@code null} if another process, or this one, holds it already
   * @throws IOException if the lock file cannot be opened
   */
  public static ExclusiveFileLock tryAcquire(Path file) throws IOException {
    return tryAcquire(file, lockFile -> {});
  }

  /**
   * Locks a lock file, which is created if needed.
   *
   * @param file - the lock file
   * @param afterLocking - what happens right after the lock file is locked (for tests)
   * @return the lock, or {@code null} if another process, or this one, holds it already
   * @throws IOException if the lock file cannot be opened
   */
  static ExclusiveFileLock tryAcquire(Path file, Consumer<Path> afterLocking) throws IOException {
    Path lockFile = file.toAbsolutePath().normalize();
    // A file lock is held by the whole process, and closing any channel of a locked file may drop
    // it: never open a lock file this process holds (e.g. two judgers in one JVM in a test).
    synchronized (HELD_LOCK_FILES) {
      if (HELD_LOCK_FILES.contains(lockFile)) {
        return null;
      }
      for (int attempt = 1; attempt <= MAXIMUM_ATTEMPTS; ++attempt) {
        FileChannel channel =
            FileChannel.open(
                lockFile,
                StandardOpenOption.CREATE,
                StandardOpenOption.WRITE,
                LinkOption.NOFOLLOW_LINKS);
        try {
          if (!tryLock(channel)) {
            channel.close();
            return null;
          }
          afterLocking.accept(lockFile);
          Object fileKey = fileKeyOfLocked(lockFile);
          if (fileKey != NOT_LOCKED) {
            ExclusiveFileLock lock = new ExclusiveFileLock(lockFile, channel, fileKey);
            HELD_LOCK_FILES.add(lockFile);
            return lock;
          }
          channel.close();
        } catch (Throwable ex) {
          try {
            channel.close();
          } catch (IOException closeException) {
            ex.addSuppressed(closeException);
          }
          throw ex;
        }
      }
      throw new IOException(
          String.format(
              "The lock file %s was removed or replaced each of the %d times it was locked.",
              lockFile, MAXIMUM_ATTEMPTS));
    }
  }

  /**
   * Locks the file a channel has open, unless another process, or this one, holds it.
   *
   * @param channel - the channel
   * @return whether the file is locked now
   * @throws IOException if the file cannot be locked
   */
  private static boolean tryLock(FileChannel channel) throws IOException {
    try {
      return channel.tryLock() != null;
    } catch (OverlappingFileLockException ex) {
      return false;
    }
  }

  /**
   * Gets the file key of the lock file just locked, i.e. of the file at its path, if that is the
   * file locked.
   *
   * @param lockFile - the path of the lock file
   * @return the file key ({@code null} if the platform has none), or {@link #NOT_LOCKED} if the
   *     file at the path is not the one locked (or there is none)
   * @throws IOException if the files open in this process cannot be listed
   */
  private static Object fileKeyOfLocked(Path lockFile) throws IOException {
    Object fileKey;
    try {
      fileKey = fileKeyOf(lockFile);
    } catch (NoSuchFileException ex) {
      return NOT_LOCKED;
    }
    // No file key on Windows, where a file cannot be removed while it is open.
    if (fileKey == null || !Files.isDirectory(OPEN_FILES)) {
      return fileKey;
    }
    try (DirectoryStream<Path> descriptors = Files.newDirectoryStream(OPEN_FILES)) {
      for (Path descriptor : descriptors) {
        try {
          // Follows the descriptor to the file it has open, even if that was removed.
          if (fileKey.equals(
              Files.readAttributes(descriptor, BasicFileAttributes.class).fileKey())) {
            return fileKey;
          }
        } catch (IOException ex) {
          // Closed meanwhile.
        }
      }
    }
    return NOT_LOCKED;
  }

  /**
   * Checks that the lock is still held and still guards the file at its path, i.e. the file was
   * neither removed nor replaced since it was locked, and marks the file as just used, so that a
   * clean-up of old files leaves it alone.
   *
   * @return whether the lock still guards the file at its path
   */
  public boolean isValid() {
    if (!channel.isOpen()) {
      return false;
    }
    try {
      // No file key on Windows, where a file cannot be removed while it is open.
      if (fileKey != null && !fileKey.equals(fileKeyOf(file))) {
        return false;
      }
      Files.setLastModifiedTime(file, FileTime.fromMillis(System.currentTimeMillis()));
      return true;
    } catch (IOException ex) {
      return false;
    }
  }

  /**
   * Gets the lock file.
   *
   * @return the (absolute) path of the lock file
   */
  public Path getFile() {
    return file;
  }

  /** Releases the lock. */
  @Override
  public void close() throws IOException {
    synchronized (HELD_LOCK_FILES) {
      if (channel.isOpen()) {
        HELD_LOCK_FILES.remove(file);
        channel.close();
      }
    }
  }

  /**
   * Gets what identifies a file (its device and inode on Linux), without following a link.
   *
   * @param file - the file
   * @return the file key, or {@code null} if the platform has none
   * @throws IOException if the file does not exist
   */
  private static Object fileKeyOf(Path file) throws IOException {
    return Files.readAttributes(file, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS)
        .fileKey();
  }

  /**
   * Constructor.
   *
   * @param file - the lock file
   * @param channel - the open channel of the lock file, which holds the lock
   * @param fileKey - what identified the lock file when it was locked
   */
  private ExclusiveFileLock(Path file, FileChannel channel, Object fileKey) {
    this.file = Objects.requireNonNull(file);
    this.channel = channel;
    this.fileKey = fileKey;
  }

  /** The lock files this process holds. */
  private static final Set<Path> HELD_LOCK_FILES = new HashSet<>();

  /** How many times a lock file is opened and locked before its removal or replacement fails. */
  private static final int MAXIMUM_ATTEMPTS = 10;

  /** The directory (on Linux) of the descriptors of the files this process has open. */
  private static final Path OPEN_FILES = Paths.get("/proc/self/fd");

  /** Stands for a lock file whose path is not the file locked. */
  private static final Object NOT_LOCKED = new Object();

  /** The lock file. */
  private final Path file;

  /**
   * The open channel of the lock file, which holds the lock. Referenced for as long as the lock is
   * held: an unreachable channel is closed (and the lock released) when it is garbage collected.
   */
  private final FileChannel channel;

  /** What identified the lock file when it was locked (its device and inode on Linux). */
  private final Object fileKey;
}
