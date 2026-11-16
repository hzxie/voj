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
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;

/**
 * How a compilation or run ended. {@link #NORMAL} only means that the program exited with 0 within
 * its limits: whether a run is Accepted is up to the comparison of its output.
 *
 * @author Haozhe Xie
 */
public enum Verdict {
  /** The program exited with 0 within its limits. */
  NORMAL(null),

  /** The program used more than its time limit. */
  TIME_LIMIT_EXCEEDED("TLE"),

  /** The program used more than its memory limit. */
  MEMORY_LIMIT_EXCEEDED("MLE"),

  /** The program's output reached the output limit. */
  OUTPUT_LIMIT_EXCEEDED("OLE"),

  /** The program failed (a non-zero exit code, or a signal). */
  RUNTIME_ERROR("RE"),

  /** The sandbox failed to run the program: no verdict on the program. */
  INTERNAL_ERROR("SE");

  /**
   * Creates a verdict.
   *
   * @param slug - the judge result it stands for
   */
  Verdict(String slug) {
    this.slug = slug;
  }

  /**
   * Gets the judge result this verdict stands for.
   *
   * @return the slug of the judge result ({@code null} for {@link #NORMAL}, which is up to the
   *     comparison of the output)
   */
  public String getSlug() {
    return slug;
  }

  /**
   * Judges how a program ended, the same way whatever sandbox ran it: a limit the sandbox stopped
   * the program for; else a limit its final figures exceed (the sandboxes measure as they go, so a
   * program may exit just past one before it is stopped), an output that reached the output limit
   * included; else its exit code. A run whose JVM ran out of heap (see {@link
   * JavaOptions#isOutOfMemoryExit}) used more memory than it may, as one stopped at its limit.
   *
   * @param command - what the sandbox ran
   * @param result - what the sandbox measured
   * @return the verdict
   */
  public static Verdict of(SandboxCommand command, SandboxResult result) {
    Limits limits = command.limits();
    switch (result.stoppedFor()) {
      case TIME:
        return TIME_LIMIT_EXCEEDED;
      case MEMORY:
        return MEMORY_LIMIT_EXCEEDED;
      case OUTPUT:
        return OUTPUT_LIMIT_EXCEEDED;
      default:
        break;
    }
    if (limits.timeLimitMs() > 0 && result.cpuTimeMs() > limits.timeLimitMs()) {
      return TIME_LIMIT_EXCEEDED;
    }
    if (limits.memoryLimitKb() > 0 && result.peakMemoryKb() > limits.memoryLimitKb()) {
      return MEMORY_LIMIT_EXCEEDED;
    }
    if (sizeOf(command.outputFile()) >= limits.outputLimitKb() * 1024L) {
      return OUTPUT_LIMIT_EXCEEDED;
    }
    if (result.exitCode() == 0) {
      return NORMAL;
    }
    if (!command.compilation()
        && JavaOptions.isOutOfMemoryExit(result.exitCode(), command.outputFile())) {
      return MEMORY_LIMIT_EXCEEDED;
    }
    return RUNTIME_ERROR;
  }

  /**
   * Gets the size of the output file, created by the sandbox for the judger.
   *
   * @param outputFile - the output file ({@code null}: none)
   * @return its size (bytes), 0 for none
   */
  private static long sizeOf(String outputFile) {
    if (outputFile == null) {
      return 0;
    }
    try {
      return Files.readAttributes(
              Paths.get(outputFile), BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS)
          .size();
    } catch (IOException ex) {
      return 0;
    }
  }

  /** The slug of the judge result this verdict stands for. */
  private final String slug;
}
