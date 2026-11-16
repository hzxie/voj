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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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

import org.verwandlung.voj.judger.core.CheckpointResult;
import org.verwandlung.voj.judger.core.CompileResult;
import org.verwandlung.voj.judger.core.Superseded;
import org.verwandlung.voj.judger.core.Verdict;
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
   * Test case: tests the outcomes of a judging. Test data: a judging that was superseded, so the
   * update of its verdict (a System Error, a Compile Error, a verdict) matches nothing. Expected
   * result: the judging is told that it was superseded, and no message is sent.
   */
  @Test
  public void testOutcomeOfASupersededJudgingIsNotSent() throws Exception {
    when(submissionMapper.updateSubmissionResult(any(Submission.class))).thenReturn(0);
    CompileResult result = new CompileResult(Verdict.RUNTIME_ERROR, "error");

    Assertions.assertThrows(
        Superseded.class, () -> applicationDispatcher.onErrorOccurred(submission(), "reason"));
    Assertions.assertThrows(
        Superseded.class, () -> applicationDispatcher.onCompileFinished(submission(), result));
    Assertions.assertThrows(
        Superseded.class,
        () -> applicationDispatcher.onAllTestPointsFinished(submission(), List.of(accepted(0))));

    verify(submissionMapper, org.mockito.Mockito.times(3))
        .updateSubmissionResult(any(Submission.class));
    verify(messageSender, never()).sendMessage(any());
  }

  /**
   * Test case: tests the messages of a judging. Test data: a claimed submission. Expected result:
   * every message (error, compilation, checkpoint, verdict) carries the judging's claim token.
   */
  @Test
  public void testMessagesCarryTheClaimTokenOfTheJudging() throws Exception {
    CompileResult result = new CompileResult(Verdict.NORMAL, "");
    applicationDispatcher.onErrorOccurred(submission(), "reason");
    applicationDispatcher.onCompileFinished(submission(), result);
    applicationDispatcher.onOneTestPointFinished(submission(), accepted(1));
    applicationDispatcher.onAllTestPointsFinished(submission(), List.of(accepted(1)));

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
  public void testClaimSubmissionClaimsTheNextOne() throws Exception {
    when(submissionMapper.getNextSubmissionId(LANGUAGES)).thenReturn(100L, 101L);
    when(submissionMapper.claimSubmission(
            eq(100L), anyLong(), eq(1001L), eq(60L), eq(ApplicationDispatcher.INTERRUPTED_REASON)))
        .thenReturn(0);
    when(submissionMapper.claimSubmission(
            eq(101L), anyLong(), eq(1001L), eq(60L), eq(ApplicationDispatcher.INTERRUPTED_REASON)))
        .thenReturn(1);
    Submission pending = new Submission();
    pending.setSubmissionId(101L);
    when(submissionMapper.getSubmission(101L)).thenReturn(pending);

    Submission submission = applicationDispatcher.claimSubmission(LANGUAGES, 60);

    Assertions.assertSame(pending, submission);
    ArgumentCaptor<Long> claimTokenCaptor = ArgumentCaptor.forClass(Long.class);
    org.mockito.InOrder inOrder = org.mockito.Mockito.inOrder(submissionMapper);
    inOrder
        .verify(submissionMapper)
        .claimSubmission(eq(100L), claimTokenCaptor.capture(), anyLong(), eq(60L), any());
    inOrder
        .verify(submissionMapper)
        .claimSubmission(eq(101L), claimTokenCaptor.capture(), anyLong(), eq(60L), any());
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
  public void testClaimSubmissionWithoutPendingSubmission() throws Exception {
    when(submissionMapper.getNextSubmissionId(LANGUAGES)).thenReturn(null);

    Assertions.assertNull(applicationDispatcher.claimSubmission(LANGUAGES, 60));
    verify(submissionMapper, never())
        .claimSubmission(anyLong(), anyLong(), anyLong(), anyLong(), any());
  }

  /**
   * Test case: tests the renewLease method. Test data: a judging that is still current, then one
   * that was superseded. Expected result: the lease of the judging's claim token is renewed for the
   * given time, and whether the judging is still current is returned.
   */
  @Test
  public void testRenewLease() throws Exception {
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
  public void testReleaseSubmission() throws Exception {
    when(submissionMapper.releaseSubmission(100L, CLAIM_TOKEN)).thenReturn(1, 0);

    Assertions.assertTrue(applicationDispatcher.releaseSubmission(submission()));
    Assertions.assertFalse(applicationDispatcher.releaseSubmission(submission()));
  }

  /**
   * Test case: tests the requeueSubmission method. Test data: a judging that is still current, with
   * a short reason, then one with a reason longer than the column takes, then one that was
   * superseded. Expected result: the submission of the judging's claim token is released with the
   * reason (cut to 255 characters), and whether it was is returned.
   */
  @Test
  public void testRequeueSubmission() throws Exception {
    String longReason = "x".repeat(300);
    when(submissionMapper.requeueSubmission(eq(100L), eq(CLAIM_TOKEN), anyString()))
        .thenReturn(1, 1, 0);

    Assertions.assertTrue(applicationDispatcher.requeueSubmission(submission(), "Failed."));
    Assertions.assertTrue(applicationDispatcher.requeueSubmission(submission(), longReason));
    Assertions.assertFalse(applicationDispatcher.requeueSubmission(submission(), "Failed."));
    verify(submissionMapper, times(2)).requeueSubmission(100L, CLAIM_TOKEN, "Failed.");
    verify(submissionMapper).requeueSubmission(100L, CLAIM_TOKEN, "x".repeat(255));
  }

  /**
   * Test case: tests the onErrorOccurred method. Test data: the database fails the first two times
   * the System Error is recorded (e.g. while it fails over), then three times. Expected result: the
   * verdict is recorded at the third try, and the message is sent; when every try fails, the
   * failure is thrown after three tries, and nothing is sent.
   */
  @Test
  public void testVerdictIsRecordedDespiteShortOutages() throws Exception {
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
  public void testRefusedVerdictIsNotTriedAgain() throws Exception {
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
  public void testJudgeResultNamesAreReadOnce() throws Exception {
    when(judgeResultMapper.getJudgeResultUsingSlug("AC"))
        .thenReturn(new JudgeResult(1, "AC", "Accepted"));

    for (int checkpointId = 0; checkpointId < 3; ++checkpointId) {
      applicationDispatcher.onOneTestPointFinished(submission(), accepted(checkpointId));
    }

    verify(judgeResultMapper, times(1)).getJudgeResultUsingSlug("AC");
    verify(messageSender, times(3)).sendMessage(any());
  }

  /**
   * Test case: tests the onErrorOccurred method. Expected result: the submission is marked as a
   * system error and an ErrorOccurred message is sent.
   */
  @Test
  public void testOnErrorOccurred() throws Exception {
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
  public void testOnCompileFinishedSuccessful() throws Exception {
    CompileResult result = new CompileResult(Verdict.NORMAL, "");

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
  public void testOnCompileFinishedFailed() throws Exception {
    CompileResult result = new CompileResult(Verdict.RUNTIME_ERROR, "error: expected ';'");

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
  public void testOnOneTestPointFinished() throws Exception {
    when(judgeResultMapper.getJudgeResultUsingSlug("AC"))
        .thenReturn(new JudgeResult(1, "AC", "Accepted"));

    applicationDispatcher.onOneTestPointFinished(
        submission(), new CheckpointResult(1, "AC", 12, 256, 10));

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
   * Test case: tests the onAllTestPointsFinished method. Test data: a fully accepted submission.
   * Expected result: the submission is persisted with the accumulated score and an
   * AllTestPointsFinished message is sent.
   */
  @Test
  public void testOnAllTestPointsFinishedAccepted() throws Exception {
    when(judgeResultMapper.getJudgeResultUsingSlug("AC"))
        .thenReturn(new JudgeResult(1, "AC", "Accepted"));

    applicationDispatcher.onAllTestPointsFinished(
        submission(),
        List.of(
            new CheckpointResult(0, "AC", 10, 128, 50), new CheckpointResult(1, "AC", 20, 256, 50)));

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
   * answer. Expected result: the overall result becomes the non-accepted slug, with the score the
   * checkpoints earned.
   */
  @Test
  public void testOnAllTestPointsFinishedWithWrongAnswer() throws Exception {
    when(judgeResultMapper.getJudgeResultUsingSlug("AC"))
        .thenReturn(new JudgeResult(1, "AC", "Accepted"));
    when(judgeResultMapper.getJudgeResultUsingSlug("WA"))
        .thenReturn(new JudgeResult(2, "WA", "Wrong Answer"));

    applicationDispatcher.onAllTestPointsFinished(
        submission(),
        List.of(
            new CheckpointResult(0, "AC", 10, 128, 50), new CheckpointResult(1, "WA", 20, 256, 0)));

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
  public void testOnAllTestPointsFinishedReportsFirstFailure() throws Exception {
    when(judgeResultMapper.getJudgeResultUsingSlug("TLE"))
        .thenReturn(new JudgeResult(3, "TLE", "Time Limit Exceeded"));

    applicationDispatcher.onAllTestPointsFinished(
        submission(),
        List.of(
            new CheckpointResult(0, "AC", 10, 128, 25),
            new CheckpointResult(1, "TLE", 1000, 128, 0),
            new CheckpointResult(2, "WA", 20, 128, 0),
            new CheckpointResult(3, "RE", 20, 128, 0)));

    ArgumentCaptor<Submission> submissionCaptor = ArgumentCaptor.forClass(Submission.class);
    verify(submissionMapper).updateSubmissionResult(submissionCaptor.capture());
    Assertions.assertEquals("TLE", submissionCaptor.getValue().getJudgeResultSlug());
    Assertions.assertEquals(25, submissionCaptor.getValue().getJudgeScore());
    Assertions.assertEquals("Time Limit Exceeded", captureSentMessage().get("runtimeResult"));
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
   * Builds the verdict of an Accepted checkpoint.
   *
   * @param checkpointId - the identifier of the checkpoint
   * @return the verdict
   */
  private static CheckpointResult accepted(int checkpointId) {
    return new CheckpointResult(checkpointId, "AC", 1, 1, 1);
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
    when(judgerAccount.getUid()).thenReturn(1001L);
  }

  /** The claim token of the judgings of the tests. */
  private static final long CLAIM_TOKEN = -4242L;

  /** The languages of the judger of the tests. */
  private static final List<Integer> LANGUAGES = List.of(2, 3);

  /** The mocked MessageSender object. */
  @Mock private MessageSender messageSender;

  /** The mocked SubmissionMapper object. */
  @Mock private SubmissionMapper submissionMapper;

  /** The mocked JudgeResultMapper object. */
  @Mock private JudgeResultMapper judgeResultMapper;

  /** The mocked account of the judger. */
  @Mock private JudgerAccount judgerAccount;

  /** The ApplicationDispatcher object under test. */
  @InjectMocks private ApplicationDispatcher applicationDispatcher;
}
