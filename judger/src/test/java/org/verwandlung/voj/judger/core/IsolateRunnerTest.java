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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Tests for IsolateRunner against a fake {@code isolate} executable (a shell script emulating
 * {@code --init}, {@code --cleanup} and {@code --run} with its bound directories and
 * redirections), since the real isolate needs kernel privileges that test hosts do not grant.
 *
 * @author Haozhe Xie
 */
@DisabledOnOs(OS.WINDOWS)
public class IsolateRunnerTest {
  /** Creates the fake isolate, a checkpoint directory and the directories of a judging. */
  @BeforeEach
  public void setUp() throws Exception {
    isolateRoot = Files.createDirectory(tempDir.resolve("isolate"));
    Path fakeIsolate = tempDir.resolve("fake-isolate");
    Files.writeString(fakeIsolate, FAKE_ISOLATE.replace("@ROOT@", isolateRoot.toString()));
    makeExecutable(fakeIsolate);

    checkpointDirectory = Files.createDirectory(tempDir.resolve("checkpoints"));
    inputFile = Files.writeString(checkpointDirectory.resolve("input#1.txt"), "1 2\n");
    Files.writeString(checkpointDirectory.resolve("output#1.txt"), "3\n");
    judgingDirectory = Files.createDirectory(tempDir.resolve("voj-1000"));
    programDirectory = Files.createDirectory(judgingDirectory.resolve("program"));
    runDirectory = Files.createDirectory(judgingDirectory.resolve("run"));

    ReflectionTestUtils.setField(runner, "isolateExecutable", fakeIsolate.toString());
    ReflectionTestUtils.setField(runner, "boxId", 0);
  }

  /**
   * Test case: a run. Expected result: only the input file (copied into the box, readable by the
   * box's user) is visible, stdout is captured without stderr (into a file of the judger's alone), the checkpoint directory is never bound, the directory of
   * the judging is bound read-only and the run's work directory read-write, the program runs
   * there, and the box is cleaned up.
   */
  @Test
  public void testRunCapturesStdoutOnlyAndHidesCheckpoints() throws Exception {
    Path program = writeProgram("prog.sh", "#!/bin/sh\ncat\necho oops >&2\npwd > where\n");
    Path outputFile = judgingDirectory.resolve("output#1.txt");

    SandboxResult result = runner.run(run(program.toString(), inputFile, outputFile));

    Assertions.assertEquals(new SandboxResult(10, 0, 0, SandboxResult.Stop.NONE), result);
    Assertions.assertEquals("1 2\n", Files.readString(outputFile));
    Assertions.assertEquals(
        PosixFilePermissions.fromString("rw-------"), Files.getPosixFilePermissions(outputFile));
    Assertions.assertEquals("444\n", Files.readString(isolateRoot.resolve("stdin.mode")));
    Assertions.assertTrue(Files.exists(runDirectory.resolve("where")));
    List<String> args = Files.readAllLines(isolateRoot.resolve("args.log"));
    Assertions.assertTrue(args.contains("--stdin=" + IsolateRunner.BOX_STDIN_FILE));
    Assertions.assertTrue(args.contains("--stdout=" + IsolateRunner.BOX_STDOUT_FILE));
    Assertions.assertTrue(args.contains("--stderr=" + IsolateRunner.BOX_STDERR_FILE));
    Assertions.assertTrue(args.contains("--dir=/judging=" + judgingDirectory));
    Assertions.assertTrue(args.contains("--dir=/judging/run=" + runDirectory + ":rw"));
    Assertions.assertTrue(args.contains("--chdir=/judging/run"));
    Assertions.assertTrue(args.contains("/judging/program/prog.sh"));
    Assertions.assertTrue(args.contains("--env=HOME=/judging/run"));
    Assertions.assertTrue(args.contains("--env=PATH=/usr/bin:/bin"));
    Assertions.assertFalse(args.contains("--stderr-to-stdout"));
    Assertions.assertTrue(args.stream().noneMatch(arg -> arg.startsWith("--syscalls")));
    for (String arg : args) {
      Assertions.assertFalse(arg.contains(checkpointDirectory.toString()), arg);
    }
    Assertions.assertFalse(Files.exists(isolateRoot.resolve("0")));
  }

