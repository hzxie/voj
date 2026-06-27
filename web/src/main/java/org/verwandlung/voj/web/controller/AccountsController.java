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

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import org.springframework.web.servlet.view.RedirectView;
import org.verwandlung.voj.web.exception.ResourceNotFoundException;
import org.verwandlung.voj.web.model.Language;
import org.verwandlung.voj.web.model.User;
import org.verwandlung.voj.web.model.VojUserDetails;
import org.verwandlung.voj.web.service.ContestService;
import org.verwandlung.voj.web.service.LanguageService;
import org.verwandlung.voj.web.service.OptionService;
import org.verwandlung.voj.web.service.SubmissionService;
import org.verwandlung.voj.web.service.UserService;
import org.verwandlung.voj.web.util.DateUtils;
import org.verwandlung.voj.web.util.HttpRequestParser;
import org.verwandlung.voj.web.util.HttpSessionParser;

/**
 * Handles users' login/registration requests.
 *
 * @author Haozhe Xie
 */
@Controller
@RequestMapping(value = "/accounts")
public class AccountsController {
  /**
   * Displays the user's login page.
   *
   * @param isLogout - whether the user is in the logout state
   * @param forwardUrl - the address to redirect to after login (relative path)
   * @param request - the HttpServletRequest object
   * @param response - the HttpResponse object
   * @return a ModelAndView object containing the information of the login page
   */
  @RequestMapping(value = "/login", method = RequestMethod.GET)
  public ModelAndView loginView(
      @RequestParam(value = "logout", required = false, defaultValue = "false") boolean isLogout,
      @RequestParam(value = "forward", required = false, defaultValue = "") String forwardUrl,
      HttpServletRequest request,
      HttpServletResponse response) {
    HttpSession session = request.getSession();
    if (isLogout) {
      destroySession(request, response);
    }

    ModelAndView view = null;
    if (isLoggedIn(session)) {
      RedirectView redirectView = new RedirectView(request.getContextPath());
      redirectView.setExposeModelAttributes(false);
      view = new ModelAndView(redirectView);
    } else {
      view = new ModelAndView("pages/accounts/login");
      view.addObject("isLogout", isLogout);
      view.addObject("forwardUrl", forwardUrl);
    }
    return view;
  }

  /**
   * Logs the current user out: clears the Spring Security context and invalidates the session.
   *
   * @param request - the HttpServletRequest object
   * @param response - the HttpServletResponse object
   */
  private void destroySession(HttpServletRequest request, HttpServletResponse response) {
    User currentUser = HttpSessionParser.getCurrentUser();
    String ipAddress = HttpRequestParser.getRemoteAddr(request);
    LOGGER.info(String.format("%s logged out at %s", new Object[] {currentUser, ipAddress}));

    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    new SecurityContextLogoutHandler().logout(request, response, authentication);
  }

  /**
   * Checks whether the user has logged in.
   *
   * @param session - the HttpSession object (unused)
   * @return whether the user has logged in
   */
  private boolean isLoggedIn(HttpSession session) {
    return HttpSessionParser.getCurrentUser() != null;
  }

  /**
   * Handles the user's login request.
   *
   * @param username - the username
   * @param password - the password (already encrypted with MD5)
   * @param request - the HttpServletRequest object
   * @return a Map<String, Boolean> object containing the login verification result
   */
  @RequestMapping(value = "/login.action", method = RequestMethod.POST)
  public @ResponseBody Map<String, Boolean> loginAction(
      @RequestParam(value = "username") String username,
      @RequestParam(value = "password") String password,
      @RequestParam(value = "rememberMe") boolean isAutoLoginAllowed,
      HttpServletRequest request,
      HttpServletResponse response) {
    String ipAddress = HttpRequestParser.getRemoteAddr(request);
    Map<String, Boolean> result = userService.isAllowedToLogin(username, password);
    LOGGER.info(
        String.format(
            "User: [Username=%s] tried to log in at %s", new Object[] {username, ipAddress}));
    if (result.get("isSuccessful")) {
      User user = userService.getUserUsingUsernameOrEmail(username);
      establishSecurityContext(request, response, user);
    }
    return result;
  }

