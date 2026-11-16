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
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mockingDetails;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.DataIntegrityViolationException;

import org.verwandlung.voj.judger.application.ApplicationDispatcher;
import org.verwandlung.voj.judger.model.Problem;
import org.verwandlung.voj.judger.model.Submission;

/**
 * Test class for the judger Dispatcher: the judging queue, with the judgings mocked.
 *
 * @author Haozhe Xie
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class DispatcherTest {
  /**
   * Creates a dispatcher with short times, which records an exit of the process rather than doing
   * it, and a pending submission to claim, without failed judgings.
   */
  @BeforeEach
  public void setUp() {
    dispatcher = new Dispatcher(applicationDispatcher, judge, TIMES, exitStatuses::add);
    when(applicationDispatcher.claimSubmission(anyList(), anyLong()))
        .thenAnswer(invocation -> submission(0, null));
    when(applicationDispatcher.renewLease(any(), anyLong())).thenReturn(true);
  }

  /**
   * Test case: no pending submission in the languages of the judger. Expected result: nothing is
   * judged, and it says so.
   */
  @Test
  public void testNothingToClaim() throws Exception {
    when(applicationDispatcher.claimSubmission(anyList(), anyLong())).thenReturn(null);

    Assertions.assertFalse(dispatcher.judgeNextSubmission(LANGUAGES));

    verify(applicationDispatcher).claimSubmission(LANGUAGES, LEASE_SECONDS);
    verify(judge, never()).judge(any(), any());
  }

  /**
   * Test case: a judging that reaches an outcome. Expected result: the submission is judged with
   * the claim token of its claim, and neither released nor counted.
   */
  @Test
  public void testAnOutcome() throws Exception {
    Assertions.assertTrue(dispatcher.judgeNextSubmission(LANGUAGES));

    verify(judge).judge(argThat(s -> s.getClaimToken() == CLAIM_TOKEN), any());
    verify(applicationDispatcher, never()).releaseSubmission(any());
    verify(applicationDispatcher, never()).requeueSubmission(any(), anyString());
    Assertions.assertEquals(TIMES.firstPauseMs(), dispatcher.getPause());
  }

  /**
   * Test case: judgings that fail for the judger or its environment: a failure of the judger, and
   * the database unreachable as the verdict is recorded. Expected result: each releases its
   * submission (to the end of the queue), counting nothing on it.
   */
  @Test
  public void testAFailureOfTheJudgerReleasesTheSubmission() throws Exception {
    doThrow(FailedJudging.ofJudger("The judger could not download the test data.", null))
        .doThrow(new DataAccessResourceFailureException("database down"))
        .when(judge)
        .judge(any(), any());

    dispatcher.judgeNextSubmission(LANGUAGES);
    dispatcher.judgeNextSubmission(LANGUAGES);

    verify(applicationDispatcher, times(2))
        .releaseSubmission(argThat(s -> s.getClaimToken() == CLAIM_TOKEN));
    verify(applicationDispatcher, never()).requeueSubmission(any(), anyString());
    verify(applicationDispatcher, never()).handBackSubmission(any());
  }

  /**
   * Test case: judgings that fail in ways the submission may have caused: the sandbox failed on its
   * program, the database refused its verdict, and a bug. Expected result: each sends the
   * submission to the end of the queue, counting the failure on it with its reason.
   */
  @Test
  public void testAFailureTheSubmissionMayHaveCausedIsCountedOnIt() throws Exception {
    doThrow(FailedJudging.ofSubmission("The sandbox failed to run the program.", null))
        .doThrow(new DataIntegrityViolationException("Data too long"))
        .doThrow(new IllegalStateException("a bug"))
        .when(judge)
        .judge(any(), any());

    dispatcher.judgeNextSubmission(LANGUAGES);
    dispatcher.judgeNextSubmission(LANGUAGES);
    dispatcher.judgeNextSubmission(LANGUAGES);

    verify(applicationDispatcher).requeueSubmission(any(), contains("run the program"));
    verify(applicationDispatcher).requeueSubmission(any(), contains("could not be recorded"));
    verify(applicationDispatcher)
        .requeueSubmission(any(), eq(Dispatcher.INTERNAL_ERROR_REASON));
    verify(applicationDispatcher, never()).releaseSubmission(any());
  }

  /**
   * Test case: a failed judging whose submission cannot be released, as the database is still
   * unreachable. Expected result: it is left to its lease, and counts on the judger all the same.
   */
  @Test
  public void testASubmissionThatCannotBeReleasedIsLeftToItsLease() throws Exception {
    doThrow(new DataAccessResourceFailureException("database down"))
        .when(judge)
        .judge(any(), any());
    when(applicationDispatcher.releaseSubmission(any()))
        .thenThrow(new DataAccessResourceFailureException("still down"));

    Assertions.assertTrue(dispatcher.judgeNextSubmission(LANGUAGES));

    Assertions.assertEquals(TIMES.firstPauseMs(), dispatcher.getPause());
  }

  /**
   * Test case: as many failed judgings in a row as allowed, one more, then an outcome. Expected
   * result: the judger pauses longer after each of them, up to the longest pause, and is not ready
   * after the last allowed one; it is ready again once a judging reached an outcome.
   */
  @Test
  public void testFailedJudgingsInARowMakeTheJudgerNotReady() throws Exception {
    when(applicationDispatcher.claimSubmission(anyList(), anyLong()))
        .thenAnswer(invocation -> judgings.size() < 5 ? submission(0, null) : null);
    doAnswer(
            invocation -> {
              judgings.add(dispatcher.isReady() + "/" + dispatcher.getPause());
              if (judgings.size() < 5) {
                throw FailedJudging.ofJudger("The judger failed.", null);
              }
              return null;
            })
        .when(judge)
        .judge(any(), any());

    Thread judging = new Thread(() -> dispatcher.judgeSubmissions(LANGUAGES));
    judging.start();
    try {
      verify(judge, timeout(10000).times(5)).judge(any(), any());
      verify(applicationDispatcher, timeout(10000).atLeast(6)).claimSubmission(anyList(), anyLong());
      Assertions.assertTrue(dispatcher.isReady());
    } finally {
      judging.interrupt();
      judging.join(10000);
    }
    // Ready, and pausing, as the failures in a row before each judging say.
    Assertions.assertEquals(
        List.of("true/10", "true/10", "true/20", "false/40", "false/40"), judgings);
  }

  /**
   * Test case: after two failed judgings, a judging superseded as its verdict is recorded.
   * Expected result: it is neither an outcome nor a failure: the judger still pauses as after two
   * failures, and the submission is neither released nor counted.
   */
  @Test
  public void testASupersededJudgingIsNeitherAnOutcomeNorAFailure() throws Exception {
    FailedJudging failure = FailedJudging.ofJudger("The judger failed.", null);
    doThrow(failure, failure, new Superseded()).when(judge).judge(any(), any());

    for (int i = 0; i < 3; ++i) {
      dispatcher.judgeNextSubmission(LANGUAGES);
    }

    Assertions.assertEquals(2 * TIMES.firstPauseMs(), dispatcher.getPause());
    verify(applicationDispatcher, times(2)).releaseSubmission(any());
    verify(applicationDispatcher, never()).requeueSubmission(any(), anyString());
  }

  /**
   * Test case: a submission whose judgings failed as often as allowed, the last one for a reason,
   * then one with a failed judging less, after a failed judging of the judger. Expected result:
   * the first is not judged but a System Error that gives the number of failed judgings and the
   * reason of the last one, which counts on the judger neither as an outcome nor as a failure; the
   * second is judged.
   */
  @Test
  public void testASubmissionIsGivenUpOnAfterTooManyFailedJudgings() throws Exception {
    when(applicationDispatcher.claimSubmission(anyList(), anyLong()))
        .thenReturn(
            submission(Dispatcher.MAX_FAILED_JUDGINGS, ApplicationDispatcher.INTERRUPTED_REASON),
            submission(Dispatcher.MAX_FAILED_JUDGINGS, ApplicationDispatcher.INTERRUPTED_REASON),
            submission(Dispatcher.MAX_FAILED_JUDGINGS - 1, "The sandbox failed."));
    doThrow(new DataAccessResourceFailureException("database down"))
        .doNothing()
        .when(applicationDispatcher)
        .onErrorOccurred(any(), anyString());

    // The System Error cannot be recorded at first: a failure of the judger, not of the claim.
    Assertions.assertTrue(dispatcher.judgeNextSubmission(LANGUAGES));
    verify(applicationDispatcher).releaseSubmission(any());
    Assertions.assertTrue(dispatcher.judgeNextSubmission(LANGUAGES));

    verify(applicationDispatcher, times(2))
        .onErrorOccurred(
            argThat(s -> s.getClaimToken() == CLAIM_TOKEN),
            eq(
                String.format(
                    Dispatcher.GIVEN_UP_REASON,
                    Dispatcher.MAX_FAILED_JUDGINGS,
                    ApplicationDispatcher.INTERRUPTED_REASON)));
    verify(judge, never()).judge(any(), any());
    Assertions.assertEquals(TIMES.firstPauseMs(), dispatcher.getPause());

    Assertions.assertTrue(dispatcher.judgeNextSubmission(LANGUAGES));
    verify(judge).judge(any(), any());
  }

  /**
   * Test case: a judging that takes four times as long as its lease. Expected result: the lease is
   * renewed in the background while it runs, and no longer once it is over.
   */
  @Test
  public void testTheLeaseIsRenewedWhileTheJudgingRuns() throws Exception {
    doAnswer(
            invocation -> {
              Thread.sleep(TIMES.leaseTimeoutMs() * 4);
              return null;
            })
        .when(judge)
        .judge(any(), any());

    dispatcher.judgeNextSubmission(LANGUAGES);

    verify(applicationDispatcher, atLeast(8)).renewLease(any(), eq(LEASE_SECONDS));
    int renewals = numberOfRenewals();
    Thread.sleep(TIMES.leaseTimeoutMs());
    Assertions.assertEquals(renewals, numberOfRenewals());
  }

  /**
   * Test case: a renewal of the lease finds the judging superseded (e.g. the submission was
   * rejudged). Expected result: the judging sees it at its next step.
   */
  @Test
  public void testARenewalThatFindsTheJudgingSupersededStopsIt() throws Exception {
    when(applicationDispatcher.renewLease(any(), anyLong())).thenReturn(false);
    AtomicBoolean wasSuperseded = new AtomicBoolean();
    doAnswer(
            invocation -> {
              Thread.sleep(TIMES.leaseTimeoutMs());
              try {
                invocation.<Lease>getArgument(1).check();
              } catch (Superseded ex) {
                wasSuperseded.set(true);
                throw ex;
              }
              return null;
            })
        .when(judge)
        .judge(any(), any());

    dispatcher.judgeNextSubmission(LANGUAGES);

    Assertions.assertTrue(wasSuperseded.get());
    verify(applicationDispatcher, never()).releaseSubmission(any());
    verify(applicationDispatcher, never()).requeueSubmission(any(), anyString());
  }

  /**
   * Test case: no pending submission at first, then the database is unreachable once, then a
   * pending submission. Expected result: the judger looks again after each of them, and judges the
   * submission.
   */
  @Test
  public void testTheJudgerLooksAgainUntilThereIsASubmission() throws Exception {
    when(applicationDispatcher.claimSubmission(anyList(), anyLong()))
        .thenReturn(null)
        .thenThrow(new DataAccessResourceFailureException("down"))
        .thenReturn(submission(0, null))
        .thenReturn(null);

    Thread judging = new Thread(() -> dispatcher.judgeSubmissions(LANGUAGES));
    judging.start();
    try {
      verify(judge, timeout(10000)).judge(any(), any());
    } finally {
      judging.interrupt();
      judging.join(10000);
    }
    Assertions.assertFalse(judging.isAlive());
    verify(applicationDispatcher, atLeast(4)).claimSubmission(LANGUAGES, LEASE_SECONDS);
  }

  /**
   * Test case: an Error (as an OutOfMemoryError) while a submission is judged, then the shutdown of
   * the judger that the exit starts. Expected result: the judging loop ends and the process exits
   * with the status of a fatal error, without releasing the submission (its judging counts on it
   * once the lease expired); the judger is not ready.
   */
  @Test
  public void testAnErrorExitsTheProcess() throws Exception {
    doThrow(new OutOfMemoryError("Java heap space")).when(judge).judge(any(), any());

    dispatcher.judgeSubmissions(LANGUAGES);
    dispatcher.destroy();

    Assertions.assertEquals(List.of(Dispatcher.FATAL_ERROR_EXIT_STATUS), exitStatuses);
    Assertions.assertFalse(dispatcher.isReady());
    verify(applicationDispatcher, never()).releaseSubmission(any());
    verify(applicationDispatcher, never()).handBackSubmission(any());
    verify(applicationDispatcher, never()).requeueSubmission(any(), anyString());
    verify(applicationDispatcher, times(1)).claimSubmission(anyList(), anyLong());
  }

  /**
   * Test case: an Error (as an OutOfMemoryError) while the lease of a judging is renewed, in the
   * background. Expected result: the judging is superseded, the process exits with the status of
   * a fatal error, nothing is released, and the judger claims nothing more.
   */
  @Test
  public void testAnErrorInARenewalExitsTheProcess() throws Exception {
    when(applicationDispatcher.renewLease(any(), anyLong()))
        .thenThrow(new OutOfMemoryError("Java heap space"));
    doAnswer(
            invocation -> {
              Thread.sleep(TIMES.leaseTimeoutMs());
              invocation.<Lease>getArgument(1).check();
              return null;
            })
        .when(judge)
        .judge(any(), any());

    Assertions.assertTrue(dispatcher.judgeNextSubmission(LANGUAGES));
    dispatcher.destroy();

    Assertions.assertEquals(List.of(Dispatcher.FATAL_ERROR_EXIT_STATUS), exitStatuses);
    verify(applicationDispatcher, never()).releaseSubmission(any());
    verify(applicationDispatcher, never()).requeueSubmission(any(), anyString());
    Assertions.assertFalse(dispatcher.judgeNextSubmission(LANGUAGES));
    verify(applicationDispatcher, times(1)).claimSubmission(anyList(), anyLong());
  }

  /**
   * Test case: the judger shuts down (e.g. in a rolling update) while it judges a submission.
   * Expected result: the submission is released at its place, without counting a failed judging
   * on it, and the judger claims nothing more and is not ready.
   */
  @Test
  public void testAShutdownReleasesTheSubmission() throws Exception {
    when(applicationDispatcher.handBackSubmission(any())).thenReturn(true);
    doAnswer(
            invocation -> {
              dispatcher.destroy();
              return null;
            })
        .when(judge)
        .judge(any(), any());

    Assertions.assertTrue(dispatcher.judgeNextSubmission(LANGUAGES));

    verify(applicationDispatcher)
        .handBackSubmission(argThat(s -> s.getClaimToken() == CLAIM_TOKEN));
    verify(applicationDispatcher, never()).releaseSubmission(any());
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
   * Builds Submission #100 of Problem #1000, claimed by a judging.
   *
   * @param failedJudgings - how many judgings of it failed
   * @param lastFailure - the reason of the last one, or {@code null}
   * @return the submission
   */
  private static Submission submission(int failedJudgings, String lastFailure) {
    Submission submission = new Submission();
    submission.setSubmissionId(100L);
    submission.setProblem(new Problem(1000L, 1000, 65536));
    submission.setFailedJudgings(failedJudgings);
    submission.setLastFailure(lastFailure);
    submission.setClaimToken(CLAIM_TOKEN);
    return submission;
  }

  /** The times of the judging queue of the tests: a lease renewed every 100 ms. */
  private static final Dispatcher.Times TIMES = new Dispatcher.Times(10, 400, 10, 40);

  /** The lease of the judgings of the tests in whole seconds, rounded up. */
  private static final long LEASE_SECONDS = 1;

  /** The languages of the judger of the tests. */
  private static final List<Integer> LANGUAGES = List.of(2, 3);

  /** The claim token of the judgings that the tests claim. */
  private static final long CLAIM_TOKEN = -4242L;

  /** The mocked ApplicationDispatcher object. */
  @Mock private ApplicationDispatcher applicationDispatcher;

  /** The mocked Judge object. */
  @Mock private Judge judge;

  /** The readiness and pause of the dispatcher at each judging, as "ready/pause". */
  private final List<String> judgings = new java.util.concurrent.CopyOnWriteArrayList<>();

  /** The exit statuses the Dispatcher exited the process with. */
  private final List<Integer> exitStatuses = new ArrayList<>();

  /** The Dispatcher object under test. */
  private Dispatcher dispatcher;
}