  /**
   * Test case: a compilation. Expected result: stdout and stderr both end up in the compile log, no
   * input is redirected, the program directory is bound read-write and the compilation may lock
   * files (for the Go build cache).
   */
  @Test
  public void testCompilationMergesStderrIntoLog() throws Exception {
    Path program = writeProgram("cc.sh", "#!/bin/sh\necho out\necho err >&2\nexit 1\n");
    Path logFile = judgingDirectory.resolve("compile.log");

    SandboxResult result = runner.run(compilation(program.toString(), logFile));

    Assertions.assertEquals(1, result.exitCode());
    Assertions.assertEquals("out\nerr\n", Files.readString(logFile));
    List<String> args = Files.readAllLines(isolateRoot.resolve("args.log"));
    Assertions.assertTrue(args.contains("--stderr-to-stdout"));
    Assertions.assertTrue(args.contains("--dir=/judging/program=" + programDirectory + ":rw"));
    Assertions.assertTrue(args.contains("--syscalls=27"));
    Assertions.assertTrue(args.stream().noneMatch(arg -> arg.startsWith("--stdin=")));
    Assertions.assertTrue(args.stream().noneMatch(arg -> arg.startsWith("--stderr=")));
  }

  /**
   * Test case: the limits of a run. Expected result: isolate gets them as they are, the wall-clock
   * limit the policy's.
   */
  @Test
  public void testLimits() throws Exception {
    List<String> args =
        runner.buildRunCommand(
            new SandboxCommand(
                List.of("/bin/true"),
                List.of(),
                runDirectory.toString(),
                null,
                null,
                false,
                SandboxPolicy.limits(false, 1500, 65536, 1024, 7)),
            new File("meta"));

    Assertions.assertTrue(args.contains("--processes=7"));
    Assertions.assertTrue(args.contains("--time=1.5"));
    Assertions.assertTrue(args.contains("--wall-time=4.0"));
    Assertions.assertTrue(args.contains("--cg-mem=65536"));
    Assertions.assertTrue(args.contains("--fsize=1024"));
    Assertions.assertTrue(args.contains("--stack=65536"));
    Assertions.assertTrue(args.contains("--open-files=128"));
  }

  /**
   * Test case: tests the parseMeta(File) method with the fields isolate writes. Expected result:
   * the figures as they are, a signal as 128 + it, and the limit isolate stopped the program for:
   * the time limit (TO), the memory limit (the cgroup's OOM killer, whatever the status says) or
   * the output limit (SIGXFSZ); a failure of isolate is an exception.
   */
  @Test
  public void testParseMeta() throws Exception {
    Assertions.assertEquals(
        new SandboxResult(500, 2048, 0, SandboxResult.Stop.NONE),
        meta("time:0.500\ncg-mem:2048\nmax-rss:1024\nexitcode:0\n"));
    Assertions.assertEquals(
        new SandboxResult(1000, 0, 137, SandboxResult.Stop.TIME),
        meta("status:TO\ntime:1.000\nexitsig:9\n"));
    Assertions.assertEquals(
        SandboxResult.Stop.MEMORY,
        meta("status:SG\nexitsig:9\ncg-oom-killed:1\n").stoppedFor());
    Assertions.assertEquals(
        SandboxResult.Stop.MEMORY, meta("status:TO\ncg-oom-killed:1\n").stoppedFor());
    Assertions.assertEquals(
        new SandboxResult(0, 0, 153, SandboxResult.Stop.OUTPUT), meta("status:SG\nexitsig:25\n"));
    Assertions.assertEquals(
        new SandboxResult(0, 0, 139, SandboxResult.Stop.NONE), meta("status:SG\nexitsig:11\n"));
    Assertions.assertEquals(
        new SandboxResult(0, 0, 1, SandboxResult.Stop.NONE), meta("status:RE\nexitcode:1\n"));
    Assertions.assertThrows(IOException.class, () -> meta("status:XX\nmessage:broken\n"));
  }

