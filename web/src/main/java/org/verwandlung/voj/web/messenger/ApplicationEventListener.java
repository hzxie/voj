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
import java.util.concurrent.ConcurrentHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import org.verwandlung.voj.web.service.SubmissionService;

/**
 * The application event listener. Forwards the progress of the judgings, which every instance
 * receives on the topic of the progress (see {@link MessageReceiver}), to the live streams of the
 * submissions on this instance.
 *
 * <p>A viewer has at most {@value #MAX_STREAMS_PER_USER} streams open on an instance, and a
 * submission at most {@value #MAX_STREAMS_PER_SUBMISSION}. Every stream gets a comment every
 * {@value #HEARTBEAT_INTERVAL_MS} ms, so that one whose browser went away is dropped once a send
 * to it fails, rather than when it times out.
 *
 * @author Haozhe Xie
 */
@Component
public class ApplicationEventListener {
  /**
   * The handler for submission events. The message is pushed to every viewer subscribed to the
   * submission (e.g. its owner on the submission page and an administrator on the edit-submission
   * page); a viewer whose stream fails is dropped without affecting the others. Once judging has
   * completed every stream of the submission is completed and unregistered.
   *
   * @param event - the submission record event
   */
  @EventListener
  public void submissionEventHandler(SubmissionEvent event) {
    long submissionId = event.getSubmissionId();
    if (getNumberOfSseEmitters(submissionId) == 0) {
      // Every instance receives the progress of every judging; most have no viewer of it.
      LOGGER.debug(String.format("No viewer of submission #%d on this instance.", submissionId));
      return;
    }
    // A judging that was superseded (by a rejudge, or by a later claim once its lease expired) may
    // still be running and sending messages: they would show the old judging on the page of the new
    // one. So a message whose claim token the submission does not carry is dropped (the streams
    // stay open for the current judging's messages).
    if (!submissionService.isCurrentJudging(submissionId, event.getClaimToken())) {
      LOGGER.info(
          String.format(
              "Dropped a message of a superseded judging of submission #%d.", submissionId));
      return;
    }
    Map<String, String> mapMessage = new HashMap<>(3, 1);
    mapMessage.put("judgeResult", event.getJudgeResult());
    mapMessage.put("message", event.getMessage());

    // The map is concurrent, so iterating it while a callback (or another event) removes a stream
    // never throws a ConcurrentModificationException.
    for (Map.Entry<SseEmitter, Stream> entry : streams.entrySet()) {
      SseEmitter sseEmitter = entry.getKey();
      if (entry.getValue().submissionId() != submissionId) {
        continue;
      }
      // The verdict ends a stream: it is sent by the one event that unregistered the stream.
      if (event.isCompleted() && streams.remove(sseEmitter) == null) {
        continue;
      }
      try {
        sseEmitter.send(mapMessage);
      } catch (IOException | IllegalStateException ex) {
        dropStream(sseEmitter, ex);
        continue;
      }
      if (event.isCompleted()) {
        completeQuietly(sseEmitter, null);
      }
    }
  }

  /**
   * Sends a comment to every stream, which the pages ignore: a stream whose browser went away is
   * dropped once the send fails.
   */
  @Scheduled(fixedDelay = HEARTBEAT_INTERVAL_MS)
  public void sendHeartbeats() {
    for (SseEmitter sseEmitter : streams.keySet()) {
      try {
        sseEmitter.send(SseEmitter.event().comment("heartbeat"));
      } catch (IOException | IllegalStateException ex) {
        dropStream(sseEmitter, ex);
      }
    }
  }

  /**
   * Registers a Server-Sent Event emitter object, unless its viewer or its submission has as many
   * streams open as it may. Several emitters may be registered for the same submission; each of
   * them receives every judge message of that submission.
   *
   * @param submissionId - the unique identifier of the submission record
   * @param uid - the unique identifier of the viewer
   * @param sseEmitter - the Server-Sent Event emitter object
   * @return whether the emitter was registered
   */
  public boolean addSseEmitter(long submissionId, long uid, SseEmitter sseEmitter) {
    synchronized (streams) {
      long streamsOfSubmission =
          streams.values().stream().filter(s -> s.submissionId() == submissionId).count();
      long streamsOfUser = streams.values().stream().filter(s -> s.uid() == uid).count();
      if (streamsOfSubmission >= MAX_STREAMS_PER_SUBMISSION
          || streamsOfUser >= MAX_STREAMS_PER_USER) {
        return false;
      }
      streams.put(sseEmitter, new Stream(submissionId, uid));
    }
    // Evict the emitter from the registry once the stream ends for any reason (normal
    // completion, container timeout or a client disconnect). Without this a closed
    // connection would linger in the map and keep failing on every future send().
    sseEmitter.onCompletion(() -> removeSseEmitter(sseEmitter));
    // A stream that lasted its time ends like any other: completed, the response ends normally and
    // the page opens another stream if the submission is still pending. Left alone, it would end
    // with an AsyncRequestTimeoutException, which the error handling logs.
    sseEmitter.onTimeout(
        () -> {
          removeSseEmitter(sseEmitter);
          completeQuietly(sseEmitter, null);
        });
    sseEmitter.onError(throwable -> removeSseEmitter(sseEmitter));
    return true;
  }

  /**
   * Unregisters a Server-Sent Event emitter.
   *
   * @param sseEmitter - the Server-Sent Event emitter object to unregister
   */
  public void removeSseEmitter(SseEmitter sseEmitter) {
    streams.remove(sseEmitter);
  }

  /**
   * Unregisters a stream a send to which failed (its browser went away, or it is already complete)
   * and completes it with the failure.
   *
   * @param sseEmitter - the Server-Sent Event emitter object
   * @param ex - the failure of the send
   */
  private void dropStream(SseEmitter sseEmitter, Exception ex) {
    LOGGER.debug("Dropping an SseEmitter after a failed send.", ex);
    removeSseEmitter(sseEmitter);
    completeQuietly(sseEmitter, ex);
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
   * Gets the number of Server-Sent Event emitters registered for a submission.
   *
   * @param submissionId - the unique identifier of the submission record
   * @return the number of registered emitters
   */
  public int getNumberOfSseEmitters(long submissionId) {
    return (int) streams.values().stream().filter(s -> s.submissionId() == submissionId).count();
  }

  /**
   * A registered stream: the submission it follows and its viewer.
   *
   * @param submissionId - the unique identifier of the submission
   * @param uid - the unique identifier of the viewer
   */
  private record Stream(long submissionId, long uid) {}

  /** How many streams a viewer may have open at once (on this instance). */
  public static final int MAX_STREAMS_PER_USER = 10;

  /** How many streams of a submission may be open at once (on this instance). */
  public static final int MAX_STREAMS_PER_SUBMISSION = 20;

  /** How often (ms) every stream gets a comment, which finds the streams whose browser went away. */
  static final long HEARTBEAT_INTERVAL_MS = 30000;

  /**
   * The registered streams: the SseEmitter object of every viewer following a submission, used to
   * push its real-time judge information. Registrations are made under its lock, so that the
   * limits hold.
   */
  private final Map<SseEmitter, Stream> streams = new ConcurrentHashMap<>();

  /**
   * The autowired SubmissionService object. Used to tell the current judging of a submission from
   * a superseded one.
   */
  @Autowired private SubmissionService submissionService;

  /** The logger. */
  private static final Logger LOGGER = LogManager.getLogger(ApplicationEventListener.class);
}
