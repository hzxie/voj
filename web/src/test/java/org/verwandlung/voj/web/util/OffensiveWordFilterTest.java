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
package org.verwandlung.voj.web.util;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

import org.verwandlung.voj.web.mapper.OffensiveWordMapper;
import org.verwandlung.voj.web.mapper.OptionMapper;

/**
 * The test class for SensitiveWordFilter.
 *
 * @author YiHao Zhou
 */
@ExtendWith(SpringExtension.class)
@Transactional
@ContextConfiguration({"classpath:test-spring-context.xml"})
public class OffensiveWordFilterTest {
  /** Test case: tests the censor() method. Test data: text containing an offensive word. Expected: the word is masked with plain asterisks. */
  @Test
  public void testCensorMasksWithPlainAsterisks() {
    Assertions.assertNotNull(filter);
    Assertions.assertEquals("**大法好 你好", filter.censor("法轮大法好 你好"));
  }

  /** Test case: tests the censorMarkdown() method. Test data: text containing an offensive word. Expected: the word is masked with escaped asterisks so Markdown renders them literally. */
  @Test
  public void testCensorMarkdownEscapesAsterisks() {
    Assertions.assertEquals("\\*\\*大法好 你好", filter.censorMarkdown("法轮大法好 你好"));
  }

  /** Test case: tests that censor() tolerates a null argument. Test data: null. Expected: null is returned unchanged. */
  @Test
  public void testCensorToleratesNull() {
    Assertions.assertNull(filter.censor(null));
    Assertions.assertNull(filter.censorMarkdown(null));
  }

  /** Test case: tests hasWordBoundary(String, int, int). Test data: a one-letter ASCII match ('b') sitting inside ordinary English words. Expected: rejected, so words such as "dashboard" and "problem" are never masked. */
  @Test
  public void testHasWordBoundaryRejectsAsciiMatchInsideWord() {
    Assertions.assertFalse(hasWordBoundary("dashboard", 5, 1)); // the 'b' inside "dashboard"
    Assertions.assertFalse(hasWordBoundary("problem", 3, 1)); // the 'b' inside "problem"
    Assertions.assertFalse(hasWordBoundary("class", 2, 3)); // "ass" inside "class"
  }

  /** Test case: tests hasWordBoundary(String, int, int). Test data: an ASCII match that forms a whole, space-delimited token. Expected: accepted, so a genuinely standalone offensive word is still masked. */
  @Test
  public void testHasWordBoundaryAcceptsStandaloneAsciiMatch() {
    Assertions.assertTrue(hasWordBoundary("plan b", 5, 1)); // a standalone 'b'
    Assertions.assertTrue(hasWordBoundary("ass", 0, 3)); // the whole string
    Assertions.assertTrue(hasWordBoundary("you ass!", 4, 3)); // bounded by a space and punctuation
  }

  /** Test case: tests hasWordBoundary(String, int, int). Test data: a CJK match with adjacent CJK characters. Expected: accepted, since Chinese has no word delimiters and must keep matching anywhere. */
  @Test
  public void testHasWordBoundaryAlwaysAcceptsCjkMatch() {
    Assertions.assertTrue(hasWordBoundary("我操你", 1, 1)); // a single CJK character between CJK
    Assertions.assertTrue(hasWordBoundary("法轮功", 0, 2)); // "法轮" immediately followed by a CJK char
  }

  /** Test case: tests the censor() method. Test data: a lower-case and a mixed-case dictionary entry, matched against text in other cases. Expected: matching ignores case and the original text around the masked words is preserved. */
  @Test
  public void testCensorIsCaseInsensitive() {
    OffensiveWordFilter caseFilter = newFilter(List.of("fuck", "BadWord"));
    Assertions.assertEquals("**** you", caseFilter.censor("FUCK you"));
    Assertions.assertEquals("Oh ****!", caseFilter.censor("Oh FuCk!"));
    Assertions.assertEquals("a ******* b", caseFilter.censor("a badword b"));
    Assertions.assertEquals("a ******* B", caseFilter.censor("a BADWORD B"));
    // Word boundaries still apply after case folding.
    Assertions.assertEquals("BADWORDS", caseFilter.censor("BADWORDS"));
  }

  /**
   * Creates a stand-alone filter over the given dictionary, with the auto-censor option unset.
   *
   * @param words - the offensive words of the dictionary
   * @return the filter
   */
  private static OffensiveWordFilter newFilter(List<String> words) {
    OffensiveWordMapper offensiveWordMapper = Mockito.mock(OffensiveWordMapper.class);
    Mockito.when(offensiveWordMapper.getOffensiveWords()).thenReturn(words);
    try {
      Constructor<OffensiveWordFilter> constructor =
          OffensiveWordFilter.class.getDeclaredConstructor(
              OffensiveWordMapper.class, OptionMapper.class);
      constructor.setAccessible(true);
      return constructor.newInstance(offensiveWordMapper, Mockito.mock(OptionMapper.class));
    } catch (ReflectiveOperationException ex) {
      throw new RuntimeException(ex);
    }
  }

  /** Invokes the private static hasWordBoundary helper. */
  private static boolean hasWordBoundary(String text, int start, int length) {
    try {
      Method method =
          OffensiveWordFilter.class.getDeclaredMethod(
              "hasWordBoundary", String.class, int.class, int.class);
      method.setAccessible(true);
      return (boolean) method.invoke(null, text, start, length);
    } catch (ReflectiveOperationException ex) {
      throw new RuntimeException(ex);
    }
  }

  /** The autowired SensitiveWordFilter object. */
  @Autowired private OffensiveWordFilter filter;
}
