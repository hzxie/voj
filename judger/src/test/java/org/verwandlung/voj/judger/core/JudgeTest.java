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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import org.verwandlung.voj.judger.application.ApplicationDispatcher;
import org.verwandlung.voj.judger.exception.NoCheckpointsException;
import org.verwandlung.voj.judger.model.Checkpoint;
import org.verwandlung.voj.judger.model.Problem;
import org.verwandlung.voj.judger.model.Submission;

/**
 * Test class for Judge: the steps of a judging, with each step mocked.
 *
 * @author Haozhe Xie
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class JudgeTest {
  /**
   * Creates the judge, whose judgings have directories of their own in a temporary directory and
   * whose problem has one checkpoint; writing the source creates the directory of the judging.
   */
  @BeforeEach
  public void setUp() throws Exception {
    judge =
        new Judge(
            applicationDispatcher, checkpointStore, preprocessor, compiler, programRunner,
            comparator);
    when(checkpointStore.getWorkDirectory()).thenReturn(workDirectory);
    when(checkpointStore.sync(any())).thenReturn(CHECKPOINT_DIRECTORY);
    when(checkpointStore.getCheckpoints(1000L)).thenReturn(List.of(checkpoint(1, true, 100)));
    doAnswer(
            invocation -> {
              Path directory = Files.createDirectory(Path.of(invocation.<String>getArgument(1)));
              Files.writeString(directory.resolve("Main.cpp"), "int main() {}");
              return null;
            })
        .when(preprocessor)
        .createTestCode(any(), anyString());
    when(compiler.getCompileResult(any(), anyString()))
        .thenReturn(new CompileResult(Verdict.NORMAL, ""));
    when(programRunner.getRuntimeResult(any(), anyString(), anyString(), anyString()))
        .thenReturn(new RunResult(Verdict.NORMAL, 10, 128));
    when(comparator.getComparisonResult(anyString(), anyString(), anyBoolean())).thenReturn("AC");
  }

  /**
   * Test case: a submission that compiles and produces the expected output. Expected result: the
   * test data is read from where the store synced it, every step is reported, the checkpoint is
   * Accepted with its score, and the directory of the judging is removed.
   */
  @Test
  public void testAccepted() throws Exception {
    judge.judge(submission(), CURRENT);

    verify(applicationDispatcher).onCompileFinished(any(), any());
    verify(programRunner)
        .getRuntimeResult(
            any(),
            anyString(),
            eq(CHECKPOINT_DIRECTORY.resolve("input#1.txt").toString()),
            anyString());
    verify(comparator)
        .getComparisonResult(
            eq(CHECKPOINT_DIRECTORY.resolve("output#1.txt").toString()), anyString(), eq(true));
    CheckpointResult accepted = new CheckpointResult(1, "AC", 10, 128, 100);
    verify(applicationDispatcher)
        .onOneTestPointFinished(argThat(s -> s.getClaimToken() == CLAIM_TOKEN), eq(accepted));
    verify(applicationDispatcher).onAllTestPointsFinished(any(), eq(List.of(accepted)));
    assertNoJudgingDirectory();
  }

  /**
   * Test case: two checkpoints, only the first requiring an exact match; the output differs from
   * the expected one in its presentation. Expected result: each checkpoint's exactlyMatch flag is
   * passed to the comparator, and its verdict (PE, earning nothing, then AC) is reported.
   */
  @Test
  public void testEachCheckpointIsComparedAsItRequires() throws Exception {
    when(checkpointStore.getCheckpoints(1000L))
        .thenReturn(List.of(checkpoint(1, true, 50), checkpoint(2, false, 50)));
    when(comparator.getComparisonResult(anyString(), anyString(), eq(true))).thenReturn("PE");

    judge.judge(submission(), CURRENT);

    verify(applicationDispatcher)
        .onAllTestPointsFinished(
            any(),
            eq(
                List.of(
                    new CheckpointResult(1, "PE", 10, 128, 0),
                    new CheckpointResult(2, "AC", 10, 128, 50))));
  }

  /**
   * Test case: a run that exceeds its time limit. Expected result: its verdict is the run's,
   * without comparing its output.
   */
  @Test
  public void testARunOverItsLimitIsNotCompared() throws Exception {
    when(programRunner.getRuntimeResult(any(), anyString(), anyString(), anyString()))
        .thenReturn(new RunResult(Verdict.TIME_LIMIT_EXCEEDED, 1000, 128));

    judge.judge(submission(), CURRENT);

    verify(comparator, never()).getComparisonResult(anyString(), anyString(), anyBoolean());
    verify(applicationDispatcher)
        .onAllTestPointsFinished(any(), eq(List.of(new CheckpointResult(1, "TLE", 1000, 128, 0))));
  }

  /**
   * Test case: a submission that does not compile. Expected result: the compilation is reported
   * (which records the Compile Error), and the program never runs.
   */
  @Test
  public void testCompileError() throws Exception {
    when(compiler.getCompileResult(any(), anyString()))
        .thenReturn(new CompileResult(Verdict.RUNTIME_ERROR, "error"));

    judge.judge(submission(), CURRENT);

    verify(applicationDispatcher).onCompileFinished(any(), any());
    verify(programRunner, never()).getRuntimeResult(any(), anyString(), anyString(), anyString());
    verify(applicationDispatcher, never()).onAllTestPointsFinished(any(), any());
    assertNoJudgingDirectory();
  }

  /**
   * Test case: the web application says it has no test data for the problem, then test data
   * without any checkpoint. Expected result: a System Error that says so each time, without
   * compiling: never an Accepted with nothing checked.
   */
  @Test
  public void testAProblemWithoutTestDataIsSystemError() throws Exception {
    when(checkpointStore.sync(any()))
        .thenThrow(new NoCheckpointsException("HTTP 404"))
        .thenReturn(CHECKPOINT_DIRECTORY);
    when(checkpointStore.getCheckpoints(1000L)).thenReturn(List.of());

    judge.judge(submission(), CURRENT);
    judge.judge(submission(), CURRENT);

    verify(applicationDispatcher, times(2))
        .onErrorOccurred(any(), eq(Judge.NO_TEST_DATA_REASON));
    verify(compiler, never()).getCompileResult(any(), anyString());
  }

  /**
   * Test case: the test data cannot be downloaded (e.g. HTTP 403 for a wrong judger.apiToken),
   * then not read, and the source cannot be written. Expected result: a failed judging of the
   * judger each time, which does not count on the submission, without compiling.
   */
  @Test
  public void testFailuresOfTheJudgerDoNotCountOnTheSubmission() throws Exception {
    when(checkpointStore.sync(any()))
        .thenThrow(new IOException("HTTP 403"))
        .thenReturn(CHECKPOINT_DIRECTORY);
    when(checkpointStore.getCheckpoints(1000L))
        .thenThrow(new IOException("unreadable manifest"))
        .thenReturn(List.of(checkpoint(1, true, 100)));
    doThrow(new IOException("disk full")).when(preprocessor).createTestCode(any(), anyString());

    assertFailure(false, "could not download");
    assertFailure(false, "could not read its copy");
    assertFailure(false, "could not write the source code");
    verify(compiler, never()).getCompileResult(any(), anyString());
    verify(applicationDispatcher, never()).onErrorOccurred(any(), anyString());
  }

  /**
   * Test case: the expected output cannot be read. Expected result: neither a Wrong Answer nor a
   * System Error: a failed judging of the judger.
   */
  @Test
  public void testAnUnreadableAnswerIsAFailureOfTheJudger() throws Exception {
    when(comparator.getComparisonResult(anyString(), anyString(), anyBoolean()))
        .thenThrow(new IOException("No such file"));

    assertFailure(false, "could not read its copy");
    verify(applicationDispatcher, never()).onAllTestPointsFinished(any(), any());
  }

  /**
   * Test case: the sandbox fails to run the compiler, then the program (e.g. as the submission
   * removed its own binary). Expected result: neither a Compile Error with an empty log nor a
   * System Error: a failed judging that counts on the submission, and the directory of the
   * judging is removed.
   */
  @Test
  public void testASandboxFailureCountsOnTheSubmission() throws Exception {
    when(compiler.getCompileResult(any(), anyString()))
        .thenReturn(new CompileResult(Verdict.INTERNAL_ERROR, ""))
        .thenReturn(new CompileResult(Verdict.NORMAL, ""));
    when(programRunner.getRuntimeResult(any(), anyString(), anyString(), anyString()))
        .thenReturn(new RunResult(Verdict.INTERNAL_ERROR, 0, 0));

    assertFailure(true, "run the compiler");
    verify(applicationDispatcher, never()).onCompileFinished(any(), any());
    assertFailure(true, "run the program");
    verify(applicationDispatcher, never()).onOneTestPointFinished(any(), any());
    verify(applicationDispatcher, never()).onAllTestPointsFinished(any(), any());
    assertNoJudgingDirectory();
  }

  /**
   * Test case: a step throws after the directory of the judging was created (a bug). Expected
   * result: the exception is the judge's caller's, and the directory is removed.
   */
  @Test
  public void testTheDirectoryIsRemovedWhateverHappens() throws Exception {
    when(compiler.getCompileResult(any(), anyString()))
        .thenThrow(new IllegalStateException("compiler crashed"));

    Assertions.assertThrows(
        IllegalStateException.class, () -> judge.judge(submission(), CURRENT));

    assertNoJudgingDirectory();
  }

  /**
   * Test case: the same submission judged twice (as by two judgers on one host). Expected result:
   * each judging gets a directory of its own, named after the submission.
   */
  @Test
  public void testEachJudgingHasItsOwnDirectory() throws Exception {
    judge.judge(submission(), CURRENT);
    judge.judge(submission(), CURRENT);

    ArgumentCaptor<String> directories = ArgumentCaptor.forClass(String.class);
    verify(compiler, times(2)).getCompileResult(any(), directories.capture());
    String first = directories.getAllValues().get(0);
    Assertions.assertTrue(first.startsWith(workDirectory.resolve("voj-100-").toString()), first);
    Assertions.assertNotEquals(first, directories.getAllValues().get(1));
  }

  /**
   * Test case: a judging found superseded while the first of two checkpoints runs. Expected result:
   * it stops once that run is over, without reporting that checkpoint, running the next one, or
   * recording anything; its directory is removed.
   */
  @Test
  public void testASupersededJudgingStopsAtItsNextStep() throws Exception {
    when(checkpointStore.getCheckpoints(1000L))
        .thenReturn(List.of(checkpoint(1, true, 50), checkpoint(2, true, 50)));
    AtomicBoolean isSuperseded = new AtomicBoolean();
    when(programRunner.getRuntimeResult(any(), anyString(), anyString(), anyString()))
        .thenAnswer(
            invocation -> {
              isSuperseded.set(true);
              return new RunResult(Verdict.NORMAL, 10, 128);
            });

    Assertions.assertThrows(
        Superseded.class,
        () ->
            judge.judge(
                submission(),
                () -> {
                  if (isSuperseded.get()) {
                    throw new Superseded();
                  }
                }));

    verify(applicationDispatcher).onCompileFinished(any(), any());
    verify(programRunner, times(1)).getRuntimeResult(any(), anyString(), anyString(), anyString());
    verify(applicationDispatcher, never()).onOneTestPointFinished(any(), any());
    verify(applicationDispatcher, never()).onAllTestPointsFinished(any(), any());
    assertNoJudgingDirectory();
  }

  /**
   * Test case: a judging found superseded while it downloaded the test data. Expected result: it
   * neither writes the source nor compiles.
   */
  @Test
  public void testAJudgingSupersededWhileDownloadingIsNotCompiled() throws Exception {
    Assertions.assertThrows(
        Superseded.class,
        () ->
            judge.judge(
                submission(),
                () -> {
                  throw new Superseded();
                }));

    verify(preprocessor, never()).createTestCode(any(), anyString());
    verify(compiler, never()).getCompileResult(any(), anyString());
  }

  /**
   * Judges a submission, which must fail.
   *
   * @param countsOnSubmission - whether the failure must count on the submission
   * @param reason - what its reason must say
   */
  private void assertFailure(boolean countsOnSubmission, String reason) {
    FailedJudging failure =
        Assertions.assertThrows(FailedJudging.class, () -> judge.judge(submission(), CURRENT));
    Assertions.assertEquals(countsOnSubmission, failure.countsOnSubmission());
    Assertions.assertTrue(failure.getMessage().contains(reason), failure.getMessage());
  }

  /** Asserts that no directory of a judging is left. */
  private void assertNoJudgingDirectory() throws IOException {
    try (Stream<Path> entries = Files.list(workDirectory)) {
      Assertions.assertEquals(0, entries.count());
    }
  }

  /**
   * Builds a checkpoint of Problem #1000.
   *
   * @param checkpointId - its identifier
   * @param isExactlyMatch - whether it requires an exact match
   * @param score - its score
   * @return the checkpoint
   */
  private static Checkpoint checkpoint(int checkpointId, boolean isExactlyMatch, int score) {
    return new Checkpoint(1000L, checkpointId, isExactlyMatch, score, "input", "output");
  }

  /**
   * Builds Submission #100 of Problem #1000, claimed by a judging.
   *
   * @return the submission
   */
  private static Submission submission() {
    Submission submission = new Submission();
    submission.setSubmissionId(100L);
    submission.setProblem(new Problem(1000L, 1000, 65536));
    submission.setClaimToken(CLAIM_TOKEN);
    return submission;
  }

  /** The lease of a judging that is never superseded. */
  private static final Lease CURRENT = () -> {};

  /** The claim token of the judgings of the tests. */
  private static final long CLAIM_TOKEN = -4242L;

  /** Where the test data of Problem #1000 is (it is never read: the comparator is mocked). */
  private static final Path CHECKPOINT_DIRECTORY = Path.of("/voj-judge-tests/1000");

  /** The mocked ApplicationDispatcher object. */
  @Mock private ApplicationDispatcher applicationDispatcher;

  /** The mocked CheckpointStore object. */
  @Mock private CheckpointStore checkpointStore;

  /** The mocked Preprocessor object. */
  @Mock private Preprocessor preprocessor;

  /** The mocked Compiler object. */
  @Mock private Compiler compiler;

  /** The mocked ProgramRunner object. */
  @Mock private ProgramRunner programRunner;

  /** The mocked Comparator object. */
  @Mock private Comparator comparator;

  /** The work directory of the judger, in which the judgings have their directories. */
  @TempDir Path workDirectory;

  /** The Judge object under test. */
  private Judge judge;
}
