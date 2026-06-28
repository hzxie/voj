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
import static org.mockito.ArgumentMatchers.anyBoolean;
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

import org.verwandlung.voj.web.model.Problem;
import org.verwandlung.voj.web.model.Submission;
import org.verwandlung.voj.web.model.User;
import org.verwandlung.voj.web.model.UserGroup;
import org.verwandlung.voj.web.model.VojUserDetails;
import org.verwandlung.voj.web.service.OptionService;
import org.verwandlung.voj.web.service.ProblemService;
import org.verwandlung.voj.web.service.SubmissionService;

/**
 * The test class for ProblemsController.
 *
 * <p>Covers the controller-specific behavior of the action endpoints: the per-user solved-status map
 * is attached only for an authenticated viewer, and {@code createSubmissionAction} passes the
 * current user through and returns the service result verbatim.
 *
 * @author Haozhe Xie
 */
public class ProblemsControllerTest {
  /** Wires mocked collaborators into a fresh controller before every test. */
  @BeforeEach
  public void setUp() {
    problemService = mock(ProblemService.class);
    submissionService = mock(SubmissionService.class);
    optionService = mock(OptionService.class);
    when(optionService.getIntOption(anyString(), anyInt())).thenReturn(100);

    controller = new ProblemsController();
    ReflectionTestUtils.setField(controller, "problemService", problemService);
    ReflectionTestUtils.setField(controller, "submissionService", submissionService);
    ReflectionTestUtils.setField(controller, "optionService", optionService);
  }

  /** Clears the security context after every test. */
  @AfterEach
  public void tearDown() {
    SecurityContextHolder.clearContext();
  }

  /** Test case: tests getProblemsAction(...). Test data: an anonymous viewer. Expected: the problems are returned but no per-user solved-status map is attached. */
  @Test
  public void testGetProblemsActionAnonymous() {
    List<Problem> problems = List.of(new Problem());
    when(problemService.getProblemsUsingFilters(
            anyLong(), nullable(String.class), nullable(String.class), any(), any(), anyBoolean(),
            anyInt()))
        .thenReturn(problems);

    Map<String, Object> result =
        controller.getProblemsAction(0L, null, null, new MockHttpServletRequest());

    Assertions.assertEquals(Boolean.TRUE, result.get("isSuccessful"));
    Assertions.assertSame(problems, result.get("problems"));
    Assertions.assertNull(result.get("submissionOfProblems"));
    verify(submissionService, never()).getSubmissionOfProblems(anyLong(), anyLong(), anyLong());
  }

  /** Test case: tests getProblemsAction(...). Test data: an authenticated viewer. Expected: the per-user solved-status map is attached for that user. */
  @Test
  public void testGetProblemsActionAuthenticated() {
    List<Problem> problems = List.of(new Problem());
    when(problemService.getProblemsUsingFilters(
            anyLong(), nullable(String.class), nullable(String.class), any(), any(), anyBoolean(),
            anyInt()))
        .thenReturn(problems);
    authenticate(currentUser());
    Map<Long, Submission> solved = Map.of(1000L, new Submission());
    when(submissionService.getSubmissionOfProblems(eq(1000L), eq(0L), eq(100L))).thenReturn(solved);

    Map<String, Object> result =
        controller.getProblemsAction(0L, null, null, new MockHttpServletRequest());

    Assertions.assertSame(solved, result.get("submissionOfProblems"));
  }

  /** Test case: tests createSubmissionAction(...). Test data: a successful submission for the current user. Expected: the current user is passed through and the service result returned verbatim. */
  @Test
  public void testCreateSubmissionActionSuccess() {
    User user = currentUser();
    authenticate(user);
    Map<String, Object> serviceResult = Map.of("isSuccessful", true, "submissionId", 1234L);
    when(submissionService.createSubmission(user, 1000L, "text/x-java", "public class Main {}"))
        .thenReturn(serviceResult);

    Map<String, Object> result =
        controller.createSubmissionAction(
            1000L, "text/x-java", "public class Main {}", request());

    Assertions.assertSame(serviceResult, result);
    verify(submissionService).createSubmission(user, 1000L, "text/x-java", "public class Main {}");
  }

  /** Test case: tests createSubmissionAction(...). Test data: a rejected submission. Expected: the failure result is returned and no submission id is dereferenced. */
  @Test
  public void testCreateSubmissionActionFailure() {
    User user = currentUser();
    authenticate(user);
    Map<String, Object> serviceResult = Map.of("isSuccessful", false);
    when(submissionService.createSubmission(eq(user), anyLong(), anyString(), anyString()))
        .thenReturn(serviceResult);

    Map<String, Object> result =
        controller.createSubmissionAction(1000L, "text/x-java", "code", request());

    Assertions.assertEquals(Boolean.FALSE, result.get("isSuccessful"));
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
    MockHttpServletRequest request = new MockHttpServletRequest("POST", "/p/createSubmission.action");
    request.setRemoteAddr("127.0.0.1");
    return request;
  }

  /** The mocked ProblemService. */
  private ProblemService problemService;

  /** The mocked SubmissionService. */
  private SubmissionService submissionService;

  /** The mocked OptionService. */
  private OptionService optionService;

  /** The ProblemsController object under test. */
  private ProblemsController controller;
}
