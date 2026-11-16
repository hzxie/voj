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
import static org.mockito.ArgumentMatchers.argThat;
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
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import org.verwandlung.voj.judger.application.ApplicationDispatcher;
import org.verwandlung.voj.judger.exception.CheckpointsRefusedException;
import org.verwandlung.voj.judger.exception.IllgealSubmissionException;
import org.verwandlung.voj.judger.exception.InvalidCheckpointsException;
import org.verwandlung.voj.judger.exception.RetryableJudgingException;
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
  public void setUp() throws Exception {
    when(checkpointStore.getWorkDirectory()).thenReturn(Path.of("/tmp/voj-dispatcher-tests"));
    when(checkpointStore.sync(any())).thenReturn(Path.of("/tmp/voj-dispatcher-tests/1000"));
    when(submissionMapper.getSubmission(100L)).thenReturn(buildSubmission());
    when(applicationDispatcher.onJudgingStarted(anyLong(), anyLong())).thenReturn(CLAIM_TOKEN);
    when(applicationDispatcher.renewLease(any())).thenReturn(true);
  }

  /**
   * Test case: tests the createNewTask method. Test data: a non-existent submission. Expected
   * result: an IllgealSubmissionException is thrown and no compilation is attempted.
   */
  @Test
  public void testCreateNewTaskIllegalSubmission() throws Exception {
    when(submissionMapper.getSubmission(anyLong())).thenReturn(null);

    Assertions.assertThrows(
        IllgealSubmissionException.class, () -> dispatcher.createNewTask(404L, GENERATION, NOT_LAST_DELIVERY));
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

    dispatcher.createNewTask(100L, GENERATION, NOT_LAST_DELIVERY);

    verify(submissionMapper, times(1)).getSubmission(100L);
    verify(applicationDispatcher).onCompileFinished(any(), any());
    verify(applicationDispatcher).onOneTestPointFinished(argThat(s -> s.getSubmissionId() == 100L), eq(1), any());
    // The test data is read from the directory the checkpoint store synced it to.
    String checkpointDirectory = "/tmp/voj-dispatcher-tests/1000/";
    verify(programRunner)
        .getRuntimeResult(
            any(),
            anyString(),
            anyString(),
            eq(checkpointDirectory + "input#1.txt"),
            anyString());
    verify(comparator)
        .getComparisonResult(eq(checkpointDirectory + "output#1.txt"), anyString(), eq(true));

    @SuppressWarnings("unchecked")
    org.mockito.ArgumentCaptor<List<Map<String, Object>>> resultsCaptor =
        org.mockito.ArgumentCaptor.forClass(List.class);
    verify(applicationDispatcher).onAllTestPointsFinished(any(), resultsCaptor.capture());
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

    dispatcher.createNewTask(100L, GENERATION, NOT_LAST_DELIVERY);

    @SuppressWarnings("unchecked")
    org.mockito.ArgumentCaptor<Map<String, Object>> resultCaptor =
        org.mockito.ArgumentCaptor.forClass(Map.class);
    verify(applicationDispatcher).onOneTestPointFinished(argThat(s -> s.getSubmissionId() == 100L), eq(1), resultCaptor.capture());
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

    dispatcher.createNewTask(100L, GENERATION, NOT_LAST_DELIVERY);

    @SuppressWarnings("unchecked")
    org.mockito.ArgumentCaptor<List<Map<String, Object>>> resultsCaptor =
        org.mockito.ArgumentCaptor.forClass(List.class);
    verify(applicationDispatcher).onAllTestPointsFinished(any(), resultsCaptor.capture());
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

    dispatcher.createNewTask(100L, GENERATION, NOT_LAST_DELIVERY);

    verify(applicationDispatcher).onCompileFinished(any(), any());
    verify(programRunner, never())
        .getRuntimeResult(any(), anyString(), anyString(), anyString(), anyString());
    verify(applicationDispatcher, never()).onAllTestPointsFinished(any(), any());
    verify(applicationDispatcher, never()).onErrorOccurred(any(), anyString());
  }

  /**
   * Test case: tests the createNewTask method. Test data: a problem without any checkpoint.
   * Expected result: a System Error, without compiling; never an Accepted with nothing checked.
   */
  @Test
  public void testCreateNewTaskWithoutCheckpointsIsSystemError() throws Exception {
    when(checkpointStore.getCheckpoints(1000L)).thenReturn(new ArrayList<>());

    dispatcher.createNewTask(100L, GENERATION, NOT_LAST_DELIVERY);

    verify(applicationDispatcher).onErrorOccurred(any(), contains("no test data"));
    verify(compiler, never()).getCompileResult(any(), anyString(), anyString());
    verify(applicationDispatcher, never()).onAllTestPointsFinished(any(), any());
  }

  /**
   * Test case: tests the createNewTask method. Test data: the web application has no test data
   * for the problem (or a malformed archive). Expected result: a System Error.
   */
  @Test
  public void testCreateNewTaskWithInvalidCheckpointsIsSystemError() throws Exception {
    org.mockito.Mockito.doThrow(new InvalidCheckpointsException("HTTP 404", null))
        .when(checkpointStore)
        .sync(any());

    dispatcher.createNewTask(100L, GENERATION, NOT_LAST_DELIVERY);

    verify(applicationDispatcher).onErrorOccurred(any(), contains("could not be loaded"));
    verify(compiler, never()).getCompileResult(any(), anyString(), anyString());
  }

  /**
   * Test case: tests the createNewTask method. Test data: the web application refuses this judger
   * the test data (HTTP 403: its API token is wrong or empty), or refuses the request otherwise
   * (HTTP 400). Expected result: a System Error, at once, whose judge log says that the judger was
   * refused (and to check its API token for a 403), rather than tries until the task expires.
   */
  @Test
  public void testCreateNewTaskWithRefusedCheckpointsIsSystemError() throws Exception {
    org.mockito.Mockito.doThrow(
            new CheckpointsRefusedException("HTTP 403", 403),
            new CheckpointsRefusedException("HTTP 400", 400))
        .when(checkpointStore)
        .sync(any());

    dispatcher.createNewTask(100L, GENERATION, NOT_LAST_DELIVERY);
    dispatcher.createNewTask(100L, GENERATION, NOT_LAST_DELIVERY);

    org.mockito.ArgumentCaptor<String> reasons = org.mockito.ArgumentCaptor.forClass(String.class);
    verify(applicationDispatcher, times(2)).onErrorOccurred(any(), reasons.capture());
    Assertions.assertTrue(reasons.getAllValues().get(0).contains("refused"));
    Assertions.assertTrue(reasons.getAllValues().get(0).contains("judger.apiToken"));
    Assertions.assertTrue(reasons.getAllValues().get(1).contains("(HTTP 400)"));
    Assertions.assertFalse(reasons.getAllValues().get(1).contains("judger.apiToken"));
    verify(compiler, never()).getCompileResult(any(), anyString(), anyString());
  }

  /**
   * Test case: tests the createNewTask method. Test data: the test data cannot be downloaded now
   * (e.g. the web application is restarting). Expected result: no System Error; the exception is
   * passed on so that the task is delivered again. (The test data is synced before the work
   * directory is created, so there is none to remove yet; the removal of one after a failure is
   * tested by testCreateNewTaskCleansUpOnFailure.)
   */
  @Test
  public void testCreateNewTaskWithUnavailableCheckpointsIsRetried() throws Exception {
    org.mockito.Mockito.doThrow(new java.io.IOException("HTTP 503"))
        .when(checkpointStore)
        .sync(any());

    Assertions.assertThrows(
        RetryableJudgingException.class, () -> dispatcher.createNewTask(100L, GENERATION, NOT_LAST_DELIVERY));

    verify(applicationDispatcher, never()).onErrorOccurred(any(), anyString());
    verify(preprocessor, never()).createTestCode(any(), anyString(), anyString());
    verify(compiler, never()).getCompileResult(any(), anyString(), anyString());
    // The submission waits for a judger again, not for this judging.
    verify(applicationDispatcher).onJudgingHandedBack(argThat(s -> s.getSubmissionId() == 100L));
  }

  /**
   * Test case: tests the createNewTask method. Test data: the test data cannot be downloaded now,
   * on the last delivery of the task. Expected result: rather than handed back to be
   * dead-lettered, the submission is a System Error saying why.
   */
  @Test
  public void testCreateNewTaskIsNotRetriedOnItsLastDelivery() throws Exception {
    org.mockito.Mockito.doThrow(new java.io.IOException("HTTP 503"))
        .when(checkpointStore)
        .sync(any());

    dispatcher.createNewTask(100L, GENERATION, true);

    verify(applicationDispatcher).onErrorOccurred(any(), contains("could not be downloaded"));
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

    dispatcher.createNewTask(100L, GENERATION, NOT_LAST_DELIVERY);

    verify(applicationDispatcher).onErrorOccurred(any(), contains("compiler"));
    verify(applicationDispatcher, never()).onCompileFinished(any(), any());
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

    dispatcher.createNewTask(100L, GENERATION, NOT_LAST_DELIVERY);

    verify(applicationDispatcher).onErrorOccurred(any(), contains("could not be read"));
    verify(applicationDispatcher, never()).onAllTestPointsFinished(any(), any());
  }

  /**
   * Test case: tests the createNewTask method. Test data: a stage throws after the work directory
   * was created. Expected result: a System Error, and the work directory is still removed.
   */
  @Test
  public void testCreateNewTaskCleansUpOnFailure(@TempDir Path tempDir) throws Exception {
    when(checkpointStore.getWorkDirectory()).thenReturn(tempDir);
    org.mockito.Mockito.doAnswer(
            invocation -> {
              String workDirectory = invocation.getArgument(1);
              Path directory = Files.createDirectory(Path.of(workDirectory));
              Files.writeString(directory.resolve("Main.cpp"), "int main() {}");
              return null;
            })
        .when(preprocessor)
        .createTestCode(any(), anyString(), anyString());
    when(checkpointStore.getCheckpoints(1000L)).thenReturn(oneCheckpoint());
    when(compiler.getCompileResult(any(), anyString(), anyString()))
        .thenThrow(new IllegalStateException("compiler crashed"));

    dispatcher.createNewTask(100L, GENERATION, NOT_LAST_DELIVERY);

    verify(applicationDispatcher).onErrorOccurred(any(), eq(Dispatcher.INTERNAL_ERROR_REASON));
    try (java.util.stream.Stream<Path> leftovers = Files.list(tempDir)) {
      Assertions.assertEquals(0, leftovers.count());
    }
  }

  /**
   * Test case: tests the createNewTask method. Test data: the same submission judged twice (as by
   * two judgers on one host). Expected result: each judging gets a work directory of its own.
   */
  @Test
  public void testEachJudgingHasItsOwnWorkDirectory() throws Exception {
    when(checkpointStore.getCheckpoints(1000L)).thenReturn(oneCheckpoint());
    when(compiler.getCompileResult(any(), anyString(), anyString()))
        .thenReturn(compileResult(false));

    dispatcher.createNewTask(100L, GENERATION, NOT_LAST_DELIVERY);
    dispatcher.createNewTask(100L, GENERATION, NOT_LAST_DELIVERY);

    org.mockito.ArgumentCaptor<String> workDirectories =
        org.mockito.ArgumentCaptor.forClass(String.class);
    verify(compiler, times(2)).getCompileResult(any(), workDirectories.capture(), anyString());
    String first = workDirectories.getAllValues().get(0);
    String second = workDirectories.getAllValues().get(1);
    Assertions.assertTrue(first.startsWith("/tmp/voj-dispatcher-tests/voj-100-"), first);
    Assertions.assertNotEquals(first, second);
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
        .onCompileFinished(any(), any());

    Assertions.assertThrows(
        org.springframework.dao.DataAccessException.class,
        () -> dispatcher.createNewTask(100L, GENERATION, NOT_LAST_DELIVERY));
    verify(applicationDispatcher, never()).onErrorOccurred(any(), anyString());
  }

  /**
   * Test case: tests the createNewTask method. Test data: a submission that is no longer pending,
   * e.g. its task was delivered a second time after it had been judged. Expected result: it is not
   * judged (nor its verdict overwritten) again.
   */
  @Test
  public void testCreateNewTaskSkipsSubmissionsThatAreNotPending() throws Exception {
    when(applicationDispatcher.onJudgingStarted(anyLong(), anyLong())).thenReturn(null);

    dispatcher.createNewTask(100L, GENERATION, NOT_LAST_DELIVERY);

    verify(submissionMapper, never()).getSubmission(anyLong());
    verify(checkpointStore, never()).sync(any());
    verify(compiler, never()).getCompileResult(any(), anyString(), anyString());
    verify(applicationDispatcher, never()).onErrorOccurred(any(), anyString());
  }

  /**
   * Test case: tests the createNewTask method. Test data: a pending submission. Expected result:
   * it is read only once it is claimed (an edit and a rejudge just before the claim are judged),
   * and judged with the claim token, which identifies the judging.
   */
  @Test
  public void testCreateNewTaskReadsTheSubmissionOnlyOnceClaimed() throws Exception {
    when(checkpointStore.getCheckpoints(1000L)).thenReturn(oneCheckpoint());
    when(compiler.getCompileResult(any(), anyString(), anyString()))
        .thenReturn(compileResult(false));

    dispatcher.createNewTask(100L, GENERATION, NOT_LAST_DELIVERY);

    InOrder inOrder = org.mockito.Mockito.inOrder(applicationDispatcher, submissionMapper);
    inOrder.verify(applicationDispatcher).onJudgingStarted(100L, GENERATION);
    inOrder.verify(submissionMapper).getSubmission(100L);
    verify(applicationDispatcher)
        .onCompileFinished(
            argThat(s -> s.getClaimToken() == CLAIM_TOKEN), any());
  }

  /**
   * Test case: tests the createNewTask method. Test data: a stale task, queued before the
   * submission was rejudged (its generation is not the submission's any more), so the claim
   * matches nothing. Expected result: the task is skipped, the submission not even read.
   */
  @Test
  public void testCreateNewTaskSkipsStaleTasks() throws Exception {
    when(applicationDispatcher.onJudgingStarted(100L, 2L)).thenReturn(null);

    dispatcher.createNewTask(100L, 2L, NOT_LAST_DELIVERY);

    verify(submissionMapper, never()).getSubmission(anyLong());
    verify(compiler, never()).getCompileResult(any(), anyString(), anyString());
  }

  /**
   * Test case: tests the createNewTask method. Test data: two checkpoints. Expected result: the
   * lease of the judging is renewed after the download of the test data, after the compilation and
   * after each checkpoint, before the step is reported.
   */
  @Test
  public void testCreateNewTaskRenewsTheLeaseAfterEachStep() throws Exception {
    List<Checkpoint> checkpoints = new ArrayList<>(oneCheckpoint());
    checkpoints.add(new Checkpoint(1000L, 2, true, 100, "input", "output"));
    when(checkpointStore.getCheckpoints(1000L)).thenReturn(checkpoints);
    when(compiler.getCompileResult(any(), anyString(), anyString()))
        .thenReturn(compileResult(true));
    when(programRunner.getRuntimeResult(any(), anyString(), anyString(), anyString(), anyString()))
        .thenReturn(runtimeResult("AC"));
    when(comparator.getComparisonResult(anyString(), anyString(), anyBoolean())).thenReturn("AC");

    dispatcher.createNewTask(100L, GENERATION, NOT_LAST_DELIVERY);

    InOrder inOrder =
        org.mockito.Mockito.inOrder(
            checkpointStore, compiler, programRunner, applicationDispatcher);
    inOrder.verify(checkpointStore).sync(any());
    inOrder.verify(applicationDispatcher).renewLease(any());
    inOrder.verify(compiler).getCompileResult(any(), anyString(), anyString());
    inOrder.verify(applicationDispatcher).renewLease(any());
    inOrder.verify(applicationDispatcher).onCompileFinished(any(), any());
    for (int checkpointId = 1; checkpointId <= 2; ++checkpointId) {
      inOrder
          .verify(programRunner)
          .getRuntimeResult(any(), anyString(), anyString(), anyString(), anyString());
      inOrder.verify(applicationDispatcher).renewLease(any());
      inOrder.verify(applicationDispatcher).onOneTestPointFinished(any(), eq(checkpointId), any());
    }
    inOrder.verify(applicationDispatcher).onAllTestPointsFinished(any(), any());
  }

  /**
   * Test case: tests the createNewTask method. Test data: a judging that finds, when it renews its
   * lease after the first of two checkpoints, that it was superseded (e.g. the submission was
   * rejudged). Expected result: it stops at once, without reporting that checkpoint, running the
   * next one, recording a verdict or a System Error, or passing on an exception; its work
   * directory is removed.
   */
  @Test
  public void testCreateNewTaskStopsOnceSuperseded(@TempDir Path tempDir) throws Exception {
    when(checkpointStore.getWorkDirectory()).thenReturn(tempDir);
    List<Checkpoint> checkpoints = new ArrayList<>(oneCheckpoint());
    checkpoints.add(new Checkpoint(1000L, 2, true, 100, "input", "output"));
    when(checkpointStore.getCheckpoints(1000L)).thenReturn(checkpoints);
    when(compiler.getCompileResult(any(), anyString(), anyString()))
        .thenReturn(compileResult(true));
    when(programRunner.getRuntimeResult(any(), anyString(), anyString(), anyString(), anyString()))
        .thenReturn(runtimeResult("AC"));
    when(comparator.getComparisonResult(anyString(), anyString(), anyBoolean())).thenReturn("AC");
    // Current after the download and the compilation, superseded after the first checkpoint.
    when(applicationDispatcher.renewLease(any())).thenReturn(true, true, false);
    org.mockito.Mockito.doAnswer(
            invocation -> {
              Files.createDirectories(Path.of(invocation.<String>getArgument(1)));
              return null;
            })
        .when(preprocessor)
        .createTestCode(any(), anyString(), anyString());

    dispatcher.createNewTask(100L, GENERATION, NOT_LAST_DELIVERY);

    verify(programRunner, times(1))
        .getRuntimeResult(any(), anyString(), anyString(), anyString(), anyString());
    verify(applicationDispatcher, never()).onOneTestPointFinished(any(), anyInt(), any());
    verify(applicationDispatcher, never()).onAllTestPointsFinished(any(), any());
    verify(applicationDispatcher, never()).onErrorOccurred(any(), anyString());
    try (java.util.stream.Stream<Path> entries = Files.list(tempDir)) {
      Assertions.assertEquals(0, entries.count());
    }
  }

  /**
   * Test case: tests the createNewTask method. Test data: a judging that was superseded while it
   * downloaded the test data. Expected result: it neither compiles nor reports anything.
   */
  @Test
  public void testCreateNewTaskSupersededWhileDownloadingIsNotCompiled() throws Exception {
    when(checkpointStore.getCheckpoints(1000L)).thenReturn(oneCheckpoint());
    when(applicationDispatcher.renewLease(any())).thenReturn(false);

    dispatcher.createNewTask(100L, GENERATION, NOT_LAST_DELIVERY);

    verify(compiler, never()).getCompileResult(any(), anyString(), anyString());
    verify(applicationDispatcher, never()).onCompileFinished(any(), any());
    verify(applicationDispatcher, never()).onErrorOccurred(any(), anyString());
  }

  /**
   * Test case: tests the createNewTask method. Test data: this judger's work directory cannot be
   * created. Expected result: the submission is not even claimed; the exception is passed on so
   * that the task is delivered again, to another judger.
   */
  @Test
  public void testCreateNewTaskWithoutWorkDirectoryIsRetried() throws Exception {
    when(checkpointStore.getWorkDirectory()).thenThrow(new java.io.IOException("read-only"));

    Assertions.assertThrows(
        RetryableJudgingException.class, () -> dispatcher.createNewTask(100L, GENERATION, NOT_LAST_DELIVERY));

    verify(applicationDispatcher, never()).onJudgingStarted(anyLong(), anyLong());
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

  /** The judging generation of the tasks of the tests. */
  private static final long GENERATION = 3L;

  /** Whether the deliveries of the tests are the last of their task: they are not. */
  private static final boolean NOT_LAST_DELIVERY = false;

  /** The claim token of the judgings that the tests claim. */
  private static final Long CLAIM_TOKEN = -4242L;

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
