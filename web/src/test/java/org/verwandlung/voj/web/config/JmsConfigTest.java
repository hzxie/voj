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

import static org.mockito.Mockito.after;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.RedeliveryPolicy;
import org.apache.activemq.broker.BrokerService;
import org.apache.activemq.command.ActiveMQQueue;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jms.connection.CachingConnectionFactory;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.listener.SimpleMessageListenerContainer;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.CannotCreateTransactionException;

import org.verwandlung.voj.web.messenger.DeadLetterSubmissionTaskReceiver;
import org.verwandlung.voj.web.service.SubmissionService;
import org.verwandlung.voj.web.service.SubmissionService.JudgingTaskFailure;

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
   * Test case: tests the redelivery policies of the connection factory. Expected: the consumer of
   * the dead-letter queue redelivers without a limit, with an exponential back-off; the other
   * consumers keep ActiveMQ's default policy.
   */
  @Test
  public void testDeadLetterQueueIsRedeliveredWithoutLimit() {
    RedeliveryPolicy deadLetterPolicy = getRedeliveryPolicy(DEAD_LETTER_QUEUE);
    Assertions.assertEquals(
        RedeliveryPolicy.NO_MAXIMUM_REDELIVERIES, deadLetterPolicy.getMaximumRedeliveries());
    Assertions.assertTrue(deadLetterPolicy.isUseExponentialBackOff());
    Assertions.assertEquals(
        JmsConfig.DEAD_LETTER_INITIAL_REDELIVERY_DELAY_MS,
        deadLetterPolicy.getInitialRedeliveryDelay());
    Assertions.assertEquals(
        JmsConfig.DEAD_LETTER_MAXIMUM_REDELIVERY_DELAY_MS,
        deadLetterPolicy.getMaximumRedeliveryDelay());

    RedeliveryPolicy judgeResultPolicy =
        getRedeliveryPolicy(new ActiveMQQueue("vojJudgeResultQueue"));
    Assertions.assertEquals(
        RedeliveryPolicy.DEFAULT_MAXIMUM_REDELIVERIES, judgeResultPolicy.getMaximumRedeliveries());
  }

  /**
   * Test case: a judging task is dead-lettered while the database is down: finalizing its
   * submission fails 8 times (more than ActiveMQ's default limit of 6 redeliveries), then succeeds.
   * Expected: the task is delivered again after every failure, its submission is finalized on the
   * 9th delivery, and it is then consumed: it is neither delivered again nor left on the queue.
   */
  @Test
  public void testDeadLetteredTaskIsRetriedUntilTheDatabaseIsBack() throws Exception {
    // Keep the test fast; the back-off itself is ActiveMQ's.
    RedeliveryPolicy deadLetterPolicy = getRedeliveryPolicy(DEAD_LETTER_QUEUE);
    deadLetterPolicy.setInitialRedeliveryDelay(10);
    deadLetterPolicy.setRedeliveryDelay(10);
    deadLetterPolicy.setMaximumRedeliveryDelay(40);

    SubmissionService submissionService = mock(SubmissionService.class);
    CannotCreateTransactionException databaseDown =
        new CannotCreateTransactionException("database down");
    when(submissionService.markSubmissionUnjudgeable(
            1000L, 3L, JudgingTaskFailure.NO_JUDGER_AVAILABLE))
        .thenThrow(
            databaseDown,
            databaseDown,
            databaseDown,
            databaseDown,
            databaseDown,
            databaseDown,
            databaseDown,
            databaseDown)
        .thenReturn(true);
    DeadLetterSubmissionTaskReceiver receiver = new DeadLetterSubmissionTaskReceiver();
    ReflectionTestUtils.setField(receiver, "submissionService", submissionService);
    listenerContainer = jmsConfig.deadLetterListenerContainer(connectionFactory, receiver);
    Assertions.assertTrue(listenerContainer.isSessionTransacted());
    listenerContainer.afterPropertiesSet();
    listenerContainer.start();

    JmsTemplate jmsTemplate =
        new JmsTemplate(new ActiveMQConnectionFactory("vm://" + BROKER_NAME + "?create=false"));
    jmsTemplate.convertAndSend(
        DEAD_LETTER_QUEUE,
        Map.of("event", "SubmissionCreated", "submissionId", 1000L, "generation", 3L),
        message -> {
          message.setStringProperty("event", "SubmissionCreated");
          message.setStringProperty(
              "dlqDeliveryFailureCause", "java.lang.Throwable: Message Expired. Expiration:1");
          return message;
        });

    verify(submissionService, timeout(10000).times(9))
        .markSubmissionUnjudgeable(1000L, 3L, JudgingTaskFailure.NO_JUDGER_AVAILABLE);
    verify(submissionService, after(500).times(9))
        .markSubmissionUnjudgeable(1000L, 3L, JudgingTaskFailure.NO_JUDGER_AVAILABLE);
    listenerContainer.shutdown();
    jmsTemplate.setReceiveTimeout(500);
    Assertions.assertNull(jmsTemplate.receive(DEAD_LETTER_QUEUE));
  }

  /** Gets the redelivery policy of the consumers of a destination. */
  private RedeliveryPolicy getRedeliveryPolicy(ActiveMQQueue destination) {
    return ((ActiveMQConnectionFactory) connectionFactory.getTargetConnectionFactory())
        .getRedeliveryPolicyMap()
        .getEntryFor(destination);
  }

  /** The JmsConfig object under test. */
  private final JmsConfig jmsConfig = new JmsConfig();

  /** The broker the test runs against. */
  private BrokerService broker;

  /** The web application's connection factory, connected to the broker. */
  private CachingConnectionFactory connectionFactory;

  /** The dead-letter listener container a test started, if any. */
  private SimpleMessageListenerContainer listenerContainer;
}
