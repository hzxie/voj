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

import java.io.Closeable;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;

import org.springframework.stereotype.Component;

import org.verwandlung.voj.judger.util.FileSystemUtils;

/**
 * Compares the user's output against the standard output.
 *
 * <p>Two comparisons are available:
 *
 * <ul>
 *   <li><b>Lenient</b> ({@link #isOutputTheSame}): trailing whitespace (spaces and tabs) at the end
 *       of each line and trailing blank lines at the end of the output are ignored; LF, CRLF and CR
 *       line endings are all accepted.
 *   <li><b>Exact</b> ({@link #isOutputExactlyTheSame}): the outputs must be byte-for-byte identical,
 *       except that CRLF line endings are treated as LF and a single trailing newline is optional.
 * </ul>
 *
 * <p>{@link #getComparisonResult} combines them according to a checkpoint's {@code exactlyMatch}
 * flag, and reports a Presentation Error when an exact match is required and only the lenient
 * comparison succeeds.
 *
 * <p>Both comparisons stream the two files side by side, byte by byte, and never hold a line in
 * memory: an output may be hundreds of megabytes on a single line, far more than the judger's
 * heap. Bytes are compared as they are, so non-ASCII (or invalid UTF-8) output is never decoded.
 *
 * @author Haozhe Xie
 */
@Component
public class Comparator {
  /** The judge result slug of an accepted output. */
  public static final String ACCEPTED = "AC";

  /** The judge result slug of an output that only differs in whitespace / blank lines. */
  public static final String PRESENTATION_ERROR = "PE";

  /** The judge result slug of a wrong output. */
  public static final String WRONG_ANSWER = "WA";

  /**
   * Compares the user's output against the standard output.
   *
   * @param standardOutputFilePath - the path of the standard output file
   * @param outputFilePath - the path of the user's output file
   * @param exactlyMatch - whether the checkpoint requires an exact match
   * @return {@link #ACCEPTED}, {@link #PRESENTATION_ERROR} (only when {@code exactlyMatch} is set
   *     and the outputs differ only in trailing whitespace / blank lines) or {@link #WRONG_ANSWER}
   * @throws IOException if either file cannot be read
   */
  public String getComparisonResult(
      String standardOutputFilePath, String outputFilePath, boolean exactlyMatch)
      throws IOException {
    if (exactlyMatch && isOutputExactlyTheSame(standardOutputFilePath, outputFilePath)) {
      return ACCEPTED;
    }
    if (!isOutputTheSame(standardOutputFilePath, outputFilePath)) {
      return WRONG_ANSWER;
    }
    return exactlyMatch ? PRESENTATION_ERROR : ACCEPTED;
  }

  /**
   * Gets the lenient comparison result between the user's output and the standard output: trailing
   * spaces/tabs on each line and trailing blank lines are ignored.
   *
   * @param standardOutputFilePath - the path of the standard output file
   * @param outputFilePath - the path of the user's output file
   * @return whether the user's output and the standard output are the same
   * @throws IOException if either file cannot be read
   */
  public boolean isOutputTheSame(String standardOutputFilePath, String outputFilePath)
      throws IOException {
    try (OutputReader stdReader = openStandardOutput(standardOutputFilePath, true);
        OutputReader reader = openOutput(outputFilePath, true)) {
      return isOutputTheSame(stdReader, reader);
    }
  }

  /**
   * Gets the exact comparison result between the user's output and the standard output: the bytes
   * must be identical, except that CRLF is treated as LF and a single trailing newline is optional.
   *
   * @param standardOutputFilePath - the path of the standard output file
   * @param outputFilePath - the path of the user's output file
   * @return whether the user's output and the standard output are exactly the same
   * @throws IOException if either file cannot be read
   */
  public boolean isOutputExactlyTheSame(String standardOutputFilePath, String outputFilePath)
      throws IOException {
    try (OutputReader stdReader = openStandardOutput(standardOutputFilePath, false);
        OutputReader reader = openOutput(outputFilePath, false)) {
      return isOutputExactlyTheSame(stdReader, reader);
    }
  }

  /**
   * Compares two outputs leniently (see {@link #isOutputTheSame(String, String)}).
   *
   * <p>Two lines are equal without their trailing whitespace exactly when, from the first byte at
   * which they differ, the rest of both lines is whitespace. So the outputs are compared byte by
   * byte, and where they differ both sides skip their spaces and tabs: the lines are still equal if
   * both have reached the end of the line then. Once one output has ended, the rest of the other
   * may only hold blank lines.
   *
   * @param stdReader - the standard output, read with every line ending as LF
   * @param reader - the user's output, read with every line ending as LF
   * @return whether the outputs are the same
   * @throws IOException if either output cannot be read
   */
  private static boolean isOutputTheSame(OutputReader stdReader, OutputReader reader)
      throws IOException {
    while (true) {
      int stdByte = stdReader.read();
      int currentByte = reader.read();
      if (stdByte == currentByte) {
        if (stdByte == -1) {
          return true;
        }
        continue;
      }
      while (isWhitespace(stdByte)) {
        stdByte = stdReader.read();
      }
      while (isWhitespace(currentByte)) {
        currentByte = reader.read();
      }
      if (!isEndOfLine(stdByte) || !isEndOfLine(currentByte)) {
        return false;
      }
      if (stdByte != currentByte) {
        // One output has ended: the rest of the other may only be blank lines.
        return isBlank(stdByte == -1 ? reader : stdReader);
      }
      if (stdByte == -1) {
        return true;
      }
    }
  }

