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
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.AclEntry;
import java.nio.file.attribute.AclEntryFlag;
import java.nio.file.attribute.AclEntryPermission;
import java.nio.file.attribute.AclEntryType;
import java.nio.file.attribute.AclFileAttributeView;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.nio.file.attribute.UserPrincipal;
import java.nio.file.attribute.UserPrincipalLookupService;
import java.nio.file.attribute.UserPrincipalNotFoundException;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import org.verwandlung.voj.judger.model.Checkpoint;
import org.verwandlung.voj.judger.model.Problem;
import org.verwandlung.voj.judger.util.ExclusiveFileLock;
import org.verwandlung.voj.judger.util.FileSystemUtils;

/**
 * A local, version-aware cache of problem checkpoints (test data).
 *
 * <p>The database remains the source of truth, but a judger no longer reads the input/output of
 * every checkpoint from the database on each submission. Instead it keeps a per-problem copy and
 * re-downloads (via {@link HttpCheckpointProvider}) only when the problem's {@code
 * checkpoints_version} changes — the version the web side bumps whenever the test data is edited.
 *
 * <p>A fresh copy is downloaded into a private temporary directory and only then swapped into
 * place, so an interrupted or failed download never leaves a half-written cache that could be
 * mistaken for valid. A temporary directory left behind by a judger that died while downloading is
 * removed when the cache is opened at the next start.
 *
 * <p>Every judger on a host claims a slot <i>n</i> of its own under the checkpoint root (its real
 * path, so that every spelling of it agrees): the first one whose lock file {@code
 * judger-<n>.lock} there no other process holds. It holds that lock until it exits, and a
 * restarted judger usually gets its old slot back. The slot is two directories next to its lock
 * file:
 *
 * <ul>
 *   <li>its cache, {@code judger-<n>/}, so that several judgers on one host never replace a
 *       problem's copy while another one reads it. Within a judger, judging is serialised (see
 *       {@code Dispatcher}: one judging at a time), so a problem's copy is never replaced while it is read.
 *   <li>its work directory, {@code work-<n>/}, which holds the work directory of each judging (see
 *       {@link #getWorkDirectory}). A judger that died while judging (killed, out of memory) leaves
 *       the work directory of that judging behind, with a submission's source and binary in it: it
 *       is removed when the slot is claimed again.
 * </ul>
 *
 * <p>The checkpoint root belongs to the judger and only it may write to it (the judger refuses it
 * otherwise), so nobody else can create, remove or replace anything in it. It is a directory for
 * the state of a service, not a temporary one, so no clean-up of old files removes anything in it
 * either: what the judger finds there when it claims its slot stays there while it runs.
 *
 * @author Haozhe Xie
 */
@Component
public class CheckpointStore {
  /** The manifest entry, written by the web side, that records the version and per-checkpoint meta. */
  private static final String MANIFEST_FILE = "manifest.properties";

  /** The most judgers on one host that can share a checkpoint root. */
  static final int MAX_SLOTS = 64;

  /**
   * Opens this judger's cache directory and work directory, claiming a slot under the checkpoint
   * root, and removes the temporary directories of downloads that never completed and the work
   * directories of judgings that never completed. Called at startup; {@link #sync} does it on
   * first use otherwise. The slot is claimed once, and kept until the judger exits.
   *
   * @return this judger's cache directory
   * @throws IOException if the checkpoint root cannot be trusted, every slot is taken, or the
   *     slot's directories cannot be prepared
   */
  public synchronized Path open() throws IOException {
    if (slotLock != null) {
      return cacheDirectory;
    }
    Path root = openCheckpointRoot();
    slotLock =
        claimSlot(
            root,
            number -> {
              Path cache = root.resolve("judger-" + number);
              Path work = root.resolve("work-" + number);
              if (!Files.exists(cache, LinkOption.NOFOLLOW_LINKS)) {
                createDirectory(cache, PRIVATE);
              }
              ensureOwnedByJudger(cache, "judger.checkpointDir");
              removeTemporaryDirectories(cache);
              removeLeftovers(root, number);
              createWorkDirectory(work);
              cacheDirectory = cache;
              judgerWorkDirectory = work;
            });
    LOGGER.info("Caching checkpoints in {}, judging in {}", cacheDirectory, judgerWorkDirectory);
    if (!obsoleteWorkDirectory.isBlank()) {
      LOGGER.warn(
          "judger.workDir ({}) is no longer used: the judger judges in {}. Remove the setting.",
          obsoleteWorkDirectory,
          judgerWorkDirectory);
    }
    return cacheDirectory;
  }

