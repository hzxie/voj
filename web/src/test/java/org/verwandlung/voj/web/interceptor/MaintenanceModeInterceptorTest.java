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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import jakarta.servlet.http.HttpServletResponse;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import org.verwandlung.voj.web.mapper.OptionMapper;
import org.verwandlung.voj.web.model.Option;
import org.verwandlung.voj.web.model.User;
import org.verwandlung.voj.web.model.UserGroup;
import org.verwandlung.voj.web.model.VojUserDetails;

/**
 * The test class for MaintenanceModeInterceptor.
 *
 * @author Haozhe Xie
 */
public class MaintenanceModeInterceptorTest {
  /** Wires a mocked OptionMapper into a fresh interceptor before every test. */
  @BeforeEach
  public void setUp() {
    optionMapper = mock(OptionMapper.class);
    interceptor = new MaintenanceModeInterceptor();
    ReflectionTestUtils.setField(interceptor, "optionMapper", optionMapper);
  }

  /** Clears the security context after every test so authentication does not leak between cases. */
  @AfterEach
  public void tearDown() {
    SecurityContextHolder.clearContext();
  }

  /** Test case: tests preHandle(...). Test data: maintenance mode disabled. Expected: the request proceeds (returns true). */
  @Test
  public void testPreHandleWhenMaintenanceDisabled() throws Exception {
    setMaintenanceMode("0");
    MockHttpServletRequest request = requestFor("/problems");
    MockHttpServletResponse response = new MockHttpServletResponse();

    Assertions.assertTrue(interceptor.preHandle(request, response, new Object()));
    Assertions.assertNull(response.getForwardedUrl());
  }

  /** Test case: tests preHandle(...). Test data: the maintenance-mode option is absent. Expected: the request proceeds (returns true). */
  @Test
  public void testPreHandleWhenOptionMissing() throws Exception {
    when(optionMapper.getOption("maintenanceMode")).thenReturn(null);
    MockHttpServletRequest request = requestFor("/problems");
    MockHttpServletResponse response = new MockHttpServletResponse();

    Assertions.assertTrue(interceptor.preHandle(request, response, new Object()));
  }

  /** Test case: tests preHandle(...). Test data: maintenance mode enabled, an anonymous user requesting an allow-listed path. Expected: the request proceeds (returns true). */
  @Test
  public void testPreHandleAllowsWhitelistedPath() throws Exception {
    setMaintenanceMode("1");
    MockHttpServletRequest request = requestFor("/accounts/login");
    MockHttpServletResponse response = new MockHttpServletResponse();

    Assertions.assertTrue(interceptor.preHandle(request, response, new Object()));
    Assertions.assertNull(response.getForwardedUrl());
  }

  /** Test case: tests preHandle(...). Test data: maintenance mode enabled, an administrator requesting a normal path. Expected: the request proceeds (returns true). */
  @Test
  public void testPreHandleAllowsAdministrator() throws Exception {
    setMaintenanceMode("1");
    authenticateAs("administrators");
    MockHttpServletRequest request = requestFor("/problems");
    MockHttpServletResponse response = new MockHttpServletResponse();

    Assertions.assertTrue(interceptor.preHandle(request, response, new Object()));
    Assertions.assertNull(response.getForwardedUrl());
  }

  /** Test case: tests preHandle(...). Test data: maintenance mode enabled, a non-administrator requesting a normal path. Expected: the request is blocked (returns false), forwarded to the maintenance page with a 503 status. */
  @Test
  public void testPreHandleBlocksRegularUser() throws Exception {
    setMaintenanceMode("1");
    authenticateAs("users");
    MockHttpServletRequest request = requestFor("/problems");
    MockHttpServletResponse response = new MockHttpServletResponse();

    Assertions.assertFalse(interceptor.preHandle(request, response, new Object()));
    Assertions.assertEquals(HttpServletResponse.SC_SERVICE_UNAVAILABLE, response.getStatus());
    Assertions.assertEquals("/maintenance", response.getForwardedUrl());
  }

  /** Stubs the maintenance-mode option to the given raw value. */
  private void setMaintenanceMode(String value) {
    Option option = new Option("maintenanceMode", value, false);
    when(optionMapper.getOption("maintenanceMode")).thenReturn(option);
  }

  /** Places an authenticated user belonging to the given group into the security context. */
  private void authenticateAs(String userGroupSlug) {
    UserGroup userGroup = new UserGroup(1, userGroupSlug, userGroupSlug);
    User user = new User(1000, "tester", "password", "tester@verwandlung.org", userGroup, null);
    VojUserDetails userDetails = new VojUserDetails(user);
    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities()));
  }

  /** Builds a mock request whose path (relative to an empty context) is the given URI. */
  private MockHttpServletRequest requestFor(String uri) {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setContextPath("");
    request.setRequestURI(uri);
    return request;
  }

  /** The mocked OptionMapper resolving the maintenance-mode flag. */
  private OptionMapper optionMapper;

  /** The MaintenanceModeInterceptor object under test. */
  private MaintenanceModeInterceptor interceptor;
}
