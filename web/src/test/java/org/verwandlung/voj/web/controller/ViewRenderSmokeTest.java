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
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.mybatis.spring.SqlSessionTemplate;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
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
import org.verwandlung.voj.web.util.DateFormatter;
import org.verwandlung.voj.web.mapper.CheckpointMapper;
import org.verwandlung.voj.web.mapper.OptionMapper;
import org.verwandlung.voj.web.mapper.ProblemMapper;
import org.verwandlung.voj.web.messenger.ApplicationEventListener;
import org.verwandlung.voj.web.model.BulletinBoardMessage;
import org.verwandlung.voj.web.model.Contest;
import org.verwandlung.voj.web.model.DiscussionReply;
import org.verwandlung.voj.web.model.DiscussionThread;
import org.verwandlung.voj.web.model.DiscussionTopic;
import org.verwandlung.voj.web.model.JudgeResult;
import org.verwandlung.voj.web.model.Language;
import org.verwandlung.voj.web.model.Option;
import org.verwandlung.voj.web.model.Problem;
import org.verwandlung.voj.web.model.ProblemDifficulty;
import org.verwandlung.voj.web.model.Submission;
import org.verwandlung.voj.web.model.User;
import org.verwandlung.voj.web.model.UserGroup;
import org.verwandlung.voj.web.model.VojUserDetails;
import org.verwandlung.voj.web.service.BulletinBoardService;
import org.verwandlung.voj.web.service.ContestService;
import org.verwandlung.voj.web.service.DiscussionService;
import org.verwandlung.voj.web.service.JudgerService;
import org.verwandlung.voj.web.service.LanguageService;
import org.verwandlung.voj.web.service.OffensiveWordImportService;
import org.verwandlung.voj.web.service.OffensiveWordService;
import org.verwandlung.voj.web.service.OptionService;
import org.verwandlung.voj.web.service.ProblemService;
import org.verwandlung.voj.web.service.SubmissionService;
import org.verwandlung.voj.web.service.TurnstileService;
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
  DateFormatter.class,
  ViewRenderSmokeTest.TestBeans.class
})
class ViewRenderSmokeTest {
  @Autowired private MockMvc mockMvc;

