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
   * process: every attempt fails with EPERM, which the program reports (see FORK_CODE).
   */
  @Test
  public void testRunCannotCreateProcesses() throws Exception {
    Assumptions.assumeTrue("native".equalsIgnoreCase(sandbox), "This tests the native runner.");
    Assumptions.assumeTrue(isLinux(), "The test program uses fork() and posix_spawn().");
    Assertions.assertEquals("AC", judge(900011L, FORK_CODE, 5000, 262144));
    Assertions.assertEquals(
        "fork: EPERM\nSYS_fork: EPERM\nposix_spawn: EPERM\n",
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
          std::printf("%s: %s\\n", how, pid > 0 ? "created" : error == EPERM ? "EPERM" : "error");
      }

      int main() {
          long pid = fork();
          report("fork", pid, errno);
      #ifdef SYS_fork
          pid = syscall(SYS_fork);
      #else
          pid = -1;
          errno = EPERM;
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
