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

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.util.AopTestUtils;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;

import org.verwandlung.voj.judger.mapper.LanguageMapper;
import org.verwandlung.voj.judger.model.Language;
import org.verwandlung.voj.judger.model.Problem;
import org.verwandlung.voj.judger.model.Submission;
import org.verwandlung.voj.judger.util.FileSystemUtils;

/**
 * Behavioural tests for the sandbox: they compile and run small programs that deliberately exceed a
 * limit (or misbehave) and assert that the verdict is the expected one.
 *
 * <p>These tests exercise whichever {@link SandboxRunner} is active ({@code judger.sandbox}); in CI
 * that is the native runner, while on a privileged Linux host with {@code judger.sandbox=isolate}
 * the very same tests validate isolate.
 *
 * <p>The bounded tests (memory/time/runtime) are safe for CI: the offending program is killed quickly
 * and never allocates more than a couple of hundred megabytes. The destructive tests (a fork bomb)
 * are tagged {@code sandbox-stress} and excluded from the default build, because if the sandbox
 * failed to contain them they would harm the runner; run them only against a sandbox you trust to
 * contain them (e.g. isolate) via {@code mvn test -Psandbox-stress}.
 *
 * @author Haozhe Xie
 */
@ExtendWith(SpringExtension.class)
@Transactional
@ContextConfiguration({"classpath:test-spring-context.xml"})
public class SandboxBehaviorTest {
  /**
   * Test case: a program that allocates far more memory than its limit. Expected result: Memory
   * Limit Exceeded.
   */
  @Test
  public void testMemoryLimitExceeded() throws Exception {
    Assertions.assertEquals("MLE", judge(900001L, MEMORY_BOMB_CODE, 5000, 65536));
  }

  /**
   * Test case: a program that allocates far more than its memory limit in a single allocation
   * ({@code std::vector<int>(1e9)}, 4 GB under 256 MB). Expected result: Memory Limit Exceeded, not
   * the runtime error of the std::bad_alloc that a refused allocation would end in.
   */
  @Test
  public void testSingleHugeAllocationIsMemoryLimitExceeded() throws Exception {
    Assertions.assertEquals("MLE", judge(900015L, HUGE_ALLOCATION_CODE, 5000, 262144));
  }

  /**
   * Test case: a program that aborts (SIGABRT) while using little memory, under a memory limit.
   * Expected result: Runtime Error.
   */
  @Test
  public void testAbortIsARuntimeError() throws Exception {
    Assertions.assertEquals("RE", judge(900016L, ABORT_CODE, 5000, 262144));
  }

  /**
   * Test case: a program that loops forever. Expected result: Time Limit Exceeded.
   */
  @Test
  public void testTimeLimitExceeded() throws Exception {
    // No memory limit: this case is purely about the time limit, so it must not depend on the
    // sandbox's memory measurement.
    Assertions.assertEquals("TLE", judge(900002L, INFINITE_LOOP_CODE, 1000, 0));
  }

  /**
   * Test case: a program that burns more CPU time than its limit in four threads, and then exits
   * with 0. Expected result: Time Limit Exceeded, whether or not it was stopped before it exited.
   */
  @Test
  public void testExitZeroOverTheTimeLimit() throws Exception {
    Assertions.assertEquals("TLE", judge(900013L, CPU_THREADS_CODE, 1000, 0));
  }

  /**
   * Test case: a program that writes far more than the output limit (256 MB) to standard output.
   * Expected result: Output Limit Exceeded. On Linux, both when the kernel's SIGXFSZ kills it and
   * when it ignores the signal, so that its writes fail, and exits with 0; on Windows, where the
   * runner polls the size of the output file, when it is killed for it.
   */
  @Test
  public void testOutputLimitExceeded() throws Exception {
    Assumptions.assumeTrue("native".equalsIgnoreCase(sandbox), "This tests the native runner.");
    try {
      Assertions.assertEquals("OLE", judge(900014L, OUTPUT_BOMB_CODE, 10000, 262144));
      if (!isLinux()) {
        return;
      }
      Files.deleteIfExists(Path.of(workBaseDirectory, "voj-sandbox-900014", "output.txt"));
      Assertions.assertEquals("OLE", judge(900014L, OUTPUT_BOMB_CODE, 10000, 262144, "ignore"));
    } finally {
      Files.deleteIfExists(Path.of(workBaseDirectory, "voj-sandbox-900014", "output.txt"));
    }
  }

  /**
   * Test case: a program that exits with a non-zero status. Expected result: Runtime Error.
   */
  @Test
  public void testRuntimeError() throws Exception {
    // Regression test for the memory-measurement pollution bug: a program that exits non-zero while
    // using little memory must be a Runtime Error, not MLE, even with a memory limit set. It used to
    // be reported with the JVM's resident memory (shared across the fork/exec window) and so was
    // misclassified MLE whenever the limit was below the judger JVM's footprint.
    Assertions.assertEquals("RE", judge(900003L, RUNTIME_ERROR_CODE, 5000, 262144));
  }

  /**
   * Test case: a trivial program, timed from start to verdict. Expected result: the cleanup after
   * the run (killing whatever the program left behind) does not add a fixed delay: the whole run
   * takes well under a second.
   */
  @Test
  public void testCleanupDoesNotDelayTheRun() throws Exception {
    Language cpp = languageMapper.getLanguageUsingSlug("text/x-c++src");
    Problem problem =
        new Problem(900008L, true, "sandbox-behavior", 1000, 65536, "", "", "", "", "", "");
    Submission submission = new Submission();
    submission.setSubmissionId(900008L);
    submission.setProblem(problem);
    submission.setLanguage(cpp);
    submission.setCode(RUNTIME_ERROR_CODE);
    String workDirectory = workBaseDirectory + "/voj-sandbox-900008";
    preprocessor.createTestCode(submission, workDirectory, "sandboxtest");
    compiler.getCompileResult(submission, workDirectory, "sandboxtest");
    Path inputFile = Files.write(Path.of(workDirectory, "input.txt"), new byte[0]);

    long startNanos = System.nanoTime();
    programRunner.getRuntimeResult(
        submission, workDirectory, "sandboxtest", inputFile.toString(), workDirectory + "/out.txt");
    long elapsedMs = (System.nanoTime() - startNanos) / 1_000_000;
    Assertions.assertTrue(elapsedMs < 800, "A trivial run took " + elapsedMs + " ms.");
  }

  /**
   * Test case: a fork bomb. Expected result: the run is contained and returns some non-accepted
   * verdict (rather than taking down the host). POSIX-only and tagged {@code sandbox-stress}, so it
   * is excluded from the default build.
   */
  @Test
  @Tag("sandbox-stress")
  public void testForkBombIsContained() throws Exception {
    Assumptions.assumeFalse(
        System.getProperty("os.name").contains("Windows"),
        "The fork-bomb test uses fork() and is POSIX-only.");
    String slug = judge(900004L, FORK_BOMB_CODE, 2000, 262144);
    Assertions.assertNotEquals("AC", slug, "A fork bomb must not be accepted.");
  }

  /**
   * Test case: a program that tries to create a user namespace with clone, clone3 and unshare, and
   * then checks that threads still work. Expected result: Accepted (every attempt is refused, see
   * USER_NAMESPACE_CODE).
   */
  @Test
  public void testUserNamespacesAreDenied() throws Exception {
    Assumptions.assumeTrue(
        System.getProperty("os.name").contains("Linux"), "User namespaces are Linux-only.");
    Assertions.assertEquals("AC", judge(900005L, USER_NAMESPACE_CODE, 5000, 262144));
  }

  /**
   * Test case: a program that tries to create processes with fork, the raw fork system call and
   * posix_spawn. Expected result: under the native sandbox, a run of the submission is a single
   * process: every attempt fails, which the program reports (see FORK_CODE): with EPERM where the C
   * library calls clone without CLONE_THREAD, with ENOSYS for fork, which is not allowlisted.
   */
  @Test
  public void testRunCannotCreateProcesses() throws Exception {
    Assumptions.assumeTrue("native".equalsIgnoreCase(sandbox), "This tests the native runner.");
    Assumptions.assumeTrue(isLinux(), "The test program uses fork() and posix_spawn().");
    Assertions.assertEquals("AC", judge(900011L, FORK_CODE, 5000, 262144));
    Assertions.assertEquals(
        "fork: EPERM\nSYS_fork: ENOSYS\nposix_spawn: EPERM\n",
        Files.readString(Path.of(workBaseDirectory, "voj-sandbox-900011", "output.txt")));
  }

  /**
   * Test case: a multi-threaded program (eight threads). Expected result: Accepted: a run may not
   * create processes, but it may create threads.
   */
  @Test
  public void testRunCanCreateThreads() throws Exception {
    Assertions.assertEquals("AC", judge(900012L, THREADS_CODE, 5000, 262144));
    // printf writes "\r\n" on Windows.
    Assertions.assertEquals(
        "8\n",
        Files.readString(Path.of(workBaseDirectory, "voj-sandbox-900012", "output.txt"))
            .replace("\r\n", "\n"));
  }

  /**
   * Test case: a program that touches 48 MB and then forks seven children that share those pages
   * until they exit. Expected result: Accepted under a 128 MB limit: the group's memory counts
   * each shared page once rather than once per process (as the sum of the resident set sizes, 8 x
   * 48 MB, would). Only for a sandbox that lets a run create processes (not the native one).
   */
  @Test
  public void testMemorySharedAfterForkIsCountedOnce() throws Exception {
    Assumptions.assumeFalse(
        System.getProperty("os.name").contains("Windows"), "This test uses fork().");
    Assumptions.assumeFalse(
        "native".equalsIgnoreCase(sandbox), "A native run cannot create processes.");
    Assertions.assertEquals("AC", judge(900006L, SHARED_MEMORY_CODE, 5000, 131072));
  }

  /**
   * Test case: a program that has three orphaned grandchildren, one after the other, each burn 600
   * ms of CPU time and exit, while it itself stays idle. Expected result: Time Limit Exceeded under
   * a 1000 ms limit: the CPU time of a process that exits after being orphaned still counts, even
   * though none of the program's processes ever waits for it. Only for a sandbox that lets a run
   * create processes (not the native one).
   */
  @Test
  public void testCpuTimeOfOrphansIsCounted() throws Exception {
    Assumptions.assumeFalse(
        System.getProperty("os.name").contains("Windows"), "This test uses fork().");
    Assumptions.assumeFalse(
        "native".equalsIgnoreCase(sandbox), "A native run cannot create processes.");
    Assertions.assertEquals("TLE", judge(900007L, ORPHAN_CPU_CODE, 1000, 262144));
  }

  /**
   * Test case: a program that tries to connect to the judger's database port on the loopback
   * interface and to an external address, and to create sockets of several non-local address
   * families. Expected result: Accepted (no connection is established, while an AF_UNIX socket pair
   * still works, see NETWORK_CODE). Under the native sandbox the program is told to be strict: the
   * seccomp filter must refuse every socket but AF_UNIX with EACCES, even where no private network
   * namespace can be created (e.g. a Docker container without CAP_SYS_ADMIN). isolate instead gives
   * the box its own, empty network namespace.
   */
  @Test
  public void testNetworkIsUnreachable() throws Exception {
    Assumptions.assumeTrue(
        System.getProperty("os.name").contains("Linux"), "The socket filter is Linux-only.");
    String mode = "native".equalsIgnoreCase(sandbox) ? "strict" : "lenient";
    Assertions.assertEquals("AC", judge(900009L, NETWORK_CODE, 10000, 262144, mode));
  }

