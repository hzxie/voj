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
import static org.mockito.Mockito.verify;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;

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
  @Mock private CheckpointProvider checkpointProvider;

  /** The CheckpointStore object under test. */
  @InjectMocks private CheckpointStore checkpointStore;

  /** A temporary directory managed by JUnit. */
  @TempDir Path tempDir;
}
