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
package org.verwandlung.voj.judger;

import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;

/**
 * Database-free tests for VojJudgerApplication.
 *
 * @author Haozhe Xie
 */
public class VojJudgerApplicationTest {
  /**
   * Test case: tests the getVersionCommand(String) method. Test data: the compile commands of the
   * languages, with extra whitespace, a path, and none at all. Expected result: the compiler itself
   * (javac, never the java runtime), asked for its version as it understands it.
   */
  @Test
  public void testGetVersionCommand() {
    Assertions.assertEquals(
        List.of("g++", "--version"), VojJudgerApplication.getVersionCommand("g++ -o a a.cpp"));
    Assertions.assertEquals(
        List.of("javac", "-version"),
        VojJudgerApplication.getVersionCommand("javac -d {filename} {filename}.java"));
    Assertions.assertEquals(
        List.of("kotlinc", "-version"),
        VojJudgerApplication.getVersionCommand("kotlinc {filename}.kt -d {filename}.jar"));
    Assertions.assertEquals(
        List.of("go", "version"), VojJudgerApplication.getVersionCommand("  go\tbuild x.go"));
    Assertions.assertEquals(
        List.of("/usr/bin/fpc", "-iV"),
        VojJudgerApplication.getVersionCommand("/usr/bin/fpc -O2 {filename}.pas"));
    Assertions.assertEquals(
        List.of("C:\\Go\\bin\\GO.EXE", "version"),
        VojJudgerApplication.getVersionCommand("C:\\Go\\bin\\GO.EXE build x.go"));
    Assertions.assertEquals(
        List.of("make", "--version"), VojJudgerApplication.getVersionCommand("make"));
    Assertions.assertNull(VojJudgerApplication.getVersionCommand("   "));
    Assertions.assertNull(VojJudgerApplication.getVersionCommand(null));
  }

  /**
   * Test case: tests the getCommandOutput method. Test data: the Java runtime asked for its
   * version (which it prints on its standard error), and a program that does not exist. Expected
   * result: the version, and nothing.
   */
  @Test
  public void testGetCommandOutput() {
    String java = Path.of(System.getProperty("java.home"), "bin", "java").toString();

    String version = VojJudgerApplication.getCommandOutput(List.of(java, "-version"), 30000);

    Assertions.assertNotNull(version);
    Assertions.assertTrue(version.contains(System.getProperty("java.version")), version);
    Assertions.assertNull(
        VojJudgerApplication.getCommandOutput(List.of("voj-no-such-compiler", "--version"), 30000));
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
        VojJudgerApplication.getCommandOutput(
            List.of("sh", "-c", "head -c 1000000 /dev/zero; head -c 1000000 /dev/zero >&2; exit 1"),
            30000));

    long start = System.nanoTime();
    Assertions.assertNull(
        VojJudgerApplication.getCommandOutput(List.of("sh", "-c", "exec sleep 60"), 500));
    Assertions.assertTrue(System.nanoTime() - start < 10_000_000_000L);
  }
}