  /**
   * Test case: the judger runs as root and the sandbox user does not exist. Expected result: the
   * native runner refuses the run, which is a system error (never a run as root, nor a verdict on
   * the submission).
   */
  @Test
  public void testMissingSandboxUserIsASystemError() {
    Assumptions.assumeTrue("native".equalsIgnoreCase(sandbox), "This tests the native runner.");
    Assumptions.assumeTrue(
        "root".equals(System.getProperty("user.name")), "Privileges are only dropped by root.");
    Object runner = AopTestUtils.getUltimateTargetObject(sandboxRunner);
    Object username = ReflectionTestUtils.getField(runner, "systemUsername");
    ReflectionTestUtils.setField(runner, "systemUsername", "voj-no-such-user");
    try {
      RunResult result = sandboxRunner.run("/bin/true", null, null, 1000, 65536, false);
      Assertions.assertEquals(Verdict.INTERNAL_ERROR, result.getVerdict());
    } finally {
      ReflectionTestUtils.setField(runner, "systemUsername", username);
    }
  }

  /**
   * Test case: a command whose executable does not exist. Expected result: a system error, for a
   * run as well as for a compilation (not a runtime error, nor a compile error with an empty log).
   */
  @Test
  public void testUnrunnableCommandIsASystemError() {
    Assumptions.assumeTrue("native".equalsIgnoreCase(sandbox), "This tests the native runner.");
    String command = "/voj-no-such-directory/program";
    Assertions.assertEquals(
        Verdict.INTERNAL_ERROR, sandboxRunner.run(command, null, null, 1000, 65536, false).getVerdict());
    Assertions.assertEquals(
        Verdict.INTERNAL_ERROR, sandboxRunner.run(command, null, null, 1000, 65536, true).getVerdict());
  }

  /**
   * Test case: what a compilation's standard input is. Expected result: /dev/null, never the
   * judger's own standard input (which a submission could otherwise read while it is compiled,
   * e.g. with {@code #include "/dev/stdin"}).
   */
  @Test
  public void testCompilationStandardInputIsDevNull() throws Exception {
    Assumptions.assumeTrue("native".equalsIgnoreCase(sandbox), "This tests the native runner.");
    Assumptions.assumeTrue(isLinux(), "This reads /proc/self/fd/0.");
    String workDirectory = createWorkDirectory(900010L);
    String logFile = workDirectory + "/stdin.log";
    RunResult result = sandboxRunner.run("readlink /proc/self/fd/0", null, logFile, 5000, 0, true);
    Assertions.assertEquals(Verdict.NORMAL, result.getVerdict());
    Assertions.assertEquals("/dev/null", Files.readString(Path.of(logFile)).strip());
  }

  /**
   * Test case: the working directory of a compilation. Expected result: the submission's work
   * directory, not the judger's own (which the sandbox user may not even be able to enter, e.g.
   * /root in the judger's Docker image, where the Go toolchain then fails to run its compiler), nor
   * the sandbox account's profile directory on Windows.
   */
  @Test
  public void testProgramRunsInItsWorkDirectory() throws Exception {
    Assumptions.assumeTrue("native".equalsIgnoreCase(sandbox), "This tests the native runner.");
    String workDirectory = createWorkDirectory(900017L);
    String logFile = workDirectory + "/cwd.log";
    String command = isLinux() ? "/bin/pwd" : "cmd /c cd";
    RunResult result = sandboxRunner.run(command, null, logFile, 5000, 0, true);
    Assertions.assertEquals(Verdict.NORMAL, result.getVerdict());
    String expected = Path.of(workDirectory).toRealPath().toString();
    String actual = Files.readString(Path.of(logFile)).strip();
    Assertions.assertTrue(
        isLinux() ? expected.equals(actual) : expected.equalsIgnoreCase(actual),
        "Expected " + expected + ", got " + actual);
  }

  /**
   * Test case: a run that tries to leave files where a later submission could find them (/tmp,
   * /var/tmp, /dev/shm), and to create System V IPC objects, a POSIX message queue, a key in the
   * user's keyring and a memory file, which would outlive it or escape its memory limit; then a
   * later run that looks for the files. Expected result: every attempt is refused (Landlock
   * confines writes to the work directory, where the run can still create files; the seccomp
   * filters refuse the rest: memfd_create is not allowlisted), and the later run finds nothing. (Docker's default seccomp profile
   * refuses the keyring system calls on its own: run the tests unconfined to test the filter's.)
   */
  @Test
  public void testRunLeavesNothingBehind() throws Exception {
    Assumptions.assumeTrue("native".equalsIgnoreCase(sandbox), "This tests the native runner.");
    Assumptions.assumeTrue(isLinux(), "Landlock and the seccomp filter are Linux-only.");
    String name = "voj-sandbox-leak-900018";
    String[] directories = {"/tmp", "/var/tmp", "/dev/shm"};
    for (String directory : directories) {
      Files.deleteIfExists(Path.of(directory, name)); // left behind by an unconfined sandbox
    }
    Assertions.assertEquals("AC", judge(900018L, LEAVE_BEHIND_CODE, 5000, 262144, "leave " + name));
    Assertions.assertEquals(
        String.join(
            "\n",
            "/tmp: EACCES",
            "/var/tmp: EACCES",
            "/dev/shm: EACCES",
            "work: created",
            "/dev/null: written",
            "shmget: EPERM",
            "semget: EPERM",
            "msgget: EPERM",
            "mq_open: EPERM",
            "add_key: EPERM",
            "request_key: EPERM",
            "keyctl: EPERM",
            "memfd_create: ENOSYS",
            ""),
        Files.readString(Path.of(workBaseDirectory, "voj-sandbox-900018", "output.txt")));
    for (String directory : directories) {
      Assertions.assertFalse(Files.exists(Path.of(directory, name)), directory + " was written");
    }
    Assertions.assertEquals("AC", judge(900019L, LEAVE_BEHIND_CODE, 5000, 262144, "find " + name));
    Assertions.assertEquals(
        "/tmp: absent\n/var/tmp: absent\n/dev/shm: absent\n",
        Files.readString(Path.of(workBaseDirectory, "voj-sandbox-900019", "output.txt")));
  }

  /**
   * Test case: a compilation that writes outside its work directory, and one that writes inside.
   * Expected result: the first fails and leaves nothing behind, the second succeeds; the compiler's
   * HOME and TMPDIR are the work directory, where it can write its temporary files.
   */
  @Test
  public void testCompilationWritesOnlyInItsWorkDirectory() throws Exception {
    Assumptions.assumeTrue("native".equalsIgnoreCase(sandbox), "This tests the native runner.");
    Assumptions.assumeTrue(isLinux(), "Landlock is Linux-only.");
    String workDirectory = createWorkDirectory(900020L);
    String logFile = workDirectory + "/compile.log";
    Path outside = Path.of("/tmp", "voj-sandbox-leak-900020");
    Files.deleteIfExists(outside);
    Assertions.assertEquals(
        Verdict.RUNTIME_ERROR,
        sandboxRunner.run("/usr/bin/touch " + outside, null, logFile, 5000, 0, true).getVerdict());
    Assertions.assertFalse(Files.exists(outside));
    Assertions.assertEquals(
        Verdict.NORMAL,
        sandboxRunner
            .run("/usr/bin/touch " + workDirectory + "/inside", null, logFile, 5000, 0, true)
            .getVerdict());
    Assertions.assertTrue(Files.exists(Path.of(workDirectory, "inside")));
    Assertions.assertEquals(
        Verdict.NORMAL, sandboxRunner.run("/usr/bin/env", null, logFile, 5000, 0, true).getVerdict());
    String environment = "\n" + Files.readString(Path.of(logFile));
    Assertions.assertTrue(environment.contains("\nHOME=" + workDirectory + "\n"), environment);
    Assertions.assertTrue(environment.contains("\nTMPDIR=" + workDirectory + "\n"), environment);
  }

  /**
   * Test case: a run on Windows that tries to write where a later run of the sandbox account would
   * find it: a variable of the account (HKCU\Environment, whose variables reach the processes the
   * account starts), a file in its profile and one in C:\Users\Public; and that creates a file in
   * its work directory and one in its USERPROFILE. Expected result: the run is Low integrity, so
   * every write is refused but those into the work directory, which USERPROFILE points into.
   */
  @Test
  public void testRunWritesOnlyInItsWorkDirectoryOnWindows() throws Exception {
    Assumptions.assumeTrue("native".equalsIgnoreCase(sandbox), "This tests the native runner.");
    Assumptions.assumeFalse(isLinux(), "Mandatory integrity control is Windows-only.");
    Assertions.assertEquals(
        "AC", judge(900041L, WINDOWS_LEAVE_BEHIND_CODE, 5000, 262144, "voj-sandbox-leak-900041"));
    Path workDirectory = Path.of(workBaseDirectory, "voj-sandbox-900041");
    Assertions.assertEquals(
        String.join(
            "\n",
            "integrity: low",
            "HKCU\\Environment: denied",
            "profile: denied",
            "public: denied",
            "work: created",
            "USERPROFILE: created",
            ""),
        Files.readString(workDirectory.resolve("output.txt")).replace("\r\n", "\n"));
    Assertions.assertTrue(Files.exists(workDirectory.resolve("voj-sandbox-leak-900041")));
    Assertions.assertTrue(Files.exists(workDirectory.resolve("home.txt")));
  }

  /**
   * Test case: a run on Windows that creates, in its work directory, a directory with a file in it
   * and an ACL that allows nobody anything, which it may set as their owner, and which keeps the
   * judger from listing, deleting or changing the ACL of either. Expected result: the judger, as
   * an elevated administrator, removes the work directory anyway, by its backup privileges.
   */
  @Test
  public void testWhatARunDeniesTheJudgerIsRemovedOnWindows() throws Exception {
    Assumptions.assumeTrue("native".equalsIgnoreCase(sandbox), "This tests the native runner.");
    Assumptions.assumeFalse(isLinux(), "ACLs are Windows-only here.");
    Assertions.assertEquals("AC", judge(900042L, WINDOWS_DENY_JUDGER_CODE, 5000, 262144));
    Path workDirectory = Path.of(workBaseDirectory, "voj-sandbox-900042");
    Assertions.assertEquals(
        "locked\n", Files.readString(workDirectory.resolve("output.txt")).replace("\r\n", "\n"));
    Assertions.assertTrue(Files.exists(workDirectory.resolve("locked")));

    FileSystemUtils.deleteRecursively(workDirectory);

    Assertions.assertFalse(Files.exists(workDirectory));
  }

  /**
   * Test case: a run that tries to empty a file of the judger outside its work directory, such as
   * the cached expected output, with truncate(2) and with open(O_RDONLY | O_TRUNC), neither of which
   * Landlock confined before ABI 3 (Linux 6.2). Expected result: both are refused and the file is
   * intact. When the judger runs as root, the sandbox user cannot write the file anyway; otherwise
   * the program runs as the judger's own user and only Landlock stops it (on an older kernel, such
   * a run is refused as a system error rather than started).
   */
  @Test
  public void testRunCannotTruncateTheJudgersFiles() throws Exception {
    Assumptions.assumeTrue("native".equalsIgnoreCase(sandbox), "This tests the native runner.");
    Assumptions.assumeTrue(isLinux(), "Landlock is Linux-only.");
    Path expectedOutput = Path.of(workBaseDirectory, "voj-sandbox-900022-output.txt");
    Files.writeString(expectedOutput, "42\n");
    try {
      Assertions.assertEquals(
          "AC", judge(900022L, TRUNCATE_CODE, 5000, 262144, expectedOutput.toString()));
      Assertions.assertEquals(
          "truncate: EACCES\nopen: EACCES\n",
          Files.readString(Path.of(workBaseDirectory, "voj-sandbox-900022", "output.txt")));
      Assertions.assertEquals("42\n", Files.readString(expectedOutput));
    } finally {
      Files.deleteIfExists(expectedOutput);
    }
  }

