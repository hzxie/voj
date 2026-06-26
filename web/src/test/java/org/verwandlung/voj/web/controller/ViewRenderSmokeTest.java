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

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Properties;

import org.mybatis.spring.SqlSessionTemplate;

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
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.DefaultCsrfToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import org.verwandlung.voj.web.interceptor.CommonModelInterceptor;
import org.verwandlung.voj.web.interceptor.CommonModelPopulator;
import org.verwandlung.voj.web.messenger.ApplicationEventListener;
import org.verwandlung.voj.web.model.Option;
import org.verwandlung.voj.web.service.BulletinBoardService;
import org.verwandlung.voj.web.service.ContestService;
import org.verwandlung.voj.web.service.DiscussionService;
import org.verwandlung.voj.web.service.LanguageService;
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
  ViewRenderSmokeTest.TestBeans.class
})
class ViewRenderSmokeTest {
  @Autowired private MockMvc mockMvc;

  @MockitoBean private UserService userService;
  @MockitoBean private LanguageService languageService;
  @MockitoBean private SubmissionService submissionService;
  @MockitoBean private OptionService optionService;
  @MockitoBean private ProblemService problemService;
  @MockitoBean private DiscussionService discussionService;
  @MockitoBean private ContestService contestService;
  @MockitoBean private BulletinBoardService bulletinBoardService;
  @MockitoBean private SessionRegistry sessionRegistry;
  @MockitoBean private ApplicationEventListener applicationEventListener;

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
        "/worldwide",
        "/not-supported",
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
        "/accounts/dashboard",
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
  // Not-yet-implemented admin features.
  //
  // These routes are linked from the admin sidebar but have no controller
  // mapping or template yet (today they fall through to the static-resource
  // handler and 500). Each is a placeholder asserting the eventual rendered
  // state (HTTP 200); once the @RequestMapping in AdministrationController and
  // the corresponding template exist, drop the @Disabled to turn it into an
  // acceptance test. They double as a backlog: each shows up as a skipped test.
  // ---------------------------------------------------------------------------

  @Disabled("not implemented: admin 'all discussions' list — enable with the controller route + template")
  @Test
  void allDiscussionViewRenders() throws Exception {
    mockMvc.perform(getWithCsrf("/administration/all-discussion")).andExpect(status().isOk());
  }

  @Disabled("not implemented: admin 'discussion topics' page — enable with the controller route + template")
  @Test
  void discussionTopicsViewRenders() throws Exception {
    mockMvc.perform(getWithCsrf("/administration/discussion-topics")).andExpect(status().isOk());
  }

  @Disabled("not implemented: admin 'all contests' list — enable with the controller route + template")
  @Test
  void allContestsViewRenders() throws Exception {
    mockMvc.perform(getWithCsrf("/administration/all-contests")).andExpect(status().isOk());
  }

  @Disabled("not implemented: admin 'new contest' page — enable with the controller route + template")
  @Test
  void newContestViewRenders() throws Exception {
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
