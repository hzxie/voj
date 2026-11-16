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
package org.verwandlung.voj.web.config;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.web.SpringJUnitWebConfig;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import org.verwandlung.voj.web.model.User;
import org.verwandlung.voj.web.model.UserGroup;
import org.verwandlung.voj.web.model.VojUserDetails;
import org.verwandlung.voj.web.service.UserService;

/**
 * Exercises the real {@link SecurityConfig} filter chain against stub endpoints: the per-request
 * re-validation of the logged-in user must run before the authorization checks, and logging out must
 * require a CSRF token.
 *
 * @author Haozhe Xie
 */
@SpringJUnitWebConfig(classes = {SecurityConfig.class, SecurityFilterChainTest.TestConfig.class})
@ActiveProfiles(SecurityFilterChainTest.PROFILE)
public class SecurityFilterChainTest {
  /**
   * The profile guarding the nested configuration and stub controller: test-spring-context.xml
   * component-scans the whole org.verwandlung.voj.web package, test classes included, and must not
   * pick them up.
   */
  static final String PROFILE = "security-filter-chain-test";

  /** Builds a MockMvc that runs the Spring Security filter chain. */
  @BeforeEach
  public void setUp() {
    when(userService.isAllowedToAccess(any(User.class)))
        .thenAnswer(
            invocation -> {
              String slug = ((User) invocation.getArgument(0)).getUserGroup().getUserGroupSlug();
              return "users".equals(slug) || "administrators".equals(slug);
            });
    mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
  }

  /** Test case: an administrator whose group is unchanged. Expected: the administration is reachable. */
  @Test
  public void testAdministratorReachesAdministration() throws Exception {
    MockHttpSession session = sessionOf(user("administrators"));
    when(userService.getUserUsingUid(1000L)).thenReturn(user("administrators"));

    mockMvc.perform(get("/administration").session(session)).andExpect(status().isOk());
  }

  /** Test case: an administrator demoted after logging in. Expected: the administration is denied on the very next request. */
  @Test
  public void testDemotedAdministratorIsDenied() throws Exception {
    MockHttpSession session = sessionOf(user("administrators"));
    when(userService.getUserUsingUid(1000L)).thenReturn(user("users"));

    mockMvc.perform(get("/administration").session(session)).andExpect(status().isForbidden());
    Assertions.assertFalse(session.isInvalid());
  }

  /** Test case: a user banned after logging in. Expected: the session is terminated and the user is sent to the login page. */
  @Test
  public void testBannedUserIsLoggedOut() throws Exception {
    MockHttpSession session = sessionOf(user("users"));
    when(userService.getUserUsingUid(1000L)).thenReturn(user("forbidden"));

    mockMvc
        .perform(get("/accounts/changePassword.action").session(session))
        .andExpect(status().is3xxRedirection());
    Assertions.assertTrue(session.isInvalid());
  }

  /**
   * Test case: the user's password changed (in another session, by a reset or by an administrator)
   * after this session logged in. Expected: the session is terminated on its next request.
   */
  @Test
  public void testSessionEndsWhenPasswordChanged() throws Exception {
    MockHttpSession session = sessionOf(user("users"));
    User freshUser = user("users");
    freshUser.setPassword("{bcrypt}new-password-hash");
    when(userService.getUserUsingUid(1000L)).thenReturn(freshUser);

    mockMvc
        .perform(get("/accounts/changePassword.action").session(session))
        .andExpect(redirectedUrl("/accounts/login"));
    Assertions.assertTrue(session.isInvalid());
  }

  /**
   * Test case: the live judge-result stream is opened by an EventSource, which can only send a GET
   * and cannot add a CSRF token. Expected: a logged-in GET without a token is served.
   */
  @Test
  public void testRealTimeJudgeResultNeedsNoCsrfToken() throws Exception {
    MockHttpSession session = sessionOf(user("users"));
    when(userService.getUserUsingUid(1000L)).thenReturn(user("users"));

    mockMvc
        .perform(
            get("/submission/getRealTimeJudgeResult.action")
                .param("submissionId", "1000")
                .session(session))
        .andExpect(status().isOk());
  }