  /**
   * Test case: a run that tries to move its memory out of its resident set while keeping it, which
   * would let it use more than its memory limit without its measured peak reaching it: paging out
   * (madvise MADV_PAGEOUT, also with garbage in the upper half of the advice's register, MADV_COLD,
   * process_madvise), and unmapping shared anonymous memory (MAP_SHARED | MAP_ANONYMOUS), whose
   * pages would stay in memory. Expected result: every attempt fails, with EPERM (process_madvise,
   * which is not allowlisted, with ENOSYS), while the advice the runtimes' allocators use
   * (MADV_DONTNEED, MADV_FREE) still works.
   */
  @Test
  public void testRunCannotPageOutItsMemory() throws Exception {
    Assumptions.assumeTrue("native".equalsIgnoreCase(sandbox), "This tests the native runner.");
    Assumptions.assumeTrue(isLinux(), "The seccomp filter is Linux-only.");
    Assertions.assertEquals("AC", judge(900023L, PAGE_OUT_CODE, 5000, 262144));
    Assertions.assertEquals(
        String.join(
            "\n",
            "MADV_PAGEOUT: EPERM",
            "MADV_PAGEOUT (upper bits set): EPERM",
            "MADV_COLD: EPERM",
            "process_madvise: ENOSYS",
            "MAP_SHARED | MAP_ANONYMOUS: EPERM",
            "MADV_DONTNEED: done",
            "MADV_FREE: done",
            ""),
        Files.readString(Path.of(workBaseDirectory, "voj-sandbox-900023", "output.txt")));
  }

  /**
   * Test case: a run that makes system calls which are not on the native sandbox's allowlist, each
   * harmless had it been allowed: querying its personality, creating an inotify instance, making a
   * symbolic link and changing a file's mode in its work directory, and connecting an AF_UNIX
   * socket. Expected result: each fails with ENOSYS, while the calls around them that are allowed
   * (creating the socket and the file) succeed.
   */
  @Test
  public void testSystemCallsOffTheAllowlistAreNotImplemented() throws Exception {
    Assumptions.assumeTrue("native".equalsIgnoreCase(sandbox), "This tests the native runner.");
    Assumptions.assumeTrue(isLinux(), "The seccomp filters are Linux-only.");
    Assertions.assertEquals("AC", judge(900024L, UNLISTED_SYSCALLS_CODE, 5000, 262144));
    Assertions.assertEquals(
        String.join(
            "\n",
            "personality: ENOSYS",
            "inotify_init1: ENOSYS",
            "symlink: ENOSYS",
            "chmod: ENOSYS",
            "connect: ENOSYS",
            ""),
        Files.readString(Path.of(workBaseDirectory, "voj-sandbox-900024", "output.txt")));
  }

  /**
   * Test case: a run that opens descriptors until it cannot, tries to raise its descriptor limit,
   * and creates a thread with a descriptor table of its own (clone with CLONE_THREAD but without
   * CLONE_FILES), which would get a limit of its own. Expected result: the limit is 128 (soft and
   * hard), cannot be raised (EPERM), opening fails with EMFILE once 128 descriptors are open, and
   * the thread without CLONE_FILES is refused (EPERM) while one with it is created.
   */
  @Test
  public void testRunHasFewDescriptors() throws Exception {
    Assumptions.assumeTrue("native".equalsIgnoreCase(sandbox), "This tests the native runner.");
    Assumptions.assumeTrue(isLinux(), "RLIMIT_NOFILE and clone are Linux-only.");
    Assertions.assertEquals("AC", judge(900025L, DESCRIPTOR_LIMIT_CODE, 5000, 262144));
    Assertions.assertEquals(
        String.join(
            "\n",
            "limit: 128 128",
            "raise: EPERM",
            "thread without CLONE_FILES: EPERM",
            "thread with CLONE_FILES: created",
            "open: EMFILE at 128",
            ""),
        Files.readString(Path.of(workBaseDirectory, "voj-sandbox-900025", "output.txt")));
  }

  /**
   * Test case: a run that reads and changes resource limits with prlimit64, its own (pid 0) and
   * its parent's (the judger, of the same user when the judger is not root). Expected result: its
   * own limits can be read and lowered, while prlimit64 on another process is not allowlisted
   * (ENOSYS), even only to read its limits.
   */
  @Test
  public void testRunCanOnlyChangeItsOwnLimits() throws Exception {
    Assumptions.assumeTrue("native".equalsIgnoreCase(sandbox), "This tests the native runner.");
    Assumptions.assumeTrue(isLinux(), "prlimit64 is Linux-only.");
    Assertions.assertEquals("AC", judge(900026L, PRLIMIT_CODE, 5000, 262144));
    Assertions.assertEquals(
        String.join(
            "\n",
            "own (read): done",
            "own (lower): done",
            "parent (read): ENOSYS",
            "parent (lower): ENOSYS",
            ""),
        Files.readString(Path.of(workBaseDirectory, "voj-sandbox-900026", "output.txt")));
  }

  /**
   * Test case: a run that looks at its session and opens its controlling terminal, as a program
   * would that pushes input into the judger's terminal (TIOCSTI). Expected result: the run leads a
   * session and a process group of its own, so it has no controlling terminal (ENXIO), whatever
   * terminal the judger was started from, and it cannot leave them (setsid: EPERM).
   */
  @Test
  public void testRunHasNoControllingTerminal() throws Exception {
    Assumptions.assumeTrue("native".equalsIgnoreCase(sandbox), "This tests the native runner.");
    Assumptions.assumeTrue(isLinux(), "The test program uses getsid().");
    Assertions.assertEquals("AC", judge(900027L, TERMINAL_CODE, 5000, 262144));
    Assertions.assertEquals(
        String.join(
            "\n",
            "session leader: yes",
            "process group leader: yes",
            "/dev/tty: ENXIO",
            "setsid: EPERM",
            ""),
        Files.readString(Path.of(workBaseDirectory, "voj-sandbox-900027", "output.txt")));
  }

  /**
   * Test case: a run that sends on an AF_UNIX datagram socket pair with send, with sendto without
   * an address and with write, and that sends to the address of the system log's socket
   * (/dev/log) with sendto and with sendmsg, which needs no connect. Expected result: the first
   * three work, while sendto with an address and sendmsg are not allowlisted (ENOSYS).
   */
  @Test
  public void testRunCannotSendToAnAddress() throws Exception {
    Assumptions.assumeTrue("native".equalsIgnoreCase(sandbox), "This tests the native runner.");
    Assumptions.assumeTrue(isLinux(), "The seccomp filters are Linux-only.");
    Assertions.assertEquals("AC", judge(900028L, SEND_CODE, 5000, 262144));
    Assertions.assertEquals(
        String.join(
            "\n",
            "send: done",
            "sendto (no address): done",
            "write: done",
            "received: 3",
            "sendto /dev/log: ENOSYS",
            "sendmsg /dev/log: ENOSYS",
            ""),
        Files.readString(Path.of(workBaseDirectory, "voj-sandbox-900028", "output.txt")));
  }

  /**
   * Test case: a run that creates threads with an explicit scheduling policy
   * (PTHREAD_EXPLICIT_SCHED, as Free Pascal's threads are), with which the C library calls
   * sched_setscheduler on the new thread, and that changes its parent's policy. Expected result:
   * a thread with the policy every process has (SCHED_OTHER) is created, while one with another
   * policy (SCHED_FIFO) is not, nor is the parent's changed: sched_setscheduler is allowlisted to
   * SCHED_OTHER only (ENOSYS otherwise).
   */
  @Test
  public void testRunCanCreateThreadsWithAnExplicitPolicy() throws Exception {
    Assumptions.assumeTrue("native".equalsIgnoreCase(sandbox), "This tests the native runner.");
    Assumptions.assumeTrue(isLinux(), "The seccomp filters are Linux-only.");
    Assertions.assertEquals("AC", judge(900040L, EXPLICIT_SCHED_CODE, 5000, 262144));
    Assertions.assertEquals(
        String.join(
            "\n",
            "explicit SCHED_OTHER: created",
            "explicit SCHED_FIFO: ENOSYS",
            "parent SCHED_IDLE: ENOSYS",
            ""),
        Files.readString(Path.of(workBaseDirectory, "voj-sandbox-900040", "output.txt")));
  }

  /**
   * Test case: a run that tries to enlarge the kernel buffer of a pipe (fcntl F_SETPIPE_SZ) and
   * those of a socket pair (setsockopt SO_SNDBUF, SO_RCVBUF, SO_SNDBUFFORCE, SO_RCVBUFFORCE), and
   * reads them and sets another socket option. Expected result: enlarging is refused (EPERM),
   * reading the sizes and setting another option work.
   */
  @Test
  public void testRunCannotEnlargeItsKernelBuffers() throws Exception {
    Assumptions.assumeTrue("native".equalsIgnoreCase(sandbox), "This tests the native runner.");
    Assumptions.assumeTrue(isLinux(), "The seccomp filters are Linux-only.");
    Assertions.assertEquals("AC", judge(900033L, KERNEL_BUFFER_CODE, 5000, 262144));
    Assertions.assertEquals(
        String.join(
            "\n",
            "F_GETPIPE_SZ: done",
            "F_SETPIPE_SZ: EPERM",
            "getsockopt SO_SNDBUF: done",
            "SO_SNDBUF: EPERM",
            "SO_RCVBUF: EPERM",
            "SO_SNDBUFFORCE: EPERM",
            "SO_RCVBUFFORCE: EPERM",
            "SO_PASSCRED: done",
            ""),
        Files.readString(Path.of(workBaseDirectory, "voj-sandbox-900033", "output.txt")));
  }

  /**
   * Test case: a run that reads and tries to raise its limit of queued signals (RLIMIT_SIGPENDING),
   * then creates POSIX timers, and queues a blocked real-time signal to itself, until it cannot.
   * Expected result: the limit is 1024 (soft and hard) and cannot be raised (EPERM); timer_create
   * and sigqueue fail with EAGAIN after at most 1024 (the count is per user, so the judger's own
   * pending signals count too when it is not root).
   */
  @Test
  public void testRunHasFewQueuedSignals() throws Exception {
    Assumptions.assumeTrue("native".equalsIgnoreCase(sandbox), "This tests the native runner.");
    Assumptions.assumeTrue(isLinux(), "RLIMIT_SIGPENDING is Linux-only.");
    Assertions.assertEquals("AC", judge(900034L, SIGNAL_QUEUE_CODE, 5000, 262144));
    Assertions.assertEquals(
        String.join(
            "\n",
            "limit: 1024 1024",
            "raise: EPERM",
            "timer_create: EAGAIN after at most 1024: yes",
            "sigqueue: EAGAIN after at most 1024: yes",
            ""),
        Files.readString(Path.of(workBaseDirectory, "voj-sandbox-900034", "output.txt")));
  }

