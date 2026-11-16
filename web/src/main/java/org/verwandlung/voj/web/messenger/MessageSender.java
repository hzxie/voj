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
package org.verwandlung.voj.web.messenger;

import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;

/**
 * The message sending service: publishes the outcome of a submission that the web application
 * judged itself on the topic of the progress of the judgings, which every instance of the web
 * application receives (see {@link MessageReceiver}), so that each of them ends the live streams
 * of the submission.
 *
 * @author Haozhe Xie
 */
@Component
public class MessageSender {
  /**
   * Publishes the System Error of a submission. The message carries no claim token: no judging
   * sent it. A message that cannot be sent (e.g. while the broker restarts) is logged: the viewers
   * of the submission see its verdict anyway, which their pages read from the database every few
   * seconds.
   *
   * @param submissionId - the unique identifier of the submission
   * @param judgeLog - the judge log of the System Error
   * @return whether the message was sent
   */
  public boolean sendSystemError(long submissionId, String judgeLog) {
    try {
      judgeProgressTemplate.convertAndSend(
          Map.of("event", "ErrorOccurred", "submissionId", submissionId, "log", judgeLog));
      return true;
    } catch (RuntimeException ex) {
      LOGGER.warn(
          String.format("Failed to send the System Error of submission #%d.", submissionId), ex);
      return false;
    }
  }

  /** The JmsTemplate of the topic of the progress of the judgings. */
  @Autowired
  @Qualifier("judgeProgressTemplate")
  private JmsTemplate judgeProgressTemplate;

  /** The logger. */
  private static final Logger LOGGER = LogManager.getLogger(MessageSender.class);
}
