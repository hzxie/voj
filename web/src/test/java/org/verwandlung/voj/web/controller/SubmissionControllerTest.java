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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import org.verwandlung.voj.web.exception.ResourceNotFoundException;
import org.verwandlung.voj.web.messenger.ApplicationEventListener;
import org.verwandlung.voj.web.messenger.SubmissionEvent;
import org.verwandlung.voj.web.model.JudgeResult;
import org.verwandlung.voj.web.model.Submission;
import org.verwandlung.voj.web.model.User;
import org.verwandlung.voj.web.model.UserGroup;
import org.verwandlung.voj.web.model.VojUserDetails;
import org.verwandlung.voj.web.service.ContestService;
import org.verwandlung.voj.web.service.OptionService;
import org.verwandlung.voj.web.service.SubmissionService;

/**
 * The test class for SubmissionController.
 *
 * <p>The collaborators are mocked and the controller methods invoked directly. The tests cover the
 * controller-specific logic - the {@code isSuccessful} flag derived from an empty/non-empty list and
 * the source-code visibility gate (owner / administrator / public-submissions option) - rather than
 * the service logic.
 *
 * @author Haozhe Xie
 */
public class SubmissionControllerTest {
  /** Wires mocked collaborators into a fresh controller before every test. */
  @BeforeEach
  public void setUp() {
    submissionService = mock(SubmissionService.class);
    optionService = mock(OptionService.class);
    when(optionService.getIntOption(anyString(), anyInt())).thenReturn(100);

    contestService = mock(ContestService.class);

    // Who may see a submission is ContestService's rule (tested in ContestServiceTest); these tests
    // cover what the controller does with a visible submission unless they say otherwise.
    when(contestService.isSubmissionVisible(any(), any())).thenReturn(true);

    controller = new SubmissionController();
    ReflectionTestUtils.setField(controller, "submissionService", submissionService);
    ReflectionTestUtils.setField(controller, "contestService", contestService);
    ReflectionTestUtils.setField(controller, "optionService", optionService);
  }

  /** Clears the security context after every test. */
  @AfterEach
  public void tearDown() {
    SecurityContextHolder.clearContext();
  }

  /** Test case: tests getSubmissionsAction(...). Test data: a non-empty submission list. Expected: isSuccessful is true and the list is returned. */
  @Test
  public void testGetSubmissionsActionNonEmpty() {
    List<Submission> submissions = List.of(new Submission());
    when(submissionService.getSubmissions(eq(1000L), eq("zjhzxhz"), eq(0L), eq(100), any()))
        .thenReturn(submissions);

    Map<String, Object> result =
        controller.getSubmissionsAction(1000L, "zjhzxhz", 0L, new MockHttpServletRequest());

    Assertions.assertEquals(Boolean.TRUE, result.get("isSuccessful"));
    Assertions.assertSame(submissions, result.get("submissions"));
  }

  /** Test case: tests getSubmissionsAction(...). Test data: an empty submission list. Expected: isSuccessful is false. */
  @Test
  public void testGetSubmissionsActionEmpty() {
    when(submissionService.getSubmissions(anyLong(), anyString(), anyLong(), anyInt(), any()))
        .thenReturn(List.of());

    Map<String, Object> result =
        controller.getSubmissionsAction(0L, "", 0L, new MockHttpServletRequest());

    Assertions.assertEquals(Boolean.FALSE, result.get("isSuccessful"));
  }

  /** Test case: tests getLatestSubmissionsAction(...). Test data: a non-empty list. Expected: isSuccessful is true and the list is returned. */
  @Test
  public void testGetLatestSubmissionsActionNonEmpty() {
    List<Submission> submissions = List.of(new Submission());
    when(submissionService.getLatestSubmissions(eq(1000L), eq(""), eq(5L), eq(100), any()))
        .thenReturn(submissions);

    Map<String, Object> result =
        controller.getLatestSubmissionsAction(1000L, "", 5L, new MockHttpServletRequest());

    Assertions.assertEquals(Boolean.TRUE, result.get("isSuccessful"));
    Assertions.assertSame(submissions, result.get("submissions"));
  }