  /**
   * Gets the work directory of this judger's slot, {@code work-<n>/} under the checkpoint root, in
   * which a judging creates its work directory. Others may pass through it ({@code rwx--x--x}), as
   * through the checkpoint root, so that the sandbox user reaches the work directory it is handed,
   * but may neither list nor change anything else in either.
   *
   * @return this judger's work directory (an absolute path)
   * @throws IOException if the slot cannot be claimed or the directory cannot be created
   */
  public synchronized Path getWorkDirectory() throws IOException {
    open();
    return judgerWorkDirectory;
  }

  /**
   * Ensures the local cache for a problem matches the problem's current checkpoints version,
   * downloading a fresh copy when the cache is missing or stale.
   *
   * @param problem - the problem whose checkpoints must be available locally
   * @return the directory holding the problem's checkpoints ({@code input#<id>.txt} and {@code
   *     output#<id>.txt})
   * @throws IOException if the checkpoints have to be downloaded but cannot be
   */
  public Path sync(Problem problem) throws IOException {
    long problemId = problem.getProblemId();
    int version = problem.getCheckpointsVersion();
    Path problemDirectory = open().resolve(String.valueOf(problemId));
    if (isCacheFresh(problemDirectory, version)) {
      return problemDirectory;
    }

    Path temporaryDirectory = cacheDirectory.resolve("." + problemId + "-" + UUID.randomUUID());
    try {
      createDirectory(temporaryDirectory, PRIVATE);
      checkpointProvider.download(problemId, temporaryDirectory.toFile());
      replaceDirectory(problemDirectory, temporaryDirectory);
    } finally {
      FileSystemUtils.deleteRecursively(temporaryDirectory);
    }
    LOGGER.info("Synced checkpoints for problem #{} to version {}", problemId, version);
    return problemDirectory;
  }

  /**
   * Reads the cached checkpoint metadata (id, score and whether an exact match is required) for a
   * problem from its local manifest. The input/output are not loaded into memory; the judger reads
   * those directly from the {@code input#<id>.txt}/{@code output#<id>.txt} files on disk.
   *
   * @param problemId - the unique identifier of the problem
   * @return the problem's checkpoints, ordered as in the manifest
   * @throws IOException if the manifest cannot be read
   */
  public List<Checkpoint> getCheckpoints(long problemId) throws IOException {
    Path problemDirectory = open().resolve(String.valueOf(problemId));
    Properties manifest = loadManifest(problemDirectory.resolve(MANIFEST_FILE));

    int count = Integer.parseInt(manifest.getProperty("count", "0"));
    List<Checkpoint> checkpoints = new ArrayList<>(count);
    for (int i = 0; i < count; ++i) {
      String prefix = "checkpoint." + i + ".";
      Checkpoint checkpoint = new Checkpoint();
      checkpoint.setProblemId(problemId);
      checkpoint.setCheckpointId(Integer.parseInt(manifest.getProperty(prefix + "id")));
      checkpoint.setScore(Integer.parseInt(manifest.getProperty(prefix + "score")));
      checkpoint.setExactlyMatch(Boolean.parseBoolean(manifest.getProperty(prefix + "exactlyMatch")));
      checkpoints.add(checkpoint);
    }
    return checkpoints;
  }

  /**
   * Claims the first free slot under the checkpoint root: locks its lock file, {@code
   * judger-<n>.lock}, for as long as this judger runs (the lock is released when the process
   * exits, however it exits), and prepares its directories. A slot whose directories cannot be
   * prepared is released, whatever went wrong.
   *
   * @param root - the checkpoint root
   * @param preparation - prepares the directories of the slot once it is locked
   * @return the lock of the slot
   * @throws IOException if every slot is taken, or a lock file cannot be opened, or the directories
   *     of the slot cannot be prepared
   */
  static ExclusiveFileLock claimSlot(Path root, SlotPreparation preparation) throws IOException {
    for (int number = 0; number < MAX_SLOTS; ++number) {
      ExclusiveFileLock lock =
          ExclusiveFileLock.tryAcquire(root.resolve("judger-" + number + ".lock"));
      if (lock == null) {
        continue;
      }
      try {
        preparation.prepare(number);
      } catch (Throwable ex) {
        try {
          lock.close();
        } catch (IOException closeException) {
          ex.addSuppressed(closeException);
        }
        throw ex;
      }
      return lock;
    }
    throw new IOException(
        String.format("All %d slots under %s are taken by other judgers.", MAX_SLOTS, root));
  }

