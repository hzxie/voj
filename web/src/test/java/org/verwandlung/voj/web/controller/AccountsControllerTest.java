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
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.test.util.ReflectionTestUtils;

import org.verwandlung.voj.web.model.Option;
import org.verwandlung.voj.web.model.User;
import org.verwandlung.voj.web.model.UserGroup;
import org.verwandlung.voj.web.model.VojUserDetails;
import org.verwandlung.voj.web.service.OptionService;
import org.verwandlung.voj.web.service.SubmissionService;
import org.verwandlung.voj.web.service.TurnstileService;
import org.verwandlung.voj.web.service.UserService;

/**
 * The test class for AccountsController.
 *
 * <p>The collaborators are mocked and the controller methods are invoked directly (they already take
 * the request/response and return the result map). The tests assert the controller-specific logic:
 * the Turnstile captcha gate, delegation to {@link UserService}, the current-user resolution and the
 * programmatic-login security-context establishment - not the service logic, which is tested
 * separately.
 *
 * @author Haozhe Xie
 */
public class AccountsControllerTest {
  /** Wires mocked collaborators into a fresh controller before every test. */
  @BeforeEach
  public void setUp() {
    userService = mock(UserService.class);
    turnstileService = mock(TurnstileService.class);
    submissionService = mock(SubmissionService.class);
    sessionRegistry = mock(SessionRegistry.class);
    optionService = mock(OptionService.class);

    controller = new AccountsController();
    ReflectionTestUtils.setField(controller, "userService", userService);
    ReflectionTestUtils.setField(controller, "turnstileService", turnstileService);
    ReflectionTestUtils.setField(controller, "submissionService", submissionService);
    ReflectionTestUtils.setField(controller, "sessionRegistry", sessionRegistry);
    ReflectionTestUtils.setField(controller, "optionService", optionService);
  }

  /** Clears any security context a login test may have established so it does not leak across cases. */
  @AfterEach
  public void tearDown() {
    SecurityContextHolder.clearContext();
  }

  /** Test case: tests loginAction(...). Test data: a request whose captcha fails verification. Expected: the captcha-failure result is returned and UserService is never consulted. */
  @Test
  public void testLoginActionCaptchaFailure() {
    when(turnstileService.verify(anyString(), anyString())).thenReturn(false);

    Map<String, Boolean> result =
        controller.loginAction(
            "zjhzxhz", "secret", false, "bad-captcha", request(), new MockHttpServletResponse());

    Assertions.assertEquals(Boolean.FALSE, result.get("isCaptchaValid"));
    Assertions.assertEquals(Boolean.FALSE, result.get("isSuccessful"));
    verify(userService, never()).isAllowedToLogin(anyString(), anyString());
    Assertions.assertNull(SecurityContextHolder.getContext().getAuthentication());
  }

  /** Test case: tests loginAction(...). Test data: a valid captcha but wrong credentials. Expected: the service result is returned, no user is loaded and no security context is established. */
  @Test
  public void testLoginActionBadCredentials() {
    when(turnstileService.verify(anyString(), anyString())).thenReturn(true);
    when(userService.isAllowedToLogin("zjhzxhz", "secret"))
        .thenReturn(Map.of("isSuccessful", false, "isAccountValid", false));

    Map<String, Boolean> result =
        controller.loginAction(
            "zjhzxhz", "secret", false, "captcha", request(), new MockHttpServletResponse());

    Assertions.assertEquals(Boolean.FALSE, result.get("isSuccessful"));
    verify(userService, never()).getUserUsingUsernameOrEmail(anyString());
    verify(sessionRegistry, never()).registerNewSession(anyString(), any());
    Assertions.assertNull(SecurityContextHolder.getContext().getAuthentication());
  }

  /** Test case: tests loginAction(...). Test data: a valid captcha and correct credentials. Expected: the user is loaded, a security context is established and the session is registered. */
  @Test
  public void testLoginActionSuccess() {
    when(turnstileService.verify(anyString(), anyString())).thenReturn(true);
    when(userService.isAllowedToLogin("zjhzxhz", "secret"))
        .thenReturn(Map.of("isSuccessful", true));
    User user = userWithGroup();
    when(userService.getUserUsingUsernameOrEmail("zjhzxhz")).thenReturn(user);

    MockHttpServletRequest request = request();
    Map<String, Boolean> result =
        controller.loginAction(
            "zjhzxhz", "secret", false, "captcha", request, new MockHttpServletResponse());

    Assertions.assertEquals(Boolean.TRUE, result.get("isSuccessful"));
    verify(sessionRegistry).registerNewSession(eq(request.getSession().getId()), any());

    // The thread-local security context now holds the logged-in user.
    Object principal =
        SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    Assertions.assertTrue(principal instanceof VojUserDetails);
    Assertions.assertEquals("zjhzxhz", ((VojUserDetails) principal).getUser().getUsername());
  }

