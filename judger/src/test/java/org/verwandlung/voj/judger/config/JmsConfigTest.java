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
import org.mockito.Mockito;
import org.springframework.jms.connection.CachingConnectionFactory;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.listener.DefaultMessageListenerContainer;
import org.springframework.test.util.ReflectionTestUtils;

import org.verwandlung.voj.judger.application.ApplicationDispatcher;
import org.verwandlung.voj.judger.exception.RetryableJudgingException;
import org.verwandlung.voj.judger.messenger.MessageReceiver;

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
   * prefetch of 0, that does not start on its own.
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
    // The consumer is closed after every receive, which hands a rolled back task to the broker.
    Assertions.assertEquals(
        DefaultMessageListenerContainer.CACHE_SESSION, listenerContainer.getCacheLevel());
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
   * Test case: the judging of a task keeps failing (the listener throws). Expected result: the task
   * is delivered again, {@link JmsConfig#MAXIMUM_REDELIVERIES} times, then dead-lettered.
   */
  @Test
  public void testFailedTaskIsRedeliveredThenDeadLettered() throws Exception {
    String brokerUrl = startBrokerOnFreePort();
    java.util.concurrent.atomic.AtomicInteger deliveries =
        new java.util.concurrent.atomic.AtomicInteger();
    DefaultMessageListenerContainer listenerContainer =
        jmsConfig.submissionTaskListenerContainer(
            brokerUrl,
            message -> {
              deliveries.incrementAndGet();
              throw new IllegalStateException("database down");
            });
    Assertions.assertTrue(listenerContainer.isSessionTransacted());
    org.apache.activemq.RedeliveryPolicy redeliveryPolicy =
        ((ActiveMQConnectionFactory) listenerContainer.getConnectionFactory())
            .getRedeliveryPolicy();
    Assertions.assertEquals(
        JmsConfig.MAXIMUM_REDELIVERIES, redeliveryPolicy.getMaximumRedeliveries());
    // Keep the test fast: the judger, the only one, takes the task again right away.
    ((JmsConfig.SubmissionTaskListenerContainer) listenerContainer).setInitialPauseMs(0);
    start(listenerContainer);

    sendTask(brokerUrl, 7L);
    JmsTemplate deadLetterReceiver = new JmsTemplate(new ActiveMQConnectionFactory(brokerUrl));
    deadLetterReceiver.setReceiveTimeout(10000);
    MapMessage deadLetter = (MapMessage) deadLetterReceiver.receive("ActiveMQ.DLQ");

    Assertions.assertNotNull(deadLetter);
    Assertions.assertEquals(7L, deadLetter.getLong("submissionId"));
    Assertions.assertEquals(1 + JmsConfig.MAXIMUM_REDELIVERIES, deliveries.get());
  }

  /**
   * Test case: the judger's MessageReceiver gets a task whose judging could succeed later, and the
   * judging hands it back every time it may. Expected result: of its 1 + {@link
   * JmsConfig#MAXIMUM_REDELIVERIES} deliveries, only the last is reported as such; the judging,
   * which then records a System Error rather than hand the task back, ends it: nothing is
   * dead-lettered.
   */
  @Test
  public void testLastDeliveryIsToldToTheJudging() throws Exception {
    String brokerUrl = startBrokerOnFreePort();
    java.util.List<Boolean> lastDeliveries = new java.util.concurrent.CopyOnWriteArrayList<>();
    ApplicationDispatcher applicationDispatcher = Mockito.mock(ApplicationDispatcher.class);
    Mockito.doAnswer(
            invocation -> {
              boolean isLastDelivery = invocation.getArgument(2);
              lastDeliveries.add(isLastDelivery);
              if (!isLastDelivery) {
                throw new RetryableJudgingException("The test data could not be downloaded.", null);
              }
              return null;
            })
        .when(applicationDispatcher)
        .onSubmissionCreated(Mockito.eq(7L), Mockito.anyLong(), Mockito.anyBoolean());
    MessageReceiver messageReceiver = new MessageReceiver();
    ReflectionTestUtils.setField(messageReceiver, "dispatcher", applicationDispatcher);
    DefaultMessageListenerContainer listenerContainer =
        jmsConfig.submissionTaskListenerContainer(brokerUrl, messageReceiver);
    ((JmsConfig.SubmissionTaskListenerContainer) listenerContainer).setInitialPauseMs(0);
    start(listenerContainer);

    sendTask(brokerUrl, 7L);
    long deadline = System.currentTimeMillis() + 10000;
    while (lastDeliveries.size() <= JmsConfig.MAXIMUM_REDELIVERIES
        && System.currentTimeMillis() < deadline) {
      Thread.sleep(50);
    }

    java.util.List<Boolean> expected = new java.util.ArrayList<>();
    for (int i = 0; i < JmsConfig.MAXIMUM_REDELIVERIES; ++i) {
      expected.add(false);
    }
    expected.add(true);
    Assertions.assertEquals(expected, lastDeliveries);
    JmsTemplate deadLetterReceiver = new JmsTemplate(new ActiveMQConnectionFactory(brokerUrl));
    deadLetterReceiver.setReceiveTimeout(1000);
    Assertions.assertNull(deadLetterReceiver.receive("ActiveMQ.DLQ"));
    Assertions.assertEquals(1 + JmsConfig.MAXIMUM_REDELIVERIES, lastDeliveries.size());
  }

  /**
   * Test case: a judger with a fault of its own (e.g. wrong database credentials) fails the judging
   * of a task, and another, healthy judger is idle. Expected result: the task is delivered again to
   * the healthy judger, right away, rather than again and again to the failing one; the failing
   * judger pauses before it takes a task again.
   */
  @Test
  public void testFailedTaskIsRedeliveredToAnotherJudger() throws Exception {
    String brokerUrl = startBrokerOnFreePort();
    java.util.concurrent.atomic.AtomicInteger failedDeliveries =
        new java.util.concurrent.atomic.AtomicInteger();
    java.util.concurrent.CountDownLatch failed = new java.util.concurrent.CountDownLatch(1);
    start(
        jmsConfig.submissionTaskListenerContainer(
            brokerUrl,
            message -> {
              failedDeliveries.incrementAndGet();
              failed.countDown();
              throw new IllegalStateException("database down");
            }));
    sendTask(brokerUrl, 7L);
    Assertions.assertTrue(failed.await(10, TimeUnit.SECONDS));

    BlockingQueue<Long> judgedByHealthyJudger = new LinkedBlockingQueue<>();
    start(
        jmsConfig.submissionTaskListenerContainer(
            brokerUrl,
            message -> {
              try {
                judgedByHealthyJudger.add(((MapMessage) message).getLong("submissionId"));
              } catch (jakarta.jms.JMSException ex) {
                throw new IllegalStateException(ex);
              }
            }));

    Assertions.assertEquals(7L, judgedByHealthyJudger.poll(5, TimeUnit.SECONDS));
    Assertions.assertEquals(1, failedDeliveries.get());
  }

  /**
   * Test case: two judgers share the queue and one of them is busy judging. Expected result: the
   * busy judger does not hold back the other tasks; the idle judger judges them meanwhile.
   */
  @Test
  public void testBusyJudgerDoesNotHoardTasks() throws Exception {
    String brokerUrl = startBrokerOnFreePort();
    java.util.concurrent.CountDownLatch busy = new java.util.concurrent.CountDownLatch(1);
    java.util.concurrent.CountDownLatch release = new java.util.concurrent.CountDownLatch(1);
    BlockingQueue<Long> judgedByIdleJudger = new LinkedBlockingQueue<>();
    start(
        jmsConfig.submissionTaskListenerContainer(
            brokerUrl,
            message -> {
              busy.countDown();
              try {
                release.await(30, TimeUnit.SECONDS);
              } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
              }
            }));
    sendTask(brokerUrl, 1L);
    Assertions.assertTrue(busy.await(10, TimeUnit.SECONDS));

    for (long submissionId = 2; submissionId <= 4; ++submissionId) {
      sendTask(brokerUrl, submissionId);
    }
    start(
        jmsConfig.submissionTaskListenerContainer(
            brokerUrl,
            message -> {
              try {
                judgedByIdleJudger.add(((MapMessage) message).getLong("submissionId"));
              } catch (jakarta.jms.JMSException ex) {
                throw new IllegalStateException(ex);
              }
            }));
    try {
      for (int i = 0; i < 3; ++i) {
        Assertions.assertNotNull(
            judgedByIdleJudger.poll(10, TimeUnit.SECONDS),
            "The busy judger holds back " + (3 - i) + " task(s)");
      }
    } finally {
      release.countDown();
    }
  }

  /**
   * Starts a broker on a free TCP port, as the broker of the current test.
   *
   * @return the URL of the broker
   */
  private String startBrokerOnFreePort() throws Exception {
    int port;
    try (ServerSocket socket = new ServerSocket(0)) {
      port = socket.getLocalPort();
    }
    String brokerUrl = "tcp://localhost:" + port;
    broker = startBroker(brokerUrl);
    return brokerUrl;
  }

  /**
   * Sends a judging task, as the web application does.
   *
   * @param brokerUrl - the URL of the broker
   * @param submissionId - the submission to judge
   */
  private static void sendTask(String brokerUrl, long submissionId) {
    JmsTemplate taskSender = new JmsTemplate(new ActiveMQConnectionFactory(brokerUrl));
    taskSender.setDefaultDestination(new ActiveMQQueue("vojSubmissionTaskQueue"));
    taskSender.convertAndSend(
        Map.of("event", "SubmissionCreated", "submissionId", submissionId),
        message -> {
          message.setStringProperty("event", "SubmissionCreated");
          return message;
        });
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
    return start(jmsConfig.submissionTaskListenerContainer(brokerUrl, messageListener));
  }

  /**
   * Starts a listener container, as the startup runner does.
   *
   * @param listenerContainer - the listener container
   * @return the started listener container
   */
  private DefaultMessageListenerContainer start(DefaultMessageListenerContainer listenerContainer) {
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
