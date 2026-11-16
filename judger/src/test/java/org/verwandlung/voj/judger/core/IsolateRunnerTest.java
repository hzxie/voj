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
import java.nio.file.attribute.PosixFilePermissions;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Tests for IsolateRunner against a fake {@code isolate} executable (a shell script emulating
 * {@code --init}, {@code --cleanup} and {@code --run} with box-relative redirections), since the
 * real isolate needs kernel privileges that test hosts do not grant.
 *
 * @author Haozhe Xie
 */
@DisabledOnOs(OS.WINDOWS)
public class IsolateRunnerTest {
  /** Creates the fake isolate, a checkpoint directory and a work directory. */
  @BeforeEach
  public void setUp() throws Exception {
    isolateRoot = Files.createDirectory(tempDir.resolve("isolate"));
    Path fakeIsolate = tempDir.resolve("fake-isolate");
    Files.writeString(fakeIsolate, FAKE_ISOLATE.replace("@ROOT@", isolateRoot.toString()));
    makeExecutable(fakeIsolate);

    checkpointDirectory = Files.createDirectory(tempDir.resolve("checkpoints"));
    inputFile = Files.writeString(checkpointDirectory.resolve("input#1.txt"), "1 2\n");
    Files.writeString(checkpointDirectory.resolve("output#1.txt"), "3\n");
    workDirectory = Files.createDirectory(tempDir.resolve("voj-1000"));

    ReflectionTestUtils.setField(runner, "isolateExecutable", fakeIsolate.toString());
    ReflectionTestUtils.setField(runner, "boxId", 0);
    ReflectionTestUtils.setField(runner, "processLimit", 64);
    ReflectionTestUtils.setField(runner, "compileProcessLimit", 1024);
    ReflectionTestUtils.setField(runner, "fileSizeLimitKb", 0);
    ReflectionTestUtils.setField(runner, "wallTimeBufferSec", 1.0);
  }

  /**
   * Test case: a submission run. Expected result: only the input file (copied into the box) is
   * visible, stdout is captured without stderr, the checkpoint directory is never bound, the work
   * directory is bound read-only, and the box is cleaned up.
   */
  @Test
  public void testRunCapturesStdoutOnlyAndHidesCheckpoints() throws Exception {
    Path program = writeProgram("prog.sh", "#!/bin/sh\ncat\necho oops >&2\n");
    Path outputFile = workDirectory.resolve("output#1.txt");

    RunResult result =
        runner.run(program.toString(), inputFile.toString(), outputFile.toString(), 1000, 0, false);

    Assertions.assertEquals(Verdict.NORMAL, result.getVerdict());
    Assertions.assertEquals("1 2\n", Files.readString(outputFile));
    List<String> args = Files.readAllLines(isolateRoot.resolve("args.log"));
    Assertions.assertTrue(args.contains("--stdin=" + IsolateRunner.BOX_STDIN_FILE));
    Assertions.assertTrue(args.contains("--stdout=" + IsolateRunner.BOX_STDOUT_FILE));
    Assertions.assertTrue(args.contains("--stderr=" + IsolateRunner.BOX_STDERR_FILE));
    Assertions.assertTrue(
        args.contains("--dir=" + IsolateRunner.BOX_WORK_DIRECTORY + "=" + workDirectory));
    Assertions.assertTrue(args.contains(IsolateRunner.BOX_WORK_DIRECTORY + "/prog.sh"));
    Assertions.assertFalse(args.contains("--stderr-to-stdout"));
    for (String arg : args) {
      Assertions.assertFalse(arg.contains(checkpointDirectory.toString()), arg);
    }
    Assertions.assertFalse(Files.exists(isolateRoot.resolve("0")));
  }

  /**
   * Test case: a compilation. Expected result: stdout and stderr both end up in the compile log, no
   * input is redirected, and the work directory is bound read-write.
   */
  @Test
  public void testCompilationMergesStderrIntoLog() throws Exception {
    Path program = writeProgram("cc.sh", "#!/bin/sh\necho out\necho err >&2\nexit 1\n");
    Path logFile = workDirectory.resolve("random-compile.log");

    RunResult result = runner.run(program.toString(), null, logFile.toString(), 1000, 0, true);

    Assertions.assertEquals(Verdict.RUNTIME_ERROR, result.getVerdict());
    Assertions.assertEquals("out\nerr\n", Files.readString(logFile));
    List<String> args = Files.readAllLines(isolateRoot.resolve("args.log"));
    Assertions.assertTrue(args.contains("--stderr-to-stdout"));
    Assertions.assertTrue(
        args.contains("--dir=" + IsolateRunner.BOX_WORK_DIRECTORY + "=" + workDirectory + ":rw"));
    Assertions.assertTrue(args.stream().noneMatch(arg -> arg.startsWith("--stdin=")));
    Assertions.assertTrue(args.stream().noneMatch(arg -> arg.startsWith("--stderr=")));
  }

