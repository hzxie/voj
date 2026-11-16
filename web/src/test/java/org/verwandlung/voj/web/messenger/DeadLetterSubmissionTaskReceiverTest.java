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
package org.verwandlung.voj.web.messenger;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.jms.JMSException;
import jakarta.jms.MapMessage;
import jakarta.jms.Message;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import org.verwandlung.voj.web.service.SubmissionService;
import org.verwandlung.voj.web.service.SubmissionService.JudgingTaskFailure;

/**
 * The test class for DeadLetterSubmissionTaskReceiver.
 *
 * @author Haozhe Xie
 */
public class DeadLetterSubmissionTaskReceiverTest {
  /** Wires a mocked SubmissionService into a fresh receiver before every test. */
  @BeforeEach
  public void setUp() {
    submissionService = mock(SubmissionService.class);
    receiver = new DeadLetterSubmissionTaskReceiver();
    ReflectionTestUtils.setField(receiver, "submissionService", submissionService);
  }

  /** Test case: tests onMessage(Message). Test data: a SubmissionCreated task the broker dead-lettered because it expired. Expected: the submission is marked unjudgeable because no judger is available. */
  @Test
  public void testOnMessageMarksExpiredSubmission() throws JMSException {
    receiver.onMessage(
        deadLetteredTask(1000L, "java.lang.Throwable: Message Expired. Expiration:1763290000000"));

    verify(submissionService)
        .markSubmissionUnjudgeable(1000L, null, JudgingTaskFailure.NO_JUDGER_AVAILABLE);
  }

  /**
   * Test case: tests onMessage(Message). Test data: a dead-lettered task that carries the judge
   * generation it was queued with. Expected: the submission is marked unjudgeable only for that
   * generation (SubmissionService leaves a submission rejudged since then alone).
   */
  @Test
  public void testOnMessagePassesTheJudgeGenerationOfTheTask() throws JMSException {
    MapMessage message = deadLetteredTask(1000L, "java.lang.Throwable: Message Expired.");
    when(message.itemExists("generation")).thenReturn(true);
    when(message.getLong("generation")).thenReturn(3L);

    receiver.onMessage(message);

    verify(submissionService)
        .markSubmissionUnjudgeable(1000L, 3L, JudgingTaskFailure.NO_JUDGER_AVAILABLE);
  }

  /** Test case: tests onMessage(Message). Test data: a SubmissionCreated task the broker dead-lettered after the judgers failed it up to the redelivery limit. Expected: the submission is marked unjudgeable because judging it failed repeatedly, not because no judger is available. */
  @Test
  public void testOnMessageMarksRepeatedlyFailedSubmission() throws JMSException {
    receiver.onMessage(
        deadLetteredTask(
            1000L,
            "java.lang.Throwable: Delivery[6] exceeds redelivery policy limit:RedeliveryPolicy"
                + " {maximumRedeliveries=5}, cause:null"));

    verify(submissionService)
        .markSubmissionUnjudgeable(1000L, null, JudgingTaskFailure.JUDGING_FAILED_REPEATEDLY);
  }

  /** Test case: tests onMessage(Message). Test data: a dead-lettered task without a recorded cause. Expected: the submission is marked unjudgeable without claiming a reason. */
  @Test
  public void testOnMessageMarksSubmissionWithUnknownCause() throws JMSException {
    receiver.onMessage(deadLetteredTask(1000L, null));

    verify(submissionService).markSubmissionUnjudgeable(1000L, null, JudgingTaskFailure.UNKNOWN);
  }

  /** Test case: tests the judge logs of the failures. Expected: each says what happened, and only the expired task mentions the language. */
  @Test
  public void testJudgeLogsSayWhatHappened() {
    Assertions.assertTrue(
        JudgingTaskFailure.NO_JUDGER_AVAILABLE.getJudgeLog().contains("No available judger"));
    Assertions.assertTrue(
        JudgingTaskFailure.JUDGING_FAILED_REPEATEDLY.getJudgeLog().contains("failed repeatedly"));
    Assertions.assertFalse(
        JudgingTaskFailure.JUDGING_FAILED_REPEATEDLY.getJudgeLog().contains("language"));
    for (JudgingTaskFailure failure : JudgingTaskFailure.values()) {
      Assertions.assertTrue(failure.getJudgeLog().startsWith("System Error."), failure.name());
    }
  }

  /** Builds a dead-lettered SubmissionCreated task with the cause the broker recorded. */
  private static MapMessage deadLetteredTask(long submissionId, String cause) throws JMSException {
    MapMessage message = mock(MapMessage.class);
    when(message.getString("event")).thenReturn("SubmissionCreated");
    when(message.getLong("submissionId")).thenReturn(submissionId);
    when(message.getStringProperty(DeadLetterSubmissionTaskReceiver.DLQ_DELIVERY_FAILURE_CAUSE))
        .thenReturn(cause);
    return message;
  }

  /** Test case: tests onMessage(Message). Test data: a message whose event is not SubmissionCreated. Expected: the submission service is never touched. */
  @Test
  public void testOnMessageIgnoresUnrelatedEvent() throws JMSException {
    MapMessage message = mock(MapMessage.class);
    when(message.getString("event")).thenReturn("SomethingElse");

    receiver.onMessage(message);

    verify(submissionService, never())
        .markSubmissionUnjudgeable(
            org.mockito.ArgumentMatchers.anyLong(),
            org.mockito.ArgumentMatchers.any(),
            org.mockito.ArgumentMatchers.any());
  }

  /** Test case: tests onMessage(Message). Test data: a message that is not a MapMessage. Expected: the message is ignored without touching the submission service. */
  @Test
  public void testOnMessageIgnoresNonMapMessage() {
    Message message = mock(Message.class);

    receiver.onMessage(message);

    verify(submissionService, never())
        .markSubmissionUnjudgeable(
            org.mockito.ArgumentMatchers.anyLong(),
            org.mockito.ArgumentMatchers.any(),
            org.mockito.ArgumentMatchers.any());
  }

  /** Test case: tests onMessage(Message). Test data: a MapMessage that throws while being read. Expected: the JMSException is swallowed rather than propagated. */
  @Test
  public void testOnMessageSwallowsJmsException() throws JMSException {
    MapMessage message = mock(MapMessage.class);
    when(message.getString("event")).thenThrow(new JMSException("broken"));

    // Must not throw out of the listener; the container would otherwise redeliver endlessly.
    receiver.onMessage(message);

    verify(submissionService, never())
        .markSubmissionUnjudgeable(
            org.mockito.ArgumentMatchers.anyLong(),
            org.mockito.ArgumentMatchers.any(),
            org.mockito.ArgumentMatchers.any());
  }

  /** The mocked SubmissionService the receiver finalizes submissions through. */
  private SubmissionService submissionService;

  /** The DeadLetterSubmissionTaskReceiver object under test. */
  private DeadLetterSubmissionTaskReceiver receiver;
}