  /**
   * Test case: a program whose static data (1 TB of bss) alone exceeds the kernel memory backstop
   * (RLIMIT_DATA), so that it cannot even be loaded. Expected result: Memory Limit Exceeded, not the
   * runtime error of the SIGSEGV the kernel kills such a program with while loading it. The program
   * is compiled with -mcmodel=medium, which static data beyond 2 GB needs on x86-64.
   */
  @Test
  public void testHugeStaticDataIsMemoryLimitExceeded() throws Exception {
    Assumptions.assumeTrue("native".equalsIgnoreCase(sandbox), "This tests the native runner.");
    Assumptions.assumeTrue(isLinux(), "RLIMIT_DATA is Linux-only.");
    Assumptions.assumeTrue(
        "amd64".equals(System.getProperty("os.arch")), "-mcmodel=medium is x86-64 only.");
    String workDirectory = createWorkDirectory(900021L);
    Path source = Files.writeString(Path.of(workDirectory, "static.cpp"), HUGE_STATIC_DATA_CODE);
    String program = workDirectory + "/static.exe";
    RunResult compilation =
        sandboxRunner.run(
            "g++ -O2 -mcmodel=medium -o " + program + " " + source,
            null,
            workDirectory + "/compile.log",
            20000,
            0,
            true);
    Assertions.assertEquals(
        Verdict.NORMAL,
        compilation.getVerdict(),
        Files.readString(Path.of(workDirectory, "compile.log")));
    RunResult run = sandboxRunner.run(program, null, workDirectory + "/output.txt", 5000, 65536, false);
    Assertions.assertEquals(Verdict.MEMORY_LIMIT_EXCEEDED, run.getVerdict());
  }

  /**
   * Test case: a Java program that allocates 2 GB in all, in arrays of 1 MB that it drops at once,
   * under a 256 MB limit (512 MB with Java's memory multiplier), and writes a line to standard
   * error. Expected result: Accepted, using less than the problem's 256 MB, with its standard output
   * alone as the output: the JVM is told the limit (see JavaOptions), so it collects its garbage long
   * before the limit, rather than growing its heap to a quarter of the host's RAM first; and neither
   * the program's line nor the java launcher's note about JDK_JAVA_OPTIONS on standard error reaches
   * the output.
   */
  @Test
  public void testJavaGarbageWithinTheLimitIsAccepted() throws Exception {
    Assumptions.assumeTrue(isLinux(), "The test needs a JDK on the sandbox user's PATH.");
    Map<String, Object> result =
        judge(900029L, "text/x-java", JAVA_GARBAGE_CODE, 5000, 262144, "");
    Assertions.assertEquals("AC", result.get("runtimeResult"), result.toString());
    Assertions.assertTrue((Integer) result.get("usedMemory") < 262144, result.toString());
    Assertions.assertEquals(
        "2048\n", Files.readString(Path.of(workBaseDirectory, "voj-sandbox-900029", "output.txt")));
  }

  /**
   * Test case: a Java program that keeps 768 MB of arrays, under a 256 MB limit (512 MB with Java's
   * memory multiplier). Expected result: Memory Limit Exceeded, reported at no less than the limit:
   * its heap, sized to the limit, runs out, and the JVM, told to exit when it runs out of memory
   * (see JavaOptions), ends in a way the judger tells from a runtime error.
   */
  @Test
  public void testJavaThatNeedsMoreThanItsLimitIsMemoryLimitExceeded() throws Exception {
    Assumptions.assumeTrue(isLinux(), "The test needs a JDK on the sandbox user's PATH.");
    Map<String, Object> result = judge(900030L, "text/x-java", JAVA_HOARD_CODE, 5000, 262144, "");
    Assertions.assertEquals("MLE", result.get("runtimeResult"), result.toString());
    Assertions.assertTrue((Integer) result.get("usedMemory") >= 524288, result.toString());
  }

  /**
   * Test case: a Java program that asks for a single array of 1.6 GB, under a 256 MB limit (512 MB
   * with Java's memory multiplier). Expected result: Memory Limit Exceeded, reported at no less than
   * the limit, although the JVM refuses the array before touching any of it (its peak is some 40
   * MB): the JVM reports that it ran out of memory, which the judger goes by.
   */
  @Test
  public void testJavaThatAsksForTooLargeAnArrayIsMemoryLimitExceeded() throws Exception {
    Assumptions.assumeTrue(isLinux(), "The test needs a JDK on the sandbox user's PATH.");
    Map<String, Object> result =
        judge(900035L, "text/x-java", JAVA_HUGE_ARRAY_CODE, 5000, 262144, "");
    Assertions.assertEquals("MLE", result.get("runtimeResult"), result.toString());
    Assertions.assertTrue((Integer) result.get("usedMemory") >= 524288, result.toString());
  }

  /**
   * Test case: a Java program that keeps 768 MB of arrays, under a 256 MB limit (512 MB with Java's
   * memory multiplier), while another of its threads keeps writing lines to standard output from
   * the time half of it is held. Expected result: Memory Limit Exceeded: the other thread usually
   * writes a few more lines after the JVM's report, before the JVM exits, so the report need not be
   * the last line of the output.
   */
  @Test
  public void testJavaThatRunsOutOfMemoryWhileAnotherThreadWritesIsMemoryLimitExceeded()
      throws Exception {
    Assumptions.assumeTrue(isLinux(), "The test needs a JDK on the sandbox user's PATH.");
    Map<String, Object> result =
        judge(900041L, "text/x-java", JAVA_HOARD_WHILE_WRITING_CODE, 5000, 262144, "");
    Assertions.assertEquals("MLE", result.get("runtimeResult"), result.toString());
  }

  /**
   * Test case: a Java program that keeps 768 MB of arrays, under a 256 MB limit, started through
   * {@code /usr/bin/env java} rather than by the java launcher itself. Expected result: Memory Limit
   * Exceeded: what tells a JVM that ran out of memory is its own report, not the command.
   */
  @Test
  public void testJavaStartedThroughEnvThatRunsOutOfMemoryIsMemoryLimitExceeded() throws Exception {
    Assumptions.assumeTrue(isLinux(), "The test needs a JDK on the sandbox user's PATH.");
    Language java = languageMapper.getLanguageUsingSlug("text/x-java");
    Language language =
        new Language(
            java.getLanguageSlug(),
            java.getLanguageName(),
            java.getCompileCommand(),
            "/usr/bin/env " + java.getRunCommand());
    language.setTimeMultiplier(java.getTimeMultiplier());
    language.setMemoryMultiplier(java.getMemoryMultiplier());
    Map<String, Object> result = judge(900036L, language, JAVA_HOARD_CODE, 5000, 262144, "");
    Assertions.assertEquals("MLE", result.get("runtimeResult"), result.toString());
  }

  /**
   * Test case: a Java program that runs 100 tasks, each sleeping 20 ms, with CompletableFuture's
   * supplyAsync (no executor), and one that sums a parallel stream, under the run's 64 threads.
   * Expected result: Accepted, with the right output: the common fork-join pool, which runs both,
   * has a parallelism of 2 (see JavaOptions), rather than 1, with which supplyAsync would start a
   * thread for every task (OpenJDK 21), past the thread limit.
   */
  @Test
  public void testJavaConcurrencyKeepsToTheThreadLimit() throws Exception {
    Assumptions.assumeTrue(isLinux(), "The test needs a JDK on the sandbox user's PATH.");
    Map<String, Object> result =
        judge(900038L, "text/x-java", JAVA_COMPLETABLE_FUTURE_CODE, 5000, 262144, "");
    Assertions.assertEquals("AC", result.get("runtimeResult"), result.toString());
    Assertions.assertEquals(
        "4950 2\n",
        Files.readString(Path.of(workBaseDirectory, "voj-sandbox-900038", "output.txt")));
    result = judge(900039L, "text/x-java", JAVA_PARALLEL_STREAM_CODE, 5000, 262144, "");
    Assertions.assertEquals("AC", result.get("runtimeResult"), result.toString());
    Assertions.assertEquals(
        "149999998\n",
        Files.readString(Path.of(workBaseDirectory, "voj-sandbox-900039", "output.txt")));
  }

  /**
   * Test case: a Java program that dies of an uncaught exception, a C++ program that exits with 3
   * (the exit code of a JVM that ran out of memory), and a C++ program that touches 192 MB of its
   * 256 MB and then crashes (SIGSEGV). Expected result: Runtime Error for all three: none is a JVM
   * that reports running out of memory, and none reached its memory limit.
   */
  @Test
  public void testOtherFailuresAreRuntimeErrors() throws Exception {
    Assumptions.assumeTrue(isLinux(), "The test needs a JDK on the sandbox user's PATH.");
    Assertions.assertEquals(
        "RE",
        judge(900031L, "text/x-java", JAVA_EXCEPTION_CODE, 5000, 262144, "").get("runtimeResult"));
    Assertions.assertEquals("RE", judge(900032L, EXIT_3_CODE, 5000, 262144));
    Assertions.assertEquals("RE", judge(900037L, CRASH_AFTER_ALLOCATION_CODE, 5000, 262144));
  }

  /**
   * Checks whether the judger runs on Linux.
   *
   * @return whether the operating system is Linux
   */
  private static boolean isLinux() {
    return System.getProperty("os.name").contains("Linux");
  }

  /**
   * Creates the work directory of a submission (owned by the sandbox user, as for a real one).
   *
   * @param id - a unique id used for the work directory and the in-memory submission
   * @return the work directory
   * @throws Exception if the directory cannot be created
   */
  private String createWorkDirectory(long id) throws Exception {
    Submission submission = new Submission();
    submission.setSubmissionId(id);
    submission.setLanguage(languageMapper.getLanguageUsingSlug("text/x-c++src"));
    submission.setCode(RUNTIME_ERROR_CODE);
    String workDirectory = workBaseDirectory + "/voj-sandbox-" + id;
    preprocessor.createTestCode(submission, workDirectory, "sandboxtest");
    return workDirectory;
  }

  /**
   * Compiles and runs a C++ program under the active sandbox and returns its verdict slug.
   *
   * @param id - a unique id used for the work directory and the in-memory submission
   * @param code - the C++ source code to compile and run
   * @param timeLimitMs - the time limit (ms)
   * @param memoryLimitKb - the memory limit (KB)
   * @return the verdict slug (AC/TLE/MLE/RE/SE)
   * @throws Exception if the test fixtures cannot be set up
   */
  private String judge(long id, String code, int timeLimitMs, int memoryLimitKb) throws Exception {
    return judge(id, code, timeLimitMs, memoryLimitKb, "");
  }

  /**
   * Compiles and runs a C++ program under the active sandbox and returns its verdict slug.
   *
   * @param id - a unique id used for the work directory and the in-memory submission
   * @param code - the C++ source code to compile and run
   * @param timeLimitMs - the time limit (ms)
   * @param memoryLimitKb - the memory limit (KB)
   * @param input - the program's standard input
   * @return the verdict slug (AC/TLE/MLE/RE/SE)
   * @throws Exception if the test fixtures cannot be set up
   */
  private String judge(long id, String code, int timeLimitMs, int memoryLimitKb, String input)
      throws Exception {
    return (String)
        judge(id, "text/x-c++src", code, timeLimitMs, memoryLimitKb, input).get("runtimeResult");
  }

