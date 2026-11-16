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

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.RedeliveryPolicy;
import org.apache.activemq.command.ActiveMQQueue;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.connection.CachingConnectionFactory;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.listener.DefaultMessageListenerContainer;

/**
 * The message queue configuration. Replaces the ActiveMQ / JMS definitions from the original
 * application-context.xml.
 *
 * <p>The judger listens on {@code vojSubmissionTaskQueue} to receive tasks to be judged, and sends
 * judging results to {@code vojJudgeResultQueue}.
 *
 * <p>Both sides survive a restart of the broker (in the Docker setup it lives in the web
 * container, so it restarts with it): the listener container keeps trying to reconnect every
 * {@value #RECOVERY_INTERVAL_MS} ms until the broker is back, and the sending side drops its
 * cached connection once it fails and opens a new one for the next message.
 *
 * @author Haozhe Xie
 */
@Configuration
public class JmsConfig {
  /** The name of the judging task queue (Web -> Judger). */
  private static final String SUBMISSION_TASK_QUEUE = "vojSubmissionTaskQueue";

  /** The name of the judging result queue (Judger -> Web). */
  private static final String JUDGE_RESULT_QUEUE = "vojJudgeResultQueue";

  /** How long (ms) the listener container waits between two attempts to reconnect. */
  static final long RECOVERY_INTERVAL_MS = 5000;

  /**
   * How many judging tasks the broker may push to this judger ahead of time. Each task takes
   * seconds to minutes and judgers share the queue, so a judger must not hoard tasks (ActiveMQ's
   * default prefetch is 1000) that idle judgers could be judging meanwhile. Not even one: with a
   * prefetch of 1, the broker extends the window of a consumer whose (transacted) task is still
   * being judged and pushes it the next task, which then waits behind the current one. With 0 the
   * consumer pulls a task only when it asks for one, i.e. once it is idle.
   */
  static final int QUEUE_PREFETCH = 0;

  /**
   * How many times a judging task whose judging failed (it could not even be recorded as a System
   * Error, e.g. while the database is down) is delivered again before the broker moves it to the
   * dead-letter queue, where the web application marks the submission as failed.
   */
  static final int MAXIMUM_REDELIVERIES = 5;

  /** The delay (ms) before the first redelivery; it doubles with each further one. */
  static final long INITIAL_REDELIVERY_DELAY_MS = 10000;

  /**
   * The JMS connection factory for sending, with a cached connection. The cache drops the
   * connection when it fails (reconnectOnException), so the next message after a broker restart is
   * sent over a new one.
   */
  @Bean
  public ConnectionFactory connectionFactory(@Value("${jms.broker.url}") String brokerUrl) {
    ActiveMQConnectionFactory amqConnectionFactory = new ActiveMQConnectionFactory(brokerUrl);
    return new CachingConnectionFactory(amqConnectionFactory);
  }

  /** The JmsTemplate used to send messages to the judging result queue. */
  @Bean
  public JmsTemplate jmsTemplate(ConnectionFactory connectionFactory) {
    JmsTemplate jmsTemplate = new JmsTemplate(connectionFactory);
    jmsTemplate.setDefaultDestination(new ActiveMQQueue(JUDGE_RESULT_QUEUE));
    return jmsTemplate;
  }

  /**
   * The message listener container that listens on the judging task queue.
   *
   * <p>A DefaultMessageListenerContainer, which re-establishes its connection in a loop until the
   * broker is reachable again; a SimpleMessageListenerContainer tries to recover only once, right
   * when the connection fails (while the broker is still down), and then stops consuming for good.
   * It has its own (uncached) connection factory, as the container caches its connection and
   * consumer itself, without prefetch. A single consumer: judging is serialized anyway (see
   * {@code Dispatcher#createNewTask}).
   *
   * <p>Each task is received in a local JMS transaction that is committed only once the listener
   * returns. If it throws, which it does when the outcome of the judging could not be recorded, the
   * transaction is rolled back and the broker delivers the task again (to this or another judger),
   * up to {@value #MAXIMUM_REDELIVERIES} times with a growing delay, then dead-letters it. A judger
   * that dies while judging never commits either, so its task is judged elsewhere.
   *
   * @param brokerUrl - the URL of the message broker
   * @param messageReceiver - the listener for judging tasks
   * @return the listener container, which the startup runner starts once the judger is ready
   */
  @Bean
  public DefaultMessageListenerContainer submissionTaskListenerContainer(
      @Value("${jms.broker.url}") String brokerUrl, MessageListener messageReceiver) {
    ActiveMQConnectionFactory amqConnectionFactory = new ActiveMQConnectionFactory(brokerUrl);
    amqConnectionFactory.getPrefetchPolicy().setQueuePrefetch(QUEUE_PREFETCH);
    RedeliveryPolicy redeliveryPolicy = amqConnectionFactory.getRedeliveryPolicy();
    redeliveryPolicy.setMaximumRedeliveries(MAXIMUM_REDELIVERIES);
    redeliveryPolicy.setInitialRedeliveryDelay(INITIAL_REDELIVERY_DELAY_MS);
    redeliveryPolicy.setRedeliveryDelay(INITIAL_REDELIVERY_DELAY_MS);
    redeliveryPolicy.setUseExponentialBackOff(true);
    redeliveryPolicy.setBackOffMultiplier(2);

    DefaultMessageListenerContainer listenerContainer = new DefaultMessageListenerContainer();
    listenerContainer.setConnectionFactory(amqConnectionFactory);
    listenerContainer.setDestinationName(SUBMISSION_TASK_QUEUE);
    listenerContainer.setMessageListener(messageReceiver);
    listenerContainer.setConcurrentConsumers(1);
    listenerContainer.setSessionTransacted(true);
    listenerContainer.setRecoveryInterval(RECOVERY_INTERVAL_MS);
    // Do not consume submissions until the application has verified the judger's identity at
    // startup; the startup runner starts this container only after that check passes.
    listenerContainer.setAutoStartup(false);
    return listenerContainer;
  }
}
