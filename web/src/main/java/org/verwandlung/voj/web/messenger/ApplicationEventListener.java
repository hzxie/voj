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
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import org.verwandlung.voj.web.service.SubmissionService;

/**
 * The application event listener. Forwards the progress of the judgings, which every instance
 * receives on the topic of the progress (see {@link MessageReceiver}), to the live streams of the
 * submissions on this instance.
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
    Set<SseEmitter> emitters = sseEmitters.get(submissionId);

    if (emitters == null || emitters.isEmpty()) {
      // Every instance receives the progress of every judging; most have no viewer of it.
      LOGGER.debug(String.format("No viewer of submission #%d on this instance.", submissionId));
      return;
    }
    // A judging that was superseded (by a rejudge, or by a later claim once its lease expired) may
    // still be running and sending messages: they would show the old judging on the page of the new
    // one. So a message whose claim token the submission
    // does not carry is dropped (the streams stay open for the current judging's messages). An
    // event without a claim token is an outcome the web application recorded itself.
    Long claimToken = event.getClaimToken();
    if (claimToken != null && !submissionService.isCurrentJudging(submissionId, claimToken)) {
      LOGGER.info(
          String.format(
              "Dropped a message of a superseded judging of submission #%d.", submissionId));
      return;
    }
    if (isCompleted) {
      emitters = sseEmitters.remove(submissionId);
      if (emitters == null) {
        return;
      }
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
  public void removeSseEmitter(long submissionId, SseEmitter sseEmitter) {
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
  public int getNumberOfSseEmitters(long submissionId) {
    Set<SseEmitter> emitters = sseEmitters.get(submissionId);
    return emitters == null ? 0 : emitters.size();
  }

  /**
   * The registered SseEmitter objects. The Key in the Map represents the unique identifier of the
   * submission record. The Value in the Map is the set of SseEmitter objects of every viewer
   * following that submission, used to push real-time judge information.
   */
  private final Map<Long, Set<SseEmitter>> sseEmitters = new ConcurrentHashMap<>();

  /**
   * The autowired SubmissionService object. Used to tell the current judging of a submission from
   * a superseded one.
   */
  @Autowired private SubmissionService submissionService;

  /** The logger. */
  private static final Logger LOGGER = LogManager.getLogger(ApplicationEventListener.class);
}