  /**
   * Compiles and runs a program under the active sandbox, with the time and memory multipliers of
   * its language, and returns the result of the run.
   *
   * @param id - a unique id used for the work directory and the in-memory submission
   * @param languageSlug - the slug of the program's language, seeded in the test database
   * @param code - the source code to compile and run
   * @param timeLimitMs - the time limit of the problem (ms)
   * @param memoryLimitKb - the memory limit of the problem (KB)
   * @param input - the program's standard input
   * @return the result of the run ("runtimeResult": the verdict slug, "usedTime", "usedMemory")
   * @throws Exception if the test fixtures cannot be set up
   */
  private Map<String, Object> judge(
      long id, String languageSlug, String code, int timeLimitMs, int memoryLimitKb, String input)
      throws Exception {
    Language language = languageMapper.getLanguageUsingSlug(languageSlug);
    Assertions.assertNotNull(language, languageSlug + " must be seeded in the test database.");
    return judge(id, language, code, timeLimitMs, memoryLimitKb, input);
  }

  /**
   * Compiles and runs a program of a language under the active sandbox, with the time and memory
   * multipliers of the language, and returns the result of the run.
   *
   * @param id - a unique id used for the work directory and the in-memory submission
   * @param language - the program's language
   * @param code - the source code to compile and run
   * @param timeLimitMs - the time limit of the problem (ms)
   * @param memoryLimitKb - the memory limit of the problem (KB)
   * @param input - the program's standard input
   * @return the result of the run ("runtimeResult": the verdict slug, "usedTime", "usedMemory")
   * @throws Exception if the test fixtures cannot be set up
   */
  private Map<String, Object> judge(
      long id, Language language, String code, int timeLimitMs, int memoryLimitKb, String input)
      throws Exception {
    Problem problem =
        new Problem(id, true, "sandbox-behavior", timeLimitMs, memoryLimitKb, "", "", "", "", "", "");
    Submission submission = new Submission();
    submission.setSubmissionId(id);
    submission.setProblem(problem);
    submission.setLanguage(language);
    submission.setCode(code);

    String workDirectory = workBaseDirectory + "/voj-sandbox-" + id;
    String baseFileName = "sandboxtest";
    preprocessor.createTestCode(submission, workDirectory, baseFileName);

    Map<String, Object> compileResult =
        compiler.getCompileResult(submission, workDirectory, baseFileName);
    Assertions.assertEquals(
        true, compileResult.get("isSuccessful"),
        "The test program must compile: " + compileResult.get("log"));

    Path inputFile = Path.of(workDirectory, "input.txt");
    Files.writeString(inputFile, input);
    String outputFile = workDirectory + "/output.txt";

    return programRunner.getRuntimeResult(
        submission, workDirectory, baseFileName, inputFile.toString(), outputFile);
  }

  /**
   * A Java program that allocates 2 GB in all, in arrays of 1 MB, each dropped at once (no
   * System.gc()), and writes to standard error as well.
   */
  private static final String JAVA_GARBAGE_CODE =
      """
      public class Main {
          public static void main(String[] args) {
              System.err.println("garbage ahead");
              long sum = 0;
              for (int i = 0; i < 2048; ++i) {
                  byte[] block = new byte[1 << 20];
                  for (int j = 0; j < block.length; j += 4096) {
                      block[j] = 1;
                  }
                  sum += block[(i % 256) * 4096];
              }
              System.out.println(sum);
          }
      }
      """;

  /** A Java program that keeps 768 MB of arrays of 1 MB, touching every page. */
  private static final String JAVA_HOARD_CODE =
      """
      import java.util.ArrayList;
      import java.util.List;

      public class Main {
          public static void main(String[] args) {
              List<byte[]> blocks = new ArrayList<>();
              for (int i = 0; i < 768; ++i) {
                  byte[] block = new byte[1 << 20];
                  for (int j = 0; j < block.length; j += 4096) {
                      block[j] = 1;
                  }
                  blocks.add(block);
              }
              System.out.println(blocks.size());
          }
      }
      """;

  /**
   * A Java program that keeps 768 MB of arrays of 1 MB, touching every page, while another thread
   * writes lines to standard output (unbuffered, without allocating) from the time it holds 384 MB
   * until the JVM exits.
   */
  private static final String JAVA_HOARD_WHILE_WRITING_CODE =
      """
      import java.io.FileDescriptor;
      import java.io.FileOutputStream;
      import java.io.IOException;
      import java.util.ArrayList;
      import java.util.List;

      public class Main {
          static volatile boolean halfFull;

          public static void main(String[] args) {
              Thread writer = new Thread(() -> {
                  FileOutputStream out = new FileOutputStream(FileDescriptor.out);
                  byte[] line = "tick\\n".getBytes();
                  try {
                      while (!halfFull) {
                          Thread.sleep(1);
                      }
                      while (true) {
                          out.write(line);
                      }
                  } catch (IOException | InterruptedException ex) {
                      // Nothing more to write.
                  }
              });
              writer.setDaemon(true);
              writer.start();
              List<byte[]> blocks = new ArrayList<>();
              for (int i = 0; i < 768; ++i) {
                  if (i == 384) {
                      halfFull = true;
                  }
                  byte[] block = new byte[1 << 20];
                  for (int j = 0; j < block.length; j += 4096) {
                      block[j] = 1;
                  }
                  blocks.add(block);
              }
              System.out.println(blocks.size());
          }
      }
      """;

  /** A Java program that asks for a single array of 1.6 GB. */
  private static final String JAVA_HUGE_ARRAY_CODE =
      """
      public class Main {
          public static void main(String[] args) {
              long[] values = new long[200_000_000];
              System.out.println(values.length);
          }
      }
      """;

  /**
   * A Java program that runs 100 tasks, each sleeping 20 ms, with CompletableFuture.supplyAsync
   * without an executor, and prints the sum of their results and the common pool's parallelism.
   */
  private static final String JAVA_COMPLETABLE_FUTURE_CODE =
      """
      import java.util.ArrayList;
      import java.util.List;
      import java.util.concurrent.CompletableFuture;
      import java.util.concurrent.ForkJoinPool;

      public class Main {
          public static void main(String[] args) {
              List<CompletableFuture<Integer>> tasks = new ArrayList<>();
              for (int i = 0; i < 100; ++i) {
                  final int value = i;
                  tasks.add(CompletableFuture.supplyAsync(() -> {
                      try {
                          Thread.sleep(20);
                      } catch (InterruptedException ex) {
                          Thread.currentThread().interrupt();
                      }
                      return value;
                  }));
              }
              int sum = 0;
              for (CompletableFuture<Integer> task : tasks) {
                  sum += task.join();
              }
              System.out.println(sum + " " + ForkJoinPool.getCommonPoolParallelism());
          }
      }
      """;

  /** A Java program that sums a parallel stream. */
  private static final String JAVA_PARALLEL_STREAM_CODE =
      """
      import java.util.stream.LongStream;

      public class Main {
          public static void main(String[] args) {
              System.out.println(LongStream.rangeClosed(1, 50_000_000L).parallel().map(x -> x % 7).sum());
          }
      }
      """;

  /** A Java program that dies of an uncaught exception. */
  private static final String JAVA_EXCEPTION_CODE =
      """
      public class Main {
          public static void main(String[] args) {
              throw new IllegalStateException("failed");
          }
      }
      """;

  /** A program that touches 192 MB and then crashes with SIGSEGV. */
  private static final String CRASH_AFTER_ALLOCATION_CODE =
      """
      #include <cstdio>
      #include <cstring>

      int main() {
          char* block = new char[192 * 1024 * 1024];
          std::memset(block, 1, 192 * 1024 * 1024);
          std::printf("%d\\n", block[12345]);
          volatile int* nowhere = nullptr;
          return *nowhere + block[0];
      }
      """;

  /** A program that exits with 3. */
  private static final String EXIT_3_CODE =
      """
      int main() {
          return 3;
      }
      """;

  /** A program that allocates ~200 MB (well above the test memory limit), touching every page. */
  private static final String MEMORY_BOMB_CODE =
      """
      #include <vector>
      #include <cstring>

      int main() {
          std::vector<char*> blocks;
          for (int i = 0; i < 200; ++i) {
              char* block = new char[1024 * 1024];
              std::memset(block, 1, 1024 * 1024);
              blocks.push_back(block);
          }
          return 0;
      }
      """;

  /**
   * A program that allocates (and would zero) 4 GB at once. The element it reads is chosen at run
   * time: an element known to be zero lets an optimising compiler (GCC 16 at -O2) drop the
   * allocation altogether.
   */
  private static final String HUGE_ALLOCATION_CODE =
      """
      #include <cstdio>
      #include <vector>

      int main() {
          std::vector<int> v(1000000000);
          volatile int i = 12345;
          v[i] = 1;
          std::printf("%d\\n", v[i]);
          return 0;
      }
      """;

  /** A program that aborts. */
  private static final String ABORT_CODE =
      """
      #include <cstdlib>

      int main() {
          std::abort();
      }
      """;

  /** A program that loops forever (the volatile counter prevents the loop being optimised away). */
  private static final String INFINITE_LOOP_CODE =
      """
      int main() {
          volatile long counter = 0;
          while (true) {
              counter = counter + 1;
          }
          return 0;
      }
      """;

  /** A program whose four threads burn 1.5 s of CPU time in total, after which it exits with 0. */
  private static final String CPU_THREADS_CODE =
      """
      #include <ctime>
      #include <thread>
      #include <vector>

      int main() {
          std::vector<std::thread> threads;
          for (int i = 0; i < 4; ++i) {
              threads.emplace_back([] {
                  timespec now;
                  do {
                      clock_gettime(CLOCK_PROCESS_CPUTIME_ID, &now);
                  } while (now.tv_sec + now.tv_nsec / 1e9 < 1.5);
              });
          }
          for (std::thread& thread : threads) {
              thread.join();
          }
          return 0;
      }
      """;

  /**
   * A program that writes 512 MB to standard output. Given "ignore" on its input, it ignores
   * SIGXFSZ (where there is one) and exits with 0 as soon as a write fails.
   */
  private static final String OUTPUT_BOMB_CODE =
      """
      #include <csignal>
      #include <cstdio>
      #include <cstring>
      #include <unistd.h>

      static char block[1 << 20];

      int main() {
          char mode[16] = "";
          if (std::scanf("%15s", mode) == 1 && std::strcmp(mode, "ignore") == 0) {
      #ifdef SIGXFSZ
              std::signal(SIGXFSZ, SIG_IGN);
      #endif
          }
          std::memset(block, 'x', sizeof(block));
          for (int i = 0; i < 512; ++i) {
              if (write(STDOUT_FILENO, block, sizeof(block)) < 0) {
                  return 0;
              }
          }
          return 0;
      }
      """;

  /** A program that exits with a non-zero status. */
  private static final String RUNTIME_ERROR_CODE =
      """
      int main() {
          return 1;
      }
      """;

