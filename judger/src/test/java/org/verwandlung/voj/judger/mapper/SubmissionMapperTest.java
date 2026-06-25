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

import org.verwandlung.voj.judger.model.Submission;

/**
 * Test class for SubmissionMapper.
 *
 * @author Haozhe Xie
 */
@ExtendWith(SpringExtension.class)
@Transactional
@ContextConfiguration({"classpath:test-spring-context.xml"})
public class SubmissionMapperTest {
  /**
   * Test case: tests the getSubmission(long) method. Test data: the unique identifier of a
   * submission for Problem#1000. Expected result: returns the expected Submission object.
   */
  @Test
  public void testGetSubmissionExists() {
    Submission submission = submissionMapper.getSubmission(1000);
    Assertions.assertNotNull(submission);

    long problemId = submission.getProblem().getProblemId();
    Assertions.assertEquals(1000, problemId);
  }

  /**
   * Test case: tests the getSubmission(long) method. Test data: a non-existent submission
   * identifier. Expected result: returns a null reference.
   */
  @Test
  public void testGetSubmissionNotExists() {
    Submission submission = submissionMapper.getSubmission(0);
    Assertions.assertNull(submission);
  }

  /** The SubmissionMapper object under test. */
  @Autowired private SubmissionMapper submissionMapper;
}
