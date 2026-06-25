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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import org.verwandlung.voj.judger.application.ApplicationDispatcher;
import org.verwandlung.voj.judger.exception.IllgealSubmissionException;
import org.verwandlung.voj.judger.mapper.CheckpointMapper;
import org.verwandlung.voj.judger.mapper.SubmissionMapper;
import org.verwandlung.voj.judger.model.Checkpoint;
import org.verwandlung.voj.judger.model.Problem;
import org.verwandlung.voj.judger.model.Submission;

/**
 * Test class for the judger Dispatcher.
 *
 * @author Haozhe Xie
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class DispatcherTest {
  /** Injects the working-directory configuration that would normally come from the property files. */
  @BeforeEach
  public void setUp() {
    ReflectionTestUtils.setField(dispatcher, "workBaseDirectory", "/tmp/voj-dispatcher-tests");
    ReflectionTestUtils.setField(dispatcher, "checkpointDirectory", "/tmp/voj-dispatcher-tests");
  }

  /**
   * Test case: tests the createNewTask method. Test data: a non-existent submission. Expected
   * result: an IllgealSubmissionException is thrown and no compilation is attempted.
   */
  @Test
  public void testCreateNewTaskIllegalSubmission() throws Exception {
    when(submissionMapper.getSubmission(anyLong())).thenReturn(null);

    Assertions.assertThrows(
        IllgealSubmissionException.class, () -> dispatcher.createNewTask(404L));
    verify(compiler, never()).getCompileResult(any(), anyString(), anyString());
  }

  /**
   * Test case: tests the createNewTask method. Test data: a submission that compiles successfully
   * and produces the correct output. Expected result: every judging stage is dispatched and the
   * runtime result is reported as accepted.
   */
  @Test
  public void testCreateNewTaskAccepted() throws Exception {
    Submission submission = buildSubmission();
    when(submissionMapper.getSubmission(100L)).thenReturn(submission);
    when(compiler.getCompileResult(any(), anyString(), anyString()))
        .thenReturn(compileResult(true));

    List<Checkpoint> checkpoints = new ArrayList<>();
    checkpoints.add(new Checkpoint(1000L, 1, true, 100, "input", "output"));
    when(checkpointMapper.getCheckpointsUsingProblemId(1000L)).thenReturn(checkpoints);

    when(runner.getRuntimeResult(any(), anyString(), anyString(), anyString(), anyString()))
        .thenReturn(runtimeResult("AC"));
    when(comparator.isOutputTheSame(anyString(), anyString())).thenReturn(true);

    dispatcher.createNewTask(100L);

    verify(applicationDispatcher).onCompileFinished(eq(100L), any());
    verify(applicationDispatcher).onOneTestPointFinished(eq(100L), eq(1), any());

    @SuppressWarnings("unchecked")
    org.mockito.ArgumentCaptor<List<Map<String, Object>>> resultsCaptor =
        org.mockito.ArgumentCaptor.forClass(List.class);
    verify(applicationDispatcher).onAllTestPointsFinished(eq(100L), resultsCaptor.capture());
    Assertions.assertEquals("AC", resultsCaptor.getValue().get(0).get("runtimeResult"));
    Assertions.assertEquals(100, resultsCaptor.getValue().get(0).get("score"));
  }

  /**
   * Test case: tests the createNewTask method. Test data: a submission that compiles successfully
   * but produces incorrect output. Expected result: an accepted runtime result is downgraded to a
   * wrong answer.
   */
  @Test
  public void testCreateNewTaskWrongAnswer() throws Exception {
    Submission submission = buildSubmission();
    when(submissionMapper.getSubmission(100L)).thenReturn(submission);
    when(compiler.getCompileResult(any(), anyString(), anyString()))
        .thenReturn(compileResult(true));

    List<Checkpoint> checkpoints = new ArrayList<>();
    checkpoints.add(new Checkpoint(1000L, 1, true, 100, "input", "output"));
    when(checkpointMapper.getCheckpointsUsingProblemId(1000L)).thenReturn(checkpoints);

    when(runner.getRuntimeResult(any(), anyString(), anyString(), anyString(), anyString()))
        .thenReturn(runtimeResult("AC"));
    when(comparator.isOutputTheSame(anyString(), anyString())).thenReturn(false);

    dispatcher.createNewTask(100L);

    @SuppressWarnings("unchecked")
    org.mockito.ArgumentCaptor<Map<String, Object>> resultCaptor =
        org.mockito.ArgumentCaptor.forClass(Map.class);
    verify(applicationDispatcher).onOneTestPointFinished(eq(100L), eq(1), resultCaptor.capture());
    Assertions.assertEquals("WA", resultCaptor.getValue().get("runtimeResult"));
  }

  /**
   * Test case: tests the createNewTask method. Test data: a submission that fails to compile.
   * Expected result: the program is never executed.
   */
  @Test
  public void testCreateNewTaskCompileFailed() throws Exception {
    Submission submission = buildSubmission();
    when(submissionMapper.getSubmission(100L)).thenReturn(submission);
    when(compiler.getCompileResult(any(), anyString(), anyString()))
        .thenReturn(compileResult(false));

    dispatcher.createNewTask(100L);

    verify(applicationDispatcher).onCompileFinished(eq(100L), any());
    verify(runner, never())
        .getRuntimeResult(any(), anyString(), anyString(), anyString(), anyString());
    verify(applicationDispatcher, never()).onAllTestPointsFinished(anyLong(), any());
  }

  /**
   * Builds a submission referencing Problem#1000.
   *
   * @return a submission object
   */
  private Submission buildSubmission() {
    Problem problem = new Problem(1000L, true, "A+B", 1000, 65536, "", "", "", "", "", "");
    Submission submission = new Submission();
    submission.setSubmissionId(100L);
    submission.setProblem(problem);
    return submission;
  }

  /**
   * Builds a compilation result.
   *
   * @param isSuccessful - whether the compilation succeeded
   * @return a compilation-result map
   */
  private Map<String, Object> compileResult(boolean isSuccessful) {
    Map<String, Object> result = new HashMap<>();
    result.put("isSuccessful", isSuccessful);
    result.put("log", isSuccessful ? "" : "error");
    return result;
  }

  /**
   * Builds a runtime result for a single checkpoint.
   *
   * @param slug - the unique English abbreviation of the judging result
   * @return a runtime-result map
   */
  private Map<String, Object> runtimeResult(String slug) {
    Map<String, Object> result = new HashMap<>();
    result.put("runtimeResult", slug);
    result.put("usedTime", 10);
    result.put("usedMemory", 128);
    return result;
  }

  /** The mocked ApplicationDispatcher object. */
  @Mock private ApplicationDispatcher applicationDispatcher;

  /** The mocked Preprocessor object. */
  @Mock private Preprocessor preprocessor;

  /** The mocked Compiler object. */
  @Mock private Compiler compiler;

  /** The mocked Runner object. */
  @Mock private Runner runner;

  /** The mocked Comparator object. */
  @Mock private Comparator comparator;

  /** The mocked SubmissionMapper object. */
  @Mock private SubmissionMapper submissionMapper;

  /** The mocked CheckpointMapper object. */
  @Mock private CheckpointMapper checkpointMapper;

  /** The Dispatcher object under test. */
  @InjectMocks private Dispatcher dispatcher;
}
