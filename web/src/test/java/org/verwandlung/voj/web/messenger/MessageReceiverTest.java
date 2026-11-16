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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.jms.JMSException;
import jakarta.jms.MapMessage;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * The test class for MessageReceiver.
 *
 * @author Haozhe Xie
 */
public class MessageReceiverTest {
  /** Wires a mocked event publisher into a fresh receiver before every test. */
  @BeforeEach
  public void setUp() {
    eventPublisher = mock(ApplicationEventPublisher.class);
    receiver = new MessageReceiver();
    ReflectionTestUtils.setField(receiver, "eventPublisher", eventPublisher);
  }

  /**
   * Test case: tests onMessage(Message). Test data: a TestPointFinished message with the claim
   * token of the judging that sent it. Expected: the submission event carries the claim token.
   */
  @Test
  public void testProgressCarriesTheClaimTokenOfTheJudging() throws JMSException {
    MapMessage message = testPointFinished();
    when(message.getLong("claimToken")).thenReturn(-4242L);

    receiver.onMessage(message);

    SubmissionEvent event = publishedEvent();
    Assertions.assertEquals(1000L, event.getSubmissionId());
    Assertions.assertEquals(-4242L, event.getClaimToken());
    Assertions.assertFalse(event.isCompleted());
  }

  /** Builds a TestPointFinished message of submission 1000. */
  private static MapMessage testPointFinished() throws JMSException {
    MapMessage message = mock(MapMessage.class);
    when(message.getString("event")).thenReturn("TestPointFinished");
    when(message.getLong("submissionId")).thenReturn(1000L);
    when(message.getInt("checkpointId")).thenReturn(1);
    when(message.getString("runtimeResult")).thenReturn("Accepted");
    return message;
  }

  /** Gets the submission event the receiver published. */
  private SubmissionEvent publishedEvent() {
    ArgumentCaptor<ApplicationEvent> eventCaptor = ArgumentCaptor.forClass(ApplicationEvent.class);
    verify(eventPublisher).publishEvent(eventCaptor.capture());
    return (SubmissionEvent) eventCaptor.getValue();
  }

  /** The mocked ApplicationEventPublisher. */
  private ApplicationEventPublisher eventPublisher;

  /** The MessageReceiver under test. */
  private MessageReceiver receiver;
}
