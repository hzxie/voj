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

import jakarta.jms.JMSException;
import jakarta.jms.MapMessage;
import jakarta.jms.Message;
import jakarta.jms.MessageListener;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import org.verwandlung.voj.web.service.SubmissionService;

/**
 * Receives submission tasks that expired on the dead-letter queue and finalizes them.
 *
 * <p>A submission task expires when no judger claims it within its time-to-live, which in practice
 * means no online judger supports the submission's language. Rather than leaving such submissions
 * Pending forever, this listener marks them as failed.
 *
 * @author Haozhe Xie
 */
@Component
public class ExpiredSubmissionTaskReceiver implements MessageListener {
  /* (non-Javadoc)
   * @see jakarta.jms.MessageListener#onMessage(jakarta.jms.Message)
   */
  public void onMessage(Message message) {
    if (!(message instanceof MapMessage)) {
      return;
    }
    final MapMessage mapMessage = (MapMessage) message;

    try {
      // Defensive: the container's selector already restricts us to expired submission tasks.
      if (!"SubmissionCreated".equals(mapMessage.getString("event"))) {
        return;
      }
      long submissionId = mapMessage.getLong("submissionId");
      boolean marked = submissionService.markSubmissionUnjudgeable(submissionId);

      if (marked) {
        LOGGER.warn(
            String.format(
                "Submission task #%d expired with no available judger; marked as System Error.",
                new Object[] {submissionId}));
      } else {
        LOGGER.info(
            String.format(
                "Ignored expired submission task #%d; it was already judged or no longer exists.",
                new Object[] {submissionId}));
      }
    } catch (JMSException ex) {
      LOGGER.catching(ex);
    }
  }

  /** The autowired SubmissionService object, used to finalize submissions whose tasks expired. */
  @Autowired private SubmissionService submissionService;

  /** The logger. */
  private static final Logger LOGGER = LogManager.getLogger(ExpiredSubmissionTaskReceiver.class);
}
