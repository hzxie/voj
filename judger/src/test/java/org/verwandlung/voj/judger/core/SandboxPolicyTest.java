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

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.springframework.test.util.ReflectionTestUtils;

import org.verwandlung.voj.judger.model.Language;
import org.verwandlung.voj.judger.model.Problem;

/**
 * Tests {@link SandboxPolicy}, which builds every compilation and run.
 *
 * @author Haozhe Xie
 */
public class SandboxPolicyTest {
  /**
   * Test case: a compile command whose program directory has a space, a dollar sign and (on
   * Windows) backslashes. Expected result: each argument of the command is one argument, with
   * {@code {filename}} replaced literally by the source's path without its extension.
   */
  @Test
  public void testFilenameStandsForTheSourceWithoutItsExtension() {
    String programDirectory = Paths.get("/srv/my $1/program").toString();
    String filename = Paths.get(programDirectory, "Main").toString();

    Assertions.assertEquals(
        List.of("cc", "-o", filename + ".exe", filename + ".c", "-I" + filename),
        SandboxPolicy.arguments(
            "cc  -o {filename}.exe\t{filename}.c -I{filename}",
            language("cc -o {filename}.exe {filename}.c", "Main.c"),
            programDirectory,
            Map.of("PATH", "")));
    Assertions.assertEquals(
        List.of("java", "-cp", filename, "Main"),
        SandboxPolicy.arguments(
            "java -cp {filename} Main",
            language("javac -d {filename} {filename}.java", "Main.java"),
            programDirectory,
            Map.of("PATH", "")));
  }

  /**
   * Test case: programs named with and without a directory. Expected result: a name is looked up
   * on the search path (the first directory that has it), a path is taken as it is, and a name
   * that is nowhere is left as it is (it then fails to start).
   */
  @Test
  @DisabledOnOs(OS.WINDOWS)
  public void testTheProgramIsLookedUpOnThePath() {
    Assertions.assertEquals("/bin/sh", SandboxPolicy.findExecutable("sh", "/nonexistent:/bin"));
    Assertions.assertEquals("./sh", SandboxPolicy.findExecutable("./sh", "/bin"));
    Assertions.assertEquals(
        "voj-no-such-program", SandboxPolicy.findExecutable("voj-no-such-program", "/bin"));
  }

  /**
   * Test case: the environment of a run. Expected result: none of the judger's secrets, the
   * judger's toolchain settings, and a home, temporary directory and Go caches of the run's own,
   * with the options of its JVM.
   */
  @Test
  public void testTheEnvironmentKeepsSecretsOutAndTheProgramInItsWorkDirectory() {
    String runDirectory = Paths.get("/srv/voj-1/run").toString();
    Map<String, String> environment =
        policy().environment(runDirectory, JavaOptions.forRun(65536));

    Assertions.assertEquals(System.getenv("PATH"), environment.get("PATH"));
    Assertions.assertEquals(runDirectory, environment.get("HOME"));
    Assertions.assertEquals(
        Paths.get(runDirectory, ".cache/go-build").toString(), environment.get("GOCACHE"));
    Assertions.assertEquals(Paths.get(runDirectory, "go").toString(), environment.get("GOPATH"));
    Assertions.assertEquals(JavaOptions.forRun(65536), environment.get("JDK_JAVA_OPTIONS"));
    for (String name : environment.keySet()) {
      Assertions.assertTrue(
          SandboxPolicy.INHERITED_VARIABLES.contains(name)
              || SandboxPolicy.WINDOWS_VARIABLES.contains(name)
              || List.of(
                      "HOME", "TMPDIR", "TEMP", "TMP", "USERPROFILE", "APPDATA", "LOCALAPPDATA",
                      "USERNAME", "GOCACHE", "GOPATH", "JDK_JAVA_OPTIONS")
                  .contains(name),
          name);
    }
    Assertions.assertFalse(
        policy().environment(runDirectory, null).containsKey("JDK_JAVA_OPTIONS"));
  }

