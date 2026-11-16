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
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.mockingDetails;
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
import java.util.concurrent.atomic.AtomicBoolean;

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
import org.springframework.test.util.ReflectionTestUtils;

import org.verwandlung.voj.judger.application.ApplicationDispatcher;
import org.verwandlung.voj.judger.exception.CheckpointsRefusedException;
import org.verwandlung.voj.judger.exception.InvalidCheckpointsException;
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
  /**
   * Injects the working-directory and the judging-queue configuration that would normally come from
   * the property files, and a pending submission to claim: its first attempt.
   */
  @BeforeEach
  public void setUp() throws Exception {
    ReflectionTestUtils.setField(dispatcher, "pollInterval", 10L);
    ReflectionTestUtils.setField(dispatcher, "leaseTimeout", LEASE_TIMEOUT);
    ReflectionTestUtils.setField(dispatcher, "maxAttempts", MAX_ATTEMPTS);
    when(checkpointStore.getWorkDirectory()).thenReturn(Path.of("/tmp/voj-dispatcher-tests"));
    when(checkpointStore.sync(any())).thenReturn(Path.of("/tmp/voj-dispatcher-tests/1000"));
    when(applicationDispatcher.claimSubmission(anyList(), anyLong()))
        .thenAnswer(invocation -> buildSubmission(1));
    when(applicationDispatcher.renewLease(any(), anyLong())).thenReturn(true);
  }

  /**
   * Test case: tests the judgeNextSubmission method. Test data: no pending submission in the
   * languages of the judger. Expected result: nothing is judged, and it says so.
   */
  @Test
  public void testJudgeNextSubmissionWithoutPendingSubmission() throws Exception {
    when(applicationDispatcher.claimSubmission(anyList(), anyLong())).thenReturn(null);

    Assertions.assertFalse(dispatcher.judgeNextSubmission(LANGUAGES));
    verify(applicationDispatcher).claimSubmission(LANGUAGES, LEASE_SECONDS);
    verify(checkpointStore, never()).sync(any());
    verify(compiler, never()).getCompileResult(any(), anyString(), anyString());
  }

  /**
   * Test case: tests the judgeNextSubmission method. Test data: a submission that compiles
   * successfully and produces the correct output. Expected result: every judging stage is
   * dispatched and the runtime result is reported as accepted.
   */
  @Test
  public void testJudgeNextSubmissionAccepted() throws Exception {
    when(compiler.getCompileResult(any(), anyString(), anyString()))
        .thenReturn(compileResult(true));

    List<Checkpoint> checkpoints = new ArrayList<>();
    checkpoints.add(new Checkpoint(1000L, 1, true, 100, "input", "output"));
    when(checkpointStore.getCheckpoints(1000L)).thenReturn(checkpoints);

    when(programRunner.getRuntimeResult(any(), anyString(), anyString(), anyString(), anyString()))
        .thenReturn(runtimeResult("AC"));
    when(comparator.getComparisonResult(anyString(), anyString(), anyBoolean())).thenReturn("AC");

    dispatcher.judgeNextSubmission(LANGUAGES);

    verify(applicationDispatcher).onCompileFinished(any(), any());
    verify(applicationDispatcher).onOneTestPointFinished(argThat(s -> s.getSubmissionId() == 100L), eq(1), any());
    // The test data is read from the directory the checkpoint store synced it to.
    Path checkpointDirectory = Path.of("/tmp/voj-dispatcher-tests/1000");
    verify(programRunner)
        .getRuntimeResult(
            any(),
            anyString(),
            anyString(),
            eq(checkpointDirectory.resolve("input#1.txt").toString()),
            anyString());
    verify(comparator)
        .getComparisonResult(
            eq(checkpointDirectory.resolve("output#1.txt").toString()), anyString(), eq(true));

    @SuppressWarnings("unchecked")
    org.mockito.ArgumentCaptor<List<Map<String, Object>>> resultsCaptor =
        org.mockito.ArgumentCaptor.forClass(List.class);
    verify(applicationDispatcher).onAllTestPointsFinished(any(), resultsCaptor.capture());
    Assertions.assertEquals("AC", resultsCaptor.getValue().get(0).get("runtimeResult"));
    Assertions.assertEquals(100, resultsCaptor.getValue().get(0).get("score"));
  }

  /**
   * Test case: tests the judgeNextSubmission method. Test data: a submission that compiles
   * successfully but produces incorrect output. Expected result: an accepted runtime result is
   * downgraded to a wrong answer.
   */
  @Test
  public void testJudgeNextSubmissionWrongAnswer() throws Exception {
    when(compiler.getCompileResult(any(), anyString(), anyString()))
        .thenReturn(compileResult(true));

    List<Checkpoint> checkpoints = new ArrayList<>();
    checkpoints.add(new Checkpoint(1000L, 1, true, 100, "input", "output"));
    when(checkpointStore.getCheckpoints(1000L)).thenReturn(checkpoints);

    when(programRunner.getRuntimeResult(any(), anyString(), anyString(), anyString(), anyString()))
        .thenReturn(runtimeResult("AC"));
    when(comparator.getComparisonResult(anyString(), anyString(), anyBoolean())).thenReturn("WA");

    dispatcher.judgeNextSubmission(LANGUAGES);

    @SuppressWarnings("unchecked")
    org.mockito.ArgumentCaptor<Map<String, Object>> resultCaptor =
        org.mockito.ArgumentCaptor.forClass(Map.class);
    verify(applicationDispatcher).onOneTestPointFinished(argThat(s -> s.getSubmissionId() == 100L), eq(1), resultCaptor.capture());
    Assertions.assertEquals("WA", resultCaptor.getValue().get("runtimeResult"));
  }

  /**
   * Test case: tests the judgeNextSubmission method. Test data: two checkpoints, only the first
   * requiring an exact match, whose output differs only in presentation. Expected result: each
   * checkpoint's exactlyMatch flag is passed to the comparator and its verdict (PE / AC) is
   * reported.
   */
  @Test
  public void testJudgeNextSubmissionHonoursExactlyMatch() throws Exception {
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

    dispatcher.judgeNextSubmission(LANGUAGES);

    @SuppressWarnings("unchecked")
    org.mockito.ArgumentCaptor<List<Map<String, Object>>> resultsCaptor =
        org.mockito.ArgumentCaptor.forClass(List.class);
    verify(applicationDispatcher).onAllTestPointsFinished(any(), resultsCaptor.capture());
    Assertions.assertEquals("PE", resultsCaptor.getValue().get(0).get("runtimeResult"));
    Assertions.assertEquals("AC", resultsCaptor.getValue().get(1).get("runtimeResult"));
  }

  /**
   * Test case: tests the judgeNextSubmission method. Test data: a submission that fails to compile.
   * Expected result: the program is never executed.
   */
  @Test
  public void testJudgeNextSubmissionCompileFailed() throws Exception {
    when(checkpointStore.getCheckpoints(1000L)).thenReturn(oneCheckpoint());
    when(compiler.getCompileResult(any(), anyString(), anyString()))
        .thenReturn(compileResult(false));

    dispatcher.judgeNextSubmission(LANGUAGES);

    verify(applicationDispatcher).onCompileFinished(any(), any());
    verify(programRunner, never())
        .getRuntimeResult(any(), anyString(), anyString(), anyString(), anyString());
    verify(applicationDispatcher, never()).onAllTestPointsFinished(any(), any());
    verify(applicationDispatcher, never()).onErrorOccurred(any(), anyString());
  }

  /**
   * Test case: tests the judgeNextSubmission method. Test data: a problem without any checkpoint.
   * Expected result: a System Error, without compiling; never an Accepted with nothing checked.
   */
  @Test
  public void testJudgeNextSubmissionWithoutCheckpointsIsSystemError() throws Exception {
    when(checkpointStore.getCheckpoints(1000L)).thenReturn(new ArrayList<>());

    dispatcher.judgeNextSubmission(LANGUAGES);

    verify(applicationDispatcher).onErrorOccurred(any(), contains("no test data"));
    verify(compiler, never()).getCompileResult(any(), anyString(), anyString());
    verify(applicationDispatcher, never()).onAllTestPointsFinished(any(), any());
  }

  /**
   * Test case: tests the judgeNextSubmission method. Test data: the web application has no test
   * data for the problem (or a malformed archive). Expected result: a System Error.
   */
  @Test
  public void testJudgeNextSubmissionWithInvalidCheckpointsIsSystemError() throws Exception {
    org.mockito.Mockito.doThrow(new InvalidCheckpointsException("HTTP 404", null))
        .when(checkpointStore)
        .sync(any());

    dispatcher.judgeNextSubmission(LANGUAGES);

    verify(applicationDispatcher).onErrorOccurred(any(), contains("could not be loaded"));
    verify(compiler, never()).getCompileResult(any(), anyString(), anyString());
  }

  /**
   * Test case: tests the judgeNextSubmission method. Test data: the web application refuses this
   * judger the test data (HTTP 403: its API token is wrong or empty), or refuses the request
   * otherwise (HTTP 400). Expected result: a System Error, at once, whose judge log says that the
   * judger was refused (and to check its API token for a 403), rather than tries again.
   */
  @Test
  public void testJudgeNextSubmissionWithRefusedCheckpointsIsSystemError() throws Exception {
    org.mockito.Mockito.doThrow(
            new CheckpointsRefusedException("HTTP 403", 403),
            new CheckpointsRefusedException("HTTP 400", 400))
        .when(checkpointStore)
        .sync(any());

    dispatcher.judgeNextSubmission(LANGUAGES);
    dispatcher.judgeNextSubmission(LANGUAGES);

    org.mockito.ArgumentCaptor<String> reasons = org.mockito.ArgumentCaptor.forClass(String.class);
    verify(applicationDispatcher, times(2)).onErrorOccurred(any(), reasons.capture());
    Assertions.assertTrue(reasons.getAllValues().get(0).contains("refused"));
    Assertions.assertTrue(reasons.getAllValues().get(0).contains("judger.apiToken"));
    Assertions.assertTrue(reasons.getAllValues().get(1).contains("(HTTP 400)"));
    Assertions.assertFalse(reasons.getAllValues().get(1).contains("judger.apiToken"));
    verify(compiler, never()).getCompileResult(any(), anyString(), anyString());
  }

  /**
   * Test case: tests the judgeNextSubmission method. Test data: the test data cannot be downloaded
   * now (e.g. the web application is restarting), on the first attempt. Expected result: no System
   * Error; the lease is no longer renewed, so that the submission is judged again once it expired.
   * (The test data is synced before the work directory is created, so there is none to remove yet;
   * the removal of one after a failure is tested by testJudgeNextSubmissionCleansUpOnFailure.)
   */
  @Test
  public void testJudgeNextSubmissionWithUnavailableCheckpointsIsRetried() throws Exception {
    org.mockito.Mockito.doThrow(new java.io.IOException("HTTP 503"))
        .when(checkpointStore)
        .sync(any());

    Assertions.assertTrue(dispatcher.judgeNextSubmission(LANGUAGES));

    verify(applicationDispatcher, never()).onErrorOccurred(any(), anyString());
    verify(preprocessor, never()).createTestCode(any(), anyString(), anyString());
    verify(compiler, never()).getCompileResult(any(), anyString(), anyString());
    int renewals = numberOfRenewals();
    Thread.sleep(LEASE_TIMEOUT);
    Assertions.assertEquals(renewals, numberOfRenewals());
  }

  /**
   * Test case: tests the judgeNextSubmission method. Test data: the test data cannot be downloaded
   * now, on the last attempt. Expected result: rather than judged once more, the submission is a
   * System Error saying why, and after how many attempts.
   */
  @Test
  public void testJudgeNextSubmissionIsNotRetriedAfterItsLastAttempt() throws Exception {
    when(applicationDispatcher.claimSubmission(anyList(), anyLong()))
        .thenReturn(buildSubmission(MAX_ATTEMPTS));
    org.mockito.Mockito.doThrow(new java.io.IOException("HTTP 503"))
        .when(checkpointStore)
        .sync(any());

    dispatcher.judgeNextSubmission(LANGUAGES);

    verify(applicationDispatcher)
        .onErrorOccurred(
            any(),
            argThat(
                reason ->
                    reason.startsWith("The test data could not be downloaded.")
                        && reason.contains("after 3 attempts")));
    verify(compiler, never()).getCompileResult(any(), anyString(), anyString());
  }

  /**
   * Test case: tests the judgeNextSubmission method. Test data: a submission claimed once more than
   * the attempts allow, as its last attempt stopped without an outcome (its judger died, or lost
   * the database until the lease expired). Expected result: it is not judged again but a System
   * Error that says so.
   */
  @Test
  public void testJudgeNextSubmissionGivesUpAfterAnAbandonedLastAttempt() throws Exception {
    when(applicationDispatcher.claimSubmission(anyList(), anyLong()))
        .thenReturn(buildSubmission(MAX_ATTEMPTS + 1));

    Assertions.assertTrue(dispatcher.judgeNextSubmission(LANGUAGES));

    verify(applicationDispatcher)
        .onErrorOccurred(
            argThat(s -> s.getClaimToken() == CLAIM_TOKEN),
            argThat(
                reason ->
                    reason.startsWith(Dispatcher.ABANDONED_REASON)
                        && reason.contains("after 3 attempts")));
    verify(checkpointStore, never()).sync(any());
    verify(applicationDispatcher, never()).renewLease(any(), anyLong());
  }

  /**
   * Test case: tests the judgeNextSubmission method. Test data: the sandbox fails to run the
   * compiler. Expected result: a System Error rather than a Compile Error with an empty log.
   */
  @Test
  public void testJudgeNextSubmissionCompilerSandboxFailureIsSystemError() throws Exception {
    when(checkpointStore.getCheckpoints(1000L)).thenReturn(oneCheckpoint());
    Map<String, Object> result = compileResult(false);
    result.put("verdict", Verdict.INTERNAL_ERROR);
    when(compiler.getCompileResult(any(), anyString(), anyString())).thenReturn(result);

    dispatcher.judgeNextSubmission(LANGUAGES);

    verify(applicationDispatcher).onErrorOccurred(any(), contains("compiler"));
    verify(applicationDispatcher, never()).onCompileFinished(any(), any());
  }

  /**
   * Test case: tests the judgeNextSubmission method. Test data: the expected output cannot be read.
   * Expected result: a System Error rather than a Wrong Answer.
   */
  @Test
  public void testJudgeNextSubmissionUnreadableAnswerIsSystemError() throws Exception {
    when(checkpointStore.getCheckpoints(1000L)).thenReturn(oneCheckpoint());
    when(compiler.getCompileResult(any(), anyString(), anyString()))
        .thenReturn(compileResult(true));
    when(programRunner.getRuntimeResult(any(), anyString(), anyString(), anyString(), anyString()))
        .thenReturn(runtimeResult("AC"));
    when(comparator.getComparisonResult(anyString(), anyString(), anyBoolean()))
        .thenThrow(new java.io.IOException("No such file"));

    dispatcher.judgeNextSubmission(LANGUAGES);

    verify(applicationDispatcher).onErrorOccurred(any(), contains("could not be read"));
    verify(applicationDispatcher, never()).onAllTestPointsFinished(any(), any());
  }

  /**
   * Test case: tests the judgeNextSubmission method. Test data: a stage throws after the work
   * directory was created. Expected result: a System Error, and the work directory is still
   * removed.
   */
  @Test
  public void testJudgeNextSubmissionCleansUpOnFailure(@TempDir Path tempDir) throws Exception {
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

    dispatcher.judgeNextSubmission(LANGUAGES);

    verify(applicationDispatcher).onErrorOccurred(any(), eq(Dispatcher.INTERNAL_ERROR_REASON));
    try (java.util.stream.Stream<Path> leftovers = Files.list(tempDir)) {
      Assertions.assertEquals(0, leftovers.count());
    }
  }

  /**
   * Test case: tests the judgeNextSubmission method. Test data: the same submission judged twice
   * (as by two judgers on one host). Expected result: each judging gets a work directory of its
   * own.
   */
  @Test
  public void testEachJudgingHasItsOwnWorkDirectory() throws Exception {
    when(checkpointStore.getCheckpoints(1000L)).thenReturn(oneCheckpoint());
    when(compiler.getCompileResult(any(), anyString(), anyString()))
        .thenReturn(compileResult(false));

    dispatcher.judgeNextSubmission(LANGUAGES);
    dispatcher.judgeNextSubmission(LANGUAGES);

    org.mockito.ArgumentCaptor<String> workDirectories =
        org.mockito.ArgumentCaptor.forClass(String.class);
    verify(compiler, times(2)).getCompileResult(any(), workDirectories.capture(), anyString());
    String first = workDirectories.getAllValues().get(0);
    String second = workDirectories.getAllValues().get(1);
    Assertions.assertTrue(
        first.startsWith(Path.of("/tmp/voj-dispatcher-tests", "voj-100-").toString()), first);
    Assertions.assertNotEquals(first, second);
  }

  /**
   * Test case: tests the judgeNextSubmission method. Test data: the database becomes unreachable
   * while the verdict is recorded. Expected result: no System Error is attempted (it could not be
   * recorded either): the submission is claimed again once its lease expired.
   */
  @Test
  public void testJudgeNextSubmissionLeavesDatabaseFailuresToTheLease() throws Exception {
    when(checkpointStore.getCheckpoints(1000L)).thenReturn(oneCheckpoint());
    when(compiler.getCompileResult(any(), anyString(), anyString()))
        .thenReturn(compileResult(false));
    org.mockito.Mockito.doThrow(
            new org.springframework.dao.DataAccessResourceFailureException("database down"))
        .when(applicationDispatcher)
        .onCompileFinished(any(), any());

    Assertions.assertTrue(dispatcher.judgeNextSubmission(LANGUAGES));
    verify(applicationDispatcher, never()).onErrorOccurred(any(), anyString());
  }

  /**
   * Test case: tests the judgeNextSubmission method. Test data: a pending submission. Expected
   * result: it is judged with the claim token of the judging that claimed it, in the languages of
   * the judger.
   */
  @Test
  public void testJudgeNextSubmissionJudgesWithTheClaimToken() throws Exception {
    when(checkpointStore.getCheckpoints(1000L)).thenReturn(oneCheckpoint());
    when(compiler.getCompileResult(any(), anyString(), anyString()))
        .thenReturn(compileResult(false));

    dispatcher.judgeNextSubmission(LANGUAGES);

    verify(applicationDispatcher).claimSubmission(eq(LANGUAGES), anyLong());
    verify(applicationDispatcher)
        .onCompileFinished(argThat(s -> s.getClaimToken() == CLAIM_TOKEN), any());
  }

  /**
   * Test case: tests the judgeNextSubmission method. Test data: a compilation that takes four
   * times as long as the lease of the judging. Expected result: the lease is renewed in the
   * background while the compilation runs, so the judging goes on and records its verdict; the
   * renewals stop once the judging is over.
   */
  @Test
  public void testJudgeNextSubmissionRenewsTheLeaseWhileAStepRuns() throws Exception {
    when(checkpointStore.getCheckpoints(1000L)).thenReturn(oneCheckpoint());
    when(compiler.getCompileResult(any(), anyString(), anyString()))
        .thenAnswer(
            invocation -> {
              Thread.sleep(LEASE_TIMEOUT * 4);
              return compileResult(false);
            });

    dispatcher.judgeNextSubmission(LANGUAGES);

    verify(applicationDispatcher, atLeast(8)).renewLease(any(), eq(LEASE_SECONDS));
    verify(applicationDispatcher).onCompileFinished(any(), any());
    int renewals = numberOfRenewals();
    Thread.sleep(LEASE_TIMEOUT);
    Assertions.assertEquals(renewals, numberOfRenewals());
  }

  /**
   * Test case: tests the judgeNextSubmission method. Test data: a judging that finds, when it
   * renews its lease while the first of two checkpoints runs, that it was superseded (e.g. the
   * submission was rejudged). Expected result: it stops once that run is over, without reporting
   * that checkpoint, running the next one, or recording a verdict or a System Error; its work
   * directory is removed.
   */
  @Test
  public void testJudgeNextSubmissionStopsOnceSuperseded(@TempDir Path tempDir) throws Exception {
    when(checkpointStore.getWorkDirectory()).thenReturn(tempDir);
    List<Checkpoint> checkpoints = new ArrayList<>(oneCheckpoint());
    checkpoints.add(new Checkpoint(1000L, 2, true, 100, "input", "output"));
    when(checkpointStore.getCheckpoints(1000L)).thenReturn(checkpoints);
    when(compiler.getCompileResult(any(), anyString(), anyString()))
        .thenReturn(compileResult(true));
    // Current until the first checkpoint runs, superseded while it runs.
    AtomicBoolean isRejudged = new AtomicBoolean();
    when(applicationDispatcher.renewLease(any(), anyLong()))
        .thenAnswer(invocation -> !isRejudged.get());
    when(programRunner.getRuntimeResult(any(), anyString(), anyString(), anyString(), anyString()))
        .thenAnswer(
            invocation -> {
              isRejudged.set(true);
              Thread.sleep(LEASE_TIMEOUT);
              return runtimeResult("AC");
            });
    when(comparator.getComparisonResult(anyString(), anyString(), anyBoolean())).thenReturn("AC");
    org.mockito.Mockito.doAnswer(
            invocation -> {
              Files.createDirectories(Path.of(invocation.<String>getArgument(1)));
              return null;
            })
        .when(preprocessor)
        .createTestCode(any(), anyString(), anyString());

    dispatcher.judgeNextSubmission(LANGUAGES);

    verify(applicationDispatcher).onCompileFinished(any(), any());
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
   * Test case: tests the judgeNextSubmission method. Test data: a judging that was superseded while
   * it downloaded the test data. Expected result: it neither compiles nor reports anything.
   */
  @Test
  public void testJudgeNextSubmissionSupersededWhileDownloadingIsNotCompiled() throws Exception {
    when(checkpointStore.getCheckpoints(1000L)).thenReturn(oneCheckpoint());
    when(applicationDispatcher.renewLease(any(), anyLong())).thenReturn(false);
    when(checkpointStore.sync(any()))
        .thenAnswer(
            invocation -> {
              Thread.sleep(LEASE_TIMEOUT);
              return Path.of("/tmp/voj-dispatcher-tests/1000");
            });

    dispatcher.judgeNextSubmission(LANGUAGES);

    verify(compiler, never()).getCompileResult(any(), anyString(), anyString());
    verify(applicationDispatcher, never()).onCompileFinished(any(), any());
    verify(applicationDispatcher, never()).onErrorOccurred(any(), anyString());
  }

  /**
   * Test case: tests the judgeNextSubmission method. Test data: this judger's work directory cannot
   * be created. Expected result: no submission is even claimed, so that another judger takes it.
   */
  @Test
  public void testJudgeNextSubmissionWithoutWorkDirectoryClaimsNothing() throws Exception {
    when(checkpointStore.getWorkDirectory()).thenThrow(new java.io.IOException("read-only"));

    Assertions.assertFalse(dispatcher.judgeNextSubmission(LANGUAGES));

    verify(applicationDispatcher, never()).claimSubmission(anyList(), anyLong());
  }

  /**
   * Test case: tests the judgeSubmissions method. Test data: no pending submission at first, then
   * the database is unreachable once, then a pending submission. Expected result: the judger looks
   * again after each of them, and judges the submission.
   */
  @Test
  public void testJudgeSubmissionsLooksAgainUntilThereIsASubmission() throws Exception {
    when(checkpointStore.getCheckpoints(1000L)).thenReturn(oneCheckpoint());
    when(compiler.getCompileResult(any(), anyString(), anyString()))
        .thenReturn(compileResult(false));
    when(applicationDispatcher.claimSubmission(anyList(), anyLong()))
        .thenReturn(null)
        .thenThrow(new org.springframework.dao.DataAccessResourceFailureException("down"))
        .thenReturn(buildSubmission(1))
        .thenReturn(null);

    Thread judging = new Thread(() -> dispatcher.judgeSubmissions(LANGUAGES));
    judging.start();
    try {
      verify(applicationDispatcher, org.mockito.Mockito.timeout(10000))
          .onCompileFinished(any(), any());
    } finally {
      judging.interrupt();
      judging.join(10000);
    }
    Assertions.assertFalse(judging.isAlive());
    verify(applicationDispatcher, atLeast(4)).claimSubmission(LANGUAGES, LEASE_SECONDS);
  }

  /**
   * Gets how many times the lease of a judging was renewed.
   *
   * @return the number of renewals
   */
  private int numberOfRenewals() {
    return (int)
        mockingDetails(applicationDispatcher).getInvocations().stream()
            .filter(invocation -> invocation.getMethod().getName().equals("renewLease"))
            .count();
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
   * Builds a submission referencing Problem#1000, claimed by a judging.
   *
   * @param attempts - how many judgings have taken it, this one included
   * @return a submission object
   */
  private Submission buildSubmission(int attempts) {
    Problem problem = new Problem(1000L, true, "A+B", 1000, 65536, "", "", "", "", "", "");
    Submission submission = new Submission();
    submission.setSubmissionId(100L);
    submission.setProblem(problem);
    submission.setJudgeAttempts(attempts);
    submission.setClaimToken(CLAIM_TOKEN);
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

  /** The languages of the judger of the tests. */
  private static final List<Integer> LANGUAGES = List.of(2, 3);

  /** The lease (ms) of the judgings of the tests: renewed every 100 ms. */
  private static final long LEASE_TIMEOUT = 400;

  /** The lease of the judgings of the tests in whole seconds, rounded up. */
  private static final long LEASE_SECONDS = 1;

  /** How many judgings may take a submission. */
  private static final int MAX_ATTEMPTS = 3;

  /** The claim token of the judgings that the tests claim. */
  private static final long CLAIM_TOKEN = -4242L;

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

  /** The mocked CheckpointStore object. */
  @Mock private CheckpointStore checkpointStore;

  /** The Dispatcher object under test. */
  @InjectMocks private Dispatcher dispatcher;
}
