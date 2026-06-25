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
