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

import jakarta.jms.ConnectionFactory;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.broker.BrokerService;
import org.apache.activemq.command.ActiveMQQueue;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.jms.connection.CachingConnectionFactory;
import org.springframework.jms.core.JmsTemplate;

/**
 * Test class for JmsConfig.
 *
 * @author Haozhe Xie
 */
public class JmsConfigTest {
  /** Stops the broker started by a test. */
  @AfterEach
  public void tearDown() throws Exception {
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
   * Test case: the broker (over TCP, as in a deployment) is restarted while the judger is running.
   * Expected result: once it is back, the judger sends messages again, without being restarted
   * itself.
   */
  @Test
  public void testJudgerSendsAgainAfterBrokerRestart() throws Exception {
    int port;
    try (ServerSocket socket = new ServerSocket(0)) {
      port = socket.getLocalPort();
    }
    String brokerUrl = "tcp://localhost:" + port;
    broker = startBroker(brokerUrl);
    JmsTemplate resultSender = jmsConfig.jmsTemplate(jmsConfig.connectionFactory(brokerUrl));
    JmsTemplate resultReceiver = new JmsTemplate(new ActiveMQConnectionFactory(brokerUrl));
    resultReceiver.setReceiveTimeout(5000);

    resultSender.convertAndSend(Map.of("event", "KeepAlive"));
    Assertions.assertNotNull(resultReceiver.receive("vojJudgeResultQueue"));

    broker.stop();
    broker.waitUntilStopped();
    broker = startBroker(brokerUrl);

    // The cached sending connection died with the broker; the next message gets a new one.
    resultSender.convertAndSend(Map.of("event", "KeepAlive"));
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

  /** The JmsConfig object under test. */
  private final JmsConfig jmsConfig = new JmsConfig();

  /** The broker started by the current test, if any. */
  private BrokerService broker;
}
