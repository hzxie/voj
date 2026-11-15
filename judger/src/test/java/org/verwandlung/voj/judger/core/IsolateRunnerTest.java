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
    ReflectionTestUtils.setField(runner, "maxProcesses", 64);
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
