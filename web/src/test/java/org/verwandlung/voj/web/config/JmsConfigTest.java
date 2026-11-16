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
package org.verwandlung.voj.web.config;

import java.util.Collections;
import java.util.Map;

import jakarta.jms.Message;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.broker.BrokerService;
import org.apache.activemq.command.ActiveMQQueue;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jms.connection.CachingConnectionFactory;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.listener.SimpleMessageListenerContainer;

/**
 * The test class for JmsConfig, against a broker embedded in the test.
 *
 * @author Haozhe Xie
 */
public class JmsConfigTest {
  /** The name of the broker the tests start, distinct from the one of the test context. */
  private static final String BROKER_NAME = "jmsConfigTest";

  /** The ActiveMQ dead-letter queue. */
  private static final ActiveMQQueue DEAD_LETTER_QUEUE = new ActiveMQQueue("ActiveMQ.DLQ");

  /** Starts a non-persistent broker and creates the web application's connection factory. */
  @BeforeEach
  public void setUp() throws Exception {
    broker = new BrokerService();
    broker.setBrokerName(BROKER_NAME);
    broker.setPersistent(false);
    broker.setUseJmx(false);
    broker.setUseShutdownHook(false);
    broker.start();
    broker.waitUntilStarted();
    connectionFactory =
        (CachingConnectionFactory)
            jmsConfig.externalConnectionFactory("vm://" + BROKER_NAME + "?create=false");
    jmsTemplate =
        new JmsTemplate(new ActiveMQConnectionFactory("vm://" + BROKER_NAME + "?create=false"));
  }

  /** Stops the listener container, the connections and the broker. */
  @AfterEach
  public void tearDown() throws Exception {
    if (listenerContainer != null) {
      listenerContainer.shutdown();
    }
    connectionFactory.destroy();
    broker.stop();
    broker.waitUntilStopped();
  }

  /**
   * Test case: a judging task and another message are on the dead-letter queue. Expected: the
   * dead-letter consumer removes the judging task, and leaves the other message alone.
   */
  @Test
  public void testDeadLetteredTasksAreDropped() throws Exception {
    sendDeadLetter("KeepAlive");
    sendDeadLetter("SubmissionCreated");
    listenerContainer = jmsConfig.deadLetterListenerContainer(connectionFactory);
    listenerContainer.afterPropertiesSet();
    listenerContainer.start();

    long deadline = System.currentTimeMillis() + 10000;
    while (getNumberOfDeadLetters() > 1 && System.currentTimeMillis() < deadline) {
      Thread.sleep(50);
    }
    listenerContainer.shutdown();
    jmsTemplate.setReceiveTimeout(1000);
    Message deadLetter = jmsTemplate.receive(DEAD_LETTER_QUEUE);
    Assertions.assertNotNull(deadLetter);
    Assertions.assertEquals("KeepAlive", deadLetter.getStringProperty("event"));
    Assertions.assertNull(jmsTemplate.receive(DEAD_LETTER_QUEUE));
  }

  /**
   * Sends a message to the dead-letter queue, with its event as a property, as the web application
   * and the judger send theirs.
   */
  private void sendDeadLetter(String event) {
    jmsTemplate.convertAndSend(
        DEAD_LETTER_QUEUE,
        Map.of("event", event, "submissionId", 1000L),
        message -> {
          message.setStringProperty("event", event);
          return message;
        });
  }

  /** Counts the messages on the dead-letter queue. */
  private int getNumberOfDeadLetters() {
    return jmsTemplate.browse(
        DEAD_LETTER_QUEUE,
        (session, queueBrowser) -> Collections.list(queueBrowser.getEnumeration()).size());
  }

  /** The JmsConfig object under test. */
  private final JmsConfig jmsConfig = new JmsConfig();

  /** The broker the test runs against. */
  private BrokerService broker;

  /** The web application's connection factory, connected to the broker. */
  private CachingConnectionFactory connectionFactory;

  /** Sends, browses and receives the messages of the dead-letter queue. */
  private JmsTemplate jmsTemplate;

  /** The dead-letter listener container a test started, if any. */
  private SimpleMessageListenerContainer listenerContainer;
}
