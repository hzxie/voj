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
import org.apache.activemq.command.ActiveMQQueue;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.connection.CachingConnectionFactory;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.listener.SimpleMessageListenerContainer;

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

  /** The JMS connection factory with connection caching. */
  @Bean
  public ConnectionFactory connectionFactory(@Value("${jms.broker.url}") String brokerUrl) {
    ActiveMQConnectionFactory amqConnectionFactory = new ActiveMQConnectionFactory(brokerUrl);
    return new CachingConnectionFactory(amqConnectionFactory);
  }

  /** The JmsTemplate used to dispatch messages to the judge task queue. */
  @Bean
  public JmsTemplate jmsTemplate(ConnectionFactory connectionFactory) {
    JmsTemplate jmsTemplate = new JmsTemplate(connectionFactory);
    jmsTemplate.setDefaultDestination(new ActiveMQQueue(SUBMISSION_TASK_QUEUE));
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
}