  /** Test case: tests registerAction(...). Test data: a request whose captcha fails verification. Expected: the captcha-failure result is returned and no user is created. */
  @Test
  public void testRegisterActionCaptchaFailure() {
    when(turnstileService.verify(anyString(), anyString())).thenReturn(false);

    Map<String, Boolean> result =
        controller.registerAction(
            "zjhzxhz",
            "secret",
            "cshzxie@gmail.com",
            "zh_CN",
            "bad-captcha",
            request(),
            new MockHttpServletResponse());

    Assertions.assertEquals(Boolean.FALSE, result.get("isCaptchaValid"));
    verify(userService, never())
        .createUser(anyString(), anyString(), anyString(), anyString(), anyString(), anyBoolean());
  }

  /** Test case: tests registerAction(...). Test data: a valid captcha with registration open and email verification off. Expected: the account is created under the "users" group, a security context is established and the service result is returned. */
  @Test
  public void testRegisterActionSuccessDelegates() {
    when(turnstileService.verify(anyString(), anyString())).thenReturn(true);
    when(optionService.getOption("allowUserRegister"))
        .thenReturn(new Option("allowUserRegister", "1", true));
    when(optionService.getOption("requireEmailVerification"))
        .thenReturn(new Option("requireEmailVerification", "0", true));
    Map<String, Boolean> serviceResult = Map.of("isSuccessful", true);
    when(userService.createUser("zjhzxhz", "secret", "cshzxie@gmail.com", "users", "zh_CN", true))
        .thenReturn(serviceResult);
    User user = userWithGroup();
    when(userService.getUserUsingUsernameOrEmail("zjhzxhz")).thenReturn(user);

    Map<String, Boolean> result =
        controller.registerAction(
            "zjhzxhz",
            "secret",
            "cshzxie@gmail.com",
            "zh_CN",
            "captcha",
            request(),
            new MockHttpServletResponse());

    Assertions.assertSame(serviceResult, result);
    verify(userService)
        .createUser("zjhzxhz", "secret", "cshzxie@gmail.com", "users", "zh_CN", true);
    // Email verification is off, so it must not be required.
    verify(userService, never()).requireEmailVerification(any());
    // A security context is established for the freshly registered user.
    Assertions.assertNotNull(SecurityContextHolder.getContext().getAuthentication());
  }

  /** Test case: tests forgotPasswordAction(...). Test data: a request whose captcha fails verification. Expected: the captcha-failure result is returned and no verification email is sent. */
  @Test
  public void testForgotPasswordActionCaptchaFailure() {
    when(turnstileService.verify(anyString(), anyString())).thenReturn(false);

    Map<String, Boolean> result =
        controller.forgotPasswordAction("zjhzxhz", "cshzxie@gmail.com", "bad-captcha", request());

    Assertions.assertEquals(Boolean.FALSE, result.get("isCaptchaValid"));
    verify(userService, never()).sendVerificationEmail(anyString(), anyString());
  }

  /** Test case: tests forgotPasswordAction(...). Test data: a valid captcha. Expected: the request is delegated to UserService.sendVerificationEmail and its result returned. */
  @Test
  public void testForgotPasswordActionDelegates() {
    when(turnstileService.verify(anyString(), anyString())).thenReturn(true);
    Map<String, Boolean> serviceResult = Map.of("isSuccessful", true);
    when(userService.sendVerificationEmail("zjhzxhz", "cshzxie@gmail.com")).thenReturn(serviceResult);

    Map<String, Boolean> result =
        controller.forgotPasswordAction("zjhzxhz", "cshzxie@gmail.com", "captcha", request());

    Assertions.assertSame(serviceResult, result);
    verify(userService).sendVerificationEmail("zjhzxhz", "cshzxie@gmail.com");
  }

  /** Test case: tests resetPasswordAction(...). Test data: a token-based reset request (no captcha gate). Expected: the request is delegated to UserService.resetPassword and its result returned. */
  @Test
  public void testResetPasswordActionDelegates() {
    Map<String, Boolean> serviceResult = Map.of("isSuccessful", true);
    when(userService.resetPassword("cshzxie@gmail.com", "token", "newPass", "newPass"))
        .thenReturn(serviceResult);

    Map<String, Boolean> result =
        controller.resetPasswordAction(
            "cshzxie@gmail.com", "token", "newPass", "newPass", request());

    Assertions.assertSame(serviceResult, result);
    verify(userService).resetPassword("cshzxie@gmail.com", "token", "newPass", "newPass");
  }

