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
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.ProviderMismatchException;
import java.nio.file.attribute.AclEntry;
import java.nio.file.attribute.AclEntryFlag;
import java.nio.file.attribute.AclEntryPermission;
import java.nio.file.attribute.AclEntryType;
import java.nio.file.attribute.AclFileAttributeView;
import java.nio.file.attribute.UserPrincipal;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.util.ReflectionTestUtils;

import org.verwandlung.voj.judger.util.ExclusiveFileLock;
import org.verwandlung.voj.judger.util.FileSystemUtils;
import org.verwandlung.voj.judger.util.FileSystemUtilsTest;

/**
 * Tests of CheckpointStore on Windows: the ACLs of the directories it creates and the removal of
 * what judgings left behind.
 *
 * @author Haozhe Xie
 */
@EnabledOnOs(OS.WINDOWS)
public class CheckpointStoreWindowsTest {
  /**
   * Test case: the checkpoint root does not exist yet. Expected result: the judger creates it and
   * the directories of its slot with an ACL of their own, not the one they would inherit: only the
   * judger (the account of its process) may access the root and its cache; in its work directory,
   * what is created is the sandbox account's too, but for changing its ACL or owner.
   */
  @Test
  public void testOpenCreatesDirectoriesOnlyTheJudgerMayAccess() throws Exception {
    Path root = tempDir.resolve("voj-judger");
    UserPrincipal judger = FileSystemUtils.getJudgerAccount();
    CheckpointStore store = newStore(root, judger.getName());

    Path cache = store.open();
    Path work = store.getWorkDirectory();

    Set<AclEntryFlag> inherited =
        EnumSet.of(AclEntryFlag.FILE_INHERIT, AclEntryFlag.DIRECTORY_INHERIT);
    Set<AclEntryFlag> inheritedOnly =
        EnumSet.of(
            AclEntryFlag.FILE_INHERIT, AclEntryFlag.DIRECTORY_INHERIT, AclEntryFlag.INHERIT_ONLY);
    for (Path directory : new Path[] {root, cache}) {
      List<AclEntry> acl = getAcl(directory);
      Assertions.assertEquals(1, acl.size(), acl.toString());
      assertEntry(acl.get(0), judger, EnumSet.allOf(AclEntryPermission.class), inherited);
    }
    List<AclEntry> acl = getAcl(work);
    Assertions.assertEquals(2, acl.size(), acl.toString());
    assertEntry(
        acl.get(0),
        judger,
        EnumSet.complementOf(
            EnumSet.of(AclEntryPermission.WRITE_ACL, AclEntryPermission.WRITE_OWNER)),
        inheritedOnly);
    assertEntry(acl.get(1), judger, EnumSet.allOf(AclEntryPermission.class), inherited);
  }

  /**
   * Test case: the sandbox account does not exist. Expected result: the judger does not start (it
   * looks the account up before it creates the work directory); it leaves no work directory
   * behind, and releases its slot.
   */
  @Test
  public void testOpenWithoutTheSandboxAccountLeavesNoWorkDirectory() throws Exception {
    Path root = tempDir.resolve("voj-judger");
    CheckpointStore store = newStore(root, "voj-no-such-account");

    Assertions.assertThrows(IOException.class, store::open);

    Assertions.assertFalse(Files.exists(root.resolve("work-0"), LinkOption.NOFOLLOW_LINKS));
    try (ExclusiveFileLock lock = ExclusiveFileLock.tryAcquire(root.resolve("judger-0.lock"))) {
      Assertions.assertNotNull(lock);
    }
  }

  /**
   * Test case: a directory is created, but its ACL cannot be set (an entry names an account of
   * another provider). Expected result: the directory is removed again, rather than left with the
   * ACL it inherited, and the failure is reported.
   */
  @Test
  public void testDirectoryWhoseAclCannotBeSetIsRemoved() throws Exception {
    Path directory = tempDir.resolve("voj-judger");
    UserPrincipal foreign = () -> "voj-foreign-account";
    AclEntry entry =
        AclEntry.newBuilder()
            .setType(AclEntryType.ALLOW)
            .setPrincipal(foreign)
            .setPermissions(AclEntryPermission.READ_DATA)
            .build();

    Assertions.assertThrows(
        ProviderMismatchException.class,
        () -> CheckpointStore.createWindowsDirectory(directory, List.of(entry)));

    Assertions.assertFalse(Files.exists(directory, LinkOption.NOFOLLOW_LINKS));
  }

