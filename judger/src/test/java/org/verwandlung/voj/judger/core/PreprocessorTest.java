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

import java.io.FileInputStream;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

import org.verwandlung.voj.judger.mapper.SubmissionMapper;
import org.verwandlung.voj.judger.model.Submission;

/**
 * Test class for the preprocessor.
 *
 * @author Haozhe Xie
 */
@ExtendWith(SpringExtension.class)
@Transactional
@ContextConfiguration({"classpath:test-spring-context.xml"})
public class PreprocessorTest {
  /**
   * Test case: tests the createTestCode(Submission, String, String) method. Test data: an existing
   * submission (Submission#1000). Expected result: the source code file is created at the specified
   * location.
   *
   * @throws Exception
   */
  @Test
  public void testCreateTestCodeCpp() throws Exception {
    Submission submission = submissionMapper.getSubmission(1000);
    String workDirectory = "/tmp/voj-1000/";
    String baseFileName = "random-name";

    preprocessor.createTestCode(submission, workDirectory, baseFileName);
    FileInputStream inputStream = new FileInputStream("/tmp/voj-1000/random-name.cpp");
    String code = IOUtils.toString(inputStream);
    inputStream.close();
    Assertions.assertEquals(submission.getCode(), code);
  }

  /**
   * Test case: tests the createTestCode(Submission, String, String) method. Test data: an existing
   * submission (Submission#1001). Expected result: the source code file is created at the specified
   * location.
   *
   * @throws Exception
   */
  @Test
  public void testCreateTestCodeJava() throws Exception {
    Submission submission = submissionMapper.getSubmission(1001);
    String workDirectory = "/tmp/voj-1001/";
    String baseFileName = "RandomName";

    preprocessor.createTestCode(submission, workDirectory, baseFileName);
    FileInputStream inputStream = new FileInputStream("/tmp/voj-1001/RandomName.java");
    String code = IOUtils.toString(inputStream).replace("RandomName", "Main");
    inputStream.close();
    Assertions.assertEquals(submission.getCode(), code);
  }

  /** The Preprocessor object under test. */
  @Autowired private Preprocessor preprocessor;

  /** The autowired SubmissionMapper object, used to set up the test cases. */
  @Autowired private SubmissionMapper submissionMapper;
}
