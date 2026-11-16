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

import java.net.ServerSocket;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import jakarta.jms.ConnectionFactory;
import jakarta.jms.MapMessage;
import jakarta.jms.MessageListener;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.broker.BrokerService;
import org.apache.activemq.command.ActiveMQQueue;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.jms.connection.CachingConnectionFactory;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.listener.DefaultMessageListenerContainer;

/**
 * Test class for JmsConfig.
 *
 * @author Haozhe Xie
 */
public class JmsConfigTest {
  /** Stops the containers and brokers started by a test. */
  @AfterEach
  public void tearDown() throws Exception {
    for (DefaultMessageListenerContainer container : containers) {
      container.shutdown();
    }
    if (broker != null) {
      broker.stop();
      broker.waitUntilStopped();
    }
  }

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
   * Test case: tests the submissionTaskListenerContainer bean. Expected result: a single-consumer
   * listener container bound to the judging-task queue and the supplied message listener, with a
   * small prefetch, that does not start on its own.
   */
  @Test
  public void testSubmissionTaskListenerContainer() {
    MessageListener messageListener = org.mockito.Mockito.mock(MessageListener.class);

    DefaultMessageListenerContainer listenerContainer =
        jmsConfig.submissionTaskListenerContainer("vm://localhost", messageListener);

    Assertions.assertEquals("vojSubmissionTaskQueue", listenerContainer.getDestinationName());
    Assertions.assertSame(messageListener, listenerContainer.getMessageListener());
    Assertions.assertEquals(1, listenerContainer.getConcurrentConsumers());
    Assertions.assertEquals(
        JmsConfig.QUEUE_PREFETCH,
        ((ActiveMQConnectionFactory) listenerContainer.getConnectionFactory())
            .getPrefetchPolicy()
            .getQueuePrefetch());
    // The container must not auto-start; the startup runner starts it only after the identity check.
    Assertions.assertFalse(listenerContainer.isAutoStartup());
  }

  /**
   * Test case: the broker (over TCP, as in a deployment) is restarted while the judger is running.
   * Expected result: once it is back, the judger receives judging tasks again and sends messages
   * again, without being restarted itself.
   */
  @Test
  public void testJudgerResumesAfterBrokerRestart() throws Exception {
    int port;
    try (ServerSocket socket = new ServerSocket(0)) {
      port = socket.getLocalPort();
    }
    String brokerUrl = "tcp://localhost:" + port;
    broker = startBroker(brokerUrl);

    BlockingQueue<Long> received = new LinkedBlockingQueue<>();
    DefaultMessageListenerContainer listenerContainer =
        startListener(
            brokerUrl,
            message -> {
              try {
                received.add(((MapMessage) message).getLong("submissionId"));
              } catch (jakarta.jms.JMSException ex) {
                throw new IllegalStateException(ex);
              }
            });
    JmsTemplate taskSender = new JmsTemplate(new ActiveMQConnectionFactory(brokerUrl));
    taskSender.setDefaultDestination(new ActiveMQQueue("vojSubmissionTaskQueue"));
    JmsTemplate resultSender = jmsConfig.jmsTemplate(jmsConfig.connectionFactory(brokerUrl));

    taskSender.convertAndSend(Map.of("event", "SubmissionCreated", "submissionId", 1L));
    resultSender.convertAndSend(Map.of("event", "KeepAlive"));
    Assertions.assertEquals(1L, received.poll(10, TimeUnit.SECONDS));

    broker.stop();
    broker.waitUntilStopped();
    broker = startBroker(brokerUrl);

    taskSender.convertAndSend(Map.of("event", "SubmissionCreated", "submissionId", 2L));
    Assertions.assertEquals(
        2L, received.poll(JmsConfig.RECOVERY_INTERVAL_MS * 3, TimeUnit.MILLISECONDS));
    Assertions.assertTrue(listenerContainer.isRunning());
    // The cached sending connection died with the broker; the next message gets a new one.
    resultSender.convertAndSend(Map.of("event", "KeepAlive"));
    JmsTemplate resultReceiver = new JmsTemplate(new ActiveMQConnectionFactory(brokerUrl));
    resultReceiver.setReceiveTimeout(5000);
    Assertions.assertNotNull(resultReceiver.receive("vojJudgeResultQueue"));
  }

  /**
   * Starts a non-persistent broker listening on a URL.
   *
   * @param brokerUrl - the URL of the broker's transport connector
   * @return the started broker
   */
  private static BrokerService startBroker(String brokerUrl) throws Exception {
    BrokerService brokerService = new BrokerService();
    brokerService.setBrokerName("voj-jms-config-test");
    brokerService.setPersistent(false);
    brokerService.setUseJmx(false);
    brokerService.setUseShutdownHook(false);
    brokerService.addConnector(brokerUrl);
    brokerService.start();
    brokerService.waitUntilStarted();
    return brokerService;
  }

  /**
   * Creates the judger's listener container for a broker and starts it, as the startup runner does.
   *
   * @param brokerUrl - the URL of the broker
   * @param messageListener - the listener for judging tasks
   * @return the started listener container
   */
  private DefaultMessageListenerContainer startListener(
      String brokerUrl, MessageListener messageListener) {
    DefaultMessageListenerContainer listenerContainer =
        jmsConfig.submissionTaskListenerContainer(brokerUrl, messageListener);
    listenerContainer.afterPropertiesSet();
    listenerContainer.start();
    containers.add(listenerContainer);
    return listenerContainer;
  }

  /** The JmsConfig object under test. */
  private final JmsConfig jmsConfig = new JmsConfig();

  /** The listener containers started by the current test. */
  private final java.util.List<DefaultMessageListenerContainer> containers =
      new java.util.ArrayList<>();

  /** The broker started by the current test, if any. */
  private BrokerService broker;
}
