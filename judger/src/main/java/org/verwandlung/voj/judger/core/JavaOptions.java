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

/**
 * The options every run of a submission gives a JVM it starts (Java, Kotlin, ...), so that the JVM
 * sizes itself to the run's limits rather than to the host. Every sandbox (the native ones on Linux
 * and Windows, and isolate) passes them in the environment variable {@value #ENVIRONMENT_VARIABLE},
 * which only the {@code java} launcher reads (JDK 9 and later): no other runtime or compiler is
 * affected, and no language's run command has to change. Options on the run command itself come
 * after them, so a command that sets its own (say {@code -Xmx}) still has the last word. The launcher
 * announces them on standard error ("NOTE: Picked up JDK_JAVA_OPTIONS: ..."), which a run discards.
 *
 * <p>Compilations do not get them: they run trusted compilers under a limit of their own, with their
 * own heap settings (kotlinc's launcher passes -Xmx512M), and a compilation's standard error is its
 * compile log, which the note would clutter.
 *
 * <p>The options (figures measured with OpenJDK 21 and 25 on a host with 24 CPUs and 60 GB of RAM,
 * under a limit of 512 MB, i.e. 256 MB times Java's memory multiplier of 2):
 *
 * <ul>
 *   <li>{@code -XX:MaxRAM=<limit>}: the JVM takes the run's memory limit (the problem's, times the
 *       language's memory multiplier) for the memory of the machine, so the sizes it derives from it
 *       are relative to the limit, the same on any host. Without it, the maximum heap is a quarter
 *       of the host's RAM (15 GB) and the initial heap 1/64 of it (1 GB): a program that allocates
 *       2 GB in arrays of 1 MB it drops at once reached a peak resident set of 560 to 670 MB before
 *       its first collection, and was judged Memory Limit Exceeded (the limit is on the peak
 *       resident set, heap and all). With it, the heap starts at 1/64 of the limit and grows only
 *       as the program's live data needs: the same program peaks at 41 MB.
 *   <li>{@code -Xmx}: the heap may grow to the limit less {@value #NON_HEAP_RESERVE_KB} KB (64
 *       MB), but at least to half of the limit. The rest is left to what the JVM needs besides (its
 *       code, class metadata, compiled code, thread stacks, the collector's own structures): about
 *       40 MB for a small program; one that filled its whole heap peaked 20 to 44 MB above it (at
 *       most 492 MB of 512, 100 MB of 128). A program that needs more heap than that runs out of
 *       it, rather than past its limit.
 *   <li>{@code -XX:+UseSerialGC}: the collector with the least memory and CPU of its own (G1, the
 *       default on a host with 2 CPUs or more, peaked at 110 to 120 MB for the program above), whose
 *       pauses do not matter to a run.
 *   <li>{@code -XX:ActiveProcessorCount=1}: the JVM sizes its compiler threads and the default
 *       parallelism of the program (parallel streams, the common fork-join pool) by the host's CPU
 *       count. A run may have {@code judger.processLimit} (64) threads: on a host with 64 CPUs, a
 *       parallel stream fails to start its 63 workers and the run dies of it, a runtime error on
 *       that host only. With one CPU the JVM keeps to a handful of threads on any host (7 at most
 *       for a parallel stream), and a parallel stream costs the CPU time of a sequential one (0.2 s
 *       rather than 2 s spread over 24 threads), the time the run is charged for.
 *   <li>{@code -XX:+ExitOnOutOfMemoryError}: a program whose heap (or class metadata) runs out exits
 *       at once with {@value #OUT_OF_MEMORY_EXIT_CODE}, which the judger tells apart from other
 *       failures (an uncaught exception exits with 1) and judges Memory Limit Exceeded (see {@link
 *       #isOutOfMemoryExit}), as a C++ program that keeps allocating is: the program needed more
 *       memory than it is allowed. (A thread the JVM cannot start, at the thread limit, is not such
 *       an exit: it stays a runtime error, as in any other language.) The JVM says so on standard
 *       output ("Terminating due to java.lang.OutOfMemoryError: ..."), which is never compared: only
 *       an Accepted run's output is.
 * </ul>
 *
 * @author Haozhe Xie
 */
final class JavaOptions {
  /** The environment variable the options are passed in, read by the {@code java} launcher. */
  static final String ENVIRONMENT_VARIABLE = "JDK_JAVA_OPTIONS";

  /** The part of the memory limit (KB) left to the JVM besides its heap. */
  static final int NON_HEAP_RESERVE_KB = 64 * 1024;

  /** The exit code of a JVM that ran out of memory ({@code -XX:+ExitOnOutOfMemoryError}). */
  static final int OUT_OF_MEMORY_EXIT_CODE = 3;

  /** No instances. */
  private JavaOptions() {}

  /**
   * Gets the options of a JVM that a run of a submission starts.
   *
   * @param memoryLimitKb - the memory limit of the run (KB, 0 means no limit, which leaves the heap
   *     to the JVM's defaults)
   * @return the value of {@value #ENVIRONMENT_VARIABLE}
   */
  static String forRun(int memoryLimitKb) {
    StringBuilder options = new StringBuilder();
    if (memoryLimitKb > 0) {
      int maxHeapKb = Math.max(memoryLimitKb / 2, memoryLimitKb - NON_HEAP_RESERVE_KB);
      options
          .append("-XX:MaxRAM=")
          .append(memoryLimitKb)
          .append("k -Xmx")
          .append(maxHeapKb)
          .append("k ");
    }
    return options
        .append("-XX:+UseSerialGC -XX:ActiveProcessorCount=1 -XX:+ExitOnOutOfMemoryError")
        .toString();
  }

  /**
   * Tells whether a run ended because its JVM ran out of memory: the program is the {@code java}
   * launcher and exited with {@value #OUT_OF_MEMORY_EXIT_CODE}. A program that calls {@code
   * System.exit(3)} itself is taken for one, which gains it nothing (it fails either way).
   *
   * @param commandLine - the command line of the run
   * @param verdict - the verdict of the run
   * @param exitCode - the exit code of the run
   * @return whether the JVM of the run ran out of memory
   */
  static boolean isOutOfMemoryExit(String commandLine, Verdict verdict, int exitCode) {
    if (verdict != Verdict.RUNTIME_ERROR || exitCode != OUT_OF_MEMORY_EXIT_CODE) {
      return false;
    }
    String line = commandLine.trim();
    String program =
        line.startsWith("\"")
            ? line.substring(1, Math.max(1, line.indexOf('"', 1)))
            : line.split("\\s+")[0];
    program = program.substring(Math.max(program.lastIndexOf('/'), program.lastIndexOf('\\')) + 1);
    return program.equals("java") || program.equalsIgnoreCase("java.exe");
  }
}
