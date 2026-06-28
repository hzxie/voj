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
package org.verwandlung.voj.web.controller;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import org.verwandlung.voj.web.model.DiscussionThread;
import org.verwandlung.voj.web.model.User;
import org.verwandlung.voj.web.model.UserGroup;
import org.verwandlung.voj.web.model.VojUserDetails;
import org.verwandlung.voj.web.service.DiscussionService;
import org.verwandlung.voj.web.service.OptionService;

/**
 * The test class for DiscussionController.
 *
 * <p>Covers the controller-specific logic: the problem-vs-topic branch and the negative-start-index
 * clamp of {@code getDiscussionThreadsAction}, the current-user pass-through of the vote endpoint,
 * and the {@code parseRelatedProblemId} parsing rules of {@code createDiscussionThreadAction}.
 *
 * @author Haozhe Xie
 */
public class DiscussionControllerTest {
  /** Wires mocked collaborators into a fresh controller before every test. */
  @BeforeEach
  public void setUp() {
    discussionService = mock(DiscussionService.class);
    optionService = mock(OptionService.class);
    when(optionService.getIntOption(anyString(), anyInt())).thenReturn(50);

    controller = new DiscussionController();
    ReflectionTestUtils.setField(controller, "discussionService", discussionService);
    ReflectionTestUtils.setField(controller, "optionService", optionService);
  }

  /** Clears the security context after every test. */
  @AfterEach
  public void tearDown() {
    SecurityContextHolder.clearContext();
  }

  /** Test case: tests getDiscussionThreadsAction(...). Test data: a problem id (not -1). Expected: the threads are fetched by problem, not by topic. */
  @Test
  public void testGetDiscussionThreadsByProblem() {
    List<DiscussionThread> threads = List.of(new DiscussionThread());
    when(discussionService.getDiscussionThreadsOfProblem(eq(1000L), eq(0L), eq(50)))
        .thenReturn(threads);

    Map<String, Object> result =
        controller.getDiscussionThreadsAction(0L, "", 1000L, new MockHttpServletRequest());

    Assertions.assertEquals(Boolean.TRUE, result.get("isSuccessful"));
    Assertions.assertSame(threads, result.get("discussionThreads"));
    verify(discussionService, never())
        .getDiscussionThreadsOfTopic(anyString(), anyLong(), anyInt());
  }

  /** Test case: tests getDiscussionThreadsAction(...). Test data: no problem id (-1). Expected: the threads are fetched by topic, not by problem. */
  @Test
  public void testGetDiscussionThreadsByTopic() {
    List<DiscussionThread> threads = List.of(new DiscussionThread());
    when(discussionService.getDiscussionThreadsOfTopic(eq("general"), eq(0L), eq(50)))
        .thenReturn(threads);

    Map<String, Object> result =
        controller.getDiscussionThreadsAction(0L, "general", -1L, new MockHttpServletRequest());

    Assertions.assertSame(threads, result.get("discussionThreads"));
    verify(discussionService, never())
        .getDiscussionThreadsOfProblem(anyLong(), anyLong(), anyInt());
  }

  /** Test case: tests getDiscussionThreadsAction(...). Test data: a negative start index. Expected: the start index is clamped to 0 before the lookup. */
  @Test
  public void testGetDiscussionThreadsClampsNegativeStartIndex() {
    when(discussionService.getDiscussionThreadsOfTopic(anyString(), anyLong(), anyInt()))
        .thenReturn(List.of());

    controller.getDiscussionThreadsAction(-5L, "general", -1L, new MockHttpServletRequest());

    verify(discussionService).getDiscussionThreadsOfTopic("general", 0L, 50);
  }

  /** Test case: tests voteDiscussionReplyAction(...). Test data: an authenticated user voting. Expected: the current user and vote values are passed to DiscussionService and the result returned. */
  @Test
  public void testVoteDiscussionReplyDelegates() {
    User user = currentUser();
    authenticate(user);
    Map<String, Boolean> serviceResult = Map.of("isSuccessful", true);
    when(discussionService.voteDiscussionReply(5L, 9L, user, 1, 0)).thenReturn(serviceResult);

    Map<String, Boolean> result =
        controller.voteDiscussionReplyAction(5L, 9L, 1, 0, request());

    Assertions.assertSame(serviceResult, result);
    verify(discussionService).voteDiscussionReply(5L, 9L, user, 1, 0);
  }

  /** Test case: tests createDiscussionThreadAction(...). Test data: a numeric related problem id. Expected: the parsed id is forwarded to DiscussionService. */
  @Test
  public void testCreateDiscussionThreadParsesNumericProblemId() {
    User user = currentUser();
    authenticate(user);
    stubCreateThread();

    controller.createDiscussionThreadAction("qa", "1000", "Title", "Body", request());

    verify(discussionService).createDiscussionThread(user, "qa", 1000L, "Title", "Body");
  }

  /** Test case: tests createDiscussionThreadAction(...). Test data: a blank related problem id. Expected: it is treated as "no related problem" (-1). */
  @Test
  public void testCreateDiscussionThreadTreatsBlankProblemIdAsAbsent() {
    User user = currentUser();
    authenticate(user);
    stubCreateThread();

    controller.createDiscussionThreadAction("qa", "  ", "Title", "Body", request());

    verify(discussionService).createDiscussionThread(user, "qa", -1L, "Title", "Body");
  }

  /** Test case: tests createDiscussionThreadAction(...). Test data: a non-numeric related problem id. Expected: it is treated as "no related problem" (-1). */
  @Test
  public void testCreateDiscussionThreadTreatsMalformedProblemIdAsAbsent() {
    User user = currentUser();
    authenticate(user);
    stubCreateThread();

    controller.createDiscussionThreadAction("qa", "not-a-number", "Title", "Body", request());

    verify(discussionService).createDiscussionThread(user, "qa", -1L, "Title", "Body");
  }

  /** Stubs createDiscussionThread for any arguments to return a successful result. */
  private void stubCreateThread() {
    when(discussionService.createDiscussionThread(
            nullable(User.class), anyString(), anyLong(), anyString(), anyString()))
        .thenReturn(Map.of("isSuccessful", (Object) true));
  }

  /** Builds the current user (uid 1000, group "users"). */
  private static User currentUser() {
    return new User(1000, "zjhzxhz", "p", "a@b.com", new UserGroup(2, "users", "Users"), null);
  }

  /** Places the given user into the Spring Security context. */
  private static void authenticate(User user) {
    VojUserDetails userDetails = new VojUserDetails(user);
    SecurityContextHolder.getContext()
        .setAuthentication(
            UsernamePasswordAuthenticationToken.authenticated(
                userDetails, null, userDetails.getAuthorities()));
  }

  /** Builds a request carrying a remote address for the controller's audit logging. */
  private static MockHttpServletRequest request() {
    MockHttpServletRequest request = new MockHttpServletRequest("POST", "/discussion");
    request.setRemoteAddr("127.0.0.1");
    return request;
  }

  /** The mocked DiscussionService. */
  private DiscussionService discussionService;

  /** The mocked OptionService. */
  private OptionService optionService;

  /** The DiscussionController object under test. */
  private DiscussionController controller;
}
