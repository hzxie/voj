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

import org.apache.commons.io.LineIterator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;
import org.verwandlung.voj.judger.util.FileSystemUtils;

import java.io.Closeable;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;

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
   */
  public boolean isOutputTheSame(String standardOutputFilePath, String outputFilePath)
      throws IOException {
    // ISO-8859-1 maps every byte to one char, so non-ASCII (or invalid UTF-8) bytes are compared
    // as-is instead of being decoded, and possibly collapsed into U+FFFD. The user's output lives in
    // the sandbox-owned work directory, so it is opened without following links.
    LineIterator stdFileItr =
        new LineIterator(
            new InputStreamReader(
                new FileInputStream(standardOutputFilePath), StandardCharsets.ISO_8859_1));
    LineIterator fileItr;
    try {
      fileItr =
          new LineIterator(
              new InputStreamReader(
                  FileSystemUtils.newSandboxFileInputStream(Paths.get(outputFilePath)),
                  StandardCharsets.ISO_8859_1));
    } catch (IOException ex) {
      LineIterator.closeQuietly(stdFileItr);
      throw ex;
    }
    try {
      return isFileOutputTheSame(stdFileItr, fileItr);
    } finally {
      LineIterator.closeQuietly(stdFileItr);
      LineIterator.closeQuietly(fileItr);
    }
  }

  /**
   * Gets the exact comparison result between the user's output and the standard output: the bytes
   * must be identical, except that CRLF is treated as LF and a single trailing newline is optional.
   *
   * @param standardOutputFilePath - the path of the standard output file
   * @param outputFilePath - the path of the user's output file
   * @return whether the user's output and the standard output are exactly the same
   */
  public boolean isOutputExactlyTheSame(String standardOutputFilePath, String outputFilePath)
      throws IOException {
    try (NewlineNormalizingReader stdReader =
            new NewlineNormalizingReader(new FileInputStream(standardOutputFilePath));
        NewlineNormalizingReader reader =
            new NewlineNormalizingReader(
                FileSystemUtils.newSandboxFileInputStream(Paths.get(outputFilePath)))) {
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
  }

  /**
   * Compares whether the standard output and the user's output are the same (leniently).
   *
   * @param stdFileItr - the iterator over the standard output file
   * @param fileItr - the iterator over the user's output file
   * @return whether the standard output and the user's output are the same
   */
  private boolean isFileOutputTheSame(LineIterator stdFileItr, LineIterator fileItr) {
    try {
      while (stdFileItr.hasNext() && fileItr.hasNext()) {
        String stdLine = stdFileItr.nextLine();
        String line = fileItr.nextLine();

        if (!stripTrailingWhitespace(stdLine).equals(stripTrailingWhitespace(line))) {
          return false;
        }
      }
      // Whatever remains of the longer output may only consist of blank lines.
      while (stdFileItr.hasNext()) {
        if (!isLineBlank(stdFileItr.nextLine())) {
          return false;
        }
      }
      while (fileItr.hasNext()) {
        if (!isLineBlank(fileItr.nextLine())) {
          return false;
        }
      }
    } catch (OutOfMemoryError ex) {
      LOGGER.catching(ex);
      return false;
    }
    return true;
  }

  /**
   * Removes the trailing spaces and tabs of a line.
   *
   * @param line - the content of a line (without its line terminator)
   * @return the line without trailing spaces and tabs
   */
  private String stripTrailingWhitespace(String line) {
    int end = line.length();
    while (end > 0 && isWhitespace(line.charAt(end - 1))) {
      --end;
    }
    return line.substring(0, end);
  }

  /**
   * Checks whether a line contains only spaces and tabs.
   *
   * @param line - the content of a line (without its line terminator)
   * @return whether the line is blank
   */
  private boolean isLineBlank(String line) {
    return stripTrailingWhitespace(line).isEmpty();
  }

  /**
   * Checks whether a character is ignorable trailing whitespace.
   *
   * @param c - the character
   * @return whether it is a space or a tab
   */
  private boolean isWhitespace(char c) {
    return c == ' ' || c == '\t';
  }

  /** A buffered byte reader that turns every CRLF into LF. */
  private static final class NewlineNormalizingReader implements Closeable {
    /**
     * Constructor.
     *
     * @param inputStream - the underlying stream (closed by {@link #close})
     */
    private NewlineNormalizingReader(InputStream inputStream) {
      this.inputStream = inputStream;
    }

    /**
     * Reads the next byte.
     *
     * @return the next byte (0-255), or -1 at the end of the stream
     * @throws IOException if the stream cannot be read
     */
    private int read() throws IOException {
      int current = readRaw();
      if (current == '\r') {
        int next = readRaw();
        if (next == '\n') {
          return '\n';
        }
        // Not a CRLF: hand the byte after the CR back on the next call.
        if (next != -1) {
          --position;
        }
      }
      return current;
    }

    /**
     * Reads the next raw byte.
     *
     * @return the next byte (0-255), or -1 at the end of the stream
     * @throws IOException if the stream cannot be read
     */
    private int readRaw() throws IOException {
      if (position == limit) {
        // Keep the last byte, so that read() can step back over it after a refill.
        if (limit > 0) {
          buffer[0] = buffer[limit - 1];
          position = 1;
          limit = 1;
        }
        int count = inputStream.read(buffer, limit, buffer.length - limit);
        if (count <= 0) {
          return -1;
        }
        limit += count;
      }
      return buffer[position++] & 0xFF;
    }

    @Override
    public void close() throws IOException {
      inputStream.close();
    }

    /** The underlying stream. */
    private final InputStream inputStream;

    /** The read buffer. */
    private final byte[] buffer = new byte[8192];

    /** The index of the next byte to return from the buffer. */
    private int position;

    /** The number of valid bytes in the buffer. */
    private int limit;
  }

  /** The logger. */
  private static final Logger LOGGER = LogManager.getLogger(Comparator.class);
}
