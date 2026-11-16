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

import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import jakarta.jms.DeliveryMode;
import jakarta.jms.MapMessage;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.broker.BrokerService;
import org.apache.activemq.command.ActiveMQTopic;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.listener.DefaultMessageListenerContainer;

/**
 * The test class for JmsConfig, against a broker embedded in the test, over TCP (as in a
 * deployment).
 *
 * @author Haozhe Xie
 */
public class JmsConfigTest {
  /** Starts the broker. */
  @BeforeEach
  public void setUp() throws Exception {
    try (ServerSocket socket = new ServerSocket(0)) {
      brokerUrl = "tcp://localhost:" + socket.getLocalPort();
    }
    tcpBroker = startBroker(brokerUrl);
  }

  /** Stops the listener containers and the broker. */
  @AfterEach
  public void tearDown() throws Exception {
    for (DefaultMessageListenerContainer listenerContainer : listenerContainers) {
      listenerContainer.shutdown();
    }
    if (tcpBroker != null) {
      tcpBroker.stop();
      tcpBroker.waitUntilStopped();
    }
  }

  /**
   * Test case: two instances of the web application subscribe to the topic of the progress, and a
   * judger, then one of them, publishes a message. Expected: each instance receives every message,
   * which are not persistent.
   */
  @Test
  public void testEveryInstanceReceivesEveryMessage() throws Exception {
    BlockingQueue<Long> first = new LinkedBlockingQueue<>();
    BlockingQueue<Long> second = new LinkedBlockingQueue<>();
    BlockingQueue<Integer> deliveryModes = new LinkedBlockingQueue<>();
    startInstance(first, deliveryModes);
    startInstance(second, new LinkedBlockingQueue<>());
    JmsTemplate judger = judgerTemplate();
    JmsTemplate instance =
        jmsConfig.judgeProgressTemplate(jmsConfig.externalConnectionFactory(brokerUrl));

    sendUntilReceived(judger, 1L, first, second);
    instance.convertAndSend(Map.of("submissionId", 2L));
    awaitMessage(first, 2L);
    awaitMessage(second, 2L);
    Assertions.assertTrue(deliveryModes.contains(DeliveryMode.NON_PERSISTENT));
    Assertions.assertTrue(instance.isPubSubDomain());
    Assertions.assertEquals(JmsConfig.MESSAGE_TIME_TO_LIVE_MS, instance.getTimeToLive());
  }

  /**
   * Test case: an external broker is restarted while the web application is running. Expected:
   * once it is back, the web application subscribes again and receives the messages of the
   * judgers, without being restarted itself.
   */
  @Test
  public void testJudgeProgressIsReceivedAfterBrokerRestart() throws Exception {
    BlockingQueue<Long> received = new LinkedBlockingQueue<>();
    DefaultMessageListenerContainer listenerContainer =
        startInstance(received, new LinkedBlockingQueue<>());
    JmsTemplate judger = judgerTemplate();

    sendUntilReceived(judger, 1L, received);

    tcpBroker.stop();
    tcpBroker.waitUntilStopped();
    tcpBroker = startBroker(brokerUrl);

    // A message published before the instance subscribed again reaches nobody: published again
    // until one is received.
    sendUntilReceived(judger, 2L, received);
    Assertions.assertTrue(listenerContainer.isRunning());
  }

  /**
   * Starts an instance of the web application's listener container of the topic.
   *
   * @param received - receives the submission identifiers of the messages
   * @param deliveryModes - receives the delivery modes of the messages
   * @return the started listener container
   */
  private DefaultMessageListenerContainer startInstance(
      BlockingQueue<Long> received, BlockingQueue<Integer> deliveryModes) {
    DefaultMessageListenerContainer listenerContainer =
        jmsConfig.judgeProgressListenerContainer(
            jmsConfig.externalConnectionFactory(brokerUrl),
            message -> {
              try {
                deliveryModes.add(message.getJMSDeliveryMode());
                received.add(((MapMessage) message).getLong("submissionId"));
              } catch (jakarta.jms.JMSException ex) {
                throw new IllegalStateException(ex);
              }
            });
    listenerContainer.afterPropertiesSet();
    listenerContainer.start();
    listenerContainers.add(listenerContainer);
    return listenerContainer;
  }

  /**
   * Builds a JmsTemplate that publishes to the topic of the progress, as a judger does.
   *
   * @return the JmsTemplate
   */
  private JmsTemplate judgerTemplate() {
    JmsTemplate judger = new JmsTemplate(new ActiveMQConnectionFactory(brokerUrl));
    judger.setDefaultDestination(new ActiveMQTopic(JmsConfig.JUDGE_PROGRESS_TOPIC));
    judger.setExplicitQosEnabled(true);
    judger.setDeliveryPersistent(false);
    return judger;
  }

  /**
   * Publishes a message, every half second, until each of some subscribers received it (a message
   * published before a subscriber subscribed reaches it not).
   *
   * @param sender - the JmsTemplate that publishes
   * @param submissionId - the submission identifier of the message
   * @param subscribers - the messages received by the subscribers
   */
  @SafeVarargs
  private static void sendUntilReceived(
      JmsTemplate sender, long submissionId, BlockingQueue<Long>... subscribers)
      throws InterruptedException {
    long deadline =
        System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(JmsConfig.RECOVERY_INTERVAL_MS * 4);
    for (BlockingQueue<Long> subscriber : subscribers) {
      Long received = null;
      while (received == null || received != submissionId) {
        Assertions.assertTrue(System.nanoTime() < deadline, "Not received: " + submissionId);
        sender.convertAndSend(Map.of("submissionId", submissionId));
        received = subscriber.poll(500, TimeUnit.MILLISECONDS);
      }
    }
  }

  /**
   * Waits until a subscriber received a message (a message published before it may still come).
   *
   * @param subscriber - the messages received by the subscriber
   * @param submissionId - the submission identifier of the message
   */
  private static void awaitMessage(BlockingQueue<Long> subscriber, long submissionId)
      throws InterruptedException {
    long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
    for (Long received = subscriber.poll(10, TimeUnit.SECONDS);
        received == null || received != submissionId;
        received = subscriber.poll(500, TimeUnit.MILLISECONDS)) {
      Assertions.assertTrue(System.nanoTime() < deadline, "Not received: " + submissionId);
    }
  }

  /**
   * Starts a non-persistent broker listening on a URL.
   *
   * @param brokerUrl - the URL of the broker's transport connector
   * @return the started broker
   */
  private static BrokerService startBroker(String brokerUrl) throws Exception {
    BrokerService brokerService = new BrokerService();
    brokerService.setBrokerName("jmsConfigTestOverTcp");
    brokerService.setPersistent(false);
    brokerService.setUseJmx(false);
    brokerService.setUseShutdownHook(false);
    brokerService.addConnector(brokerUrl);
    brokerService.start();
    brokerService.waitUntilStarted();
    return brokerService;
  }

  /** The JmsConfig object under test. */
  private final JmsConfig jmsConfig = new JmsConfig();

  /** The URL of the broker. */
  private String brokerUrl;

  /** The broker over TCP. */
  private BrokerService tcpBroker;

  /** The listener containers a test started. */
  private final List<DefaultMessageListenerContainer> listenerContainers = new ArrayList<>();
}