  /** Test case: tests getSubmissionsAction(...). Test data: an anonymous viewer. Expected: the source code is stripped from every listed submission. */
  @Test
  public void testGetSubmissionsActionStripsCodeForOtherViewer() {
    Submission submission = submissionWithCode(ownerUser(), "secret code");
    when(submissionService.getSubmissions(eq(1000L), eq("zjhzxhz"), eq(0L), eq(100), any()))
        .thenReturn(List.of(submission));

    Map<String, Object> result =
        controller.getSubmissionsAction(1000L, "zjhzxhz", 0L, new MockHttpServletRequest());

    Assertions.assertEquals(Boolean.TRUE, result.get("isSuccessful"));
    Assertions.assertNull(submission.getCode());
  }

  /** Test case: tests getSubmissionsAction(...). Test data: the owner browsing their own submissions. Expected: the source code is retained for the owner. */
  @Test
  public void testGetSubmissionsActionKeepsCodeForOwner() {
    User owner = ownerUser();
    Submission submission = submissionWithCode(owner, "my code");
    when(submissionService.getSubmissions(eq(0L), eq(""), eq(0L), eq(100), any()))
        .thenReturn(List.of(submission));
    authenticate(owner);

    controller.getSubmissionsAction(0L, "", 0L, new MockHttpServletRequest());

    Assertions.assertEquals("my code", submission.getCode());
  }

  /** Test case: tests getLatestSubmissionsAction(...). Test data: an anonymous viewer. Expected: the source code is stripped from every listed submission. */
  @Test
  public void testGetLatestSubmissionsActionStripsCodeForOtherViewer() {
    Submission submission = submissionWithCode(ownerUser(), "secret code");
    when(submissionService.getLatestSubmissions(eq(1000L), eq(""), eq(5L), eq(100), any()))
        .thenReturn(List.of(submission));

    controller.getLatestSubmissionsAction(1000L, "", 5L, new MockHttpServletRequest());

    Assertions.assertNull(submission.getCode());
  }

  /** Test case: tests getSubmissionAction(...). Test data: a non-existent submission id. Expected: isSuccessful is false and the submission is null. */
  @Test
  public void testGetSubmissionActionNotFound() {
    when(submissionService.getSubmission(0L)).thenReturn(null);

    Map<String, Object> result = controller.getSubmissionAction(0L, new MockHttpServletRequest());

    Assertions.assertEquals(Boolean.FALSE, result.get("isSuccessful"));
    Assertions.assertNull(result.get("submission"));
  }

  /** Test case: tests getSubmissionAction(...). Test data: an anonymous viewer. Expected: the source code is stripped before returning. */
  @Test
  public void testGetSubmissionActionStripsCodeForAnonymousViewer() {
    Submission submission = submissionWithCode(ownerUser(), "secret code");
    when(submissionService.getSubmission(1000L)).thenReturn(submission);

    Map<String, Object> result = controller.getSubmissionAction(1000L, new MockHttpServletRequest());

    Assertions.assertEquals(Boolean.TRUE, result.get("isSuccessful"));
    Assertions.assertNull(((Submission) result.get("submission")).getCode());
  }

  /** Test case: tests getSubmissionAction(...). Test data: a logged-in user viewing another user's submission. Expected: the source code is stripped for the non-owner. */
  @Test
  public void testGetSubmissionActionStripsCodeForOtherLoggedInUser() {
    Submission submission = submissionWithCode(ownerUser(), "secret code");
    when(submissionService.getSubmission(1000L)).thenReturn(submission);
    authenticate(otherUser());

    Map<String, Object> result = controller.getSubmissionAction(1000L, new MockHttpServletRequest());

    Assertions.assertNull(((Submission) result.get("submission")).getCode());
  }

  /** Test case: tests getSubmissionAction(...). Test data: an administrator viewing another user's submission. Expected: the source code is retained for administrators. */
  @Test
  public void testGetSubmissionActionKeepsCodeForAdministrator() {
    Submission submission = submissionWithCode(ownerUser(), "secret code");
    when(submissionService.getSubmission(1000L)).thenReturn(submission);
    authenticate(administratorUser());

    Map<String, Object> result = controller.getSubmissionAction(1000L, new MockHttpServletRequest());

    Assertions.assertEquals("secret code", ((Submission) result.get("submission")).getCode());
  }

