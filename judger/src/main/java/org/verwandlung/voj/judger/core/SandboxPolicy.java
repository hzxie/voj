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
import java.lang.management.ManagementFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.sun.management.OperatingSystemMXBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import org.verwandlung.voj.judger.model.Language;
import org.verwandlung.voj.judger.model.Problem;

/**
 * Decides what a compilation or a run of a submission gets: its command line, its environment and
 * its limits. Every sandbox backend (see {@link SandboxRunner}) enforces what this builds, and
 * nothing of it is decided anywhere else. What no program gets at all, whatever its limits (the
 * network, core dumps, ...), is the backends' own, alike for both.
 *
 * @author Haozhe Xie
 */
@Component
public class SandboxPolicy implements InitializingBean {
  /**
   * Checks the configured limits as the judger starts: a compilation or run without a limit could
   * hold the judger forever, so a limit that is not positive keeps the judger from starting.
   *
   * @throws IllegalStateException if a configured limit is not positive
   */
  @Override
  public void afterPropertiesSet() {
    requirePositive("judger.compileTimeLimit", compileTimeLimit);
    requirePositive("judger.compileMemoryLimit", compileMemoryLimit);
    requirePositive("judger.processLimit", processLimit);
    requirePositive("judger.compileProcessLimit", compileProcessLimit);
    requirePositive("judger.outputLimit", outputLimitKb);
  }

  /**
   * Checks that a configured limit is positive.
   *
   * @param name - the name of the setting
   * @param value - its value
   * @throws IllegalStateException if it is not
   */
  private static void requirePositive(String name, int value) {
    if (value <= 0) {
      throw new IllegalStateException(name + " must be positive, not " + value);
    }
  }

  /**
   * Builds the compilation of a submission, which runs in the program directory, the only one it
   * may write to, and writes its log (standard output and error) to {@code logFile}.
   *
   * @param language - the language of the submission
   * @param programDirectory - the program directory, which holds the source
   * @param logFile - the file of the compile log
   * @return the compilation
   */
  public SandboxCommand compilation(Language language, String programDirectory, String logFile) {
    Map<String, String> environment = environment(programDirectory, null);
    Limits limits =
        limits(
            true,
            compileTimeLimit,
            scale(compileMemoryLimit, Math.max(language.getMemoryMultiplier(), 1.0)),
            outputLimitKb,
            compileProcessLimit);
    return new SandboxCommand(
        arguments(language.getCompileCommand(), language, programDirectory, environment),
        toList(environment),
        programDirectory,
        null,
        logFile,
        true,
        limits);
  }

  /**
   * Builds a run of a compiled submission on one checkpoint, under the problem's limits scaled by
   * the language's multipliers. On Linux the process limit (judger.processLimit) counts processes
   * and threads alike: the native sandbox holds a run to a single process, so that the limit is on
   * its threads, while isolate cannot tell threads from processes, and lets a run fork within it.
   * A Windows Job counts processes only, and holds the program and its console host.
   *
   * @param language - the language of the submission
   * @param problem - the problem
   * @param programDirectory - the program directory, which the run may only read
   * @param runDirectory - the work directory of the run
   * @param inputFile - the input of the checkpoint
   * @param outputFile - the file the output goes to
   * @return the run
   */
  public SandboxCommand run(
      Language language,
      Problem problem,
      String programDirectory,
      String runDirectory,
      String inputFile,
      String outputFile) {
    int memoryLimit = scale(problem.getMemoryLimit(), language.getMemoryMultiplier());
    Map<String, String> environment =
        environment(runDirectory, JavaOptions.forRun(memoryLimit));
    Limits limits =
        limits(
            false,
            scale(problem.getTimeLimit(), language.getTimeMultiplier()),
            memoryLimit,
            outputLimitKb,
            IS_WINDOWS ? 2 : processLimit);
    return new SandboxCommand(
        arguments(language.getRunCommand(), language, programDirectory, environment),
        toList(environment),
        runDirectory,
        inputFile,
        outputFile,
        false,
        limits);
  }

