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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import org.verwandlung.voj.judger.model.Language;

/**
 * Database-free unit tests for the compiler.
 *
 * @author Haozhe Xie
 */
public class CompilerUnitTest {
  /**
   * Test case: tests the getCompileMemoryLimit(Language) method. Test data: languages with memory
   * multipliers 1 (C++), 2 (Kotlin), 0.5 and a missing language. Expected result: the base limit is
   * scaled by multipliers above 1 only, so native compilers keep the compiler-bomb cap.
   */
  @Test
  public void testGetCompileMemoryLimitScalesWithTheMemoryMultiplier() {
    Compiler compiler = new Compiler();
    ReflectionTestUtils.setField(compiler, "compileMemoryLimit", 524288);

    Assertions.assertEquals(524288, compiler.getCompileMemoryLimit(language(1.0)));
    Assertions.assertEquals(1048576, compiler.getCompileMemoryLimit(language(2.0)));
    Assertions.assertEquals(524288, compiler.getCompileMemoryLimit(language(0.5)));
    Assertions.assertEquals(524288, compiler.getCompileMemoryLimit(null));
  }

  /**
   * Test case: tests the getCompileMemoryLimit(Language) method. Test data: no compile memory limit
   * (0). Expected result: still no limit, whatever the multiplier.
   */
  @Test
  public void testGetCompileMemoryLimitKeepsNoLimit() {
    Compiler compiler = new Compiler();
    ReflectionTestUtils.setField(compiler, "compileMemoryLimit", 0);

    Assertions.assertEquals(0, compiler.getCompileMemoryLimit(language(2.0)));
  }

  /**
   * Creates a language with the given memory multiplier.
   *
   * @param memoryMultiplier - the memory multiplier
   * @return the language
   */
  private Language language(double memoryMultiplier) {
    Language language = new Language("text/x-test", "Test", "cc {filename}.c", "{filename}.exe");
    language.setMemoryMultiplier(memoryMultiplier);
    return language;
  }
}
