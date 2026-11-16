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
import java.nio.file.AccessDeniedException;
import java.nio.file.DirectoryIteratorException;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.SecureDirectoryStream;
import java.nio.file.attribute.AclEntry;
import java.nio.file.attribute.AclEntryFlag;
import java.nio.file.attribute.AclEntryPermission;
import java.nio.file.attribute.AclEntryType;
import java.nio.file.attribute.AclFileAttributeView;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.PosixFileAttributes;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.UserPrincipal;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import com.sun.security.auth.module.NTSystem;
import com.sun.security.auth.module.UnixSystem;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * File-system helpers for directories that untrusted (sandboxed) code may have written to.
 *
 * @author Haozhe Xie
 */
public class FileSystemUtils {
  /** Utility classes should not have a public constructor. */
  private FileSystemUtils() {}

  /**
   * Opens a file a program wrote (its output or the compile log) for reading, refusing anything but
   * a regular file with a single link. The sandbox creates these files for the judger, in a
   * directory no program may write to; this is defence in depth against a link or a FIFO in their
   * place, read with the judger's privileges. Every program is gone by the time it is read.
   *
   * @param path - the file to open
   * @return an input stream over the file
   * @throws IOException if the file does not exist, is not a regular file, has more than one link,
   *     or cannot be opened
   */
  public static InputStream newSandboxFileInputStream(Path path) throws IOException {
    BasicFileAttributes attributes =
        Files.readAttributes(path, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
    if (!attributes.isRegularFile()) {
      throw new IOException("Refusing to read a non-regular sandbox file: " + path);
    }
    if (!IS_WINDOWS) {
      Object numberOfLinks = Files.getAttribute(path, "unix:nlink", LinkOption.NOFOLLOW_LINKS);
      if (numberOfLinks instanceof Integer && (Integer) numberOfLinks != 1) {
        throw new IOException("Refusing to read a hard-linked sandbox file: " + path);
      }
    }
    return Files.newInputStream(path, LinkOption.NOFOLLOW_LINKS);
  }

  /**
   * Recursively deletes a file or directory <em>without following symbolic links</em>. A symbolic
   * link (including one at {@code path} itself) is removed as a link; its target is never touched.
   * This makes it safe to delete a directory whose contents were written by a submission, which
   * could otherwise plant e.g. {@code output -> /etc} so that a recursive delete running as the
   * judger wipes files outside the directory. Does nothing if {@code path} does not exist.
   *
   * <p>Nor can a submission make the directory impossible to delete. It may nest directories
   * deeper than a path may be long ({@code PATH_MAX}), by creating a directory and entering it
   * again and again, so the tree is never walked by path: every directory is opened, read and
   * emptied relative to the open handle of its parent ({@link SecureDirectoryStream}, i.e. {@code
   * openat}, {@code unlinkat} and {@code renameat}), and the subdirectories of a directory being
   * emptied are moved up into {@code path} first, to be emptied in turn. So no directory handled
   * is ever more than two levels below {@code path}, and only two handles are open at a time,
   * however deep the tree. A submission that runs as the judger's own account (that of a judger
   * that is not root) may also take its directories' permissions away (e.g. {@code chmod 000}):
   * when the judger is denied access to a directory, it gives the directory's owner full access to
   * it again and tries once more. Root is never denied access, so a judger running as root never
   * changes a permission. Where there are no directory handles (Windows, whose paths have no such
   * limit), the tree is walked by path (see {@link #deleteTreeByPath}). There, a submission may
   * also deny the judger access to what it creates, by the ACL it creates it with (as its owner, it
   * may set any), so a judger that can enables its backup privileges first (see {@link
   * #enableBackupPrivilegesOnce}), which no ACL withstands.
   *
   * @param path - the file or directory to delete
   * @throws IOException if something cannot be deleted
   */
  public static void deleteRecursively(Path path) throws IOException {
    Path file = path.toAbsolutePath();
    if (IS_WINDOWS) {
      enableBackupPrivilegesOnce();
      // Never opened as a directory first: it may be a junction.
      deleteTreeByPath(file);
      return;
    }
    BasicFileAttributes attributes;
    try {
      attributes = Files.readAttributes(file, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
    } catch (NoSuchFileException ex) {
      return;
    }
    if (attributes.isDirectory() && !emptyDirectory(file)) {
      deleteTreeByPath(file);
      return;
    }
    // A plain file, a link or a directory emptied above, removed without following a link.
    Files.deleteIfExists(file);
  }

  /**
   * Empties a directory through directory handles.
   *
   * @param directory - the absolute path of the directory
   * @return whether the directory was emptied, {@code false} if the platform has no {@link
   *     SecureDirectoryStream}
   * @throws IOException if the directory cannot be emptied
   */
  private static boolean emptyDirectory(Path directory) throws IOException {
    boolean empty;
    do {
      DirectoryStream<Path> stream =
          withOwnerAccess(() -> Files.newDirectoryStream(directory), directory);
      if (!(stream instanceof SecureDirectoryStream)) {
        stream.close();
        return false;
      }
      try (SecureDirectoryStream<Path> handle = (SecureDirectoryStream<Path>) stream) {
        ensureIsDirectoryAt(handle, directory);
        empty = true;
        // The directories moved up into it meanwhile may or may not be listed: whatever is left
        // is listed again, until nothing is.
        for (Path entry : handle) {
          empty = false;
          deleteEntry(handle, directory, entry.getFileName());
        }
      } catch (DirectoryIteratorException ex) {
        throw ex.getCause();
      }
    } while (!empty);
    return true;
  }

  /**
   * Deletes an entry of the directory being emptied; a directory once its files are deleted and
   * its subdirectories moved up into the directory being emptied (which deletes them in turn).
   *
   * @param handle - the handle of the directory being emptied
   * @param directory - the absolute path of the directory being emptied
   * @param name - the name of the entry
   * @throws IOException if the entry cannot be deleted
   */
  private static void deleteEntry(SecureDirectoryStream<Path> handle, Path directory, Path name)
      throws IOException {
    BasicFileAttributes attributes = readAttributes(handle, name, directory);
    if (attributes == null) {
      return;
    }
    if (!attributes.isDirectory()) {
      withOwnerAccess(() -> deleteFile(handle, name), directory);
      return;
    }
    Path child = directory.resolve(name);
    try (SecureDirectoryStream<Path> childHandle =
        withOwnerAccess(
            () -> handle.newDirectoryStream(name, LinkOption.NOFOLLOW_LINKS), directory, child)) {
      for (Path entry : childHandle) {
        Path grandchild = entry.getFileName();
        BasicFileAttributes grandchildAttributes = readAttributes(childHandle, grandchild, child);
        if (grandchildAttributes == null) {
          continue;
        }
        if (grandchildAttributes.isDirectory()) {
          // Moving a directory into another one also takes write access to the directory itself.
          Path movedName = name.getFileSystem().getPath(UUID.randomUUID().toString());
          withOwnerAccess(
              () -> {
                childHandle.move(grandchild, handle, movedName);
                return null;
              },
              directory,
              child,
              child.resolve(grandchild));
        } else {
          withOwnerAccess(() -> deleteFile(childHandle, grandchild), child);
        }
      }
    } catch (DirectoryIteratorException ex) {
      throw ex.getCause();
    }
    try {
      withOwnerAccess(
          () -> {
            handle.deleteDirectory(name);
            return null;
          },
          directory);
    } catch (DirectoryNotEmptyException ex) {
      // Something was created in it meanwhile: it is listed (and emptied) again.
    }
  }

  /**
   * Reads the attributes of an entry of a directory, without following a link.
   *
   * @param handle - the handle of the directory
   * @param name - the name of the entry
   * @param directory - the absolute path of the directory
   * @return the attributes, or {@code null} if the entry no longer exists
   * @throws IOException if the attributes cannot be read
   */
  private static BasicFileAttributes readAttributes(
      SecureDirectoryStream<Path> handle, Path name, Path directory) throws IOException {
    try {
      return withOwnerAccess(
          () ->
              handle
                  .getFileAttributeView(
                      name, BasicFileAttributeView.class, LinkOption.NOFOLLOW_LINKS)
                  .readAttributes(),
          directory);
    } catch (NoSuchFileException ex) {
      return null;
    }
  }

  /**
   * Deletes a file (or a link, never its target) of a directory, if it still exists.
   *
   * @param handle - the handle of the directory
   * @param name - the name of the file
   * @return {@code null}
   * @throws IOException if the file cannot be deleted
   */
  private static Void deleteFile(SecureDirectoryStream<Path> handle, Path name)
      throws IOException {
    try {
      handle.deleteFile(name);
    } catch (NoSuchFileException ex) {
      // Deleted already.
    }
    return null;
  }

  /**
   * Makes sure that a handle opened by path is that of the directory at the path, not of the
   * target of a link put in its place.
   *
   * @param handle - the handle
   * @param directory - the path it was opened by
   * @throws IOException if the path is not the directory opened
   */
  private static void ensureIsDirectoryAt(SecureDirectoryStream<Path> handle, Path directory)
      throws IOException {
    Object openedKey =
        handle.getFileAttributeView(BasicFileAttributeView.class).readAttributes().fileKey();
    BasicFileAttributes attributes =
        Files.readAttributes(directory, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
    if (!attributes.isDirectory() || !Objects.equals(openedKey, attributes.fileKey())) {
      throw new IOException("Refusing to delete " + directory + ": it was replaced meanwhile.");
    }
  }

  /**
   * Performs a file operation and, if access is denied, gives the judger full access to the files
   * involved (see {@link #grantOwnerAccess}) and performs it once more.
   *
   * @param operation - the operation
   * @param files - the absolute paths of the files involved (no links), parents first
   * @return the result of the operation
   * @throws IOException if the operation fails
   */
  private static <T> T withOwnerAccess(FileOperation<T> operation, Path... files)
      throws IOException {
    try {
      return operation.perform();
    } catch (AccessDeniedException ex) {
      boolean granted = false;
      for (Path file : files) {
        granted |= grantOwnerAccess(file);
      }
      if (!granted) {
        throw ex;
      }
      return operation.perform();
    }
  }

  /**
   * Gives the judger full access to a file it is denied access to. On POSIX systems, the owner of
   * a directory gets full access ({@code rwx}) to it, if the owner lacks any. On Windows, the ACL
   * of the file, which a submission may have set, is replaced by one that gives the judger full
   * control (which it may do if it owns the file or may change its ACL; failing that, it takes the
   * file first, which its take-ownership privilege lets it), and its read-only attribute, which
   * keeps it from being deleted, is cleared. Never applied to a link or a junction: on Windows,
   * that would change its target.
   *
   * @param file - the absolute path of the file: on POSIX systems short, as it is at most two
   *     levels below the directory being deleted
   * @return whether the access was changed
   */
  private static boolean grantOwnerAccess(Path file) {
    if (IS_WINDOWS) {
      boolean granted = false;
      try {
        UserPrincipal judger = getJudgerAccount();
        AclFileAttributeView view =
            Files.getFileAttributeView(file, AclFileAttributeView.class, LinkOption.NOFOLLOW_LINKS);
        List<AclEntry> acl =
            List.of(
                AclEntry.newBuilder()
                    .setType(AclEntryType.ALLOW)
                    .setPrincipal(judger)
                    .setPermissions(AclEntryPermission.values())
                    .build());
        try {
          view.setAcl(acl);
        } catch (IOException ex) {
          view.setOwner(judger);
          view.setAcl(acl);
        }
        granted = true;
      } catch (IOException ex) {
        // Not the judger's to change: perhaps its read-only attribute only.
      }
      try {
        Files.setAttribute(file, "dos:readonly", false, LinkOption.NOFOLLOW_LINKS);
        granted = true;
      } catch (IOException ex) {
        // Denied too.
      }
      return granted;
    }
    try {
      PosixFileAttributes attributes =
          Files.readAttributes(file, PosixFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
      Set<PosixFilePermission> permissions = attributes.permissions();
      if (!attributes.isDirectory() || permissions.containsAll(OWNER_PERMISSIONS)) {
        return false;
      }
      permissions.addAll(OWNER_PERMISSIONS);
      // By path: through a handle, the directory would have to be opened, i.e. readable.
      Files.setPosixFilePermissions(file, permissions);
      return true;
    } catch (IOException | UnsupportedOperationException ex) {
      return false;
    }
  }

  /**
   * Deletes a file or directory tree by path, where there are no directory handles (Windows), if
   * it exists. A link, or a directory junction, which a submission may create without any
   * privilege, is removed as a link: never listed, nor anything done to it but its removal, which
   * would reach its target (a junction to a missing target cannot even be listed). Java reports a
   * junction (as any reparse point but a symbolic link) as a directory and as "other", and a
   * symbolic link as neither. Anything else the judger is denied, it takes back (see {@link
   * #grantOwnerAccess}). A directory is listed again once the entries it had are deleted, until it
   * is empty, rather than entered recursively, however deep the tree.
   *
   * @param path - the absolute path of the file or directory
   * @throws IOException if something cannot be deleted
   */
  private static void deleteTreeByPath(Path path) throws IOException {
    Deque<Path> pending = new ArrayDeque<>();
    pending.push(path);
    while (!pending.isEmpty()) {
      Path file = pending.peek();
      BasicFileAttributes attributes;
      try {
        attributes =
            Files.readAttributes(file, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
      } catch (NoSuchFileException ex) {
        pending.pop();
        continue;
      }
      if (attributes.isSymbolicLink() || attributes.isOther()) {
        Files.deleteIfExists(file);
        pending.pop();
        continue;
      }
      if (attributes.isDirectory()) {
        int entries = pending.size();
        try (DirectoryStream<Path> stream =
            withOwnerAccess(() -> Files.newDirectoryStream(file), file)) {
          stream.forEach(pending::push);
        } catch (DirectoryIteratorException ex) {
          throw ex.getCause();
        }
        if (pending.size() > entries) {
          continue;
        }
      }
      withOwnerAccess(() -> Files.deleteIfExists(file), file);
      pending.pop();
    }
  }

  /**
   * Makes a directory tree that sandboxed code wrote (the program directory, once compiled) the
   * judger's, which the sandbox may then only read and execute: it can no longer change, remove,
   * rename or create anything in it. The tree is walked without following links, parents before
   * what is in them.
   *
   * <ul>
   *   <li>On POSIX systems, a judger that runs as root takes what is not its own yet ({@code
   *       lchown} to root), and every directory gets mode {@code 0755}, every file its mode without
   *       the write permission of the group and others, the set-user-ID, set-group-ID and sticky
   *       bits, plus the read permission of everyone: so the sandbox user, now one of the others,
   *       may read and execute it, but not write it. A file the judger owns already, such as a
   *       link to a file of the Go build cache seed, keeps its mode but for those write
   *       permissions, which it does not have anyway: the seed is never changed. A judger that is
   *       not root runs submissions as its own user, whom no permission keeps from writing: the
   *       sandbox's Landlock rules do (a run may write only to its own work directory), and the
   *       modes are set all the same. Symbolic links are left as they are.
   *   <li>On Windows, the judger takes every file and directory (with its take-ownership
   *       privilege, which no ACL withstands) and replaces its ACL by one that gives the judger
   *       full control and {@code reader} read and execute access, and nothing else: as their
   *       owner, the sandbox account could otherwise give itself any access again. A link or a
   *       junction is removed (it is never followed): no program needs one.
   * </ul>
   *
   * @param directory - the root of the tree
   * @param reader - the account that may read the tree (Windows; ignored elsewhere)
   * @throws IOException if anything in the tree cannot be made read-only
   */
  public static void makeTreeReadOnly(Path directory, UserPrincipal reader) throws IOException {
    Path root = directory.toAbsolutePath();
    if (IS_WINDOWS) {
      enableBackupPrivilegesOnce();
    }
    boolean isRoot = isRunningAsRoot();
    Deque<Path> pending = new ArrayDeque<>();
    pending.push(root);
    while (!pending.isEmpty()) {
      Path file = pending.pop();
      BasicFileAttributes attributes =
          Files.readAttributes(file, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
      if (IS_WINDOWS) {
        if (attributes.isSymbolicLink() || attributes.isOther()) {
          if (file.equals(root)) {
            throw new IOException("Refusing to make a link read-only: " + file);
          }
          Files.delete(file);
          continue;
        }
        makeReadOnlyOnWindows(file, attributes.isDirectory(), reader);
      } else if (!attributes.isSymbolicLink()) {
        makeReadOnlyOnPosix(file, attributes.isDirectory(), isRoot);
      } else if (isRoot) {
        Files.setAttribute(file, "unix:uid", 0, LinkOption.NOFOLLOW_LINKS);
        Files.setAttribute(file, "unix:gid", 0, LinkOption.NOFOLLOW_LINKS);
      }
      if (attributes.isDirectory()) {
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(file)) {
          stream.forEach(pending::push);
        } catch (DirectoryIteratorException ex) {
          throw ex.getCause();
        }
      }
    }
  }

  /**
   * Makes a file or directory read-only for everybody but its owner, the judger (POSIX; see {@link
   * #makeTreeReadOnly}).
   *
   * @param file - the file or directory (not a link)
   * @param isDirectory - whether it is a directory
   * @param isRoot - whether the judger runs as root, and takes what is not its own
   * @throws IOException if its owner or mode cannot be changed
   */
  private static void makeReadOnlyOnPosix(Path file, boolean isDirectory, boolean isRoot)
      throws IOException {
    int mode = (Integer) Files.getAttribute(file, "unix:mode", LinkOption.NOFOLLOW_LINKS) & 07777;
    int owner = (Integer) Files.getAttribute(file, "unix:uid", LinkOption.NOFOLLOW_LINKS);
    int newMode;
    if (isRoot && owner != 0) {
      // Taken before its mode is set: chown clears the set-user-ID and set-group-ID bits anyway.
      Files.setAttribute(file, "unix:uid", 0, LinkOption.NOFOLLOW_LINKS);
      Files.setAttribute(file, "unix:gid", 0, LinkOption.NOFOLLOW_LINKS);
      newMode = isDirectory ? 0755 : (mode & 0755) | 0444;
    } else if (isDirectory) {
      newMode = 0755;
    } else {
      newMode = mode & ~(07000 | 0022);
    }
    if (newMode != mode) {
      Files.setAttribute(file, "unix:mode", newMode, LinkOption.NOFOLLOW_LINKS);
    }
  }

  /**
   * Makes a file or directory the judger's, which another account may only read and execute
   * (Windows; see {@link #makeTreeReadOnly}).
   *
   * @param file - the file or directory (not a link)
   * @param isDirectory - whether it is a directory: its entries are then passed on to what is
   *     created in it (nothing is, any more)
   * @param reader - the account that may read it
   * @throws IOException if its owner or ACL cannot be changed
   */
  private static void makeReadOnlyOnWindows(Path file, boolean isDirectory, UserPrincipal reader)
      throws IOException {
    UserPrincipal judger = getJudgerAccount();
    AclFileAttributeView view =
        Files.getFileAttributeView(file, AclFileAttributeView.class, LinkOption.NOFOLLOW_LINKS);
    Set<AclEntryFlag> flags =
        isDirectory
            ? EnumSet.of(AclEntryFlag.FILE_INHERIT, AclEntryFlag.DIRECTORY_INHERIT)
            : EnumSet.noneOf(AclEntryFlag.class);
    view.setOwner(judger);
    view.setAcl(
        List.of(
            AclEntry.newBuilder()
                .setType(AclEntryType.ALLOW)
                .setPrincipal(judger)
                .setPermissions(AclEntryPermission.values())
                .setFlags(flags)
                .build(),
            AclEntry.newBuilder()
                .setType(AclEntryType.ALLOW)
                .setPrincipal(reader)
                .setPermissions(READ_AND_EXECUTE)
                .setFlags(flags)
                .build()));
  }

  /** What the sandbox account may do with a program directory once it is compiled (Windows). */
  private static final Set<AclEntryPermission> READ_AND_EXECUTE =
      EnumSet.of(
          AclEntryPermission.READ_DATA,
          AclEntryPermission.READ_NAMED_ATTRS,
          AclEntryPermission.EXECUTE,
          AclEntryPermission.READ_ATTRIBUTES,
          AclEntryPermission.READ_ACL,
          AclEntryPermission.SYNCHRONIZE);

  /** A file operation, which may be denied access. */
  @FunctionalInterface
  private interface FileOperation<T> {
    /**
     * Performs the operation.
     *
     * @return its result
     * @throws IOException if it fails
     */
    T perform() throws IOException;
  }

  /**
   * Tells whether the judger runs as root (uid 0), as the native sandbox tells it: then it takes
   * what is not its own, and runs submissions as the sandbox account.
   *
   * @return whether the judger runs as root (never on Windows)
   */
  public static boolean isRunningAsRoot() {
    return !IS_WINDOWS && new UnixSystem().getUid() == 0;
  }

  /**
   * Gets the account the judger runs as, on Windows: the user of its process's token (as {@link
   * NTSystem} reads it: that of the calling thread, which the judger's threads do not
   * impersonate, or else of the process), looked up by its name qualified with its domain (e.g.
   * "HOST\judger", "NT AUTHORITY\SYSTEM" or "NT SERVICE\voj-judger"). Java's {@code user.name} is
   * the bare name of the account, which a domain account and a local one may share, and not even
   * an account's for some (that of SYSTEM is the computer's).
   *
   * @return the judger's account
   * @throws IOException if the account cannot be told or looked up
   */
  public static synchronized UserPrincipal getJudgerAccount() throws IOException {
    if (judgerAccount == null) {
      String name;
      try {
        NTSystem system = new NTSystem();
        String domain = system.getDomain();
        // Well-known accounts such as Everyone have no domain.
        name =
            domain == null || domain.isEmpty()
                ? system.getName()
                : domain + "\\" + system.getName();
      } catch (RuntimeException | LinkageError ex) {
        throw new IOException("Cannot tell which account the judger runs as.", ex);
      }
      judgerAccount =
          FileSystems.getDefault().getUserPrincipalLookupService().lookupPrincipalByName(name);
    }
    return judgerAccount;
  }

  /**
   * Enables the judger's backup privileges (Windows), once: with them, Windows lets it list,
   * delete and take what a submission left, whatever its ACL (see {@link #enableBackupPrivileges}).
   * They are an administrator's, so a judger that does not run as an elevated administrator goes
   * without them, and may have to leave what it is denied behind (see {@link #grantOwnerAccess}).
   */
  private static synchronized void enableBackupPrivilegesOnce() {
    if (backupPrivilegesEnabled != null) {
      return;
    }
    Throwable failure = null;
    try {
      NativeLibraryLoader.loadLibrary(NATIVE_LIBRARY);
      backupPrivilegesEnabled = enableBackupPrivileges();
    } catch (IOException | RuntimeException | UnsatisfiedLinkError ex) {
      backupPrivilegesEnabled = false;
      failure = ex;
    }
    if (!backupPrivilegesEnabled) {
      LOGGER.warn(
          "The judger holds no backup privileges (it does not run as an elevated administrator):"
              + " it may be unable to remove what a submission denies it access to.",
          failure);
    }
  }

  /**
   * Enables the backup, restore and take-ownership privileges of the judger's process (Windows),
   * for good. With them, a file opened for backup, as Windows opens a file to delete it and a
   * directory to list it, is the judger's to list, delete and take, whatever its ACL.
   *
   * @return whether all of them are enabled, which they are not if the judger does not hold them
   */
  private static native boolean enableBackupPrivileges();


  /** The native library that holds the native methods. */
  private static final String NATIVE_LIBRARY = "JudgerCore";

  /** The account the judger runs as (Windows), once looked up. */
  private static UserPrincipal judgerAccount;

  /** Whether the judger's backup privileges are enabled (Windows), once it tried. */
  private static Boolean backupPrivilegesEnabled;

  /** Whether the judger runs on Windows. */
  private static final boolean IS_WINDOWS = System.getProperty("os.name").contains("Windows");

  /** The logger. */
  private static final Logger LOGGER = LogManager.getLogger(FileSystemUtils.class);

  /** The permissions the owner of a directory needs to empty it. */
  private static final Set<PosixFilePermission> OWNER_PERMISSIONS =
      EnumSet.of(
          PosixFilePermission.OWNER_READ,
          PosixFilePermission.OWNER_WRITE,
          PosixFilePermission.OWNER_EXECUTE);
}