  /** Test case: tests getSubmissionAction(...). Test data: the owner viewing their own submission. Expected: the source code is retained for the owner. */
  @Test
  public void testGetSubmissionActionKeepsCodeForOwner() {
    User owner = ownerUser();
    Submission submission = submissionWithCode(owner, "my code");
    when(submissionService.getSubmission(1000L)).thenReturn(submission);
    authenticate(owner);

    Map<String, Object> result = controller.getSubmissionAction(1000L, new MockHttpServletRequest());

    Assertions.assertEquals("my code", ((Submission) result.get("submission")).getCode());
  }

  /** Test case: tests getSubmissionAction(...). Test data: a submission ContestService says the viewer may not see (e.g. another contestant's during a contest). Expected: the submission is treated as not found. */
  @Test
  public void testGetSubmissionActionHidesInvisibleSubmission() {
    User owner = ownerUser();
    Submission submission = submissionWithCode(owner, "my code");
    when(submissionService.getSubmission(1000L)).thenReturn(submission);
    when(contestService.isSubmissionVisible(submission, otherUser())).thenReturn(false);
    authenticate(otherUser());

    Map<String, Object> result = controller.getSubmissionAction(1000L, new MockHttpServletRequest());

    Assertions.assertEquals(Boolean.FALSE, result.get("isSuccessful"));
    Assertions.assertNull(result.get("submission"));
  }

  /** Test case: tests getSubmissionsAction(...). Test data: an administrator browsing the list. Expected: submissions to unpublished problems are included. */
  @Test
  public void testGetSubmissionsActionIncludesUnpublishedProblemsForAdministrator() {
    List<Submission> submissions = List.of(new Submission());
    when(submissionService.getSubmissions(eq(0L), eq(""), eq(0L), eq(100), any()))
        .thenReturn(submissions);
    authenticate(administratorUser());

    Map<String, Object> result =
        controller.getSubmissionsAction(0L, "", 0L, new MockHttpServletRequest());

    Assertions.assertSame(submissions, result.get("submissions"));
  }

  /** Builds the submission owner (uid 1000, group "users"). */
  private static User ownerUser() {
    return new User(1000, "zjhzxhz", "p", "a@b.com", new UserGroup(2, "users", "Users"), null);
  }

  /** Builds a different regular user (uid 1001, group "users") who does not own the submission. */
  private static User otherUser() {
    return new User(1001, "someone", "p", "c@d.com", new UserGroup(2, "users", "Users"), null);
  }

  /** Builds an administrator (uid 1, group "administrators"). */
  private static User administratorUser() {
    return new User(
        1, "admin", "p", "e@f.com", new UserGroup(1, "administrators", "Administrators"), null);
  }

  /** Builds a submission owned by the given user and carrying the given source code. */
  private static Submission submissionWithCode(User user, String code) {
    Submission submission = new Submission();
    submission.setUser(user);
    submission.setCode(code);
    return submission;
  }

  /**
   * Test case: tests getRealTimeJudgeResultAction(...). Test data: the owner of a pending
   * submission follows its judging, and its outcome is sent while the controller reads the
   * submission (after the read found it pending, before the controller returns). Expected: the
   * stream the controller returns received it and is completed, rather than waiting for messages
   * that will never come.
   */
  @Test
  public void testAnOutcomeSentWhileTheStreamIsOpenedReachesIt() throws Exception {
    User owner = ownerUser();
    authenticate(owner);
    long submissionId = 987654321L;
    ApplicationEventListener listener = new ApplicationEventListener();
    ReflectionTestUtils.setField(listener, "submissionService", submissionService);
    when(submissionService.isCurrentJudging(submissionId, 42L)).thenReturn(true);
    ReflectionTestUtils.setField(controller, "submissionEventListener", listener);
    Submission pending = submissionWithCode(owner, "code");
    pending.setJudgeResult(new JudgeResult(1, "PD", "Pending"));
    when(submissionService.getSubmission(submissionId))
        .thenAnswer(
            invocation -> {
              listener.submissionEventHandler(
                  new SubmissionEvent(this, submissionId, 42L, "System Error", "Log", true));
              return pending;
            });

    SseEmitter sseEmitter =
        controller.getRealTimeJudgeResultAction(
            submissionId, new MockHttpServletRequest(), new MockHttpServletResponse());

    Assertions.assertThrows(IllegalStateException.class, () -> sseEmitter.send("more"));
    Assertions.assertEquals(0, listener.getNumberOfSseEmitters(submissionId));
  }

