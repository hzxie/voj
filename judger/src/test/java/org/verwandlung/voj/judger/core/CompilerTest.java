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

import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

import org.verwandlung.voj.judger.mapper.SubmissionMapper;
import org.verwandlung.voj.judger.model.Submission;

/**
 * Test class for the program compiler.
 *
 * @author Haozhe Xie
 */
@ExtendWith(SpringExtension.class)
@Transactional
@ContextConfiguration({"classpath:test-spring-context.xml"})
public class CompilerTest {
  /**
   * Test case: tests the getCompileResult() method. Test data: C++ code that compiles successfully.
   * Expected result: compilation succeeds.
   */
  @Test
  public void testGetCompileResultCppWithSuccess() throws Exception {
    String workDirectory = workBaseDirectory + "/voj-1000";
    String baseFileName = "random-name";
    Submission submission = submissionMapper.getSubmission(1000);
    preprocessor.createTestCode(submission, workDirectory, baseFileName);

    Map<String, Object> result = compiler.getCompileResult(submission, workDirectory, baseFileName);
    Assertions.assertEquals(true, result.get("isSuccessful"));
  }

  /**
   * Test case: tests the getCompileResult() method. Test data: Java code that compiles
   * successfully. Expected result: compilation succeeds.
   */
  @Test
  public void testGetCompileResultJavaWithSuccess() throws Exception {
    String workDirectory = workBaseDirectory + "/voj-1001";
    String baseFileName = "RandomName";
    Submission submission = submissionMapper.getSubmission(1001);
    preprocessor.createTestCode(submission, workDirectory, baseFileName);

    Map<String, Object> result = compiler.getCompileResult(submission, workDirectory, baseFileName);
    Assertions.assertEquals(true, result.get("isSuccessful"));
  }

  /**
   * Test case: tests the getCompileResult() method. Test data: C++ code that fails to compile.
   * Expected result: compilation fails.
   */
  @Test
  public void testGetCompileResultCppWithError() throws Exception {
    String workDirectory = workBaseDirectory + "/voj-1002";
    String baseFileName = "random-name";
    Submission submission = submissionMapper.getSubmission(1002);
    preprocessor.createTestCode(submission, workDirectory, baseFileName);

    Map<String, Object> result = compiler.getCompileResult(submission, workDirectory, baseFileName);
    Assertions.assertEquals(false, result.get("isSuccessful"));
  }

  /** The Compiler object under test. */
  @Autowired private Compiler compiler;

  /** The autowired Preprocessor object, used to set up the test cases. */
  @Autowired private Preprocessor preprocessor;

  /** The autowired SubmissionMapper object, used to set up the test cases. */
  @Autowired private SubmissionMapper submissionMapper;

  /** The working directory of the judger, used to store compilation results and program output. */
  @Value("${judger.workDir}")
  private String workBaseDirectory;
}
