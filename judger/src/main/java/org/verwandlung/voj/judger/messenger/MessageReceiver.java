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

import jakarta.jms.JMSException;
import jakarta.jms.MapMessage;
import jakarta.jms.Message;
import jakarta.jms.MessageListener;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import org.verwandlung.voj.judger.application.ApplicationDispatcher;
import org.verwandlung.voj.judger.util.LogSanitizer;

/**
 * The message receiving service.
 *
 * <p>A message that cannot be understood is logged and dropped: delivering it again would not
 * help. A failure while handling a judging task is not caught here, though: it makes the listener
 * container roll back the task's transaction, so that the broker delivers the task again (see
 * {@code JmsConfig#submissionTaskListenerContainer}).
 *
 * @author Haozhe Xie
 */
@Component
public class MessageReceiver implements MessageListener {
  /* (non-Javadoc)
   * @see jakarta.jms.MessageListener#onMessage(jakarta.jms.Message)
   */
  public void onMessage(Message message) {
    if (!(message instanceof MapMessage)) {
      LOGGER.warn("Ignoring a message that is not a MapMessage: " + message);
      return;
    }
    long submissionId;
    Long generation;
    try {
      MapMessage mapMessage = (MapMessage) message;
      String event = mapMessage.getString("event");
      if (!"SubmissionCreated".equals(event)) {
        LOGGER.warn(
            String.format(
                "Unknown Event Received. [Event = %s]",
                new Object[] {LogSanitizer.forLog(event)}));
        return;
      }
      submissionId = mapMessage.getLong("submissionId");
      // Absent from a task queued by a web application that predates judging generations.
      generation =
          mapMessage.itemExists("generation") ? mapMessage.getLong("generation") : null;
    } catch (JMSException | RuntimeException ex) {
      LOGGER.error("Ignoring a malformed message", ex);
      return;
    }
    LOGGER.info(
        String.format(
            "Received new submission task #%d (generation %s)", submissionId, generation));
    dispatcher.onSubmissionCreated(submissionId, generation);
  }

  /** The autowired ApplicationDispatcher object, used to perform the callback after a message is received. */
  @Autowired private ApplicationDispatcher dispatcher;

  /** The logger. */
  private static final Logger LOGGER = LogManager.getLogger(MessageReceiver.class);
}
