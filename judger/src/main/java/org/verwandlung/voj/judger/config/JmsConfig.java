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
import org.apache.activemq.command.ActiveMQQueue;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.connection.CachingConnectionFactory;
import org.springframework.jms.core.JmsTemplate;

/**
 * The message queue configuration. Replaces the ActiveMQ / JMS definitions from the original
 * application-context.xml.
 *
 * <p>The judger sends the progress of its judgings and its heartbeats to {@code
 * vojJudgeResultQueue}. It receives nothing: it takes the pending submissions from the database.
 *
 * <p>It survives a restart of the broker (in the Docker setup it lives in the web container, so it
 * restarts with it): the cached connection is dropped once it fails, and the next message is sent
 * over a new one.
 *
 * @author Haozhe Xie
 */
@Configuration
public class JmsConfig {
  /** The name of the judging result queue (Judger -> Web). */
  private static final String JUDGE_RESULT_QUEUE = "vojJudgeResultQueue";

  /**
   * The JMS connection factory for sending, with a cached connection. The cache drops the
   * connection when it fails (reconnectOnException), so the next message after a broker restart is
   * sent over a new one.
   */
  @Bean
  public ConnectionFactory connectionFactory(@Value("${jms.broker.url}") String brokerUrl) {
    return new CachingConnectionFactory(new ActiveMQConnectionFactory(brokerUrl));
  }

  /** The JmsTemplate used to send messages to the judging result queue. */
  @Bean
  public JmsTemplate jmsTemplate(ConnectionFactory connectionFactory) {
    JmsTemplate jmsTemplate = new JmsTemplate(connectionFactory);
    jmsTemplate.setDefaultDestination(new ActiveMQQueue(JUDGE_RESULT_QUEUE));
    return jmsTemplate;
  }
}
