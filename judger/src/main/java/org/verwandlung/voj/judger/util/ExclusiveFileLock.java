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
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.OverlappingFileLockException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * An exclusive lock on a lock file, held by this process until it is closed or the process exits
 * (however it exits), which tells judgers on one host apart.
 *
 * <p>The lock is a POSIX record lock on Linux, which belongs to the process, not to the channel
 * that took it: closing <em>any</em> descriptor of the file, e.g. one opened to set its times,
 * releases it. So a file this process holds is never opened again: it is written to only through
 * the channel that holds the lock, and not opened to be locked again, whatever path names it.
 *
 * <p>Lock files belong in a directory only the judger may write to, where nobody else can remove
 * or replace them. A clean-up of old files may still remove one (e.g. the age-based clean-up of
 * {@code /tmp}, where the checkpoint root is by default), and another process would then lock a new
 * file at the same path: {@link #isValid} tells whether the file at the path is still the one
 * locked, and marks it as just used, so that such a clean-up leaves it alone. The file holds the
 * process id of its holder, and only its owner may open it, so that nobody else can take a lock on
 * it that would keep the judger from it.
 *
 * @author Haozhe Xie
 */
public final class ExclusiveFileLock implements Closeable {
  /**
   * Locks a lock file, which is created if needed (never through a link).
   *
   * @param file - the lock file
   * @return the lock, or {@code null} if another process, or this one, holds it already
   * @throws IOException if the lock file cannot be opened or locked
   */
  public static ExclusiveFileLock tryAcquire(Path file) throws IOException {
    Path lockFile = file.toAbsolutePath().normalize();
    synchronized (HELD_LOCK_FILES) {
      if (HELD_LOCK_FILES.contains(identityOf(lockFile))) {
        return null;
      }
      FileChannel channel = FileChannel.open(lockFile, OPEN_OPTIONS, ownerOnly());
      try {
        if (channel.tryLock() == null) {
          channel.close();
          return null;
        }
        ExclusiveFileLock lock = new ExclusiveFileLock(lockFile, channel, fileKeyOf(lockFile));
        channel.truncate(0);
        lock.markAsUsed();
        HELD_LOCK_FILES.add(lock.identity);
        return lock;
      } catch (OverlappingFileLockException ex) {
        // Held by this process after all (moved to this path meanwhile): closing the channel would
        // release that lock, so it is kept open instead.
        UNCLOSED_CHANNELS.add(channel);
        return null;
      } catch (Throwable ex) {
        try {
          channel.close();
        } catch (IOException closeException) {
          ex.addSuppressed(closeException);
        }
        throw ex;
      }
    }
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
      markAsUsed();
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
        HELD_LOCK_FILES.remove(identity);
        channel.close();
      }
    }
  }

  /**
   * Marks the lock file as just used (sets its modification time) by writing the process id of its
   * holder to it, through the channel that holds the lock.
   *
   * @throws IOException if the file cannot be written
   */
  private void markAsUsed() throws IOException {
    channel.write(ByteBuffer.wrap(CONTENT), 0);
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
   * Gets what identifies a lock file among those this process holds, without opening it: its file
   * key, whatever path names it, or its path on a platform without file keys.
   *
   * @param file - the absolute, normalized path of the lock file
   * @return what identifies it, or {@code null} if there is no file at the path
   * @throws IOException if its attributes cannot be read
   */
  private static Object identityOf(Path file) throws IOException {
    try {
      Object fileKey = fileKeyOf(file);
      return fileKey != null ? fileKey : file;
    } catch (NoSuchFileException ex) {
      return null;
    }
  }

  /**
   * Gets the permissions of a new lock file: read and write for its owner only (POSIX).
   *
   * @return the file attributes to create a lock file with
   */
  private static FileAttribute<?>[] ownerOnly() {
    if (!FileSystems.getDefault().supportedFileAttributeViews().contains("posix")) {
      return new FileAttribute<?>[0];
    }
    return new FileAttribute<?>[] {
      PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rw-------"))
    };
  }

  /**
   * Constructor.
   *
   * @param file - the lock file
   * @param channel - the open channel of the lock file, which holds the lock
   * @param fileKey - what identified the lock file when it was locked
   */
  private ExclusiveFileLock(Path file, FileChannel channel, Object fileKey) {
    this.file = file;
    this.channel = channel;
    this.fileKey = fileKey;
    this.identity = fileKey != null ? fileKey : file;
  }

  /** How a lock file is opened: created if needed, for writing, never through a link. */
  private static final Set<OpenOption> OPEN_OPTIONS =
      Set.of(StandardOpenOption.CREATE, StandardOpenOption.WRITE, LinkOption.NOFOLLOW_LINKS);

  /** What a lock file holds: the process id of its holder. */
  private static final byte[] CONTENT =
      (ProcessHandle.current().pid() + "\n").getBytes(StandardCharsets.US_ASCII);

  /** What identifies the lock files this process holds (see {@link #identityOf}). */
  private static final Set<Object> HELD_LOCK_FILES = new HashSet<>();

  /**
   * The channels of lock files this process held already when it opened them again, which are
   * never closed, as closing them would release the lock.
   */
  private static final List<FileChannel> UNCLOSED_CHANNELS = new ArrayList<>();

  /** The lock file. */
  private final Path file;

  /**
   * The open channel of the lock file, which holds the lock. Referenced for as long as the lock is
   * held: an unreachable channel is closed (and the lock released) when it is garbage collected.
   */
  private final FileChannel channel;

  /** What identified the lock file when it was locked (its device and inode on Linux). */
  private final Object fileKey;

  /** What identifies the lock file among those this process holds (see {@link #identityOf}). */
  private final Object identity;
}
