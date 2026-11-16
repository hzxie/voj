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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import org.verwandlung.voj.web.service.SubmissionService;

/**
 * The test class for the Server-Sent Event handling of ApplicationEventListener.
 *
 * @author Haozhe Xie
 */
public class ApplicationEventListenerTest {
  /** Test case: tests submissionEventHandler(...). Test data: two viewers of the same submission. Expected: both receive every message, and both streams are completed and unregistered when judging completes. */
  @Test
  public void testEveryViewerReceivesMessages() throws IOException {
    SseEmitter owner = mock(SseEmitter.class);
    SseEmitter administrator = mock(SseEmitter.class);
    listener.addSseEmitters(9101L, owner);
    listener.addSseEmitters(9101L, administrator);
    Assertions.assertEquals(2, listener.getNumberOfSseEmitters(9101L));

    listener.submissionEventHandler(
        new SubmissionEvent(this, 9101L, CLAIM_TOKEN, "Running", "#1 AC", false));
    verify(owner).send(any(Object.class));
    verify(administrator).send(any(Object.class));
    verify(owner, never()).complete();

    listener.submissionEventHandler(
        new SubmissionEvent(this, 9101L, CLAIM_TOKEN, "AC", "Accepted", true));
    verify(owner, times(2)).send(any(Object.class));
    verify(administrator, times(2)).send(any(Object.class));
    verify(owner).complete();
    verify(administrator).complete();
    Assertions.assertEquals(0, listener.getNumberOfSseEmitters(9101L));
  }

  /** Test case: tests submissionEventHandler(...). Test data: a completed submission with a larger id than another pending one (judged out of order by parallel judgers). Expected: only the completed submission's emitters are removed. */
  @Test
  public void testCompletionOnlyRemovesItsOwnEmitters() {
    SseEmitter older = mock(SseEmitter.class);
    SseEmitter newer = mock(SseEmitter.class);
    listener.addSseEmitters(9201L, older);
    listener.addSseEmitters(9202L, newer);

    listener.submissionEventHandler(
        new SubmissionEvent(this, 9202L, CLAIM_TOKEN, "AC", "Accepted", true));

    Assertions.assertEquals(0, listener.getNumberOfSseEmitters(9202L));
    Assertions.assertEquals(1, listener.getNumberOfSseEmitters(9201L));
    verify(older, never()).complete();

    listener.submissionEventHandler(
        new SubmissionEvent(this, 9201L, CLAIM_TOKEN, "AC", "Accepted", true));
    Assertions.assertEquals(0, listener.getNumberOfSseEmitters(9201L));
  }

  /** Test case: tests submissionEventHandler(...). Test data: one viewer whose connection is closed and one healthy viewer. Expected: the dead emitter is dropped without an exception and the healthy viewer still receives the message. */
  @Test
  public void testFailedSendOnlyDropsThatEmitter() throws IOException {
    SseEmitter closed = mock(SseEmitter.class);
    SseEmitter healthy = mock(SseEmitter.class);
    doThrow(new IOException("Broken pipe")).when(closed).send(any(Object.class));
    listener.addSseEmitters(9301L, closed);
    listener.addSseEmitters(9301L, healthy);

    Assertions.assertDoesNotThrow(
        () ->
            listener.submissionEventHandler(
                new SubmissionEvent(this, 9301L, CLAIM_TOKEN, "Running", "#1 AC", false)));

    verify(healthy).send(any(Object.class));
    verify(closed).completeWithError(any(IOException.class));
    Assertions.assertEquals(1, listener.getNumberOfSseEmitters(9301L));

    listener.submissionEventHandler(
        new SubmissionEvent(this, 9301L, CLAIM_TOKEN, "AC", "Accepted", true));
    Assertions.assertEquals(0, listener.getNumberOfSseEmitters(9301L));
  }

  /** Test case: tests submissionEventHandler(...). Test data: an event for a submission nobody follows. Expected: it is ignored. */
  @Test
  public void testEventWithoutViewersIsIgnored() {
    Assertions.assertDoesNotThrow(
        () ->
            listener.submissionEventHandler(
                new SubmissionEvent(this, 9401L, CLAIM_TOKEN, "AC", "Accepted", true)));
    Assertions.assertEquals(0, listener.getNumberOfSseEmitters(9401L));
  }

  /**
   * Test case: tests submissionEventHandler(...). Test data: a viewer of a submission that was
   * rejudged while its old judging still runs; messages of the old judging (claim token 1000),
   * including its verdict, then of the new one (claim token 2000). Expected: the old judging's
   * messages are dropped and leave the stream open; the new judging's messages are sent and its
   * verdict completes it.
   */
  @Test
  public void testMessagesOfASupersededJudgingAreDropped() throws IOException {
    when(submissionService.isCurrentJudging(9501L, 1000L)).thenReturn(false);
    when(submissionService.isCurrentJudging(9501L, 2000L)).thenReturn(true);
    SseEmitter viewer = mock(SseEmitter.class);
    listener.addSseEmitters(9501L, viewer);

    listener.submissionEventHandler(
        new SubmissionEvent(this, 9501L, 1000L, "Running", "#1 WA", false));
    listener.submissionEventHandler(
        new SubmissionEvent(this, 9501L, 1000L, "Wrong Answer", "Wrong Answer", true));
    verify(viewer, never()).send(any(Object.class));
    verify(viewer, never()).complete();
    Assertions.assertEquals(1, listener.getNumberOfSseEmitters(9501L));

    listener.submissionEventHandler(
        new SubmissionEvent(this, 9501L, 2000L, "Running", "#1 AC", false));
    listener.submissionEventHandler(
        new SubmissionEvent(this, 9501L, 2000L, "Accepted", "Accepted", true));
    verify(viewer, times(2)).send(any(Object.class));
    verify(viewer).complete();
    Assertions.assertEquals(0, listener.getNumberOfSseEmitters(9501L));
  }

  /**
   * Test case: tests addSseEmitters(...). Test data: a stream that lasted its time. Expected: it is
   * unregistered and completed, so that its response ends normally rather than with an
   * AsyncRequestTimeoutException.
   */
  @Test
  public void testAStreamThatTimesOutIsCompleted() {
    SseEmitter viewer = mock(SseEmitter.class);
    listener.addSseEmitters(9601L, viewer);
    ArgumentCaptor<Runnable> onTimeout = ArgumentCaptor.forClass(Runnable.class);
    verify(viewer).onTimeout(onTimeout.capture());

    onTimeout.getValue().run();

    verify(viewer).complete();
    Assertions.assertEquals(0, listener.getNumberOfSseEmitters(9601L));
  }

  /** Creates the listener under test, whose service says that every judging is current. */
  @BeforeEach
  public void setUp() {
    submissionService = mock(SubmissionService.class);
    when(submissionService.isCurrentJudging(anyLong(), anyLong())).thenReturn(true);
    listener = new ApplicationEventListener();
    ReflectionTestUtils.setField(listener, "submissionService", submissionService);
  }

  /** The claim token of the judgings of the tests. */
  private static final long CLAIM_TOKEN = 42L;

  /** The mocked SubmissionService, which tells the current judging of a submission. */
  private SubmissionService submissionService;

  /** The ApplicationEventListener under test. */
  private ApplicationEventListener listener;
}
