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
 * @author Haozhe Xie
 */
@Component
public class MessageReceiver implements MessageListener {
  /* (non-Javadoc)
   * @see jakarta.jms.MessageListener#onMessage(jakarta.jms.Message)
   */
  public void onMessage(Message message) {
    if (message instanceof MapMessage) {
      final MapMessage mapMessage = (MapMessage) message;

      try {
        String event = mapMessage.getString("event");

        if ("SubmissionCreated".equals(event)) {
          newSubmissionHandler(mapMessage);
        } else {
          LOGGER.warn(
              String.format(
                  "Unknown Event Received. [Event = %s]",
                  new Object[] {LogSanitizer.forLog(event)}));
        }
      } catch (Exception ex) {
        LOGGER.catching(ex);
      }
    }
  }

  /**
   * Handles a new submission request.
   *
   * @param mapMessage - the MapMessage object received from the message queue
   * @throws JMSException
   */
  private void newSubmissionHandler(MapMessage mapMessage) throws JMSException {
    long submissionId = mapMessage.getLong("submissionId");
    LOGGER.info(String.format("Received new submission task #%d", new Object[] {submissionId}));

    dispatcher.onSubmissionCreated(submissionId);
  }

  /** The autowired ApplicationDispatcher object, used to perform the callback after a message is received. */
  @Autowired private ApplicationDispatcher dispatcher;

  /** The logger. */
  private static final Logger LOGGER = LogManager.getLogger(MessageReceiver.class);
}