  /**
   * Establishes the Spring Security context for a logged-in user, persists it to the session and
   * registers the session for online-user tracking.
   *
   * @param request - the HttpServletRequest object
   * @param response - the HttpServletResponse object
   * @param user - a User object containing the basic information of the user
   */
  private void establishSecurityContext(
      HttpServletRequest request, HttpServletResponse response, User user) {
    VojUserDetails userDetails = new VojUserDetails(user);
    Authentication authentication =
        UsernamePasswordAuthenticationToken.authenticated(
            userDetails, null, userDetails.getAuthorities());

    SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
    securityContext.setAuthentication(authentication);
    SecurityContextHolder.setContext(securityContext);
    securityContextRepository.saveContext(securityContext, request, response);
    sessionRegistry.registerNewSession(request.getSession().getId(), userDetails);

    String ipAddress = HttpRequestParser.getRemoteAddr(request);
    LOGGER.info(String.format("%s logged in at %s", new Object[] {user, ipAddress}));
  }

  /**
   * Displays the user registration page.
   *
   * @param request - the HttpServletRequest object
   * @param response - the HttpResponse object
   * @return a ModelAndView object containing the information of the registration page
   */
  @RequestMapping(value = "/register", method = RequestMethod.GET)
  public ModelAndView registerView(
      @RequestParam(value = "forward", required = false, defaultValue = "") String forwardUrl,
      HttpServletRequest request,
      HttpServletResponse response) {
    HttpSession session = request.getSession();
    ModelAndView view = null;
    if (isLoggedIn(session)) {
      RedirectView redirectView = new RedirectView(request.getContextPath());
      redirectView.setExposeModelAttributes(false);
      view = new ModelAndView(redirectView);
    } else {
      List<Language> languages = languageService.getAllLanguages();
      boolean isAllowRegister =
          optionService.getOption("allowUserRegister").getOptionValue().equals("1");

      view = new ModelAndView("pages/accounts/register");
      view.addObject("languages", languages);
      view.addObject("isAllowRegister", isAllowRegister);
    }
    return view;
  }

  /**
   * Handles the user registration request.
   *
   * @param username - the username
   * @param password - the password
   * @param email - the email address
   * @param languageSlug - the slug of the preferred language
   * @param request - the HttpServletRequest object
   * @return a Map<String, Boolean> object containing the account creation result
   */
  @RequestMapping(value = "/register.action", method = RequestMethod.POST)
  public @ResponseBody Map<String, Boolean> registerAction(
      @RequestParam(value = "username") String username,
      @RequestParam(value = "password") String password,
      @RequestParam(value = "email") String email,
      @RequestParam(value = "languagePreference") String languageSlug,
      HttpServletRequest request,
      HttpServletResponse response) {
    boolean isAllowRegister =
        optionService.getOption("allowUserRegister").getOptionValue().equals("1");
    String userGroupSlug = "users";
    Map<String, Boolean> result =
        userService.createUser(
            username, password, email, userGroupSlug, languageSlug, isAllowRegister);

    if (result.get("isSuccessful")) {
      User user = userService.getUserUsingUsernameOrEmail(username);
      establishSecurityContext(request, response, user);

      String ipAddress = HttpRequestParser.getRemoteAddr(request);
      LOGGER.info(
          String.format("User: [Username=%s] created at %s.", new Object[] {username, ipAddress}));
    }
    return result;
  }

  /**
   * Loads the reset password page.
   *
   * @param email - the user's email address
   * @param token - the random string used to reset the password
   * @param request - the HttpServletRequest object
   * @param response - the HttpResponse object
   * @return a ModelAndView object containing the information of the password reset page
   */
  @RequestMapping(value = "/reset-password", method = RequestMethod.GET)
  public ModelAndView resetPasswordView(
      @RequestParam(value = "email", required = false) String email,
      @RequestParam(value = "token", required = false) String token,
      HttpServletRequest request,
      HttpServletResponse response) {
    HttpSession session = request.getSession();
    ModelAndView view = null;

    if (isLoggedIn(session)) {
      RedirectView redirectView = new RedirectView(request.getContextPath());
      redirectView.setExposeModelAttributes(false);
      view = new ModelAndView(redirectView);
    } else {
      boolean isTokenValid = false;
      if (token != null && !token.isEmpty()) {
        isTokenValid = userService.isEmailValidationValid(email, token);
      }

      view = new ModelAndView("pages/accounts/reset-password");
      view.addObject("email", email);
      view.addObject("token", token);
      view.addObject("isTokenValid", isTokenValid);
    }
    return view;
  }

