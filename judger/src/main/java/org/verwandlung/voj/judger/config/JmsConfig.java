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
import jakarta.jms.JMSException;
import jakarta.jms.MessageConsumer;
import jakarta.jms.MessageListener;
import jakarta.jms.Session;

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

  /**
   * How long (ms) a judger whose judging of a task failed waits before it takes a task again; it
   * doubles with each further failure in a row, up to {@link #MAXIMUM_PAUSE_MS}.
   */
  static final long INITIAL_PAUSE_MS = 10000;

  /** The longest pause (ms) of a judger whose judgings keep failing. */
  public static final long MAXIMUM_PAUSE_MS = INITIAL_PAUSE_MS << (MAXIMUM_REDELIVERIES - 1);

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
   * It has its own (uncached) connection factory, as the container caches its connection itself,
   * without prefetch. A single consumer: judging is serialized anyway (see {@code
   * Dispatcher#createNewTask}).
   *
   * <p>Each task is received in a local JMS transaction that is committed only once the listener
   * returns. If it throws, which it does when the outcome of the judging could not be recorded or
   * the judging should be tried again (see {@code RetryableJudgingException}), the transaction is
   * rolled back and the task is delivered again, to any judger, up to {@value
   * #MAXIMUM_REDELIVERIES} times, then dead-lettered. A judger that dies while judging never
   * commits either, so its task is judged elsewhere. See {@link SubmissionTaskListenerContainer}
   * for how a rolled back task gets to another judger.
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
    // Counts the deliveries and dead-letters a task delivered too often. Its delays are those of a
    // redelivery to the same consumer, which never happens here (the consumer is closed first).
    RedeliveryPolicy redeliveryPolicy = amqConnectionFactory.getRedeliveryPolicy();
    redeliveryPolicy.setMaximumRedeliveries(MAXIMUM_REDELIVERIES);
    redeliveryPolicy.setInitialRedeliveryDelay(0);
    redeliveryPolicy.setRedeliveryDelay(0);

    SubmissionTaskListenerContainer listenerContainer = new SubmissionTaskListenerContainer();
    listenerContainer.setConnectionFactory(amqConnectionFactory);
    listenerContainer.setDestinationName(SUBMISSION_TASK_QUEUE);
    listenerContainer.setMessageListener(messageReceiver);
    listenerContainer.setConcurrentConsumers(1);
    listenerContainer.setSessionTransacted(true);
    listenerContainer.setCacheLevel(DefaultMessageListenerContainer.CACHE_SESSION);
    listenerContainer.setRecoveryInterval(RECOVERY_INTERVAL_MS);
    // Do not consume submissions until the application has verified the judger's identity at
    // startup; the startup runner starts this container only after that check passes.
    listenerContainer.setAutoStartup(false);
    return listenerContainer;
  }

  /**
   * The listener container of the judging tasks, which hands a task whose judging failed to
   * another judger.
   *
   * <p>ActiveMQ redelivers a rolled back message on the client: the consumer puts it back into its
   * own queue and delivers it to itself again after the redelivery delay, while the broker keeps
   * it assigned to that consumer. A judger with a fault of its own (wrong database credentials, a
   * network partition, a full disk) would then fail the task again and again until it is
   * dead-lettered, and the next one after it, while healthy judgers sit idle. So the consumer is
   * not cached: it is closed after every receive ({@code CACHE_SESSION}), which hands a rolled back
   * task back to the broker, for whichever judger asks for a task first. The broker counts the
   * delivery, and the consumer that receives the task once too often dead-letters it.
   *
   * <p>The judger whose judging failed waits before it asks for a task again, {@value
   * #INITIAL_PAUSE_MS} ms after a failure and twice as long after each further one in a row (up to
   * {@link #MAXIMUM_PAUSE_MS} ms), so that another judger takes the task first, and so that a
   * judger on its own does not burn every delivery of a task within seconds (e.g. while the
   * database restarts). The pause ends as soon as a judging succeeds.
   */
  static class SubmissionTaskListenerContainer extends DefaultMessageListenerContainer {
    /* (non-Javadoc)
     * @see org.springframework.jms.listener.AbstractPollingMessageListenerContainer#receiveAndExecute
     */
    @Override
    protected boolean receiveAndExecute(
        Object invoker, Session session, MessageConsumer consumer) throws JMSException {
      if (!pauseAfterFailures()) {
        return false;
      }
      listenerFailed = false;
      boolean messageReceived = super.receiveAndExecute(invoker, session, consumer);
      if (messageReceived) {
        consecutiveFailures = listenerFailed ? consecutiveFailures + 1 : 0;
      }
      return messageReceived;
    }

    /* (non-Javadoc)
     * @see org.springframework.jms.listener.AbstractMessageListenerContainer#handleListenerException
     */
    @Override
    protected void handleListenerException(Throwable ex) {
      listenerFailed = true;
      super.handleListenerException(ex);
    }

    /**
     * Waits after judgings that failed in a row, until the pause is over or the container stops.
     *
     * @return whether the container may receive a task (it is still running)
     */
    private boolean pauseAfterFailures() {
      if (consecutiveFailures == 0) {
        return true;
      }
      long pause =
          Math.min(initialPauseMs << Math.min(consecutiveFailures - 1, 20), MAXIMUM_PAUSE_MS);
      long end = System.currentTimeMillis() + pause;
      for (long left = pause; left > 0; left = end - System.currentTimeMillis()) {
        if (!isRunning()) {
          return false;
        }
        try {
          Thread.sleep(Math.min(left, 1000));
        } catch (InterruptedException ex) {
          Thread.currentThread().interrupt();
          return false;
        }
      }
      return isRunning();
    }

    /**
     * Sets how long the judger waits after a failed judging (for tests).
     *
     * @param initialPauseMs - the pause (ms) after the first failure in a row
     */
    void setInitialPauseMs(long initialPauseMs) {
      this.initialPauseMs = initialPauseMs;
    }

    /** The pause (ms) after the first failure in a row. */
    private volatile long initialPauseMs = INITIAL_PAUSE_MS;

    /** How many judgings failed in a row (touched by the single consumer thread only). */
    private volatile int consecutiveFailures;

    /** Whether the listener failed in the current receive. */
    private volatile boolean listenerFailed;
  }
}
