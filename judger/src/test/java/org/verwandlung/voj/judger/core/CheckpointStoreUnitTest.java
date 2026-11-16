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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import org.verwandlung.voj.judger.model.Checkpoint;
import org.verwandlung.voj.judger.model.Problem;

/**
 * Database-free unit tests for the checkpoint root checks of CheckpointStore.
 *
 * @author Haozhe Xie
 */
@ExtendWith(MockitoExtension.class)
@DisabledOnOs(OS.WINDOWS)
public class CheckpointStoreUnitTest {
  /**
   * Test case: the checkpoint root does not exist yet. Expected result: the judger creates it,
   * restricted to its owner, and downloads the checkpoints into it.
   */
  @Test
  public void testSyncCreatesPrivateRoot() throws Exception {
    Path root = tempDir.resolve("voj-testpoints");
    ReflectionTestUtils.setField(checkpointStore, "checkpointDirectory", root.toString());
    doAnswer(
            invocation -> {
              File destination = invocation.getArgument(1);
              Files.createDirectories(destination.toPath());
              Files.writeString(destination.toPath().resolve("manifest.properties"), "count=0\n");
              return null;
            })
        .when(checkpointProvider)
        .download(anyLong(), any(File.class));

    checkpointStore.sync(getProblem());

    Assertions.assertTrue(Files.isDirectory(root, LinkOption.NOFOLLOW_LINKS));
    Assertions.assertEquals(
        "rwx------", PosixFilePermissions.toString(Files.getPosixFilePermissions(root)));
    verify(checkpointProvider).download(anyLong(), any(File.class));
  }

  /**
   * Test case: the checkpoint root is a symbolic link planted by someone else. Expected result: the
   * sync is refused and nothing is downloaded through the link.
   */
  @Test
  public void testSyncRefusesSymlinkedRoot() throws Exception {
    Path target = Files.createDirectory(tempDir.resolve("elsewhere"));
    Path root = Files.createSymbolicLink(tempDir.resolve("voj-testpoints"), target);
    ReflectionTestUtils.setField(checkpointStore, "checkpointDirectory", root.toString());

    Assertions.assertThrows(IOException.class, () -> checkpointStore.sync(getProblem()));
    verify(checkpointProvider, never()).download(anyLong(), any(File.class));
  }

  /**
   * Test case: the checkpoint root already exists and is readable by others. Expected result: the
   * download goes into a directory of this judger's own that only the judger can read, and the
   * checkpoints are then read from there; the same version is not downloaded twice, a new one is.
   */
  @Test
  public void testSyncDownloadsPrivatelyIntoTheJudgersSlot() throws Exception {
    Path root = Files.createDirectory(tempDir.resolve("voj-testpoints"));
    Files.setPosixFilePermissions(root, PosixFilePermissions.fromString("rwxr-xr-x"));
    ReflectionTestUtils.setField(checkpointStore, "checkpointDirectory", root.toString());
    List<String> downloadPermissions = new ArrayList<>();
    doAnswer(
            invocation -> {
              Path destination = ((File) invocation.getArgument(1)).toPath();
              downloadPermissions.add(
                  PosixFilePermissions.toString(Files.getPosixFilePermissions(destination)));
              Files.writeString(
                  destination.resolve("manifest.properties"),
                  "version=1\ncount=1\ncheckpoint.0.id=3\ncheckpoint.0.score=100\n"
                      + "checkpoint.0.exactlyMatch=true\n");
              Files.writeString(destination.resolve("input#3.txt"), "1 2\n");
              return null;
            })
        .when(checkpointProvider)
        .download(anyLong(), any(File.class));

    Path problemDirectory = checkpointStore.sync(getProblem());
    checkpointStore.sync(getProblem());

    Assertions.assertEquals(root.resolve("judger-0").resolve("1000"), problemDirectory);
    Assertions.assertEquals(List.of("rwx------"), downloadPermissions);
    Assertions.assertEquals(
        "rwx------",
        PosixFilePermissions.toString(Files.getPosixFilePermissions(problemDirectory.getParent())));
    Assertions.assertEquals("1 2\n", Files.readString(problemDirectory.resolve("input#3.txt")));
    List<Checkpoint> checkpoints = checkpointStore.getCheckpoints(1000);
    Assertions.assertEquals(1, checkpoints.size());
    Assertions.assertEquals(3, checkpoints.get(0).getCheckpointId());
    Assertions.assertTrue(checkpoints.get(0).isExactlyMatch());
    try (Stream<Path> entries = Files.list(problemDirectory.getParent())) {
      Assertions.assertEquals(List.of(problemDirectory), entries.toList());
    }

    Problem updated = getProblem();
    updated.setCheckpointsVersion(2);
    checkpointStore.sync(updated);
    verify(checkpointProvider, times(2)).download(anyLong(), any(File.class));
  }

  /**
   * Test case: a judger died while downloading, leaving a temporary directory behind. Expected
   * result: it is removed when the judger's cache is opened (at startup), the cache is kept.
   */
  @Test
  public void testOpenRemovesIncompleteDownloads() throws Exception {
    Path root = Files.createDirectory(tempDir.resolve("voj-testpoints"));
    Path slot = Files.createDirectory(root.resolve("judger-0"));
    Path incomplete = Files.createDirectories(slot.resolve(".1000-dead").resolve("sub"));
    Files.writeString(incomplete.resolve("output#1.txt"), "42\n");
    Path cached = Files.createDirectory(slot.resolve("1001"));
    ReflectionTestUtils.setField(checkpointStore, "checkpointDirectory", root.toString());

    Assertions.assertEquals(slot, checkpointStore.open());

    Assertions.assertFalse(Files.exists(slot.resolve(".1000-dead")));
    Assertions.assertTrue(Files.isDirectory(cached));
  }

  /**
   * Test case: two judgers share the checkpoint root (two stores in one JVM here; across processes
   * the lock file does the same). Expected result: each gets a cache directory of its own, so
   * neither ever replaces test data that the other one is reading.
   */
  @Test
  public void testJudgersOnOneHostGetSeparateCaches() throws Exception {
    Path root = tempDir.resolve("voj-testpoints");
    ReflectionTestUtils.setField(checkpointStore, "checkpointDirectory", root.toString());
    CheckpointStore otherStore = new CheckpointStore();
    ReflectionTestUtils.setField(otherStore, "checkpointDirectory", root.toString());

    Path first = checkpointStore.open();
    Path second = otherStore.open();

    Assertions.assertEquals(root.resolve("judger-0"), first);
    Assertions.assertEquals(root.resolve("judger-1"), second);
    Assertions.assertEquals(first, checkpointStore.open());
  }

  /**
   * Builds the problem used by the tests.
   *
   * @return a problem with a checkpoints version
   */
  private Problem getProblem() {
    Problem problem = new Problem();
    problem.setProblemId(1000);
    problem.setCheckpointsVersion(1);
    return problem;
  }

  /** The download back-end, mocked. */
  @Mock private HttpCheckpointProvider checkpointProvider;

  /** The CheckpointStore object under test. */
  @InjectMocks private CheckpointStore checkpointStore;

  /** A temporary directory managed by JUnit. */
  @TempDir Path tempDir;
}
