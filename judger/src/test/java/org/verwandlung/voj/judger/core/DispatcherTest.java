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
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import org.verwandlung.voj.judger.application.ApplicationDispatcher;
import org.verwandlung.voj.judger.exception.IllgealSubmissionException;
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
    when(submissionMapper.getSubmission(100L)).thenReturn(buildSubmission());
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
    verify(submissionMapper, times(1)).getSubmission(404L);
    verify(compiler, never()).getCompileResult(any(), anyString(), anyString());
  }

  /**
   * Test case: tests the createNewTask method. Test data: a submission that compiles successfully
   * and produces the correct output. Expected result: every judging stage is dispatched and the
   * runtime result is reported as accepted.
   */
  @Test
  public void testCreateNewTaskAccepted() throws Exception {
    when(compiler.getCompileResult(any(), anyString(), anyString()))
        .thenReturn(compileResult(true));

    List<Checkpoint> checkpoints = new ArrayList<>();
    checkpoints.add(new Checkpoint(1000L, 1, true, 100, "input", "output"));
    when(checkpointStore.getCheckpoints(1000L)).thenReturn(checkpoints);

    when(programRunner.getRuntimeResult(any(), anyString(), anyString(), anyString(), anyString()))
        .thenReturn(runtimeResult("AC"));
    when(comparator.getComparisonResult(anyString(), anyString(), anyBoolean())).thenReturn("AC");

    dispatcher.createNewTask(100L);

    verify(submissionMapper, times(1)).getSubmission(100L);
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
    when(compiler.getCompileResult(any(), anyString(), anyString()))
        .thenReturn(compileResult(true));

    List<Checkpoint> checkpoints = new ArrayList<>();
    checkpoints.add(new Checkpoint(1000L, 1, true, 100, "input", "output"));
    when(checkpointStore.getCheckpoints(1000L)).thenReturn(checkpoints);

    when(programRunner.getRuntimeResult(any(), anyString(), anyString(), anyString(), anyString()))
        .thenReturn(runtimeResult("AC"));
    when(comparator.getComparisonResult(anyString(), anyString(), anyBoolean())).thenReturn("WA");

    dispatcher.createNewTask(100L);

    @SuppressWarnings("unchecked")
    org.mockito.ArgumentCaptor<Map<String, Object>> resultCaptor =
        org.mockito.ArgumentCaptor.forClass(Map.class);
    verify(applicationDispatcher).onOneTestPointFinished(eq(100L), eq(1), resultCaptor.capture());
    Assertions.assertEquals("WA", resultCaptor.getValue().get("runtimeResult"));
  }

  /**
   * Test case: tests the createNewTask method. Test data: two checkpoints, only the first requiring
   * an exact match, whose output differs only in presentation. Expected result: each checkpoint's
   * exactlyMatch flag is passed to the comparator and its verdict (PE / AC) is reported.
   */
  @Test
  public void testCreateNewTaskHonoursExactlyMatch() throws Exception {
    when(compiler.getCompileResult(any(), anyString(), anyString()))
        .thenReturn(compileResult(true));

    List<Checkpoint> checkpoints = new ArrayList<>();
    checkpoints.add(new Checkpoint(1000L, 1, true, 50, "input", "output"));
    checkpoints.add(new Checkpoint(1000L, 2, false, 50, "input", "output"));
    when(checkpointStore.getCheckpoints(1000L)).thenReturn(checkpoints);

    when(programRunner.getRuntimeResult(any(), anyString(), anyString(), anyString(), anyString()))
        .thenAnswer(invocation -> runtimeResult("AC"));
    when(comparator.getComparisonResult(anyString(), anyString(), eq(true))).thenReturn("PE");
    when(comparator.getComparisonResult(anyString(), anyString(), eq(false))).thenReturn("AC");

    dispatcher.createNewTask(100L);

    @SuppressWarnings("unchecked")
    org.mockito.ArgumentCaptor<List<Map<String, Object>>> resultsCaptor =
        org.mockito.ArgumentCaptor.forClass(List.class);
    verify(applicationDispatcher).onAllTestPointsFinished(eq(100L), resultsCaptor.capture());
    Assertions.assertEquals("PE", resultsCaptor.getValue().get(0).get("runtimeResult"));
    Assertions.assertEquals("AC", resultsCaptor.getValue().get(1).get("runtimeResult"));
  }

  /**
   * Test case: tests the createNewTask method. Test data: a submission that fails to compile.
   * Expected result: the program is never executed.
   */
  @Test
  public void testCreateNewTaskCompileFailed() throws Exception {
    when(checkpointStore.getCheckpoints(1000L)).thenReturn(oneCheckpoint());
    when(compiler.getCompileResult(any(), anyString(), anyString()))
        .thenReturn(compileResult(false));

    dispatcher.createNewTask(100L);

    verify(applicationDispatcher).onCompileFinished(eq(100L), any());
    verify(programRunner, never())
        .getRuntimeResult(any(), anyString(), anyString(), anyString(), anyString());
    verify(applicationDispatcher, never()).onAllTestPointsFinished(anyLong(), any());
    verify(applicationDispatcher, never()).onErrorOccurred(anyLong(), anyString());
  }

  /**
   * Test case: tests the createNewTask method. Test data: a problem without any checkpoint.
   * Expected result: a System Error, without compiling; never an Accepted with nothing checked.
   */
  @Test
  public void testCreateNewTaskWithoutCheckpointsIsSystemError() throws Exception {
    when(checkpointStore.getCheckpoints(1000L)).thenReturn(new ArrayList<>());

    dispatcher.createNewTask(100L);

    verify(applicationDispatcher).onErrorOccurred(eq(100L), contains("no test data"));
    verify(compiler, never()).getCompileResult(any(), anyString(), anyString());
    verify(applicationDispatcher, never()).onAllTestPointsFinished(anyLong(), any());
  }

  /**
   * Test case: tests the createNewTask method. Test data: the test data cannot be downloaded.
   * Expected result: a System Error.
   */
  @Test
  public void testCreateNewTaskWithUnavailableCheckpointsIsSystemError() throws Exception {
    org.mockito.Mockito.doThrow(new java.io.IOException("HTTP 403"))
        .when(checkpointStore)
        .sync(any());

    dispatcher.createNewTask(100L);

    verify(applicationDispatcher).onErrorOccurred(eq(100L), contains("could not be loaded"));
    verify(compiler, never()).getCompileResult(any(), anyString(), anyString());
  }

  /**
   * Test case: tests the createNewTask method. Test data: the sandbox fails to run the compiler.
   * Expected result: a System Error rather than a Compile Error with an empty log.
   */
  @Test
  public void testCreateNewTaskCompilerSandboxFailureIsSystemError() throws Exception {
    when(checkpointStore.getCheckpoints(1000L)).thenReturn(oneCheckpoint());
    Map<String, Object> result = compileResult(false);
    result.put("verdict", Verdict.INTERNAL_ERROR);
    when(compiler.getCompileResult(any(), anyString(), anyString())).thenReturn(result);

    dispatcher.createNewTask(100L);

    verify(applicationDispatcher).onErrorOccurred(eq(100L), contains("compiler"));
    verify(applicationDispatcher, never()).onCompileFinished(anyLong(), any());
  }

  /**
   * Test case: tests the createNewTask method. Test data: the expected output cannot be read.
   * Expected result: a System Error rather than a Wrong Answer.
   */
  @Test
  public void testCreateNewTaskUnreadableAnswerIsSystemError() throws Exception {
    when(checkpointStore.getCheckpoints(1000L)).thenReturn(oneCheckpoint());
    when(compiler.getCompileResult(any(), anyString(), anyString()))
        .thenReturn(compileResult(true));
    when(programRunner.getRuntimeResult(any(), anyString(), anyString(), anyString(), anyString()))
        .thenReturn(runtimeResult("AC"));
    when(comparator.getComparisonResult(anyString(), anyString(), anyBoolean()))
        .thenThrow(new java.io.IOException("No such file"));

    dispatcher.createNewTask(100L);

    verify(applicationDispatcher).onErrorOccurred(eq(100L), contains("could not be read"));
    verify(applicationDispatcher, never()).onAllTestPointsFinished(anyLong(), any());
  }

  /**
   * Test case: tests the createNewTask method. Test data: a stage throws after the work directory
   * was created. Expected result: a System Error, and the work directory is still removed.
   */
  @Test
  public void testCreateNewTaskCleansUpOnFailure(@TempDir Path tempDir) throws Exception {
    ReflectionTestUtils.setField(dispatcher, "workBaseDirectory", tempDir.toString());
    Path workDirectory = Files.createDirectory(tempDir.resolve("voj-100"));
    Files.writeString(workDirectory.resolve("Main.cpp"), "int main() {}");

    when(checkpointStore.getCheckpoints(1000L)).thenReturn(oneCheckpoint());
    when(compiler.getCompileResult(any(), anyString(), anyString()))
        .thenThrow(new IllegalStateException("compiler crashed"));

    dispatcher.createNewTask(100L);

    verify(applicationDispatcher).onErrorOccurred(100L, Dispatcher.INTERNAL_ERROR_REASON);
    Assertions.assertFalse(Files.exists(workDirectory));
  }

  /**
   * Test case: tests the createNewTask method. Test data: the database becomes unreachable while
   * the verdict is recorded. Expected result: no System Error is attempted (it could not be
   * recorded either); the exception is passed on so that the task is delivered again.
   */
  @Test
  public void testCreateNewTaskPassesOnDatabaseFailures() throws Exception {
    when(checkpointStore.getCheckpoints(1000L)).thenReturn(oneCheckpoint());
    when(compiler.getCompileResult(any(), anyString(), anyString()))
        .thenReturn(compileResult(false));
    org.mockito.Mockito.doThrow(
            new org.springframework.dao.DataAccessResourceFailureException("database down"))
        .when(applicationDispatcher)
        .onCompileFinished(anyLong(), any());

    Assertions.assertThrows(
        org.springframework.dao.DataAccessException.class, () -> dispatcher.createNewTask(100L));
    verify(applicationDispatcher, never()).onErrorOccurred(anyLong(), anyString());
  }

  /**
   * Builds a single checkpoint of Problem#1000.
   *
   * @return the checkpoints
   */
  private List<Checkpoint> oneCheckpoint() {
    List<Checkpoint> checkpoints = new ArrayList<>();
    checkpoints.add(new Checkpoint(1000L, 1, true, 100, "input", "output"));
    return checkpoints;
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

  /** The mocked ProgramRunner object. */
  @Mock private ProgramRunner programRunner;

  /** The mocked Comparator object. */
  @Mock private Comparator comparator;

  /** The mocked SubmissionMapper object. */
  @Mock private SubmissionMapper submissionMapper;

  /** The mocked CheckpointStore object. */
  @Mock private CheckpointStore checkpointStore;

  /** The Dispatcher object under test. */
  @InjectMocks private Dispatcher dispatcher;
}