  /**
   * Sends the password reset email.
   *
   * @param username - the user's username
   * @param email - the user's email address
   * @param request - the HttpServletRequest object
   * @return a Map<String, Boolean> object containing the result of sending the password reset email
   */
  @RequestMapping(value = "/forgotPassword.action", method = RequestMethod.POST)
  public @ResponseBody Map<String, Boolean> forgotPasswordAction(
      @RequestParam(value = "username") String username,
      @RequestParam(value = "email") String email,
      HttpServletRequest request) {
    String ipAddress = HttpRequestParser.getRemoteAddr(request);
    Map<String, Boolean> result = userService.sendVerificationEmail(username, email);

    if (result.get("isSuccessful")) {
      LOGGER.info(
          String.format(
              "User: [Username=%s] send an email for resetting password at %s.",
              new Object[] {username, ipAddress}));
    }
    return result;
  }

  /**
   * Resets the user's password.
   *
   * @param email - the user's email address
   * @param token - the token used to reset the password
   * @param newPassword - the new password
   * @param confirmPassword - the confirmed password
   * @param request - the HttpServletRequest object
   * @return a Map<String, Boolean> object containing the password reset result
   */
  @RequestMapping(value = "/resetPassword.action", method = RequestMethod.POST)
  public @ResponseBody Map<String, Boolean> resetPasswordAction(
      @RequestParam(value = "email") String email,
      @RequestParam(value = "token") String token,
      @RequestParam(value = "newPassword") String newPassword,
      @RequestParam(value = "confirmPassword") String confirmPassword,
      HttpServletRequest request) {
    String ipAddress = HttpRequestParser.getRemoteAddr(request);
    Map<String, Boolean> result =
        userService.resetPassword(email, token, newPassword, confirmPassword);

    if (result.get("isSuccessful")) {
      LOGGER.info(
          String.format(
              "User: [Email=%s] resetted password at %s", new Object[] {email, ipAddress}));
    }
    return result;
  }

  /**
   * Loads the user's profile information.
   *
   * @param userId - the unique identifier of the user
   * @param request - the HttpServletRequest object
   * @param response - the HttpResponse object
   * @return a ModelAndView object containing the user's profile information
   */
  @RequestMapping(value = "/user/{userId}", method = RequestMethod.GET)
  public ModelAndView userView(
      @PathVariable("userId") long userId,
      HttpServletRequest request,
      HttpServletResponse response) {
    User user = userService.getUserUsingUid(userId);
    if (user == null
        || user.getUserGroup() == null
        || "judgers".equals(user.getUserGroup().getUserGroupSlug())) {
      throw new ResourceNotFoundException();
    }

    ModelAndView view = new ModelAndView("pages/accounts/user");
    view.addObject("user", user);
    view.addAllObjects(userService.getUserMetaUsingUid(user));

    view.addObject(
        "recentSubmissions",
        submissionService.getSubmissions(0, user.getUsername(), NUMBER_OF_RECENT_SUBMISSIONS));
    view.addObject("submissionStats", submissionService.getSubmissionStatsOfUser(userId));
    view.addObject("contestsAttended", contestService.getNumberOfContestsOfUser(userId));
    view.addObject("solvedByDifficulty", submissionService.getSolvedProblemsByDifficulty(userId));
    return view;
  }

  /**
   * Gets a user's daily submission counts over the last year, feeding the activity heat-map.
   *
   * @param userId - the unique identifier of the user
   * @param request - the HttpServletRequest object
   * @return a Map object containing a user's daily submission counts keyed by date
   */
  @RequestMapping(value = "/getNumberOfSubmissionsOfUsers.action", method = RequestMethod.GET)
  public @ResponseBody Map<String, Object> getNumberOfSubmissionsOfUsersAction(
      @RequestParam(value = "uid", required = false, defaultValue = "0") long userId,
      HttpServletRequest request) {
    if (userId == 0) {
      userId = HttpSessionParser.getCurrentUser().getUid();
    }
    Date today = new Date();
    Date startDate = DateUtils.getDateBefore(ACTIVITY_HEAT_MAP_DAYS);
    Map<String, Object> submissions = new HashMap<>(1, 1);
    submissions.put(
        "totalSubmissions",
        submissionService.getNumberOfSubmissionsGroupByDay(startDate, today, userId, false));
    return submissions;
  }