  /**
   * Test case: a judging that never completed left a junction to a missing target, a junction to a
   * directory outside, a read-only file and a directory whose ACL denies everyone in the work
   * directory of the slot. Expected result: the judger starts in an empty work directory, sets
   * nothing aside, and leaves the target of the junction alone.
   */
  @Test
  public void testOpenRemovesLeftoversWithJunctionsAndReadOnlyFiles() throws Exception {
    Path root = Files.createDirectory(tempDir.resolve("voj-judger"));
    Path leftover = Files.createDirectories(root.resolve("work-0").resolve("voj-1000-abc"));
    Path outside = Files.createDirectory(tempDir.resolve("outside"));
    Path secret = Files.writeString(outside.resolve("secret.txt"), "secret");
    FileSystemUtilsTest.createJunction(leftover.resolve("outside"), outside);
    Path gone = Files.createDirectory(tempDir.resolve("gone"));
    FileSystemUtilsTest.createJunction(leftover.resolve("gone"), gone);
    Files.delete(gone);
    Path readOnly = Files.writeString(leftover.resolve("a.exe"), "binary");
    Files.setAttribute(readOnly, "dos:readonly", true);
    Path denied = Files.createDirectory(leftover.resolve("denied"));
    Files.getFileAttributeView(denied, AclFileAttributeView.class).setAcl(List.of());
    CheckpointStore store = newStore(root, FileSystemUtils.getJudgerAccount().getName());

    store.open();

    try (Stream<Path> entries = Files.list(store.getWorkDirectory())) {
      Assertions.assertEquals(0, entries.count());
    }
    try (Stream<Path> entries = Files.list(root)) {
      Assertions.assertTrue(
          entries.noneMatch(entry -> entry.getFileName().toString().startsWith("trash-")));
    }
    Assertions.assertEquals("secret", Files.readString(secret));
  }

  /**
   * Checks an entry of an ACL.
   *
   * @param entry - the entry
   * @param principal - the account it must allow
   * @param permissions - what it must allow
   * @param flags - its inheritance flags
   */
  private static void assertEntry(
      AclEntry entry,
      UserPrincipal principal,
      Set<AclEntryPermission> permissions,
      Set<AclEntryFlag> flags) {
    Assertions.assertEquals(principal, entry.principal(), entry.toString());
    Assertions.assertEquals(permissions, entry.permissions(), entry.toString());
    Assertions.assertEquals(flags, entry.flags(), entry.toString());
  }

  /**
   * Reads the ACL of a file.
   *
   * @param file - the file
   * @return its ACL
   */
  private static List<AclEntry> getAcl(Path file) throws IOException {
    return Files.getFileAttributeView(file, AclFileAttributeView.class).getAcl();
  }

  /**
   * Creates the store of a judger.
   *
   * @param root - its checkpoint root
   * @param sandboxUsername - its sandbox account
   * @return the store
   */
  private CheckpointStore newStore(Path root, String sandboxUsername) {
    CheckpointStore store = new CheckpointStore();
    ReflectionTestUtils.setField(store, "checkpointDirectory", root.toString());
    ReflectionTestUtils.setField(store, "sandboxUsername", sandboxUsername);
    stores.add(store);
    return store;
  }

  /**
   * Releases the slots of the stores, as a judger does when it exits: a held lock file cannot be
   * deleted on Windows, so the temporary directory could not be removed otherwise.
   */
  @AfterEach
  public void releaseSlots() throws IOException {
    for (CheckpointStore store : stores) {
      ExclusiveFileLock lock = (ExclusiveFileLock) ReflectionTestUtils.getField(store, "slotLock");
      if (lock != null) {
        lock.close();
      }
    }
  }

  /** The stores of the test. */
  private final List<CheckpointStore> stores = new ArrayList<>();

  /** A temporary directory managed by JUnit. */
  @TempDir Path tempDir;
}
