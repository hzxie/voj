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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.i18n.SessionLocaleResolver;

import org.verwandlung.voj.web.model.Option;
import org.verwandlung.voj.web.model.User;
import org.verwandlung.voj.web.model.UserGroup;
import org.verwandlung.voj.web.model.VojUserDetails;
import org.verwandlung.voj.web.service.OptionService;
import org.verwandlung.voj.web.service.SubmissionService;

/**
 * The test class for CommonModelPopulator.
 *
 * <p>The collaborators are mocked: the populator only assembles model attributes, so the tests
 * assert which attributes land on the {@link ModelAndView} for anonymous vs authenticated requests.
 *
 * @author Haozhe Xie
 */
public class CommonModelPopulatorTest {
  /** Wires mocked collaborators and stub properties into a fresh populator before every test. */
  @BeforeEach
  public void setUp() {
    optionService = mock(OptionService.class);
    submissionService = mock(SubmissionService.class);
    when(optionService.getAutoloadOptions())
        .thenReturn(List.of(new Option("siteName", "Verwandlung OJ", true)));

    Properties properties = new Properties();
    properties.setProperty("url.cdn", "https://cdn.example.org");
    properties.setProperty("build.version", "20260628");
    properties.setProperty("product.version", "1.2.3");

    populator = new CommonModelPopulator();
    ReflectionTestUtils.setField(populator, "optionService", optionService);
    ReflectionTestUtils.setField(populator, "submissionService", submissionService);
    ReflectionTestUtils.setField(populator, "applicationProperties", properties);
  }

  /** Clears the security context after every test so authentication state does not leak across cases. */
  @AfterEach
  public void tearDown() {
    SecurityContextHolder.clearContext();
  }

  /** Test case: tests populate(...). Test data: an anonymous request (empty security context). Expected: isLogin is false and no myProfile / mySubmissionStats are added; the submission service is never queried. */
  @Test
  public void testPopulateAnonymous() {
    ModelAndView view = new ModelAndView();
    MockHttpServletRequest request = requestFor("/problems");

    populator.populate(view, request, new MockHttpServletResponse());

    Map<String, Object> model = view.getModel();
    Assertions.assertEquals(Boolean.FALSE, model.get("isLogin"));
    Assertions.assertFalse(model.containsKey("myProfile"));
    Assertions.assertFalse(model.containsKey("mySubmissionStats"));
    verify(submissionService, never()).getSubmissionStatsOfUser(ArgumentMatchers.anyLong());
  }

  /** Test case: tests populate(...). Test data: a request whose security context holds an authenticated user. Expected: isLogin is true, myProfile is the user and mySubmissionStats is the user's stats. */
  @Test
  public void testPopulateAuthenticated() {
    UserGroup userGroup = new UserGroup(8, "administrators", "Administrators");
    User user = new User(1000, "zjhzxhz", "password", "cshzxie@gmail.com", userGroup, null);
    VojUserDetails userDetails = new VojUserDetails(user);
    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities()));

    Map<String, Long> stats = Map.of("acceptedSubmission", 2L, "totalSubmission", 3L, "acRate", 66L);
    when(submissionService.getSubmissionStatsOfUser(1000L)).thenReturn(stats);

    ModelAndView view = new ModelAndView();
    MockHttpServletRequest request = requestFor("/problems");

    populator.populate(view, request, new MockHttpServletResponse());

    Map<String, Object> model = view.getModel();
    Assertions.assertEquals(Boolean.TRUE, model.get("isLogin"));
    Assertions.assertSame(user, model.get("myProfile"));
    Assertions.assertSame(stats, model.get("mySubmissionStats"));
  }

  /** Test case: tests populate(...). Test data: any request. Expected: the shared option, CDN/version and forward-URI attributes are populated on the view. */
  @Test
  public void testPopulateAddsSharedAttributes() {
    ModelAndView view = new ModelAndView();
    MockHttpServletRequest request = requestFor("/about");

    populator.populate(view, request, new MockHttpServletResponse());

    Map<String, Object> model = view.getModel();
    Assertions.assertEquals("Verwandlung OJ", model.get("siteName"));
    Assertions.assertEquals("https://cdn.example.org", model.get("cdnUrl"));
    Assertions.assertEquals("20260628", model.get("version"));
    Assertions.assertEquals("1.2.3", model.get("productVersion"));
    Assertions.assertEquals("/about", model.get("forwardUri"));
  }

  /**
   * Builds a GET request for the given URI with a {@link SessionLocaleResolver} attached, which
   * {@code getUserLanguage} needs (it resolves and stores the locale through the request's resolver).
   */
  private static MockHttpServletRequest requestFor(String uri) {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", uri);
    request.setRequestURI(uri);
    request.setAttribute(DispatcherServlet.LOCALE_RESOLVER_ATTRIBUTE, new SessionLocaleResolver());
    return request;
  }

  /** The mocked OptionService the populator reads autoload options from. */
  private OptionService optionService;

  /** The mocked SubmissionService the populator reads the user's stats from. */
  private SubmissionService submissionService;

  /** The CommonModelPopulator object under test. */
  private CommonModelPopulator populator;
}
