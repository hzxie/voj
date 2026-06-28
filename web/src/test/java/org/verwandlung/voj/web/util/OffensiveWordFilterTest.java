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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

/**
 * The test class for SensitiveWordFilter.
 *
 * @author YiHao Zhou
 */
@ExtendWith(SpringExtension.class)
@Transactional
@ContextConfiguration({"classpath:test-spring-context.xml"})
public class OffensiveWordFilterTest {
  /** Test case: tests the filter() method. Test data: data containing offensive words. Expected: the data with the offensive words filtered out. */
  @Test
  public void testFilterUsingOffensiveWord() {
    Assertions.assertNotNull(filter);
    Assertions.assertEquals("**大法好 你好", filter.filter("法轮大法好 你好"));
  }

  /** Test case: tests the censor() method. Test data: text containing an offensive word. Expected: the word is masked with plain asterisks. */
  @Test
  public void testCensorMasksWithPlainAsterisks() {
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

  /** The autowired SensitiveWordFilter object. */
  @Autowired private OffensiveWordFilter filter;
}
