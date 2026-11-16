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

import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.core.config.Property;

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

  /**
   * Test case: tests the sendMessage method. Test data: three messages the broker does not take
   * (an outage), one it takes, then one it does not take again. Expected result: each outage is
   * logged once, with a warning, and its end once; the other failures are not logged above debug.
   */
  @Test
  public void testABrokerOutageIsLoggedOnce() throws Exception {
    Map<String, Object> sent = Map.of("event", "AllTestPointsFinished");
    doAnswer(
            invocation -> {
              if (!sent.equals(invocation.getArgument(0))) {
                throw new UncategorizedJmsException("broker down");
              }
              return null;
            })
        .when(jmsTemplate)
        .convertAndSend(any(Object.class));
    List<Level> levels = new CopyOnWriteArrayList<>();
    LoggerContext context = (LoggerContext) LogManager.getContext(false);
    Configuration configuration = context.getConfiguration();
    AbstractAppender appender =
        new AbstractAppender("MessageSenderTest", null, null, true, Property.EMPTY_ARRAY) {
          @Override
          public void append(LogEvent event) {
            levels.add(event.getLevel());
          }
        };
    appender.start();
    LoggerConfig loggerConfig = new LoggerConfig(MessageSender.class.getName(), Level.INFO, false);
    loggerConfig.addAppender(appender, Level.INFO, null);
    configuration.addLogger(MessageSender.class.getName(), loggerConfig);
    context.updateLoggers();
    try {
      for (int i = 0; i < 3; ++i) {
        messageSender.sendMessage(Map.of("event", "TestPointFinished"));
      }
      messageSender.sendMessage(sent);
      messageSender.sendMessage(Map.of("event", "TestPointFinished"));
      verify(jmsTemplate, timeout(10000).times(4))
          .convertAndSend(Map.of("event", "TestPointFinished"));
      Thread.sleep(100);

      Assertions.assertEquals(List.of(Level.WARN, Level.INFO, Level.WARN), levels);
    } finally {
      configuration.removeLogger(MessageSender.class.getName());
      context.updateLoggers();
      appender.stop();
    }
  }

  /** The mocked JmsTemplate object. */
  @Mock private JmsTemplate jmsTemplate;

  /** The MessageSender object under test. */
  @InjectMocks private MessageSender messageSender;
}
