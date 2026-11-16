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
package org.verwandlung.voj.judger.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.HashSet;
import java.util.Set;

/**
 * Contains helper methods for loading native libraries, particularly JNI.
 *
 * @author gkubisa
 */
public class NativeLibraryLoader {
  /** Utility classes should not have a public constructor. */
  private NativeLibraryLoader() {}

  /**
   * Loads a native shared library. It tries the standard System.loadLibrary method first and if it
   * fails, it looks for the library in the current class path. It will handle libraries packed
   * within jar files, too. A library is loaded once, however many classes ask for it: one taken
   * from the class path would be extracted, and loaded, once more every time otherwise.
   *
   * @param libraryName - name of the library to load
   * @throws IOException if the library cannot be extracted from a jar file into a temporary file
   */
  public static synchronized void loadLibrary(String libraryName) throws IOException {
    if (LOADED_LIBRARIES.contains(libraryName)) {
      return;
    }
    load(libraryName);
    LOADED_LIBRARIES.add(libraryName);
  }

  /**
   * Loads a native shared library, from the java.library.path or the class path.
   *
   * @param libraryName - name of the library to load
   * @throws IOException if the library cannot be extracted from a jar file into a temporary file
   */
  private static void load(String libraryName) throws IOException {
    try {
      System.loadLibrary(libraryName);
    } catch (UnsatisfiedLinkError e) {
      String fileName = System.mapLibraryName(libraryName);
      try (InputStream inputStream =
          NativeLibraryLoader.class.getClassLoader().getResourceAsStream(fileName)) {
        if (inputStream == null) {
          throw new IOException(
              String.format(
                  "Native library %s is neither on java.library.path nor bundled as %s on the"
                      + " class path",
                  libraryName, fileName),
              e);
        }
        int dotPosition = fileName.lastIndexOf('.');
        File file =
            File.createTempFile(
                fileName.substring(0, dotPosition), fileName.substring(dotPosition));
        file.deleteOnExit();
        // Copy until end of stream: available() only estimates what can be read without blocking,
        // so looping on it could stop early and load a truncated library.
        Files.copy(inputStream, file.toPath(), StandardCopyOption.REPLACE_EXISTING);
        System.load(file.getAbsolutePath());
      }
    }
  }

  /** The names of the libraries loaded so far. */
  private static final Set<String> LOADED_LIBRARIES = new HashSet<>();
}
