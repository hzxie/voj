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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * The {@link SandboxRunner} that runs programs under <a
 * href="https://github.com/ioi/isolate">isolate</a> (namespaces and a cgroup per box): the
 * recommended backend for Linux in production, as root. On top of what the native sandbox does, a
 * program sees only isolate's system directories, {@code judger.isolate.extraDirs} and the
 * directory of its judging, has no network, no other processes and no IPC of the host, and its
 * memory and processes are accounted for by its cgroup.
 *
 * <p>The directory of the judging (the work directory's parent, which holds the program) is bound
 * read-only at {@value #BOX_JUDGING_DIRECTORY}, the work directory read-write below it, and every
 * host path in the arguments and the environment is rewritten to its path in the box (isolate
 * mounts a private {@code /tmp} over anything below the host's). Standard input and output never go
 * through a bound directory: the input is copied into isolate's own box directory and the output
 * copied out of it after the run, so the checkpoint directory is never bound.
 *
 * @author Haozhe Xie
 */
@Component
public class IsolateRunner implements SandboxRunner {
  /** Where the directory of the judging is bound inside the box. */
  static final String BOX_JUDGING_DIRECTORY = "/judging";

  /** The copy of the input file, in the box. */
  static final String BOX_STDIN_FILE = "/box/stdin.txt";

  /** The captured standard output, in the box. */
  static final String BOX_STDOUT_FILE = "/box/stdout.txt";

  /** The (discarded) standard error of a run, in the box. */
  static final String BOX_STDERR_FILE = "/box/stderr.txt";

  /**
   * Runs a program in an isolate box. Not synchronized: judging is serialized (see {@code
   * Dispatcher}), and every judger on a host has a box of its own (judger.isolate.boxId).
   *
   * @param command - what to run, and how
   * @return what isolate measured
   * @throws IOException if isolate failed
   */
  @Override
  public SandboxResult run(SandboxCommand command) throws IOException {
    File metaFile = null;
    try {
      Path boxDirectory = initBox();
      metaFile = File.createTempFile("voj-isolate-", ".meta");
      if (command.inputFile() != null) {
        Files.copy(Paths.get(command.inputFile()), inBox(boxDirectory, BOX_STDIN_FILE));
      }
      if (command.compilation()) {
        copyCompileFiles(Paths.get(command.workDirectory()));
      }
      List<String> isolateCommand = buildRunCommand(command, metaFile);
      LOGGER.info("Running under isolate: " + String.join(" ", isolateCommand));
      execute(isolateCommand);

      SandboxResult result = parseMeta(metaFile);
      if (command.outputFile() != null) {
        copyOutOfBox(inBox(boxDirectory, BOX_STDOUT_FILE), Paths.get(command.outputFile()));
      }
      return result;
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
      throw new IOException("Interrupted while running isolate", ex);
    } finally {
      cleanupBox();
      if (metaFile != null) {
        metaFile.delete();
      }
    }
  }

  /**
   * Initializes (or re-initializes, if a judger that died left it behind) the box.
   *
   * @return the box directory on the host, the program's {@code /box}
   * @throws IOException if the box cannot be initialized
   * @throws InterruptedException if interrupted
   */
  private Path initBox() throws IOException, InterruptedException {
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

  /** Cleans up the box, which frees its files and its cgroup at once; a failure is logged. */
  private void cleanupBox() {
    try {
      execute(Arrays.asList(isolateExecutable, "--cg", "-b", String.valueOf(boxId), "--cleanup"));
    } catch (Exception ex) {
      LOGGER.catching(ex);
    }
  }

  /**
   * Gets the host path of a file of the box.
   *
   * @param boxDirectory - the box directory on the host
   * @param boxFile - the file's path in the box ({@code /box/...})
   * @return its path on the host
   */
  private static Path inBox(Path boxDirectory, String boxFile) {
    return boxDirectory.resolve(Paths.get(boxFile).getFileName());
  }

  /**
   * Builds the {@code isolate --run} command.
   *
   * @param command - what to run, and how
   * @param metaFile - the file isolate writes its measurements to
   * @return the isolate command
   */
  List<String> buildRunCommand(SandboxCommand command, File metaFile) {
    Limits limits = command.limits();
    Path workDirectory = Paths.get(command.workDirectory()).toAbsolutePath().normalize();
    String judgingDirectory = workDirectory.getParent().toString();
    String boxWorkDirectory = BOX_JUDGING_DIRECTORY + "/" + workDirectory.getFileName();

    List<String> isolateCommand = new ArrayList<>();
    isolateCommand.add(isolateExecutable);
    isolateCommand.add("--cg");
    isolateCommand.add("-b");
    isolateCommand.add(String.valueOf(boxId));
    isolateCommand.add("-M");
    isolateCommand.add(metaFile.getAbsolutePath());
    isolateCommand.add("--processes=" + limits.processLimit());
    // isolate's filter makes file locks fail, as a lock on an inode several boxes see is a side
    // channel between them; the Go build cache cannot do without them, and a compiler is trusted.
    if (command.compilation() && !compileSyscalls.isBlank()) {
      isolateCommand.add("--syscalls=" + compileSyscalls.trim());
    }
    isolateCommand.add("--fsize=" + limits.outputLimitKb());
    isolateCommand.add("--time=" + limits.timeLimitMs() / 1000.0);
    isolateCommand.add("--wall-time=" + limits.wallTimeLimitMs() / 1000.0);
    isolateCommand.add("--cg-mem=" + limits.memoryLimitKb());
    isolateCommand.add("--dir=" + BOX_JUDGING_DIRECTORY + "=" + judgingDirectory);
    isolateCommand.add("--dir=" + boxWorkDirectory + "=" + workDirectory + ":rw");
    for (String directory : extraDirectories.split(",")) {
      if (!directory.isBlank()) {
        isolateCommand.add("--dir=" + directory.trim());
      }
    }
    isolateCommand.add("--chdir=" + boxWorkDirectory);
    if (command.inputFile() != null) {
      isolateCommand.add("--stdin=" + BOX_STDIN_FILE);
    }
    if (command.outputFile() != null) {
      isolateCommand.add("--stdout=" + BOX_STDOUT_FILE);
      isolateCommand.add(
          command.compilation() ? "--stderr-to-stdout" : "--stderr=" + BOX_STDERR_FILE);
    }
    for (String variable : command.environment()) {
      isolateCommand.add("--env=" + toBoxPath(variable, judgingDirectory));
    }
    isolateCommand.add("--run");
    isolateCommand.add("--");
    for (String argument : command.arguments()) {
      isolateCommand.add(toBoxPath(argument, judgingDirectory));
    }
    return isolateCommand;
  }

  /**
   * Copies the configured host files (judger.isolate.compileFiles, {@code name=hostPath} pairs)
   * into the work directory of a compilation, for compilers that read a configuration file from
   * there or HOME (fpc's {@code .fpc.cfg}) that isolate cannot bind: it binds directories only, and
   * the file's directory (such as {@code /etc}) would expose far more. A missing file is skipped.
   *
   * @param workDirectory - the work directory of the compilation
   * @throws IOException if a file cannot be copied
   */
  private void copyCompileFiles(Path workDirectory) throws IOException {
    for (String rule : compileFiles.split(",")) {
      int separator = rule.indexOf('=');
      if (separator <= 0) {
        continue;
      }
      String name = rule.substring(0, separator).trim();
      Path hostFile = Paths.get(rule.substring(separator + 1).trim());
      if (name.contains("/") || name.equals(".") || name.equals("..")) {
        LOGGER.warn("Ignoring the compile file rule " + rule + ": not a plain file name.");
        continue;
      }
      if (Files.isRegularFile(hostFile)) {
        try (InputStream inputStream = Files.newInputStream(hostFile)) {
          Files.copy(inputStream, workDirectory.resolve(name));
        }
      }
    }
  }

  /**
   * Rewrites the host path of the directory of the judging, wherever it appears in an argument or
   * a variable (e.g. {@code -o/tmp/voj-1/program/Main} or {@code HOME=/tmp/voj-1/run}), to its path
   * in the box, {@value #BOX_JUDGING_DIRECTORY}, however its separators are repeated.
   *
   * @param argument - the argument or variable
   * @param hostDirectory - the host path of the directory of the judging
   * @return the argument as seen in the box
   */
  static String toBoxPath(String argument, String hostDirectory) {
    StringBuilder pattern = new StringBuilder();
    for (String segment : hostDirectory.split("/+")) {
      if (!segment.isEmpty()) {
        pattern.append("/+").append(Pattern.quote(segment));
      }
    }
    // Only the whole directory (also in a path list), not e.g. /tmp/voj-1 in /tmp/voj-10.
    pattern.append("(?=[/:]|$)");
    return Pattern.compile(pattern.toString())
        .matcher(argument)
        .replaceAll(Matcher.quoteReplacement(BOX_JUDGING_DIRECTORY));
  }

  /**
   * Copies the captured output out of the box once the program is gone. Only a regular file is
   * copied (the program may have put a link in its place); anything else is an empty output.
   *
   * @param boxFile - the output file in the box
   * @param destination - the output file of the judger
   * @throws IOException if the output cannot be copied
   */
  private void copyOutOfBox(Path boxFile, Path destination) throws IOException {
    if (Files.isRegularFile(boxFile, LinkOption.NOFOLLOW_LINKS)) {
      try (InputStream inputStream = Files.newInputStream(boxFile, LinkOption.NOFOLLOW_LINKS)) {
        Files.copy(inputStream, destination, StandardCopyOption.REPLACE_EXISTING);
      }
    } else {
      Files.deleteIfExists(destination);
      Files.createFile(destination);
    }
  }

  /**
   * Reads isolate's measurements: {@code time} (CPU seconds of the box's cgroup), {@code cg-mem}
   * and {@code max-rss} (KB), {@code exitcode} or {@code exitsig}, and why it stopped the program:
   * {@code cg-oom-killed} (the memory limit), {@code status:TO} (the time limit) or SIGXFSZ (the
   * file size limit, i.e. the output limit). {@code status:XX} is a failure of isolate.
   *
   * @param metaFile - the file isolate wrote
   * @return the measurements
   * @throws IOException if isolate failed
   */
  SandboxResult parseMeta(File metaFile) throws IOException {
    Map<String, String> meta = new HashMap<>();
    for (String line : Files.readAllLines(metaFile.toPath(), StandardCharsets.UTF_8)) {
      int separator = line.indexOf(':');
      if (separator > 0) {
        meta.put(line.substring(0, separator), line.substring(separator + 1).trim());
      }
    }
    String status = meta.getOrDefault("status", "");
    if ("XX".equals(status)) {
      throw new IOException("isolate failed: " + meta.get("message"));
    }
    int signal = parseInt(meta.get("exitsig"));
    SandboxResult.Stop stoppedFor = SandboxResult.Stop.NONE;
    if ("1".equals(meta.get("cg-oom-killed"))) {
      stoppedFor = SandboxResult.Stop.MEMORY;
    } else if ("TO".equals(status)) {
      stoppedFor = SandboxResult.Stop.TIME;
    } else if (signal == SIGXFSZ) {
      stoppedFor = SandboxResult.Stop.OUTPUT;
    }
    return new SandboxResult(
        (int) Math.round(parseDouble(meta.get("time")) * 1000),
        Math.max(parseInt(meta.get("cg-mem")), parseInt(meta.get("max-rss"))),
        signal > 0 ? 128 + signal : parseInt(meta.get("exitcode")),
        stoppedFor);
  }

  /** The signal of a file that outgrows the file size limit. */
  private static final int SIGXFSZ = 25;

  /**
   * Executes a command and waits for it, with {@code /dev/null} as its standard input. isolate's
   * exit code is not interpreted here: for {@code --run}, the meta file tells how the program
   * ended.
   *
   * @param command - the command
   * @return its exit code and its output (isolate's own diagnostics, which are short)
   * @throws IOException if it cannot be started
   * @throws InterruptedException if interrupted
   */
  private ProcessOutput execute(List<String> command) throws IOException, InterruptedException {
    Process process =
        new ProcessBuilder(command)
            .redirectErrorStream(true)
            .redirectInput(ProcessBuilder.Redirect.from(new File("/dev/null")))
            .start();
    String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
    return new ProcessOutput(process.waitFor(), output);
  }

  /**
   * Parses an integer of the meta file.
   *
   * @param value - the value ({@code null} if absent)
   * @return the integer, 0 if absent or malformed
   */
  private static int parseInt(String value) {
    try {
      return value == null ? 0 : Integer.parseInt(value.trim());
    } catch (NumberFormatException ex) {
      return 0;
    }
  }

  /**
   * Parses a number of the meta file.
   *
   * @param value - the value ({@code null} if absent)
   * @return the number, 0 if absent or malformed
   */
  private static double parseDouble(String value) {
    try {
      return value == null ? 0 : Double.parseDouble(value.trim());
    } catch (NumberFormatException ex) {
      return 0;
    }
  }

  /**
   * The exit code and output of an executed process.
   *
   * @param exitCode - the exit code
   * @param output - standard output and error
   */
  private record ProcessOutput(int exitCode, String output) {}

  /** The isolate executable. */
  @Value("${judger.isolate.executable:isolate}")
  private String isolateExecutable;

  /** The box of this judger: every judger on a host needs one of its own. */
  @Value("${judger.isolate.boxId:0}")
  private int boxId;

  /**
   * More directories bound read-only into the box (isolate {@code --dir} rules, comma-separated),
   * for toolchains outside isolate's default mounts.
   */
  @Value("${judger.isolate.extraDirs:/etc/alternatives:maybe,/opt:maybe}")
  private String extraDirectories = "/etc/alternatives:maybe,/opt:maybe";

  /**
   * isolate's filter flags ({@code --syscalls}) for a compilation, blank for isolate's own: 27
   * keeps every restriction but the one on file locks (flag 4).
   */
  @Value("${judger.isolate.compileSyscalls:27}")
  private String compileSyscalls = "27";

  /** Host files copied into a compilation's work directory ({@code name=hostPath}, ...). */
  @Value("${judger.isolate.compileFiles:.fpc.cfg=/etc/fpc.cfg}")
  private String compileFiles = ".fpc.cfg=/etc/fpc.cfg";

  /** The logger. */
  private static final Logger LOGGER = LogManager.getLogger(IsolateRunner.class);
}
