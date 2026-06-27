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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.PosixFilePermission;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import org.verwandlung.voj.judger.model.Checkpoint;
import org.verwandlung.voj.judger.model.Problem;

/**
 * A local, version-aware cache of problem checkpoints (test data).
 *
 * <p>The database remains the source of truth, but a judger no longer reads the input/output of
 * every checkpoint from the database on each submission. Instead it keeps a per-problem copy under
 * {@code judger.checkpointDir/<problemId>/} and re-downloads (via a {@link CheckpointProvider}) only
 * when the problem's {@code checkpoints_version} changes — the version the web side bumps whenever the
 * test data is edited.
 *
 * <p>A fresh copy is downloaded into a temporary directory and only then swapped into place, so an
 * interrupted or failed download never leaves a half-written cache that could be mistaken for valid.
 * Judging is serialised on a single judger (see {@code Dispatcher#createNewTask}), so no additional
 * locking between problems is required.
 *
 * @author Haozhe Xie
 */
@Component
public class CheckpointStore {
  /** The manifest entry, written by the web side, that records the version and per-checkpoint meta. */
  private static final String MANIFEST_FILE = "manifest.properties";

  /**
   * Ensures the local cache for a problem matches the problem's current checkpoints version,
   * downloading a fresh copy when the cache is missing or stale.
   *
   * @param problem - the problem whose checkpoints must be available locally
   * @throws IOException if the checkpoints have to be downloaded but cannot be
   */
  public void sync(Problem problem) throws IOException {
    long problemId = problem.getProblemId();
    int version = problem.getCheckpointsVersion();
    File problemDirectory = new File(checkpointDirectory, String.valueOf(problemId));

    if (isCacheFresh(problemDirectory, version)) {
      return;
    }

    File temporaryDirectory =
        new File(checkpointDirectory, "." + problemId + "-" + UUID.randomUUID());
    try {
      checkpointProvider.download(problemId, temporaryDirectory);
      replaceDirectory(problemDirectory, temporaryDirectory);
      restrictToOwner(problemDirectory);
    } finally {
      if (temporaryDirectory.exists()) {
        FileUtils.deleteQuietly(temporaryDirectory);
      }
    }
    LOGGER.info("Synced checkpoints for problem #{} to version {}", problemId, version);
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
    File problemDirectory = new File(checkpointDirectory, String.valueOf(problemId));
    Properties manifest = loadManifest(new File(problemDirectory, MANIFEST_FILE));

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
   * Checks whether the cached copy is present and at the expected version.
   *
   * @param problemDirectory - the per-problem cache directory
   * @param version - the version the cache must match to be considered fresh
   * @return whether the cache can be reused without downloading
   */
  private boolean isCacheFresh(File problemDirectory, int version) {
    File manifestFile = new File(problemDirectory, MANIFEST_FILE);
    if (!problemDirectory.isDirectory() || !manifestFile.isFile()) {
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
  private Properties loadManifest(File manifestFile) throws IOException {
    Properties manifest = new Properties();
    try (InputStream inputStream = new FileInputStream(manifestFile)) {
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
  private void replaceDirectory(File problemDirectory, File temporaryDirectory) throws IOException {
    if (problemDirectory.exists()) {
      FileUtils.deleteDirectory(problemDirectory);
    } else {
      File parent = problemDirectory.getParentFile();
      if (parent != null && !parent.exists() && !parent.mkdirs()) {
        throw new IOException("Failed to create the checkpoint directory: " + parent);
      }
    }
    try {
      Files.move(
          temporaryDirectory.toPath(),
          problemDirectory.toPath(),
          StandardCopyOption.ATOMIC_MOVE);
    } catch (AtomicMoveNotSupportedException ex) {
      Files.move(temporaryDirectory.toPath(), problemDirectory.toPath());
    }
  }

  /**
   * Restricts the checkpoint directory to its owner (the judger), so that submitted programs running
   * as the unprivileged sandbox user cannot read the reference answers. Mirrors the protection the
   * preprocessor previously applied. A no-op on Windows.
   *
   * @param directory - the per-problem cache directory
   * @throws IOException if the permissions cannot be set
   */
  private void restrictToOwner(File directory) throws IOException {
    if (System.getProperty("os.name").contains("Windows")) {
      return;
    }
    Set<PosixFilePermission> permissions = new HashSet<>();
    permissions.add(PosixFilePermission.OWNER_READ);
    permissions.add(PosixFilePermission.OWNER_WRITE);
    permissions.add(PosixFilePermission.OWNER_EXECUTE);
    Files.setPosixFilePermissions(directory.toPath(), permissions);
  }

  /** The autowired CheckpointProvider, which performs the actual download. */
  @Autowired private CheckpointProvider checkpointProvider;

  /** The storage directory of checkpoints, used to cache the input/output data of checkpoints. */
  @Value("${judger.checkpointDir}")
  private String checkpointDirectory;

  /** The logger. */
  private static final Logger LOGGER = LogManager.getLogger(CheckpointStore.class);
}
