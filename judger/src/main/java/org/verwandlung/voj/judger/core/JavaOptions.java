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

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;

/**
 * The options every run of a submission gives a JVM it starts (Java, Kotlin, ...), so that the JVM
 * sizes itself to the run's limits rather than to the host. Every sandbox (the native ones on Linux
 * and Windows, and isolate) passes them in the environment variable {@value #ENVIRONMENT_VARIABLE},
 * which only the {@code java} launcher reads (JDK 9 and later): no other runtime or compiler is
 * affected, and no language's run command has to change. Options on the run command itself come
 * after them, and the last of an option given twice wins, so a command can set its own {@code -Xmx}
 * (or {@code -XX:ActiveProcessorCount}, ...) instead. A command that selects another collector
 * ({@code -XX:+UseG1GC}: "Multiple garbage collectors selected"), or an initial heap ({@code -Xms})
 * larger than the {@code -Xmx} here without an {@code -Xmx} of its own ("Initial heap size set to
 * a larger value than the maximum heap size"), conflicts with them instead, and the JVM refuses to
 * start: the deployment guide tells administrators to keep such options off the run commands. The
 * launcher announces them on standard error ("NOTE: Picked up JDK_JAVA_OPTIONS: ..."), which a run
 * discards.
 *
 * <p>Compilations do not get them: they run trusted compilers under a limit of their own, with their
 * own heap settings (kotlinc's launcher passes -Xmx512M), and a compilation's standard error is its
 * compile log, which the note would clutter.
 *
 * <p>The options (figures measured with OpenJDK 21 and 25 on a host with 24 CPUs and 60 GB of RAM,
 * under a limit of 512 MB, i.e. 256 MB times Java's memory multiplier of 2, unless they say
 * otherwise):
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
 *   <li>{@code -Xmx}: the heap may grow to the limit less a reserve of 1/8 of it, but at least
 *       {@value #MIN_NON_HEAP_RESERVE_KB} KB (48 MB), and to at least half of the limit (and {@value
 *       #MIN_MAX_HEAP_KB} KB, see there). The reserve is left to what the JVM needs besides (its
 *       code, class metadata, compiled code, thread stacks, the collector's own structures): about
 *       40 MB for a small program. A program that filled its whole heap, until it ran out, peaked
 *       at most 26 to 36 MB above it: 84 MB of 96, 116 of 128, 177 of 192, 243 of 256, 362 of 384,
 *       481 of 512, 706 of 768 and 923 of 1024. A program that needs more heap than that runs out
 *       of it, rather than past its limit. (Under a limit below 96 MB, where half of it is the
 *       heap, the JVM may pass the limit before its heap runs out: at 64 MB, that program peaked at
 *       68 MB, Memory Limit Exceeded all the same.) A fixed reserve of 64 MB left a 128 MB limit
 *       only 64 MB of heap; one of 40 MB left a 128 MB limit 5 MB to spare, and 256 MB 8 MB.
 *   <li>{@code -XX:+UseSerialGC}: the collector with the least memory and CPU of its own (G1, the
 *       default on a host with 2 CPUs or more, peaked at 110 to 120 MB for the program above), whose
 *       pauses do not matter to a run.
 *   <li>{@code -XX:ActiveProcessorCount=1}: the JVM sizes its compiler threads and the default
 *       parallelism of the program (parallel streams, the common fork-join pool) by the host's CPU
 *       count. A run may have {@code judger.processLimit} (64) threads: on a host with 64 CPUs, a
 *       parallel stream fails to start its 63 workers and the run dies of it, a runtime error on
 *       that host only. With one CPU the JVM keeps to a handful of threads on any host (14 in all,
 *       its own included), and a parallel stream does not cost much more CPU time than a sequential
 *       one (0.2 s rather than 2 s spread over 24 threads), the time the run is charged for.
 *   <li>{@code -Djava.util.concurrent.ForkJoinPool.common.parallelism=}{@value
 *       #COMMON_POOL_PARALLELISM}: with one CPU, the common fork-join pool would have a parallelism
 *       of 1, and CompletableFuture's async methods without an executor then start a new thread
 *       for every task (OpenJDK 21): 100 tasks that each sleep 20 ms took 114 threads, past a run's
 *       64, and the run died of it; 100 that compute, 72 threads and 1.7 s of CPU time rather than
 *       0.2 s. With 2, they run in the common pool, as a parallel stream does: 16 threads in all
 *       for either, and for a parallel stream (OpenJDK 21 and 25).
 *   <li>{@code -XX:+ExitOnOutOfMemoryError}: a program whose heap (or class metadata) runs out, or
 *       that asks for an array larger than the JVM allows, writes "{@value #OUT_OF_MEMORY_REPORT}..."
 *       on standard output and exits at once with {@value #OUT_OF_MEMORY_EXIT_CODE}, which the
 *       judger tells apart from any other failure (see {@link #isOutOfMemoryExit}) and judges Memory
 *       Limit Exceeded, reported at no less than the limit, as a C++ program that keeps allocating,
 *       or asks for more than its limit at once, is: the program needed more memory than it is
 *       allowed. (A thread the JVM cannot start, at the thread limit, is not such an exit: it stays
 *       a runtime error, as in any other language.) The program cannot catch the OutOfMemoryError
 *       then, which a judged program rarely does: an uncaught one would exit with 1, a runtime error
 *       the judger could not tell from any other, with the program's peak memory telling nothing
 *       either (see {@link #isOutOfMemoryExit}). The output is never compared: only an Accepted
 *       run's output is.
 * </ul>
 *
 * @author Haozhe Xie
 */
