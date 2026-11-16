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
import org.apache.activemq.command.ActiveMQTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jms.connection.CachingConnectionFactory;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.listener.DefaultMessageListenerContainer;

/**
 * The message queue configuration. Replaces the ActiveMQ / JMS definitions in the original
 * dispatcher-servlet.xml.
 *
 * <p>Every instance of the web module subscribes to the topic {@value #JUDGE_PROGRESS_TOPIC}, on
 * which the judgers publish the progress of their judgings, so that the viewers of a judging see its
 * progress whichever instance serves them. The web module publishes there too, the outcome of a
 * submission it judged itself (see {@code SubmissionService#failSubmissionsWithoutJudger}), so that
 * every instance ends the live streams of the submission. The judgers take the pending submissions
 * from the database, and record their heartbeats there.
 *
 * <p>The messages are live updates, the database holds the verdicts: they are not persistent and
 * expire after {@value #MESSAGE_TIME_TO_LIVE_MS} ms, and a subscriber that is not running misses
 * them.
 *
 * <p>It survives a restart of an external broker: the listener container keeps trying to reconnect
 * every {@value #RECOVERY_INTERVAL_MS} ms until the broker is back.
 *
 * @author Haozhe Xie
 */
@Configuration
public class JmsConfig {
  /** The name of the topic of the progress of the judgings (Judger, Web -> Web). */
  static final String JUDGE_PROGRESS_TOPIC = "vojJudgeProgressTopic";

  /**
   * How long (ms) a message lives in the broker before it is dropped. Its expiry is set by the
   * clock of its sender and checked by the broker's and the receiver's: this leaves the clocks of
   * the hosts minutes to disagree by.
   */
  static final long MESSAGE_TIME_TO_LIVE_MS = 300000;

  /** How long (ms) a send may be held up by the broker's flow control before it fails. */
  static final int SEND_TIMEOUT_MS = 10000;

  /** How long (ms) the listener container waits between two attempts to reconnect. */
  static final long RECOVERY_INTERVAL_MS = 5000;

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
   * <p>The broker is non-persistent: a web restart drops the progress messages in flight, which
   * only feed the live view of a judging (the verdicts are in the database). That keeps the
   * embedded footprint small and suits the single-node use case this mode targets.
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
  @Primary
  @ConditionalOnProperty(name = "jms.broker.embedded", havingValue = "true")
  public ConnectionFactory connectionFactory(BrokerService embeddedBroker) {
    ActiveMQConnectionFactory connectionFactory =
        new ActiveMQConnectionFactory("vm://" + EMBEDDED_BROKER_NAME + "?create=false");
    connectionFactory.setSendTimeout(SEND_TIMEOUT_MS);
    return connectionFactory;
  }

  /**
   * The JMS connection factory used when talking to an external broker (the default). Dials the
   * configured {@code jms.broker.url} over TCP.
   */
  @Bean
  @Primary
  @ConditionalOnProperty(name = "jms.broker.embedded", havingValue = "false", matchIfMissing = true)
  public ConnectionFactory externalConnectionFactory(@Value("${jms.broker.url}") String brokerUrl) {
    ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory(brokerUrl);
    connectionFactory.setSendTimeout(SEND_TIMEOUT_MS);
    return connectionFactory;
  }

  /**
   * The connection factory of the publisher of the topic of the progress of the judgings: it keeps
   * one connection to the broker (and its sessions and producers) for every message, rather than a
   * connection of its own per message, as a JmsTemplate opens on a plain connection factory. The
   * connection is dropped once it fails (e.g. the broker restarts), and the next message opens a
   * new one.
   *
   * @param connectionFactory - the connection factory of the broker
   * @return the caching connection factory
   */
  @Bean
  public CachingConnectionFactory judgeProgressConnectionFactory(
      ConnectionFactory connectionFactory) {
    CachingConnectionFactory cachingConnectionFactory =
        new CachingConnectionFactory(connectionFactory);
    cachingConnectionFactory.setReconnectOnException(true);
    return cachingConnectionFactory;
  }

  /**
   * The JmsTemplate that publishes to the topic of the progress of the judgings: messages that are
   * not persistent and expire.
   *
   * @param connectionFactory - the connection factory of the publisher (see {@link
   *     #judgeProgressConnectionFactory})
   * @return the JmsTemplate
   */
  @Bean
  public JmsTemplate judgeProgressTemplate(
      @Qualifier("judgeProgressConnectionFactory") ConnectionFactory connectionFactory) {
    JmsTemplate jmsTemplate = new JmsTemplate(connectionFactory);
    jmsTemplate.setPubSubDomain(true);
    jmsTemplate.setDefaultDestination(new ActiveMQTopic(JUDGE_PROGRESS_TOPIC));
    jmsTemplate.setExplicitQosEnabled(true);
    jmsTemplate.setDeliveryPersistent(false);
    jmsTemplate.setTimeToLive(MESSAGE_TIME_TO_LIVE_MS);
    return jmsTemplate;
  }

  /**
   * The message listener container that subscribes to the topic of the progress of the judgings,
   * with a single consumer (a subscription, which receives every message published while it
   * lasts).
   *
   * <p>A DefaultMessageListenerContainer, which re-establishes its connection in a loop until the
   * broker is reachable again; a SimpleMessageListenerContainer tries to recover only once, right
   * when the connection fails (while the broker is still down), and then stops consuming for good.
   * It holds its connection itself, on the broker's connection factory (not a caching one).
   *
   * @param connectionFactory - the connection factory of the broker
   * @param messageReceiver - the listener of the messages
   * @return the listener container
   */
  @Bean
  public DefaultMessageListenerContainer judgeProgressListenerContainer(
      ConnectionFactory connectionFactory, MessageListener messageReceiver) {
    DefaultMessageListenerContainer listenerContainer = new DefaultMessageListenerContainer();
    listenerContainer.setConnectionFactory(connectionFactory);
    listenerContainer.setPubSubDomain(true);
    listenerContainer.setDestinationName(JUDGE_PROGRESS_TOPIC);
    listenerContainer.setMessageListener(messageReceiver);
    listenerContainer.setRecoveryInterval(RECOVERY_INTERVAL_MS);
    return listenerContainer;
  }
}
