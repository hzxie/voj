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

import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Properties;

import org.mybatis.spring.SqlSessionTemplate;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.DefaultCsrfToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import org.verwandlung.voj.web.interceptor.CommonModelInterceptor;
import org.verwandlung.voj.web.interceptor.CommonModelPopulator;
import org.verwandlung.voj.web.interceptor.MaintenanceModeInterceptor;
import org.verwandlung.voj.web.mapper.CheckpointMapper;
import org.verwandlung.voj.web.mapper.OptionMapper;
import org.verwandlung.voj.web.mapper.ProblemMapper;
import org.verwandlung.voj.web.messenger.ApplicationEventListener;
import org.verwandlung.voj.web.model.Option;
import org.verwandlung.voj.web.model.User;
import org.verwandlung.voj.web.model.UserGroup;
import org.verwandlung.voj.web.model.VojUserDetails;
import org.verwandlung.voj.web.service.BulletinBoardService;
import org.verwandlung.voj.web.service.ContestService;
import org.verwandlung.voj.web.service.DiscussionService;
import org.verwandlung.voj.web.service.LanguageService;
import org.verwandlung.voj.web.service.OffensiveWordImportService;
import org.verwandlung.voj.web.service.OffensiveWordService;
import org.verwandlung.voj.web.service.OptionService;
import org.verwandlung.voj.web.service.ProblemService;
import org.verwandlung.voj.web.service.SubmissionService;
import org.verwandlung.voj.web.service.UserService;

/**
 * Render smoke tests for the Thymeleaf views.
 *
 * <p>Loads only the web layer ({@link WebMvcTest}) with every service mocked, then performs a real
 * GET against each view route and asserts the template renders (HTTP 200). Because a Thymeleaf
 * parse / SpEL evaluation error during rendering propagates out of {@code MockMvc.perform}, this
 * suite catches exactly the class of migration regressions that only surface at render time -- e.g.
 * the {@code |...|} literal-substitution, the {@code [[...]]} inlining and the {@code null}-into-
 * boolean / null-map-index SpEL failures fixed during the JSP -> Thymeleaf migration -- without
 * needing a database.
 *
 * <p>The shared page model (system options, CDN/version metadata, {@code isLogin}, etc.) is supplied
 * by {@link CommonModelInterceptor} / {@link CommonModelPopulator}, imported here and fed by the
 * mocked services.
 *
 * <p>TODO (follow-up): the routes in {@link #detailAndAuthenticatedViewsRender} and
 * {@link #errorViewsRender} are {@link Disabled} for now. They need either a representative mocked
 * entity (problem / contest / thread / user) or an authenticated session ({@code
 * HttpSessionParser.getCurrentUser()}), plus -- for the admin pages -- the security context. Fill in
 * the fixtures and remove the {@code @Disabled} markers.
 *
 * @author Haozhe Xie
 */
@WebMvcTest
@AutoConfigureMockMvc(addFilters = false)
@Import({
  CommonModelInterceptor.class,
  CommonModelPopulator.class,
  MaintenanceModeInterceptor.class,
  ViewRenderSmokeTest.TestBeans.class
})
class ViewRenderSmokeTest {
  @Autowired private MockMvc mockMvc;

  @MockitoBean private UserService userService;
  @MockitoBean private LanguageService languageService;
  @MockitoBean private SubmissionService submissionService;
  @MockitoBean private OptionService optionService;
  @MockitoBean private OptionMapper optionMapper;
  @MockitoBean private OffensiveWordService offensiveWordService;
  @MockitoBean private OffensiveWordImportService offensiveWordImportService;
  @MockitoBean private ProblemService problemService;
  @MockitoBean private DiscussionService discussionService;
  @MockitoBean private ContestService contestService;
  @MockitoBean private BulletinBoardService bulletinBoardService;
  @MockitoBean private SessionRegistry sessionRegistry;
  @MockitoBean private ApplicationEventListener applicationEventListener;
  // CheckpointApiController is a @RestController, so the @WebMvcTest slice picks it up; unlike the
  // other controllers it injects these mappers directly rather than going through a (mocked) service.
  @MockitoBean private ProblemMapper problemMapper;
  @MockitoBean private CheckpointMapper checkpointMapper;

