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
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

import com.sun.nio.file.ExtendedOpenOption;

/**
 * An exclusive lock on a lock file, held by this process until it is closed or the process exits
 * (however it exits), which tells judgers on one host apart.
 *
 * <p>The lock is a POSIX record lock on Linux, which belongs to the process, not to the channel
 * that took it: closing <em>any</em> descriptor of the file releases it. A judger takes each of its
 * locks once; should this process try to lock a file it holds (whatever path names it), the JVM
 * refuses the second lock, and the channel opened for it is kept open rather than closed.
 *
 * <p>Lock files belong in a directory only the judger may write to, and that no clean-up of old
 * files touches, so that nobody removes or replaces a lock file while it is held. The file holds
 * the process id of its holder, and only its owner may open it, so that nobody else can take a lock
 * on it that would keep the judger from it.
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
    FileChannel channel = FileChannel.open(lockFile, OPEN_OPTIONS, CREATION_ATTRIBUTES);
    try {
      if (channel.tryLock() == null) {
        channel.close();
        return null;
      }
      if (IS_POSIX) {
        // A lock file others may read could take a (shared) lock that keeps the judger from it. By
        // its path: a descriptor of it opened (and closed) to change its permissions would release
        // the lock.
        Files.setPosixFilePermissions(lockFile, OWNER_ONLY);
      }
      channel.truncate(0);
      channel.write(ByteBuffer.wrap(CONTENT), 0);
      return new ExclusiveFileLock(lockFile, channel);
    } catch (OverlappingFileLockException ex) {
      // This process holds the file already: closing the channel would release that lock.
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
    channel.close();
  }

  /**
   * Constructor.
   *
   * @param file - the lock file
   * @param channel - the open channel of the lock file, which holds the lock
   */
  private ExclusiveFileLock(Path file, FileChannel channel) {
    this.file = file;
    this.channel = channel;
  }

  /** Whether the file system has POSIX permissions (i.e. is not Windows). */
  private static final boolean IS_POSIX =
      FileSystems.getDefault().supportedFileAttributeViews().contains("posix");

  /** The permissions of a lock file: read and write for its owner only (POSIX). */
  private static final Set<PosixFilePermission> OWNER_ONLY =
      PosixFilePermissions.fromString("rw-------");

  /** The attributes a lock file is created with: its permissions (POSIX). */
  private static final FileAttribute<?>[] CREATION_ATTRIBUTES =
      IS_POSIX
          ? new FileAttribute<?>[] {PosixFilePermissions.asFileAttribute(OWNER_ONLY)}
          : new FileAttribute<?>[0];

  /**
   * How a lock file is opened: created if needed, for writing, never through a link. On Windows
   * also without letting it be deleted or renamed while it is open (Java lets it otherwise), as a
   * file locked at a path that names another file meanwhile would not keep another judger from
   * the file there.
   */
  private static final Set<OpenOption> OPEN_OPTIONS =
      IS_POSIX
          ? Set.of(StandardOpenOption.CREATE, StandardOpenOption.WRITE, LinkOption.NOFOLLOW_LINKS)
          : Set.of(
              StandardOpenOption.CREATE,
              StandardOpenOption.WRITE,
              LinkOption.NOFOLLOW_LINKS,
              ExtendedOpenOption.NOSHARE_DELETE);

  /** What a lock file holds: the process id of its holder. */
  private static final byte[] CONTENT =
      (ProcessHandle.current().pid() + "\n").getBytes(StandardCharsets.US_ASCII);

  /**
   * The channels of lock files this process held already when it opened them again: referenced
   * for good, as an unreachable channel is closed when it is garbage collected.
   */
  private static final List<FileChannel> UNCLOSED_CHANNELS = new CopyOnWriteArrayList<>();

  /** The lock file. */
  private final Path file;

  /**
   * The open channel of the lock file, which holds the lock. Referenced for as long as the lock is
   * held: an unreachable channel is closed (and the lock released) when it is garbage collected.
   */
  private final FileChannel channel;
}