  /**
   * A program that exits with 1, 2 or 3 if it managed to create a user namespace with clone, clone3
   * or unshare, with 4 if a thread fails, and with 0 otherwise.
   */
  private static final String USER_NAMESPACE_CODE =
      """
      #include <cstring>
      #include <thread>
      #include <linux/sched.h>
      #include <sched.h>
      #include <signal.h>
      #include <sys/syscall.h>
      #include <sys/wait.h>
      #include <unistd.h>

      static int tryChild(long pid, int code) {
          if (pid == 0) {
              _exit(0);
          }
          if (pid > 0) {
              waitpid((pid_t) pid, nullptr, 0);
              return code;
          }
          return 0;
      }

      int main() {
          int code = tryChild(syscall(SYS_clone, CLONE_NEWUSER | SIGCHLD, 0, 0, 0, 0), 1);
          if (code != 0) {
              return code;
          }
          struct clone_args args;
          std::memset(&args, 0, sizeof(args));
          args.flags = CLONE_NEWUSER;
          args.exit_signal = SIGCHLD;
          code = tryChild(syscall(SYS_clone3, &args, sizeof(args)), 2);
          if (code != 0) {
              return code;
          }
          if (unshare(CLONE_NEWUSER) == 0) {
              return 3;
          }
          try {
              std::thread thread([] {});
              thread.join();
          } catch (...) {
              return 4;
          }
          return 0;
      }
      """;

  /** A program that reports how its attempts to create a process fail. */
  private static final String FORK_CODE =
      """
      #include <cerrno>
      #include <cstdio>
      #include <spawn.h>
      #include <sys/syscall.h>
      #include <sys/wait.h>
      #include <unistd.h>

      extern char** environ;

      static void report(const char* how, long pid, int error) {
          if (pid == 0) {
              _exit(0);
          }
          if (pid > 0) {
              waitpid((pid_t) pid, nullptr, 0);
          }
          std::printf("%s: %s\\n", how, pid > 0 ? "created"
              : error == EPERM ? "EPERM" : error == ENOSYS ? "ENOSYS" : "error");
      }

      int main() {
          long pid = fork();
          report("fork", pid, errno);
      #ifdef SYS_fork
          pid = syscall(SYS_fork);
      #else
          pid = -1;
          errno = ENOSYS;
      #endif
          report("SYS_fork", pid, errno);
          char* argv[] = {(char*) "/bin/true", nullptr};
          pid_t spawned = -1;
          int error = posix_spawn(&spawned, "/bin/true", nullptr, nullptr, argv, environ);
          report("posix_spawn", error == 0 ? spawned : -1, error);
          return 0;
      }
      """;

  /** A program that sums the work of eight threads. */
  private static final String THREADS_CODE =
      """
      #include <atomic>
      #include <cstdio>
      #include <thread>
      #include <vector>

      int main() {
          std::atomic<int> done(0);
          std::vector<std::thread> threads;
          for (int i = 0; i < 8; ++i) {
              threads.emplace_back([&done] {
                  volatile long sum = 0;
                  for (long j = 0; j < 10000000; ++j) {
                      sum = sum + j;
                  }
                  ++done;
              });
          }
          for (std::thread& thread : threads) {
              thread.join();
          }
          std::printf("%d\\n", done.load());
          return 0;
      }
      """;

  /** A program whose eight processes share 48 MB of memory (see testMemorySharedAfterFork...). */
  private static final String SHARED_MEMORY_CODE =
      """
      #include <cstdlib>
      #include <cstring>
      #include <sys/wait.h>
      #include <unistd.h>

      int main() {
          const size_t size = 48 * 1024 * 1024;
          char* block = static_cast<char*>(std::malloc(size));
          std::memset(block, 1, size);
          for (int i = 0; i < 7; ++i) {
              if (fork() == 0) {
                  usleep(300 * 1000);
                  _exit(block[i] == 1 ? 0 : 1);
              }
          }
          while (wait(nullptr) > 0) {
          }
          return 0;
      }
      """;

  /** A program whose orphaned grandchildren burn 3 x 600 ms of CPU time, one after the other. */
  private static final String ORPHAN_CPU_CODE =
      """
      #include <ctime>
      #include <sys/wait.h>
      #include <unistd.h>

      static void burn(double seconds) {
          timespec now;
          do {
              clock_gettime(CLOCK_PROCESS_CPUTIME_ID, &now);
          } while (now.tv_sec + now.tv_nsec / 1e9 < seconds);
      }

      int main() {
          for (int i = 0; i < 3; ++i) {
              int fds[2];
              if (pipe(fds) != 0) {
                  return 1;
              }
              pid_t child = fork();
              if (child == 0) {
                  if (fork() == 0) {
                      close(fds[0]);
                      burn(0.6);
                      _exit(0);
                  }
                  _exit(0); // orphans the grandchild
              }
              close(fds[1]);
              waitpid(child, nullptr, 0);
              char c;
              while (read(fds[0], &c, 1) > 0) {
              }
              close(fds[0]); // end-of-file: the grandchild has exited
          }
          return 0;
      }
      """;

  /**
   * A program that exits with 1 or 2 if it connected to 127.0.0.1:3306 (the judger's database in
   * the test setup) or to 1.1.1.1:80, and with 5 if an AF_UNIX socket pair fails. Given "strict" on
   * its input, it also exits with 3 if a socket of another address family is created, with 4 if the
   * refusal is not the seccomp filter's EACCES, or with 6 if an AF_INET socket can be created
   * through the i386 system call entry point (x86-64 only). Otherwise it exits with 0.
   */
  private static final String NETWORK_CODE =
      """
      #include <arpa/inet.h>
      #include <atomic>
      #include <cerrno>
      #include <cstdio>
      #include <cstring>
      #include <netinet/in.h>
      #include <poll.h>
      #include <sys/socket.h>
      #include <thread>
      #include <unistd.h>

      static bool connects(const char* address, int port) {
          int fd = socket(AF_INET, SOCK_STREAM | SOCK_NONBLOCK, 0);
          if (fd < 0) {
              return false;
          }
          sockaddr_in peer;
          std::memset(&peer, 0, sizeof(peer));
          peer.sin_family = AF_INET;
          peer.sin_port = htons(port);
          inet_pton(AF_INET, address, &peer.sin_addr);
          bool connected = connect(fd, (sockaddr*) &peer, sizeof(peer)) == 0;
          if (!connected && errno == EINPROGRESS) {
              pollfd event = {fd, POLLOUT, 0};
              int error = -1;
              socklen_t length = sizeof(error);
              connected = poll(&event, 1, 2000) == 1
                  && getsockopt(fd, SOL_SOCKET, SO_ERROR, &error, &length) == 0 && error == 0;
          }
          close(fd);
          return connected;
      }

      int main() {
          char mode[16] = "";
          bool strict = std::scanf("%15s", mode) == 1 && std::strcmp(mode, "strict") == 0;
          if (connects("127.0.0.1", 3306)) {
              return 1;
          }
          if (connects("1.1.1.1", 80)) {
              return 2;
          }
          const int families[] = {AF_INET, AF_INET6, AF_NETLINK, AF_PACKET, AF_VSOCK};
          for (int family : families) {
              int fd = socket(family, SOCK_DGRAM, 0);
              if (fd >= 0) {
                  close(fd);
                  if (strict) {
                      return 3;
                  }
              } else if (strict && errno != EACCES) {
                  return 4;
              }
              int pair[2];
              if (socketpair(family, SOCK_STREAM, 0, pair) == 0) {
                  return 3;
              }
              if (strict && errno != EACCES) {
                  return 4;
              }
          }
          int pair[2];
          if (socketpair(AF_UNIX, SOCK_STREAM, 0, pair) != 0) {
              return 5;
          }
      #if defined(__x86_64__)
          // The i386 entry point (socketcall) must not get around the filter either. It is tried in
          // a thread of its own, which the filter may kill (a run cannot create processes).
          static std::atomic<long> i386Socket(-1);
          std::thread([] {
              static unsigned int args[3] = {AF_INET, SOCK_STREAM, 0};
              long fd;
              asm volatile("int $0x80" : "=a"(fd) : "a"(102), "b"(1), "c"(args) : "memory");
              i386Socket = fd;
          }).join();
          if (strict && i386Socket >= 0) {
              return 6;
          }
      #endif
          return 0;
      }
      """;

  /**
   * Given a NAME on its input, a Windows program that reports its integrity level, tries to set
   * the variable NAME of its account (HKCU\Environment) and to create the file NAME in its
   * account's profile directory and in C:\Users\Public (removing what it managed to write), creates
   * the file NAME in its work directory and home.txt in its USERPROFILE, and reports how each
   * attempt ends.
   */
  private static final String WINDOWS_LEAVE_BEHIND_CODE =
      """
      #include <windows.h>
      #include <cstdio>
      #include <string>

      typedef BOOL (WINAPI *GetUserProfileDirectoryFunction)(HANDLE, LPSTR, LPDWORD);

      static const char* create(const std::string& path, bool keep) {
          HANDLE file = CreateFileA(path.c_str(), GENERIC_WRITE, 0, NULL, CREATE_NEW, 0, NULL);
          if (file == INVALID_HANDLE_VALUE) {
              return GetLastError() == ERROR_ACCESS_DENIED ? "denied" : "failed";
          }
          CloseHandle(file);
          if (!keep) {
              DeleteFileA(path.c_str());
          }
          return keep ? "created" : "written";
      }

      int main() {
          char name[64];
          if (std::scanf("%63s", name) != 1) {
              return 1;
          }
          HANDLE token;
          OpenProcessToken(GetCurrentProcess(), TOKEN_QUERY, &token);
          char label[64];
          DWORD size;
          GetTokenInformation(token, TokenIntegrityLevel, label, sizeof(label), &size);
          PSID sid = ((TOKEN_MANDATORY_LABEL*) label)->Label.Sid;
          DWORD level = *GetSidSubAuthority(sid, *GetSidSubAuthorityCount(sid) - 1);
          std::printf("integrity: %s\\n", level == SECURITY_MANDATORY_LOW_RID ? "low" : "not low");

          HKEY key;
          LONG status = RegOpenKeyExA(HKEY_CURRENT_USER, "Environment", 0, KEY_SET_VALUE, &key);
          if (status == ERROR_SUCCESS) {
              status = RegSetValueExA(key, name, 0, REG_SZ, (const BYTE*) "1", 2);
              if (status == ERROR_SUCCESS) {
                  RegDeleteValueA(key, name);
              }
              RegCloseKey(key);
          }
          std::printf("HKCU\\\\Environment: %s\\n", status == ERROR_SUCCESS ? "written"
                      : status == ERROR_ACCESS_DENIED ? "denied" : "failed");

          char profile[MAX_PATH] = "";
          DWORD length = MAX_PATH;
          GetUserProfileDirectoryFunction getUserProfileDirectory =
              (GetUserProfileDirectoryFunction) (void*) GetProcAddress(
                  LoadLibraryA("userenv.dll"), "GetUserProfileDirectoryA");
          if (!getUserProfileDirectory(token, profile, &length)) {
              return 2;
          }
          std::printf("profile: %s\\n", create(std::string(profile) + "\\\\" + name, false));
          char publicDirectory[MAX_PATH];
          if (!GetEnvironmentVariableA("PUBLIC", publicDirectory, MAX_PATH)) {
              return 3;
          }
          std::printf("public: %s\\n", create(std::string(publicDirectory) + "\\\\" + name, false));
          std::printf("work: %s\\n", create(name, true));
          char home[MAX_PATH];
          if (!GetEnvironmentVariableA("USERPROFILE", home, MAX_PATH)) {
              return 4;
          }
          std::printf("USERPROFILE: %s\\n", create(std::string(home) + "\\\\home.txt", true));
          return 0;
      }
      """;

