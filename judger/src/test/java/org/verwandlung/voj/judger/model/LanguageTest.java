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
package org.verwandlung.voj.judger.model;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Test class for the Language model.
 *
 * @author Haozhe Xie
 */
public class LanguageTest {
  /**
   * Test case: tests the four-argument constructor. Expected result: the getters return the values
   * passed to the constructor and the identifier defaults to zero.
   */
  @Test
  public void testConstructorWithoutId() {
    Language language = new Language("cpp", "C++", "g++ foo.cpp", "./foo");
    Assertions.assertEquals(0, language.getLanguageId());
    Assertions.assertEquals("cpp", language.getLanguageSlug());
    Assertions.assertEquals("C++", language.getLanguageName());
    Assertions.assertEquals("g++ foo.cpp", language.getCompileCommand());
    Assertions.assertEquals("./foo", language.getRunCommand());
  }

  /**
   * Test case: tests the five-argument constructor. Expected result: the getters return the values
   * passed to the constructor, including the identifier.
   */
  @Test
  public void testConstructorWithId() {
    Language language = new Language(3, "java", "Java", "javac Main.java", "java Main");
    Assertions.assertEquals(3, language.getLanguageId());
    Assertions.assertEquals("java", language.getLanguageSlug());
    Assertions.assertEquals("Java", language.getLanguageName());
    Assertions.assertEquals("javac Main.java", language.getCompileCommand());
    Assertions.assertEquals("java Main", language.getRunCommand());
  }

  /**
   * Test case: tests the default constructor together with the setters. Expected result: the
   * getters return the values set by the setters.
   */
  @Test
  public void testSetters() {
    Language language = new Language();
    language.setLanguageId(5);
    language.setLanguageSlug("py");
    language.setLanguageName("Python");
    language.setCompileCommand("");
    language.setRunCommand("python main.py");
    language.setTimeMultiplier(3.0);
    language.setMemoryMultiplier(2.0);

    Assertions.assertEquals(5, language.getLanguageId());
    Assertions.assertEquals("py", language.getLanguageSlug());
    Assertions.assertEquals("Python", language.getLanguageName());
    Assertions.assertEquals("", language.getCompileCommand());
    Assertions.assertEquals("python main.py", language.getRunCommand());
    Assertions.assertEquals(3.0, language.getTimeMultiplier());
    Assertions.assertEquals(2.0, language.getMemoryMultiplier());
  }

  /** Test case: tests toString. Expected result: a string containing the slug. */
  @Test
  public void testToString() {
    Language language = new Language(3, "java", "Java", "javac Main.java", "java Main");
    Assertions.assertTrue(language.toString().contains("java"));
  }
}
