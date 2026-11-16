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
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
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
import org.springframework.test.util.ReflectionTestUtils;

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
   * Test case: tests the claimSubmission method. Test data: the next submission in the languages of
   * the judger, 100, which another judger claims first, then 101. Expected result: 101 is claimed
   * and read once claimed, with a new random claim token for every claim, which it carries.
   */
  @Test
  public void testClaimSubmissionClaimsTheNextOne() {
    when(submissionMapper.getNextSubmissionId(LANGUAGES, SKIPPED)).thenReturn(100L, 101L);
    when(submissionMapper.claimSubmission(eq(100L), anyLong(), any(), eq(60L))).thenReturn(0);
    when(submissionMapper.claimSubmission(eq(101L), anyLong(), any(), eq(60L))).thenReturn(1);
    Submission pending = new Submission();
    pending.setSubmissionId(101L);
    when(submissionMapper.getSubmission(101L)).thenReturn(pending);

    Submission submission = applicationDispatcher.claimSubmission(LANGUAGES, SKIPPED, 60);

    Assertions.assertSame(pending, submission);
    ArgumentCaptor<Long> claimTokenCaptor = ArgumentCaptor.forClass(Long.class);
    org.mockito.InOrder inOrder = org.mockito.Mockito.inOrder(submissionMapper);
    inOrder
        .verify(submissionMapper)
        .claimSubmission(eq(100L), claimTokenCaptor.capture(), any(), eq(60L));
    inOrder
        .verify(submissionMapper)
        .claimSubmission(eq(101L), claimTokenCaptor.capture(), any(), eq(60L));
    inOrder.verify(submissionMapper).getSubmission(101L);
    List<Long> claimTokens = claimTokenCaptor.getAllValues();
    Assertions.assertNotEquals(claimTokens.get(0), claimTokens.get(1));
    Assertions.assertEquals(claimTokens.get(1), submission.getClaimToken());
    verify(submissionMapper, never()).getSubmission(100L);
  }

  /**
   * Test case: tests the claimSubmission method. Test data: no pending submission in the languages
   * of the judger. Expected result: nothing is claimed.
   */
  @Test
  public void testClaimSubmissionWithoutPendingSubmission() {
    when(submissionMapper.getNextSubmissionId(LANGUAGES, SKIPPED)).thenReturn(null);

    Assertions.assertNull(applicationDispatcher.claimSubmission(LANGUAGES, SKIPPED, 60));
    verify(submissionMapper, never()).claimSubmission(anyLong(), anyLong(), any(), anyLong());
  }

  /**
   * Test case: tests the renewLease method. Test data: a judging that is still current, then one
   * that was superseded. Expected result: the lease of the judging's claim token is renewed for the
   * given time, and whether the judging is still current is returned.
   */
  @Test
  public void testRenewLease() {
    when(submissionMapper.renewLease(100L, CLAIM_TOKEN, 60L)).thenReturn(1, 0);

    Assertions.assertTrue(applicationDispatcher.renewLease(submission(), 60));
    Assertions.assertFalse(applicationDispatcher.renewLease(submission(), 60));
  }

  /**
   * Test case: tests the releaseSubmission method. Test data: a judging that is still current, then
   * one that was superseded. Expected result: the submission of the judging's claim token is
   * released, and whether it was is returned.
   */
  @Test
  public void testReleaseSubmission() {
    when(submissionMapper.releaseSubmission(100L, CLAIM_TOKEN)).thenReturn(1, 0);

    Assertions.assertTrue(applicationDispatcher.releaseSubmission(submission()));
    Assertions.assertFalse(applicationDispatcher.releaseSubmission(submission()));
  }

  /**
   * Test case: tests the onErrorOccurred method. Test data: the database fails the first two times
   * the System Error is recorded (e.g. while it fails over), then three times. Expected result: the
   * verdict is recorded at the third try, and the message is sent; when every try fails, the
   * failure is thrown after three tries, and nothing is sent.
   */
  @Test
  public void testVerdictIsRecordedDespiteShortOutages() {
    ReflectionTestUtils.setField(applicationDispatcher, "retryDelay", 1L);
    DataAccessResourceFailureException outage = new DataAccessResourceFailureException("down");
    when(submissionMapper.updateSubmissionResult(any(Submission.class)))
        .thenThrow(outage, outage)
        .thenReturn(1)
        .thenThrow(outage);

    applicationDispatcher.onErrorOccurred(submission(), "No test data.");
    verify(submissionMapper, times(3)).updateSubmissionResult(any(Submission.class));
    Assertions.assertEquals("ErrorOccurred", captureSentMessage().get("event"));

    Assertions.assertThrows(
        DataAccessResourceFailureException.class,
        () -> applicationDispatcher.onErrorOccurred(submission(), "No test data."));
    verify(submissionMapper, times(6)).updateSubmissionResult(any(Submission.class));
    verify(messageSender, times(1)).sendMessage(any());
  }

  /**
   * Test case: tests the onErrorOccurred method. Test data: the database refuses the verdict (a
   * value its column does not take). Expected result: it is not tried again, which would be
   * refused again.
   */
  @Test
  public void testRefusedVerdictIsNotTriedAgain() {
    when(submissionMapper.updateSubmissionResult(any(Submission.class)))
        .thenThrow(new org.springframework.dao.DataIntegrityViolationException("Data too long"));

    Assertions.assertThrows(
        org.springframework.dao.DataIntegrityViolationException.class,
        () -> applicationDispatcher.onErrorOccurred(submission(), "No test data."));
    verify(submissionMapper, times(1)).updateSubmissionResult(any(Submission.class));
    Assertions.assertTrue(
        ApplicationDispatcher.isUnreachable(
            new org.springframework.dao.QueryTimeoutException("timed out")));
    Assertions.assertTrue(
        ApplicationDispatcher.isUnreachable(
            new org.springframework.jdbc.CannotGetJdbcConnectionException("no connection")));
  }

  /**
   * Test case: tests the onOneTestPointFinished method. Test data: several checkpoints judged
   * Accepted. Expected result: the name of the judge result is read from the database once.
   */
  @Test
  public void testJudgeResultNamesAreReadOnce() {
    when(judgeResultMapper.getJudgeResultUsingSlug("AC"))
        .thenReturn(new JudgeResult(1, "AC", "Accepted"));

    for (int checkpointId = 0; checkpointId < 3; ++checkpointId) {
      applicationDispatcher.onOneTestPointFinished(
          submission(), checkpointId, buildRuntimeResult("AC", 1, 1, 1));
    }

    verify(judgeResultMapper, times(1)).getJudgeResultUsingSlug("AC");
    verify(messageSender, times(3)).sendMessage(any());
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

  /** The languages of the judger of the tests. */
  private static final List<Integer> LANGUAGES = List.of(2, 3);

  /** The problems the judger of the tests leaves to others. */
  private static final List<Long> SKIPPED = List.of(1001L);

  /** The mocked MessageSender object. */
  @Mock private MessageSender messageSender;

  /** The mocked SubmissionMapper object. */
  @Mock private SubmissionMapper submissionMapper;

  /** The mocked JudgeResultMapper object. */
  @Mock private JudgeResultMapper judgeResultMapper;

  /** The ApplicationDispatcher object under test. */
  @InjectMocks private ApplicationDispatcher applicationDispatcher;
}
