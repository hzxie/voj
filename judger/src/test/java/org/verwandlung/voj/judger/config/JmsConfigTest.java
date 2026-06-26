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
package org.verwandlung.voj.judger.config;

import jakarta.jms.ConnectionFactory;
import jakarta.jms.MessageListener;

import org.apache.activemq.command.ActiveMQQueue;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.jms.connection.CachingConnectionFactory;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.listener.SimpleMessageListenerContainer;

/**
 * Test class for JmsConfig.
 *
 * @author Haozhe Xie
 */
public class JmsConfigTest {
  /**
   * Test case: tests the connectionFactory bean. Expected result: a caching connection factory is
   * returned.
   */
  @Test
  public void testConnectionFactory() {
    ConnectionFactory connectionFactory = jmsConfig.connectionFactory("vm://localhost");
    Assertions.assertInstanceOf(CachingConnectionFactory.class, connectionFactory);
  }

  /**
   * Test case: tests the jmsTemplate bean. Expected result: a JmsTemplate whose default destination
   * is the judging-result queue is returned.
   */
  @Test
  public void testJmsTemplate() throws jakarta.jms.JMSException {
    ConnectionFactory connectionFactory = jmsConfig.connectionFactory("vm://localhost");
    JmsTemplate jmsTemplate = jmsConfig.jmsTemplate(connectionFactory);

    Assertions.assertSame(connectionFactory, jmsTemplate.getConnectionFactory());
    Assertions.assertInstanceOf(ActiveMQQueue.class, jmsTemplate.getDefaultDestination());
    Assertions.assertEquals(
        "vojJudgeResultQueue",
        ((ActiveMQQueue) jmsTemplate.getDefaultDestination()).getQueueName());
  }

  /**
   * Test case: tests the submissionTaskListenerContainer bean. Expected result: a listener
   * container bound to the judging-task queue and the supplied message listener is returned.
   */
  @Test
  public void testSubmissionTaskListenerContainer() {
    ConnectionFactory connectionFactory = jmsConfig.connectionFactory("vm://localhost");
    MessageListener messageListener = org.mockito.Mockito.mock(MessageListener.class);

    SimpleMessageListenerContainer listenerContainer =
        jmsConfig.submissionTaskListenerContainer(connectionFactory, messageListener);

    Assertions.assertEquals("vojSubmissionTaskQueue", listenerContainer.getDestinationName());
    Assertions.assertSame(messageListener, listenerContainer.getMessageListener());
    Assertions.assertSame(connectionFactory, listenerContainer.getConnectionFactory());
    // The container must not auto-start; the startup runner starts it only after the identity check.
    Assertions.assertFalse(listenerContainer.isAutoStartup());
  }

  /** The JmsConfig object under test. */
  private final JmsConfig jmsConfig = new JmsConfig();
}
