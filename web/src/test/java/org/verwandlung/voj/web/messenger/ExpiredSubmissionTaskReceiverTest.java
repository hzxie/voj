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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import org.verwandlung.voj.web.service.SubmissionService;

/**
 * The test class for ExpiredSubmissionTaskReceiver.
 *
 * @author Haozhe Xie
 */
public class ExpiredSubmissionTaskReceiverTest {
  /** Wires a mocked SubmissionService into a fresh receiver before every test. */
  @BeforeEach
  public void setUp() {
    submissionService = mock(SubmissionService.class);
    receiver = new ExpiredSubmissionTaskReceiver();
    ReflectionTestUtils.setField(receiver, "submissionService", submissionService);
  }

  /** Test case: tests onMessage(Message). Test data: an expired SubmissionCreated task. Expected: the submission is marked unjudgeable. */
  @Test
  public void testOnMessageMarksExpiredSubmission() throws JMSException {
    MapMessage message = mock(MapMessage.class);
    when(message.getString("event")).thenReturn("SubmissionCreated");
    when(message.getLong("submissionId")).thenReturn(1000L);

    receiver.onMessage(message);

    verify(submissionService).markSubmissionUnjudgeable(1000L);
  }

  /** Test case: tests onMessage(Message). Test data: a message whose event is not SubmissionCreated. Expected: the submission service is never touched. */
  @Test
  public void testOnMessageIgnoresUnrelatedEvent() throws JMSException {
    MapMessage message = mock(MapMessage.class);
    when(message.getString("event")).thenReturn("SomethingElse");

    receiver.onMessage(message);

    verify(submissionService, never()).markSubmissionUnjudgeable(org.mockito.ArgumentMatchers.anyLong());
  }

  /** Test case: tests onMessage(Message). Test data: a message that is not a MapMessage. Expected: the message is ignored without touching the submission service. */
  @Test
  public void testOnMessageIgnoresNonMapMessage() {
    Message message = mock(Message.class);

    receiver.onMessage(message);

    verify(submissionService, never()).markSubmissionUnjudgeable(org.mockito.ArgumentMatchers.anyLong());
  }

  /** Test case: tests onMessage(Message). Test data: a MapMessage that throws while being read. Expected: the JMSException is swallowed rather than propagated. */
  @Test
  public void testOnMessageSwallowsJmsException() throws JMSException {
    MapMessage message = mock(MapMessage.class);
    when(message.getString("event")).thenThrow(new JMSException("broken"));

    // Must not throw out of the listener; the container would otherwise redeliver endlessly.
    receiver.onMessage(message);

    verify(submissionService, never()).markSubmissionUnjudgeable(org.mockito.ArgumentMatchers.anyLong());
  }

  /** The mocked SubmissionService the receiver finalizes submissions through. */
  private SubmissionService submissionService;

  /** The ExpiredSubmissionTaskReceiver object under test. */
  private ExpiredSubmissionTaskReceiver receiver;
}
