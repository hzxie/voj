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

import jakarta.jms.ConnectionFactory;
import jakarta.jms.MessageListener;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.broker.BrokerService;
import org.apache.activemq.command.ActiveMQQueue;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.connection.CachingConnectionFactory;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.listener.SimpleMessageListenerContainer;

import org.verwandlung.voj.web.messenger.ExpiredSubmissionTaskReceiver;

/**
 * The message queue configuration. Replaces the ActiveMQ / JMS definitions in the original
 * dispatcher-servlet.xml.
 *
 * <p>The web module dispatches pending judge tasks to {@code vojSubmissionTaskQueue}, and listens to
 * {@code vojJudgeResultQueue} to receive judge results.
 *
 * @author Haozhe Xie
 */
@Configuration
public class JmsConfig {
  /** The name of the judge task queue (Web -> Judger). */
  private static final String SUBMISSION_TASK_QUEUE = "vojSubmissionTaskQueue";

  /** The name of the judge result queue (Judger -> Web). */
  private static final String JUDGE_RESULT_QUEUE = "vojJudgeResultQueue";

  /** The ActiveMQ dead-letter queue that expired, undeliverable messages land on by default. */
  private static final String DEAD_LETTER_QUEUE = "ActiveMQ.DLQ";

  /** The well-known name of the in-JVM broker, matched by the {@code vm://localhost} transport. */
  private static final String EMBEDDED_BROKER_NAME = "localhost";

  /**
   * An ActiveMQ broker embedded in this JVM, created only when {@code jms.broker.embedded=true}.
   *
   * <p>This lets a single-instance deployment drop the standalone broker process (and its whole
   * JVM, ~150&nbsp;MB+) entirely. The web module talks to it over the in-process {@code vm://}
   * transport, while out-of-process judgers still reach it through the TCP connector opened here
   * (the same {@code jms.broker.url} address the judger already dials).
   *
   * <p>The broker is non-persistent: a web restart drops any in-flight submission tasks. That keeps
   * the embedded footprint small and is acceptable for the single-node use case this mode targets;
   * deployments that need durable queues should run an external broker (the default,
   * {@code jms.broker.embedded=false}) instead.
   *
   * <p><strong>Do not run the bundled standalone broker at the same time</strong> — both bind
   * {@code jms.broker.url}, so the Docker image / supervisord must skip the separate ActiveMQ
   * process whenever this mode is enabled.
   */
  @Bean(initMethod = "start", destroyMethod = "stop")
  @ConditionalOnProperty(name = "jms.broker.embedded", havingValue = "true")
  public BrokerService embeddedBroker(@Value("${jms.broker.url}") String brokerUrl)
      throws Exception {
    BrokerService broker = new BrokerService();
    broker.setBrokerName(EMBEDDED_BROKER_NAME);
    broker.setPersistent(false);
    broker.setUseJmx(false);
    // Spring manages the lifecycle via initMethod/destroyMethod; no JVM shutdown hook needed.
    broker.setUseShutdownHook(false);
    // Expose the TCP connector so out-of-process judgers can connect.
    broker.addConnector(brokerUrl);
    return broker;
  }

  /**
   * The JMS connection factory used when the broker is embedded. Declaring {@code embeddedBroker} as
   * a parameter makes Spring start the broker before this factory is created, so the in-process
   * {@code vm://localhost} transport always finds a running broker ({@code create=false} stops it
   * from silently spinning up a second, unconfigured one).
   */
  @Bean
  @ConditionalOnProperty(name = "jms.broker.embedded", havingValue = "true")
  public ConnectionFactory connectionFactory(BrokerService embeddedBroker) {
    ActiveMQConnectionFactory amqConnectionFactory =
        new ActiveMQConnectionFactory("vm://" + EMBEDDED_BROKER_NAME + "?create=false");
    return new CachingConnectionFactory(amqConnectionFactory);
  }

  /**
   * The JMS connection factory used when talking to an external broker (the default). Dials the
   * configured {@code jms.broker.url} over TCP.
   */
  @Bean
  @ConditionalOnProperty(name = "jms.broker.embedded", havingValue = "false", matchIfMissing = true)
  public ConnectionFactory externalConnectionFactory(@Value("${jms.broker.url}") String brokerUrl) {
    ActiveMQConnectionFactory amqConnectionFactory = new ActiveMQConnectionFactory(brokerUrl);
    return new CachingConnectionFactory(amqConnectionFactory);
  }

  /** The JmsTemplate used to dispatch messages to the judge task queue. */
  @Bean
  public JmsTemplate jmsTemplate(
      ConnectionFactory connectionFactory,
      @Value("${jms.submissionTask.ttl:1800000}") long submissionTaskTtl) {
    JmsTemplate jmsTemplate = new JmsTemplate(connectionFactory);
    jmsTemplate.setDefaultDestination(new ActiveMQQueue(SUBMISSION_TASK_QUEUE));
    // Give each submission task a time-to-live. If no judger claims it before then (e.g. no online
    // judger supports its language), the broker expires it to the dead-letter queue, where
    // deadLetterListenerContainer picks it up and marks the submission as failed instead of leaving
    // it Pending forever. Keep the default generous so a temporarily busy or offline (but capable)
    // cluster does not fail otherwise valid submissions.
    jmsTemplate.setExplicitQosEnabled(true);
    jmsTemplate.setTimeToLive(submissionTaskTtl);
    return jmsTemplate;
  }

  /** The message listener container that listens to the judge result queue. */
  @Bean
  public SimpleMessageListenerContainer judgeResultListenerContainer(
      ConnectionFactory connectionFactory, MessageListener messageReceiver) {
    SimpleMessageListenerContainer listenerContainer = new SimpleMessageListenerContainer();
    listenerContainer.setConnectionFactory(connectionFactory);
    listenerContainer.setDestinationName(JUDGE_RESULT_QUEUE);
    listenerContainer.setMessageListener(messageReceiver);
    return listenerContainer;
  }

  /**
   * The message listener container that watches the dead-letter queue for submission tasks that
   * expired before any judger could handle them. The selector keeps this consumer from touching
   * unrelated dead-lettered messages (only our submission tasks carry the {@code event} property).
   */
  @Bean
  public SimpleMessageListenerContainer deadLetterListenerContainer(
      ConnectionFactory connectionFactory,
      ExpiredSubmissionTaskReceiver expiredSubmissionTaskReceiver) {
    SimpleMessageListenerContainer listenerContainer = new SimpleMessageListenerContainer();
    listenerContainer.setConnectionFactory(connectionFactory);
    listenerContainer.setDestinationName(DEAD_LETTER_QUEUE);
    listenerContainer.setMessageSelector("event = 'SubmissionCreated'");
    listenerContainer.setMessageListener(expiredSubmissionTaskReceiver);
    return listenerContainer;
  }
}