  /**
   * A Windows program that creates the directory "locked" with a file in it, then gives both a
   * protected ACL that allows nobody anything (not inherited from the work directory), and prints
   * "locked" once it did.
   */
  private static final String WINDOWS_DENY_JUDGER_CODE =
      """
      #include <windows.h>
      #include <sddl.h>
      #include <cstdio>

      int main() {
          PSECURITY_DESCRIPTOR nobody;
          if (!ConvertStringSecurityDescriptorToSecurityDescriptorA(
                  "D:P", SDDL_REVISION_1, &nobody, NULL)) {
              return 1;
          }
          if (!CreateDirectoryA("locked", NULL)) {
              return 2;
          }
          HANDLE file =
              CreateFileA("locked\\\\file.txt", GENERIC_WRITE, 0, NULL, CREATE_NEW, 0, NULL);
          if (file == INVALID_HANDLE_VALUE) {
              return 3;
          }
          CloseHandle(file);
          if (!SetFileSecurityA("locked\\\\file.txt", DACL_SECURITY_INFORMATION, nobody) ||
                  !SetFileSecurityA("locked", DACL_SECURITY_INFORMATION, nobody)) {
              return 4;
          }
          std::printf("locked\\n");
          return 0;
      }
      """;

  /**
   * Given "leave NAME" on its input, a program that tries to create the file NAME in /tmp, /var/tmp,
   * /dev/shm and its work directory, to write to /dev/null, and to create System V IPC objects, a
   * POSIX message queue, a key in the user keyring (through the raw system calls: libkeyutils need
   * not be installed) and a memory file, and reports how each attempt ends. Given "find NAME", it
   * reports whether NAME exists in /tmp, /var/tmp and /dev/shm.
   */
  private static final String LEAVE_BEHIND_CODE =
      """
      #include <cerrno>
      #include <cstdio>
      #include <cstring>
      #include <fcntl.h>
      #include <mqueue.h>
      #include <sys/mman.h>
      #include <sys/msg.h>
      #include <sys/sem.h>
      #include <sys/shm.h>
      #include <sys/syscall.h>
      #include <unistd.h>

      static const long KEY_SPEC_USER_KEYRING = -4;
      static const long KEYCTL_GET_KEYRING_ID = 0;

      static void report(const char* what, bool succeeded, const char* success) {
          int error = errno;
          std::printf("%s: %s\\n", what, succeeded ? success
              : error == EACCES ? "EACCES" : error == EPERM ? "EPERM"
              : error == ENOSYS ? "ENOSYS" : std::strerror(error));
      }

      int main() {
          char mode[16] = "", name[64] = "";
          if (std::scanf("%15s %63s", mode, name) != 2) {
              return 1;
          }
          const char* directories[] = {"/tmp", "/var/tmp", "/dev/shm"};
          char path[128];
          for (const char* directory : directories) {
              std::snprintf(path, sizeof(path), "%s/%s", directory, name);
              if (std::strcmp(mode, "find") == 0) {
                  std::printf("%s: %s\\n", directory, access(path, F_OK) == 0 ? "present" : "absent");
              } else {
                  int fd = open(path, O_WRONLY | O_CREAT | O_EXCL, 0644);
                  report(directory, fd >= 0, "created");
              }
          }
          if (std::strcmp(mode, "find") == 0) {
              return 0;
          }
          report("work", open(name, O_WRONLY | O_CREAT | O_EXCL, 0644) >= 0, "created");
          report("/dev/null", open("/dev/null", O_WRONLY | O_TRUNC) >= 0, "written");
          report("shmget", shmget(IPC_PRIVATE, 1 << 20, IPC_CREAT | 0600) >= 0, "created");
          report("semget", semget(IPC_PRIVATE, 1, IPC_CREAT | 0600) >= 0, "created");
          report("msgget", msgget(IPC_PRIVATE, IPC_CREAT | 0600) >= 0, "created");
          report("mq_open", mq_open("/voj-sandbox-leak", O_RDWR | O_CREAT, 0600, nullptr) != (mqd_t) -1,
              "created");
          report("add_key", syscall(SYS_add_key, "user", "voj-sandbox-leak", "secret", 6L,
              KEY_SPEC_USER_KEYRING) >= 0, "created");
          report("request_key", syscall(SYS_request_key, "user", "voj-sandbox-leak", nullptr,
              0L) >= 0, "found");
          report("keyctl",
              syscall(SYS_keyctl, KEYCTL_GET_KEYRING_ID, KEY_SPEC_USER_KEYRING, 1L) >= 0, "created");
          report("memfd_create", memfd_create("voj-sandbox-leak", 0) >= 0, "created");
          return 0;
      }
      """;

  /**
   * Given the path of a file, a program that tries to empty it with truncate(2) and with
   * open(O_RDONLY | O_TRUNC), and reports how each attempt ends.
   */
  private static final String TRUNCATE_CODE =
      """
      #include <cerrno>
      #include <cstdio>
      #include <cstring>
      #include <fcntl.h>
      #include <unistd.h>

      static void report(const char* what, bool succeeded) {
          int error = errno;
          std::printf("%s: %s\\n", what, succeeded ? "truncated"
              : error == EACCES ? "EACCES" : error == EPERM ? "EPERM" : std::strerror(error));
      }

      int main() {
          char path[4096] = "";
          if (std::scanf("%4095s", path) != 1) {
              return 1;
          }
          report("truncate", truncate(path, 0) == 0);
          report("open", open(path, O_RDONLY | O_TRUNC) >= 0);
          return 0;
      }
      """;

  /**
   * A program that tries to page out its memory and to create shared anonymous memory, and reports
   * how each attempt ends.
   */
  private static final String PAGE_OUT_CODE =
      """
      #include <cerrno>
      #include <cstdio>
      #include <cstring>
      #include <sys/mman.h>
      #include <sys/syscall.h>
      #include <sys/uio.h>
      #include <unistd.h>

      static void report(const char* what, bool succeeded) {
          int error = errno;
          std::printf("%s: %s\\n", what, succeeded ? "done"
              : error == EPERM ? "EPERM" : error == ENOSYS ? "ENOSYS" : std::strerror(error));
      }

      int main() {
          const size_t length = 1 << 20;
          char* memory = static_cast<char*>(mmap(nullptr, length, PROT_READ | PROT_WRITE,
              MAP_PRIVATE | MAP_ANONYMOUS, -1, 0));
          if (memory == MAP_FAILED) {
              return 1;
          }
          std::memset(memory, 1, length);
          report("MADV_PAGEOUT", madvise(memory, length, MADV_PAGEOUT) == 0);
          report("MADV_PAGEOUT (upper bits set)",
              syscall(SYS_madvise, memory, length, (1L << 32) | MADV_PAGEOUT) == 0);
          report("MADV_COLD", madvise(memory, length, MADV_COLD) == 0);
          int self = (int) syscall(SYS_pidfd_open, getpid(), 0);
          struct iovec range = {memory, length};
          report("process_madvise",
              syscall(SYS_process_madvise, self, &range, 1, MADV_PAGEOUT, 0) >= 0);
          report("MAP_SHARED | MAP_ANONYMOUS", mmap(nullptr, length, PROT_READ | PROT_WRITE,
              MAP_SHARED | MAP_ANONYMOUS, -1, 0) != MAP_FAILED);
          report("MADV_DONTNEED", madvise(memory, length, MADV_DONTNEED) == 0);
          report("MADV_FREE", madvise(memory, length, MADV_FREE) == 0);
          return 0;
      }
      """;

  /**
   * A program that makes system calls which are not on the native sandbox's allowlist, and reports
   * how each ends (it exits with 1 if a call that is allowed fails).
   */
  private static final String UNLISTED_SYSCALLS_CODE =
      """
      #include <cerrno>
      #include <cstdio>
      #include <cstring>
      #include <fcntl.h>
      #include <sys/inotify.h>
      #include <sys/personality.h>
      #include <sys/socket.h>
      #include <sys/stat.h>
      #include <sys/un.h>
      #include <unistd.h>

      static void report(const char* what, bool succeeded) {
          int error = errno;
          std::printf("%s: %s\\n", what, succeeded ? "done"
              : error == ENOSYS ? "ENOSYS" : std::strerror(error));
      }

      int main() {
          report("personality", personality(0xffffffff) != -1);
          report("inotify_init1", inotify_init1(IN_CLOEXEC) >= 0);
          int file = open("unlisted.txt", O_WRONLY | O_CREAT | O_EXCL, 0644);
          if (file < 0) {
              return 1;
          }
          close(file);
          report("symlink", symlink("unlisted.txt", "unlisted.lnk") == 0);
          report("chmod", chmod("unlisted.txt", 0666) == 0);
          int fd = socket(AF_UNIX, SOCK_STREAM, 0);
          if (fd < 0) {
              return 1;
          }
          sockaddr_un address;
          std::memset(&address, 0, sizeof(address));
          address.sun_family = AF_UNIX;
          std::strcpy(address.sun_path, "/voj-no-such-socket");
          report("connect", connect(fd, (sockaddr*) &address, sizeof(address)) == 0);
          return 0;
      }
      """;

  /**
   * A program that reports its descriptor limit, how raising it, creating a thread with and
   * without CLONE_FILES and opening descriptors until it cannot end (it exits with 1 if setting up
   * an attempt fails).
   */
  private static final String DESCRIPTOR_LIMIT_CODE =
      """
      #include <cerrno>
      #include <cstdio>
      #include <cstring>
      #include <fcntl.h>
      #include <sched.h>
      #include <signal.h>
      #include <sys/resource.h>
      #include <sys/syscall.h>
      #include <unistd.h>

      static const char* name(int error) {
          return error == EPERM ? "EPERM" : error == EMFILE ? "EMFILE"
              : error == ENOSYS ? "ENOSYS" : std::strerror(error);
      }

      static volatile int threadRan = 0;
      static int threadMain(void*) {
          threadRan = 1;
          return 0;
      }

      static void tryThread(const char* what, int flags) {
          static char stacks[2][64 * 1024];
          static int used = 0;
          char* stack = stacks[used++] + sizeof(stacks[0]);
          int tid = clone(threadMain, stack, flags, nullptr);
          if (tid < 0) {
              std::printf("%s: %s\\n", what, name(errno));
              return;
          }
          while (!threadRan) {
              sched_yield();
          }
          threadRan = 0;
          std::printf("%s: created\\n", what);
      }

      int main() {
          rlimit limit;
          if (getrlimit(RLIMIT_NOFILE, &limit) != 0) {
              return 1;
          }
          std::printf("limit: %llu %llu\\n", (unsigned long long) limit.rlim_cur,
              (unsigned long long) limit.rlim_max);
          rlimit raised = {limit.rlim_max + 1, limit.rlim_max + 1};
          std::printf("raise: %s\\n", setrlimit(RLIMIT_NOFILE, &raised) == 0 ? "raised" : name(errno));
          const int threadFlags = CLONE_VM | CLONE_FS | CLONE_SIGHAND | CLONE_THREAD | CLONE_SYSVSEM;
          tryThread("thread without CLONE_FILES", threadFlags);
          tryThread("thread with CLONE_FILES", threadFlags | CLONE_FILES);
          std::fflush(stdout);
          int last = -1;
          for (;;) {
              int fd = open("/dev/null", O_RDONLY);
              if (fd < 0) {
                  std::printf("open: %s at %d\\n", name(errno), last + 1);
                  break;
              }
              last = fd;
          }
          return 0;
      }
      """;