  /**
   * Handles the user's password change request.
   *
   * @param oldPassword - the old password
   * @param newPassword - the new password
   * @param confirmPassword - the confirmed new password
   * @param request - the HttpServletRequest object
   * @return a Map<String, Boolean> object containing the password verification result
   */
  @RequestMapping(value = "/changePassword.action", method = RequestMethod.POST)
  public @ResponseBody Map<String, Boolean> changePasswordInDashboardAction(
      @RequestParam(value = "oldPassword") String oldPassword,
      @RequestParam(value = "newPassword") String newPassword,
      @RequestParam(value = "confirmPassword") String confirmPassword,
      HttpServletRequest request) {
    User currentUser = HttpSessionParser.getCurrentUser(request.getSession());
    String ipAddress = HttpRequestParser.getRemoteAddr(request);

    Map<String, Boolean> result =
        userService.changePassword(currentUser, oldPassword, newPassword, confirmPassword);
    if (result.get("isSuccessful")) {
      LOGGER.info(
          String.format("%s changed password at %s", new Object[] {currentUser, ipAddress}));
    }
    return result;
  }

  /**
   * Handles the user's request to change their profile.
   *
   * @param email - the user's email address
   * @param location - the user's location
   * @param website - the user's personal homepage
   * @param socialLinks - the user's social network information
   * @param aboutMe - the user's personal bio
   * @param request - the HttpServletRequest object
   * @return a Map<String, Boolean> object containing the profile update result
   */
  @RequestMapping(value = "/updateProfile.action", method = RequestMethod.POST)
  public @ResponseBody Map<String, Boolean> updateProfileInDashboardAction(
      @RequestParam(value = "email") String email,
      @RequestParam(value = "location") String location,
      @RequestParam(value = "website") String website,
      @RequestParam(value = "socialLinks") String socialLinks,
      @RequestParam(value = "aboutMe") String aboutMe,
      HttpServletRequest request) {
    User currentUser = HttpSessionParser.getCurrentUser();
    String ipAddress = HttpRequestParser.getRemoteAddr(request);

    Map<String, Boolean> result =
        userService.updateProfile(currentUser, email, location, website, socialLinks, aboutMe);
    if (result.get("isSuccessful")) {
      LOGGER.info(String.format("%s updated profile at %s", new Object[] {currentUser, ipAddress}));
    }
    return result;
  }

  /** The autowired UserService object. Used for completing the business logic operations of users. */
  @Autowired private UserService userService;

  /**
   * The autowired LanguageService object. Used for loading the language options of the registration
   * page.
   */
  @Autowired private LanguageService languageService;

  /**
   * The autowired SubmissionService object. Used for loading the user's submission and acceptance
   * status on the profile page.
   */
  @Autowired private SubmissionService submissionService;

  /** The autowired ContestService object. Used for the profile page's contests-attended count. */
  @Autowired private ContestService contestService;

  /**
   * The autowired OptionService object. Used for querying whether the registration feature is open.
   */
  @Autowired private OptionService optionService;

  /** The registry of authenticated sessions, used to track logged-in users. */
  @Autowired private SessionRegistry sessionRegistry;

  /** Persists the security context to the HTTP session after a programmatic login. */
  private final SecurityContextRepository securityContextRepository =
      new HttpSessionSecurityContextRepository();

  /** The number of days of daily activity returned for the heat-map (53 weeks). */
  private static final int ACTIVITY_HEAT_MAP_DAYS = 53 * 7;

  /** The number of latest submissions shown in the profile's recent-submissions list. */
  private static final int NUMBER_OF_RECENT_SUBMISSIONS = 8;

  /** The logger. */
  private static final Logger LOGGER = LogManager.getLogger(AccountsController.class);
}
