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
package org.verwandlung.voj.web.service;

import java.util.Arrays;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

/**
 * The test class for OffensiveWordService.
 *
 * @author Haozhe Xie
 */
@ExtendWith(SpringExtension.class)
@Transactional
@ContextConfiguration({"classpath:test-spring-context.xml"})
public class OffensiveWordServiceTest {
  /** Test case: tests the getOffensiveWordSources() method. Test data: N/a. Expected: the configured import sources pinned by test-data.sql. */
  @Test
  public void testGetOffensiveWordSources() {
    String sources = offensiveWordService.getOffensiveWordSources();
    Assertions.assertTrue(sources.contains("https://example.org/badwords.txt"), sources);
  }

  /** Test case: tests the updateOffensiveWordSources(String) method. Test data: a new source list. Expected: the value is persisted and read back. */
  @Test
  public void testUpdateOffensiveWordSources() {
    offensiveWordService.updateOffensiveWordSources("https://example.org/updated.txt");
    Assertions.assertEquals(
        "https://example.org/updated.txt", offensiveWordService.getOffensiveWordSources());
  }

  /** Test case: tests the updateOffensiveWordSources(String) method. Test data: a null value. Expected: the stored value becomes an empty string rather than null. */
  @Test
  public void testUpdateOffensiveWordSourcesWithNull() {
    offensiveWordService.updateOffensiveWordSources(null);
    Assertions.assertEquals("", offensiveWordService.getOffensiveWordSources());
  }

  /** Test case: tests the getNumberOfOffensiveWords() method. Test data: N/a. Expected: the number of seeded offensive words (six). */
  @Test
  public void testGetNumberOfOffensiveWords() {
    Assertions.assertEquals(6, offensiveWordService.getNumberOfOffensiveWords());
  }

  /** Test case: tests the getOffensiveWordsImportedAt() method. Test data: N/a. Expected: the last import time pinned by test-data.sql. */
  @Test
  public void testGetOffensiveWordsImportedAt() {
    Assertions.assertEquals(
        "2018-02-23 12:00:00", offensiveWordService.getOffensiveWordsImportedAt());
  }

  /** Test case: tests the replaceAllOffensiveWords(Collection) method. Test data: a fresh set of three words. Expected: the dictionary is replaced and the count reflects the new words. */
  @Test
  public void testReplaceAllOffensiveWords() {
    int numberOfWords =
        offensiveWordService.replaceAllOffensiveWords(Arrays.asList("alpha", "beta", "gamma"));
    Assertions.assertEquals(3, numberOfWords);
    Assertions.assertEquals(3, offensiveWordService.getNumberOfOffensiveWords());
    Assertions.assertFalse(offensiveWordService.getOffensiveWordsImportedAt().isEmpty());
  }

  /** The OffensiveWordService object under test. */
  @Autowired private OffensiveWordService offensiveWordService;
}
