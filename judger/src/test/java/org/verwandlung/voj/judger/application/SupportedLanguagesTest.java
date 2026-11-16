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
package org.verwandlung.voj.judger.application;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;

import org.verwandlung.voj.judger.mapper.LanguageMapper;
import org.verwandlung.voj.judger.model.Language;

/**
 * Database-free tests for SupportedLanguages.
 *
 * @author Haozhe Xie
 */
public class SupportedLanguagesTest {
  /**
   * Test case: tests the getVersionCommand(String) method. Test data: the compile commands of the
   * languages, with extra whitespace, a path, and none at all. Expected result: the compiler itself
   * (javac, never the java runtime), asked for its version as it understands it.
   */
  @Test
  public void testGetVersionCommand() {
    Assertions.assertEquals(
        List.of("g++", "--version"), SupportedLanguages.getVersionCommand("g++ -o a a.cpp"));
    Assertions.assertEquals(
        List.of("javac", "-version"),
        SupportedLanguages.getVersionCommand("javac -d {filename} {filename}.java"));
    Assertions.assertEquals(
        List.of("kotlinc", "-version"),
        SupportedLanguages.getVersionCommand("kotlinc {filename}.kt -d {filename}.jar"));
    Assertions.assertEquals(
        List.of("go", "version"), SupportedLanguages.getVersionCommand("  go\tbuild x.go"));
    Assertions.assertEquals(
        List.of("/usr/bin/fpc", "-iV"),
        SupportedLanguages.getVersionCommand("/usr/bin/fpc -O2 {filename}.pas"));
    Assertions.assertEquals(
        List.of("C:\\Go\\bin\\GO.EXE", "version"),
        SupportedLanguages.getVersionCommand("C:\\Go\\bin\\GO.EXE build x.go"));
    Assertions.assertEquals(
        List.of("make", "--version"), SupportedLanguages.getVersionCommand("make"));
    Assertions.assertNull(SupportedLanguages.getVersionCommand("   "));
    Assertions.assertNull(SupportedLanguages.getVersionCommand(null));
  }

  /**
   * Test case: tests the getCommandOutput method. Test data: the Java runtime asked for its
   * version (which it prints on its standard error), and a program that does not exist. Expected
   * result: the version, and nothing.
   */
  @Test
  public void testGetCommandOutput() {
    String java = Path.of(System.getProperty("java.home"), "bin", "java").toString();

    String version = SupportedLanguages.getCommandOutput(List.of(java, "-version"), 30000);

    Assertions.assertNotNull(version);
    Assertions.assertTrue(version.contains(System.getProperty("java.version")), version);
    Assertions.assertNull(
        SupportedLanguages.getCommandOutput(List.of("voj-no-such-compiler", "--version"), 30000));
  }

  /**
   * Test case: tests the getCommandOutput method. Test data: a command that prints a lot and then
   * fails, and one that never exits. Expected result: nothing for either; the second is killed at
   * the timeout.
   */
  @Test
  @DisabledOnOs(OS.WINDOWS)
  public void testGetCommandOutputOfAFailingCommand() {
    Assertions.assertNull(
        SupportedLanguages.getCommandOutput(
            List.of("sh", "-c", "head -c 1000000 /dev/zero; head -c 1000000 /dev/zero >&2; exit 1"),
            30000));

    long start = System.nanoTime();
    Assertions.assertNull(
        SupportedLanguages.getCommandOutput(List.of("sh", "-c", "exec sleep 60"), 500));
    Assertions.assertTrue(System.nanoTime() - start < 10_000_000_000L);
  }

