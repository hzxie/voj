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
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * A Linux {@link SandboxRunner} that delegates execution to <a
 * href="https://github.com/ioi/isolate">isolate</a> (cgroups v2 + namespaces + seccomp). Unlike the
 * legacy native runner, isolate reports the verdict and the resource usage from its own kernel-level
 * accounting, so {@link #run} trusts isolate's {@code status} and {@code cg-mem}/{@code time} rather
 * than guessing from an exit code.
 *
 * <p>This runner is selected when {@code judger.sandbox=isolate}; see {@code SandboxConfiguration}.
 * It is a thin wrapper that shells out to the {@code isolate} binary, so it requires no JNI.
 *
 * <p><b>Deployment requirements / assumptions (validate against your isolate version):</b>
 *
 * <ul>
 *   <li>isolate 2.x with cgroup v2 support is installed and the {@code --cg} option is available.
 *   <li>The judger process is allowed to run isolate (isolate is installed setuid-root, or the
 *       judger runs with the appropriate privileges / via {@code isolate}'s init).
 *   <li>The work directory is bound into the sandbox at its <em>same absolute path</em> via {@code
 *       --dir}, so the absolute command line built by {@link ProgramRunner} works unchanged inside
 *       the box. It is writable while compiling (the compiler writes the binary there) and
 *       read-only while running a submission. It must be accessible to isolate's box user.
 *   <li>Standard input/output never go through a bound directory. The checkpoint directory, which
 *       holds both the inputs and the expected outputs, is never bound: the single input file of
 *       the current run is copied into isolate's own box directory and redirected from there, and
 *       the captured output is copied out of the box after the run. isolate opens these
 *       redirections inside the sandbox, relative to the box (its working directory), as the box
 *       user, and owns the box directory's permissions, so this works whatever the permissions of
 *       the checkpoint and work directories are.
 * </ul>
 *
 * @author Haozhe Xie
 */
@Component
public class IsolateRunner implements SandboxRunner {
  /** The name, inside the box, of the copy of the input file. */
  static final String BOX_STDIN_FILE = "stdin.txt";

  /** The name, inside the box, of the captured standard output. */
  static final String BOX_STDOUT_FILE = "stdout.txt";

  /** The name, inside the box, of the (discarded) standard error of a submission run. */
  static final String BOX_STDERR_FILE = "stderr.txt";

  /**
   * Runs a command inside an isolate sandbox, capturing standard output and standard error together
   * (the semantics of a compilation).
   *
   * @param commandLine - the command line to execute (absolute paths)
   * @param inputFilePath - the file to redirect to standard input, or {@code null} for none
   * @param outputFilePath - the file to capture standard output/error into, or {@code null} for none
   * @param timeLimitMs - the time limit in milliseconds (0 means no limit)
   * @param memoryLimitKb - the memory limit in kilobytes (0 means no limit)
   * @return the structured run result; never {@code null}
   */
  @Override
  public RunResult run(
      String commandLine,
      String inputFilePath,
      String outputFilePath,
      int timeLimitMs,
      int memoryLimitKb) {
    return run(commandLine, inputFilePath, outputFilePath, timeLimitMs, memoryLimitKb, true);
  }

  /**
   * Runs a command inside an isolate sandbox.
   *
   * @param commandLine - the command line to execute (absolute paths)
   * @param inputFilePath - the file to redirect to standard input, or {@code null} for none
   * @param outputFilePath - the file to capture the output into, or {@code null} for none
   * @param timeLimitMs - the time limit in milliseconds (0 means no limit)
   * @param memoryLimitKb - the memory limit in kilobytes (0 means no limit)
   * @param isCompilation - whether this is a compilation (stderr merged into the output, work
   *     directory writable) or a run of the submission (stderr discarded, work directory read-only)
   * @return the structured run result; never {@code null}
   */
  @Override
  public synchronized RunResult run(
      String commandLine,
      String inputFilePath,
      String outputFilePath,
      int timeLimitMs,
      int memoryLimitKb,
      boolean isCompilation) {
    File metaFile = null;
    try {
      Path boxDirectory = initBox();
      metaFile = File.createTempFile("voj-isolate-", ".meta");

      boolean hasInput = inputFilePath != null && !inputFilePath.isEmpty();
      boolean hasOutput = outputFilePath != null && !outputFilePath.isEmpty();
      if (hasInput) {
        // Only this one file is made visible to the sandbox; the box is wiped by --cleanup.
        Files.copy(Paths.get(inputFilePath), boxDirectory.resolve(BOX_STDIN_FILE));
      }

      List<String> command =
          buildRunCommand(
              commandLine,
              hasInput,
              outputFilePath,
              timeLimitMs,
              memoryLimitKb,
              isCompilation,
              metaFile);
      LOGGER.info("Running under isolate: " + String.join(" ", command));
      execute(command);

      RunResult result = parseMeta(metaFile, memoryLimitKb);
      if (hasOutput) {
        copyOutOfBox(boxDirectory.resolve(BOX_STDOUT_FILE), Paths.get(outputFilePath));
      }
      return result;
    } catch (Exception ex) {
      LOGGER.catching(ex);
      return RunResult.internalError();
    } finally {
      cleanupBox();
      if (metaFile != null) {
        metaFile.delete();
      }
    }
  }

  /**
   * Initializes (or re-initializes) the isolate sandbox box.
   *
   * @return the box directory on the host (isolate prints the box root; the box is its {@code box}
   *     sub-directory, which is the sandbox's working directory)
   * @throws Exception if the box cannot be initialized
   */
  private Path initBox() throws Exception {
    // Start from an empty box even if a previous judger process died before cleaning up.
    cleanupBox();
    ProcessOutput init =
        execute(Arrays.asList(isolateExecutable, "--cg", "-b", String.valueOf(boxId), "--init"));
    if (init.exitCode != 0) {
      throw new IOException("isolate --init failed: " + init.output.trim());
    }
    String boxRoot = "";
    for (String line : init.output.split("\\R")) {
      if (!line.isBlank()) {
        boxRoot = line.trim();
      }
    }
    Path boxDirectory = Paths.get(boxRoot, "box");
    if (!boxRoot.startsWith("/") || !Files.isDirectory(boxDirectory, LinkOption.NOFOLLOW_LINKS)) {
      throw new IOException("Unexpected isolate --init output: " + init.output.trim());
    }
    return boxDirectory;
  }

  /** Cleans up the isolate sandbox box, ignoring any failure. */
  private void cleanupBox() {
    try {
      execute(Arrays.asList(isolateExecutable, "--cg", "-b", String.valueOf(boxId), "--cleanup"));
    } catch (Exception ex) {
      LOGGER.catching(ex);
    }
  }

  /**
   * Builds the {@code isolate --run} command.
   *
   * @param commandLine - the command line to execute
   * @param hasInput - whether the input was copied into the box as {@link #BOX_STDIN_FILE}
   * @param outputFilePath - the file the output is eventually copied to, or {@code null}
   * @param timeLimitMs - the time limit (ms, 0 means no limit)
   * @param memoryLimitKb - the memory limit (KB, 0 means no limit)
   * @param isCompilation - whether this is a compilation or a run of the submission
   * @param metaFile - the file isolate writes its accounting metadata to
   * @return the full command (program + arguments) to execute
   */
  List<String> buildRunCommand(
      String commandLine,
      boolean hasInput,
      String outputFilePath,
      int timeLimitMs,
      int memoryLimitKb,
      boolean isCompilation,
      File metaFile) {
    List<String> command = new ArrayList<>();
    command.add(isolateExecutable);
    command.add("--cg");
    command.add("-b");
    command.add(String.valueOf(boxId));
    command.add("-M");
    command.add(metaFile.getAbsolutePath());
    command.add("--processes=" + maxProcesses);
    // Cap the size of files the sandboxed program may create, so a "disk bomb" (a compiler or a
    // submission emitting a huge file) cannot fill the disk. Mirrors the native runner's
    // RLIMIT_FSIZE. isolate expects the limit in kilobytes.
    if (fileSizeLimitKb > 0) {
      command.add("--fsize=" + fileSizeLimitKb);
    }

    if (timeLimitMs > 0) {
      double timeLimitSec = timeLimitMs / 1000.0;
      command.add("-t");
      command.add(String.valueOf(timeLimitSec));
      // Wall-clock guard a little above the CPU limit, so a sleeping/blocked process is still killed.
      command.add("-w");
      command.add(String.valueOf(timeLimitSec + wallTimeBufferSec));
    }
    if (memoryLimitKb > 0) {
      command.add("--cg-mem=" + memoryLimitKb);
    }

    // Bind the work directory (source + compiled binary) at its same path: read-write while
    // compiling, read-only while running a submission, so a submission cannot leave files behind
    // for later checkpoints. The work directory is derived from the output file, which always lives
    // in it (the program output for a run, or the compile log for a compilation); this is robust
    // regardless of whether the first command-line token is a path (e.g. "java", "python") or the
    // binary itself. The checkpoint directory (inputs AND expected outputs) is never bound.
    String workDirectory = directoryOf(outputFilePath);
    if (workDirectory != null) {
      command.add("--dir=" + workDirectory + (isCompilation ? ":rw" : ""));
    }

    // Redirections are opened by isolate inside the sandbox, relative to the box (the working
    // directory), so they name files in the box rather than host paths.
    if (hasInput) {
      command.add("--stdin=" + BOX_STDIN_FILE);
    }
    if (outputFilePath != null && !outputFilePath.isEmpty()) {
      command.add("--stdout=" + BOX_STDOUT_FILE);
      if (isCompilation) {
        // The compile log keeps the compiler's diagnostics (usually written to stderr).
        command.add("--stderr-to-stdout");
      } else {
        // Keep stderr out of the output compared against the answer; it is wiped with the box.
        command.add("--stderr=" + BOX_STDERR_FILE);
      }
    }

    command.add("--run");
    command.add("--");
    command.addAll(Arrays.asList(commandLine.trim().split("\\s+")));
    return command;
  }

  /**
   * Copies the captured output out of the box once the sandbox has exited. The sandboxed program
   * could have replaced the file with a symbolic link or a FIFO, so only a regular file is copied
   * (opened without following links); anything else counts as empty output. The destination is
   * replaced rather than written through, so a link planted at the destination is never followed.
   *
   * @param boxFile - the output file inside the box
   * @param destination - the file to copy the output to
   * @throws IOException if the output cannot be copied
   */
  private void copyOutOfBox(Path boxFile, Path destination) throws IOException {
    if (Files.isRegularFile(boxFile, LinkOption.NOFOLLOW_LINKS)) {
      try (InputStream inputStream = Files.newInputStream(boxFile, LinkOption.NOFOLLOW_LINKS)) {
        Files.copy(inputStream, destination, StandardCopyOption.REPLACE_EXISTING);
      }
    } else {
      LOGGER.warn("No regular output file in the isolate box; treating the output as empty.");
      Files.deleteIfExists(destination);
      Files.createFile(destination);
    }
  }

  /**
   * Parses an isolate meta file into a {@link RunResult}.
   *
   * <p>isolate writes {@code key:value} lines. The fields used here are {@code status} (TO=time
   * limit, SG=killed by signal, RE=runtime error, XX=internal error; absent means a normal exit),
   * {@code time} (CPU seconds), {@code cg-mem}/{@code max-rss} (peak memory in KB) and
   * {@code exitcode}.
   *
   * @param metaFile - the meta file written by isolate
   * @param memoryLimitKb - the memory limit (KB, 0 means no limit), used to disambiguate OOM kills
   * @return the structured run result
   * @throws Exception if the meta file cannot be read
   */
  private RunResult parseMeta(File metaFile, int memoryLimitKb) throws Exception {
    Map<String, String> meta = new HashMap<>();
    for (String line : Files.readAllLines(metaFile.toPath(), StandardCharsets.UTF_8)) {
      int separator = line.indexOf(':');
      if (separator > 0) {
        meta.put(line.substring(0, separator), line.substring(separator + 1));
      }
    }

    int cpuTimeMs = (int) Math.round(parseDouble(meta.get("time")) * 1000);
    int peakMemoryKb = Math.max(parseInt(meta.get("cg-mem")), parseInt(meta.get("max-rss")));
    int exitCode = parseInt(meta.get("exitcode"));
    Verdict verdict = mapVerdict(meta.get("status"), peakMemoryKb, memoryLimitKb);
    return new RunResult(verdict, cpuTimeMs, peakMemoryKb, exitCode);
  }

  /**
   * Maps an isolate {@code status} to a {@link Verdict}.
   *
   * @param status - the isolate status (may be {@code null} for a normal exit)
   * @param peakMemoryKb - the peak memory used (KB)
   * @param memoryLimitKb - the memory limit (KB, 0 means no limit)
   * @return the verdict
   */
  private Verdict mapVerdict(String status, int peakMemoryKb, int memoryLimitKb) {
    if (status == null || status.isEmpty()) {
      return Verdict.NORMAL;
    }
    switch (status) {
      case "TO":
        return Verdict.TIME_LIMIT_EXCEEDED;
      case "SG":
      case "RE":
        // A signal/runtime failure at or above the memory limit is reported as a memory-limit error.
        if (memoryLimitKb > 0 && peakMemoryKb >= memoryLimitKb) {
          return Verdict.MEMORY_LIMIT_EXCEEDED;
        }
        return Verdict.RUNTIME_ERROR;
      case "XX":
      default:
        return Verdict.INTERNAL_ERROR;
    }
  }

  /**
   * Executes a command and waits for it to finish. isolate's exit code is not interpreted here: for
   * {@code --run} a non-zero exit merely means the sandboxed program failed, which is recorded in
   * the meta file. Standard input is {@code /dev/null}, so a sandboxed process without a redirected
   * input (e.g. a compiler) gets EOF rather than blocking on the judger's stdin.
   *
   * @param command - the command (program + arguments) to execute
   * @return isolate's exit code and its (merged stdout/stderr) output
   * @throws Exception if the process cannot be started or is interrupted
   */
  private ProcessOutput execute(List<String> command) throws Exception {
    ProcessBuilder processBuilder = new ProcessBuilder(command);
    processBuilder.redirectErrorStream(true);
    processBuilder.redirectInput(ProcessBuilder.Redirect.from(new File("/dev/null")));
    Process process = processBuilder.start();
    // Drain the output so the process cannot block on a full pipe buffer. Every stream of the
    // sandboxed program is redirected, so this is only isolate's own (short) diagnostics.
    String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
    return new ProcessOutput(process.waitFor(), output);
  }

  /**
   * Gets the parent directory of an absolute file path.
   *
   * @param filePath - the file path (may be {@code null})
   * @return the parent directory, or {@code null} if it cannot be determined
   */
  private String directoryOf(String filePath) {
    if (filePath == null) {
      return null;
    }
    File parent = new File(filePath).getParentFile();
    return parent == null ? null : parent.getAbsolutePath();
  }

  /**
   * Parses an integer from a (possibly {@code null}/blank) meta value, defaulting to 0.
   *
   * @param value - the value to parse
   * @return the parsed integer, or 0
   */
  private int parseInt(String value) {
    try {
      return value == null ? 0 : Integer.parseInt(value.trim());
    } catch (NumberFormatException ex) {
      return 0;
    }
  }

  /**
   * Parses a double from a (possibly {@code null}/blank) meta value, defaulting to 0.
   *
   * @param value - the value to parse
   * @return the parsed double, or 0
   */
  private double parseDouble(String value) {
    try {
      return value == null ? 0 : Double.parseDouble(value.trim());
    } catch (NumberFormatException ex) {
      return 0;
    }
  }

  /** The exit code and output of an executed process. */
  private static final class ProcessOutput {
    /** The process's exit code. */
    private final int exitCode;

    /** The process's merged standard output and standard error. */
    private final String output;

    /**
     * Constructor.
     *
     * @param exitCode - the process's exit code
     * @param output - the process's merged standard output and standard error
     */
    private ProcessOutput(int exitCode, String output) {
      this.exitCode = exitCode;
      this.output = output;
    }
  }

  /** The path to the isolate executable. */
  @Value("${judger.isolate.executable:isolate}")
  private String isolateExecutable;

  /** The isolate box id used for judging (the judger judges one submission at a time). */
  @Value("${judger.isolate.boxId:0}")
  private int boxId;

  /** The maximum number of processes/threads the sandboxed program may create. */
  @Value("${judger.isolate.maxProcesses:64}")
  private int maxProcesses;

  /** The maximum size (kilobytes) of files the sandboxed program may create (0 means no limit). */
  @Value("${judger.isolate.fileSizeLimitKb:262144}")
  private int fileSizeLimitKb;

  /** Extra wall-clock time (seconds) allowed on top of the CPU time limit. */
  @Value("${judger.isolate.wallTimeBufferSec:1.0}")
  private double wallTimeBufferSec;

  /** The logger. */
  private static final Logger LOGGER = LogManager.getLogger(IsolateRunner.class);
}