  /** Prepares the directories of a slot once it is locked. */
  @FunctionalInterface
  interface SlotPreparation {
    /**
     * Prepares the directories of a slot.
     *
     * @param number - the number of the slot
     * @throws IOException if the directories cannot be prepared
     */
    void prepare(int number) throws IOException;
  }

  /**
   * Removes the temporary directories (named {@code .<problemId>-<uuid>}) of downloads that never
   * completed because the judger died. Only this judger uses its cache directory, so none of them
   * is in use.
   *
   * @param directory - this judger's cache directory
   * @throws IOException if a directory cannot be removed
   */
  private static void removeTemporaryDirectories(Path directory) throws IOException {
    try (DirectoryStream<Path> entries = Files.newDirectoryStream(directory, ".*")) {
      for (Path entry : entries) {
        LOGGER.info("Removing the incomplete checkpoint download {}", entry);
        FileSystemUtils.deleteRecursively(entry);
      }
    }
  }

  /**
   * Removes what judgings of a slot that never completed (as the judger died) left in its work
   * directory, {@code work-<n>/}, with the directory. A leftover the judger cannot remove (e.g. on
   * Windows, a directory of the sandbox account's whose ACL the judger may not change) does not
   * keep it from starting: the directory is moved aside, to {@code trash-<n>-<random>/}, and
   * removed at a later start.
   *
   * @param root - the checkpoint root
   * @param number - the number of the slot
   * @throws IOException if the work directory can be neither removed nor moved aside
   */
  private static void removeLeftovers(Path root, int number) throws IOException {
    try (DirectoryStream<Path> trash = Files.newDirectoryStream(root, "trash-" + number + "-*")) {
      for (Path entry : trash) {
        try {
          FileSystemUtils.deleteRecursively(entry);
        } catch (IOException ex) {
          LOGGER.warn("Cannot remove {} yet: trying again at the next start.", entry, ex);
        }
      }
    }
    Path work = root.resolve("work-" + number);
    try {
      FileSystemUtils.deleteRecursively(work);
    } catch (IOException ex) {
      Path trash = root.resolve("trash-" + number + "-" + UUID.randomUUID());
      LOGGER.warn("Cannot remove {}: moving it to {}, to remove it later.", work, trash, ex);
      try {
        Files.move(work, trash);
      } catch (IOException moveException) {
        moveException.addSuppressed(ex);
        throw moveException;
      }
    }
  }

  /**
   * Creates the work directory of this judger's slot. Others may pass through it ({@code
   * rwx--x--x}). On Windows, where every account may pass through a directory it cannot read
   * (Bypass traverse checking), the sandbox account is granted access to what is created in it
   * instead (not to the directory itself), i.e. to the work directory of each judging and what is
   * in it, but changing its ACL or owner. As the owner of what it creates there, it could still
   * change the ACL of that (and deny the judger, which then could not remove it): an entry for
   * {@code OWNER RIGHTS} gives the owner of what is created there no more than reading its ACL
   * instead.
   *
   * @param directory - the work directory of this judger's slot
   * @throws IOException if the directory cannot be created, or the access cannot be granted
   */
  private void createWorkDirectory(Path directory) throws IOException {
    if (isPosix()) {
      createDirectory(directory, PASSABLE);
      return;
    }
    UserPrincipalLookupService accounts =
        directory.getFileSystem().getUserPrincipalLookupService();
    List<AclEntry> entries = new ArrayList<>();
    entries.add(
        inheritedOnly(
            accounts.lookupPrincipalByName(sandboxUsername),
            EnumSet.complementOf(
                EnumSet.of(AclEntryPermission.WRITE_ACL, AclEntryPermission.WRITE_OWNER))));
    try {
      entries.add(
          inheritedOnly(
              accounts.lookupPrincipalByName(OWNER_RIGHTS),
              EnumSet.of(AclEntryPermission.READ_ACL)));
    } catch (UserPrincipalNotFoundException ex) {
      // Its name (S-1-3-4) may be translated on Windows in another language.
      LOGGER.warn(
          "No account is named {} on this host: the sandbox account may change the ACL of what it"
              + " creates in {}.",
          OWNER_RIGHTS,
          directory);
    }
    createWindowsDirectory(directory, entries);
  }