  @MockitoBean private UserService userService;
  @MockitoBean private TurnstileService turnstileService;
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
  @MockitoBean private JudgerService judgerService;
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
    // The registration page reads this option in the controller.
    when(optionService.isOptionEnabled("allowUserRegister", true)).thenReturn(true);
    // Paginated list controllers resolve their page size via getIntOption; return the default.
    when(optionService.getIntOption(anyString(), anyInt()))
        .thenAnswer(invocation -> invocation.getArgument(1));
    // The user edit / new forms index languages[0]; expose at least one language.
    when(languageService.getAllLanguages())
        .thenReturn(List.of(new Language(1, "java", "Java", "javac Main.java", "java Main")));
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
    authenticate(admin);
  }

  /** Installs the given user as the authenticated principal in the Spring Security context. */
  private void authenticate(User user) {
    VojUserDetails principal = new VojUserDetails(user);
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
   * Administration list / form pages that render against the admin layout with their service calls
   * resolving to empty collections (the Mockito default). They need an authenticated administrator —
   * the admin chrome reads {@code myProfile} via {@code HttpSessionParser.getCurrentUser()} — but no
   * per-request entity, so a single parameterised test covers them all. The edit / detail admin pages
   * (which load and dereference a specific entity) are covered individually below.
   */
  @ParameterizedTest(name = "GET {0} renders")
  @ValueSource(
      strings = {
        "/administration",
        "/administration/all-users",
        "/administration/new-user",
        "/administration/all-problems",
        "/administration/new-problem",
        "/administration/problem-categories",
        "/administration/all-submissions",
        "/administration/general-settings",
        "/administration/judgers",
        "/administration/all-bulletins",
        "/administration/new-bulletin",
        "/administration/moderation"
      })
  void adminListViewsRender(String url) throws Exception {
    authenticateAdmin();
    mockMvc.perform(getWithCsrf(url)).andExpect(status().isOk());
  }

  // ---------------------------------------------------------------------------
  // Error pages. A request whose target entity is missing raises
  // ResourceNotFoundException (handled into the 404 view); an unexpected failure
  // in a handler is caught by the catch-all @ExceptionHandler (the 500 view).
  // ---------------------------------------------------------------------------

  /** The 404 view renders when a detail route's entity does not exist. */
  @Test
  void notFoundViewRenders() throws Exception {
    when(problemService.getProblem(anyLong())).thenReturn(null);
    mockMvc.perform(getWithCsrf("/p/999999")).andExpect(status().isNotFound());
  }

  /** An unmapped URL renders the 404 view with a 404 status, so it is not indexed as a page. */
  @Test
  void unmappedUrlReturnsNotFound() throws Exception {
    mockMvc.perform(getWithCsrf("/no-such-page")).andExpect(status().isNotFound());
  }

  /** The 500 view renders when a handler fails unexpectedly. */
  @Test
  void internalServerErrorViewRenders() throws Exception {
    when(userService.getUserGroupUsingSlug("judgers"))
        .thenThrow(new RuntimeException("boom"));
    mockMvc.perform(getWithCsrf("/judgers")).andExpect(status().isInternalServerError());
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
    when(optionService.isOptionEnabled("maintenanceMode", false)).thenReturn(true);

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
    when(contestService.getContests(anyString(), anyBoolean(), anyLong(), anyInt()))
        .thenReturn(List.of());
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

  // ---------------------------------------------------------------------------
  // Detail / edit pages. Each loads and dereferences a specific entity, so the
  // owning service call is stubbed with a representative fixture (a null would
  // raise ResourceNotFoundException before the template is reached).
  // ---------------------------------------------------------------------------

  /** The public problem statement page renders for a published problem. */
  @Test
  void problemViewRenders() throws Exception {
    when(problemService.getProblem(1000L)).thenReturn(aPublishedProblem(1000L));
    mockMvc
        .perform(getWithCsrf("/p/1000"))
        .andExpect(status().isOk())
        .andExpect(
            content()
                .string(containsString("<meta name=\"description\" content=\"Compute a + b.\"")))
        .andExpect(
            content()
                .string(containsString("<meta property=\"og:title\" content=\"P1000 A + B Problem\"")))
        .andExpect(
            content()
                .string(containsString(
                    "<link rel=\"canonical\" href=\"https://oj.example.edu/voj/p/1000\"")))
        .andExpect(content().string(not(containsString("hreflang"))));
  }

  /**
   * The contest problem page renders for a logged-in contestant and its editor posts to the
   * contest submission endpoint, while the normal problem page posts to the problem one.
   */
  @Test
  void contestProblemViewPostsToContestEndpoint() throws Exception {
    authenticateUser();
    Contest contest = aContest(1L, "ACM", past(), future());
    contest.setProblems("[1000]");
    when(contestService.getContest(1L)).thenReturn(contest);
    when(contestService.isContestProblemsViewable(any(), any())).thenReturn(true);
    when(problemService.getProblem(1000L)).thenReturn(aPublishedProblem(1000L));
    when(languageService.getEnabledLanguages())
        .thenReturn(List.of(new Language(1, "java", "Java", "javac Main.java", "java Main")));

    mockMvc
        .perform(getWithCsrf("/contest/1/p/1000"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("\\/contest\\/1\\/createSubmission.action")))
        .andExpect(content().string(not(containsString("\\/p\\/createSubmission.action"))));
    mockMvc
        .perform(getWithCsrf("/p/1000"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("\\/p\\/createSubmission.action")))
        .andExpect(content().string(not(containsString("\\/contest\\/1\\/createSubmission.action"))));
  }

  /** A page whose content is the translated UI lists an alternate URL per display language. */
  @Test
  void homeViewListsLanguageAlternates() throws Exception {
    mockMvc
        .perform(getWithCsrf("/").param("language", "zh_CN"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString(" lang=\"zh-CN\"")))
        .andExpect(
            content()
                .string(containsString(
                    "<link rel=\"canonical\" href=\"https://oj.example.edu/voj/?language=zh_CN\"")))
        .andExpect(
            content()
                .string(containsString(
                    "hreflang=\"ja\" href=\"https://oj.example.edu/voj/?language=ja_JP\"")))
        .andExpect(
            content()
                .string(containsString(
                    "hreflang=\"x-default\" href=\"https://oj.example.edu/voj/\"")));
  }

  /**
   * The shared UI chrome renders: the color-scheme meta before first paint, the theme toggle as a
   * real button, the popover navigation menu and the dialog behind vojConfirm / vojAlert.
   */
  @Test
  void homeViewRendersSharedChrome() throws Exception {
    mockMvc
        .perform(getWithCsrf("/"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("<meta name=\"color-scheme\" content=\"light dark\"")))
        .andExpect(content().string(containsString("data-vw=\"theme-toggle\" aria-pressed=\"true\"")))
        .andExpect(content().string(containsString("popovertarget=\"vw-menu\" aria-controls=\"vw-menu\"")))
        .andExpect(content().string(containsString("<ul id=\"vw-menu\" class=\"vw-menu\" popover=\"auto\">")))
        .andExpect(content().string(containsString("<dialog id=\"vw-dialog\" class=\"vw-dialog\"")))
        .andExpect(content().string(not(containsString("X-UA-Compatible"))));
  }

  /** The navigation marks the current section with aria-current="page" (and only that one). */
  @Test
  void navigationMarksCurrentPage() throws Exception {
    mockMvc
        .perform(getWithCsrf("/submission"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("href=\"/submission\" aria-current=\"page\"")))
        .andExpect(content().string(not(containsString("href=\"/p\" aria-current"))));
  }

  /** The list pages share the "load more" fragment, with its failure note for createLoadMore. */
  @Test
  void listPagesRenderTheLoadMoreFragment() throws Exception {
    for (String url : new String[] {"/submission", "/contest", "/discussion"}) {
      mockMvc
          .perform(getWithCsrf(url))
          .andExpect(status().isOk())
          .andExpect(content().string(containsString("<div id=\"more-")))
          .andExpect(content().string(containsString(
              "<span class=\"vw-spinner hide\" role=\"status\" aria-label=\"Loading\"></span>")))
          .andExpect(content().string(containsString(
              "<div class=\"vw-loadmore-error\" role=\"alert\""
                  + " data-message=\"Could not load more. Please try again.\"></div>")));
    }
  }

  /** The admin sidebar is a modal {@code <dialog>} drawer on narrow screens, opened by the header button. */
  @Test
  void adminSidebarRendersAsDrawer() throws Exception {
    authenticateAdmin();
    mockMvc
        .perform(getWithCsrf("/administration/all-users"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("<dialog id=\"adm-sidebar\" class=\"adm-drawer\" data-adm=\"sidebar\" closedby=\"any\" role=\"none\"")))
        .andExpect(content().string(containsString("data-adm=\"drawer-close\" autofocus")))
        .andExpect(content().string(containsString("aria-controls=\"adm-sidebar\" aria-haspopup=\"dialog\" aria-expanded=\"false\"")))
        .andExpect(content().string(not(containsString("adm-scrim"))));
  }

  /** robots.txt keeps crawlers out of the administration pages and points to the sitemap. */
  @Test
  void robotsTxtRenders() throws Exception {
    mockMvc
        .perform(get("/robots.txt"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("Disallow: /voj/administration\n")))
        .andExpect(
            content()
                .string(containsString("Sitemap: https://oj.example.edu/voj/sitemap.xml\n")));
  }

  /** sitemap.xml lists the public problems and the published contests. */
  @Test
  void sitemapXmlRenders() throws Exception {
    when(problemService.getIdsOfPublicProblems(anyInt())).thenReturn(List.of(1000L));
    when(contestService.getContests(any(), eq(true), anyLong(), anyInt()))
        .thenReturn(List.of(aContest(1L, "ACM", future(), farFuture())));
    mockMvc
        .perform(get("/sitemap.xml"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("<loc>https://oj.example.edu/voj/p/1000</loc>")))
        .andExpect(
            content()
                .string(containsString("<loc>https://oj.example.edu/voj/contest/1</loc>")));
  }

  /** The auth pages are kept out of search results. */
  @Test
  void loginViewIsNoindex() throws Exception {
    mockMvc
        .perform(getWithCsrf("/accounts/login"))
        .andExpect(content().string(containsString("<meta name=\"robots\" content=\"noindex\"")));
  }

  /** The problem editorial (solution) page reuses the discussion-thread template. */
  @Test
  void problemSolutionViewRenders() throws Exception {
    when(problemService.getProblem(1000L)).thenReturn(aPublishedProblem(1000L));
    when(discussionService.getSolutionThreadOfProblem(1000L)).thenReturn(aThread(5000L));
    when(discussionService.isDiscussionThreadViewable(any(), any())).thenReturn(true);
    mockMvc.perform(getWithCsrf("/p/1000/solution")).andExpect(status().isOk());
  }

  /** The solution page of an unpublished problem is not found for non-administrators. */
  @Test
  void draftProblemSolutionIsHiddenFromNonAdministrators() throws Exception {
    Problem draftProblem = aPublishedProblem(1000L);
    draftProblem.setStatus("DRAFT");
    when(problemService.getProblem(1000L)).thenReturn(draftProblem);
    when(discussionService.getSolutionThreadOfProblem(1000L)).thenReturn(aThread(5000L));
    when(discussionService.isDiscussionThreadViewable(any(), any())).thenReturn(true);
    mockMvc.perform(getWithCsrf("/p/1000/solution")).andExpect(status().isNotFound());

    authenticateUser();
    mockMvc.perform(getWithCsrf("/p/1000/solution")).andExpect(status().isNotFound());

    authenticateAdmin();
    mockMvc.perform(getWithCsrf("/p/1000/solution")).andExpect(status().isOk());
  }

  /** The public user profile page renders for a regular (non-judger) user. */
  @Test
  void userProfileViewRenders() throws Exception {
    when(userService.getUserUsingUid(2000L)).thenReturn(aUser(2000L, usersGroup()));
    when(userService.getUserMetaUsingUid(any())).thenReturn(userMeta());
    // submissionStats is intentionally left at its empty default: the template must default the
    // missing acceptance counters to 0 (see user.html's `?: 0`) rather than fail on a null.
    mockMvc.perform(getWithCsrf("/accounts/user/2000")).andExpect(status().isOk());
  }

  /** A profile's email address is shown only to its owner and to administrators. */
  @Test
  void userProfileEmailIsPrivate() throws Exception {
    when(userService.getUserUsingUid(2000L)).thenReturn(aUser(2000L, usersGroup()));
    when(userService.getUserMetaUsingUid(any())).thenReturn(userMeta());
    mockMvc
        .perform(getWithCsrf("/accounts/user/2000"))
        .andExpect(status().isOk())
        .andExpect(content().string(not(containsString("alice@example.com"))));

    authenticate(aUser(3000L, usersGroup()));
    mockMvc
        .perform(getWithCsrf("/accounts/user/2000"))
        .andExpect(status().isOk())
        .andExpect(content().string(not(containsString("alice@example.com"))));

    authenticateUser();
    mockMvc
        .perform(getWithCsrf("/accounts/user/2000"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("alice@example.com")));

    authenticateAdmin();
    mockMvc
        .perform(getWithCsrf("/accounts/user/2000"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("alice@example.com")));
  }

  /** Signing out is a CSRF-protected POST form, not a GET link that a cross-site request could hit. */
  @Test
  void headerSignOutIsCsrfProtectedPost() throws Exception {
    authenticateUser();
    mockMvc
        .perform(getWithCsrf("/"))
        .andExpect(status().isOk())
        .andExpect(content().string(not(containsString("logout=true"))))
        .andExpect(content().string(containsString("action=\"/accounts/logout\"")))
        .andExpect(
            content().string(containsString("name=\"_csrf\" value=\"test-csrf-token\"")));
  }

  /** The contest overview page renders for an upcoming contest (leaderboard block skipped). */
  @Test
  void contestViewRenders() throws Exception {
    when(contestService.getContest(1L))
        .thenReturn(aContest(1L, "ACM", future(), farFuture()));
    mockMvc
        .perform(getWithCsrf("/contest/1"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("id=\"attend-contest-button\"")));
  }

  /**
   * An upcoming contest whose registration is closed offers no "Attend" button (attending would be
   * refused) but says that registration is closed.
   */
  @Test
  void contestWithClosedRegistrationOffersNoAttendButton() throws Exception {
    Contest contest = aContest(1L, "ACM", future(), farFuture());
    contest.setOpenRegistration(false);
    when(contestService.getContest(1L)).thenReturn(contest);
    mockMvc
        .perform(getWithCsrf("/contest/1"))
        .andExpect(status().isOk())
        .andExpect(content().string(not(containsString("id=\"attend-contest-button\""))))
        .andExpect(content().string(containsString("Registration for this contest is closed.")));
  }

  /**
   * A running contest offers no "Attend" button even when its registration is open (a contest can
   * only be attended before it starts) but says that registration is closed.
   */
  @Test
  void liveContestOffersNoAttendButton() throws Exception {
    when(contestService.getContest(1L)).thenReturn(aContest(1L, "ACM", past(), future()));
    mockMvc
        .perform(getWithCsrf("/contest/1"))
        .andExpect(status().isOk())
        .andExpect(content().string(not(containsString("id=\"attend-contest-button\""))))
        .andExpect(content().string(not(containsString("/attend.action"))))
        .andExpect(content().string(containsString("Registration for this contest is closed.")));
  }

  /** An unpublished contest is not found for anonymous users but still renders for administrators. */
  @Test
  void draftContestIsHiddenFromNonAdministrators() throws Exception {
    Contest draftContest = aContest(1L, "ACM", future(), farFuture());
    draftContest.setStatus("DRAFT");
    when(contestService.getContest(1L)).thenReturn(draftContest);
    mockMvc.perform(getWithCsrf("/contest/1")).andExpect(status().isNotFound());

    authenticateAdmin();
    mockMvc.perform(getWithCsrf("/contest/1")).andExpect(status().isOk());
  }

  /** The contest leaderboard renders for a started ACM contest. */
  @Test
  void contestLeaderboardViewRenders() throws Exception {
    when(contestService.getContest(1L)).thenReturn(aContest(1L, "ACM", past(), future()));
    mockMvc.perform(getWithCsrf("/contest/1/leaderboard")).andExpect(status().isOk());
  }

  /**
   * The ACM leaderboard formats the penalty as HH:MM:SS (3725 s = 01:02:05) and shows the freeze
   * notice while the scoreboard is frozen.
   */
  @Test
  void contestLeaderboardFormatsPenaltyAndShowsFreeze() throws Exception {
    Contest contest = aContest(1L, "ACM", past(), future());
    contest.setProblems("[1000]");
    contest.setFreeze(60);
    when(contestService.getContest(1L)).thenReturn(contest);
    when(contestService.getProblemsOfContests(any())).thenReturn(List.of(aPublishedProblem(1000L)));
    org.verwandlung.voj.web.model.ContestContestant contestant =
        new org.verwandlung.voj.web.model.ContestContestant(contest, aUser(2000L, usersGroup()));
    contestant.setTime(3725);
    contestant.setRank(1);
    Submission submission = aSubmission(3000L);
    submission.setUsedTime(125);
    Map<String, Object> leaderBoard = new HashMap<>();
    leaderBoard.put("contestants", List.of(contestant));
    leaderBoard.put("submissions", Map.of(2000L, Map.of(1000L, submission)));
    leaderBoard.put("frozenAt", new Date());
    when(contestService.getLeaderBoardForAcm(eq(1L), any())).thenReturn(leaderBoard);

    mockMvc
        .perform(getWithCsrf("/contest/1/leaderboard"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString(">01</span>:<span>02</span>:<span>05<")))
        .andExpect(content().string(containsString(">00</span>:<span>02</span>:<span>05<")))
        .andExpect(content().string(containsString("last 60 minutes")));
  }

  /** A visible discussion thread renders. */
  @Test
  void discussionThreadViewRenders() throws Exception {
    when(discussionService.getDiscussionThreadForDisplay(5000L)).thenReturn(aThread(5000L));
    when(discussionService.isDiscussionThreadViewable(any(), any())).thenReturn(true);
    mockMvc.perform(getWithCsrf("/discussion/5000")).andExpect(status().isOk());
  }

  /**
   * The thread page tells its script which reply is the opening post (which it offers no delete
   * control for), and its creator gets the edit-thread control only while the thread is unlocked.
   */
  @Test
  void threadPageOffersOnlyTheControlsTheServerAccepts() throws Exception {
    DiscussionThread thread = aThread(5000L);
    when(discussionService.getDiscussionThreadForDisplay(5000L)).thenReturn(thread);
    when(discussionService.isDiscussionThreadViewable(any(), any())).thenReturn(true);
    when(discussionService.getOpeningPostIdOfThread(5000L)).thenReturn(42L);
    authenticateUser();
    mockMvc
        .perform(getWithCsrf("/discussion/5000"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("openingPostId = 42")))
        .andExpect(content().string(containsString("id=\"edit-discussion-btn\"")));

    thread.setLocked(true);
    mockMvc
        .perform(getWithCsrf("/discussion/5000"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("isThreadLocked = true")))
        .andExpect(content().string(not(containsString("id=\"edit-discussion-btn\""))));
  }

  /** User text is stored as plain text: the thread page escapes markup in the title exactly once. */
  @Test
  void discussionThreadTitleIsEscapedOnce() throws Exception {
    DiscussionThread thread = aThread(5000L);
    thread.setDiscussionThreadTitle("A & B <img src=x onerror=alert(1)>");
    when(discussionService.getDiscussionThreadForDisplay(5000L)).thenReturn(thread);
    when(discussionService.isDiscussionThreadViewable(any(), any())).thenReturn(true);
    mockMvc
        .perform(getWithCsrf("/discussion/5000"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("A &amp; B &lt;img src=x onerror=alert(1)&gt;")))
        .andExpect(content().string(not(containsString("<img src=x"))));
  }

  /**
   * Problem statements and contest notes are Markdown sources that the page renders client-side
   * from the element's text: they are written escaped, so "a<b" survives and admin-entered HTML
   * never reaches the DOM as markup.
   */
  @Test
  void markdownSourcesAreEscaped() throws Exception {
    Problem problem = aPublishedProblem(1000L);
    problem.setDescription("If $x<y$ and a<b <img src=x onerror=alert(1)>");
    problem.setInpuFormatt("n<m");
    problem.setOutputFormat("<b>sum</b>");
    problem.setSampleInput("1<2");
    problem.setSampleOutput("<3");
    when(problemService.getProblem(1000L)).thenReturn(problem);
    mockMvc
        .perform(getWithCsrf("/p/1000"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("If $x&lt;y$ and a&lt;b &lt;img src=x onerror=alert(1)&gt;")))
        .andExpect(content().string(containsString(">n&lt;m</div>")))
        .andExpect(content().string(containsString(">&lt;b&gt;sum&lt;/b&gt;</div>")))
        .andExpect(content().string(containsString(">1&lt;2</pre>")))
        .andExpect(content().string(containsString(">&lt;3</pre>")))
        .andExpect(content().string(not(containsString("<img src=x"))));

    Contest contest = aContest(1L, "ACM", future(), farFuture());
    contest.setContestNotes("a<b <script>alert(1)</script>");
    when(contestService.getContest(1L)).thenReturn(contest);
    mockMvc
        .perform(getWithCsrf("/contest/1"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("a&lt;b &lt;script&gt;alert(1)&lt;/script&gt;")))
        .andExpect(content().string(not(containsString("<script>alert(1)"))));
  }

  /**
   * The contest editor's start time is a native datetime-local input, and its problem picker is
   * opened by a real button. The start is rendered as an instant (epoch ms) for the browser to
   * show in the admin's time zone, never as a wall-clock time in the server's zone.
   */
  @Test
  void adminContestEditorUsesNativeControls() throws Exception {
    authenticateAdmin();
    Contest contest = aContest(1L, "ACM", future(), farFuture());
    contest.setStartTime(new Date(1893553440000L));
    when(contestService.getContest(1L)).thenReturn(contest);
    when(contestService.getContestStatus(any())).thenReturn(Contest.CONTEST_STATUS.READY);
    mockMvc
        .perform(getWithCsrf("/administration/edit-contest/1"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString(
            "<input id=\"start-time\" class=\"adm-input\" type=\"datetime-local\" required")))
        .andExpect(content().string(containsString("data-start-time=\"1893553440000\" />")))
        .andExpect(content().string(containsString(
            "<button type=\"button\" id=\"picker-toggle\" class=\"toggle\" aria-expanded=\"false\" aria-controls=\"picker-panel\">")))
        .andExpect(content().string(not(containsString("dt-trigger"))));
  }

  /** The judge log carries raw compiler output, so both submission pages must escape it. */
  @Test
  void judgeLogIsEscaped() throws Exception {
    Submission submission = aSubmission(7000L);
    submission.setJudgeLog("#error <img src=x onerror=alert(1)>");
    when(submissionService.getSubmission(7000L)).thenReturn(submission);
    when(contestService.isSubmissionVisible(any(), any())).thenReturn(true);
    mockMvc
        .perform(getWithCsrf("/submission/7000"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("#error &lt;img src=x onerror=alert(1)&gt;")))
        .andExpect(content().string(not(containsString("<img src=x"))));

    authenticateAdmin();
    mockMvc
        .perform(getWithCsrf("/administration/edit-submission/7000"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("#error &lt;img src=x onerror=alert(1)&gt;")))
        .andExpect(content().string(not(containsString("<img src=x"))));
  }

  /** The profile escapes the display name and the bio (the bio is rendered as Markdown client-side). */
  @Test
  void userProfileTextIsEscaped() throws Exception {
    Map<String, Object> meta = userMeta();
    meta.put("displayName", "A & B <b>");
    meta.put("aboutMe", "I like <script>alert(1)</script> & vector<int>");
    when(userService.getUserUsingUid(2000L)).thenReturn(aUser(2000L, usersGroup()));
    when(userService.getUserMetaUsingUid(any())).thenReturn(meta);
    mockMvc
        .perform(getWithCsrf("/accounts/user/2000"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("A &amp; B &lt;b&gt;")))
        .andExpect(
            content()
                .string(
                    containsString(
                        "I like &lt;script&gt;alert(1)&lt;/script&gt; &amp; vector&lt;int&gt;")))
        .andExpect(content().string(not(containsString("<script>alert(1)"))));
  }

  /** The profile editor explains a rejected social-links value, in the display language. */
  @Test
  void userProfileExplainsIllegalSocialLinks() throws Exception {
    authenticateUser();
    when(userService.getUserUsingUid(2000L)).thenReturn(aUser(2000L, usersGroup()));
    when(userService.getUserMetaUsingUid(any())).thenReturn(userMeta());
    mockMvc
        .perform(getWithCsrf("/accounts/user/2000"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("if (!result['isSocialLinksLegal'])")))
        .andExpect(content().string(containsString("The social links seem invalid.")));
    // Thymeleaf's JavaScript inlining escapes non-ASCII text; this is "社交账号" escaped.
    mockMvc
        .perform(getWithCsrf("/accounts/user/2000").param("language", "zh_CN"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("\\u793E\\u4EA4\\u8D26\\u53F7")));
  }

  /** The new-thread form renders for a logged-in user. */
  @Test
  void newDiscussionThreadViewRenders() throws Exception {
    authenticateUser();
    mockMvc.perform(getWithCsrf("/discussion/new")).andExpect(status().isOk());
  }

  /** The admin user-edit form renders for an existing user. */
  @Test
  void adminEditUserViewRenders() throws Exception {
    authenticateAdmin();
    when(userService.getUserUsingUid(2000L)).thenReturn(aUser(2000L, usersGroup()));
    when(submissionService.getSolvedCountAndRankOfUser(2000L))
        .thenReturn(Map.of("solvedCount", 3L, "rank", 1L));
    mockMvc.perform(getWithCsrf("/administration/edit-user/2000")).andExpect(status().isOk());
  }

  /**
   * The user create / edit forms render even when no languages are configured and the user has no
   * preferred language set — the template must guard {@code languages[0]} and {@code
   * user.preferLanguage} rather than fail on an empty list / null.
   */
  @Test
  void adminUserFormsRenderWithoutLanguageOrPreference() throws Exception {
    authenticateAdmin();
    when(languageService.getAllLanguages()).thenReturn(List.of());
    User user = new User();
    user.setUid(2000);
    user.setUsername("bob");
    user.setEmail("bob@example.com");
    user.setUserGroup(usersGroup());
    when(userService.getUserUsingUid(2000L)).thenReturn(user);
    // A user who has solved nothing has no rank.
    java.util.Map<String, Long> unranked = new java.util.HashMap<>();
    unranked.put("solvedCount", 0L);
    unranked.put("rank", null);
    when(submissionService.getSolvedCountAndRankOfUser(2000L)).thenReturn(unranked);

    mockMvc.perform(getWithCsrf("/administration/new-user")).andExpect(status().isOk());
    mockMvc.perform(getWithCsrf("/administration/edit-user/2000")).andExpect(status().isOk());
  }

  /** The admin problem-edit form renders for an existing problem. */
  @Test
  void adminEditProblemViewRenders() throws Exception {
    authenticateAdmin();
    when(problemService.getProblem(1000L)).thenReturn(aPublishedProblem(1000L));
    mockMvc.perform(getWithCsrf("/administration/edit-problem/1000")).andExpect(status().isOk());
  }

  /** The admin submission-edit form renders for an existing submission. */
  @Test
  void adminEditSubmissionViewRenders() throws Exception {
    authenticateAdmin();
    when(submissionService.getSubmission(7000L)).thenReturn(aSubmission(7000L));
    mockMvc.perform(getWithCsrf("/administration/edit-submission/7000")).andExpect(status().isOk());
  }

  /** The email-verification result page renders. */
  @Test
  void verifyEmailViewRenders() throws Exception {
    when(userService.verifyEmail(any(), any())).thenReturn(Map.of("isSuccessful", true));
    mockMvc
        .perform(getWithCsrf("/accounts/verify-email?email=a@b.com&token=t"))
        .andExpect(status().isOk());
  }

  /**
   * The login form still sends rememberMe, which the login action no longer declares: the request
   * must keep working (Spring ignores parameters a handler does not bind).
   */
  @Test
  void loginActionAcceptsTheRememberMeTheFormSends() throws Exception {
    when(turnstileService.verify(any(), any())).thenReturn(true);
    when(userService.isAllowedToLogin(eq("zjhzxhz"), eq("secret"), anyString()))
        .thenReturn(new java.util.HashMap<>(Map.of("isSuccessful", false)));
    mockMvc
        .perform(
            org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(
                    "/accounts/login.action")
                .param("username", "zjhzxhz")
                .param("password", "secret")
                .param("rememberMe", "true")
                .with(
                    org.springframework.security.test.web.servlet.request
                        .SecurityMockMvcRequestPostProcessors.csrf()))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("\"isSuccessful\":false")));
  }

  /** The public submission-detail page renders for an existing submission. */
  @Test
  void submissionViewRenders() throws Exception {
    when(submissionService.getSubmission(7000L)).thenReturn(aSubmission(7000L));
    when(contestService.isSubmissionVisible(any(), any())).thenReturn(true);
    mockMvc.perform(getWithCsrf("/submission/7000")).andExpect(status().isOk());
  }

  /**
   * Both submission pages render the execute time as an instant that site.js formats in the
   * viewer's time zone, as the live update that replaces it does, so the time does not jump.
   */
  @Test
  void submissionExecuteTimeIsFormattedInTheBrowser() throws Exception {
    Submission submission = aSubmission(7000L);
    submission.setExecuteTime(new Date(1893553440000L));
    when(submissionService.getSubmission(7000L)).thenReturn(submission);
    when(contestService.isSubmissionVisible(any(), any())).thenReturn(true);
    String executeTime =
        "id=\"execute-time\" data-time=\"1893553440000\" data-format=\"datetime\"";
    mockMvc
        .perform(getWithCsrf("/submission/7000"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString(executeTime)));

    authenticateAdmin();
    mockMvc
        .perform(getWithCsrf("/administration/edit-submission/7000"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString(executeTime)));
  }

  /**
   * A submission the viewer may not see (ContestService#isSubmissionVisible: a submission to an
   * unpublished problem, or another user's during a contest) is not found, so the page reveals
   * neither the verdict nor the problem's name.
   */
  @Test
  void invisibleSubmissionIsNotFound() throws Exception {
    when(submissionService.getSubmission(7000L)).thenReturn(aSubmission(7000L));
    when(contestService.isSubmissionVisible(any(), any())).thenReturn(false);
    authenticateUser();
    mockMvc.perform(getWithCsrf("/submission/7000")).andExpect(status().isNotFound());
  }

  /** The OI-mode leaderboard renders for a started OI contest. */
  @Test
  void contestLeaderboardOiViewRenders() throws Exception {
    when(contestService.getContest(2L)).thenReturn(aContest(2L, "OI", past(), future()));
    mockMvc.perform(getWithCsrf("/contest/2/leaderboard")).andExpect(status().isOk());
  }

  /** The admin bulletin-edit form renders for an existing bulletin. */
  @Test
  void adminEditBulletinViewRenders() throws Exception {
    authenticateAdmin();
    when(bulletinBoardService.getBulletinBoardMessage(9000L)).thenReturn(aBulletin(9000L));
    mockMvc.perform(getWithCsrf("/administration/edit-bulletin/9000")).andExpect(status().isOk());
  }

  /** The admin contest-edit form renders for an existing contest. */
  @Test
  void adminEditContestViewRenders() throws Exception {
    authenticateAdmin();
    when(contestService.getContest(1L)).thenReturn(aContest(1L, "ACM", future(), farFuture()));
    when(contestService.getContestStatus(any())).thenReturn(Contest.CONTEST_STATUS.READY);
    mockMvc.perform(getWithCsrf("/administration/edit-contest/1")).andExpect(status().isOk());
  }

  /** The contest editor lists the contestants (escaped) and offers to register more. */
  @Test
  void adminEditContestListsContestants() throws Exception {
    authenticateAdmin();
    when(contestService.getContest(1L)).thenReturn(aContest(1L, "ACM", future(), farFuture()));
    when(contestService.getContestStatus(any())).thenReturn(Contest.CONTEST_STATUS.READY);
    User contestant = new User();
    contestant.setUid(1002L);
    contestant.setUsername("a<b");
    when(contestService.getContestantsOfContest(1L)).thenReturn(List.of(contestant));
    mockMvc
        .perform(getWithCsrf("/administration/edit-contest/1"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("id=\"contestant-username\"")))
        .andExpect(content().string(containsString("data-uid=\"1002\"")))
        .andExpect(content().string(containsString(">a&lt;b</span>")))
        .andExpect(content().string(not(containsString("a<b"))));
  }

  /** The admin thread-edit form renders for an existing thread. */
  @Test
  void adminEditThreadViewRenders() throws Exception {
    authenticateAdmin();
    when(discussionService.getDiscussionThreadUsingThreadId(5000L)).thenReturn(aThread(5000L));
    mockMvc.perform(getWithCsrf("/administration/edit-thread/5000")).andExpect(status().isOk());
  }

  /** The admin moderation-review page renders for a reported reply. */
  @Test
  void adminModerationReviewViewRenders() throws Exception {
    authenticateAdmin();
    when(discussionService.getDiscussionReplyUsingReplyId(8000L)).thenReturn(aReply(8000L, 5000L));
    when(discussionService.getDiscussionThreadUsingThreadId(5000L)).thenReturn(aThread(5000L));
    mockMvc.perform(getWithCsrf("/administration/moderation/8000")).andExpect(status().isOk());
  }

  // --- Fixtures -------------------------------------------------------------

  private static UserGroup usersGroup() {
    return new UserGroup(3, "users", "Users");
  }

  private void authenticateUser() {
    authenticate(aUser(2000L, usersGroup()));
  }

  private static User aUser(long uid, UserGroup group) {
    User user = new User();
    user.setUid(uid);
    user.setUsername("alice");
    user.setEmail("alice@example.com");
    user.setUserGroup(group);
    user.setEmailVerified(true);
    user.setPreferLanguage(new Language(1, "java", "Java", "javac Main.java", "java Main"));
    return user;
  }

  private static Problem aPublishedProblem(long id) {
    Problem problem = new Problem();
    problem.setProblemId(id);
    problem.setProblemName("A + B Problem");
    problem.setStatus("PUBLISHED");
    problem.setProblemDifficulty(new ProblemDifficulty(1, "easy", "Easy"));
    problem.setTimeLimit(1000);
    problem.setMemoryLimit(65536);
    problem.setDescription("Compute a + b.");
    problem.setInpuFormatt("Two integers a and b.");
    problem.setOutputFormat("Their sum.");
    problem.setSampleInput("1 2");
    problem.setSampleOutput("3");
    problem.setHint("None.");
    problem.setTotalSubmission(100);
    problem.setAcceptedSubmission(50);
    problem.setProblemTags(List.of());
    return problem;
  }

  private static Contest aContest(long id, String mode, Date start, Date end) {
    Contest contest = new Contest();
    contest.setContestId(id);
    contest.setContestName("Weekly Contest");
    contest.setContestNotes("Have fun.");
    contest.setStartTime(start);
    contest.setEndTime(end);
    contest.setStatus("PUBLISHED");
    contest.setScoring("OI".equals(mode) ? "IOI" : "ICPC");
    contest.setProblems("[]");
    return contest;
  }

  private static DiscussionThread aThread(long id) {
    DiscussionThread thread = new DiscussionThread();
    thread.setDiscussionThreadId(id);
    thread.setDiscussionThreadTitle("How do I solve this?");
    thread.setDiscussionThreadCreator(aUser(2000L, usersGroup()));
    thread.setDiscussionThreadCreateTime(new Date());
    thread.setDiscussionTopic(new DiscussionTopic(1, "general", "General", 0));
    thread.setNumberOfReplies(0);
    thread.setNumberOfReports(0);
    thread.setVisible(true);
    return thread;
  }

  private static Submission aSubmission(long id) {
    Submission submission = new Submission();
    submission.setSubmissionId(id);
    submission.setProblem(aPublishedProblem(1000L));
    submission.setUser(aUser(2000L, usersGroup()));
    submission.setLanguage(new Language(1, "java", "Java", "javac Main.java", "java Main"));
    submission.setSubmitTime(new Date());
    submission.setExecuteTime(new Date());
    submission.setUsedTime(120);
    submission.setUsedMemory(2048);
    submission.setJudgeResult(new JudgeResult(1, "AC", "Accepted"));
    submission.setJudgeScore(100);
    submission.setJudgeLog("");
    submission.setCode("print(a + b)");
    return submission;
  }

  /** The user-meta attributes the profile page reads (social links are indexed by key). */
  private static Map<String, Object> userMeta() {
    Map<String, Object> meta = new HashMap<>();
    meta.put("socialLinks", new HashMap<String, Object>());
    return meta;
  }

  private static BulletinBoardMessage aBulletin(long id) {
    BulletinBoardMessage message = new BulletinBoardMessage();
    message.setMessageId(id);
    message.setMessageTitle("Welcome");
    message.setMessageBody("Welcome to VOJ.");
    message.setMessageCreateTime(new Date());
    message.setMessageAuthor(aUser(1000L, new UserGroup(8, "administrators", "Administrators")));
    return message;
  }

  private static DiscussionReply aReply(long id, long threadId) {
    DiscussionReply reply = new DiscussionReply();
    reply.setDiscussionReplyId(id);
    reply.setDiscussionThreadId(threadId);
    reply.setDiscussionReplyCreator(aUser(2000L, usersGroup()));
    reply.setDiscussionReplyCreateTime(new Date());
    reply.setDiscussionReplyContent("This is a reply.");
    return reply;
  }

  private static Date past() {
    return new Date(System.currentTimeMillis() - 86_400_000L);
  }

  private static Date future() {
    return new Date(System.currentTimeMillis() + 86_400_000L);
  }

  private static Date farFuture() {
    return new Date(System.currentTimeMillis() + 7 * 86_400_000L);
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
      properties.setProperty("url.base", "https://oj.example.edu/voj");
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
