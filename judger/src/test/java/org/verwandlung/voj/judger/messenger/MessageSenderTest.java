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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jms.UncategorizedJmsException;
import org.springframework.jms.core.JmsTemplate;

/**
 * Test class for MessageSender.
 *
 * @author Haozhe Xie
 */
@ExtendWith(MockitoExtension.class)
public class MessageSenderTest {
  /**
   * Test case: tests the sendMessage method. Test data: two messages, the first of which cannot be
   * sent (e.g. the broker is down). Expected result: both are handed to the JmsTemplate, in order,
   * in the background.
   */
  @Test
  public void testSendMessage() {
    Map<String, Object> first = Map.of("event", "CompileFinished");
    Map<String, Object> second = Map.of("event", "AllTestPointsFinished");
    doThrow(new UncategorizedJmsException("broker down")).when(jmsTemplate).convertAndSend(first);

    messageSender.sendMessage(first);
    messageSender.sendMessage(second);

    verify(jmsTemplate, timeout(10000)).convertAndSend(second);
    InOrder order = inOrder(jmsTemplate);
    order.verify(jmsTemplate).convertAndSend(first);
    order.verify(jmsTemplate).convertAndSend(second);
  }

  /**
   * Test case: tests the sendMessage method. Test data: a broker that holds the sender up (e.g. by
   * flow control) while more messages come than may wait. Expected result: the caller is never held
   * up; the messages that do not fit are dropped, and the sender sends the others once it goes on.
   */
  @Test
  public void testSendMessageNeverWaitsForTheBroker() throws Exception {
    CountDownLatch broker = new CountDownLatch(1);
    doAnswer(
            invocation -> {
              broker.await(10, TimeUnit.SECONDS);
              return null;
            })
        .when(jmsTemplate)
        .convertAndSend(any(Object.class));

    long startTime = System.nanoTime();
    for (int i = 0; i < MessageSender.MAX_PENDING_MESSAGES + 10; ++i) {
      messageSender.sendMessage(Map.of("event", "TestPointFinished", "checkpointId", i));
    }
    Assertions.assertTrue(System.nanoTime() - startTime < TimeUnit.SECONDS.toNanos(5));
    broker.countDown();

    // The one being sent when the others came, and those that fitted the queue.
    verify(jmsTemplate, timeout(10000).times(MessageSender.MAX_PENDING_MESSAGES + 1))
        .convertAndSend(any(Object.class));
    Thread.sleep(100);
    verify(jmsTemplate, times(MessageSender.MAX_PENDING_MESSAGES + 1))
        .convertAndSend(any(Object.class));
  }

  /** The mocked JmsTemplate object. */
  @Mock private JmsTemplate jmsTemplate;

  /** The MessageSender object under test. */
  @InjectMocks private MessageSender messageSender;
}
