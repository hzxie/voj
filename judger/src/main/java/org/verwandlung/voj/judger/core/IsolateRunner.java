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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
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
 *   <li>Directories are bound into the sandbox at their <em>same absolute path</em> via {@code
 *       --dir}, so the absolute command line built by {@link ProgramRunner} works unchanged inside
 *       the box. The work directory is bound read-write; the input file's directory read-only. The
 *       expected-output (checkpoint) directory is deliberately <em>not</em> bound, so submitted code
 *       cannot read the answers.
 * </ul>
 *
 * @author Haozhe Xie
 */
@Component
public class IsolateRunner implements SandboxRunner {
  /**
   * Runs a command inside an isolate sandbox.
   *
   * @param commandLine - the command line to execute (absolute paths)
   * @param inputFilePath - the file to redirect to standard input, or {@code null} for none
   * @param outputFilePath - the file to capture standard output/error into, or {@code null} for none
   * @param timeLimitMs - the time limit in milliseconds (0 means no limit)
   * @param memoryLimitKb - the memory limit in kilobytes (0 means no limit)
   * @return the structured run result; never {@code null}
   */
  @Override
  public synchronized RunResult run(
      String commandLine,
      String inputFilePath,
      String outputFilePath,
      int timeLimitMs,
      int memoryLimitKb) {
    File metaFile = null;
    try {
      initBox();
      metaFile = File.createTempFile("voj-isolate-", ".meta");

      List<String> command =
          buildRunCommand(
              commandLine, inputFilePath, outputFilePath, timeLimitMs, memoryLimitKb, metaFile);
      LOGGER.info("Running under isolate: " + String.join(" ", command));
      execute(command);

      return parseMeta(metaFile, memoryLimitKb);
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
   * @throws Exception if the box cannot be initialized
   */
  private void initBox() throws Exception {
    execute(Arrays.asList(isolateExecutable, "--cg", "-b", String.valueOf(boxId), "--init"));
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
   * @param inputFilePath - the stdin file, or {@code null}
   * @param outputFilePath - the stdout/stderr file, or {@code null}
   * @param timeLimitMs - the time limit (ms, 0 means no limit)
   * @param memoryLimitKb - the memory limit (KB, 0 means no limit)
   * @param metaFile - the file isolate writes its accounting metadata to
   * @return the full command (program + arguments) to execute
   */
  private List<String> buildRunCommand(
      String commandLine,
      String inputFilePath,
      String outputFilePath,
      int timeLimitMs,
      int memoryLimitKb,
      File metaFile) {
    List<String> command = new ArrayList<>();
    command.add(isolateExecutable);
    command.add("--cg");
    command.add("-b");
    command.add(String.valueOf(boxId));
    command.add("-M");
    command.add(metaFile.getAbsolutePath());
    command.add("--processes=" + maxProcesses);

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

    // Bind the work directory (compiled binary + program output) read-write at its same path, and
    // the input file's directory read-only. The expected-output directory is intentionally NOT bound.
    // The work directory is derived from the output file, which always lives in it (the program
    // output for a run, or the compile log for a compilation); this is robust regardless of whether
    // the first command-line token is a path (e.g. "java", "python") or the binary itself.
    String workDirectory = directoryOf(outputFilePath);
    if (workDirectory != null) {
      command.add("--dir=" + workDirectory + ":rw");
    }
    if (inputFilePath != null && !inputFilePath.isEmpty()) {
      String inputDirectory = directoryOf(inputFilePath);
      if (inputDirectory != null) {
        command.add("--dir=" + inputDirectory);
      }
      command.add("--stdin=" + inputFilePath);
    }
    if (outputFilePath != null && !outputFilePath.isEmpty()) {
      command.add("--stdout=" + outputFilePath);
      command.add("--stderr=" + outputFilePath);
    }

    command.add("--run");
    command.add("--");
    command.addAll(Arrays.asList(commandLine.trim().split("\\s+")));
    return command;
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
   * Executes a command and waits for it to finish. isolate's exit code is intentionally ignored: a
   * non-zero exit merely means the sandboxed program failed, which is recorded in the meta file.
   *
   * @param command - the command (program + arguments) to execute
   * @throws Exception if the process cannot be started or is interrupted
   */
  private void execute(List<String> command) throws Exception {
    ProcessBuilder processBuilder = new ProcessBuilder(command);
    processBuilder.redirectErrorStream(true);
    Process process = processBuilder.start();
    // Drain the output so the process cannot block on a full pipe buffer.
    process.getInputStream().readAllBytes();
    process.waitFor();
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

  /** The path to the isolate executable. */
  @Value("${judger.isolate.executable:isolate}")
  private String isolateExecutable;

  /** The isolate box id used for judging (the judger judges one submission at a time). */
  @Value("${judger.isolate.boxId:0}")
  private int boxId;

  /** The maximum number of processes/threads the sandboxed program may create. */
  @Value("${judger.isolate.maxProcesses:64}")
  private int maxProcesses;

  /** Extra wall-clock time (seconds) allowed on top of the CPU time limit. */
  @Value("${judger.isolate.wallTimeBufferSec:1.0}")
  private double wallTimeBufferSec;

  /** The logger. */
  private static final Logger LOGGER = LogManager.getLogger(IsolateRunner.class);
}
