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
import org.verwandlung.voj.web.service.SubmissionService.JudgingTaskFailure;

/**
 * Receives the judging tasks that ended on the dead-letter queue and finalizes their submissions.
 *
 * <p>A judging task is dead-lettered for one of two reasons, which the broker records in the
 * {@value #DLQ_DELIVERY_FAILURE_CAUSE} property of the dead-lettered copy:
 *
 * <ul>
 *   <li>it expired: no judger took it within its time-to-live, which in practice means no online
 *       judger supports the submission's language ({@code "Message Expired. Expiration:..."});
 *   <li>a judger took it but judging it failed again and again, until the redelivery limit was
 *       reached ({@code "Delivery[n] exceeds redelivery policy limit:..."}).
 * </ul>
 *
 * <p>Rather than leaving such a submission pending forever, it is marked as a system error with a
 * judge log that says which of the two happened.
 *
 * @author Haozhe Xie
 */
@Component
public class DeadLetterSubmissionTaskReceiver implements MessageListener {
  /** The message property in which ActiveMQ records why it dead-lettered a message. */
  static final String DLQ_DELIVERY_FAILURE_CAUSE = "dlqDeliveryFailureCause";

  /* (non-Javadoc)
   * @see jakarta.jms.MessageListener#onMessage(jakarta.jms.Message)
   */
  public void onMessage(Message message) {
    if (!(message instanceof MapMessage)) {
      return;
    }
    final MapMessage mapMessage = (MapMessage) message;

    try {
      // Defensive: the container's selector already restricts us to judging tasks.
      if (!"SubmissionCreated".equals(mapMessage.getString("event"))) {
        return;
      }
      long submissionId = mapMessage.getLong("submissionId");
      String cause = mapMessage.getStringProperty(DLQ_DELIVERY_FAILURE_CAUSE);
      JudgingTaskFailure failure = getJudgingTaskFailure(cause);
      boolean marked = submissionService.markSubmissionUnjudgeable(submissionId, failure);

      if (marked) {
        LOGGER.warn(
            String.format(
                "Judging task #%d was dead-lettered (%s: %s); marked as System Error.",
                submissionId, failure, cause));
      } else {
        LOGGER.info(
            String.format(
                "Ignored dead-lettered judging task #%d; it was already judged or no longer exists.",
                submissionId));
      }
    } catch (JMSException ex) {
      LOGGER.catching(ex);
    }
  }

  /**
   * Tells why the broker dead-lettered a judging task from the cause it recorded.
   *
   * @param cause - the {@value #DLQ_DELIVERY_FAILURE_CAUSE} property, or null
   * @return why the task was dead-lettered
   */
  static JudgingTaskFailure getJudgingTaskFailure(String cause) {
    if (cause == null) {
      return JudgingTaskFailure.UNKNOWN;
    }
    if (cause.contains("exceeds redelivery policy limit")) {
      return JudgingTaskFailure.JUDGING_FAILED_REPEATEDLY;
    }
    if (cause.contains("Message Expired")) {
      return JudgingTaskFailure.NO_JUDGER_AVAILABLE;
    }
    return JudgingTaskFailure.UNKNOWN;
  }

  /** The autowired SubmissionService object, used to finalize the dead-lettered submissions. */
  @Autowired private SubmissionService submissionService;

  /** The logger. */
  private static final Logger LOGGER = LogManager.getLogger(DeadLetterSubmissionTaskReceiver.class);
}
