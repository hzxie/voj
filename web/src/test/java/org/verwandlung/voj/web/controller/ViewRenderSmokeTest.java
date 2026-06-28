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
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
        "/administration/language-settings",
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

  // ---------------------------------------------------------------------------
  // Detail / edit pages. Each loads and dereferences a specific entity, so the
  // owning service call is stubbed with a representative fixture (a null would
  // raise ResourceNotFoundException before the template is reached).
  // ---------------------------------------------------------------------------

  /** The public problem statement page renders for a published problem. */
  @Test
  void problemViewRenders() throws Exception {
    when(problemService.getProblem(1000L)).thenReturn(aPublishedProblem(1000L));
    mockMvc.perform(getWithCsrf("/p/1000")).andExpect(status().isOk());
  }

  /** The problem editorial (solution) page reuses the discussion-thread template. */
  @Test
  void problemSolutionViewRenders() throws Exception {
    when(discussionService.getSolutionThreadOfProblem(1000L)).thenReturn(aThread(5000L));
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

  /** The contest overview page renders for an upcoming contest (leaderboard block skipped). */
  @Test
  void contestViewRenders() throws Exception {
    when(contestService.getContest(1L))
        .thenReturn(aContest(1L, "ACM", future(), farFuture()));
    mockMvc.perform(getWithCsrf("/contest/1")).andExpect(status().isOk());
  }

  /** The contest leaderboard renders for a started ACM contest. */
  @Test
  void contestLeaderboardViewRenders() throws Exception {
    when(contestService.getContest(1L)).thenReturn(aContest(1L, "ACM", past(), future()));
    mockMvc.perform(getWithCsrf("/contest/1/leaderboard")).andExpect(status().isOk());
  }

  /** A visible discussion thread renders. */
  @Test
  void discussionThreadViewRenders() throws Exception {
    when(discussionService.getDiscussionThreadUsingThreadId(5000L)).thenReturn(aThread(5000L));
    mockMvc.perform(getWithCsrf("/discussion/5000")).andExpect(status().isOk());
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

  /** The public submission-detail page renders for an existing submission. */
  @Test
  void submissionViewRenders() throws Exception {
    when(submissionService.getSubmission(7000L)).thenReturn(aSubmission(7000L));
    mockMvc.perform(getWithCsrf("/submission/7000")).andExpect(status().isOk());
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
    contest.setContestMode(mode);
    contest.setStatus("PUBLISHED");
    contest.setScoring("ICPC");
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
