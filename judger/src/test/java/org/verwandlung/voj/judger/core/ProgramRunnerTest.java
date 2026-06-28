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

import org.verwandlung.voj.judger.mapper.CheckpointMapper;
import org.verwandlung.voj.judger.mapper.SubmissionMapper;
import org.verwandlung.voj.judger.model.Checkpoint;
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
   * Test case: tests the getRuntimeResult(Submission, String, String, String, String) method. Test
   * data: C++ code that compiles successfully. Expected result: compiles successfully and produces
   * output when run.
   */
  @Test
  public void testGetRuntimeResultCpp() throws Exception {
    String workDirectory = workBaseDirectory + "/voj-1000";
    String baseFileName = "random-name";
    Submission submission = submissionMapper.getSubmission(1000);

    String inputFilePath = workBaseDirectory + "/testpoints/1000/input#0.txt";
    String outputFilePath = workBaseDirectory + "/voj-1000/output#0.txt";

    preprocessor.createTestCode(submission, workDirectory, baseFileName);
    writeCheckpoints(submission.getProblem().getProblemId());
    compiler.getCompileResult(submission, workDirectory, baseFileName);

    Map<String, Object> result =
        programRunner.getRuntimeResult(
            submission, workDirectory, baseFileName, inputFilePath, outputFilePath);
    Assertions.assertEquals("AC", result.get("runtimeResult"));
  }

  /**
   * Test case: tests the getRuntimeResult(Submission, String, String, String, String) method. Test
   * data: Java code that compiles successfully. Expected result: compiles successfully and produces
   * output when run.
   *
   * <p>The test case is disabled for passing CI on AppVeyor.
   */
  public void testGetRuntimeResultJava() throws Exception {
    String workDirectory = workBaseDirectory + "/voj-1001";
    String baseFileName = "RandomName";
    Submission submission = submissionMapper.getSubmission(1001);

    String inputFilePath = workBaseDirectory + "/testpoints/1001/input#0.txt";
    String outputFilePath = workBaseDirectory + "/voj-1000/output#0.txt";

    preprocessor.createTestCode(submission, workDirectory, baseFileName);
    writeCheckpoints(submission.getProblem().getProblemId());
    compiler.getCompileResult(submission, workDirectory, baseFileName);

    Map<String, Object> result =
        programRunner.getRuntimeResult(
            submission, workDirectory, baseFileName, inputFilePath, outputFilePath);
    Assertions.assertEquals("AC", result.get("runtimeResult"));
  }

  /**
   * Materializes a problem's checkpoints onto the local disk where the runner expects them. The
   * judger no longer fetches test data from the database at runtime (it downloads and caches it from
   * the web side), so this test populates the cache directory directly from the database to stay
   * self-contained without a running web application.
   *
   * @param problemId - the unique identifier of the problem
   * @throws Exception if a checkpoint file cannot be written
   */
  private void writeCheckpoints(long problemId) throws Exception {
    File problemDirectory = new File(checkpointDirectory, String.valueOf(problemId));
    problemDirectory.mkdirs();
    for (Checkpoint checkpoint : checkpointMapper.getCheckpointsUsingProblemId(problemId)) {
      int checkpointId = checkpoint.getCheckpointId();
      Files.writeString(
          new File(problemDirectory, "input#" + checkpointId + ".txt").toPath(),
          checkpoint.getInput(),
          StandardCharsets.UTF_8);
      Files.writeString(
          new File(problemDirectory, "output#" + checkpointId + ".txt").toPath(),
          checkpoint.getOutput(),
          StandardCharsets.UTF_8);
    }
  }

  /** The ProgramRunner object under test. */
  @Autowired private ProgramRunner programRunner;

  /** The autowired Compiler object, used to set up the test cases. */
  @Autowired private Compiler compiler;

  /** The autowired Preprocessor object, used to set up the test cases. */
  @Autowired private Preprocessor preprocessor;

  /** The autowired SubmissionMapper object, used to set up the test cases. */
  @Autowired private SubmissionMapper submissionMapper;

  /** The autowired CheckpointMapper object, used to materialize the test data from the database. */
  @Autowired private CheckpointMapper checkpointMapper;

  /** The working directory of the judger, used to store compilation results and program output. */
  @Value("${judger.workDir}")
  private String workBaseDirectory;

  /** The storage directory of checkpoints, where the runner reads its input/output files. */
  @Value("${judger.checkpointDir}")
  private String checkpointDirectory;
}