  /**
   * Test case: a command naming its program without a path ("sh ..."), as every seeded language
   * does. Expected result: isolate, which neither searches PATH nor passes on the environment, is
   * given the program's absolute path and a PATH for the box.
   */
  @Test
  public void testRunResolvesTheProgramOnPath() throws Exception {
    Path script = writeProgram("hello.sh", "echo hello\n");
    Path logFile = workDirectory.resolve("random-compile.log");

    RunResult result = runner.run("sh " + script, null, logFile.toString(), 1000, 0, true);

    Assertions.assertEquals(Verdict.NORMAL, result.getVerdict());
    Assertions.assertEquals("hello\n", Files.readString(logFile));
    List<String> args = Files.readAllLines(isolateRoot.resolve("args.log"));
    String program = args.get(args.indexOf("--") + 1);
    Assertions.assertTrue(program.startsWith("/") && program.endsWith("/sh"), program);
    Assertions.assertTrue(args.stream().anyMatch(arg -> arg.startsWith("--env=PATH=/")));
  }

  /**
   * Test case: a run and a compilation. Expected result: a run may have judger.processLimit
   * processes and threads, a compilation judger.compileProcessLimit.
   */
  @Test
  public void testProcessLimits() throws Exception {
    ReflectionTestUtils.setField(runner, "processLimit", 7);
    Path logFile = workDirectory.resolve("random-compile.log");
    runner.run("true", null, logFile.toString(), 1000, 0, true);
    Assertions.assertTrue(
        Files.readAllLines(isolateRoot.resolve("args.log")).contains("--processes=1024"));

    runner.run("true", null, workDirectory.resolve("output#1.txt").toString(), 1000, 0, false);
    Assertions.assertTrue(
        Files.readAllLines(isolateRoot.resolve("args.log")).contains("--processes=7"));
  }

  /**
   * Test case: a run and a compilation. Expected result: the run's JVM is given the options derived
   * from its memory limit (see JavaOptions); a compilation's is not.
   */
  @Test
  public void testOnlyRunsGetTheJavaOptions() throws Exception {
    runner.run("true", null, workDirectory.resolve("output#1.txt").toString(), 1000, 65536, false);
    Assertions.assertTrue(
        Files.readAllLines(isolateRoot.resolve("args.log"))
            .contains("--env=JDK_JAVA_OPTIONS=" + JavaOptions.forRun(65536)));

    Path logFile = workDirectory.resolve("random-compile.log");
    runner.run("true", null, logFile.toString(), 1000, 65536, true);
    Assertions.assertTrue(
        Files.readAllLines(isolateRoot.resolve("args.log")).stream()
            .noneMatch(arg -> arg.startsWith("--env=JDK_JAVA_OPTIONS=")));
  }

  /**
   * Test case: tests the mapVerdict(Map, int) method with the fields isolate writes. Expected
   * result: a program killed by SIGXFSZ (its output outgrew --fsize) exceeded the output limit; one
   * killed by the cgroup's OOM killer exceeded the memory limit, whatever its status says.
   */
  @Test
  public void testMapVerdict() {
    Assertions.assertEquals(Verdict.NORMAL, IsolateRunner.mapVerdict(Map.of(), 65536));
    Assertions.assertEquals(
        Verdict.TIME_LIMIT_EXCEEDED, IsolateRunner.mapVerdict(Map.of("status", "TO"), 65536));
    Assertions.assertEquals(
        Verdict.OUTPUT_LIMIT_EXCEEDED,
        IsolateRunner.mapVerdict(Map.of("status", "SG", "exitsig", "25"), 65536));
    Assertions.assertEquals(
        Verdict.RUNTIME_ERROR,
        IsolateRunner.mapVerdict(Map.of("status", "SG", "exitsig", "11"), 65536));
    Assertions.assertEquals(
        Verdict.MEMORY_LIMIT_EXCEEDED,
        IsolateRunner.mapVerdict(
            Map.of("status", "SG", "exitsig", "9", "cg-oom-killed", "1", "cg-mem", "1024"),
            65536));
    Assertions.assertEquals(
        Verdict.MEMORY_LIMIT_EXCEEDED,
        IsolateRunner.mapVerdict(Map.of("status", "TO", "cg-oom-killed", "1"), 65536));
    Assertions.assertEquals(
        Verdict.MEMORY_LIMIT_EXCEEDED,
        IsolateRunner.mapVerdict(Map.of("status", "RE", "max-rss", "65536"), 65536));
    Assertions.assertEquals(
        Verdict.RUNTIME_ERROR, IsolateRunner.mapVerdict(Map.of("status", "RE"), 65536));
    Assertions.assertEquals(
        Verdict.INTERNAL_ERROR, IsolateRunner.mapVerdict(Map.of("status", "XX"), 65536));
  }