  /**
   * Test case: tests the detect method. Test data: a language whose compiler is found, one whose
   * compiler is not, and languages whose compiler is found but whose source file name is empty (as
   * a language of 1.0.2 may have it) or not a plain file name. Expected result: only the first is
   * supported.
   */
  @Test
  public void testDetectLeavesOutLanguagesWithoutAValidSourceFilename() {
    // A command is split at white space.
    Assumptions.assumeFalse(JAVA.contains(" "));
    LanguageMapper languageMapper = mock(LanguageMapper.class);
    when(languageMapper.getAllLanguages())
        .thenReturn(
            List.of(
                language(1, JAVA, "Main.x"),
                language(2, "voj-no-such-compiler", "Main.c"),
                language(3, JAVA, ""),
                language(4, JAVA, "../Main.x")));
    SupportedLanguages supportedLanguages = new SupportedLanguages(languageMapper, Runnable::run);

    Assertions.assertEquals(List.of(), supportedLanguages.getLanguageIds());
    supportedLanguages.detect();

    Assertions.assertEquals(List.of(1), supportedLanguages.getLanguageIds());
  }

  /**
   * Test case: tests the checkForChanges method. Test data: the languages as probed at startup,
   * unchanged, then with a language added, then with its source file name changed to an invalid
   * one. Expected result: the compilers are probed again (in the background) after each change
   * only, and the supported languages follow the changes.
   */
  @Test
  public void testChangedLanguagesAreProbedAgain() {
    // A command is split at white space.
    Assumptions.assumeFalse(JAVA.contains(" "));
    LanguageMapper languageMapper = mock(LanguageMapper.class);
    List<Runnable> probes = new ArrayList<>();
    SupportedLanguages supportedLanguages =
        new SupportedLanguages(
            languageMapper,
            probe -> {
              probes.add(probe);
              probe.run();
            });
    when(languageMapper.getAllLanguages()).thenReturn(List.of(language(1, JAVA, "Main.x")));
    supportedLanguages.detect();

    supportedLanguages.checkForChanges();
    Assertions.assertEquals(0, probes.size());

    when(languageMapper.getAllLanguages())
        .thenReturn(List.of(language(2, JAVA, "Main.y"), language(1, JAVA, "Main.x")));
    supportedLanguages.checkForChanges();
    Assertions.assertEquals(1, probes.size());
    Assertions.assertEquals(List.of(2, 1), supportedLanguages.getLanguageIds());
    supportedLanguages.checkForChanges();
    Assertions.assertEquals(1, probes.size());

    when(languageMapper.getAllLanguages())
        .thenReturn(List.of(language(1, JAVA, "Main.x"), language(2, JAVA, "")));
    supportedLanguages.checkForChanges();
    Assertions.assertEquals(2, probes.size());
    Assertions.assertEquals(List.of(1), supportedLanguages.getLanguageIds());
  }

  /**
   * Test case: tests the checkForChanges method. Test data: the languages change, and their probe
   * has not run yet when the heartbeats look again. Expected result: one probe runs at a time, and
   * the languages are not even read while it runs.
   */
  @Test
  public void testOneProbeRunsAtATime() {
    // A command is split at white space.
    Assumptions.assumeFalse(JAVA.contains(" "));
    LanguageMapper languageMapper = mock(LanguageMapper.class);
    List<Runnable> probes = new ArrayList<>();
    SupportedLanguages supportedLanguages = new SupportedLanguages(languageMapper, probes::add);
    when(languageMapper.getAllLanguages()).thenReturn(List.of(language(1, JAVA, "Main.x")));

    supportedLanguages.checkForChanges();
    supportedLanguages.checkForChanges();

    Assertions.assertEquals(1, probes.size());
    verify(languageMapper, times(1)).getAllLanguages();
    probes.get(0).run();
    Assertions.assertEquals(List.of(1), supportedLanguages.getLanguageIds());
    supportedLanguages.checkForChanges();
    Assertions.assertEquals(1, probes.size());
  }

  /**
   * Builds a language.
   *
   * @param languageId - its unique identifier
   * @param compiler - the program its compile command runs
   * @param sourceFilename - its source file name
   * @return the language
   */
  private static Language language(int languageId, String compiler, String sourceFilename) {
    Language language =
        new Language(
            languageId,
            "text/x-" + languageId,
            "Language " + languageId,
            compiler + " {filename}.x",
            "{filename}.exe");
    language.setSourceFilename(sourceFilename);
    return language;
  }

  /** The Java runtime of the tests, as a compiler that is found: it prints its version. */
  private static final String JAVA =
      Path.of(System.getProperty("java.home"), "bin", "java").toString();
}
