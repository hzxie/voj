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
package org.verwandlung.voj.web.mapper;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

/**
 * The test class for OffensiveWordMapper.
 *
 * @author Haozhe Xie
 */
@ExtendWith(SpringExtension.class)
@Transactional
@ContextConfiguration({"classpath:test-spring-context.xml"})
public class OffensiveWordMapperTest {
  /** Test case: tests the getOffensiveWords() method. Test data: N/a. Expected: the six seeded offensive words. */
  @Test
  public void testGetOffensiveWords() {
    List<String> words = offensiveWordMapper.getOffensiveWords();
    Assertions.assertNotNull(words);
    Assertions.assertEquals(6, words.size());
  }

  /** Test case: tests the getNumberOfOffensiveWords() method. Test data: N/a. Expected: the number of seeded offensive words (six). */
  @Test
  public void testGetNumberOfOffensiveWords() {
    Assertions.assertEquals(6, offensiveWordMapper.getNumberOfOffensiveWords());
  }

  /** Test case: tests the batchCreateOffensiveWords(List) method. Test data: two new offensive words. Expected: two rows are inserted and the dictionary grows accordingly. */
  @Test
  public void testBatchCreateOffensiveWords() {
    int numberOfRowsAffected =
        offensiveWordMapper.batchCreateOffensiveWords(Arrays.asList("test-word-a", "test-word-b"));
    Assertions.assertEquals(2, numberOfRowsAffected);
    Assertions.assertEquals(8, offensiveWordMapper.getNumberOfOffensiveWords());
  }

  /** Test case: tests the deleteAllOffensiveWords() method. Test data: N/a. Expected: the dictionary is emptied. */
  @Test
  public void testDeleteAllOffensiveWords() {
    offensiveWordMapper.deleteAllOffensiveWords();
    Assertions.assertEquals(0, offensiveWordMapper.getNumberOfOffensiveWords());
    Assertions.assertTrue(offensiveWordMapper.getOffensiveWords().isEmpty());
  }

  /** The OffensiveWordMapper object under test. */
  @Autowired private OffensiveWordMapper offensiveWordMapper;
}