  /**
   * Test case: tests the toBoxPath(String, String) method. Expected result: the work directory is
   * rewritten to its path in the box wherever it appears in an argument, and only as a whole path
   * component.
   */
  @Test
  public void testToBoxPathRewritesTheWorkDirectory() {
    Assertions.assertEquals("/work/a.cpp", IsolateRunner.toBoxPath("/tmp/voj-1/a.cpp", "/tmp/voj-1"));
    Assertions.assertEquals("-o/work/a.exe", IsolateRunner.toBoxPath("-o/tmp/voj-1/a.exe", "/tmp/voj-1"));
    Assertions.assertEquals("/work", IsolateRunner.toBoxPath("/tmp/voj-1", "/tmp/voj-1"));
    Assertions.assertEquals("/tmp/voj-10/a", IsolateRunner.toBoxPath("/tmp/voj-10/a", "/tmp/voj-1"));
    Assertions.assertEquals("g++", IsolateRunner.toBoxPath("g++", "/tmp/voj-1"));
    // Repeated separators, e.g. from a directory setting with a trailing slash.
    Assertions.assertEquals(
        "/work/a.cpp", IsolateRunner.toBoxPath("/tmp//voj-1/a.cpp", "/tmp/voj-1"));
    Assertions.assertEquals(
        "-o/work//a.exe", IsolateRunner.toBoxPath("-o/tmp//voj-1//a.exe", "/tmp/voj-1"));
    Assertions.assertEquals("/work/", IsolateRunner.toBoxPath("/tmp/voj-1/", "/tmp/voj-1"));
    Assertions.assertEquals(
        "/work:/work/lib", IsolateRunner.toBoxPath("/tmp/voj-1:/tmp/voj-1/lib", "/tmp/voj-1"));
  }

  /**
   * Test case: a compilation and a run. Expected result: the compilation is given its own syscall
   * filter flags (file locks allowed, for the Go build cache); the run keeps isolate's.
   */
  @Test
  public void testOnlyCompilationsMayLockFiles() throws Exception {
    Path logFile = workDirectory.resolve("random-compile.log");
    RunResult compilation = runner.run("true", null, logFile.toString(), 1000, 0, true);

    Assertions.assertEquals(Verdict.NORMAL, compilation.getVerdict());
    Assertions.assertTrue(
        Files.readAllLines(isolateRoot.resolve("args.log")).contains("--syscalls=27"));

    Path outputFile = workDirectory.resolve("output#1.txt");
    RunResult run = runner.run("true", null, outputFile.toString(), 1000, 0, false);

    Assertions.assertEquals(Verdict.NORMAL, run.getVerdict());
    Assertions.assertTrue(
        Files.readAllLines(isolateRoot.resolve("args.log")).stream()
            .noneMatch(arg -> arg.startsWith("--syscalls")));
  }