  /**
   * Builds an entry of an ACL (Windows) that applies only to what is created in a directory, not to
   * the directory itself.
   *
   * @param account - the account it allows
   * @param permissions - what it allows
   * @return the entry
   */
  private static AclEntry inheritedOnly(
      UserPrincipal account, Set<AclEntryPermission> permissions) {
    return AclEntry.newBuilder()
        .setType(AclEntryType.ALLOW)
        .setPrincipal(account)
        .setPermissions(permissions)
        .setFlags(
            AclEntryFlag.FILE_INHERIT, AclEntryFlag.DIRECTORY_INHERIT, AclEntryFlag.INHERIT_ONLY)
        .build();
  }

  /** The name of the account that stands for the owner of a file in an ACL (S-1-3-4). */
  private static final String OWNER_RIGHTS = "OWNER RIGHTS";

  /**
   * Creates a directory on Windows with an ACL of its own instead of the one it would inherit
   * (from {@code %ProgramData%}, every user may read and create files): it gives the judger full
   * control of the directory and of what is created in it, and nobody else any access but the
   * entries given. The accounts are looked up first, and a directory whose ACL cannot be set is
   * removed again, so that it never stays with the ACL it inherited.
   *
   * @param directory - the directory to create
   * @param entries - the entries of its ACL besides the judger's
   * @throws IOException if the directory cannot be created, or its ACL cannot be set
   */
  private static void createWindowsDirectory(Path directory, List<AclEntry> entries)
      throws IOException {
    List<AclEntry> acl = new ArrayList<>(entries);
    acl.add(
        AclEntry.newBuilder()
            .setType(AclEntryType.ALLOW)
            .setPrincipal(
                directory
                    .getFileSystem()
                    .getUserPrincipalLookupService()
                    .lookupPrincipalByName(System.getProperty("user.name")))
            .setPermissions(AclEntryPermission.values())
            .setFlags(AclEntryFlag.FILE_INHERIT, AclEntryFlag.DIRECTORY_INHERIT)
            .build());
    Files.createDirectory(directory);
    try {
      Files.getFileAttributeView(directory, AclFileAttributeView.class, LinkOption.NOFOLLOW_LINKS)
          .setAcl(acl);
    } catch (Throwable ex) {
      try {
        Files.delete(directory);
      } catch (IOException deleteException) {
        ex.addSuppressed(deleteException);
      }
      throw ex;
    }
  }

  /**
   * Checks whether the cached copy is present and at the expected version. A copy is swapped into
   * place whole (see {@link #sync}), so a copy with a manifest is a whole one.
   *
   * @param problemDirectory - the per-problem cache directory
   * @param version - the version the cache must match to be considered fresh
   * @return whether the cache can be reused without downloading
   */
  private boolean isCacheFresh(Path problemDirectory, int version) {
    Path manifestFile = problemDirectory.resolve(MANIFEST_FILE);
    if (!Files.isRegularFile(manifestFile)) {
      return false;
    }
    try {
      Properties manifest = loadManifest(manifestFile);
      return Integer.parseInt(manifest.getProperty("version", "-1")) == version;
    } catch (IOException | NumberFormatException ex) {
      LOGGER.warn("Ignoring unreadable checkpoint manifest at {}", manifestFile, ex);
      return false;
    }
  }

