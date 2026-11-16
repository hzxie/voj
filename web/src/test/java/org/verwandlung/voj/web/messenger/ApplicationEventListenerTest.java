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
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import org.verwandlung.voj.web.service.SubmissionService;

/**
 * The test class for the Server-Sent Event handling of ApplicationEventListener. The emitter
 * registry is static, so every test uses its own submission ids.
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

    listener.submissionEventHandler(new SubmissionEvent(this, 9101L, "Running", "#1 AC", false));
    verify(owner).send(any(Object.class));
    verify(administrator).send(any(Object.class));
    verify(owner, never()).complete();

    listener.submissionEventHandler(new SubmissionEvent(this, 9101L, "AC", "Accepted", true));
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

    listener.submissionEventHandler(new SubmissionEvent(this, 9202L, "AC", "Accepted", true));

    Assertions.assertEquals(0, listener.getNumberOfSseEmitters(9202L));
    Assertions.assertEquals(1, listener.getNumberOfSseEmitters(9201L));
    verify(older, never()).complete();

    listener.submissionEventHandler(new SubmissionEvent(this, 9201L, "AC", "Accepted", true));
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
                new SubmissionEvent(this, 9301L, "Running", "#1 AC", false)));

    verify(healthy).send(any(Object.class));
    verify(closed).completeWithError(any(IOException.class));
    Assertions.assertEquals(1, listener.getNumberOfSseEmitters(9301L));

    listener.submissionEventHandler(new SubmissionEvent(this, 9301L, "AC", "Accepted", true));
    Assertions.assertEquals(0, listener.getNumberOfSseEmitters(9301L));
  }

  /** Test case: tests submissionEventHandler(...). Test data: an event for a submission nobody follows. Expected: it is ignored. */
  @Test
  public void testEventWithoutViewersIsIgnored() {
    Assertions.assertDoesNotThrow(
        () ->
            listener.submissionEventHandler(
                new SubmissionEvent(this, 9401L, "AC", "Accepted", true)));
    Assertions.assertEquals(0, listener.getNumberOfSseEmitters(9401L));
  }

  /**
   * Test case: tests submissionEventHandler(...). Test data: a viewer of a submission that was
   * rejudged while its old judging still runs; messages of the old judging (claim 1000), including
   * its verdict, then of the new one (claim 2000). Expected: the old judging's messages are dropped
   * and leave the stream open; the new judging's messages are sent and its verdict completes it.
   */
  @Test
  public void testMessagesOfASupersededJudgingAreDropped() throws IOException {
    SubmissionService submissionService = mock(SubmissionService.class);
    when(submissionService.isCurrentJudging(9501L, 1000L, null)).thenReturn(false);
    when(submissionService.isCurrentJudging(9501L, 2000L, null)).thenReturn(true);
    ReflectionTestUtils.setField(listener, "submissionService", submissionService);
    SseEmitter viewer = mock(SseEmitter.class);
    listener.addSseEmitters(9501L, viewer);

    listener.submissionEventHandler(
        new SubmissionEvent(this, 9501L, "Running", "#1 WA", false, 1000L, null));
    listener.submissionEventHandler(
        new SubmissionEvent(this, 9501L, "Wrong Answer", "Wrong Answer", true, 1000L, null));
    verify(viewer, never()).send(any(Object.class));
    verify(viewer, never()).complete();
    Assertions.assertEquals(1, listener.getNumberOfSseEmitters(9501L));

    listener.submissionEventHandler(
        new SubmissionEvent(this, 9501L, "Running", "#1 AC", false, 2000L, null));
    listener.submissionEventHandler(
        new SubmissionEvent(this, 9501L, "Accepted", "Accepted", true, 2000L, null));
    verify(viewer, times(2)).send(any(Object.class));
    verify(viewer).complete();
    Assertions.assertEquals(0, listener.getNumberOfSseEmitters(9501L));
  }

  /**
   * Test case: tests submissionEventHandler(...). Test data: a viewer of a submission rejudged to
   * judge generation 2; a message of the judging of generation 1, then one of generation 2, both
   * with the same claim (two judgings that claimed the submission in the same second cannot be told
   * apart by their claim). Expected: the message of the stale generation is dropped, the current
   * one is sent.
   */
  @Test
  public void testMessagesOfAStaleJudgeGenerationAreDropped() throws IOException {
    SubmissionService submissionService = mock(SubmissionService.class);
    when(submissionService.isCurrentJudging(9601L, 1000L, 1L)).thenReturn(false);
    when(submissionService.isCurrentJudging(9601L, 1000L, 2L)).thenReturn(true);
    ReflectionTestUtils.setField(listener, "submissionService", submissionService);
    SseEmitter viewer = mock(SseEmitter.class);
    listener.addSseEmitters(9601L, viewer);

    listener.submissionEventHandler(
        new SubmissionEvent(this, 9601L, "Wrong Answer", "Wrong Answer", true, 1000L, 1L));
    verify(viewer, never()).send(any(Object.class));
    Assertions.assertEquals(1, listener.getNumberOfSseEmitters(9601L));

    listener.submissionEventHandler(
        new SubmissionEvent(this, 9601L, "Accepted", "Accepted", true, 1000L, 2L));
    verify(viewer).send(any(Object.class));
    verify(viewer).complete();
  }

  /**
   * Test case: tests submissionEventHandler(...). Test data: a message that names neither the
   * claim nor the judge generation of its judging (an older judger). Expected: it is sent without
   * asking the database whether its judging is current.
   */
  @Test
  public void testMessagesWithoutJudgingAreNotChecked() throws IOException {
    SubmissionService submissionService = mock(SubmissionService.class);
    ReflectionTestUtils.setField(listener, "submissionService", submissionService);
    SseEmitter viewer = mock(SseEmitter.class);
    listener.addSseEmitters(9701L, viewer);

    listener.submissionEventHandler(new SubmissionEvent(this, 9701L, "Running", "#1 AC", false));

    verify(viewer).send(any(Object.class));
    verify(submissionService, never()).isCurrentJudging(anyLong(), any(), any());
  }

  /** The ApplicationEventListener under test. */
  private final ApplicationEventListener listener = new ApplicationEventListener();
}
