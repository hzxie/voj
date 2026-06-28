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

import java.util.Calendar;
import java.util.Date;

import jakarta.jms.JMSException;
import jakarta.jms.MapMessage;
import jakarta.jms.Message;
import jakarta.jms.MessageListener;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * The message receiving service. Used to receive real-time judge results from the judgers.
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
        } else if ("KeepAlive".equals(event)) {
          receiveFromAliveJudgersHandler(mapMessage);
        } else {
          LOGGER.warn(String.format("Unknown Event Received. [Event = %s]", new Object[] {event}));
        }
      } catch (JMSException ex) {
        LOGGER.catching(ex);
      }
    }
  }

  /**
   * Handles the message of an internal error occurring in the judger.
   *
   * @param mapMessage - the MapMessage object received from the message queue
   * @throws JMSException
   */
  private void errorHandler(MapMessage mapMessage) throws JMSException {
    long submissionId = mapMessage.getLong("submissionId");
    eventPublisher.publishEvent(
        new SubmissionEvent(this, submissionId, "System Error", "System Error.", true));
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
    boolean isSuccessful = mapMessage.getBoolean("isSuccessful");
    String log = mapMessage.getString("log");

    if (isSuccessful) {
      String message = "Compile Successfully.\n\n";
      eventPublisher.publishEvent(
          new SubmissionEvent(this, submissionId, "Running", message, false));
      LOGGER.info(String.format("Submission #%d returned [Compile Successfully].", submissionId));
    } else {
      eventPublisher.publishEvent(
          new SubmissionEvent(this, submissionId, "Compiler Error", log, true));
      LOGGER.info(
          String.format(
              "Submission #%d returned [Compile Error].\n\tError Message:%s",
              new Object[] {submissionId, log}));
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
    int checkpointId = mapMessage.getInt("checkpointId");
    String runtimeResult = mapMessage.getString("runtimeResult");
    int usedTime = mapMessage.getInt("usedTime");
    int usedMemory = mapMessage.getInt("usedMemory");
    int score = mapMessage.getInt("score");

    String message =
        String.format(
            "- Test Point #%d: %s, Time = %d ms, Memory = %d KB, Score = %d\n",
            new Object[] {checkpointId, runtimeResult, usedTime, usedMemory, score});
    eventPublisher.publishEvent(new SubmissionEvent(this, submissionId, "Running", message, false));

    LOGGER.info(
        String.format(
            "Submission #%d/ CheckPoint#%d returned [%s] (Time = %dms, Memory = %d KB, Score ="
                + " %d).",
            new Object[] {submissionId, checkpointId, runtimeResult, usedTime, usedMemory, score}));
  }

  /**
   * Handles the message of the judger finishing all test points.
   *
   * @param mapMessage - the MapMessage object received from the message queue
   * @throws JMSException
   */
  private void allTestPointsFinishedHandler(MapMessage mapMessage) throws JMSException {
    long submissionId = mapMessage.getLong("submissionId");
    String runtimeResult = mapMessage.getString("runtimeResult");
    int usedTime = mapMessage.getInt("totalTime");
    int usedMemory = mapMessage.getInt("maxMemory");
    int score = mapMessage.getInt("totalScore");

    String message =
        String.format(
            "\n%s, Time = %d ms, Memory = %d KB, Score = %d\n",
            new Object[] {runtimeResult, usedTime, usedMemory, score});
    eventPublisher.publishEvent(
        new SubmissionEvent(this, submissionId, runtimeResult, message, true));

    LOGGER.info(
        String.format(
            "Submission #%d judge completed and returned [%s] (Time = %d ms, Memory = %d KB, Score"
                + " = %d).",
            new Object[] {submissionId, runtimeResult, usedTime, usedMemory, score}));
  }

  /**
   * Handles the Keep-Alive message from the judger. Used to obtain the information of backend
   * judgers on the web side.
   *
   * @param mapMessage - the MapMessage object received from the message queue
   * @throws JMSException
   */
  private void receiveFromAliveJudgersHandler(MapMessage mapMessage) throws JMSException {
    String judgerUsername = mapMessage.getString("username");
    String judgerDescription = mapMessage.getString("description");
    long heartbeatTimeInMillis = mapMessage.getLong("heartbeatTime");
    // Telemetry fields are tolerated as optional so older judgers still report in.
    int cpuLoad = mapMessage.itemExists("cpuLoad") ? mapMessage.getInt("cpuLoad") : -1;
    int memoryUsage = mapMessage.itemExists("memoryUsage") ? mapMessage.getInt("memoryUsage") : -1;
    long uptime = mapMessage.itemExists("uptime") ? mapMessage.getLong("uptime") : 0;

    Calendar calendar = Calendar.getInstance();
    calendar.setTimeInMillis(heartbeatTimeInMillis);
    Date heartbeatTime = calendar.getTime();

    eventPublisher.publishEvent(
        new KeepAliveEvent(
            this, judgerUsername, judgerDescription, heartbeatTime, cpuLoad, memoryUsage, uptime));
    LOGGER.info(String.format("Received heartbeat from Judger[%s]", judgerUsername));
  }

  /**
   * The autowired ApplicationEventPublisher object. Used for dispatching messages after receiving
   * them from the judgers.
   */
  @Autowired private ApplicationEventPublisher eventPublisher;

  /** The logger. */
  private static final Logger LOGGER = LogManager.getLogger(MessageReceiver.class);
}