  /**
   * Test case: tests getRealTimeJudgeResultAction(...). Test data: the owner of a pending
   * submission follows its judging. Expected: the stream is registered and lasts 10 minutes, rather
   * than the servlet container's default timeout of the asynchronous requests (30 seconds).
   */
  @Test
  public void testTheStreamOfAPendingSubmissionLastsTenMinutes() throws Exception {
    User owner = ownerUser();
    authenticate(owner);
    long submissionId = 987654320L;
    ApplicationEventListener listener = new ApplicationEventListener();
    ReflectionTestUtils.setField(controller, "submissionEventListener", listener);
    Submission pending = submissionWithCode(owner, "code");
    pending.setJudgeResult(new JudgeResult(1, "PD", "Pending"));
    when(submissionService.getSubmission(submissionId)).thenReturn(pending);

    SseEmitter sseEmitter =
        controller.getRealTimeJudgeResultAction(
            submissionId, new MockHttpServletRequest(), new MockHttpServletResponse());

    Assertions.assertEquals(600000L, sseEmitter.getTimeout());
    Assertions.assertEquals(1, listener.getNumberOfSseEmitters(submissionId));
  }

  /**
   * Test case: tests getRealTimeJudgeResultAction(...). Test data: a submission that is no longer
   * pending. Expected: no stream (404), and none is left registered.
   */
  @Test
  public void testNoStreamOfAJudgedSubmission() throws Exception {
    User owner = ownerUser();
    authenticate(owner);
    long submissionId = 987654322L;
    ApplicationEventListener listener = new ApplicationEventListener();
    ReflectionTestUtils.setField(controller, "submissionEventListener", listener);
    Submission judged = submissionWithCode(owner, "code");
    judged.setJudgeResult(new JudgeResult(2, "AC", "Accepted"));
    when(submissionService.getSubmission(submissionId)).thenReturn(judged);

    Assertions.assertThrows(
        ResourceNotFoundException.class,
        () ->
            controller.getRealTimeJudgeResultAction(
                submissionId, new MockHttpServletRequest(), new MockHttpServletResponse()));
    Assertions.assertEquals(0, listener.getNumberOfSseEmitters(submissionId));
  }

  /**
   * Test case: tests getRealTimeJudgeResultAction(...). Test data: the submission cannot be read
   * (the database is unreachable). Expected: the failure propagates, and the stream, which is
   * never returned, is not left registered.
   */
  @Test
  public void testNoStreamIsLeftRegisteredWhenTheChecksFail() throws Exception {
    authenticate(ownerUser());
    long submissionId = 987654323L;
    ApplicationEventListener listener = new ApplicationEventListener();
    ReflectionTestUtils.setField(controller, "submissionEventListener", listener);
    when(submissionService.getSubmission(submissionId))
        .thenThrow(new org.springframework.dao.DataAccessResourceFailureException("down"));

    Assertions.assertThrows(
        org.springframework.dao.DataAccessResourceFailureException.class,
        () ->
            controller.getRealTimeJudgeResultAction(
                submissionId, new MockHttpServletRequest(), new MockHttpServletResponse()));
    Assertions.assertEquals(0, listener.getNumberOfSseEmitters(submissionId));
  }

  /** Places the given user into the Spring Security context. */
  private static void authenticate(User user) {
    VojUserDetails userDetails = new VojUserDetails(user);
    SecurityContextHolder.getContext()
        .setAuthentication(
            UsernamePasswordAuthenticationToken.authenticated(
                userDetails, null, userDetails.getAuthorities()));
  }

  /** The mocked SubmissionService. */
  private SubmissionService submissionService;

  /** The mocked ContestService. */
  private ContestService contestService;

  /** The mocked OptionService. */
  private OptionService optionService;

  /** The SubmissionController object under test. */
  private SubmissionController controller;
}
