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
   * Test case: a program that loops forever. Expected result: Time Limit Exceeded.
   */
  @Test
  public void testTimeLimitExceeded() throws Exception {
    // No memory limit: this case is purely about the time limit, so it must not depend on the
    // sandbox's memory measurement.
    Assertions.assertEquals("TLE", judge(900002L, INFINITE_LOOP_CODE, 1000, 0));
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
   * then checks that threads and fork still work. Expected result: Accepted (every attempt is
   * refused, see USER_NAMESPACE_CODE).
   */
  @Test
  public void testUserNamespacesAreDenied() throws Exception {
    Assumptions.assumeTrue(
        System.getProperty("os.name").contains("Linux"), "User namespaces are Linux-only.");
    Assertions.assertEquals("AC", judge(900005L, USER_NAMESPACE_CODE, 5000, 262144));
  }

  /**
   * Test case: a program that touches 48 MB and then forks seven children that share those pages
   * until they exit. Expected result: Accepted under a 128 MB limit: the group's memory is the
   * proportional set size, which counts each shared page once rather than once per process (as
   * the sum of the resident set sizes, 8 x 48 MB, would).
   */
  @Test
  public void testMemorySharedAfterForkIsCountedOnce() throws Exception {
    Assumptions.assumeFalse(
        System.getProperty("os.name").contains("Windows"), "This test uses fork().");
    Assertions.assertEquals("AC", judge(900006L, SHARED_MEMORY_CODE, 5000, 131072));
  }

  /**
   * Test case: a program that has three orphaned grandchildren, one after the other, each burn 600
   * ms of CPU time and exit, while it itself stays idle. Expected result: Time Limit Exceeded under
   * a 1000 ms limit: the CPU time of a process that exits after being orphaned still counts, even
   * though none of the program's processes ever waits for it.
   */
  @Test
  public void testCpuTimeOfOrphansIsCounted() throws Exception {
    Assumptions.assumeFalse(
        System.getProperty("os.name").contains("Windows"), "This test uses fork().");
    Assertions.assertEquals("TLE", judge(900007L, ORPHAN_CPU_CODE, 1000, 262144));
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
    Files.write(inputFile, new byte[0]);
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

  /** A program that exits with a non-zero status. */
  private static final String RUNTIME_ERROR_CODE =
      """
      int main() {
          return 1;
      }
      """;

  /**
   * A program that exits with 1, 2 or 3 if it managed to create a user namespace with clone, clone3
   * or unshare, with 4 if fork or a thread fails, and with 0 otherwise.
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
          pid_t child = fork();
          if (child < 0) {
              return 4;
          }
          if (child == 0) {
              _exit(0);
          }
          waitpid(child, nullptr, 0);
          std::thread thread([] {});
          thread.join();
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

  /** The ProgramRunner object under test. */
  @Autowired private ProgramRunner programRunner;

  /** The autowired Compiler object, used to compile the test programs. */
  @Autowired private Compiler compiler;

  /** The autowired Preprocessor object, used to write the test source to disk. */
  @Autowired private Preprocessor preprocessor;

  /** The autowired LanguageMapper object, used to fetch the C++ language definition. */
  @Autowired private LanguageMapper languageMapper;

  /** The working directory of the judger, used to store compilation results and program output. */
  @Value("${judger.workDir}")
  private String workBaseDirectory;
}