  /**
   * Test case: a compilation and a run of a program that prints a configured compile file from its
   * working directory (the box), as fpc reads its configuration from HOME. Expected result: the
   * compilation finds the host file copied into the box (rules naming a path rather than a plain
   * file name are ignored); the run does not get it.
   */
  @Test
  public void testOnlyCompilationsGetTheirConfigFiles() throws Exception {
    Path configFile = Files.writeString(tempDir.resolve("fpc.cfg"), "-Fu/usr/lib/fpc\n");
    // Copying to ".." or "sub/x" would fail and turn the compilation into an internal error.
    ReflectionTestUtils.setField(
        runner,
        "compileFiles",
        ".fpc.cfg=" + configFile + ",..=" + configFile + ",sub/x=" + configFile);
    Path logFile = workDirectory.resolve("random-compile.log");

    RunResult compilation = runner.run("cat .fpc.cfg", null, logFile.toString(), 1000, 0, true);

    Assertions.assertEquals(Verdict.NORMAL, compilation.getVerdict());
    Assertions.assertEquals("-Fu/usr/lib/fpc\n", Files.readString(logFile));

    Path outputFile = workDirectory.resolve("output#1.txt");
    RunResult run = runner.run("cat .fpc.cfg", null, outputFile.toString(), 1000, 0, false);

    Assertions.assertEquals(Verdict.RUNTIME_ERROR, run.getVerdict());
  }

  /**
   * Test case: tests the boxEnvironment(Map) method. Expected result: only the locale and toolchain
   * variables are passed on (no secrets), and the Go build cache and GOPATH are the submission's
   * own, in the work directory, even when the judger sets them.
   */
  @Test
  public void testBoxEnvironmentKeepsSecretsOutAndGivesGoItsOwnCache() {
    Map<String, String> environment =
        IsolateRunner.boxEnvironment(
            Map.of(
                "PATH", "/opt/go/bin:/usr/bin",
                "GOROOT", "/opt/go",
                "GOCACHE", "/root/.cache/go-build",
                "GOPATH", "/root/go",
                "VOJ_DB_PASSWORD", "secret"));

    Assertions.assertEquals("/opt/go/bin:/usr/bin", environment.get("PATH"));
    Assertions.assertEquals("/opt/go", environment.get("GOROOT"));
    Assertions.assertEquals("/work/.cache/go-build", environment.get("GOCACHE"));
    Assertions.assertEquals("/work/go", environment.get("GOPATH"));
    Assertions.assertEquals("/box", environment.get("HOME"));
    Assertions.assertEquals("C.UTF-8", environment.get("LANG"));
    Assertions.assertFalse(environment.containsKey("VOJ_DB_PASSWORD"));
  }

  /**
   * Test case: runs whose output reaches the file size limit (--fsize), as when the program ignores
   * SIGXFSZ, so that it is not killed but its writes fail and it exits on its own, normally or
   * not. Expected result: Output Limit Exceeded, as with the native runner; an output below the
   * limit and a compile log of any size are judged as usual.
   */
  @Test
  public void testOutputThatReachedTheFileSizeLimitIsOutputLimitExceeded() throws Exception {
    ReflectionTestUtils.setField(runner, "fileSizeLimitKb", 1);
    Path outputFile = workDirectory.resolve("output#1.txt");
    Path logFile = workDirectory.resolve("random-compile.log");

    Path exitsNormally = writeProgram("normal.sh", "#!/bin/sh\nhead -c 1024 /dev/zero\n");
    Path exitsWithError = writeProgram("error.sh", "#!/bin/sh\nhead -c 1024 /dev/zero\nexit 1\n");
    Path smallOutput = writeProgram("small.sh", "#!/bin/sh\nhead -c 1023 /dev/zero\n");

    Assertions.assertEquals(
        Verdict.OUTPUT_LIMIT_EXCEEDED,
        runner.run(exitsNormally.toString(), null, outputFile.toString(), 1000, 0, false)
            .getVerdict());
    Assertions.assertEquals(
        Verdict.OUTPUT_LIMIT_EXCEEDED,
        runner.run(exitsWithError.toString(), null, outputFile.toString(), 1000, 0, false)
            .getVerdict());
    Assertions.assertEquals(
        Verdict.NORMAL,
        runner.run(smallOutput.toString(), null, outputFile.toString(), 1000, 0, false)
            .getVerdict());
    Assertions.assertEquals(
        Verdict.NORMAL,
        runner.run(exitsNormally.toString(), null, logFile.toString(), 1000, 0, true).getVerdict());
    // A program killed for exceeding another limit keeps that verdict.
    Assertions.assertEquals(
        Verdict.TIME_LIMIT_EXCEEDED,
        runner
            .applyOutputLimit(new RunResult(Verdict.TIME_LIMIT_EXCEEDED, 1000, 0, 0), 1024)
            .getVerdict());
  }

