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
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.PosixFilePermissions;
import java.nio.file.attribute.UserPrincipal;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
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
 * <p>Every judger has a cache directory of its own, {@code judger.checkpointDir/judger-<n>/}, so
 * that several judgers on one host never replace a problem's copy while another one reads it: the
 * judger claims the first slot <i>n</i> whose lock file ({@code judger-<n>.lock}) no other process
 * holds, and holds that lock until it exits. A restarted judger usually gets its old slot (and
 * cache) back. Within a judger, judging is serialised (see {@code Dispatcher#createNewTask}), so a
 * problem's copy is never replaced while it is read. A clean-up of old files in {@code /tmp} may
 * remove a lock file while it is held, and another judger would then lock a new file at the same
 * path: so before each use the judger checks that the file at the path is still the one it
 * locked (and marks it as just used), and claims a slot again if not.
 *
 * <p>The slot also gives the judger a directory of its own under {@code judger.workDir}, {@code
 * voj-judger-<n>/}, which holds the work directory of each judging (see {@link
 * #getWorkDirectory}). A judger that died while judging (killed, out of memory) leaves the work
 * directory of that judging behind, with a submission's source and binary in it: it is removed
 * when the slot is claimed again, while the judgings of other judgers on the host, in directories
 * of their own slots, are left alone. Judgers that share {@code judger.workDir} must therefore
 * share {@code judger.checkpointDir} too, as they do by default.
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
   * Opens this judger's cache directory, claiming a slot under the checkpoint root, and removes the
   * temporary directories of downloads that never completed and the work directories of judgings
   * that never completed. Called at startup; {@link #sync} does it on first use otherwise.
   *
   * @return this judger's cache directory
   * @throws IOException if the checkpoint root cannot be trusted or every slot is taken
   */
  public synchronized Path open() throws IOException {
    ensureCheckpointRootIsTrusted();
    if (slotLock != null && !slotLock.isValid()) {
      LOGGER.warn(
          "The lock file {} of this judger's slot was removed or replaced (e.g. by a clean-up of"
              + " temporary files): claiming a slot again.",
          slotLock.getFile());
      slotLock.close();
      slotLock = null;
      cacheDirectory = null;
      judgerWorkDirectory = null;
    }
    if (cacheDirectory == null) {
      cacheDirectory = claimSlot(Paths.get(checkpointDirectory));
      removeTemporaryDirectories(cacheDirectory);
      LOGGER.info("Caching checkpoints in {}", cacheDirectory);
    } else if (!Files.isDirectory(cacheDirectory, LinkOption.NOFOLLOW_LINKS)) {
      // Removed behind the judger's back, e.g. by a clean-up of /tmp.
      createPrivateDirectory(cacheDirectory);
    }
    return cacheDirectory;
  }

  /**
   * Gets this judger's directory under {@code judger.workDir} (absolute and normalized, whatever
   * the setting looks like), in which a judging creates its work directory. It belongs to the
   * judger and lets others through ({@code rwx--x--x}), so that the sandbox user reaches the work
   * directory it is handed but cannot list or change anything else in it.
   *
   * @return this judger's work directory
   * @throws IOException if the slot cannot be claimed or the directory cannot be created
   */
  public synchronized Path getWorkDirectory() throws IOException {
    open();
    if (!Files.isDirectory(judgerWorkDirectory, LinkOption.NOFOLLOW_LINKS)) {
      // Removed behind the judger's back, e.g. by a clean-up of /tmp.
      resetWorkDirectory(judgerWorkDirectory);
    }
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
      // Private from the start: the root may be readable by others if it existed already.
      createPrivateDirectory(temporaryDirectory);
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
   * Claims the first free slot under the checkpoint root: locks its lock file, for as long as this
   * judger runs (the lock is released when the process exits, however it exits), creates its
   * cache directory if needed, and empties its work directory.
   *
   * @param root - the checkpoint root
   * @return the slot's cache directory
   * @throws IOException if every slot is taken
   */
  private Path claimSlot(Path root) throws IOException {
    Files.createDirectories(root);
    // Relative to the directory the judger was started in, as any relative path is; but the
    // sandbox resolves the paths of a judging after entering its work directory.
    Path workRoot = Paths.get(workRootDirectory).toAbsolutePath().normalize();
    Path[] workDirectory = new Path[1];
    Slot slot =
        claimSlot(
            root,
            "judger-",
            directory -> {
              if (!Files.exists(directory, LinkOption.NOFOLLOW_LINKS)) {
                createPrivateDirectory(directory);
              }
              ensureOwnedByJudger(directory);
              workDirectory[0] = workRoot.resolve("voj-" + directory.getFileName());
              resetWorkDirectory(workDirectory[0]);
            });
    if (slot == null) {
      throw new IOException(
          String.format(
              "All %d checkpoint cache slots under %s are taken by other judgers.",
              MAX_SLOTS, root));
    }
    LOGGER.info("Judging in {}", workDirectory[0]);
    slotLock = slot.lock();
    judgerWorkDirectory = workDirectory[0];
    return slot.directory();
  }

  /**
   * Claims the first free slot in a directory: locks its lock file, {@code <name><n>.lock}, for as
   * long as this judger runs (the lock is released when the process exits, however it exits), and
   * prepares its directory, {@code <name><n>}. A slot whose directory cannot be prepared is
   * released, whatever went wrong.
   *
   * @param root - the directory of the slots
   * @param name - the name of the slots, without their number
   * @param preparation - prepares the directory of the slot once it is locked
   * @return the slot, or {@code null} if every slot is taken
   * @throws IOException if a lock file cannot be opened or the directory cannot be prepared
   */
  static Slot claimSlot(Path root, String name, SlotPreparation preparation) throws IOException {
    for (int number = 0; number < MAX_SLOTS; ++number) {
      ExclusiveFileLock lock = ExclusiveFileLock.tryAcquire(root.resolve(name + number + ".lock"));
      if (lock == null) {
        continue;
      }
      Path directory = root.resolve(name + number);
      try {
        preparation.prepare(directory);
      } catch (Throwable ex) {
        try {
          lock.close();
        } catch (IOException closeException) {
          ex.addSuppressed(closeException);
        }
        throw ex;
      }
      return new Slot(lock, directory);
    }
    return null;
  }

  /**
   * A slot claimed by this judger.
   *
   * @param lock - the lock of the slot, held while the judger runs
   * @param directory - the directory of the slot
   */
  record Slot(ExclusiveFileLock lock, Path directory) {}

  /** Prepares the directory of a slot once it is locked. */
  @FunctionalInterface
  interface SlotPreparation {
    /**
     * Prepares the directory of a slot.
     *
     * @param directory - the directory of the slot
     * @throws IOException if the directory cannot be prepared
     */
    void prepare(Path directory) throws IOException;
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
   * Creates the work directory of this judger's slot afresh: whatever is there (the work
   * directories of judgings a judger with this slot never finished, or anything else) is removed
   * first, without following links. The directory is created restricted to the judger (the umask
   * can only narrow that), then opened for others to pass through.
   *
   * @param directory - the work directory of this judger's slot
   * @throws IOException if the directory cannot be emptied or created
   */
  private static void resetWorkDirectory(Path directory) throws IOException {
    if (Files.exists(directory, LinkOption.NOFOLLOW_LINKS)) {
      LOGGER.info("Removing what judgings that never completed left in {}", directory);
      FileSystemUtils.deleteRecursively(directory);
    }
    Files.createDirectories(directory.getParent());
    createPrivateDirectory(directory);
    if (isPosix()) {
      Files.setPosixFilePermissions(directory, PosixFilePermissions.fromString("rwx--x--x"));
    }
  }

  /**
   * Checks whether the cached copy is present and at the expected version.
   *
   * @param problemDirectory - the per-problem cache directory
   * @param version - the version the cache must match to be considered fresh
   * @return whether the cache can be reused without downloading
   */
  private boolean isCacheFresh(Path problemDirectory, int version) {
    Path manifestFile = problemDirectory.resolve(MANIFEST_FILE);
    if (!Files.isDirectory(problemDirectory) || !Files.isRegularFile(manifestFile)) {
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
   * Makes sure the checkpoint root directory belongs to the judger before anything in it is trusted.
   * The default root ({@code /tmp/voj-testpoints}) lives in the world-writable {@code /tmp}, so the
   * sandbox user (or any local account) could create it first, or plant a symbolic link there. The
   * owner of the root can rename and replace the per-problem directories inside it, which would let
   * a submission swap in answers of its choosing. The root is therefore created by the judger,
   * restricted to its owner, and refused when it is a link or owned by someone else. Other users
   * must not be able to write to it either: they could rename the judger's directories in it and
   * put their own in place (a sticky bit would prevent that, but not creating the directory of a
   * slot before the judger does). The judger owns the root, so it takes their write permission
   * away; a slot directory someone else created meanwhile is refused when it is claimed.
   *
   * @throws IOException if the root cannot be created or is not owned by the judger
   */
  private void ensureCheckpointRootIsTrusted() throws IOException {
    if (!isPosix()) {
      return;
    }
    Path root = Paths.get(checkpointDirectory);
    if (!Files.exists(root, LinkOption.NOFOLLOW_LINKS)) {
      Files.createDirectories(root);
      Files.setPosixFilePermissions(root, PosixFilePermissions.fromString("rwx------"));
    }
    if (!Files.isDirectory(root, LinkOption.NOFOLLOW_LINKS)) {
      throw new IOException("The checkpoint directory is not a real directory: " + root);
    }
    UserPrincipal owner = Files.getOwner(root, LinkOption.NOFOLLOW_LINKS);
    UserPrincipal judger =
        root.getFileSystem()
            .getUserPrincipalLookupService()
            .lookupPrincipalByName(System.getProperty("user.name"));
    if (!owner.equals(judger)) {
      throw new IOException(
          String.format(
              "Refusing to use the checkpoint directory %s: it is owned by %s, not by the judger"
                  + " (%s). Remove it or set judger.checkpointDir to a directory the judger owns.",
              root, owner.getName(), judger.getName()));
    }
    int mode = (Integer) Files.getAttribute(root, "unix:mode", LinkOption.NOFOLLOW_LINKS);
    if ((mode & (GROUP_WRITE | OTHERS_WRITE)) != 0) {
      LOGGER.warn(
          "Other users may write to the checkpoint directory {} (mode {}), so they could replace"
              + " the test data in it: taking their write permission away.",
          root,
          Integer.toOctalString(mode & 07777));
      int restrictedMode = mode & 07777 & ~(GROUP_WRITE | OTHERS_WRITE);
      Files.setAttribute(root, "unix:mode", restrictedMode, LinkOption.NOFOLLOW_LINKS);
    }
  }

  /**
   * Makes sure a directory in the checkpoint root (a slot's) belongs to the judger, rather than to
   * a user who created it while the root was writable by others.
   *
   * @param directory - the directory
   * @throws IOException if it is not a real directory or not owned by the judger
   */
  private static void ensureOwnedByJudger(Path directory) throws IOException {
    if (!isPosix()) {
      return;
    }
    UserPrincipal owner = Files.getOwner(directory, LinkOption.NOFOLLOW_LINKS);
    UserPrincipal judger =
        directory
            .getFileSystem()
            .getUserPrincipalLookupService()
            .lookupPrincipalByName(System.getProperty("user.name"));
    if (!Files.isDirectory(directory, LinkOption.NOFOLLOW_LINKS) || !owner.equals(judger)) {
      throw new IOException(
          String.format(
              "Refusing to use %s: it is not a directory of the judger (%s) but of %s. Remove it.",
              directory, judger.getName(), owner.getName()));
    }
  }

  /** The permission bit of a file mode that lets the group write. */
  private static final int GROUP_WRITE = 0020;

  /** The permission bit of a file mode that lets other users write. */
  private static final int OTHERS_WRITE = 0002;

  /**
   * Creates a directory restricted to its owner (the judger), so that submitted programs running as
   * the unprivileged sandbox user cannot read the reference answers in it, whatever the permissions
   * of its parent. A plain directory on Windows.
   *
   * @param directory - the directory to create
   * @throws IOException if the directory cannot be created
   */
  private static void createPrivateDirectory(Path directory) throws IOException {
    if (isPosix()) {
      Files.createDirectory(
          directory,
          PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rwx------")));
    } else {
      Files.createDirectory(directory);
    }
  }

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

  /** This judger's cache directory (its slot under the checkpoint root), once opened. */
  private Path cacheDirectory;

  /** This judger's work directory (its slot's under the work root), once opened. */
  private Path judgerWorkDirectory;

  /** The autowired HttpCheckpointProvider, which performs the actual download. */
  @Autowired private HttpCheckpointProvider checkpointProvider;

  /** The root of the checkpoint caches, which holds a directory per judger on this host. */
  @Value("${judger.checkpointDir}")
  private String checkpointDirectory;

  /** The root of the work directories, which holds a directory per judger on this host. */
  @Value("${judger.workDir}")
  private String workRootDirectory;

  /** The logger. */
  private static final Logger LOGGER = LogManager.getLogger(CheckpointStore.class);
}
