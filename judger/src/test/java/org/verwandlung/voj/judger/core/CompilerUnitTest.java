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
import java.nio.file.Path;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

import org.verwandlung.voj.judger.model.Language;
import org.verwandlung.voj.judger.model.Submission;

/**
 * Tests {@link Compiler} with a sandbox that only pretends to run the compiler.
 *
 * @author Haozhe Xie
 */
public class CompilerUnitTest {
  /**
   * Test case: the sandbox stops the compiler at its time limit before it prints anything. Expected
   * result: a failed compilation whose log says why, rather than an empty Compile Error.
   */
  @Test
  public void testCompilationTimeLimitIsExplained() throws Exception {
    CompileResult result = compile(SandboxResult.Stop.TIME, "");

    Assertions.assertFalse(result.isSuccessful());
    Assertions.assertEquals(Verdict.TIME_LIMIT_EXCEEDED, result.verdict());
    Assertions.assertEquals("\nCompilation exceeded the time limit (20000 ms).\n", result.log());
    Assertions.assertTrue(
        compile(SandboxResult.Stop.MEMORY, "cc1plus: out of memory\n")
            .log()
            .startsWith("cc1plus: out of memory\n\nCompilation exceeded the memory limit."));
  }

  /**
   * Test case: the sandbox fails to run the compiler. Expected result: an internal error, so that
   * the dispatcher judges a System Error rather than a Compile Error.
   */
  @Test
  public void testCompilationSandboxFailureIsReported() throws Exception {
    CompileResult result = compile(null, "");

    Assertions.assertFalse(result.isSuccessful());
    Assertions.assertEquals(Verdict.INTERNAL_ERROR, result.verdict());
  }

  /** Test case: a compiler that prints a huge log. Expected result: only its beginning is kept. */
  @Test
  public void testCompilationLogIsTruncated() throws Exception {
    CompileResult result =
        compile(SandboxResult.Stop.NONE, "e".repeat(Compiler.MAX_COMPILE_LOG_BYTES * 4));

    Assertions.assertEquals(Verdict.RUNTIME_ERROR, result.verdict());
    Assertions.assertEquals(
        "e".repeat(Compiler.MAX_COMPILE_LOG_BYTES) + "\n... (truncated)\n", result.log());
  }

  /**
   * Compiles a submission with a sandbox that stops the compiler (which exits with 1) for the given
   * limit and leaves the given log.
   *
   * @param stop - the limit the sandbox stops the compiler for ({@code null}: the sandbox fails)
   * @param log - the compile log the compiler leaves
   * @return the compilation result
   */
  private CompileResult compile(SandboxResult.Stop stop, String log) throws Exception {
    Path workDirectory = Files.createTempDirectory(tempDir, "voj-");
    Files.createDirectory(workDirectory.resolve(Preprocessor.PROGRAM_DIRECTORY));
    Files.writeString(workDirectory.resolve(Compiler.COMPILE_LOG_FILE), log);
    SandboxRunner sandboxRunner = Mockito.mock(SandboxRunner.class);
    if (stop == null) {
      Mockito.when(sandboxRunner.run(Mockito.any())).thenThrow(new IOException("no sandbox"));
    } else {
      Mockito.when(sandboxRunner.run(Mockito.any())).thenReturn(new SandboxResult(0, 0, 1, stop));
    }
    SandboxPolicy sandboxPolicy = new SandboxPolicy();
    ReflectionTestUtils.setField(sandboxPolicy, "compileTimeLimit", 20000);
    ReflectionTestUtils.setField(sandboxPolicy, "compileMemoryLimit", 524288);
    ReflectionTestUtils.setField(sandboxPolicy, "outputLimitKb", 262144);
    Compiler compiler = new Compiler();
    ReflectionTestUtils.setField(compiler, "sandboxRunner", sandboxRunner);
    ReflectionTestUtils.setField(compiler, "sandboxPolicy", sandboxPolicy);
    Language language = new Language("text/x-test", "Test", "cc {filename}.c", "{filename}.exe");
    language.setSourceFilename("Main.c");
    Submission submission = new Submission();
    submission.setLanguage(language);
    return compiler.getCompileResult(submission, workDirectory.toString());
  }

  /** A temporary directory managed by JUnit. */
  @TempDir Path tempDir;
}
