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

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
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
 * Test class for the program runner.
 *
 * <p>The test context does not set {@code judger.sandbox}, so it exercises the default native
 * sandbox runner end to end.
 *
 * @author Haozhe Xie
 */
@ExtendWith(SpringExtension.class)
@Transactional
@ContextConfiguration({"classpath:test-spring-context.xml"})
public class ProgramRunnerTest {
  /**
   * Test case: tests the getRuntimeResult(Submission, String, String, String) method. Test
   * data: C++ code that compiles successfully. Expected result: compiles successfully and produces
   * output when run.
   */
  @Test
  public void testGetRuntimeResultCpp() throws Exception {
    String workDirectory = workBaseDirectory + "/voj-1000";
    Submission submission = submissionMapper.getSubmission(1000);

    String inputFilePath = workBaseDirectory + "/testpoints/1000/input#0.txt";
    String outputFilePath = workBaseDirectory + "/voj-1000/output#0.txt";

    preprocessor.createTestCode(submission, workDirectory);
    writeInput(inputFilePath, "18820 26832\r\n");
    compiler.getCompileResult(submission, workDirectory);

    Map<String, Object> result =
        programRunner.getRuntimeResult(
            submission, workDirectory, inputFilePath, outputFilePath);
    Assertions.assertEquals("AC", result.get("runtimeResult"));
  }

  /**
   * Test case: tests the getRuntimeResult(Submission, String, String, String) method. Test
   * data: Java code that compiles successfully. Expected result: compiles successfully and produces
   * output when run.
   *
   * <p>The test case is disabled for passing CI on AppVeyor.
   */
  public void testGetRuntimeResultJava() throws Exception {
    String workDirectory = workBaseDirectory + "/voj-1001";
    Submission submission = submissionMapper.getSubmission(1001);

    String inputFilePath = workBaseDirectory + "/testpoints/1001/input#0.txt";
    String outputFilePath = workBaseDirectory + "/voj-1000/output#0.txt";

    preprocessor.createTestCode(submission, workDirectory);
    writeInput(inputFilePath, "18820 26832\r\n");
    compiler.getCompileResult(submission, workDirectory);

    Map<String, Object> result =
        programRunner.getRuntimeResult(
            submission, workDirectory, inputFilePath, outputFilePath);
    Assertions.assertEquals("AC", result.get("runtimeResult"));
  }

  /**
   * Writes the input of a checkpoint where the runner reads it from (the first checkpoint of
   * Problem#1000 in test-data.sql). The judger downloads test data from the web application, which
   * the test does without.
   *
   * @param inputFilePath - the path of the input file
   * @param input - the input
   * @throws Exception if the file cannot be written
   */
  private void writeInput(String inputFilePath, String input) throws Exception {
    File inputFile = new File(inputFilePath);
    inputFile.getParentFile().mkdirs();
    Files.writeString(inputFile.toPath(), input, StandardCharsets.UTF_8);
  }

  /** The ProgramRunner object under test. */
  @Autowired private ProgramRunner programRunner;

  /** The autowired Compiler object, used to set up the test cases. */
  @Autowired private Compiler compiler;

  /** The autowired Preprocessor object, used to set up the test cases. */
  @Autowired private Preprocessor preprocessor;

  /** The autowired SubmissionMapper object, used to set up the test cases. */
  @Autowired private SubmissionMapper submissionMapper;

  /** Where the test judges: the compilation results and the program output go there. */
  @Value("${test.workDir}")
  private String workBaseDirectory;
}
