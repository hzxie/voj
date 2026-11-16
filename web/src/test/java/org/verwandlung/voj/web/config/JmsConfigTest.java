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
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import jakarta.jms.MapMessage;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.broker.BrokerService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.listener.DefaultMessageListenerContainer;

/**
 * The test class for JmsConfig, against a broker embedded in the test.
 *
 * @author Haozhe Xie
 */
public class JmsConfigTest {
  /** Stops the listener container and the broker. */
  @AfterEach
  public void tearDown() throws Exception {
    if (listenerContainer != null) {
      listenerContainer.shutdown();
    }
    if (tcpBroker != null) {
      tcpBroker.stop();
      tcpBroker.waitUntilStopped();
    }
  }

  /**
   * Test case: an external broker (over TCP, as in a deployment) is restarted while the web
   * application is running. Expected: once it is back, the web application receives the messages
   * of the judgers again, without being restarted itself.
   */
  @Test
  public void testJudgeResultsAreReceivedAfterBrokerRestart() throws Exception {
    String brokerUrl;
    try (ServerSocket socket = new ServerSocket(0)) {
      brokerUrl = "tcp://localhost:" + socket.getLocalPort();
    }
    tcpBroker = startBroker(brokerUrl);
    BlockingQueue<Long> received = new LinkedBlockingQueue<>();
    listenerContainer =
        jmsConfig.judgeResultListenerContainer(
            jmsConfig.externalConnectionFactory(brokerUrl),
            message -> {
              try {
                received.add(((MapMessage) message).getLong("submissionId"));
              } catch (jakarta.jms.JMSException ex) {
                throw new IllegalStateException(ex);
              }
            });
    listenerContainer.afterPropertiesSet();
    listenerContainer.start();
    JmsTemplate judger = new JmsTemplate(new ActiveMQConnectionFactory(brokerUrl));

    judger.convertAndSend("vojJudgeResultQueue", Map.of("submissionId", 1L));
    Assertions.assertEquals(1L, received.poll(10, TimeUnit.SECONDS));

    tcpBroker.stop();
    tcpBroker.waitUntilStopped();
    tcpBroker = startBroker(brokerUrl);

    judger.convertAndSend("vojJudgeResultQueue", Map.of("submissionId", 2L));
    Assertions.assertEquals(
        2L, received.poll(JmsConfig.RECOVERY_INTERVAL_MS * 3, TimeUnit.MILLISECONDS));
    Assertions.assertTrue(listenerContainer.isRunning());
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

  /** The broker over TCP a test started, if any. */
  private BrokerService tcpBroker;

  /** The listener container a test started, if any. */
  private DefaultMessageListenerContainer listenerContainer;
}