  /**
   * Test case: the sandboxed program replaces its captured output with a symbolic link to a file
   * outside the box. Expected result: the link is not followed; the output is treated as empty.
   */
  @Test
  public void testRunDoesNotFollowSymbolicLinkedOutput() throws Exception {
    Path secret = Files.writeString(tempDir.resolve("secret.txt"), "secret\n");
    Path program =
        writeProgram(
            "evil.sh",
            "#!/bin/sh\nrm -f " + IsolateRunner.BOX_STDOUT_FILE + "\nln -s " + secret + " "
                + IsolateRunner.BOX_STDOUT_FILE + "\n");
    Path outputFile = workDirectory.resolve("output#1.txt");

    runner.run(program.toString(), inputFile.toString(), outputFile.toString(), 1000, 0, false);

    Assertions.assertFalse(Files.isSymbolicLink(outputFile));
    Assertions.assertEquals("", Files.readString(outputFile));
  }

  /**
   * Writes an executable program into the work directory.
   *
   * @param name - the file name
   * @param content - the script content
   * @return the program path
   */
  private Path writeProgram(String name, String content) throws Exception {
    Path program = Files.writeString(workDirectory.resolve(name), content);
    makeExecutable(program);
    return program;
  }

  /**
   * Makes a file executable by its owner.
   *
   * @param file - the file
   */
  private static void makeExecutable(Path file) throws Exception {
    Files.setPosixFilePermissions(file, PosixFilePermissions.fromString("rwx------"));
  }

  /**
   * A fake isolate: {@code --init} creates {@code <root>/<id>/box} and prints {@code <root>/<id>},
   * {@code --cleanup} removes it, and {@code --run} logs its arguments, then runs the command in
   * the box with the box-relative redirections and writes a minimal meta file.
   */
  private static final String FAKE_ISOLATE =
      String.join(
          "\n",
          "#!/bin/bash",
          "ROOT='@ROOT@'",
          "ARGS=(\"$@\")",
          "id=0; meta=; stdin=; stdout=; stderr=; merge=0; action=; inside=; outside=",
          "while [ $# -gt 0 ]; do",
          "  case \"$1\" in",
          "    -b) id=$2; shift;;",
          "    --dir=*=*) rule=${1#--dir=}; inside=${rule%%=*}; outside=${rule#*=};"
              + " outside=${outside%:rw};;",
          "    -M) meta=$2; shift;;",
          "    --stdin=*) stdin=${1#--stdin=};;",
          "    --stdout=*) stdout=${1#--stdout=};;",
          "    --stderr=*) stderr=${1#--stderr=};;",
          "    --stderr-to-stdout) merge=1;;",
          "    --init) action=init;;",
          "    --cleanup) action=cleanup;;",
          "    --run) action=run;;",
          "    --) shift; break;;",
          "  esac",
          "  shift",
          "done",
          "case $action in",
          "  init) mkdir -p \"$ROOT/$id/box\" && echo \"$ROOT/$id\";;",
          "  cleanup) rm -rf \"$ROOT/$id\";;",
          "  run)",
          "    printf '%s\\n' \"${ARGS[@]}\" > \"$ROOT/args.log\"",
          "    cd \"$ROOT/$id/box\" || exit 2",
          "    if [ -n \"$stdin\" ]; then exec 0<\"$stdin\"; fi",
          "    if [ -n \"$stdout\" ]; then exec 1>\"$stdout\"; fi",
          "    if [ $merge = 1 ]; then exec 2>&1;",
          "    elif [ -n \"$stderr\" ]; then exec 2>\"$stderr\"; fi",
          "    if [ -n \"$inside\" ]; then", // map the box's paths back onto the host
          "      cmd=(); for a in \"$@\"; do cmd+=(\"${a//$inside\\//$outside/}\"); done",
          "      set -- \"${cmd[@]}\"",
          "    fi",
          "    \"$@\"; code=$?",
          "    printf 'time:0.010\\nexitcode:%d\\n' $code > \"$meta\"",
          "    if [ $code -ne 0 ]; then echo 'status:RE' >> \"$meta\"; fi",
          "    exit $code;;",
          "esac",
          "");

  /** The IsolateRunner object under test. */
  private final IsolateRunner runner = new IsolateRunner();

  /** The root directory of the fake isolate's boxes. */
  private Path isolateRoot;

  /** The checkpoint directory holding both the input and the expected output. */
  private Path checkpointDirectory;

  /** The input file of the checkpoint. */
  private Path inputFile;

  /** The work directory of the submission. */
  private Path workDirectory;

  /** A temporary directory managed by JUnit. */
  @TempDir Path tempDir;
}