  /**
   * Test case: tests the toBoxPath(String, String) method. Expected result: the directory of the
   * judging is rewritten to its path in the box wherever it appears in an argument, and only as a
   * whole path component.
   */
  @Test
  public void testToBoxPathRewritesTheDirectoryOfTheJudging() {
    Assertions.assertEquals(
        "/judging/program/a.cpp",
        IsolateRunner.toBoxPath("/tmp/voj-1/program/a.cpp", "/tmp/voj-1"));
    Assertions.assertEquals(
        "-o/judging/program/a", IsolateRunner.toBoxPath("-o/tmp/voj-1/program/a", "/tmp/voj-1"));
    Assertions.assertEquals(
        "HOME=/judging/run", IsolateRunner.toBoxPath("HOME=/tmp/voj-1/run", "/tmp/voj-1"));
    Assertions.assertEquals(
        "/tmp/voj-10/a", IsolateRunner.toBoxPath("/tmp/voj-10/a", "/tmp/voj-1"));
    Assertions.assertEquals("g++", IsolateRunner.toBoxPath("g++", "/tmp/voj-1"));
    // However its separators are repeated.
    Assertions.assertEquals(
        "/judging/a.cpp", IsolateRunner.toBoxPath("/tmp//voj-1/a.cpp", "/tmp/voj-1"));
    Assertions.assertEquals(
        "/judging:/judging/lib",
        IsolateRunner.toBoxPath("/tmp/voj-1:/tmp/voj-1/lib", "/tmp/voj-1"));
  }

  /**
   * Test case: a compilation and a run of a program that prints a configured compile file from its
   * work directory, as fpc reads its configuration from HOME. Expected result: the compilation
   * finds the host file copied there, readable by the box's user (rules naming a path rather than
   * a plain file name are ignored); a run's work directory does not get it.
   */
  @Test
  public void testOnlyCompilationsGetTheirConfigFiles() throws Exception {
    Path configFile = Files.writeString(tempDir.resolve("fpc.cfg"), "-Fu/usr/lib/fpc\n");
    ReflectionTestUtils.setField(
        runner,
        "compileFiles",
        ".fpc.cfg=" + configFile + ",..=" + configFile + ",sub/x=" + configFile);
    Path logFile = judgingDirectory.resolve("compile.log");
    Path cat = Path.of("/bin/cat");

    SandboxResult compilation = runner.run(compilation(cat + " .fpc.cfg", logFile));

    Assertions.assertEquals(0, compilation.exitCode());
    Assertions.assertEquals("-Fu/usr/lib/fpc\n", Files.readString(logFile));
    Assertions.assertEquals(
        PosixFilePermissions.fromString("r--r--r--"),
        Files.getPosixFilePermissions(programDirectory.resolve(".fpc.cfg")));

    Path outputFile = judgingDirectory.resolve("output#1.txt");
    SandboxResult run = runner.run(run(cat + " .fpc.cfg", null, outputFile));

    Assertions.assertNotEquals(0, run.exitCode());
  }

  /**
   * Test case: the program replaces its captured output with a symbolic link to a file outside the
   * box. Expected result: the link is not followed; the output is empty.
   */
  @Test
  public void testRunDoesNotFollowSymbolicLinkedOutput() throws Exception {
    Path secret = Files.writeString(tempDir.resolve("secret.txt"), "secret\n");
    Path box = isolateRoot.resolve("0/box");
    Path program =
        writeProgram(
            "evil.sh",
            "#!/bin/sh\nrm -f " + box + "/stdout.txt\nln -s " + secret + " " + box
                + "/stdout.txt\n");
    Path outputFile = judgingDirectory.resolve("output#1.txt");

    runner.run(run(program.toString(), inputFile, outputFile));

    Assertions.assertFalse(Files.isSymbolicLink(outputFile));
    Assertions.assertEquals("", Files.readString(outputFile));
  }

