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
    Assertions.assertEquals(
        "8\n", Files.readString(Path.of(workBaseDirectory, "voj-sandbox-900012", "output.txt")));
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
    Language cpp = languageMapper.getLanguageUsingSlug("text/x-c++src");
    Assertions.assertNotNull(cpp, "The C++ language must be seeded in the test database.");

    Problem problem =
        new Problem(id, true, "sandbox-behavior", timeLimitMs, memoryLimitKb, "", "", "", "", "", "");
    Submission submission = new Submission();
    submission.setSubmissionId(id);
    submission.setProblem(problem);
    submission.setLanguage(cpp);
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

    Map<String, Object> result =
        programRunner.getRuntimeResult(
            submission, workDirectory, baseFileName, inputFile.toString(), outputFile);
    return (String) result.get("runtimeResult");
  }

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

  /** A program that allocates (and would zero) 4 GB at once. */
  private static final String HUGE_ALLOCATION_CODE =
      """
      #include <cstdio>
      #include <vector>

      int main() {
          std::vector<int> v(1000000000);
          std::printf("%d\\n", v[12345]);
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

  /** The working directory of the judger, used to store compilation results and program output. */
  @Value("${judger.workDir}")
  private String workBaseDirectory;
}