  /**
   * Decides the limits of a compilation or run from its time, memory, output and process limits:
   *
   * <ul>
   *   <li>the wall-clock limit, which ends a program that sleeps or blocks rather than using CPU
   *       time: {@value #WALL_TIME_FACTOR} times the time limit plus {@value #WALL_TIME_SLACK_MS}
   *       ms, so that a program on a busy host is not stopped short of its CPU time;
   *   <li>the memory backstop: twice the memory limit plus {@value #BACKSTOP_HEADROOM_KB} KB (256
   *       MB), the same on every host, which a run's JVM, sized to its memory limit (see {@link
   *       JavaOptions}), fits in with room to spare. The memory limit is measured on the peak
   *       resident set; the backstop only bounds how far a program gets between two
   *       measurements, and decides whether a single allocation, or a program's static data, is
   *       beyond any limit (see docs/deployment.md). A compilation's headroom is at least 1/32 of
   *       the host's RAM: its compilers' JVMs (javac, kotlinc) size themselves to the host, and
   *       commit far more than they touch;
   *   <li>the stack of a run's main thread: its memory limit, as is usual for a judge, so that a
   *       program may recurse as deep as its memory allows, under every backend alike; a
   *       compilation's {@value #COMPILE_STACK_LIMIT_KB} KB, Linux's usual default, which every
   *       toolchain is built for (and which the C library also gives each thread that does not
   *       choose its own stack, see docs/deployment.md);
   *   <li>the open descriptors of each process: {@value #RUN_FILE_LIMIT} for a run, since an open
   *       pipe or socket pair holds kernel memory that no memory limit sees (Node.js opens about
   *       24, and 6 per worker thread); {@value #COMPILE_FILE_LIMIT} for a compilation, whose
   *       toolchains open many files at once (javac and kotlinc their jars, go build its
   *       packages);
   *   <li>the queued signals and POSIX timers, kernel objects that no memory limit of the native
   *       sandbox sees: {@value #SIGNAL_QUEUE_LIMIT}.
   * </ul>
   *
   * @param compilation - whether the limits are a compilation's
   * @param timeLimitMs - the CPU time limit (ms)
   * @param memoryLimitKb - the memory limit (KB)
   * @param outputLimitKb - the largest file the program may write (KB)
   * @param processLimit - the processes (on Linux, and threads) the program may have at once
   * @return the limits
   * @throws IllegalArgumentException if a limit is not positive
   */
  static Limits limits(
      boolean compilation,
      int timeLimitMs,
      int memoryLimitKb,
      int outputLimitKb,
      int processLimit) {
    long headroomKb =
        compilation ? Math.max(BACKSTOP_HEADROOM_KB, TOTAL_MEMORY_KB / 32) : BACKSTOP_HEADROOM_KB;
    return new Limits(
        timeLimitMs,
        saturate((long) timeLimitMs * WALL_TIME_FACTOR + WALL_TIME_SLACK_MS),
        memoryLimitKb,
        saturate(2L * memoryLimitKb + headroomKb),
        compilation ? COMPILE_STACK_LIMIT_KB : memoryLimitKb,
        outputLimitKb,
        processLimit,
        compilation ? COMPILE_FILE_LIMIT : RUN_FILE_LIMIT,
        SIGNAL_QUEUE_LIMIT);
  }

  /**
   * Caps a figure at the largest int.
   *
   * @param value - the figure
   * @return the figure, at most {@link Integer#MAX_VALUE}
   */
  private static int saturate(long value) {
    return (int) Math.min(Integer.MAX_VALUE, value);
  }

  /**
   * Builds the arguments of a compile or run command: the command is split at white space, and
   * {@code {filename}} in any argument stands for the source file's path without its extension
   * (e.g. {@code .../program/Main} for {@code Main.java}). The program is looked up on the
   * environment's PATH, so that no sandbox searches it (or the work directory) itself.
   *
   * @param command - the compile or run command of the language
   * @param language - the language, which names the source file
   * @param programDirectory - the program directory
   * @param environment - the environment of the program
   * @return the arguments
   */
  static List<String> arguments(
      String command, Language language, String programDirectory, Map<String, String> environment) {
    String sourceFile = language.getSourceFilename();
    int extension = sourceFile.lastIndexOf('.');
    String filename =
        Paths.get(programDirectory, extension > 0 ? sourceFile.substring(0, extension) : sourceFile)
            .toString();
    List<String> arguments = new ArrayList<>();
    for (String argument : command.trim().split("\\s+")) {
      if (!argument.isEmpty()) {
        arguments.add(argument.replace("{filename}", filename));
      }
    }
    if (arguments.isEmpty()) {
      throw new IllegalArgumentException(
          "The command of " + language.getLanguageName() + " is empty");
    }
    arguments.set(0, findExecutable(arguments.get(0), environment.getOrDefault("PATH", "")));
    return arguments;
  }

