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
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.io.IOException;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

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

  /** The ApplicationEventListener under test. */
  private final ApplicationEventListener listener = new ApplicationEventListener();
}