  /**
   * Test case: a compilation and a run in a language with multipliers of 2. Expected result: the
   * compilation gets the compile limits (memory scaled by the multiplier) and the compile process
   * limit, in the program directory, writing to the log; the run the problem's limits scaled by
   * the multipliers, judger.processLimit processes and threads (on Windows, its process and its
   * console host), a stack as large as its memory limit and few descriptors, in its own work
   * directory; and both a wall-clock limit of twice the time limit plus a second.
   */
  @Test
  public void testLimitsOfACompilationAndARun() {
    Language language = language("cc {filename}.c", "Main.c");
    language.setRunCommand("{filename}.exe");
    language.setTimeMultiplier(2.0);
    language.setMemoryMultiplier(2.0);
    Problem problem = new Problem();
    problem.setTimeLimit(1000);
    problem.setMemoryLimit(65536);

    SandboxCommand compilation = policy().compilation(language, "/w/program", "/w/compile.log");
    SandboxCommand run =
        policy()
            .run(language, problem, "/w/program", "/w/run", "/c/input#1.txt", "/w/output#1.txt");

    Assertions.assertTrue(compilation.compilation());
    Assertions.assertEquals("/w/program", compilation.workDirectory());
    Assertions.assertEquals("/w/compile.log", compilation.outputFile());
    Assertions.assertEquals(20000, compilation.limits().timeLimitMs());
    Assertions.assertEquals(41000, compilation.limits().wallTimeLimitMs());
    Assertions.assertEquals(1048576, compilation.limits().memoryLimitKb());
    Assertions.assertEquals(262144, compilation.limits().outputLimitKb());
    Assertions.assertEquals(1024, compilation.limits().processLimit());
    Assertions.assertEquals(8192, compilation.limits().stackLimitKb());
    Assertions.assertEquals(4096, compilation.limits().fileLimit());
    Assertions.assertEquals(1024, compilation.limits().signalQueueLimit());
    Assertions.assertFalse(run.compilation());
    Assertions.assertEquals("/w/run", run.workDirectory());
    Assertions.assertEquals(2000, run.limits().timeLimitMs());
    Assertions.assertEquals(5000, run.limits().wallTimeLimitMs());
    Assertions.assertEquals(131072, run.limits().memoryLimitKb());
    Assertions.assertEquals(262144, run.limits().outputLimitKb());
    Assertions.assertEquals(
        System.getProperty("os.name").contains("Windows") ? 2 : 64, run.limits().processLimit());
    Assertions.assertEquals(131072, run.limits().stackLimitKb());
    Assertions.assertEquals(128, run.limits().fileLimit());
    Assertions.assertEquals(1024, run.limits().signalQueueLimit());
    Assertions.assertEquals(
        Path.of("/w/program", "Main.exe").toString(), run.arguments().get(0));
  }

  /**
   * Test case: tests the memory backstop. Expected result: twice the limit plus 256 MB for a run,
   * whatever the host's RAM, so that whether an allocation is beyond it does not depend on the
   * host; at least that for a compilation, whose compilers' JVMs size themselves to the host.
   */
  @Test
  public void testTheMemoryBackstop() {
    Assertions.assertEquals(
        2 * 65536 + 262144, SandboxPolicy.limits(false, 1000, 65536, 1, 1).memoryBackstopKb());
    Assertions.assertTrue(
        SandboxPolicy.limits(true, 1000, 65536, 1, 1).memoryBackstopKb() >= 2 * 65536 + 262144);
    Assertions.assertEquals(
        Integer.MAX_VALUE,
        SandboxPolicy.limits(false, 1000, Integer.MAX_VALUE, 1, 1).memoryBackstopKb());
  }

  /**
   * Test case: limits that are zero or negative, of a problem or configured. Expected result:
   * refused, where the limits are built and as the judger starts: nothing runs without a limit.
   */
  @Test
  public void testEveryLimitMustBePositive() {
    Assertions.assertThrows(
        IllegalArgumentException.class, () -> SandboxPolicy.limits(false, 0, 65536, 1, 1));
    Assertions.assertThrows(
        IllegalArgumentException.class, () -> SandboxPolicy.limits(false, 1000, -1, 1, 1));
    Assertions.assertThrows(
        IllegalArgumentException.class, () -> SandboxPolicy.limits(true, 1000, 65536, 0, 1));
    Assertions.assertThrows(
        IllegalArgumentException.class, () -> SandboxPolicy.limits(true, 1000, 65536, 1, 0));
    policy().afterPropertiesSet();
    for (String setting :
        List.of(
            "compileTimeLimit",
            "compileMemoryLimit",
            "processLimit",
            "compileProcessLimit",
            "outputLimitKb")) {
      SandboxPolicy policy = policy();
      ReflectionTestUtils.setField(policy, setting, 0);
      Assertions.assertThrows(IllegalStateException.class, policy::afterPropertiesSet, setting);
    }
  }

  /**
   * Creates the policy, with the default settings.
   *
   * @return the policy
   */
  private static SandboxPolicy policy() {
    SandboxPolicy policy = new SandboxPolicy();
    ReflectionTestUtils.setField(policy, "sandboxUsername", "voj-sandbox");
    ReflectionTestUtils.setField(policy, "compileTimeLimit", 20000);
    ReflectionTestUtils.setField(policy, "compileMemoryLimit", 524288);
    ReflectionTestUtils.setField(policy, "processLimit", 64);
    ReflectionTestUtils.setField(policy, "compileProcessLimit", 1024);
    ReflectionTestUtils.setField(policy, "outputLimitKb", 262144);
    return policy;
  }

  /**
   * Creates a language.
   *
   * @param compileCommand - its compile command
   * @param sourceFilename - the name of its source file
   * @return the language
   */
  private static Language language(String compileCommand, String sourceFilename) {
    Language language = new Language("text/x-test", "Test", compileCommand, "{filename}.exe");
    language.setSourceFilename(sourceFilename);
    return language;
  }
}
