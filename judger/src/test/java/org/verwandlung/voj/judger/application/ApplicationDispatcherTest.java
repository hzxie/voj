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
package org.verwandlung.voj.judger.application;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
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
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import org.springframework.dao.DataAccessResourceFailureException;

import org.verwandlung.voj.judger.core.Dispatcher;
import org.verwandlung.voj.judger.exception.IllgealSubmissionException;
import org.verwandlung.voj.judger.mapper.JudgeResultMapper;
import org.verwandlung.voj.judger.mapper.SubmissionMapper;
import org.verwandlung.voj.judger.messenger.MessageSender;
import org.verwandlung.voj.judger.model.JudgeResult;
import org.verwandlung.voj.judger.model.Submission;

/**
 * Test class for ApplicationDispatcher.
 *
 * @author Haozhe Xie
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class ApplicationDispatcherTest {
  /**
   * Test case: tests the onSubmissionCreated method. Expected result: the judging task is delegated
   * to the judger dispatcher.
   */
  @Test
  public void testOnSubmissionCreated() throws Exception {
    applicationDispatcher.onSubmissionCreated(100L, 3L, true);
    verify(judgerDispatcher).createNewTask(100L, 3L, true);
  }

  /**
   * Test case: tests the onSubmissionCreated method. Test data: the judger dispatcher cannot record
   * the outcome of the judging (the database is down). Expected result: the exception is passed on,
   * so that the task is delivered again, rather than swallowed with the submission left pending.
   */
  @Test
  public void testOnSubmissionCreatedPassesOnFailures() throws Exception {
    org.mockito.Mockito.doThrow(new DataAccessResourceFailureException("database down"))
        .when(judgerDispatcher)
        .createNewTask(anyLong(), anyLong(), anyBoolean());

    Assertions.assertThrows(
        DataAccessResourceFailureException.class,
        () -> applicationDispatcher.onSubmissionCreated(100L, 3L, true));
  }

  /**
   * Test case: tests the onSubmissionCreated method. Test data: a task for a submission that does
   * not exist (any more). Expected result: the task is dropped quietly.
   */
  @Test
  public void testOnSubmissionCreatedIgnoresMissingSubmissions() throws Exception {
    org.mockito.Mockito.doThrow(new IllgealSubmissionException("Illegal submission #100"))
        .when(judgerDispatcher)
        .createNewTask(anyLong(), anyLong(), anyBoolean());

    Assertions.assertDoesNotThrow(() -> applicationDispatcher.onSubmissionCreated(100L, 3L, true));
    verify(submissionMapper, never()).updateSubmissionResult(any(Submission.class));
  }

  /**
   * Test case: tests the onErrorOccurred method. Test data: the message broker is unreachable.
   * Expected result: the System Error is still recorded, without an exception: the progress
   * messages are only a live view of what is stored in the database.
   */
  @Test
  public void testOnErrorOccurredWithoutBroker() {
    org.mockito.Mockito.doThrow(new org.springframework.jms.UncategorizedJmsException("down"))
        .when(messageSender)
        .sendMessage(any());

    Assertions.assertDoesNotThrow(
        () -> applicationDispatcher.onErrorOccurred(submission(), "reason"));
    verify(submissionMapper).updateSubmissionResult(any(Submission.class));
  }

  /**
   * Test case: tests the outcomes of a judging. Test data: a judging that was superseded, so the
   * update of its verdict (a System Error, a Compile Error, a verdict) matches nothing. Expected
   * result: the stale verdict is dropped and no message is sent, without an exception.
   */
  @Test
  public void testOutcomeOfASupersededJudgingIsNotSent() {
    when(submissionMapper.updateSubmissionResult(any(Submission.class))).thenReturn(0);
    Map<String, Object> result = new HashMap<>();
    result.put("isSuccessful", false);
    result.put("log", "error");

    applicationDispatcher.onErrorOccurred(submission(), "reason");
    applicationDispatcher.onCompileFinished(submission(), result);
    applicationDispatcher.onAllTestPointsFinished(submission(), new ArrayList<>());

    verify(submissionMapper, org.mockito.Mockito.times(3))
        .updateSubmissionResult(any(Submission.class));
    verify(messageSender, never()).sendMessage(any());
  }

  /**
   * Test case: tests the messages of a judging. Test data: a claimed submission. Expected result:
   * every message (error, compilation, checkpoint, verdict) carries the judging's claim token.
   */
  @Test
  public void testMessagesCarryTheClaimTokenOfTheJudging() {
    Map<String, Object> result = new HashMap<>();
    result.put("isSuccessful", true);
    result.put("log", "");
    applicationDispatcher.onErrorOccurred(submission(), "reason");
    applicationDispatcher.onCompileFinished(submission(), result);
    applicationDispatcher.onOneTestPointFinished(submission(), 1, new HashMap<>());
    applicationDispatcher.onAllTestPointsFinished(submission(), new ArrayList<>());

    ArgumentCaptor<Map<String, Object>> messageCaptor = ArgumentCaptor.forClass(Map.class);
    verify(messageSender, org.mockito.Mockito.times(4)).sendMessage(messageCaptor.capture());
    for (Map<String, Object> message : messageCaptor.getAllValues()) {
      Assertions.assertEquals(CLAIM_TOKEN, message.get("claimToken"), message.toString());
    }
  }

  /**
   * Test case: tests the onJudgingStarted method. Test data: a pending submission. Expected result:
   * it is claimed at the generation of the task, with a new random claim token for every judging,
   * which is returned.
   */
  @Test
  public void testOnJudgingStartedClaimsPendingSubmission() {
    when(submissionMapper.claimSubmission(eq(100L), eq(3L), anyLong(), any())).thenReturn(1);

    Long claimToken = applicationDispatcher.onJudgingStarted(100L, 3L);
    Long nextClaimToken = applicationDispatcher.onJudgingStarted(100L, 3L);

    ArgumentCaptor<Long> claimTokenCaptor = ArgumentCaptor.forClass(Long.class);
    verify(submissionMapper, org.mockito.Mockito.times(2))
        .claimSubmission(eq(100L), eq(3L), claimTokenCaptor.capture(), any());
    Assertions.assertEquals(claimTokenCaptor.getAllValues(), List.of(claimToken, nextClaimToken));
    Assertions.assertNotEquals(claimToken, nextClaimToken);
  }

  /**
   * Test case: tests the onJudgingStarted method. Test data: a submission that is not pending (it
   * has been judged already), or at another generation. Expected result: it is not claimed.
   */
  @Test
  public void testOnJudgingStartedSkipsJudgedSubmission() {
    when(submissionMapper.claimSubmission(eq(100L), eq(3L), anyLong(), any())).thenReturn(0);

    Assertions.assertNull(applicationDispatcher.onJudgingStarted(100L, 3L));
  }

  /**
   * Test case: tests the renewLease method. Test data: a judging that is still current, then one
   * that was superseded. Expected result: the lease of the judging's claim token is renewed, and
   * whether the judging is still current is returned.
   */
  @Test
  public void testRenewLease() {
    when(submissionMapper.renewLease(100L, CLAIM_TOKEN)).thenReturn(1, 0);

    Assertions.assertTrue(applicationDispatcher.renewLease(submission()));
    Assertions.assertFalse(applicationDispatcher.renewLease(submission()));
  }

  /**
   * Test case: tests the onJudgingHandedBack method. Expected result: the claim of the judging is
   * released.
   */
  @Test
  public void testOnJudgingHandedBack() {
    applicationDispatcher.onJudgingHandedBack(submission());

    verify(submissionMapper).releaseSubmission(100L, CLAIM_TOKEN);
  }

  /**
   * Test case: tests the onErrorOccurred method. Expected result: the submission is marked as a
   * system error and an ErrorOccurred message is sent.
   */
  @Test
  public void testOnErrorOccurred() {
    applicationDispatcher.onErrorOccurred(submission(), "This problem has no test data.");

    ArgumentCaptor<Submission> submissionCaptor = ArgumentCaptor.forClass(Submission.class);
    verify(submissionMapper).updateSubmissionResult(submissionCaptor.capture());
    Assertions.assertEquals("SE", submissionCaptor.getValue().getJudgeResultSlug());
    Assertions.assertEquals(
        "System Error.\n\nThis problem has no test data.\n",
        submissionCaptor.getValue().getJudgeLog());

    Map<String, Object> message = captureSentMessage();
    Assertions.assertEquals("ErrorOccurred", message.get("event"));
    Assertions.assertEquals(100L, message.get("submissionId"));
  }

  /**
   * Test case: tests the onCompileFinished method. Test data: a successful compilation. Expected
   * result: the submission is left untouched and a CompileFinished message is sent.
   */
  @Test
  public void testOnCompileFinishedSuccessful() {
    Map<String, Object> result = new HashMap<>();
    result.put("isSuccessful", true);
    result.put("log", "");

    applicationDispatcher.onCompileFinished(submission(), result);

    verify(submissionMapper, never()).updateSubmissionResult(any(Submission.class));

    Map<String, Object> message = captureSentMessage();
    Assertions.assertEquals("CompileFinished", message.get("event"));
    Assertions.assertEquals(true, message.get("isSuccessful"));
  }

  /**
   * Test case: tests the onCompileFinished method. Test data: a failed compilation. Expected
   * result: the submission is marked as a compile error and a CompileFinished message is sent.
   */
  @Test
  public void testOnCompileFinishedFailed() {
    Map<String, Object> result = new HashMap<>();
    result.put("isSuccessful", false);
    result.put("log", "error: expected ';'");

    applicationDispatcher.onCompileFinished(submission(), result);

    ArgumentCaptor<Submission> submissionCaptor = ArgumentCaptor.forClass(Submission.class);
    verify(submissionMapper).updateSubmissionResult(submissionCaptor.capture());
    Assertions.assertEquals("CE", submissionCaptor.getValue().getJudgeResultSlug());

    Map<String, Object> message = captureSentMessage();
    Assertions.assertEquals(false, message.get("isSuccessful"));
  }

  /**
   * Test case: tests the onOneTestPointFinished method. Expected result: a TestPointFinished message
   * carrying the resolved judge-result name is sent.
   */
  @Test
  public void testOnOneTestPointFinished() {
    when(judgeResultMapper.getJudgeResultUsingSlug("AC"))
        .thenReturn(new JudgeResult(1, "AC", "Accepted"));

    Map<String, Object> runtimeResult = new HashMap<>();
    runtimeResult.put("runtimeResult", "AC");
    runtimeResult.put("usedTime", 12);
    runtimeResult.put("usedMemory", 256);
    runtimeResult.put("score", 10);

    applicationDispatcher.onOneTestPointFinished(submission(), 1, runtimeResult);

    Map<String, Object> message = captureSentMessage();
    Assertions.assertEquals("TestPointFinished", message.get("event"));
    Assertions.assertEquals(100L, message.get("submissionId"));
    Assertions.assertEquals(1, message.get("checkpointId"));
    Assertions.assertEquals("Accepted", message.get("runtimeResult"));
    Assertions.assertEquals(12, message.get("usedTime"));
    Assertions.assertEquals(256, message.get("usedMemory"));
    Assertions.assertEquals(10, message.get("score"));
  }

  /**
   * Test case: tests the onOneTestPointFinished method. Test data: a runtime result without an
   * explicit result slug, used to verify the System Error fallbacks. Expected result: the message
   * reports a System Error with zeroed metrics.
   */
  @Test
  public void testOnOneTestPointFinishedFallbacks() {
    when(judgeResultMapper.getJudgeResultUsingSlug("SE")).thenReturn(null);

    Map<String, Object> runtimeResult = new HashMap<>();

    applicationDispatcher.onOneTestPointFinished(submission(), 2, runtimeResult);

    Map<String, Object> message = captureSentMessage();
    Assertions.assertEquals("System Error", message.get("runtimeResult"));
    Assertions.assertEquals(0, message.get("usedTime"));
    Assertions.assertEquals(0, message.get("usedMemory"));
    Assertions.assertEquals(0, message.get("score"));
  }

  /**
   * Test case: tests the onAllTestPointsFinished method. Test data: a fully accepted submission.
   * Expected result: the submission is persisted with the accumulated score and an
   * AllTestPointsFinished message is sent.
   */
  @Test
  public void testOnAllTestPointsFinishedAccepted() {
    when(judgeResultMapper.getJudgeResultUsingSlug("AC"))
        .thenReturn(new JudgeResult(1, "AC", "Accepted"));

    List<Map<String, Object>> runtimeResults = new ArrayList<>();
    runtimeResults.add(buildRuntimeResult("AC", 10, 128, 50));
    runtimeResults.add(buildRuntimeResult("AC", 20, 256, 50));

    applicationDispatcher.onAllTestPointsFinished(submission(), runtimeResults);

    ArgumentCaptor<Submission> submissionCaptor = ArgumentCaptor.forClass(Submission.class);
    verify(submissionMapper).updateSubmissionResult(submissionCaptor.capture());
    Submission submission = submissionCaptor.getValue();
    Assertions.assertEquals("AC", submission.getJudgeResultSlug());
    Assertions.assertEquals(100, submission.getJudgeScore());
    Assertions.assertEquals(30, submission.getUsedTime());
    Assertions.assertEquals(256, submission.getUsedMemory());

    Map<String, Object> message = captureSentMessage();
    Assertions.assertEquals("AllTestPointsFinished", message.get("event"));
    Assertions.assertEquals(100, message.get("totalScore"));
    Assertions.assertEquals(30, message.get("totalTime"));
    Assertions.assertEquals(256, message.get("maxMemory"));
  }

  /**
   * Test case: tests the onAllTestPointsFinished method. Test data: a submission containing a wrong
   * answer. Expected result: the overall result becomes the non-accepted slug and only accepted
   * checkpoints contribute to the score.
   */
  @Test
  public void testOnAllTestPointsFinishedWithWrongAnswer() {
    when(judgeResultMapper.getJudgeResultUsingSlug("AC"))
        .thenReturn(new JudgeResult(1, "AC", "Accepted"));
    when(judgeResultMapper.getJudgeResultUsingSlug("WA"))
        .thenReturn(new JudgeResult(2, "WA", "Wrong Answer"));

    List<Map<String, Object>> runtimeResults = new ArrayList<>();
    runtimeResults.add(buildRuntimeResult("AC", 10, 128, 50));
    runtimeResults.add(buildRuntimeResult("WA", 20, 256, 50));

    applicationDispatcher.onAllTestPointsFinished(submission(), runtimeResults);

    ArgumentCaptor<Submission> submissionCaptor = ArgumentCaptor.forClass(Submission.class);
    verify(submissionMapper).updateSubmissionResult(submissionCaptor.capture());
    Submission submission = submissionCaptor.getValue();
    Assertions.assertEquals("WA", submission.getJudgeResultSlug());
    Assertions.assertEquals(50, submission.getJudgeScore());

    Map<String, Object> message = captureSentMessage();
    Assertions.assertEquals("Wrong Answer", message.get("runtimeResult"));
  }

  /**
   * Test case: tests the onAllTestPointsFinished method. Test data: several failing checkpoints
   * with different verdicts. Expected result: the overall verdict is the first failing one.
   */
  @Test
  public void testOnAllTestPointsFinishedReportsFirstFailure() {
    when(judgeResultMapper.getJudgeResultUsingSlug("TLE"))
        .thenReturn(new JudgeResult(3, "TLE", "Time Limit Exceeded"));

    List<Map<String, Object>> runtimeResults = new ArrayList<>();
    runtimeResults.add(buildRuntimeResult("AC", 10, 128, 25));
    runtimeResults.add(buildRuntimeResult("TLE", 1000, 128, 25));
    runtimeResults.add(buildRuntimeResult("WA", 20, 128, 25));
    runtimeResults.add(buildRuntimeResult("RE", 20, 128, 25));

    applicationDispatcher.onAllTestPointsFinished(submission(), runtimeResults);

    ArgumentCaptor<Submission> submissionCaptor = ArgumentCaptor.forClass(Submission.class);
    verify(submissionMapper).updateSubmissionResult(submissionCaptor.capture());
    Assertions.assertEquals("TLE", submissionCaptor.getValue().getJudgeResultSlug());
    Assertions.assertEquals(25, submissionCaptor.getValue().getJudgeScore());
    Assertions.assertEquals("Time Limit Exceeded", captureSentMessage().get("runtimeResult"));
  }

  /**
   * Test case: tests the onAllTestPointsFinished method. Test data: no checkpoint result at all.
   * Expected result: a System Error, never an Accepted with nothing checked.
   */
  @Test
  public void testOnAllTestPointsFinishedWithoutCheckpointsIsSystemError() {
    applicationDispatcher.onAllTestPointsFinished(submission(), new ArrayList<>());

    ArgumentCaptor<Submission> submissionCaptor = ArgumentCaptor.forClass(Submission.class);
    verify(submissionMapper).updateSubmissionResult(submissionCaptor.capture());
    Assertions.assertEquals("SE", submissionCaptor.getValue().getJudgeResultSlug());
  }

  /**
   * Builds Submission#100, claimed by a judging.
   *
   * @return the submission
   */
  private Submission submission() {
    Submission submission = new Submission();
    submission.setSubmissionId(100L);
    submission.setClaimToken(CLAIM_TOKEN);
    return submission;
  }

  /**
   * Builds a runtime-result map for a single checkpoint.
   *
   * @param slug - the unique English abbreviation of the judging result
   * @param usedTime - the running time in milliseconds
   * @param usedMemory - the memory usage in kilobytes
   * @param score - the score for the checkpoint
   * @return a runtime-result map
   */
  private Map<String, Object> buildRuntimeResult(
      String slug, int usedTime, int usedMemory, int score) {
    Map<String, Object> runtimeResult = new HashMap<>();
    runtimeResult.put("runtimeResult", slug);
    runtimeResult.put("usedTime", usedTime);
    runtimeResult.put("usedMemory", usedMemory);
    runtimeResult.put("score", score);
    return runtimeResult;
  }

  /**
   * Captures the most recent message handed to the message sender.
   *
   * @return the captured message
   */
  @SuppressWarnings("unchecked")
  private Map<String, Object> captureSentMessage() {
    ArgumentCaptor<Map<String, Object>> messageCaptor = ArgumentCaptor.forClass(Map.class);
    verify(messageSender).sendMessage(messageCaptor.capture());
    return messageCaptor.getValue();
  }

  /** Records every verdict: the judgings of the tests are current, unless a test says otherwise. */
  @BeforeEach
  public void setUp() {
    when(submissionMapper.updateSubmissionResult(any(Submission.class))).thenReturn(1);
  }

  /** The claim token of the judgings of the tests. */
  private static final long CLAIM_TOKEN = -4242L;

  /** The mocked Dispatcher object. */
  @Mock private Dispatcher judgerDispatcher;

  /** The mocked MessageSender object. */
  @Mock private MessageSender messageSender;

  /** The mocked SubmissionMapper object. */
  @Mock private SubmissionMapper submissionMapper;

  /** The mocked JudgeResultMapper object. */
  @Mock private JudgeResultMapper judgeResultMapper;

  /** The ApplicationDispatcher object under test. */
  @InjectMocks private ApplicationDispatcher applicationDispatcher;
}
