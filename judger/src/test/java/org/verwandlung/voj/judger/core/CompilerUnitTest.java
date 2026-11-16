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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

import org.verwandlung.voj.judger.model.Language;
import org.verwandlung.voj.judger.model.Submission;

/**
 * Database-free unit tests for the compiler.
 *
 * @author Haozhe Xie
 */
public class CompilerUnitTest {
  /**
   * Test case: tests the getCompileMemoryLimit(Language) method. Test data: languages with memory
   * multipliers 1 (C++), 2 (Kotlin), 0.5 and a missing language. Expected result: the base limit is
   * scaled by multipliers above 1 only, so native compilers keep the compiler-bomb cap.
   */
  @Test
  public void testGetCompileMemoryLimitScalesWithTheMemoryMultiplier() {
    Compiler compiler = new Compiler();
    ReflectionTestUtils.setField(compiler, "compileMemoryLimit", 524288);

    Assertions.assertEquals(524288, compiler.getCompileMemoryLimit(language(1.0)));
    Assertions.assertEquals(1048576, compiler.getCompileMemoryLimit(language(2.0)));
    Assertions.assertEquals(524288, compiler.getCompileMemoryLimit(language(0.5)));
    Assertions.assertEquals(524288, compiler.getCompileMemoryLimit(null));
  }

  /**
   * Test case: tests the getCompileMemoryLimit(Language) method. Test data: no compile memory limit
   * (0). Expected result: still no limit, whatever the multiplier.
   */
  @Test
  public void testGetCompileMemoryLimitKeepsNoLimit() {
    Compiler compiler = new Compiler();
    ReflectionTestUtils.setField(compiler, "compileMemoryLimit", 0);

    Assertions.assertEquals(0, compiler.getCompileMemoryLimit(language(2.0)));
  }

  /**
   * Test case: the sandbox stops the compiler at its time limit before it prints anything. Expected
   * result: a failed compilation whose log says why, rather than an empty Compile Error.
   */
  @Test
  public void testCompilationTimeLimitIsExplained() throws Exception {
    Map<String, Object> result = compile(Verdict.TIME_LIMIT_EXCEEDED, "");

    Assertions.assertEquals(false, result.get("isSuccessful"));
    Assertions.assertEquals(Verdict.TIME_LIMIT_EXCEEDED, result.get("verdict"));
    Assertions.assertEquals(
        "\nCompilation exceeded the time limit (20000 ms).\n", result.get("log"));
    Assertions.assertTrue(
        ((String) compile(Verdict.MEMORY_LIMIT_EXCEEDED, "cc1plus: out of memory\n").get("log"))
            .startsWith("cc1plus: out of memory\n\nCompilation exceeded the memory limit."));
  }

  /**
   * Test case: the sandbox fails to run the compiler. Expected result: the verdict is reported, so
   * that the dispatcher judges a System Error rather than a Compile Error.
   */
  @Test
  public void testCompilationSandboxFailureIsReported() throws Exception {
    Map<String, Object> result = compile(Verdict.INTERNAL_ERROR, "");

    Assertions.assertEquals(false, result.get("isSuccessful"));
    Assertions.assertEquals(Verdict.INTERNAL_ERROR, result.get("verdict"));
  }

  /**
   * Test case: a compiler that prints a huge log. Expected result: only its beginning is kept.
   */
  @Test
  public void testCompilationLogIsTruncated() throws Exception {
    Map<String, Object> result =
        compile(Verdict.RUNTIME_ERROR, "e".repeat(Compiler.MAX_COMPILE_LOG_BYTES * 4));

    Assertions.assertEquals(
        "e".repeat(Compiler.MAX_COMPILE_LOG_BYTES) + "\n... (truncated)\n", result.get("log"));
  }

  /**
   * Compiles a submission with a sandbox that reports the given verdict and leaves the given log.
   *
   * @param verdict - the verdict the sandbox reports
   * @param log - the compile log the compiler leaves
   * @return the compilation result
   */
  private Map<String, Object> compile(Verdict verdict, String log) throws Exception {
    Path workDirectory = Files.createTempDirectory(tempDir, "voj-");
    Files.writeString(workDirectory.resolve(Compiler.COMPILE_LOG_FILE), log);
    SandboxRunner sandboxRunner = Mockito.mock(SandboxRunner.class);
    Mockito.when(
            sandboxRunner.run(
                Mockito.anyString(),
                Mockito.any(),
                Mockito.any(),
                Mockito.any(),
                Mockito.anyString(),
                Mockito.anyInt(),
                Mockito.anyInt(),
                Mockito.eq(true)))
        .thenReturn(new RunResult(verdict, 0, 0, 1));
    Compiler compiler = new Compiler();
    ReflectionTestUtils.setField(compiler, "sandboxRunner", sandboxRunner);
    ReflectionTestUtils.setField(compiler, "compileTimeLimit", 20000);
    ReflectionTestUtils.setField(compiler, "compileMemoryLimit", 524288);
    Submission submission = new Submission();
    submission.setLanguage(language(1.0));
    return compiler.getCompileResult(submission, workDirectory.toString());
  }

  /**
   * Creates a language with the given memory multiplier.
   *
   * @param memoryMultiplier - the memory multiplier
   * @return the language
   */
  private Language language(double memoryMultiplier) {
    Language language = new Language("text/x-test", "Test", "cc {filename}.c", "{filename}.exe");
    language.setSourceFilename("Main.c");
    language.setMemoryMultiplier(memoryMultiplier);
    return language;
  }

  /** A temporary directory managed by JUnit. */
  @TempDir Path tempDir;
}
