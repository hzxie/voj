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
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.verwandlung.voj.web.util.LogSanitizer;

/**
 * The message receiving service. Receives the progress of the judgings, which the judgers publish
 * on a topic, and forwards it to the live streams of the submissions on this instance.
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

        if ("ErrorOccurred".equals(event)) {
          errorHandler(mapMessage);
        } else if ("CompileFinished".equals(event)) {
          compileFinishedHandler(mapMessage);
        } else if ("TestPointFinished".equals(event)) {
          testPointFinishedHandler(mapMessage);
        } else if ("AllTestPointsFinished".equals(event)) {
          allTestPointsFinishedHandler(mapMessage);
        } else {
          LOGGER.warn(String.format("Unknown Event Received. [Event = %s]", new Object[] {event}));
        }
      } catch (JMSException ex) {
        LOGGER.catching(ex);
      }
    }
  }

  /**
   * Handles the message of a System Error. Its judge log is read from the database by the page.
   *
   * @param mapMessage - the MapMessage object received from the message queue
   * @throws JMSException
   */
  private void errorHandler(MapMessage mapMessage) throws JMSException {
    long submissionId = mapMessage.getLong("submissionId");
    long claimToken = mapMessage.getLong("claimToken");
    eventPublisher.publishEvent(
        new SubmissionEvent(
            this, submissionId, claimToken, "System Error", "System Error.", true));
    LOGGER.info(String.format("Submission #%d returned [System Error].", submissionId));
  }

  /**
   * Handles the message of the judger finishing compilation.
   *
   * @param mapMessage - the MapMessage object received from the message queue
   * @throws JMSException
   */
  private void compileFinishedHandler(MapMessage mapMessage) throws JMSException {
    long submissionId = mapMessage.getLong("submissionId");
    long claimToken = mapMessage.getLong("claimToken");
    boolean isSuccessful = mapMessage.getBoolean("isSuccessful");
    String log = mapMessage.getString("log");

    if (isSuccessful) {
      String message = "Compile Successfully.\n\n";
      eventPublisher.publishEvent(
          new SubmissionEvent(this, submissionId, claimToken, "Running", message, false));
      LOGGER.info(String.format("Submission #%d returned [Compile Successfully].", submissionId));
    } else {
      eventPublisher.publishEvent(
          new SubmissionEvent(this, submissionId, claimToken, "Compiler Error", log, true));
      LOGGER.info(
          String.format(
              "Submission #%d returned [Compile Error].\n\tError Message:%s",
              new Object[] {submissionId, LogSanitizer.forLog(log)}));
    }
  }

  /**
   * Handles the message of the judger finishing a single test point.
   *
   * @param mapMessage - the MapMessage object received from the message queue
   * @throws JMSException
   */
  private void testPointFinishedHandler(MapMessage mapMessage) throws JMSException {
    long submissionId = mapMessage.getLong("submissionId");
    long claimToken = mapMessage.getLong("claimToken");
    int checkpointId = mapMessage.getInt("checkpointId");
    String runtimeResult = mapMessage.getString("runtimeResult");
    int usedTime = mapMessage.getInt("usedTime");
    int usedMemory = mapMessage.getInt("usedMemory");
    int score = mapMessage.getInt("score");

    String message =
        String.format(
            "- Test Point #%d: %s, Time = %d ms, Memory = %d KB, Score = %d\n",
            new Object[] {checkpointId, runtimeResult, usedTime, usedMemory, score});
    eventPublisher.publishEvent(
        new SubmissionEvent(this, submissionId, claimToken, "Running", message, false));

    LOGGER.info(
        String.format(
            "Submission #%d/ CheckPoint#%d returned [%s] (Time = %dms, Memory = %d KB, Score ="
                + " %d).",
            new Object[] {
              submissionId,
              checkpointId,
              LogSanitizer.forLog(runtimeResult),
              usedTime,
              usedMemory,
              score
            }));
  }

  /**
   * Handles the message of the judger finishing all test points.
   *
   * @param mapMessage - the MapMessage object received from the message queue
   * @throws JMSException
   */
  private void allTestPointsFinishedHandler(MapMessage mapMessage) throws JMSException {
    long submissionId = mapMessage.getLong("submissionId");
    long claimToken = mapMessage.getLong("claimToken");
    String runtimeResult = mapMessage.getString("runtimeResult");
    int usedTime = mapMessage.getInt("totalTime");
    int usedMemory = mapMessage.getInt("maxMemory");
    int score = mapMessage.getInt("totalScore");

    String message =
        String.format(
            "\n%s, Time = %d ms, Memory = %d KB, Score = %d\n",
            new Object[] {runtimeResult, usedTime, usedMemory, score});
    eventPublisher.publishEvent(
        new SubmissionEvent(this, submissionId, claimToken, runtimeResult, message, true));

    LOGGER.info(
        String.format(
            "Submission #%d judge completed and returned [%s] (Time = %d ms, Memory = %d KB, Score"
                + " = %d).",
            new Object[] {
              submissionId, LogSanitizer.forLog(runtimeResult), usedTime, usedMemory, score
            }));
  }

  /**
   * The autowired ApplicationEventPublisher object. Used for dispatching messages after receiving
   * them from the judgers.
   */
  @Autowired private ApplicationEventPublisher eventPublisher;

  /** The logger. */
  private static final Logger LOGGER = LogManager.getLogger(MessageReceiver.class);
}
