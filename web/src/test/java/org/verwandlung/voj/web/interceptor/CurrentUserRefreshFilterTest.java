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
package org.verwandlung.voj.web.interceptor;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;

import org.verwandlung.voj.web.model.User;
import org.verwandlung.voj.web.model.UserGroup;
import org.verwandlung.voj.web.model.VojUserDetails;
import org.verwandlung.voj.web.service.UserService;
import org.verwandlung.voj.web.util.HttpSessionParser;

/**
 * The test class for CurrentUserRefreshFilter.
 *
 * @author Haozhe Xie
 */
public class CurrentUserRefreshFilterTest {
  /** Creates the filter with a mocked UserService. */
  @BeforeEach
  public void setUp() {
    userService = mock(UserService.class);
    when(userService.isAllowedToAccess(any(User.class)))
        .thenAnswer(
            invocation -> {
              User user = invocation.getArgument(0);
              String slug = user.getUserGroup().getUserGroupSlug();
              return "users".equals(slug) || "administrators".equals(slug);
            });
    filter = new CurrentUserRefreshFilter(userService);
    session = new MockHttpSession();
    request = new MockHttpServletRequest("GET", "/administration");
    request.setSession(session);
    response = new MockHttpServletResponse();
    chain = new MockFilterChain();
  }

  /** Clears the security context between test cases. */
  @AfterEach
  public void tearDown() {
    SecurityContextHolder.clearContext();
  }

  /** Test case: an anonymous request. Expected: no lookup, the request passes. */
  @Test
  public void testAnonymousRequestIsNotLookedUp() throws Exception {
    filter.doFilter(request, response, chain);

    verify(userService, never()).getUserUsingUid(anyLong());
    Assertions.assertNotNull(chain.getRequest());
  }

  /** Test case: a static asset requested by a logged-in user. Expected: no lookup. */
  @Test
  public void testStaticAssetsAreSkipped() throws Exception {
    login(user("administrators"));
    request.setRequestURI("/assets/css/app.css");

    filter.doFilter(request, response, chain);

    verify(userService, never()).getUserUsingUid(anyLong());
  }

  /** Test case: a user banned (moved to "forbidden") after logging in. Expected: the session is invalidated and the request continues anonymously. */
  @Test
  public void testBannedUserIsLoggedOut() throws Exception {
    login(user("users"));
    when(userService.getUserUsingUid(1000L)).thenReturn(user("forbidden"));

    filter.doFilter(request, response, chain);

    Assertions.assertTrue(session.isInvalid());
    Assertions.assertNull(SecurityContextHolder.getContext().getAuthentication());
    Assertions.assertNotNull(chain.getRequest());
  }

  /** Test case: a user deleted after logging in. Expected: the session is invalidated. */
  @Test
  public void testDeletedUserIsLoggedOut() throws Exception {
    login(user("users"));
    when(userService.getUserUsingUid(1000L)).thenReturn(null);

    filter.doFilter(request, response, chain);

    Assertions.assertTrue(session.isInvalid());
    Assertions.assertNull(SecurityContextHolder.getContext().getAuthentication());
  }

  /** Test case: an administrator demoted to a regular user after logging in. Expected: the role is refreshed in the request's context and in the session. */
  @Test
  public void testDemotedAdministratorLosesRole() throws Exception {
    login(user("administrators"));
    when(userService.getUserUsingUid(1000L)).thenReturn(user("users"));

    filter.doFilter(request, response, chain);

    Assertions.assertFalse(session.isInvalid());
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    Assertions.assertEquals(
        "ROLE_USERS", authentication.getAuthorities().iterator().next().getAuthority());
    Assertions.assertEquals(
        "users", HttpSessionParser.getCurrentUser().getUserGroup().getUserGroupSlug());

    SecurityContext storedContext =
        (SecurityContext)
            session.getAttribute(
                HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);
    Assertions.assertEquals(
        "ROLE_USERS",
        storedContext.getAuthentication().getAuthorities().iterator().next().getAuthority());
  }

  /**
   * Test case: the user's password changed after this session logged in (changed in another
   * session, reset by email or set by an administrator). Expected: the session is invalidated.
   */
  @Test
  public void testUserWhosePasswordChangedIsLoggedOut() throws Exception {
    login(user("users"));
    User freshUser = user("users");
    freshUser.setPassword("{bcrypt}another-password");
    when(userService.getUserUsingUid(1000L)).thenReturn(freshUser);

    filter.doFilter(request, response, chain);

    Assertions.assertTrue(session.isInvalid());
    Assertions.assertNull(SecurityContextHolder.getContext().getAuthentication());
    Assertions.assertNotNull(chain.getRequest());
  }

  /** Test case: an unchanged user. Expected: the session stays valid and the fresh user is used. */
  @Test
  public void testUnchangedUserIsRefreshed() throws Exception {
    login(user("users"));
    User freshUser = user("users");
    freshUser.setEmail("new-address@example.com");
    when(userService.getUserUsingUid(1000L)).thenReturn(freshUser);

    filter.doFilter(request, response, chain);

    Assertions.assertFalse(session.isInvalid());
    Assertions.assertSame(freshUser, HttpSessionParser.getCurrentUser());
  }

  /** Builds a user (uid 1000) in the given user group. */
  private static User user(String userGroupSlug) {
    return new User(
        1000,
        "zjhzxhz",
        "password",
        "cshzxie@gmail.com",
        new UserGroup(1, userGroupSlug, userGroupSlug),
        null);
  }

  /** Installs the given user as logged in, both in the thread-local context and in the session. */
  private void login(User user) {
    VojUserDetails userDetails = new VojUserDetails(user);
    SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
    securityContext.setAuthentication(
        UsernamePasswordAuthenticationToken.authenticated(
            userDetails, null, userDetails.getAuthorities()));
    SecurityContextHolder.setContext(securityContext);
    session.setAttribute(
        HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, securityContext);
  }

  /** The mocked UserService. */
  private UserService userService;

  /** The filter under test. */
  private CurrentUserRefreshFilter filter;

  /** The session of the request. */
  private MockHttpSession session;

  /** The request. */
  private MockHttpServletRequest request;

  /** The response. */
  private MockHttpServletResponse response;

  /** The filter chain, which records whether the request was passed on. */
  private MockFilterChain chain;
}