  /** Test case: tests changePasswordInDashboardAction(...). Test data: an authenticated user changing their password. Expected: the current user is resolved and passed to UserService.changePassword. */
  @Test
  public void testChangePasswordResolvesCurrentUser() {
    User user = userWithGroup();
    authenticate(user);
    Map<String, Boolean> serviceResult = Map.of("isSuccessful", true);
    when(userService.changePassword(user, "old", "new", "new")).thenReturn(serviceResult);

    Map<String, Boolean> result =
        controller.changePasswordInDashboardAction("old", "new", "new", request());

    Assertions.assertSame(serviceResult, result);
    verify(userService).changePassword(user, "old", "new", "new");
  }

  /** Test case: tests updateProfileInDashboardAction(...). Test data: an authenticated user updating their profile. Expected: the current user is resolved and passed to UserService.updateProfile. */
  @Test
  public void testUpdateProfileResolvesCurrentUser() {
    User user = userWithGroup();
    authenticate(user);
    Map<String, Boolean> serviceResult = Map.of("isSuccessful", true);
    when(userService.updateProfile(user, "cshzxie@gmail.com", "Haozhe", "Earth", "x.com", "{}", "Hi"))
        .thenReturn(serviceResult);

    Map<String, Boolean> result =
        controller.updateProfileInDashboardAction(
            "cshzxie@gmail.com", "Haozhe", "Earth", "x.com", "{}", "Hi", request());

    Assertions.assertSame(serviceResult, result);
    verify(userService)
        .updateProfile(user, "cshzxie@gmail.com", "Haozhe", "Earth", "x.com", "{}", "Hi");
  }

  /** Test case: tests getNumberOfSubmissionsOfUsersAction(...). Test data: an explicit uid. Expected: that uid drives the per-day query and the result is keyed under totalSubmissions. */
  @Test
  public void testGetNumberOfSubmissionsOfUsersWithExplicitUid() {
    Map<String, Long> perDay = Map.of("2026-06-28", 3L);
    when(submissionService.getNumberOfSubmissionsGroupByDay(
            any(), any(), eq(1000L), eq(false)))
        .thenReturn(perDay);

    Map<String, Object> result =
        controller.getNumberOfSubmissionsOfUsersAction(1000L, request());

    Assertions.assertSame(perDay, result.get("totalSubmissions"));
  }

  /** Test case: tests getNumberOfSubmissionsOfUsersAction(...). Test data: uid 0 with an authenticated user. Expected: the current user's uid is resolved and used for the query. */
  @Test
  public void testGetNumberOfSubmissionsOfUsersFallsBackToCurrentUser() {
    User user = userWithGroup();
    authenticate(user);
    Map<String, Long> perDay = Map.of("2026-06-28", 5L);
    when(submissionService.getNumberOfSubmissionsGroupByDay(
            any(), any(), eq(1000L), eq(false)))
        .thenReturn(perDay);

    Map<String, Object> result = controller.getNumberOfSubmissionsOfUsersAction(0L, request());

    Assertions.assertSame(perDay, result.get("totalSubmissions"));
    verify(submissionService)
        .getNumberOfSubmissionsGroupByDay(any(), any(), eq(1000L), eq(false));
  }

  /** Builds a User (uid 1000, group "users") suitable for VojUserDetails (which needs a non-null user group). */
  private static User userWithGroup() {
    UserGroup userGroup = new UserGroup(2, "users", "Users");
    return new User(1000, "zjhzxhz", "password", "cshzxie@gmail.com", userGroup, null);
  }

  /** Places the given user into the Spring Security context, as the dashboard endpoints expect. */
  private static void authenticate(User user) {
    VojUserDetails userDetails = new VojUserDetails(user);
    SecurityContextHolder.getContext()
        .setAuthentication(
            org.springframework.security.authentication.UsernamePasswordAuthenticationToken
                .authenticated(userDetails, null, userDetails.getAuthorities()));
  }

  /** Builds a request carrying a remote address, which the controller reads for its audit logging. */
  private static MockHttpServletRequest request() {
    MockHttpServletRequest request = new MockHttpServletRequest("POST", "/accounts/login.action");
    request.setRemoteAddr("127.0.0.1");
    return request;
  }

  /** The mocked UserService. */
  private UserService userService;

  /** The mocked TurnstileService (captcha verification). */
  private TurnstileService turnstileService;

  /** The mocked SubmissionService. */
  private SubmissionService submissionService;

  /** The mocked SessionRegistry (online-user tracking). */
  private SessionRegistry sessionRegistry;

  /** The mocked OptionService (registration-open / email-verification flags). */
  private OptionService optionService;

  /** The AccountsController object under test. */
  private AccountsController controller;
}
