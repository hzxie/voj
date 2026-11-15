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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Test class for NativeLibraryLoader.
 *
 * @author Haozhe Xie
 */
public class NativeLibraryLoaderTest {
  /**
   * Test case: tests the loadLibrary method. Test data: the name of a library that is neither on the
   * java.library.path nor on the class path. Expected result: System.loadLibrary fails, the loader
   * falls back to the class-path lookup, fails to locate the resource, and an IOException naming the
   * library is thrown.
   */
  @Test
  public void testLoadLibraryNotFound() {
    String libraryName = "voj-runner-does-not-exist-" + System.nanoTime();
    java.io.IOException ex =
        Assertions.assertThrows(
            java.io.IOException.class, () -> NativeLibraryLoader.loadLibrary(libraryName));
    // The failure names the library instead of surfacing as a NullPointerException.
    Assertions.assertTrue(ex.getMessage().contains(libraryName), ex.getMessage());
    Assertions.assertInstanceOf(UnsatisfiedLinkError.class, ex.getCause());
  }
}
