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
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

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
  /**
   * The handler for submission events. The message is pushed to every viewer subscribed to the
   * submission (e.g. its owner on the submission page and an administrator on the edit-submission
   * page); a viewer whose stream fails is dropped without affecting the others. Once judging has
   * completed every stream of the submission is completed and the submission is unregistered.
   *
   * @param event - the submission record event
   */
  @EventListener
  public void submissionEventHandler(SubmissionEvent event) {
    long submissionId = event.getSubmissionId();
    boolean isCompleted = event.isCompleted();
    Set<SseEmitter> emitters =
        isCompleted ? sseEmitters.remove(submissionId) : sseEmitters.get(submissionId);

    if (emitters == null || emitters.isEmpty()) {
      LOGGER.warn(String.format("CANNOT get the SseEmitter for submission #%d.", submissionId));
      return;
    }
    Map<String, String> mapMessage = new HashMap<>(3, 1);
    mapMessage.put("judgeResult", event.getJudgeResult());
    mapMessage.put("message", event.getMessage());

    // The set is concurrent, so iterating it while a callback (or another event) removes an
    // emitter never throws a ConcurrentModificationException.
    for (SseEmitter sseEmitter : emitters) {
      try {
        sseEmitter.send(mapMessage);
      } catch (IOException | IllegalStateException ex) {
        // The client closed the EventSource (or the stream is already complete): drop the dead
        // emitter instead of letting the exception propagate. Re-throwing here would surface
        // through Spring's async error handling and, because the SSE response is already
        // committed, fail again while rendering the HTML error view.
        LOGGER.warn(
            String.format(
                "Dropping SseEmitter for submission #%d after a failed send.", submissionId),
            ex);
        removeSseEmitter(submissionId, sseEmitter);
        completeQuietly(sseEmitter, ex);
        continue;
      }
      if (isCompleted) {
        completeQuietly(sseEmitter, null);
      }
    }
  }

  /**
   * Registers a Server-Sent Event emitter object. Several emitters may be registered for the same
   * submission; each of them receives every judge message of that submission.
   *
   * @param submissionId - the unique identifier of the submission record
   * @param sseEmitter - the Server-Sent Event emitter object
   */
  public void addSseEmitters(long submissionId, SseEmitter sseEmitter) {
    // Evict the emitter from the registry once the stream ends for any reason (normal
    // completion, container timeout or a client disconnect). Without this a closed
    // connection would linger in the map and keep failing on every future send().
    sseEmitter.onCompletion(() -> removeSseEmitter(submissionId, sseEmitter));
    sseEmitter.onTimeout(() -> removeSseEmitter(submissionId, sseEmitter));
    sseEmitter.onError(throwable -> removeSseEmitter(submissionId, sseEmitter));
    sseEmitters.compute(
        submissionId,
        (id, emitters) -> {
          Set<SseEmitter> registeredEmitters =
              emitters != null ? emitters : ConcurrentHashMap.newKeySet();
          registeredEmitters.add(sseEmitter);
          return registeredEmitters;
        });
  }

  /**
   * Unregisters one Server-Sent Event emitter of a submission, and the submission itself once it
   * has no emitter left. Emitters of other submissions are never touched: several judgers run in
   * parallel, so submissions do not complete in the order of their identifiers.
   *
   * @param submissionId - the unique identifier of the submission record
   * @param sseEmitter - the Server-Sent Event emitter object to unregister
   */
  private void removeSseEmitter(long submissionId, SseEmitter sseEmitter) {
    sseEmitters.computeIfPresent(
        submissionId,
        (id, emitters) -> {
          emitters.remove(sseEmitter);
          return emitters.isEmpty() ? null : emitters;
        });
  }

  /**
   * Completes a Server-Sent Event emitter, ignoring the failure of an emitter whose stream has
   * already been closed.
   *
   * @param sseEmitter - the Server-Sent Event emitter object
   * @param error - the error to complete the stream with, or null to complete it normally
   */
  private static void completeQuietly(SseEmitter sseEmitter, Throwable error) {
    try {
      if (error == null) {
        sseEmitter.complete();
      } else {
        sseEmitter.completeWithError(error);
      }
    } catch (RuntimeException ex) {
      LOGGER.debug("Ignoring the failure to complete an SseEmitter.", ex);
    }
  }

  /**
   * Gets the number of Server-Sent Event emitters registered for a submission (used by tests).
   *
   * @param submissionId - the unique identifier of the submission record
   * @return the number of registered emitters
   */
  int getNumberOfSseEmitters(long submissionId) {
    Set<SseEmitter> emitters = sseEmitters.get(submissionId);
    return emitters == null ? 0 : emitters.size();
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
    judgerInformation.put("cpuLoad", event.getCpuLoad());
    judgerInformation.put("memoryUsage", event.getMemoryUsage());
    judgerInformation.put("uptime", event.getUptime());

    onlineJudgers.put(judgerUsername, judgerInformation);
  }

  /**
   * Gets the live information (description + telemetry) of an online judger.
   *
   * @param judgerUsername - the username of the judger
   * @return the judger's information map, or null when the judger is offline
   */
  public Map<String, Object> getJudgerInformation(String judgerUsername) {
    Map<String, Object> judgerInformation = onlineJudgers.get(judgerUsername);
    return isOnline(judgerInformation) ? judgerInformation : null;
  }

  /**
   * Gets the number of online judgers.
   *
   * @return the number of online judgers
   */
  public long getOnlineJudgers() {
    long onlineJudgersCount = 0;

    for (Map<String, Object> judgerInformation : onlineJudgers.values()) {
      if (isOnline(judgerInformation)) {
        ++onlineJudgersCount;
      }
    }
    return onlineJudgersCount;
  }

  /**
   * Gets a snapshot of the currently online judgers and their live telemetry (description, CPU load,
   * memory usage, uptime), used by the administration dashboard and the judgers page. Each entry is a
   * copy that also carries the judger's {@code username}.
   *
   * @return a list of telemetry maps, one per online judger
   */
  public List<Map<String, Object>> getOnlineJudgerList() {
    List<Map<String, Object>> judgers = new ArrayList<>();
    for (Entry<String, Map<String, Object>> entry : onlineJudgers.entrySet()) {
      if (isOnline(entry.getValue())) {
        Map<String, Object> telemetry = new HashMap<>(entry.getValue());
        telemetry.put("username", entry.getKey());
        judgers.add(telemetry);
      }
    }
    return judgers;
  }

  /**
   * Gets the average CPU load across the currently online judgers, used by the dashboard's system
   * status. CPU load is reported as a percentage in {@code [0, 100]} by each judger's heartbeat.
   *
   * @return the average CPU load (0..100), or {@code -1} when no judger is online or none reports it
   */
  public double getAverageCpuLoad() {
    double total = 0;
    int counted = 0;
    for (Map<String, Object> judgerInformation : onlineJudgers.values()) {
      if (!isOnline(judgerInformation)) {
        continue;
      }
      Object cpuLoad = judgerInformation.get("cpuLoad");
      if (cpuLoad instanceof Number) {
        total += ((Number) cpuLoad).doubleValue();
        ++counted;
      }
    }
    return counted == 0 ? -1 : total / counted;
  }

  /**
   * Checks whether a judger is currently online, i.e. whether its most recent heartbeat is recent
   * enough that the judger is still considered alive.
   *
   * @param judgerInformation - the judger's information map, or null when never seen
   * @return whether the judger is currently online
   */
  private boolean isOnline(Map<String, Object> judgerInformation) {
    if (judgerInformation == null) {
      return false;
    }
    Date lastHeartbeatTime = (Date) judgerInformation.get("heartbeatTime");
    return lastHeartbeatTime != null
        && System.currentTimeMillis() - lastHeartbeatTime.getTime() <= ONLINE_TIMEOUT_MILLIS;
  }

  /**
   * The registered SseEmitter objects. The Key in the Map represents the unique identifier of the
   * submission record. The Value in the Map is the set of SseEmitter objects of every viewer
   * following that submission, used to push real-time judge information.
   */
  private static final Map<Long, Set<SseEmitter>> sseEmitters = new ConcurrentHashMap<>();

  /**
   * The list of online judgers. The Key in the Map represents the username of the judger. The Value
   * in the Map represents the information of the corresponding judger. A judger that stopped
   * sending heartbeats is treated as offline when the map is read (see isOnline); its entry stays,
   * which is bounded by the number of judger accounts.
   */
  private static Map<String, Map<String, Object>> onlineJudgers =
      new Hashtable<String, Map<String, Object>>();

  /**
   * The maximum age (in milliseconds) of a judger's last heartbeat for it to still be considered
   * online. A judger that misses roughly three 30-second heartbeats is treated as offline.
   */
  private static final long ONLINE_TIMEOUT_MILLIS = 90_000L;

  /** The logger. */
  private static final Logger LOGGER = LogManager.getLogger(ApplicationEventListener.class);
}
