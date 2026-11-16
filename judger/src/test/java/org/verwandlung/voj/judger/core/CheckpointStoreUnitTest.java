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
import org.junit.jupiter.api.Assumptions;
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
import org.verwandlung.voj.judger.util.ExclusiveFileLock;

/**
 * Database-free unit tests for CheckpointStore: its checkpoint root checks and its slots.
 *
 * @author Haozhe Xie
 */
@ExtendWith(MockitoExtension.class)
@DisabledOnOs(OS.WINDOWS)
public class CheckpointStoreUnitTest {
  /**
   * Test case: the checkpoint root does not exist yet. Expected result: the judger creates it, so
   * that others may only pass through it, and downloads the checkpoints into it.
   */
  @Test
  public void testSyncCreatesRootOthersMayOnlyPassThrough() throws Exception {
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
        "rwx--x--x", PosixFilePermissions.toString(Files.getPosixFilePermissions(root)));
    verify(checkpointProvider).download(anyLong(), any(File.class));
  }

  /**
   * Test case: judger.checkpointDir is empty, and the judger, which is not root, may not create
   * the default checkpoint root, nor a root it is set to. Expected result: the default is a
   * directory for the state of a service, not in the temporary directory, and the judger does not
   * start, saying to set judger.checkpointDir.
   */
  @Test
  public void testOpenTellsToSetTheCheckpointDirectoryItCannotCreate() throws Exception {
    Assumptions.assumeFalse("root".equals(System.getProperty("user.name")), "root may create it");
    Assertions.assertEquals("/var/lib/voj-judger", CheckpointStore.defaultCheckpointDirectory());
    Path readOnly =
        Files.createDirectory(
            tempDir.resolve("read-only"),
            PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("r-x------")));

    for (String directory : new String[] {"", readOnly.resolve("voj-judger").toString()}) {
      CheckpointStore store = newStore(Path.of(directory));
      IOException ex = Assertions.assertThrows(IOException.class, store::open);
      Assertions.assertTrue(ex.getMessage().contains("judger.checkpointDir"), ex.getMessage());
    }
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
   * Test case: the checkpoint root may be written to by other users (the sandbox user among them),
   * with or without a sticky bit (as /tmp), or by the group. Expected result: the judger refuses
   * it, and leaves its permissions alone, as other users may rely on them.
   */
  @Test
  public void testOpenRefusesARootOthersMayWriteTo() throws Exception {
    Path root = Files.createDirectory(tempDir.resolve("voj-testpoints"));
    ReflectionTestUtils.setField(checkpointStore, "checkpointDirectory", root.toString());

    for (int mode : new int[] {0777, 01777, 0770}) {
      Files.setAttribute(root, "unix:mode", mode);
      IOException ex = Assertions.assertThrows(IOException.class, () -> checkpointStore.open());
      Assertions.assertTrue(ex.getMessage().contains("other users may write to it"), ex.toString());
      Assertions.assertEquals(mode, (Integer) Files.getAttribute(root, "unix:mode") & 07777);
      try (Stream<Path> entries = Files.list(root)) {
        Assertions.assertEquals(0, entries.count());
      }
    }
  }

  /**
   * Test case: someone else (here the sandbox user nobody) created the directory of a slot while
   * the checkpoint root was writable by others. Expected result: the judger refuses to use it.
   */
  @Test
  public void testOpenRefusesSlotOfAnotherUser() throws Exception {
    Assumptions.assumeTrue("root".equals(System.getProperty("user.name")), "needs root to chown");
    Path root = Files.createDirectory(tempDir.resolve("voj-testpoints"));
    Path slot = Files.createDirectory(root.resolve("judger-0"));
    Files.setAttribute(slot, "unix:uid", 65534);
    ReflectionTestUtils.setField(checkpointStore, "checkpointDirectory", root.toString());

    Assertions.assertThrows(IOException.class, () -> checkpointStore.open());
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
              Files.writeString(destination.resolve("output#3.txt"), "3\n");
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

    verify(checkpointProvider, times(1)).download(anyLong(), any(File.class));

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
    Path root =
        Files.createDirectory(
            tempDir.resolve("voj-testpoints"),
            PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rwx------")));
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
   * the lock file does the same), one of them spelling it through a link to its parent. Expected
   * result: each gets a slot of its own, i.e. a cache directory and a work directory of its own,
   * so neither ever replaces test data that the other one is reading, nor removes its judgings.
   */
  @Test
  public void testJudgersSharingTheCheckpointRootGetSlotsOfTheirOwn() throws Exception {
    Path root = Files.createDirectory(tempDir.resolve("real")).resolve("voj-testpoints");
    ReflectionTestUtils.setField(checkpointStore, "checkpointDirectory", root.toString());
    Path link = Files.createSymbolicLink(tempDir.resolve("link"), root.getParent());
    CheckpointStore otherStore = newStore(link.resolve("voj-testpoints"));

    Path first = checkpointStore.open();
    Path judging =
        Files.createDirectory(checkpointStore.getWorkDirectory().resolve("voj-1000-abc"));
    Path second = otherStore.open();

    Assertions.assertEquals(root.resolve("judger-0"), first);
    Assertions.assertEquals(root.resolve("judger-1"), second);
    Assertions.assertEquals(root.resolve("work-0"), judging.getParent());
    Assertions.assertEquals(root.resolve("work-1"), otherStore.getWorkDirectory());
    Assertions.assertEquals(first, checkpointStore.open());
    Assertions.assertTrue(Files.isDirectory(judging));
  }

  /**
   * Test case: the directory of a slot cannot be prepared, for a reason that is no IOException.
   * Expected result: the slot is released all the same, so that it can be claimed again.
   */
  @Test
  public void testClaimSlotReleasesTheSlotWhateverGoesWrong() throws Exception {
    Path root = Files.createDirectory(tempDir.resolve("voj-testpoints"));

    Assertions.assertThrows(
        IllegalStateException.class,
        () ->
            CheckpointStore.claimSlot(
                root,
                number -> {
                  throw new IllegalStateException("unexpected");
                }));

    try (ExclusiveFileLock lock =
        CheckpointStore.claimSlot(root, number -> Assertions.assertEquals(0, number))) {
      Assertions.assertEquals(root.resolve("judger-0.lock"), lock.getFile());
    }
  }

  /**
   * Test case: judger.checkpointDir is relative and ends with a slash. Expected result: the judger's
   * work directory is in the checkpoint root, absolute and normalized (the native sandbox resolves
   * paths after entering the work directory, isolate binds it as java.io.File spells it), and
   * others may only pass through it.
   */
  @Test
  public void testWorkDirectoryIsInTheCheckpointRoot() throws Exception {
    Path root = Path.of("").toAbsolutePath().relativize(tempDir);
    ReflectionTestUtils.setField(
        checkpointStore, "checkpointDirectory", root + "//voj-testpoints/./");

    Path workDirectory = checkpointStore.getWorkDirectory();

    Assertions.assertEquals(tempDir.resolve("voj-testpoints").resolve("work-0"), workDirectory);
    Assertions.assertEquals(
        "rwx--x--x", PosixFilePermissions.toString(Files.getPosixFilePermissions(workDirectory)));
  }

  /**
   * Test case: judgings that a judger with the same slot never finished (it was killed) left their
   * work directories behind, and another judger on the host is judging in the directory of its own
   * slot. Expected result: when the slot is claimed, the leftovers of its own slot are removed,
   * without following a link planted in them, and the other judger's are left alone.
   */
  @Test
  public void testOpenRemovesLeftoversOfItsOwnSlotOnly() throws Exception {
    Path root = createRoot();
    Path leftover = Files.createDirectories(root.resolve("work-0/voj-1000-abc"));
    Files.writeString(leftover.resolve("a.cpp"), "int main() {}");
    Path outside = Files.writeString(tempDir.resolve("outside.txt"), "keep");
    Files.createSymbolicLink(leftover.resolve("link"), outside);
    Path otherJudging = Files.createDirectories(root.resolve("work-1/voj-1001-def"));
    ReflectionTestUtils.setField(checkpointStore, "checkpointDirectory", root.toString());

    checkpointStore.open();

    Path workDirectory = checkpointStore.getWorkDirectory();
    Assertions.assertEquals(leftover.getParent(), workDirectory);
    try (Stream<Path> entries = Files.list(workDirectory)) {
      Assertions.assertEquals(0, entries.count());
    }
    Assertions.assertEquals("keep", Files.readString(outside));
    Assertions.assertTrue(Files.isDirectory(otherJudging));
  }

  /**
   * Test case: a judging that never completed left a directory without any permission behind (as
   * a submission running as the judger's own account can leave it). Expected result: the judger
   * still starts, and the leftovers are removed.
   */
  @Test
  public void testOpenRemovesLeftoversWithoutPermissions() throws Exception {
    Assumptions.assumeFalse(
        "root".equals(System.getProperty("user.name")), "root is never denied access");
    Path root = createRoot();
    Path locked = Files.createDirectories(root.resolve("work-0/voj-1000-abc/locked"));
    Files.writeString(locked.resolve("a.out"), "binary");
    Files.setPosixFilePermissions(locked, PosixFilePermissions.fromString("---------"));
    ReflectionTestUtils.setField(checkpointStore, "checkpointDirectory", root.toString());

    checkpointStore.open();

    try (Stream<Path> entries = Files.list(checkpointStore.getWorkDirectory())) {
      Assertions.assertEquals(0, entries.count());
    }
  }

  /**
   * Creates a checkpoint root as the judger does.
   *
   * @return the checkpoint root
   */
  private Path createRoot() throws IOException {
    return Files.createDirectory(
        tempDir.resolve("voj-testpoints"),
        PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rwx--x--x")));
  }

  /**
   * Creates the store of another judger on the host.
   *
   * @param checkpointRoot - its checkpoint root
   * @return the store
   */
  private static CheckpointStore newStore(Path checkpointRoot) {
    CheckpointStore store = new CheckpointStore();
    ReflectionTestUtils.setField(store, "checkpointDirectory", checkpointRoot.toString());
    return store;
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