  /**
   * Builds a run in the run directory.
   *
   * @param command - the program and its arguments, separated by spaces
   * @param input - the input ({@code null}: none)
   * @param output - the output file
   * @return the run
   */
  private SandboxCommand run(String command, Path input, Path output) {
    return new SandboxCommand(
        List.of(command.split(" ")),
        List.of("PATH=/usr/bin:/bin", "HOME=" + runDirectory),
        runDirectory.toString(),
        input == null ? null : input.toString(),
        output.toString(),
        false,
        SandboxPolicy.limits(false, 1000, 262144, 262144, 64));
  }

  /**
   * Builds a compilation in the program directory.
   *
   * @param command - the program and its arguments, separated by spaces
   * @param log - the compile log
   * @return the compilation
   */
  private SandboxCommand compilation(String command, Path log) {
    return new SandboxCommand(
        List.of(command.split(" ")),
        List.of("PATH=/usr/bin:/bin", "HOME=" + programDirectory),
        programDirectory.toString(),
        null,
        log.toString(),
        true,
        SandboxPolicy.limits(true, 1000, 524288, 262144, 1024));
  }

  /**
   * Parses a meta file.
   *
   * @param content - the content of the meta file
   * @return what it says
   */
  private SandboxResult meta(String content) throws Exception {
    Path metaFile = Files.writeString(tempDir.resolve("meta"), content);
    return runner.parseMeta(metaFile.toFile());
  }

  /**
   * Writes an executable program into the program directory.
   *
   * @param name - the file name
   * @param content - the script content
   * @return the program path
   */
  private Path writeProgram(String name, String content) throws Exception {
    Path program = Files.writeString(programDirectory.resolve(name), content);
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
   * {@code --cleanup} removes it, and {@code --run} logs its arguments, then runs the command with
   * the box's paths mapped back onto the host (the bound directories and {@code /box}), in the
   * directory of {@code --chdir}, with the redirections, and writes a minimal meta file.
   */
  private static final String FAKE_ISOLATE =
      String.join(
          "\n",
          "#!/bin/bash",
          "ROOT='@ROOT@'",
          "ARGS=(\"$@\")",
          "id=0; meta=; stdin=; stdout=; stderr=; merge=0; action=; chdir=; maps=()",
          "while [ $# -gt 0 ]; do",
          "  case \"$1\" in",
          "    -b) id=$2; shift;;",
          "    --dir=*=*) rule=${1#--dir=}; outside=${rule#*=};"
              + " maps+=(\"${rule%%=*}=${outside%:rw}\");;",
          "    -M) meta=$2; shift;;",
          "    --chdir=*) chdir=${1#--chdir=};;",
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
          "host() { local p=$1 m; p=${p//\\/box\\//$ROOT/$id/box/};"
              + " for m in \"${maps[@]}\"; do p=${p//${m%%=*}/${m#*=}}; done; echo \"$p\"; }",
          "case $action in",
          "  init) mkdir -p \"$ROOT/$id/box\" && echo \"$ROOT/$id\";;",
          "  cleanup) rm -rf \"$ROOT/$id\";;",
          "  run)",
          "    printf '%s\\n' \"${ARGS[@]}\" > \"$ROOT/args.log\"",
          "    cd \"$(host \"$chdir\")\" || exit 2",
          "    if [ -n \"$stdin\" ]; then stat -c %a \"$(host \"$stdin\")\" > \"$ROOT/stdin.mode\";"
              + " exec 0<\"$(host \"$stdin\")\"; fi",
          "    if [ -n \"$stdout\" ]; then exec 1>\"$(host \"$stdout\")\"; fi",
          "    if [ $merge = 1 ]; then exec 2>&1;",
          "    elif [ -n \"$stderr\" ]; then exec 2>\"$(host \"$stderr\")\"; fi",
          "    cmd=(); for a in \"$@\"; do cmd+=(\"$(host \"$a\")\"); done",
          "    \"${cmd[@]}\"; code=$?",
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

  /** The directory of the judging. */
  private Path judgingDirectory;

  /** The program directory of the judging. */
  private Path programDirectory;

  /** The work directory of a run. */
  private Path runDirectory;

  /** A temporary directory managed by JUnit. */
  @TempDir Path tempDir;
}