final class JavaOptions {
  /** The environment variable the options are passed in, read by the {@code java} launcher. */
  static final String ENVIRONMENT_VARIABLE = "JDK_JAVA_OPTIONS";

  /** The part of the memory limit left to the JVM besides its heap: 1/8 of it, ... */
  static final int NON_HEAP_RESERVE_DIVISOR = 8;

  /** ... but at least this much (KB). */
  static final int MIN_NON_HEAP_RESERVE_KB = 48 * 1024;

  /**
   * The least maximum heap (KB): HotSpot refuses to start with one below 2 MB ("Too small maximum
   * heap"). It only matters under a limit below 32 MB, which no JVM fits in (it needs some 40 MB
   * of its own), so that the run is stopped at its memory limit (Memory Limit Exceeded) rather
   * than failing to start (a Runtime Error).
   */
  static final int MIN_MAX_HEAP_KB = 16 * 1024;

  /** The parallelism of the common fork-join pool (parallel streams, CompletableFuture). */
  static final int COMMON_POOL_PARALLELISM = 2;

  /** The exit code of a JVM that ran out of memory ({@code -XX:+ExitOnOutOfMemoryError}). */
  static final int OUT_OF_MEMORY_EXIT_CODE = 3;

  /**
   * What a JVM that ran out of memory writes on standard output before it exits ({@code
   * -XX:+ExitOnOutOfMemoryError}), followed by the kind of memory ("Java heap space", "Metaspace",
   * "Requested array size exceeds VM limit", ...).
   */
  static final String OUT_OF_MEMORY_REPORT = "Terminating due to java.lang.OutOfMemoryError: ";

