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
package org.verwandlung.voj.judger.messenger;

import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.jms.JMSException;
import jakarta.jms.MapMessage;
import jakarta.jms.Message;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.verwandlung.voj.judger.application.ApplicationDispatcher;
import org.verwandlung.voj.judger.config.JmsConfig;

/**
 * Test class for MessageReceiver.
 *
 * @author Haozhe Xie
 */
@ExtendWith(MockitoExtension.class)
public class MessageReceiverTest {
  /**
   * Test case: tests the onMessage method. Test data: a MapMessage carrying a SubmissionCreated
   * event. Expected result: the dispatcher's onSubmissionCreated callback is invoked with the
   * submission identifier, the judging generation and that this is not the task's last delivery.
   */
  @Test
  public void testOnMessageSubmissionCreated() throws JMSException {
    MapMessage mapMessage = org.mockito.Mockito.mock(MapMessage.class);
    when(mapMessage.getString("event")).thenReturn("SubmissionCreated");
    when(mapMessage.getLong("submissionId")).thenReturn(1234L);
    when(mapMessage.getLong("generation")).thenReturn(3L);
    when(mapMessage.getIntProperty("JMSXDeliveryCount")).thenReturn(1);

    messageReceiver.onMessage(mapMessage);

    verify(dispatcher).onSubmissionCreated(1234L, 3L, false);
  }

  /**
   * Test case: tests the onMessage method. Test data: the deliveries of a task that was handed back
   * {@link JmsConfig#MAXIMUM_REDELIVERIES} times before its last one. Expected result: only the
   * last one, after which the broker client dead-letters the task, is told it is the last.
   */
  @Test
  public void testOnMessageTellsTheLastDelivery() throws JMSException {
    MapMessage mapMessage = org.mockito.Mockito.mock(MapMessage.class);
    when(mapMessage.getString("event")).thenReturn("SubmissionCreated");
    when(mapMessage.getLong("submissionId")).thenReturn(1234L);
    when(mapMessage.getIntProperty("JMSXDeliveryCount"))
        .thenReturn(JmsConfig.MAXIMUM_REDELIVERIES, JmsConfig.MAXIMUM_REDELIVERIES + 1);

    messageReceiver.onMessage(mapMessage);
    verify(dispatcher).onSubmissionCreated(1234L, 0L, false);
    messageReceiver.onMessage(mapMessage);
    verify(dispatcher).onSubmissionCreated(1234L, 0L, true);
  }

  /**
   * Test case: tests the onMessage method. Test data: a MapMessage carrying an unknown event.
   * Expected result: no callback is invoked.
   */
  @Test
  public void testOnMessageUnknownEvent() throws JMSException {
    MapMessage mapMessage = org.mockito.Mockito.mock(MapMessage.class);
    when(mapMessage.getString("event")).thenReturn("SomethingElse");

    messageReceiver.onMessage(mapMessage);

    verify(dispatcher, never()).onSubmissionCreated(anyLong(), anyLong(), anyBoolean());
  }

  /**
   * Test case: tests the onMessage method. Test data: a message that is not a MapMessage. Expected
   * result: no callback is invoked.
   */
  @Test
  public void testOnMessageNotMapMessage() {
    Message message = org.mockito.Mockito.mock(Message.class);

    messageReceiver.onMessage(message);

    verify(dispatcher, never()).onSubmissionCreated(anyLong(), anyLong(), anyBoolean());
  }

  /**
   * Test case: tests the onMessage method. Test data: a MapMessage that throws a JMSException while
   * being read. Expected result: the exception is swallowed and no callback is invoked.
   */
  @Test
  public void testOnMessageThrowsException() throws JMSException {
    MapMessage mapMessage = org.mockito.Mockito.mock(MapMessage.class);
    when(mapMessage.getString("event")).thenThrow(new JMSException("broken"));

    messageReceiver.onMessage(mapMessage);

    verify(dispatcher, never()).onSubmissionCreated(anyLong(), anyLong(), anyBoolean());
  }

  /**
   * Test case: tests the onMessage method. Test data: a judging task whose outcome cannot be
   * recorded. Expected result: the exception reaches the listener container, which rolls the task
   * back so that the broker delivers it again.
   */
  @Test
  public void testOnMessagePassesOnJudgingFailures() throws JMSException {
    MapMessage mapMessage = org.mockito.Mockito.mock(MapMessage.class);
    when(mapMessage.getString("event")).thenReturn("SubmissionCreated");
    when(mapMessage.getLong("submissionId")).thenReturn(1234L);
    org.mockito.Mockito.doThrow(new IllegalStateException("database down"))
        .when(dispatcher)
        .onSubmissionCreated(1234L, 0L, false);

    org.junit.jupiter.api.Assertions.assertThrows(
        IllegalStateException.class, () -> messageReceiver.onMessage(mapMessage));
  }

  /** The mocked ApplicationDispatcher object. */
  @Mock private ApplicationDispatcher dispatcher;

  /** The MessageReceiver object under test. */
  @InjectMocks private MessageReceiver messageReceiver;
}