  /**
   * Loads a manifest properties file as UTF-8.
   *
   * @param manifestFile - the manifest file
   * @return the parsed manifest
   * @throws IOException if the file cannot be read
   */
  private Properties loadManifest(Path manifestFile) throws IOException {
    Properties manifest = new Properties();
    try (InputStream inputStream = Files.newInputStream(manifestFile)) {
      manifest.load(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
    }
    return manifest;
  }

  /**
   * Replaces the cached directory with the freshly downloaded one, atomically where the platform
   * supports it.
   *
   * @param problemDirectory - the destination cache directory
   * @param temporaryDirectory - the directory holding the freshly downloaded data
   * @throws IOException if the directory cannot be swapped into place
   */
  private void replaceDirectory(Path problemDirectory, Path temporaryDirectory)
      throws IOException {
    FileSystemUtils.deleteRecursively(problemDirectory);
    try {
      Files.move(temporaryDirectory, problemDirectory, StandardCopyOption.ATOMIC_MOVE);
    } catch (AtomicMoveNotSupportedException ex) {
      Files.move(temporaryDirectory, problemDirectory);
    }
  }

  /**
   * Opens the checkpoint root, making sure it belongs to the judger before anything in it is
   * trusted. The owner of the root can rename and replace the per-problem directories inside it,
   * which would let a submission swap in answers of its choosing. The root is therefore created by
   * the judger, with its permissions from the start (never changed afterwards, through a path
   * someone else may have put a link at meanwhile), and refused when it is a link or owned by
   * someone else. Other users must not be able to write to it either: they could rename the
   * judger's directories in it and put their own in place (a sticky bit would prevent that, but not
   * creating the directory of a slot before the judger does). Such a root is refused too, rather
   * than its permissions changed: a root the judger did not create may be a directory others rely
   * on (e.g. {@code /tmp}, when the judger runs as root).
   *
   * @return the real path of the root, so that every spelling of it agrees
   * @throws IOException if the root cannot be created, is not owned by the judger or others may
   *     write to it
   */
  private Path openCheckpointRoot() throws IOException {
    String directory =
        checkpointDirectory.isBlank() ? defaultCheckpointDirectory() : checkpointDirectory;
    Path root = Paths.get(directory).toAbsolutePath().normalize();
    try {
      Files.createDirectories(root.getParent());
      createDirectory(root, PASSABLE);
    } catch (FileAlreadyExistsException ex) {
      // Checked below, as any root the judger did not create.
    } catch (IOException ex) {
      throw new IOException(
          String.format(
              "Cannot create the checkpoint directory %s (%s). Create it for the judger's account,"
                  + " or set judger.checkpointDir (VOJ_JUDGER_CHECKPOINT_DIR) to a directory the"
                  + " judger may create.",
              root, ex),
          ex);
    }
    if (!isPosix()) {
      return root.toRealPath();
    }
    if (!Files.isDirectory(root, LinkOption.NOFOLLOW_LINKS)) {
      throw new IOException("The checkpoint directory is not a real directory: " + root);
    }
    ensureOwnedByJudger(root, "judger.checkpointDir");
    int mode = (Integer) Files.getAttribute(root, "unix:mode", LinkOption.NOFOLLOW_LINKS);
    if ((mode & (GROUP_WRITE | OTHERS_WRITE)) != 0) {
      throw new IOException(
          String.format(
              "Refusing to use the checkpoint directory %s: other users may write to it (mode"
                  + " %o), so they could replace the test data in it. Set judger.checkpointDir to"
                  + " a directory of the judger's own (it creates a missing one others may only"
                  + " pass through), or take their write permission away (chmod go-w) if nothing"
                  + " else needs it.",
              root, mode & 07777));
    }
    Path realRoot = root.toRealPath();
    ensureSandboxCanPassThrough(realRoot);
    return realRoot;
  }

  /**
   * Makes sure the sandbox account, when the judger runs submissions as it, may pass through the
   * checkpoint root and every directory above it, on its way to the work directory of a judging:
   * it could not even start a compilation otherwise. A missing directory above the root is created
   * with the umask (only the judger may enter it under a umask of {@code 0077}, as some hardened
   * hosts and service units set), and a root the judger did not create may be private too.
   *
   * @param realRoot - the real path of the checkpoint root
   * @throws IOException if others may not pass through one of these directories
   */
  private void ensureSandboxCanPassThrough(Path realRoot) throws IOException {
    if (!SandboxAccountLock.dropsPrivileges(sandbox)) {
      return;
    }
    for (Path directory = realRoot; directory != null; directory = directory.getParent()) {
      int mode = (Integer) Files.getAttribute(directory, "unix:mode", LinkOption.NOFOLLOW_LINKS);
      if ((mode & OTHERS_EXECUTE) == 0) {
        throw new IOException(
            String.format(
                "The sandbox account (system.username) may not pass through %s (mode %o) to the"
                    + " work directories in the checkpoint directory %s, so every submission"
                    + " would be judged a System Error. Let others pass through it (chmod o+x),"
                    + " or set judger.checkpointDir to a directory they may pass through to.",
                directory, mode & 07777, realRoot));
      }
    }
  }

  /**
   * Gets the default checkpoint root: a directory for the state of a service, which no clean-up of
   * old files touches, unlike the temporary directory.
   *
   * @return {@code /var/lib/voj-judger}, or {@code %ProgramData%\voj-judger} on Windows
   */
  static String defaultCheckpointDirectory() {
    if (isPosix()) {
      return "/var/lib/voj-judger";
    }
    String programData = System.getenv("ProgramData");
    return Paths.get(programData != null ? programData : "C:\\ProgramData")
        .resolve("voj-judger")
        .toString();
  }

  /**
   * Makes sure a file belongs to the judger's account (on POSIX systems), without following a link.
   *
   * @param file - the file
   * @param setting - the setting that names its directory
   * @throws IOException if it belongs to another account
   */
  private static void ensureOwnedByJudger(Path file, String setting) throws IOException {
    if (!isPosix()) {
      return;
    }
    UserPrincipal owner = Files.getOwner(file, LinkOption.NOFOLLOW_LINKS);
    UserPrincipal judger =
        file.getFileSystem()
            .getUserPrincipalLookupService()
            .lookupPrincipalByName(System.getProperty("user.name"));
    if (!owner.equals(judger)) {
      throw new IOException(
          String.format(
              "Refusing to use %s: it belongs to %s, not to the judger (%s). Remove it, or set %s"
                  + " to a directory of the judger's own.",
              file, owner.getName(), judger.getName(), setting));
    }
  }

  /** The permission bit of a file mode that lets the group write. */
  private static final int GROUP_WRITE = 0020;

  /** The permission bit of a file mode that lets other users write. */
  private static final int OTHERS_WRITE = 0002;

  /** The permission bit of a directory's mode that lets other users pass through it. */
  private static final int OTHERS_EXECUTE = 0001;

  /**
   * Creates a directory with the given permissions (on POSIX systems) from the start, whatever the
   * permissions of its parent and the umask, so that submitted programs running as the
   * unprivileged sandbox user cannot read the reference answers in a private one. On Windows, one
   * only the judger may access (see {@link #createWindowsDirectory}), whatever the permissions.
   *
   * @param directory - the directory to create
   * @param permissions - its permissions, e.g. {@code rwx------}
   * @throws IOException if the directory cannot be created
   */
  private static void createDirectory(Path directory, String permissions) throws IOException {
    if (!isPosix()) {
      // Others may pass through any directory (Bypass traverse checking).
      createWindowsDirectory(directory, List.of());
      return;
    }
    Set<PosixFilePermission> permissionSet = PosixFilePermissions.fromString(permissions);
    Files.createDirectory(directory, PosixFilePermissions.asFileAttribute(permissionSet));
    // The umask may have taken some of them away: set them again on the directory just created,
    // never through a link put at its path meanwhile.
    Files.getFileAttributeView(directory, PosixFileAttributeView.class, LinkOption.NOFOLLOW_LINKS)
        .setPermissions(permissionSet);
  }

  /** The permissions of a directory only the judger may enter: a cache. */
  private static final String PRIVATE = "rwx------";

  /**
   * The permissions of a directory others may pass through, but neither list nor change: the
   * checkpoint root and a work directory, on the way to the work directory of a judging.
   */
  private static final String PASSABLE = "rwx--x--x";

  /**
   * Checks whether the host file system supports POSIX permissions (i.e. is not Windows).
   *
   * @return whether POSIX permissions are available
   */
  private static boolean isPosix() {
    return !System.getProperty("os.name").contains("Windows");
  }

  /** The lock of this judger's slot, held until it exits, once claimed. */
  private ExclusiveFileLock slotLock;

  /** This judger's cache directory (of its slot, under the checkpoint root), once claimed. */
  private Path cacheDirectory;

  /** This judger's work directory (of its slot, under the checkpoint root), once claimed. */
  private Path judgerWorkDirectory;

  /** The autowired HttpCheckpointProvider, which performs the actual download. */
  @Autowired private HttpCheckpointProvider checkpointProvider;

  /**
   * The checkpoint root, which holds the slots of the judgers on this host; empty for the default
   * (see {@link #defaultCheckpointDirectory}).
   */
  @Value("${judger.checkpointDir:}")
  private String checkpointDirectory = "";

  /** The sandbox backend ({@code native} or {@code isolate}). */
  @Value("${judger.sandbox:native}")
  private String sandbox = "native";

  /** The account the sandbox runs submissions as on Windows. */
  @Value("${system.username:}")
  private String sandboxUsername;

  /** What judger.workDir, which is no longer used, is set to, if anything. */
  @Value("${judger.workDir:${VOJ_JUDGER_WORK_DIR:}}")
  private String obsoleteWorkDirectory = "";

  /** The logger. */
  private static final Logger LOGGER = LogManager.getLogger(CheckpointStore.class);
}