  /**
   * A program that reads and lowers its own core file size limit and its parent's with the raw
   * prlimit64 system call, and reports how each attempt ends.
   */
  private static final String PRLIMIT_CODE =
      """
      #include <cerrno>
      #include <cstdio>
      #include <cstring>
      #include <sys/resource.h>
      #include <sys/syscall.h>
      #include <unistd.h>

      static void report(const char* what, bool succeeded) {
          int error = errno;
          std::printf("%s: %s\\n", what, succeeded ? "done"
              : error == EPERM ? "EPERM" : error == ENOSYS ? "ENOSYS" : std::strerror(error));
      }

      int main() {
          rlimit limit, lowered = {0, 0};
          report("own (read)", syscall(SYS_prlimit64, 0, RLIMIT_CORE, nullptr, &limit) == 0);
          report("own (lower)", syscall(SYS_prlimit64, 0, RLIMIT_CORE, &lowered, nullptr) == 0);
          long parent = getppid();
          report("parent (read)", syscall(SYS_prlimit64, parent, RLIMIT_CORE, nullptr, &limit) == 0);
          report("parent (lower)", syscall(SYS_prlimit64, parent, RLIMIT_CORE, &lowered, nullptr) == 0);
          return 0;
      }
      """;

  /**
   * A program that reports whether it leads its session and process group, and how opening its
   * controlling terminal and leaving its session end.
   */
  private static final String TERMINAL_CODE =
      """
      #include <cerrno>
      #include <cstdio>
      #include <cstring>
      #include <fcntl.h>
      #include <unistd.h>

      static void report(const char* what, bool succeeded) {
          int error = errno;
          std::printf("%s: %s\\n", what, succeeded ? "done"
              : error == ENXIO ? "ENXIO" : error == EPERM ? "EPERM" : std::strerror(error));
      }

      int main() {
          std::printf("session leader: %s\\n", getsid(0) == getpid() ? "yes" : "no");
          std::printf("process group leader: %s\\n", getpgrp() == getpid() ? "yes" : "no");
          report("/dev/tty", open("/dev/tty", O_RDONLY) >= 0);
          report("setsid", setsid() >= 0);
          return 0;
      }
      """;

  /**
   * A program that sends on a socket pair and to the system log's socket, and reports how each
   * attempt ends (it exits with 1 if the socket pair cannot be created).
   */
  private static final String SEND_CODE =
      """
      #include <cerrno>
      #include <cstdio>
      #include <cstring>
      #include <sys/socket.h>
      #include <sys/un.h>
      #include <unistd.h>

      static void report(const char* what, bool succeeded) {
          int error = errno;
          std::printf("%s: %s\\n", what, succeeded ? "done"
              : error == EPERM ? "EPERM" : error == ENOSYS ? "ENOSYS" : std::strerror(error));
      }

      int main() {
          int pair[2];
          if (socketpair(AF_UNIX, SOCK_DGRAM, 0, pair) != 0) {
              return 1;
          }
          report("send", send(pair[0], "a", 1, 0) == 1);
          report("sendto (no address)", sendto(pair[0], "b", 1, 0, nullptr, 0) == 1);
          report("write", write(pair[0], "c", 1) == 1);
          char buffer[4];
          int received = 0;
          while (recv(pair[1], buffer, sizeof(buffer), MSG_DONTWAIT) == 1) {
              ++received;
          }
          std::printf("received: %d\\n", received);
          int fd = socket(AF_UNIX, SOCK_DGRAM, 0);
          if (fd < 0) {
              return 1;
          }
          sockaddr_un log;
          std::memset(&log, 0, sizeof(log));
          log.sun_family = AF_UNIX;
          std::strcpy(log.sun_path, "/dev/log");
          report("sendto /dev/log",
              sendto(fd, "<14>voj", 7, 0, (sockaddr*) &log, sizeof(log)) == 7);
          char text[] = "<14>voj";
          iovec data = {text, 7};
          msghdr message;
          std::memset(&message, 0, sizeof(message));
          message.msg_name = &log;
          message.msg_namelen = sizeof(log);
          message.msg_iov = &data;
          message.msg_iovlen = 1;
          report("sendmsg /dev/log", sendmsg(fd, &message, 0) == 7);
          return 0;
      }
      """;

  /**
   * A program that creates threads with an explicit scheduling policy, and changes its parent's,
   * and reports how each attempt ends.
   */
  private static final String EXPLICIT_SCHED_CODE =
      """
      #include <cerrno>
      #include <cstdio>
      #include <cstring>
      #include <pthread.h>
      #include <sched.h>
      #include <unistd.h>

      static const char* name(int error) {
          return error == EPERM ? "EPERM" : error == ENOSYS ? "ENOSYS" : std::strerror(error);
      }

      static void* threadMain(void*) {
          return nullptr;
      }

      static void tryThread(const char* what, int policy) {
          pthread_attr_t attributes;
          pthread_attr_init(&attributes);
          pthread_attr_setinheritsched(&attributes, PTHREAD_EXPLICIT_SCHED);
          pthread_attr_setschedpolicy(&attributes, policy);
          sched_param parameters;
          parameters.sched_priority = sched_get_priority_min(policy);
          pthread_attr_setschedparam(&attributes, &parameters);
          pthread_t thread;
          int error = pthread_create(&thread, &attributes, threadMain, nullptr);
          if (error == 0) {
              pthread_join(thread, nullptr);
          }
          std::printf("%s: %s\\n", what, error == 0 ? "created" : name(error));
      }

      int main() {
          tryThread("explicit SCHED_OTHER", SCHED_OTHER);
          tryThread("explicit SCHED_FIFO", SCHED_FIFO);
          sched_param parameters;
          parameters.sched_priority = 0;
          std::printf("parent SCHED_IDLE: %s\\n",
              sched_setscheduler(getppid(), SCHED_IDLE, &parameters) == 0 ? "changed" : name(errno));
          return 0;
      }
      """;

  /**
   * A program that reads and tries to enlarge the kernel buffers of a pipe and of a socket pair,
   * and sets another socket option, and reports how each attempt ends.
   */
  private static final String KERNEL_BUFFER_CODE =
      """
      #include <cerrno>
      #include <cstdio>
      #include <cstring>
      #include <fcntl.h>
      #include <sys/socket.h>
      #include <unistd.h>

      static void report(const char* what, bool succeeded) {
          int error = errno;
          std::printf("%s: %s\\n", what, succeeded ? "done"
              : error == EPERM ? "EPERM" : error == ENOSYS ? "ENOSYS" : std::strerror(error));
      }

      int main() {
          int pipeFds[2];
          int pair[2];
          if (pipe(pipeFds) != 0 || socketpair(AF_UNIX, SOCK_STREAM, 0, pair) != 0) {
              return 1;
          }
          report("F_GETPIPE_SZ", fcntl(pipeFds[0], F_GETPIPE_SZ) > 0);
          report("F_SETPIPE_SZ", fcntl(pipeFds[0], F_SETPIPE_SZ, 1 << 20) >= 0);
          int size = 0;
          socklen_t length = sizeof(size);
          report("getsockopt SO_SNDBUF",
              getsockopt(pair[0], SOL_SOCKET, SO_SNDBUF, &size, &length) == 0 && size > 0);
          const int options[] = {SO_SNDBUF, SO_RCVBUF, SO_SNDBUFFORCE, SO_RCVBUFFORCE};
          const char* names[] = {"SO_SNDBUF", "SO_RCVBUF", "SO_SNDBUFFORCE", "SO_RCVBUFFORCE"};
          for (int i = 0; i < 4; ++i) {
              size = 1 << 24;
              report(names[i], setsockopt(pair[0], SOL_SOCKET, options[i], &size, sizeof(size)) == 0);
          }
          int on = 1;
          report("SO_PASSCRED", setsockopt(pair[0], SOL_SOCKET, SO_PASSCRED, &on, sizeof(on)) == 0);
          return 0;
      }
      """;

  /**
   * A program that reads and tries to raise its RLIMIT_SIGPENDING, then creates POSIX timers and
   * queues a blocked real-time signal to itself until it cannot, and reports how each ends.
   */
  private static final String SIGNAL_QUEUE_CODE =
      """
      #include <cerrno>
      #include <csignal>
      #include <cstdio>
      #include <cstring>
      #include <ctime>
      #include <sys/resource.h>
      #include <unistd.h>

      static const char* name(int error) {
          return error == EPERM ? "EPERM" : error == EAGAIN ? "EAGAIN" : std::strerror(error);
      }

      static timer_t timers[1 << 20];

      int main() {
          rlimit limit;
          if (getrlimit(RLIMIT_SIGPENDING, &limit) != 0) {
              return 1;
          }
          std::printf("limit: %llu %llu\\n", (unsigned long long) limit.rlim_cur,
              (unsigned long long) limit.rlim_max);
          rlimit raised = {limit.rlim_max + 1, limit.rlim_max + 1};
          std::printf("raise: %s\\n", setrlimit(RLIMIT_SIGPENDING, &raised) == 0 ? "raised" : name(errno));
          sigevent event;
          std::memset(&event, 0, sizeof(event));
          event.sigev_notify = SIGEV_NONE;
          int created = 0;
          while (created < (1 << 20) && timer_create(CLOCK_MONOTONIC, &event, &timers[created]) == 0) {
              ++created;
          }
          int error = errno;
          std::printf("timer_create: %s after at most 1024: %s\\n", name(error),
              created <= 1024 ? "yes" : "no");
          for (int i = 0; i < created; ++i) {
              timer_delete(timers[i]);
          }
          sigset_t blocked;
          sigemptyset(&blocked);
          sigaddset(&blocked, SIGRTMIN);
          sigprocmask(SIG_BLOCK, &blocked, nullptr);
          int queued = 0;
          union sigval value;
          value.sival_int = 0;
          while (queued < (1 << 20) && sigqueue(getpid(), SIGRTMIN, value) == 0) {
              ++queued;
          }
          error = errno;
          std::printf("sigqueue: %s after at most 1024: %s\\n", name(error),
              queued <= 1024 ? "yes" : "no");
          return 0;
      }
      """;

  /** A program with 1 TB of static data (bss), of which it touches a single byte. */
  private static final String HUGE_STATIC_DATA_CODE =
      """
      static char data[1ULL << 40];

      int main(int argc, char**) {
          data[argc] = 1;
          return data[argc * 2];
      }
      """;

  /** A fork bomb. */
  private static final String FORK_BOMB_CODE =
      """
      #include <unistd.h>

      int main() {
          while (true) {
              fork();
          }
          return 0;
      }
      """;

  /** The active sandbox runner, exercised directly by the tests of the runner itself. */
  @Autowired private SandboxRunner sandboxRunner;

  /** The ProgramRunner object under test. */
  @Autowired private ProgramRunner programRunner;

  /** The autowired Compiler object, used to compile the test programs. */
  @Autowired private Compiler compiler;

  /** The autowired Preprocessor object, used to write the test source to disk. */
  @Autowired private Preprocessor preprocessor;

  /** The autowired LanguageMapper object, used to fetch the C++ language definition. */
  @Autowired private LanguageMapper languageMapper;

  /** The active sandbox backend ({@code native} or {@code isolate}). */
  @Value("${judger.sandbox:native}")
  private String sandbox;

  /** Where the test judges: the compilation results and the program output go there. */
  @Value("${test.workDir}")
  private String workBaseDirectory;
}
