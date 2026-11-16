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

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.command.ActiveMQTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.connection.CachingConnectionFactory;
import org.springframework.jms.core.JmsTemplate;

/**
 * The message queue configuration: the connection to the ActiveMQ broker at {@code jms.broker.url}.
 *
 * <p>The judger publishes the progress of its judgings on the topic {@value #JUDGE_PROGRESS_TOPIC},
 * which every instance of the web application subscribes to, so that the viewers of a judging see
 * its progress whichever instance serves them. It receives nothing: it takes the pending
 * submissions from the database, and records its heartbeats there.
 *
 * <p>The messages are live updates, the database holds the verdicts: they are not persistent, a
 * web application that is not subscribed misses them, and a send that the broker holds up (by flow
 * control) fails after {@value #SEND_TIMEOUT_MS} ms (see also {@code MessageSender}, which sends in
 * the background).
 *
 * <p>It survives a restart of the broker (in the Docker setup it lives in the web container, so it
 * restarts with it): the cached connection is dropped once it fails, and the next message is sent
 * over a new one.
 *
 * @author Haozhe Xie
 */
@Configuration
public class JmsConfig {
  /** The name of the topic of the progress of the judgings (Judger -> Web). */
  static final String JUDGE_PROGRESS_TOPIC = "vojJudgeProgressTopic";

  /** How long (ms) a send may be held up by the broker's flow control before it fails. */
  static final int SEND_TIMEOUT_MS = 10000;

  /**
   * The JMS connection factory for sending, with a cached connection. The cache drops the
   * connection when it fails (reconnectOnException), so the next message after a broker restart is
   * sent over a new one.
   */
  @Bean
  public ConnectionFactory connectionFactory(@Value("${jms.broker.url}") String brokerUrl) {
    ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory(brokerUrl);
    connectionFactory.setSendTimeout(SEND_TIMEOUT_MS);
    return new CachingConnectionFactory(connectionFactory);
  }

  /** The JmsTemplate used to publish to the topic of the progress of the judgings. */
  @Bean
  public JmsTemplate jmsTemplate(ConnectionFactory connectionFactory) {
    JmsTemplate jmsTemplate = new JmsTemplate(connectionFactory);
    jmsTemplate.setPubSubDomain(true);
    jmsTemplate.setDefaultDestination(new ActiveMQTopic(JUDGE_PROGRESS_TOPIC));
    jmsTemplate.setExplicitQosEnabled(true);
    jmsTemplate.setDeliveryPersistent(false);
    return jmsTemplate;
  }
}