  /** A fixed CSRF token so templates referencing {@code ${_csrf.token}} render. */
  private static final CsrfToken CSRF =
      new DefaultCsrfToken("X-CSRF-TOKEN", "_csrf", "test-csrf-token");

  @BeforeEach
  void stubSharedModel() {
    // Options consumed by the shared header / footer fragments.
    when(optionService.getAutoloadOptions())
        .thenReturn(
            List.of(
                new Option("websiteName", "Verwandlung Online Judge", true),
                new Option("description", "VOJ", true),
                new Option("copyright", "&copy; 2026", true),
                new Option("googleAnalyticsCode", "", true),
                new Option("icpNumber", "", true),
                new Option("policeIcpNumber", "", true)));
    // The registration page reads this option's value in the controller.
    when(optionService.getOption("allowUserRegister"))
        .thenReturn(new Option("allowUserRegister", "1", false));
    // Paginated list controllers resolve their page size via getIntOption; return the default.
    when(optionService.getIntOption(anyString(), anyInt()))
        .thenAnswer(invocation -> invocation.getArgument(1));
  }

  @AfterEach
  void clearSecurityContext() {
    SecurityContextHolder.clearContext();
  }

  /**
   * Authenticates an administrator in the Spring Security context so the admin chrome (which reads
   * {@code myProfile} via {@code HttpSessionParser.getCurrentUser()}) renders. Admin views require a
   * logged-in user; public views do not, so this is opt-in per test.
   */
  private void authenticateAdmin() {
    User admin = new User();
    admin.setUid(1000);
    admin.setUsername("root");
    admin.setUserGroup(new UserGroup(8, "administrators", "Administrators"));
    VojUserDetails principal = new VojUserDetails(admin);
    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken(
                principal, "", principal.getAuthorities()));
  }

  /**
   * Public pages that render with no per-request entity and no authenticated user. These exercise
   * the shared master layout and the header / footer fragments.
   */
  @ParameterizedTest(name = "GET {0} renders")
  @ValueSource(
      strings = {
        "/",
        "/terms",
        "/privacy",
        "/judgers",
        "/help",
        "/about",
        "/not-supported",
        "/maintenance",
        "/p",
        "/submission",
        "/contest",
        "/discussion",
        "/accounts/login",
        "/accounts/register",
        "/accounts/reset-password"
      })
  void publicViewsRender(String url) throws Exception {
    mockMvc.perform(getWithCsrf(url)).andExpect(status().isOk());
  }

  /**
   * Detail / form / admin pages. Disabled until fixtures (a mocked entity and / or an authenticated
   * session) are supplied; see the class-level TODO.
   */
  @Disabled("TODO: provide mocked entities and an authenticated session, then enable")
  @ParameterizedTest(name = "GET {0} renders")
  @ValueSource(
      strings = {
        "/p/1000",
        "/p/1000/solution",
        "/accounts/user/1000",
        "/contest/1",
        "/contest/1/leaderboard",
        "/discussion/1",
        "/discussion/new",
        "/administration",
        "/administration/all-users",
        "/administration/edit-user/1000",
        "/administration/new-user",
        "/administration/all-problems",
        "/administration/new-problem",
        "/administration/edit-problem/1000",
        "/administration/problem-categories",
        "/administration/all-submissions",
        "/administration/edit-submission/1000",
        "/administration/general-settings",
        "/administration/language-settings"
      })
  void detailAndAuthenticatedViewsRender(String url) throws Exception {
    mockMvc.perform(getWithCsrf(url)).andExpect(status().isOk());
  }

  /**
   * Error pages. Disabled for now (the user will complete this): assert that an unmapped URL yields
   * the 404 view and that a handler exception yields the 500 view, both rendering without error.
   */
  @Disabled("TODO: assert 404 / 500 views render with the expected status")
  @ParameterizedTest(name = "GET {0} renders the error view")
  @ValueSource(strings = {"/this-route-does-not-exist"})
  void errorViewsRender(String url) throws Exception {
    mockMvc.perform(getWithCsrf(url)).andExpect(status().isNotFound());
  }

  // ---------------------------------------------------------------------------
  // Admin list / form pages. Each route + template now exists, so these render
  // against the admin layout with the page's service calls stubbed to empty
  // collections (the controllers iterate those lists, so a null mock would NPE
  // before reaching the template).
  // ---------------------------------------------------------------------------

  /**
   * Maintenance mode: when the option is on, an anonymous visitor is forwarded to the maintenance
   * page with a 503 status, while an administrator is let through.
   */
  @Test
  void maintenanceModeBlocksAnonymousButAllowsAdmin() throws Exception {
    when(optionMapper.getOption("maintenanceMode"))
        .thenReturn(new Option("maintenanceMode", "1", false));

    // Anonymous visitor (no authentication) is forwarded to the maintenance page with 503.
    mockMvc
        .perform(getWithCsrf("/"))
        .andExpect(status().isServiceUnavailable())
        .andExpect(forwardedUrl("/maintenance"));

    // The maintenance page itself and the informational pages linked from the footer stay reachable.
    mockMvc.perform(getWithCsrf("/maintenance")).andExpect(status().isOk());
    mockMvc.perform(getWithCsrf("/about")).andExpect(status().isOk());
    mockMvc.perform(getWithCsrf("/help")).andExpect(status().isOk());

    // Administrators are let through to the rest of the site.
    authenticateAdmin();
    mockMvc.perform(getWithCsrf("/")).andExpect(status().isOk());
  }

  @Test
  void allDiscussionViewRenders() throws Exception {
    authenticateAdmin();
    when(discussionService.getAllDiscussionThreads(anyLong(), anyInt())).thenReturn(List.of());
    mockMvc.perform(getWithCsrf("/administration/all-discussion")).andExpect(status().isOk());
  }

  @Test
  void discussionTopicsViewRenders() throws Exception {
    authenticateAdmin();
    when(discussionService.getDiscussionTopics()).thenReturn(List.of());
    mockMvc.perform(getWithCsrf("/administration/discussion-topics")).andExpect(status().isOk());
  }

  @Test
  void allContestsViewRenders() throws Exception {
    authenticateAdmin();
    when(contestService.getContests(anyString(), anyLong(), anyInt())).thenReturn(List.of());
    mockMvc.perform(getWithCsrf("/administration/all-contests")).andExpect(status().isOk());
  }

  @Test
  void newContestViewRenders() throws Exception {
    authenticateAdmin();
    when(problemService.getProblemsUsingFilters(
            anyLong(), anyString(), anyString(), anyString(), anyString(), anyBoolean(), anyInt()))
        .thenReturn(List.of());
    mockMvc.perform(getWithCsrf("/administration/new-contest")).andExpect(status().isOk());
  }

  /** Builds a GET that carries a CSRF token request attribute for templates that reference it. */
  private static MockHttpServletRequestBuilder getWithCsrf(String url) {
    return get(url).requestAttr("_csrf", CSRF).requestAttr(CsrfToken.class.getName(), CSRF);
  }

  /**
   * Beans the sliced context needs but the web slice does not provide: the {@code
   * propertyConfigurer} properties (consumed by CommonModelPopulator) and a stub {@link
   * SqlSessionFactory}. The MyBatis {@code @MapperScan} registers mapper beans even in a
   * {@code @WebMvcTest} slice; since the data layer is mocked, the mappers are never invoked, so a
   * stub factory (with a throwaway Configuration) is enough to let them instantiate.
   */
  @TestConfiguration
  static class TestBeans {
    @Bean(name = "propertyConfigurer")
    Properties propertyConfigurer() {
      Properties properties = new Properties();
      properties.setProperty("url.cdn", "/assets");
      properties.setProperty("build.version", "test");
      properties.setProperty("product.version", "0.0.0-test");
      return properties;
    }

    @Bean
    SqlSessionTemplate sqlSessionTemplate() {
      SqlSessionTemplate template = org.mockito.Mockito.mock(SqlSessionTemplate.class);
      when(template.getConfiguration()).thenReturn(new org.apache.ibatis.session.Configuration());
      return template;
    }
  }
}
