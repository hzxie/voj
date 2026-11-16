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
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.IntConsumer;

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
import org.verwandlung.voj.judger.exception.NoCheckpointsException;
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
   * Shortens the times of the judging queue, and injects the working directory and a pending
   * submission to claim, without failed judgings. The judger is judging
   * (as in its judging loop), and an exit of the process is recorded rather than done.
   */
  @BeforeEach
  public void setUp() throws Exception {
    IntConsumer processExit = exitStatuses::add;
    ReflectionTestUtils.setField(dispatcher, "processExit", processExit);
    ReflectionTestUtils.setField(dispatcher, "pollInterval", 10L);
    ReflectionTestUtils.setField(dispatcher, "leaseTimeout", LEASE_TIMEOUT);
    ReflectionTestUtils.setField(dispatcher, "firstPause", 10L);
    ReflectionTestUtils.setField(dispatcher, "maxPause", 40L);
    ReflectionTestUtils.setField(dispatcher, "isJudging", true);
    when(checkpointStore.getWorkDirectory()).thenReturn(Path.of("/tmp/voj-dispatcher-tests"));
    when(checkpointStore.sync(any())).thenReturn(Path.of("/tmp/voj-dispatcher-tests/1000"));
    when(applicationDispatcher.claimSubmission(anyList(), anyLong()))
        .thenAnswer(invocation -> buildSubmission());
    when(applicationDispatcher.renewLease(any(), anyLong())).thenReturn(true);
  }

  /**
   * Test case: tests the judgeNextSubmission method. Test data: no pending submission in the
   * languages of the judger. Expected result: nothing is judged, and it says so.
   */
  @Test
  public void testJudgeNextSubmissionWithoutPendingSubmission() throws Exception {
    when(applicationDispatcher.claimSubmission(anyList(), anyLong()))
        .thenReturn(null);

    Assertions.assertFalse(dispatcher.judgeNextSubmission(LANGUAGES));
    verify(applicationDispatcher).claimSubmission(LANGUAGES, LEASE_SECONDS);
    verify(checkpointStore, never()).sync(any());
    verify(compiler, never()).getCompileResult(any(), anyString());
  }

  /**
   * Test case: tests the judgeNextSubmission method. Test data: a submission that compiles
   * successfully and produces the correct output. Expected result: every judging stage is
   * dispatched and the runtime result is reported as accepted.
   */
  @Test
  public void testJudgeNextSubmissionAccepted() throws Exception {
    when(compiler.getCompileResult(any(), anyString()))
        .thenReturn(compileResult(true));

    List<Checkpoint> checkpoints = new ArrayList<>();
    checkpoints.add(new Checkpoint(1000L, 1, true, 100, "input", "output"));
    when(checkpointStore.getCheckpoints(1000L)).thenReturn(checkpoints);

    when(programRunner.getRuntimeResult(any(), anyString(), anyString(), anyString()))
        .thenReturn(runtimeResult(Verdict.NORMAL));
    when(comparator.getComparisonResult(anyString(), anyString(), anyBoolean())).thenReturn("AC");

    dispatcher.judgeNextSubmission(LANGUAGES);

    verify(applicationDispatcher).onCompileFinished(any(), any());
    verify(applicationDispatcher)
        .onOneTestPointFinished(
            argThat(s -> s.getSubmissionId() == 100L),
            eq(new CheckpointResult(1, "AC", 10, 128, 100)));
    // The test data is read from the directory the checkpoint store synced it to.
    Path checkpointDirectory = Path.of("/tmp/voj-dispatcher-tests/1000");
    verify(programRunner)
        .getRuntimeResult(
            any(),
            anyString(),
            eq(checkpointDirectory.resolve("input#1.txt").toString()),
            anyString());
    verify(comparator)
        .getComparisonResult(
            eq(checkpointDirectory.resolve("output#1.txt").toString()), anyString(), eq(true));

    verify(applicationDispatcher)
        .onAllTestPointsFinished(any(), eq(List.of(new CheckpointResult(1, "AC", 10, 128, 100))));
  }

  /**
   * Test case: tests the judgeNextSubmission method. Test data: a submission that compiles
   * successfully but produces incorrect output. Expected result: an accepted runtime result is
   * downgraded to a wrong answer, which earns no score.
   */
  @Test
  public void testJudgeNextSubmissionWrongAnswer() throws Exception {
    when(compiler.getCompileResult(any(), anyString()))
        .thenReturn(compileResult(true));

    List<Checkpoint> checkpoints = new ArrayList<>();
    checkpoints.add(new Checkpoint(1000L, 1, true, 100, "input", "output"));
    when(checkpointStore.getCheckpoints(1000L)).thenReturn(checkpoints);

    when(programRunner.getRuntimeResult(any(), anyString(), anyString(), anyString()))
        .thenReturn(runtimeResult(Verdict.NORMAL));
    when(comparator.getComparisonResult(anyString(), anyString(), anyBoolean())).thenReturn("WA");

    dispatcher.judgeNextSubmission(LANGUAGES);

    verify(applicationDispatcher)
        .onOneTestPointFinished(
            argThat(s -> s.getSubmissionId() == 100L),
            eq(new CheckpointResult(1, "WA", 10, 128, 0)));
  }

  /**
   * Test case: tests the judgeNextSubmission method. Test data: two checkpoints, only the first
   * requiring an exact match, whose output differs only in presentation. Expected result: each
   * checkpoint's exactlyMatch flag is passed to the comparator and its verdict (PE / AC) is
   * reported.
   */
  @Test
  public void testJudgeNextSubmissionHonoursExactlyMatch() throws Exception {
    when(compiler.getCompileResult(any(), anyString()))
        .thenReturn(compileResult(true));

    List<Checkpoint> checkpoints = new ArrayList<>();
    checkpoints.add(new Checkpoint(1000L, 1, true, 50, "input", "output"));
    checkpoints.add(new Checkpoint(1000L, 2, false, 50, "input", "output"));
    when(checkpointStore.getCheckpoints(1000L)).thenReturn(checkpoints);

    when(programRunner.getRuntimeResult(any(), anyString(), anyString(), anyString()))
        .thenAnswer(invocation -> runtimeResult(Verdict.NORMAL));
    when(comparator.getComparisonResult(anyString(), anyString(), eq(true))).thenReturn("PE");
    when(comparator.getComparisonResult(anyString(), anyString(), eq(false))).thenReturn("AC");

    dispatcher.judgeNextSubmission(LANGUAGES);

    verify(applicationDispatcher)
        .onAllTestPointsFinished(
            any(),
            eq(
                List.of(
                    new CheckpointResult(1, "PE", 10, 128, 0),
                    new CheckpointResult(2, "AC", 10, 128, 50))));
  }

  /**
   * Test case: tests the judgeNextSubmission method. Test data: a submission that fails to compile.
   * Expected result: the program is never executed.
   */
  @Test
  public void testJudgeNextSubmissionCompileFailed() throws Exception {
    when(checkpointStore.getCheckpoints(1000L)).thenReturn(oneCheckpoint());
    when(compiler.getCompileResult(any(), anyString()))
        .thenReturn(compileResult(false));

    dispatcher.judgeNextSubmission(LANGUAGES);

    verify(applicationDispatcher).onCompileFinished(any(), any());
    verify(programRunner, never())
        .getRuntimeResult(any(), anyString(), anyString(), anyString());
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
    verify(compiler, never()).getCompileResult(any(), anyString());
    verify(applicationDispatcher, never()).onAllTestPointsFinished(any(), any());
  }

  /**
   * Test case: tests the judgeNextSubmission method. Test data: the web application says that it
   * has no test data for the problem. Expected result: a System Error that says so, which is an
   * outcome: the judger is not counted a failed judging.
   */
  @Test
  public void testJudgeNextSubmissionWithoutTestDataIsSystemError() throws Exception {
    org.mockito.Mockito.doThrow(new NoCheckpointsException("HTTP 404"))
        .when(checkpointStore)
        .sync(any());

    dispatcher.judgeNextSubmission(LANGUAGES);

    verify(applicationDispatcher).onErrorOccurred(any(), eq(Dispatcher.NO_TEST_DATA_REASON));
    verify(applicationDispatcher, never()).requeueSubmission(any(), anyString());
    verify(compiler, never()).getCompileResult(any(), anyString());
    Assertions.assertTrue(dispatcher.isReady());
  }

  /**
   * Test case: tests the judgeNextSubmission and isReady methods. Test data: every download fails
   * as the judger's settings are wrong: HTTP 404 without the mark of the web application (a wrong
   * judger.web.baseUrl), then HTTP 403 (a wrong judger.apiToken). Expected result: no System Error:
   * each judging fails, which releases its submission at its place without counting it there and
   * without compiling, and after as many in a row as allowed the judger is not ready.
   */
  @Test
  public void testFailedDownloadsAreFailedJudgings() throws Exception {
    org.mockito.Mockito.doThrow(
            new java.io.IOException("HTTP 404"),
            new java.io.IOException("HTTP 403"),
            new java.io.IOException("HTTP 404"))
        .when(checkpointStore)
        .sync(any());

    for (int i = 0; i < Dispatcher.MAX_FAILURES_IN_A_ROW; ++i) {
      Assertions.assertTrue(dispatcher.isReady());
      Assertions.assertTrue(dispatcher.judgeNextSubmission(LANGUAGES));
    }

    Assertions.assertFalse(dispatcher.isReady());
    verify(applicationDispatcher, times(Dispatcher.MAX_FAILURES_IN_A_ROW))
        .releaseSubmission(argThat(s -> s.getSubmissionId() == 100L));
    verify(applicationDispatcher, never()).requeueSubmission(any(), anyString());
    verify(applicationDispatcher, never()).onErrorOccurred(any(), anyString());
    verify(compiler, never()).getCompileResult(any(), anyString());
  }

  /**
   * Test case: tests the judgeNextSubmission method. Test data: the test data cannot be downloaded
   * now (e.g. the web application is restarting). Expected result: no System Error: the submission
   * is released at once, at its place in the queue and without counting the failed judging on it,
   * and the lease is no longer renewed. (The test data is
   * synced before the work directory is created, so there is none to remove yet; the removal of one
   * after a failure is tested by testJudgeNextSubmissionCleansUpOnFailure.)
   */
  @Test
  public void testJudgeNextSubmissionWithUnavailableCheckpointsIsReleased() throws Exception {
    org.mockito.Mockito.doThrow(new java.io.IOException("HTTP 503"))
        .when(checkpointStore)
        .sync(any());

    Assertions.assertTrue(dispatcher.judgeNextSubmission(LANGUAGES));

    verify(applicationDispatcher).releaseSubmission(argThat(s -> s.getClaimToken() == CLAIM_TOKEN));
    verify(applicationDispatcher, never()).requeueSubmission(any(), anyString());
    verify(applicationDispatcher, never()).onErrorOccurred(any(), anyString());
    verify(preprocessor, never()).createTestCode(any(), anyString());
    verify(compiler, never()).getCompileResult(any(), anyString());
    int renewals = numberOfRenewals();
    Thread.sleep(LEASE_TIMEOUT);
    Assertions.assertEquals(renewals, numberOfRenewals());
  }

  /**
   * Test case: tests the judgeNextSubmission method. Test data: a submission whose judgings failed
   * as often as allowed, the last one for a reason, and one with a failed judging less. Expected
   * result: the first is not judged again but a System Error that gives the number of failed
   * judgings and the reason of the last one; it counts on the judger neither as a failed judging
   * nor as one that reached an outcome. The second is judged.
   */
  @Test
  public void testJudgeNextSubmissionGivesUpAfterTooManyFailedJudgings() throws Exception {
    when(applicationDispatcher.claimSubmission(anyList(), anyLong()))
        .thenReturn(
            buildSubmission(MAX_FAILED_JUDGINGS, ApplicationDispatcher.INTERRUPTED_REASON),
            buildSubmission(MAX_FAILED_JUDGINGS - 1, "The sandbox failed."));
    when(checkpointStore.getCheckpoints(1000L)).thenReturn(oneCheckpoint());
    when(compiler.getCompileResult(any(), anyString()))
        .thenReturn(compileResult(false));
    ReflectionTestUtils.setField(dispatcher, "failuresInARow", 1);

    Assertions.assertTrue(dispatcher.judgeNextSubmission(LANGUAGES));

    verify(applicationDispatcher)
        .onErrorOccurred(
            argThat(s -> s.getClaimToken() == CLAIM_TOKEN),
            eq(
                String.format(
                    Dispatcher.GIVEN_UP_REASON,
                    MAX_FAILED_JUDGINGS,
                    ApplicationDispatcher.INTERRUPTED_REASON)));
    verify(checkpointStore, never()).sync(any());
    Assertions.assertEquals(10L, dispatcher.getPause());

    Assertions.assertTrue(dispatcher.judgeNextSubmission(LANGUAGES));
    verify(applicationDispatcher).onCompileFinished(any(), any());
    verify(applicationDispatcher, times(1)).onErrorOccurred(any(), anyString());
  }

  /**
   * Test case: tests the judgeSubmissions method. Test data: a submission with as many failed
   * judgings as allowed, whose System Error cannot be recorded as the database is unreachable.
   * Expected result: a failed judging of the judger (the submission is released without counting
   * it), not a failure to take a submission: the judger pauses after it.
   */
  @Test
  public void testAGiveUpThatCannotBeRecordedIsAFailedJudging() throws Exception {
    when(applicationDispatcher.claimSubmission(anyList(), anyLong()))
        .thenReturn(buildSubmission(MAX_FAILED_JUDGINGS, "The sandbox failed."))
        .thenReturn(null);
    org.mockito.Mockito.doThrow(
            new org.springframework.dao.DataAccessResourceFailureException("database down"))
        .when(applicationDispatcher)
        .onErrorOccurred(any(), anyString());

    Assertions.assertTrue(dispatcher.judgeNextSubmission(LANGUAGES));

    verify(applicationDispatcher).releaseSubmission(argThat(s -> s.getClaimToken() == CLAIM_TOKEN));
    verify(applicationDispatcher, never()).requeueSubmission(any(), anyString());
    Assertions.assertEquals(10L, dispatcher.getPause());
    Assertions.assertTrue(dispatcher.isReady());
  }

  /**
   * Test case: tests the judgeNextSubmission method. Test data: the sandbox fails to run the
   * compiler, then the program (e.g. the submission removed its own binary). Expected result:
   * neither a Compile Error with an empty log nor a System Error: each judging fails, with its
   * reason, which releases the submission.
   */
  @Test
  public void testJudgeNextSubmissionSandboxFailureIsAFailedJudging() throws Exception {
    when(checkpointStore.getCheckpoints(1000L)).thenReturn(oneCheckpoint());
    when(compiler.getCompileResult(any(), anyString()))
        .thenReturn(new CompileResult(Verdict.INTERNAL_ERROR, ""), compileResult(true));
    when(programRunner.getRuntimeResult(any(), anyString(), anyString(), anyString()))
        .thenReturn(runtimeResult(Verdict.INTERNAL_ERROR));

    dispatcher.judgeNextSubmission(LANGUAGES);
    verify(applicationDispatcher, never()).onCompileFinished(any(), any());
    dispatcher.judgeNextSubmission(LANGUAGES);

    verify(applicationDispatcher).requeueSubmission(any(), contains("run the compiler"));
    verify(applicationDispatcher).requeueSubmission(any(), contains("run the program"));
    verify(applicationDispatcher, never()).onErrorOccurred(any(), anyString());
    verify(applicationDispatcher, never()).onOneTestPointFinished(any(), any());
    verify(applicationDispatcher, never()).onAllTestPointsFinished(any(), any());
  }

  /**
   * Test case: tests the judgeNextSubmission method. Test data: this judger cannot read its copy of
   * the expected output. Expected result: neither a Wrong Answer nor a System Error: the judgings
   * fail, which releases the submission without counting them on it.
   */
  @Test
  public void testJudgeNextSubmissionUnreadableAnswerIsAFailedJudging() throws Exception {
    when(checkpointStore.getCheckpoints(1000L)).thenReturn(oneCheckpoint());
    when(compiler.getCompileResult(any(), anyString()))
        .thenReturn(compileResult(true));
    when(programRunner.getRuntimeResult(any(), anyString(), anyString(), anyString()))
        .thenReturn(runtimeResult(Verdict.NORMAL));
    when(comparator.getComparisonResult(anyString(), anyString(), anyBoolean()))
        .thenThrow(new java.io.IOException("No such file"));

    dispatcher.judgeNextSubmission(LANGUAGES);
    dispatcher.judgeNextSubmission(LANGUAGES);

    verify(applicationDispatcher, times(2)).releaseSubmission(any());
    verify(applicationDispatcher, never()).requeueSubmission(any(), anyString());
    verify(applicationDispatcher, never()).onErrorOccurred(any(), anyString());
    verify(applicationDispatcher, never()).onAllTestPointsFinished(any(), any());
  }

  /**
   * Test case: tests the judgeNextSubmission and isReady methods. Test data: as many judgings in a
   * row as allowed fail (the sandbox cannot run the compiler), and one more, then one compiles.
   * Expected result: the judger pauses longer after each of them, up to the longest pause, and is
   * not ready after the last allowed one; it is ready again once it judged a submission.
   */
  @Test
  public void testFailedJudgingsInARowMakeTheJudgerNotReady() throws Exception {
    when(checkpointStore.getCheckpoints(1000L)).thenReturn(oneCheckpoint());
    CompileResult failure = new CompileResult(Verdict.INTERNAL_ERROR, "");
    when(compiler.getCompileResult(any(), anyString()))
        .thenReturn(failure, failure, failure, failure, compileResult(false));

    Assertions.assertTrue(dispatcher.isReady());
    List<Long> backoffs = new ArrayList<>();
    for (int i = 0; i < Dispatcher.MAX_FAILURES_IN_A_ROW + 1; ++i) {
      dispatcher.judgeNextSubmission(LANGUAGES);
      backoffs.add(dispatcher.getPause());
      Assertions.assertEquals(
          i + 1 < Dispatcher.MAX_FAILURES_IN_A_ROW, dispatcher.isReady(), "" + i);
    }
    Assertions.assertEquals(List.of(10L, 20L, 40L, 40L), backoffs);
    verify(applicationDispatcher, never()).onErrorOccurred(any(), anyString());

    dispatcher.judgeNextSubmission(LANGUAGES);
    verify(applicationDispatcher).onCompileFinished(any(), any());
    Assertions.assertTrue(dispatcher.isReady());
  }

  /**
   * Test case: tests the judgeNextSubmission method. Test data: a stage throws after the work
   * directory was created (a bug). Expected result: a failed judging, with the reason of an
   * internal error, and the work directory is still removed.
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
        .createTestCode(any(), anyString());
    when(checkpointStore.getCheckpoints(1000L)).thenReturn(oneCheckpoint());
    when(compiler.getCompileResult(any(), anyString()))
        .thenThrow(new IllegalStateException("compiler crashed"));

    dispatcher.judgeNextSubmission(LANGUAGES);

    verify(applicationDispatcher).requeueSubmission(any(), eq(Dispatcher.INTERNAL_ERROR_REASON));
    verify(applicationDispatcher, never()).onErrorOccurred(any(), anyString());
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
    when(compiler.getCompileResult(any(), anyString()))
        .thenReturn(compileResult(false));

    dispatcher.judgeNextSubmission(LANGUAGES);
    dispatcher.judgeNextSubmission(LANGUAGES);

    org.mockito.ArgumentCaptor<String> workDirectories =
        org.mockito.ArgumentCaptor.forClass(String.class);
    verify(compiler, times(2)).getCompileResult(any(), workDirectories.capture());
    String first = workDirectories.getAllValues().get(0);
    String second = workDirectories.getAllValues().get(1);
    Assertions.assertTrue(
        first.startsWith(Path.of("/tmp/voj-dispatcher-tests", "voj-100-").toString()), first);
    Assertions.assertNotEquals(first, second);
  }

  /**
   * Test case: tests the judgeNextSubmission method. Test data: the database becomes unreachable
   * while the verdict is recorded, then refuses it (a value its column does not take). Expected
   * result: no System Error is attempted: each judging fails, which releases the submission, at its
   * place for the outage (or, if the database is still unreachable, leaves it to the lease), to the
   * end of the queue counting the failed judging for the refusal, which the verdict may cause.
   */
  @Test
  public void testAVerdictThatCannotBeRecordedIsAFailedJudging() throws Exception {
    when(checkpointStore.getCheckpoints(1000L)).thenReturn(oneCheckpoint());
    when(compiler.getCompileResult(any(), anyString()))
        .thenReturn(compileResult(false));
    org.mockito.Mockito.doThrow(
            new org.springframework.dao.DataAccessResourceFailureException("database down"),
            new org.springframework.dao.DataIntegrityViolationException("Data too long"))
        .when(applicationDispatcher)
        .onCompileFinished(any(), any());
    when(applicationDispatcher.releaseSubmission(any()))
        .thenThrow(new org.springframework.dao.DataAccessResourceFailureException("still down"));

    Assertions.assertTrue(dispatcher.judgeNextSubmission(LANGUAGES));
    Assertions.assertTrue(dispatcher.judgeNextSubmission(LANGUAGES));

    verify(applicationDispatcher, never()).onErrorOccurred(any(), anyString());
    verify(applicationDispatcher).releaseSubmission(any());
    verify(applicationDispatcher).requeueSubmission(any(), contains("could not be recorded"));
  }

  /**
   * Test case: tests the judgeNextSubmission method. Test data: a pending submission. Expected
   * result: it is judged with the claim token of the judging that claimed it, in the languages of
   * the judger.
   */
  @Test
  public void testJudgeNextSubmissionJudgesWithTheClaimToken() throws Exception {
    when(checkpointStore.getCheckpoints(1000L)).thenReturn(oneCheckpoint());
    when(compiler.getCompileResult(any(), anyString()))
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
    when(compiler.getCompileResult(any(), anyString()))
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
    when(compiler.getCompileResult(any(), anyString()))
        .thenReturn(compileResult(true));
    // Current until the first checkpoint runs, superseded while it runs.
    AtomicBoolean isRejudged = new AtomicBoolean();
    when(applicationDispatcher.renewLease(any(), anyLong()))
        .thenAnswer(invocation -> !isRejudged.get());
    when(programRunner.getRuntimeResult(any(), anyString(), anyString(), anyString()))
        .thenAnswer(
            invocation -> {
              isRejudged.set(true);
              Thread.sleep(LEASE_TIMEOUT);
              return runtimeResult(Verdict.NORMAL);
            });
    when(comparator.getComparisonResult(anyString(), anyString(), anyBoolean())).thenReturn("AC");
    org.mockito.Mockito.doAnswer(
            invocation -> {
              Files.createDirectories(Path.of(invocation.<String>getArgument(1)));
              return null;
            })
        .when(preprocessor)
        .createTestCode(any(), anyString());

    dispatcher.judgeNextSubmission(LANGUAGES);

    verify(applicationDispatcher).onCompileFinished(any(), any());
    verify(programRunner, times(1))
        .getRuntimeResult(any(), anyString(), anyString(), anyString());
    verify(applicationDispatcher, never()).onOneTestPointFinished(any(), any());
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

    verify(compiler, never()).getCompileResult(any(), anyString());
    verify(applicationDispatcher, never()).onCompileFinished(any(), any());
    verify(applicationDispatcher, never()).onErrorOccurred(any(), anyString());
  }

  /**
   * Test case: tests the judgeSubmissions method. Test data: no pending submission at first, then
   * the database is unreachable once, then a pending submission. Expected result: the judger looks
   * again after each of them, and judges the submission.
   */
  @Test
  public void testJudgeSubmissionsLooksAgainUntilThereIsASubmission() throws Exception {
    when(checkpointStore.getCheckpoints(1000L)).thenReturn(oneCheckpoint());
    when(compiler.getCompileResult(any(), anyString()))
        .thenReturn(compileResult(false));
    when(applicationDispatcher.claimSubmission(anyList(), anyLong()))
        .thenReturn(null)
        .thenThrow(new org.springframework.dao.DataAccessResourceFailureException("down"))
        .thenReturn(buildSubmission())
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
    verify(applicationDispatcher, atLeast(4))
        .claimSubmission(LANGUAGES, LEASE_SECONDS);
  }

  /**
   * Test case: tests the judgeSubmissions and destroy methods. Test data: an Error (as an
   * OutOfMemoryError) while a submission is judged, then the shutdown of the judger that the exit
   * starts. Expected result: the judging loop ends and the process exits with the status of a fatal
   * error, without releasing the submission (its judging counts as a failed one once the lease
   * expired) or judging it; the judger is not ready meanwhile.
   */
  @Test
  public void testJudgeSubmissionsExitsOnAnError() throws Exception {
    when(checkpointStore.getCheckpoints(1000L)).thenReturn(oneCheckpoint());
    when(compiler.getCompileResult(any(), anyString()))
        .thenThrow(new OutOfMemoryError("Java heap space"));

    dispatcher.judgeSubmissions(LANGUAGES);
    dispatcher.destroy();

    Assertions.assertEquals(List.of(Dispatcher.FATAL_ERROR_EXIT_STATUS), exitStatuses);
    Assertions.assertFalse(dispatcher.isReady());
    verify(applicationDispatcher, never()).releaseSubmission(any());
    verify(applicationDispatcher, never()).requeueSubmission(any(), anyString());
    verify(applicationDispatcher, never()).onErrorOccurred(any(), anyString());
    verify(applicationDispatcher, times(1)).claimSubmission(anyList(), anyLong());
  }

  /**
   * Test case: tests the judgeNextSubmission method. Test data: an Error (as an OutOfMemoryError)
   * while the lease of a judging is renewed, in the background. Expected result: the judging stops
   * (as a superseded one) without recording anything, the process exits with the status of a fatal
   * error, nothing is released, and the judger is not ready and claims nothing more.
   */
  @Test
  public void testAnErrorInARenewalStopsTheJudger() throws Exception {
    when(checkpointStore.getCheckpoints(1000L)).thenReturn(oneCheckpoint());
    when(applicationDispatcher.renewLease(any(), anyLong()))
        .thenThrow(new OutOfMemoryError("Java heap space"));
    when(compiler.getCompileResult(any(), anyString()))
        .thenAnswer(
            invocation -> {
              Thread.sleep(LEASE_TIMEOUT);
              return compileResult(false);
            });

    Assertions.assertTrue(dispatcher.judgeNextSubmission(LANGUAGES));
    dispatcher.destroy();

    Assertions.assertEquals(List.of(Dispatcher.FATAL_ERROR_EXIT_STATUS), exitStatuses);
    verify(applicationDispatcher, never()).onCompileFinished(any(), any());
    verify(applicationDispatcher, never()).releaseSubmission(any());
    verify(applicationDispatcher, never()).requeueSubmission(any(), anyString());
    Assertions.assertFalse(dispatcher.isReady());
    Assertions.assertFalse(dispatcher.judgeNextSubmission(LANGUAGES));
    verify(applicationDispatcher, times(1)).claimSubmission(anyList(), anyLong());
  }

  /**
   * Test case: tests the judgeNextSubmission and destroy methods. Test data: the judger shuts down
   * (e.g. in a rolling update) while it compiles a submission. Expected result: the submission is
   * released at its place in the queue, without counting a failed judging on it, and the judger
   * claims nothing more and is not ready.
   */
  @Test
  public void testAShutdownReleasesTheSubmission() throws Exception {
    when(checkpointStore.getCheckpoints(1000L)).thenReturn(oneCheckpoint());
    when(applicationDispatcher.releaseSubmission(any())).thenReturn(true);
    when(compiler.getCompileResult(any(), anyString()))
        .thenAnswer(
            invocation -> {
              dispatcher.destroy();
              return compileResult(false);
            });

    Assertions.assertTrue(dispatcher.judgeNextSubmission(LANGUAGES));

    verify(applicationDispatcher).releaseSubmission(argThat(s -> s.getClaimToken() == CLAIM_TOKEN));
    verify(applicationDispatcher, never()).requeueSubmission(any(), anyString());
    Assertions.assertFalse(dispatcher.isReady());
    Assertions.assertFalse(dispatcher.judgeNextSubmission(LANGUAGES));
    verify(applicationDispatcher, times(1)).claimSubmission(anyList(), anyLong());
    Assertions.assertEquals(List.of(), exitStatuses);
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
   * Builds a submission referencing Problem#1000, claimed by a judging, without failed judgings.
   *
   * @return a submission object
   */
  private Submission buildSubmission() {
    return buildSubmission(0, null);
  }

  /**
   * Builds a submission referencing Problem#1000, claimed by a judging.
   *
   * @param failedJudgings - how many judgings of it failed
   * @param lastFailure - the reason of the last one, or {@code null}
   * @return a submission object
   */
  private Submission buildSubmission(int failedJudgings, String lastFailure) {
    Problem problem = new Problem(1000L, true, "A+B", 1000, 65536, "", "", "", "", "", "");
    Submission submission = new Submission();
    submission.setSubmissionId(100L);
    submission.setProblem(problem);
    submission.setFailedJudgings(failedJudgings);
    submission.setLastFailure(lastFailure);
    submission.setClaimToken(CLAIM_TOKEN);
    return submission;
  }

  /**
   * Builds a compilation result.
   *
   * @param isSuccessful - whether the compilation succeeded
   * @return a compilation result
   */
  private CompileResult compileResult(boolean isSuccessful) {
    return isSuccessful
        ? new CompileResult(Verdict.NORMAL, "")
        : new CompileResult(Verdict.RUNTIME_ERROR, "error");
  }

  /**
   * Builds the outcome of a run on a single checkpoint.
   *
   * @param verdict - how the run ended
   * @return the outcome
   */
  private RunResult runtimeResult(Verdict verdict) {
    return new RunResult(verdict, 10, 128);
  }

  /** The languages of the judger of the tests. */
  private static final List<Integer> LANGUAGES = List.of(2, 3);

  /** The lease (ms) of the judgings of the tests: renewed every 100 ms. */
  private static final long LEASE_TIMEOUT = 400;

  /** The lease of the judgings of the tests in whole seconds, rounded up. */
  private static final long LEASE_SECONDS = 1;

  /** How many judgings of a submission may fail. */
  private static final int MAX_FAILED_JUDGINGS = Dispatcher.MAX_FAILED_JUDGINGS;

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

  /** The exit statuses the Dispatcher exited the process with. */
  private final List<Integer> exitStatuses = new ArrayList<>();
}
