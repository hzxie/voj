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
package org.verwandlung.voj.judger.mapper;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

import org.verwandlung.voj.judger.model.JudgeResult;

/**
 * Test class for JudgeResultMapper.
 *
 * @author Haozhe Xie
 */
@ExtendWith(SpringExtension.class)
@Transactional
@ContextConfiguration({"classpath:test-spring-context.xml"})
public class JudgeResultMapperTest {
  /**
   * Test case: tests the getJudgeResultUsingSlug(String) method. Test data: the unique English
   * abbreviation of an ordinary judge result. Expected result: returns the corresponding
   * JudgeResult object.
   */
  @Test
  public void testGetJudgeResultUsingSlugExists() {
    JudgeResult judgeResult = judgeResultMapper.getJudgeResultUsingSlug("AC");
    Assertions.assertNotNull(judgeResult);

    int judgeResultId = judgeResult.getJudgeResultId();
    Assertions.assertEquals(2, judgeResultId);
  }

  /**
   * Test case: tests the getJudgeResultUsingSlug(String) method. Test data: a non-existent unique
   * English abbreviation of a judge result. Expected result: returns a null reference.
   */
  @Test
  public void testGetJudgeResultUsingSlugNotExists() {
    JudgeResult judgeResult = judgeResultMapper.getJudgeResultUsingSlug("Not-Exists");
    Assertions.assertNull(judgeResult);
  }

  /** The JudgeResultMapper object under test. */
  @Autowired private JudgeResultMapper judgeResultMapper;
}