  /**
   * Finds a program on a search path. A name with a directory in it is taken as it is; on
   * Windows, a name without an extension is looked for as an {@code .exe}.
   *
   * @param program - the program
   * @param searchPath - the directories to look in
   * @return the path of the program, or its name if it was not found (it then fails to start)
   */
  static String findExecutable(String program, String searchPath) {
    if (program.contains("/") || program.contains(File.separator)) {
      return program;
    }
    String name = IS_WINDOWS && !program.contains(".") ? program + ".exe" : program;
    for (String directory : searchPath.split(File.pathSeparator)) {
      if (!directory.isEmpty()) {
        Path candidate = Paths.get(directory, name);
        if (Files.isRegularFile(candidate) && Files.isExecutable(candidate)) {
          return candidate.toAbsolutePath().toString();
        }
      }
    }
    return program;
  }

  /**
   * Builds the environment of a program. The judger's own environment holds secrets (the database
   * password, the API token, ...), so a program gets only the toolchain and locale settings of
   * {@link #INHERITED_VARIABLES} from it (and on Windows the system's settings programs need,
   * {@link #WINDOWS_VARIABLES}); its home, temporary directory and Go caches are in its work
   * directory, the only place it may write to, so that nothing one submission writes is used by
   * another.
   *
   * @param workDirectory - the work directory of the program
   * @param javaOptions - the options of a JVM the program starts ({@code null}: none, for a
   *     compilation)
   * @return the environment
   */
  Map<String, String> environment(String workDirectory, String javaOptions) {
    Map<String, String> environment = new LinkedHashMap<>();
    for (String name : IS_WINDOWS ? WINDOWS_VARIABLES : List.<String>of()) {
      inherit(environment, name);
    }
    for (String name : INHERITED_VARIABLES) {
      inherit(environment, name);
    }
    if (IS_WINDOWS) {
      environment.put("USERNAME", sandboxUsername);
    } else {
      environment.putIfAbsent("PATH", DEFAULT_PATH);
      environment.putIfAbsent("LANG", DEFAULT_LANG);
    }
    String directory = Paths.get(workDirectory).toString();
    for (String name : IS_WINDOWS ? WINDOWS_HOME_VARIABLES : POSIX_HOME_VARIABLES) {
      environment.put(name, directory);
    }
    environment.put("GOCACHE", Paths.get(directory, GO_BUILD_CACHE_DIRECTORY).toString());
    environment.put("GOPATH", Paths.get(directory, GO_PATH_DIRECTORY).toString());
    if (javaOptions != null) {
      environment.put(JavaOptions.ENVIRONMENT_VARIABLE, javaOptions);
    }
    return environment;
  }

  /**
   * Passes a variable of the judger's environment on, if it is set.
   *
   * @param environment - the environment of the program
   * @param name - the name of the variable
   */
  private static void inherit(Map<String, String> environment, String name) {
    String value = System.getenv(name);
    if (value != null && !value.isEmpty()) {
      environment.put(name, value);
    }
  }

  /**
   * Converts an environment to {@code NAME=value} strings.
   *
   * @param environment - the environment
   * @return its variables
   */
  private static List<String> toList(Map<String, String> environment) {
    List<String> variables = new ArrayList<>();
    environment.forEach((name, value) -> variables.add(name + "=" + value));
    return variables;
  }

  /**
   * Scales a limit by a language's multiplier.
   *
   * @param limit - the limit
   * @param multiplier - the multiplier
   * @return the scaled limit
   */
  private static int scale(int limit, double multiplier) {
    return (int) Math.min(Integer.MAX_VALUE, Math.ceil(limit * multiplier));
  }

  /** The wall-clock limit is this many times the time limit, ... */
  static final int WALL_TIME_FACTOR = 2;

  /** ... plus this much (ms). */
  static final int WALL_TIME_SLACK_MS = 1000;

