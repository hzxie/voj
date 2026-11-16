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
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
 *   <li>The work directory is bound into the sandbox at {@link #BOX_WORK_DIRECTORY} via {@code
 *       --dir}, and its host path in the command line is rewritten to that. It is writable while
 *       compiling (the compiler writes the binary there) and read-only while running a submission.
 *       It must belong to isolate's box user (UID first_uid + box id, see
 *       judger.isolate.firstUid), which the judger arranges when it runs as root.
 *   <li>isolate neither searches PATH nor passes on the environment: the program is resolved on the
 *       judger's PATH, and the box gets the locale and toolchain variables the native sandbox
 *       passes on, HOME, and a Go build cache and GOPATH of its own in the work directory.
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
  /** Where the submission's work directory is bound inside the box. */
  static final String BOX_WORK_DIRECTORY = "/work";

  /** The name, inside the box, of the copy of the input file. */
  static final String BOX_STDIN_FILE = "stdin.txt";

  /** The name, inside the box, of the captured standard output. */
  static final String BOX_STDOUT_FILE = "stdout.txt";

  /** The name, inside the box, of the (discarded) standard error of a submission run. */
  static final String BOX_STDERR_FILE = "stderr.txt";

  /**
   * Runs a command inside an isolate sandbox. Not synchronized: the box is used by one run at a
   * time because judging is serialized (see {@code Dispatcher#judgeNextSubmission}), and every
   * judger on a host has a box of its own (judger.isolate.boxId).
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
  public RunResult run(
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
      if (isCompilation) {
        copyCompileFilesIntoBox(boxDirectory);
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
        Path boxOutputFile = boxDirectory.resolve(BOX_STDOUT_FILE);
        if (!isCompilation) {
          result = applyOutputLimit(result, sizeOf(boxOutputFile));
        }
        copyOutOfBox(boxOutputFile, Paths.get(outputFilePath));
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
    // --init re-initializes (empties) a box that a judger which died mid-run left behind, so no
    // --cleanup is needed first; the box is cleaned up after every run instead (see run), which
    // frees its files (up to --fsize per captured stream) and its cgroup right away.
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
    // isolate counts threads too; compilers get a higher cap (see judger.compileProcessLimit).
    command.add("--processes=" + (isCompilation ? compileProcessLimit : processLimit));
    // isolate's syscall filter makes file locks fail (flock with ENOSYS, fcntl locks with EPERM),
    // because a lock on an inode visible in several boxes (e.g. under the read-only /usr) is a side
    // channel between them. The Go build cache cannot do without locks ("go: failed to trim cache:
    // Lock .../trim.txt: function not implemented"), and a compilation runs a trusted compiler, not
    // the submission, so compilations get their own filter flags (judger.isolate.compileSyscalls).
    if (isCompilation && !compileSyscalls.isBlank()) {
      command.add("--syscalls=" + compileSyscalls.trim());
    }
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

    // Bind the work directory (source + compiled binary) at BOX_WORK_DIRECTORY: read-write while
    // compiling, read-only while running a submission, so a submission cannot leave files behind
    // for later checkpoints. The work directory is derived from the output file, which always lives
    // in it (the program output for a run, or the compile log for a compilation); this is robust
    // regardless of whether the first command-line token is a path (e.g. "java", "python") or the
    // binary itself. The checkpoint directory (inputs AND expected outputs) is never bound. It
    // cannot be bound at its own path: isolate mounts the box's private /tmp over every mount point
    // below /tmp (where judger.checkpointDir may be), so the command line's paths are rewritten
    // instead.
    String workDirectory = directoryOf(outputFilePath);
    if (workDirectory != null) {
      command.add(
          "--dir=" + BOX_WORK_DIRECTORY + "=" + workDirectory + (isCompilation ? ":rw" : ""));
    }
    // Toolchains that live outside isolate's default mounts (/bin, /lib, /usr, ...): the
    // alternatives symlinks behind /usr/bin/javac, cc, fpc, ... and /opt (Go, Kotlin).
    for (String directory : extraDirectories.split(",")) {
      if (!directory.isBlank()) {
        command.add("--dir=" + directory.trim());
      }
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

    // isolate starts the program with an empty environment and execve()s it without searching
    // PATH, so a compile/run command such as "g++ ..." or "python3 ..." would fail with ENOENT:
    // resolve the program on the judger's PATH, and give the box a minimal environment (the
    // compilers and runtimes themselves look up their helpers on PATH), like the native sandbox,
    // with the options that fit a run's JVM to its limits (see JavaOptions).
    Map<String, String> environment = boxEnvironment(System.getenv());
    if (!isCompilation) {
      environment.put(JavaOptions.ENVIRONMENT_VARIABLE, JavaOptions.forRun(memoryLimitKb));
    }
    String searchPath = environment.get("PATH");
    for (Map.Entry<String, String> variable : environment.entrySet()) {
      command.add("--env=" + variable.getKey() + "=" + variable.getValue());
    }

    command.add("--run");
    command.add("--");
    List<String> arguments = new ArrayList<>();
    for (String argument : commandLine.trim().split("\\s+")) {
      arguments.add(toBoxPath(argument, workDirectory));
    }
    arguments.set(0, resolveExecutable(arguments.get(0), searchPath));
    command.addAll(arguments);
    return command;
  }

  /**
   * Copies the configured host files (judger.isolate.compileFiles, {@code boxName=hostPath} pairs)
   * into the box before a compilation, for compilers that need a configuration file isolate cannot
   * bind: it binds directories only, and the file's directory, such as /etc for Free Pascal's
   * /etc/fpc.cfg, would expose far more than the file. The box is the compiler's working directory
   * and HOME, where e.g. fpc looks for its configuration ({@code ~/.fpc.cfg}) before /etc. Links are
   * followed on the host (the file is usually reached through /etc/alternatives); a missing file is
   * skipped. The copies are wiped with the box.
   *
   * @param boxDirectory - the box directory on the host
   * @throws IOException if a file cannot be copied
   */
  private void copyCompileFilesIntoBox(Path boxDirectory) throws IOException {
    for (String rule : compileFiles.split(",")) {
      int separator = rule.indexOf('=');
      if (separator <= 0) {
        continue;
      }
      String boxName = rule.substring(0, separator).trim();
      Path hostFile = Paths.get(rule.substring(separator + 1).trim());
      if (boxName.contains("/") || boxName.equals(".") || boxName.equals("..")) {
        LOGGER.warn("Ignoring the compile file rule " + rule + ": not a plain file name.");
        continue;
      }
      if (!Files.isRegularFile(hostFile)) {
        continue;
      }
      try (InputStream inputStream = Files.newInputStream(hostFile)) {
        Files.copy(inputStream, boxDirectory.resolve(boxName));
      }
    }
  }

  /**
   * Builds the environment of the sandboxed program, as the native sandbox does: only the locale
   * and toolchain settings of {@link #INHERITED_ENVIRONMENT_VARIABLES} are taken from the judger,
   * whose own environment holds secrets (database password, API token). The Go toolchain's writable
   * state (GOCACHE, GOPATH) is never inherited: it is placed in the submission's own work directory
   * ({@link Preprocessor#GO_BUILD_CACHE_DIRECTORY}, seeded from judger.goBuildCacheSeed), because a
   * build cache shared between submissions could be poisoned by one of them for every later build.
   *
   * @param judgerEnvironment - the judger's environment
   * @return the variables to pass to the box, in a stable order
   */
  static Map<String, String> boxEnvironment(Map<String, String> judgerEnvironment) {
    Map<String, String> environment = new LinkedHashMap<>();
    for (String name : INHERITED_ENVIRONMENT_VARIABLES) {
      String value = judgerEnvironment.get(name);
      if (value != null && !value.isEmpty()) {
        environment.put(name, value);
      }
    }
    environment.putIfAbsent("PATH", DEFAULT_PATH);
    environment.putIfAbsent("LANG", DEFAULT_LANG);
    environment.put("HOME", BOX_HOME_DIRECTORY);
    environment.put(
        "GOCACHE", BOX_WORK_DIRECTORY + "/" + Preprocessor.GO_BUILD_CACHE_DIRECTORY);
    environment.put("GOPATH", BOX_WORK_DIRECTORY + "/" + GO_PATH_DIRECTORY);
    return environment;
  }

  /**
   * The judger's environment variables the box inherits (see Judger.Core.Runner.cpp). GOCACHE and
   * GOPATH are deliberately absent, see {@link #boxEnvironment}.
   */
  private static final String[] INHERITED_ENVIRONMENT_VARIABLES = {
    "PATH", "LANG", "LC_ALL", "TZ",
    "JAVA_HOME", "KOTLIN_HOME",
    "GOROOT", "GOENV", "GOTOOLCHAIN", "GOFLAGS",
    "RUSTUP_HOME", "RUSTUP_TOOLCHAIN", "CARGO_HOME",
  };

  /** GOPATH, relative to the work directory (GO_PATH_DIRECTORY in Judger.Core.Runner.cpp). */
  private static final String GO_PATH_DIRECTORY = "go";

  /** The box's own directory (isolate's working directory), given to the program as HOME. */
  private static final String BOX_HOME_DIRECTORY = "/box";

  /** The locale given to the box when the judger has none. */
  private static final String DEFAULT_LANG = "C.UTF-8";

  /**
   * Rewrites the work directory's host path, wherever it appears in a command-line argument (e.g.
   * {@code /tmp/voj-1/a.cpp}, {@code -o/tmp/voj-1/a.exe} or {@code -cp /tmp/voj-1}), to its path in
   * the box, {@link #BOX_WORK_DIRECTORY}. The path is matched however its separators are repeated
   * ({@code /tmp//voj-1/a.cpp}): the bound directory comes from {@link #directoryOf}, which
   * collapses them, the command line from the compile and run commands, which do not; a path that
   * was not rewritten names nothing in the box, so every compilation would fail.
   *
   * @param argument - the command-line argument
   * @param workDirectory - the work directory's host path (may be {@code null})
   * @return the argument as seen from inside the box
   */
  static String toBoxPath(String argument, String workDirectory) {
    if (workDirectory == null) {
      return argument;
    }
    StringBuilder pattern = new StringBuilder();
    for (String segment : workDirectory.split("/+")) {
      if (!segment.isEmpty()) {
        pattern.append("/+").append(Pattern.quote(segment));
      }
    }
    // Only the whole directory (also in a path list such as a class path), not e.g. /tmp/voj-1 in
    // /tmp/voj-10.
    pattern.append("(?=[/:]|$)");
    return Pattern.compile(pattern.toString())
        .matcher(argument)
        .replaceAll(Matcher.quoteReplacement(BOX_WORK_DIRECTORY));
  }

  /**
   * Resolves a program name on a search path, as a shell would. A name containing a slash is used
   * as is.
   *
   * @param program - the program name (first token of the command line)
   * @param searchPath - the colon-separated directories to search
   * @return the absolute path of the first executable regular file found, or the name unchanged
   */
  static String resolveExecutable(String program, String searchPath) {
    if (program.contains("/")) {
      return program;
    }
    for (String directory : searchPath.split(":")) {
      if (directory.isEmpty()) {
        continue;
      }
      Path candidate = Paths.get(directory, program);
      if (Files.isRegularFile(candidate) && Files.isExecutable(candidate)) {
        return candidate.toAbsolutePath().toString();
      }
    }
    return program;
  }

  /** The search path given to the box when the judger itself has none. */
  private static final String DEFAULT_PATH = "/usr/local/bin:/usr/bin:/bin";

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
   * {@code exitsig} (the signal), {@code cg-oom-killed} (killed by the out-of-memory killer of the
   * box's cgroup), {@code time} (CPU seconds), {@code cg-mem}/{@code max-rss} (peak memory in KB)
   * and {@code exitcode}.
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
    return new RunResult(mapVerdict(meta, memoryLimitKb), cpuTimeMs, peakMemoryKb, exitCode);
  }

  /**
   * Maps the fields of an isolate meta file to a {@link Verdict}.
   *
   * @param meta - the fields of the meta file
   * @param memoryLimitKb - the memory limit (KB, 0 means no limit)
   * @return the verdict
   */
  static Verdict mapVerdict(Map<String, String> meta, int memoryLimitKb) {
    String status = meta.get("status");
    if (status == null || status.isEmpty()) {
      return Verdict.NORMAL;
    }
    if ("XX".equals(status)) {
      return Verdict.INTERNAL_ERROR;
    }
    // The box's cgroup ran out of memory and its OOM killer stepped in (status SG with SIGKILL, or
    // TO when the program was thrashing): whatever the status says, the memory limit was exceeded.
    if ("1".equals(trim(meta.get("cg-oom-killed")))) {
      return Verdict.MEMORY_LIMIT_EXCEEDED;
    }
    if ("TO".equals(status)) {
      return Verdict.TIME_LIMIT_EXCEEDED;
    }
    if (!"SG".equals(status) && !"RE".equals(status)) {
      return Verdict.INTERNAL_ERROR;
    }
    // The kernel raises SIGXFSZ when a file outgrows --fsize (the output size limit).
    if ("SG".equals(status) && parseInt(meta.get("exitsig")) == SIGXFSZ) {
      return Verdict.OUTPUT_LIMIT_EXCEEDED;
    }
    // E.g. a program that died of a failed allocation at the memory limit.
    int peakMemoryKb = Math.max(parseInt(meta.get("cg-mem")), parseInt(meta.get("max-rss")));
    if (memoryLimitKb > 0 && peakMemoryKb >= memoryLimitKb) {
      return Verdict.MEMORY_LIMIT_EXCEEDED;
    }
    return Verdict.RUNTIME_ERROR;
  }

  /**
   * Judges a run whose output reached the file size limit (--fsize) Output Limit Exceeded, as the
   * native runner does. A program that ignores (or blocks) SIGXFSZ is not killed by it: its writes
   * past the limit fail with EFBIG, and it may then exit normally or with an error, with its output
   * cut off at the limit. That is no Wrong Answer or Runtime Error but an output that was too large.
   *
   * @param result - the result of the run, as isolate reports it
   * @param outputSize - the size (bytes) of the captured output
   * @return the result, Output Limit Exceeded if the output reached the limit
   */
  RunResult applyOutputLimit(RunResult result, long outputSize) {
    Verdict verdict = result.getVerdict();
    boolean exitedOnItsOwn = verdict == Verdict.NORMAL || verdict == Verdict.RUNTIME_ERROR;
    if (!exitedOnItsOwn || fileSizeLimitKb <= 0 || outputSize < fileSizeLimitKb * 1024L) {
      return result;
    }
    return new RunResult(
        Verdict.OUTPUT_LIMIT_EXCEEDED,
        result.getCpuTimeMs(),
        result.getPeakMemoryKb(),
        result.getExitCode());
  }

  /**
   * Gets the size of a file in the box without following a link the program may have planted.
   *
   * @param file - the file
   * @return its size (bytes), 0 if it is missing or not a regular file
   */
  private static long sizeOf(Path file) {
    try {
      BasicFileAttributes attributes =
          Files.readAttributes(file, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
      return attributes.isRegularFile() ? attributes.size() : 0;
    } catch (IOException ex) {
      return 0;
    }
  }

  /** The signal raised when a file outgrows the file size limit (SIGXFSZ on Linux). */
  private static final int SIGXFSZ = 25;

  /**
   * Trims a (possibly {@code null}) meta value.
   *
   * @param value - the value
   * @return the trimmed value, or {@code null}
   */
  private static String trim(String value) {
    return value == null ? null : value.trim();
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
  private static int parseInt(String value) {
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

  /**
   * The maximum number of processes and threads a run of the submission may have at once (isolate
   * counts both).
   */
  @Value("${judger.processLimit:64}")
  private int processLimit;

  /**
   * Extra directory rules (isolate {@code --dir} syntax, comma-separated) bound read-only into the
   * box, for toolchains outside isolate's default mounts.
   */
  @Value("${judger.isolate.extraDirs:/etc/alternatives:maybe,/opt:maybe}")
  private String extraDirectories = "/etc/alternatives:maybe,/opt:maybe";

  /** The maximum number of processes/threads a compilation may create. */
  @Value("${judger.compileProcessLimit:1024}")
  private int compileProcessLimit;

  /**
   * isolate's syscall filter flags ({@code --syscalls}) for a compilation; blank to use isolate's
   * configured syscall_flags. The default, 27, keeps every restriction but the one on file locks
   * (flag 4), which the Go build cache needs.
   */
  @Value("${judger.isolate.compileSyscalls:27}")
  private String compileSyscalls = "27";

  /**
   * Host files copied into the box before a compilation ({@code boxName=hostPath}, comma-separated),
   * for compiler configuration files isolate cannot bind.
   */
  @Value("${judger.isolate.compileFiles:.fpc.cfg=/etc/fpc.cfg}")
  private String compileFiles = ".fpc.cfg=/etc/fpc.cfg";

  /** The maximum size (kilobytes) of files the sandboxed program may create (0 means no limit). */
  @Value("${judger.isolate.fileSizeLimitKb:262144}")
  private int fileSizeLimitKb;

  /** Extra wall-clock time (seconds) allowed on top of the CPU time limit. */
  @Value("${judger.isolate.wallTimeBufferSec:1.0}")
  private double wallTimeBufferSec;

  /** The logger. */
  private static final Logger LOGGER = LogManager.getLogger(IsolateRunner.class);
}
