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
import static org.mockito.Mockito.mock;
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

import org.verwandlung.voj.web.model.Submission;
import org.verwandlung.voj.web.model.User;
import org.verwandlung.voj.web.model.UserGroup;
import org.verwandlung.voj.web.model.VojUserDetails;
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

    controller = new SubmissionController();
    ReflectionTestUtils.setField(controller, "submissionService", submissionService);
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
    when(submissionService.getSubmissions(eq(1000L), eq("zjhzxhz"), eq(0L), eq(100)))
        .thenReturn(submissions);

    Map<String, Object> result =
        controller.getSubmissionsAction(1000L, "zjhzxhz", 0L, new MockHttpServletRequest());

    Assertions.assertEquals(Boolean.TRUE, result.get("isSuccessful"));
    Assertions.assertSame(submissions, result.get("submissions"));
  }

  /** Test case: tests getSubmissionsAction(...). Test data: an empty submission list. Expected: isSuccessful is false. */
  @Test
  public void testGetSubmissionsActionEmpty() {
    when(submissionService.getSubmissions(anyLong(), anyString(), anyLong(), anyInt()))
        .thenReturn(List.of());

    Map<String, Object> result =
        controller.getSubmissionsAction(0L, "", 0L, new MockHttpServletRequest());

    Assertions.assertEquals(Boolean.FALSE, result.get("isSuccessful"));
  }

  /** Test case: tests getLatestSubmissionsAction(...). Test data: a non-empty list. Expected: isSuccessful is true and the list is returned. */
  @Test
  public void testGetLatestSubmissionsActionNonEmpty() {
    List<Submission> submissions = List.of(new Submission());
    when(submissionService.getLatestSubmissions(eq(1000L), eq(""), eq(5L), eq(100)))
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
    when(submissionService.getSubmissions(eq(1000L), eq("zjhzxhz"), eq(0L), eq(100)))
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
    when(submissionService.getSubmissions(eq(0L), eq(""), eq(0L), eq(100)))
        .thenReturn(List.of(submission));
    authenticate(owner);

    controller.getSubmissionsAction(0L, "", 0L, new MockHttpServletRequest());

    Assertions.assertEquals("my code", submission.getCode());
  }

  /** Test case: tests getLatestSubmissionsAction(...). Test data: an anonymous viewer. Expected: the source code is stripped from every listed submission. */
  @Test
  public void testGetLatestSubmissionsActionStripsCodeForOtherViewer() {
    Submission submission = submissionWithCode(ownerUser(), "secret code");
    when(submissionService.getLatestSubmissions(eq(1000L), eq(""), eq(5L), eq(100)))
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

  /** The mocked OptionService. */
  private OptionService optionService;

  /** The SubmissionController object under test. */
  private SubmissionController controller;
}