  /** The headroom of the memory backstop over twice the memory limit (KB). */
  static final int BACKSTOP_HEADROOM_KB = 256 * 1024;

  /** The stack of a compilation's main thread (KB). */
  static final int COMPILE_STACK_LIMIT_KB = 8 * 1024;

  /** The descriptors each process of a run may have open at once, ... */
  static final int RUN_FILE_LIMIT = 128;

  /** ... and of a compilation. */
  static final int COMPILE_FILE_LIMIT = 4096;

  /** The signals and POSIX timers a compilation or run may have queued at once. */
  static final int SIGNAL_QUEUE_LIMIT = 1024;

  /** The host's physical memory (KB). */
  private static final long TOTAL_MEMORY_KB =
      ((OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean()).getTotalMemorySize()
          / 1024;

  /** The judger's variables a program gets: the locale and the toolchains' settings. */
  static final List<String> INHERITED_VARIABLES =
      List.of(
          "PATH", "LANG", "LC_ALL", "TZ", "JAVA_HOME", "KOTLIN_HOME", "GOROOT", "GOENV",
          "GOTOOLCHAIN", "GOFLAGS", "RUSTUP_HOME", "RUSTUP_TOOLCHAIN", "CARGO_HOME");

  /** The system's settings Windows programs need (the CRT, Winsock, cmd, ...). */
  static final List<String> WINDOWS_VARIABLES =
      List.of(
          "SystemRoot", "SystemDrive", "windir", "ComSpec", "PATHEXT", "OS", "ProgramData",
          "ProgramFiles", "ProgramFiles(x86)", "ProgramW6432", "CommonProgramFiles",
          "CommonProgramFiles(x86)", "CommonProgramW6432", "ALLUSERSPROFILE", "PUBLIC",
          "NUMBER_OF_PROCESSORS", "PROCESSOR_ARCHITECTURE");

  /** The variables that point a program at its work directory on Linux, ... */
  private static final List<String> POSIX_HOME_VARIABLES = List.of("HOME", "TMPDIR");

  /** ... and on Windows. */
  private static final List<String> WINDOWS_HOME_VARIABLES =
      List.of("HOME", "USERPROFILE", "APPDATA", "LOCALAPPDATA", "TEMP", "TMP");

  /** The Go build cache of a program, in its work directory (seeded by {@link Preprocessor}). */
  static final String GO_BUILD_CACHE_DIRECTORY = ".cache/go-build";

  /** The GOPATH of a program, in its work directory. */
  private static final String GO_PATH_DIRECTORY = "go";

  /** The search path of a program when the judger has none (Linux). */
  private static final String DEFAULT_PATH = "/usr/local/bin:/usr/bin:/bin";

  /** The locale of a program when the judger has none (Linux). */
  private static final String DEFAULT_LANG = "C.UTF-8";

  /** Whether the judger runs on Windows. */
  private static final boolean IS_WINDOWS = System.getProperty("os.name").contains("Windows");

  /** The account submissions run as on Windows, their USERNAME. */
  @Value("${system.username}")
  private String sandboxUsername;

  /**
   * The CPU time limit of a compilation (ms): the Go build driver and the JVM-based Kotlin
   * compiler need far more than a few seconds.
   */
  @Value("${judger.compileTimeLimit:20000}")
  private int compileTimeLimit;

  /**
   * The memory limit of a compilation (KB), scaled by the language's memory multiplier (kotlinc is
   * a JVM with a 512 MB heap of its own): it bounds a "compiler bomb", while leaving room for the
   * Go linker and rustc.
   */
  @Value("${judger.compileMemoryLimit:524288}")
  private int compileMemoryLimit;

  /**
   * The threads a run may have at once on Linux (under isolate, its processes and threads
   * together).
   */
  @Value("${judger.processLimit:64}")
  private int processLimit;

  /**
   * The processes (on Linux, and threads) a compilation may have at once: the Go toolchain runs a
   * compiler per CPU, each with a thread per CPU.
   */
  @Value("${judger.compileProcessLimit:1024}")
  private int compileProcessLimit;

  /** The largest file a compilation or run may write (KB), its output included. */
  @Value("${judger.outputLimit:262144}")
  private int outputLimitKb;
}