  /** Test case: an anonymous request to resend the verification email. Expected: a redirect to the login page. */
  @Test
  public void testResendVerificationEmailRequiresLogin() throws Exception {
    mockMvc
        .perform(post("/accounts/resendVerificationEmail.action").with(csrf()))
        .andExpect(redirectedUrl("/accounts/login"));
  }

  /** Test case: a logout POST without a CSRF token (e.g. a cross-site form). Expected: it is rejected. */
  @Test
  public void testLogoutRequiresCsrfToken() throws Exception {
    MockHttpSession session = sessionOf(user("users"));
    when(userService.getUserUsingUid(1000L)).thenReturn(user("users"));

    mockMvc.perform(post("/accounts/logout").session(session)).andExpect(status().isForbidden());
    Assertions.assertFalse(session.isInvalid());
  }

  /**
   * Test case: registering a contestant through the contest editor
   * (/administration/addContestant.action) as a regular user, as an administrator without a CSRF
   * token and as an administrator with one. Expected: only the last one reaches the action.
   */
  @Test
  public void testAddingContestantsRequiresAnAdministratorAndCsrfToken() throws Exception {
    MockHttpSession userSession = sessionOf(user("users"));
    when(userService.getUserUsingUid(1000L)).thenReturn(user("users"));
    mockMvc
        .perform(post("/administration/addContestant.action").with(csrf()).session(userSession))
        .andExpect(status().isForbidden());

    MockHttpSession administratorSession = sessionOf(user("administrators"));
    when(userService.getUserUsingUid(1000L)).thenReturn(user("administrators"));
    mockMvc
        .perform(post("/administration/addContestant.action").session(administratorSession))
        .andExpect(status().isForbidden());
    mockMvc
        .perform(
            post("/administration/addContestant.action").with(csrf()).session(administratorSession))
        .andExpect(status().isOk());
  }

  /** Test case: an anonymous request to an authenticated endpoint. Expected: a redirect to the login page. */
  @Test
  public void testAnonymousUserIsRedirectedToLogin() throws Exception {
    mockMvc
        .perform(get("/accounts/changePassword.action"))
        .andExpect(redirectedUrl("/accounts/login"));
  }

  /** Test case: any response. Expected: it carries the Content-Security-Policy and still denies framing. */
  @Test
  public void testResponsesCarrySecurityHeaders() throws Exception {
    mockMvc
        .perform(get("/accounts/changePassword.action"))
        .andExpect(header().string("Content-Security-Policy", SecurityConfig.CONTENT_SECURITY_POLICY))
        .andExpect(header().string("X-Frame-Options", "DENY"));
  }

  /** Builds a session holding a security context for the given user, as the login stores it. */
  private static MockHttpSession sessionOf(User user) {
    VojUserDetails userDetails = new VojUserDetails(user);
    SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
    securityContext.setAuthentication(
        UsernamePasswordAuthenticationToken.authenticated(
            userDetails, null, userDetails.getAuthorities()));
    MockHttpSession session = new MockHttpSession();
    session.setAttribute(
        HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, securityContext);
    return session;
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

  /** The test application context: Spring MVC, Spring Security and stub endpoints. */
  @Configuration
  @EnableWebMvc
  @EnableWebSecurity
  @Profile(PROFILE)
  static class TestConfig {
    @Bean
    public StubController stubController() {
      return new StubController();
    }
  }

  /** Stub endpoints standing in for the real controllers. */
  @RestController
  @Profile(PROFILE)
  static class StubController {
    @GetMapping("/administration")
    public String administration() {
      return "administration";
    }

    @GetMapping("/accounts/changePassword.action")
    public String changePassword() {
      return "changePassword";
    }

    @PostMapping("/accounts/logout")
    public String logout() {
      return "logout";
    }

    @GetMapping("/submission/getRealTimeJudgeResult.action")
    public String realTimeJudgeResult() {
      return "stream";
    }

    @PostMapping("/accounts/resendVerificationEmail.action")
    public String resendVerificationEmail() {
      return "resent";
    }

    @PostMapping("/administration/addContestant.action")
    public String addContestant() {
      return "added";
    }
  }

  /** The web application context. */
  @Autowired private WebApplicationContext context;

  /** The mocked UserService. */
  @MockitoBean private UserService userService;

  /** The MockMvc running the security filter chain. */
  private MockMvc mockMvc;
}
