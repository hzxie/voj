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

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.LineIterator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;

/**
 * Compares the user's output against the standard output.
 *
 * @author Haozhe Xie
 */
@Component
public class Comparator {
  /**
   * Gets the comparison result between the user's output and the standard output.
   *
   * @param standardOutputFilePath - the path of the standard output file
   * @param outputFilePath - the path of the user's output file
   * @return whether the user's output and the standard output are the same
   */
  public boolean isOutputTheSame(String standardOutputFilePath, String outputFilePath)
      throws IOException {
    File stdFile = new File(standardOutputFilePath);
    File file = new File(outputFilePath);

    LineIterator stdFileItr = FileUtils.lineIterator(stdFile, "UTF-8");
    LineIterator fileItr = FileUtils.lineIterator(file, "UTF-8");
    boolean isFileOutputTheSame = isFileOutputTheSame(stdFileItr, fileItr);

    LineIterator.closeQuietly(stdFileItr);
    LineIterator.closeQuietly(fileItr);
    return isFileOutputTheSame;
  }

  /**
   * Compares whether the standard output and the user's output are the same.
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

        if (!isLineOutputTheSame(stdLine, line)) {
          return false;
        }
      }
      while (stdFileItr.hasNext()) {
        String line = stdFileItr.nextLine();
        if (!isLineEmpty(line, 0)) {
          return false;
        }
      }
      while (fileItr.hasNext()) {
        String line = fileItr.nextLine();
        if (!isLineEmpty(line, 0)) {
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
   * Compares a line of the standard output against the user's output (trailing spaces ignored).
   *
   * @param stdLine - a line of the standard output
   * @param line - a line of the user's output
   * @return whether the line of the standard output and the user's output are the same
   */
  private boolean isLineOutputTheSame(String stdLine, String line) {
    int i = 0, j = 0;
    for (; i < stdLine.length() && j < line.length(); ++i, ++j) {
      if (stdLine.charAt(i) != line.charAt(j)) {
        if (stdLine.charAt(i) == '\n') {
          if (!isLineEmpty(line, j)) {
            return false;
          }
          return true;
        } else if (line.charAt(j) == '\n') {
          if (!isLineEmpty(stdLine, i)) {
            return false;
          }
          return true;
        }
        return false;
      }
    }
    while (i < stdLine.length()) {
      if (!isLineEmpty(stdLine, i)) {
        return false;
      }
      ++i;
    }
    while (j < line.length()) {
      if (!isLineEmpty(line, j)) {
        return false;
      }
      ++j;
    }
    return true;
  }

  /**
   * Ignores blank lines and spaces at the end of the file.
   *
   * @param line - the content of a line
   * @param startIndex - the index at which to start checking
   * @return whether the line contains only spaces and newline characters
   */
  private boolean isLineEmpty(String line, int startIndex) {
    for (int i = startIndex; i < line.length(); ++i) {
      if (!(line.charAt(i) == ' ' || line.charAt(i) == '\n')) {
        return false;
      }
    }
    return true;
  }

  /** The logger. */
  private static final Logger LOGGER = LogManager.getLogger(Comparator.class);
}