  /**
   * Compares two outputs exactly (see {@link #isOutputExactlyTheSame(String, String)}).
   *
   * @param stdReader - the standard output, read with CRLF as LF
   * @param reader - the user's output, read with CRLF as LF
   * @return whether the outputs are exactly the same
   * @throws IOException if either output cannot be read
   */
  private static boolean isOutputExactlyTheSame(OutputReader stdReader, OutputReader reader)
      throws IOException {
    int lastByte = -1;
    while (true) {
      int stdByte = stdReader.read();
      int currentByte = reader.read();
      if (stdByte != currentByte) {
        // Equal after dropping one optional trailing newline from each side: one output ended and
        // the other has exactly one "\n" left, which does not follow another "\n".
        if (stdByte == -1) {
          return currentByte == '\n' && reader.read() == -1 && lastByte != '\n';
        }
        if (currentByte == -1) {
          return stdByte == '\n' && stdReader.read() == -1 && lastByte != '\n';
        }
        return false;
      }
      if (stdByte == -1) {
        return true;
      }
      lastByte = stdByte;
    }
  }

  /**
   * Checks whether the rest of an output only consists of spaces, tabs and line endings.
   *
   * @param reader - the output
   * @return whether the rest of the output is blank
   * @throws IOException if the output cannot be read
   */
  private static boolean isBlank(OutputReader reader) throws IOException {
    int current;
    while ((current = reader.read()) != -1) {
      if (!isWhitespace(current) && current != '\n') {
        return false;
      }
    }
    return true;
  }

  /**
   * Checks whether a byte is ignorable trailing whitespace.
   *
   * @param current - the byte, or -1 at the end of the output
   * @return whether it is a space or a tab
   */
  private static boolean isWhitespace(int current) {
    return current == ' ' || current == '\t';
  }

  /**
   * Checks whether a byte ends a line.
   *
   * @param current - the byte, or -1 at the end of the output
   * @return whether it is a line ending or the end of the output
   */
  private static boolean isEndOfLine(int current) {
    return current == '\n' || current == -1;
  }

  /**
   * Opens the standard output, which the judger wrote itself.
   *
   * @param path - the path of the standard output file
   * @param lenient - whether a lone CR ends a line too
   * @return the reader
   * @throws IOException if the file cannot be opened
   */
  private OutputReader openStandardOutput(String path, boolean lenient) throws IOException {
    return new OutputReader(new FileInputStream(path), lenient);
  }

  /**
   * Opens the user's output. It lives in the sandbox-owned work directory, so it is opened without
   * following links.
   *
   * @param path - the path of the user's output file
   * @param lenient - whether a lone CR ends a line too
   * @return the reader
   * @throws IOException if the file cannot be opened or is not a plain file
   */
  private OutputReader openOutput(String path, boolean lenient) throws IOException {
    return new OutputReader(FileSystemUtils.newSandboxFileInputStream(Paths.get(path)), lenient);
  }

  /** A buffered byte reader that reads every CRLF as LF, and when lenient a lone CR as well. */
  private static final class OutputReader implements Closeable {
    /**
     * Constructor.
     *
     * @param inputStream - the underlying stream (closed by {@link #close})
     * @param lenient - whether a lone CR is read as LF too
     */
    private OutputReader(InputStream inputStream, boolean lenient) {
      this.inputStream = inputStream;
      this.lenient = lenient;
    }

    /**
     * Reads the next byte, with the line endings normalized.
     *
     * @return the next byte (0-255), or -1 at the end of the stream
     * @throws IOException if the stream cannot be read
     */
    private int read() throws IOException {
      int current = readRaw();
      if (current != '\r') {
        return current;
      }
      int next = readRaw();
      if (next == '\n') {
        return '\n';
      }
      pushedBack = next;
      return lenient ? '\n' : '\r';
    }

    /**
     * Reads the next raw byte.
     *
     * @return the next byte (0-255), or -1 at the end of the stream
     * @throws IOException if the stream cannot be read
     */
    private int readRaw() throws IOException {
      if (pushedBack != NONE) {
        int current = pushedBack;
        pushedBack = NONE;
        return current;
      }
      if (position == limit) {
        int count = inputStream.read(buffer);
        if (count <= 0) {
          return -1;
        }
        position = 0;
        limit = count;
      }
      return buffer[position++] & 0xFF;
    }

    @Override
    public void close() throws IOException {
      inputStream.close();
    }

    /** The value of {@link #pushedBack} when no byte has been pushed back. */
    private static final int NONE = -2;

    /** The underlying stream. */
    private final InputStream inputStream;

    /** Whether a lone CR is read as LF. */
    private final boolean lenient;

    /** The read buffer. */
    private final byte[] buffer = new byte[65536];

    /** The index of the next byte to return from the buffer. */
    private int position;

    /** The number of valid bytes in the buffer. */
    private int limit;

    /** The byte read after a lone CR, returned by the next read, or {@link #NONE}. */
    private int pushedBack = NONE;
  }
}
