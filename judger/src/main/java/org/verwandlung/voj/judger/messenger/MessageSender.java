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
package org.verwandlung.voj.judger.messenger;

import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;

/**
 * The message sending service: streams the progress of the judgings to the web application.
 *
 * <p>The messages are sent in the background, by a thread of their own, in the order they were
 * given, so the caller (a judging) never waits for the broker: the messages only stream the
 * progress live (the verdicts are in the database), and a broker that is down, slow or holds the
 * sender up (e.g. by flow control) must not hold a judging up. While {@value #MAX_PENDING_MESSAGES}
 * messages wait to be sent, further ones are dropped, which is logged once until the sender caught
 * up. A message the broker does not take (e.g. while it restarts) is dropped too: the first one of
 * an outage is logged, and the next one that is sent ends it.
 *
 * @author Haozhe Xie
 */
@Component
public class MessageSender {
  /**
   * Sends a message to the web application, in the background.
   *
   * @param mapMessage - the message in key-value format
   */
  public void sendMessage(final Map<String, Object> mapMessage) {
    try {
      sender.execute(() -> send(mapMessage));
    } catch (RejectedExecutionException ex) {
      if (!isDropping) {
        isDropping = true;
        LOGGER.warn(
            "The messages to the web application are not sent as fast as they come (the broker is"
                + " unreachable or slow): dropping them until the sender caught up.");
      }
    }
  }

  /**
   * Sends a message, on the sender's thread.
   *
   * @param mapMessage - the message in key-value format
   */
  private void send(Map<String, Object> mapMessage) {
    try {
      jmsTemplate.convertAndSend(mapMessage);
      if (isFailing) {
        isFailing = false;
        LOGGER.info("The messages to the web application are sent again.");
      }
    } catch (RuntimeException ex) {
      if (!isFailing) {
        isFailing = true;
        LOGGER.warn(
            "Failed to send the "
                + mapMessage.get("event")
                + " message to the web application (is the broker reachable?): dropping the"
                + " messages until one is sent again.",
            ex);
      } else {
        LOGGER.debug("Failed to send the " + mapMessage.get("event") + " message", ex);
      }
    }
    if (isDropping && sender.getQueue().isEmpty()) {
      isDropping = false;
      LOGGER.info("The sender of the messages to the web application caught up.");
    }
  }

  /** The autowired JmsTemplate object, used to send messages to the message queue. */
  @Autowired private JmsTemplate jmsTemplate;

  /** How many messages may wait to be sent. */
  static final int MAX_PENDING_MESSAGES = 1000;

  /**
   * Sends the messages, one at a time, on a daemon thread (which never keeps the judger alive); a
   * message that does not fit the queue is rejected.
   */
  private final ThreadPoolExecutor sender =
      new ThreadPoolExecutor(
          1,
          1,
          0,
          TimeUnit.MILLISECONDS,
          new ArrayBlockingQueue<>(MAX_PENDING_MESSAGES),
          runnable -> {
            Thread thread = new Thread(runnable, "voj-message-sender");
            thread.setDaemon(true);
            return thread;
          });

  /** Whether messages are being dropped as the sender is behind. */
  private volatile boolean isDropping;

  /** Whether the last message could not be sent; only the sender's thread uses it. */
  private boolean isFailing;

  /** The logger. */
  private static final Logger LOGGER = LogManager.getLogger(MessageSender.class);
}