  /** How much of the end of a run's output is read to find the JVM's report (bytes). */
  private static final int REPORT_TAIL_BYTES = 4096;

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
      int reserveKb =
          Math.max(memoryLimitKb / NON_HEAP_RESERVE_DIVISOR, MIN_NON_HEAP_RESERVE_KB);
      int maxHeapKb =
          Math.max(MIN_MAX_HEAP_KB, Math.max(memoryLimitKb / 2, memoryLimitKb - reserveKb));
      options
          .append("-XX:MaxRAM=")
          .append(memoryLimitKb)
          .append("k -Xmx")
          .append(maxHeapKb)
          .append("k ");
    }
    return options
        .append("-XX:+UseSerialGC -XX:ActiveProcessorCount=1")
        .append(" -Djava.util.concurrent.ForkJoinPool.common.parallelism=")
        .append(COMMON_POOL_PARALLELISM)
        .append(" -XX:+ExitOnOutOfMemoryError")
        .toString();
  }

  /**
   * Tells whether a run ended because its JVM ran out of memory: it exited with {@value
   * #OUT_OF_MEMORY_EXIT_CODE}, and the last line of its standard output holds the JVM's report
   * ({@value #OUT_OF_MEMORY_REPORT}...), which the JVM writes just before it exits. That is what the
   * JVM itself says, whatever started it (the {@code java} launcher by any path, {@code env}, a
   * launcher script that execs it) and on any system, while any other program exiting with 3 is not
   * taken for it. The report follows whatever the program had written, on the same line if that
   * did not end with a line break; nothing follows it. A program that writes the report and exits
   * with 3 itself is taken for one, which gains it nothing (it fails either way).
   *
   * <p>The run's peak memory does not tell it: the JVM refuses an allocation that does not fit in
   * its heap before touching any of it, so a program that asks for one array larger than the heap
   * runs out at a peak of some 40 MB, and one that holds a few large arrays, which the JVM puts in
   * the old generation, runs out with part of the young one never touched (at 70 MB of a 128 MB
   * limit with 32 MB arrays). Nor does the command: a Kotlin or Scala launcher script, {@code
   * /usr/bin/env java} or {@code JAVA.EXE} would be missed, and a program of another language that
   * happens to be named {@code java} taken for one.
   *
   * @param verdict - the verdict of the run
   * @param exitCode - the exit code of the run
   * @param outputFilePath - the file that holds the run's standard output ({@code null} for none)
   * @return whether the JVM of the run ran out of memory
   */
  static boolean isOutOfMemoryExit(Verdict verdict, int exitCode, String outputFilePath) {
    if (verdict != Verdict.RUNTIME_ERROR
        || exitCode != OUT_OF_MEMORY_EXIT_CODE
        || outputFilePath == null
        || outputFilePath.isEmpty()) {
      return false;
    }
    String output = readTail(Paths.get(outputFilePath));
    int end = output.length();
    while (end > 0 && (output.charAt(end - 1) == '\n' || output.charAt(end - 1) == '\r')) {
      --end;
    }
    String lastLine = output.substring(output.lastIndexOf('\n', end - 1) + 1, end);
    return lastLine.contains(OUT_OF_MEMORY_REPORT);
  }

  /**
   * Reads the end of a run's output file, without following a link the program may have planted
   * in its place.
   *
   * @param file - the output file
   * @return its last {@value #REPORT_TAIL_BYTES} bytes (as ISO-8859-1, so that any byte reads as
   *     one character), or an empty string if it is not a regular file or cannot be read
   */
  private static String readTail(Path file) {
    try {
      BasicFileAttributes attributes =
          Files.readAttributes(file, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
      if (!attributes.isRegularFile()) {
        return "";
      }
      try (SeekableByteChannel channel =
          Files.newByteChannel(file, StandardOpenOption.READ, LinkOption.NOFOLLOW_LINKS)) {
        long start = Math.max(0, channel.size() - REPORT_TAIL_BYTES);
        ByteBuffer buffer = ByteBuffer.allocate((int) (channel.size() - start));
        channel.position(start);
        while (buffer.hasRemaining() && channel.read(buffer) > 0) {
          // Keep reading until the buffer is full or the file ends.
        }
        return new String(buffer.array(), 0, buffer.position(), StandardCharsets.ISO_8859_1);
      }
    } catch (IOException ex) {
      return "";
    }
  }
}
