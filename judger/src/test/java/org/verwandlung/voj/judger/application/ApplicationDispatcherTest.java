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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import org.verwandlung.voj.judger.core.Dispatcher;
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
    applicationDispatcher.onSubmissionCreated(100L);
    verify(judgerDispatcher).createNewTask(100L);
  }

  /**
   * Test case: tests the onSubmissionCreated method. Test data: the judger dispatcher throws an
   * exception. Expected result: the exception is swallowed and the submission is marked as a system
   * error so it does not stay stuck pending.
   */
  @Test
  public void testOnSubmissionCreatedMarksSystemErrorOnFailure() throws Exception {
    org.mockito.Mockito.doThrow(new InterruptedException("interrupted"))
        .when(judgerDispatcher)
        .createNewTask(anyLong());
    when(submissionMapper.getSubmission(100L)).thenReturn(new Submission());

    Assertions.assertDoesNotThrow(() -> applicationDispatcher.onSubmissionCreated(100L));

    ArgumentCaptor<Submission> submissionCaptor = ArgumentCaptor.forClass(Submission.class);
    verify(submissionMapper).updateSubmission(submissionCaptor.capture());
    Assertions.assertEquals("SE", submissionCaptor.getValue().getJudgeResultSlug());
  }

  /**
   * Test case: tests the onErrorOccurred method. Test data: a submission that no longer exists.
   * Expected result: no update is attempted and the call does not throw.
   */
  @Test
  public void testOnErrorOccurredWithMissingSubmission() {
    when(submissionMapper.getSubmission(100L)).thenReturn(null);

    Assertions.assertDoesNotThrow(() -> applicationDispatcher.onErrorOccurred(100L));
    verify(submissionMapper, never()).updateSubmission(any(Submission.class));
  }

  /**
   * Test case: tests the onErrorOccurred method. Expected result: the submission is marked as a
   * system error and an ErrorOccurred message is sent.
   */
  @Test
  public void testOnErrorOccurred() {
    when(submissionMapper.getSubmission(100L)).thenReturn(new Submission());

    applicationDispatcher.onErrorOccurred(100L);

    ArgumentCaptor<Submission> submissionCaptor = ArgumentCaptor.forClass(Submission.class);
    verify(submissionMapper).updateSubmission(submissionCaptor.capture());
    Assertions.assertEquals("SE", submissionCaptor.getValue().getJudgeResultSlug());

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

    applicationDispatcher.onCompileFinished(100L, result);

    verify(submissionMapper, never()).updateSubmission(any(Submission.class));

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
    when(submissionMapper.getSubmission(100L)).thenReturn(new Submission());
    Map<String, Object> result = new HashMap<>();
    result.put("isSuccessful", false);
    result.put("log", "error: expected ';'");

    applicationDispatcher.onCompileFinished(100L, result);

    ArgumentCaptor<Submission> submissionCaptor = ArgumentCaptor.forClass(Submission.class);
    verify(submissionMapper).updateSubmission(submissionCaptor.capture());
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

    applicationDispatcher.onOneTestPointFinished(100L, 1, runtimeResult);

    Map<String, Object> message = captureSentMessage();
    Assertions.assertEquals("TestPointFinished", message.get("event"));
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

    applicationDispatcher.onOneTestPointFinished(100L, 2, runtimeResult);

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
    when(submissionMapper.getSubmission(100L)).thenReturn(new Submission());
    when(judgeResultMapper.getJudgeResultUsingSlug("AC"))
        .thenReturn(new JudgeResult(1, "AC", "Accepted"));

    List<Map<String, Object>> runtimeResults = new ArrayList<>();
    runtimeResults.add(buildRuntimeResult("AC", 10, 128, 50));
    runtimeResults.add(buildRuntimeResult("AC", 20, 256, 50));

    applicationDispatcher.onAllTestPointsFinished(100L, runtimeResults);

    ArgumentCaptor<Submission> submissionCaptor = ArgumentCaptor.forClass(Submission.class);
    verify(submissionMapper).updateSubmission(submissionCaptor.capture());
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
    when(submissionMapper.getSubmission(100L)).thenReturn(new Submission());
    when(judgeResultMapper.getJudgeResultUsingSlug("AC"))
        .thenReturn(new JudgeResult(1, "AC", "Accepted"));
    when(judgeResultMapper.getJudgeResultUsingSlug("WA"))
        .thenReturn(new JudgeResult(2, "WA", "Wrong Answer"));

    List<Map<String, Object>> runtimeResults = new ArrayList<>();
    runtimeResults.add(buildRuntimeResult("AC", 10, 128, 50));
    runtimeResults.add(buildRuntimeResult("WA", 20, 256, 50));

    applicationDispatcher.onAllTestPointsFinished(100L, runtimeResults);

    ArgumentCaptor<Submission> submissionCaptor = ArgumentCaptor.forClass(Submission.class);
    verify(submissionMapper).updateSubmission(submissionCaptor.capture());
    Submission submission = submissionCaptor.getValue();
    Assertions.assertEquals("WA", submission.getJudgeResultSlug());
    Assertions.assertEquals(50, submission.getJudgeScore());

    Map<String, Object> message = captureSentMessage();
    Assertions.assertEquals("Wrong Answer", message.get("runtimeResult"));
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
