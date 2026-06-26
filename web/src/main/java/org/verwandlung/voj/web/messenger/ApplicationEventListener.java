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

import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * The application event listener. Responsible for forwarding the messages in the message queue to
 * the controllers.
 *
 * @author Haozhe Xie
 */
@Component
public class ApplicationEventListener {
  /** The constructor of ApplicationEventListener. */
  public ApplicationEventListener() {
    synchronized (this) {
      if (scheduler == null) {
        final int INITIAL_DELAY = 0;
        final int PERIOD = 30;

        scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(
            new Runnable() {
              @Override
              public void run() {
                Calendar calendar = Calendar.getInstance();
                calendar.add(Calendar.MINUTE, -PERIOD);
                Date heartbeatTimeDeadline = calendar.getTime();

                for (Iterator<Entry<String, Map<String, Object>>> itr =
                        onlineJudgers.entrySet().iterator();
                    itr.hasNext(); ) {
                  Entry<String, Map<String, Object>> entry = itr.next();
                  Date lastHeartbeatTime = (Date) entry.getValue().get("heartbeatTime");

                  if (!lastHeartbeatTime.after(heartbeatTimeDeadline)) {
                    itr.remove();
                  }
                }
              }
            },
            INITIAL_DELAY,
            PERIOD,
            TimeUnit.MINUTES);
      }
    }
  }

  /**
   * The handler for submission events.
   *
   * @param event - the submission record event
   * @throws IOException
   */
  @EventListener
  public void submissionEventHandler(SubmissionEvent event) throws IOException {
    long submissionId = event.getSubmissionId();
    String judgeResult = event.getJudgeResult();
    String message = event.getMessage();
    boolean isCompleted = event.isCompleted();
    SseEmitter sseEmitter = sseEmitters.get(submissionId);

    if (sseEmitter == null) {
      LOGGER.warn(String.format("CANNOT get the SseEmitter for submission #%d.", submissionId));
      return;
    }
    Map<String, String> mapMessage = new HashMap<>(3, 1);
    mapMessage.put("judgeResult", judgeResult);
    mapMessage.put("message", message);

    try {
      sseEmitter.send(mapMessage);
    } catch (IOException | IllegalStateException ex) {
      // The client closed the EventSource (or the stream is already complete): drop the dead
      // emitter instead of letting the IOException propagate. Re-throwing here would surface
      // through Spring's async error handling and, because the SSE response is already
      // committed, fail again while rendering the HTML error view.
      LOGGER.warn(
          String.format("Dropping SseEmitter for submission #%d after a failed send.", submissionId),
          ex);
      sseEmitter.completeWithError(ex);
      removeSseEmitters(submissionId);
      return;
    }

    if (isCompleted) {
      sseEmitter.complete();
      removeSseEmitters(submissionId);
    }
  }

  /**
   * Registers a Server-Sent Event emitter object.
   *
   * @param submissionId - the unique identifier of the submission record
   * @param sseEmitter - the Server-Sent Event emitter object
   */
  public void addSseEmitters(long submissionId, SseEmitter sseEmitter) {
    // Evict the emitter from the registry once the stream ends for any reason (normal
    // completion, container timeout or a client disconnect). Without this a closed
    // connection would linger in the map and keep failing on every future send().
    sseEmitter.onCompletion(() -> sseEmitters.remove(submissionId, sseEmitter));
    sseEmitter.onTimeout(() -> sseEmitters.remove(submissionId, sseEmitter));
    sseEmitter.onError(throwable -> sseEmitters.remove(submissionId, sseEmitter));
    sseEmitters.put(submissionId, sseEmitter);
  }

  /**
   * Removes a Server-Sent Event emitter object.
   *
   * @param submissionId - the unique identifier of the submission record
   */
  private void removeSseEmitters(long submissionId) {
    sseEmitters.remove(submissionId);

    for (Entry<Long, SseEmitter> mapEntry : sseEmitters.entrySet()) {
      long currentSubmissionId = mapEntry.getKey();
      if (currentSubmissionId < submissionId) {
        sseEmitters.remove(currentSubmissionId);
      }
    }
  }

  /**
   * Handles judger heartbeat events.
   *
   * @param event - the judger heartbeat event
   */
  @EventListener
  public void keepAliveEventHandler(KeepAliveEvent event) {
    String judgerUsername = event.getJudgerUsername();
    String judgerDescription = event.getJudgerDescription();
    Date heartbeatTime = event.getHeartbeatTime();

    Map<String, Object> judgerInformation = new HashMap<>();
    judgerInformation.put("description", judgerDescription);
    judgerInformation.put("heartbeatTime", heartbeatTime);

    onlineJudgers.put(judgerUsername, judgerInformation);
  }

  /**
   * Gets the description information of a judger.
   *
   * @param judgerUsername - the username of the judger
   * @return the description information of the judger
   */
  public String getJudgerDescription(String judgerUsername) {
    String judgerDescription = "[Offline]";

    if (onlineJudgers.containsKey(judgerUsername)) {
      String description = (String) onlineJudgers.get(judgerUsername).get("description");
      judgerDescription = "[Online] " + description;
    }
    return judgerDescription;
  }

  /**
   * Gets the number of online judgers.
   *
   * @return the number of online judgers
   */
  public long getOnlineJudgers() {
    return onlineJudgers.size();
  }

  /**
   * The list of SseEmitter objects. The Key in the Map represents the unique identifier of the
   * submission record. The Value in the Map represents the corresponding SseEmitter object, used to
   * push real-time judge information.
   */
  private static Map<Long, SseEmitter> sseEmitters = new Hashtable<>();

  /**
   * The list of online judgers. The Key in the Map represents the username of the judger. The Value
   * in the Map represents the information of the corresponding judger.
   */
  private static Map<String, Map<String, Object>> onlineJudgers =
      new Hashtable<String, Map<String, Object>>();

  /** The ScheduledExecutorService object. Used to periodically remove offline judgers. */
  private static ScheduledExecutorService scheduler = null;

  /** The logger. */
  private static final Logger LOGGER = LogManager.getLogger(ApplicationEventListener.class);
}
