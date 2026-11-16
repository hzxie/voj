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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import org.verwandlung.voj.web.exception.ResourceNotFoundException;
import org.verwandlung.voj.web.interceptor.AdministrationSection;
import org.verwandlung.voj.web.model.Language;
import org.verwandlung.voj.web.model.Submission;
import org.verwandlung.voj.web.model.User;
import org.verwandlung.voj.web.model.UserGroup;
import org.verwandlung.voj.web.service.LanguageService;
import org.verwandlung.voj.web.service.OptionService;
import org.verwandlung.voj.web.service.SubmissionService;
import org.verwandlung.voj.web.service.UserService;
import org.verwandlung.voj.web.util.HttpSessionParser;
import org.verwandlung.voj.web.util.JsonUtils;
import org.verwandlung.voj.web.util.LogSanitizer;
import org.verwandlung.voj.web.util.Pagination;

/**
 * The user administration: the user list, creating, editing, banning and deleting users.
 *
 * @author Haozhe Xie
 */
@Controller
@RequestMapping(value = "/administration")
@AdministrationSection("users")
public class UserAdministrationController {
  /**
   * Loads the user list page.
   *
   * @param request - the HttpRequest object
   * @param response - the HttpResponse object
   * @return a ModelAndView object containing the information of the user list page
   */
  @RequestMapping(value = "/all-users", method = RequestMethod.GET)
  public ModelAndView allUsersView(
      @RequestParam(value = "userGroup", required = false, defaultValue = "") String userGroupSlug,
      @RequestParam(value = "username", required = false, defaultValue = "") String username,
      @RequestParam(value = "page", required = false, defaultValue = "1") long pageNumber,
      HttpServletRequest request,
      HttpServletResponse response) {
    final int NUMBER_OF_USERS_PER_PAGE = optionService.getIntOption("usersPerPage", 100);
    List<UserGroup> userGroups = userService.getUserGroups();
    UserGroup userGroup = userService.getUserGroupUsingSlug(userGroupSlug);
    long totalUsers = userService.getNumberOfUsersUsingUserGroupAndUsername(userGroup, username);
    long offset = Pagination.getOffset(pageNumber, NUMBER_OF_USERS_PER_PAGE);
    List<User> users =
        userService.getUserUsingUserGroupAndUsername(
            userGroup, username, offset, NUMBER_OF_USERS_PER_PAGE);

    // SOLVED + RANK: the solved-problem count for every user (single query), with each user's global
    // rank derived from it using standard competition ranking (ties share the lowest rank).
    Map<Long, Long> solvedCounts = submissionService.getSolvedProblemCountForAllUsers();
    Map<Long, Long> userRanks = computeUserRanks(solvedCounts);

    // JOINED: the registration time for the users on this page (single query).
    List<Long> uids = new ArrayList<>(users.size());
    for (User user : users) {
      uids.add(user.getUid());
    }
    Map<Long, Date> joinedDates = userService.getRegisterTimes(uids);

    ModelAndView view = new ModelAndView("pages/administration/all-users");
    view.addObject("userGroups", userGroups);
    view.addObject("selectedUserGroup", userGroupSlug);
    view.addObject("username", username);
    view.addObject("currentPage", pageNumber);
    view.addObject("totalPages", (long) Math.ceil(totalUsers * 1.0 / NUMBER_OF_USERS_PER_PAGE));
    view.addObject("users", users);
    view.addObject("solvedCounts", solvedCounts);
    view.addObject("userRanks", userRanks);
    view.addObject("joinedDates", joinedDates);
    return view;
  }

  /**
   * Computes each user's global rank from their solved-problem counts using standard competition
   * ranking: users are ordered by solved count descending, ties share the lowest rank, and the next
   * distinct count skips the appropriate number of positions (e.g. 1, 1, 3). Users with no solved
   * problems are absent from both the input and the result.
   *
   * @param solvedCounts - a map from user identifier to solved-problem count
   * @return a map from user identifier to rank (starting at 1)
   */
  private Map<Long, Long> computeUserRanks(Map<Long, Long> solvedCounts) {
    List<Map.Entry<Long, Long>> entries = new ArrayList<>(solvedCounts.entrySet());
    entries.sort((a, b) -> Long.compare(b.getValue(), a.getValue()));
    Map<Long, Long> userRanks = new HashMap<>(entries.size() * 4 / 3 + 1);
    long rank = 0;
    long previousSolved = -1;
    long processed = 0;
    for (Map.Entry<Long, Long> entry : entries) {
      processed++;
      if (entry.getValue() != previousSolved) {
        rank = processed;
        previousSolved = entry.getValue();
      }
      userRanks.put(entry.getKey(), rank);
    }
    return userRanks;
  }

  /**
   * Deletes the selected users.
   *
   * @param users - the set of user IDs, separated by commas (, )
   * @param request - the HttpServletRequest object
   * @return the deletion result
   */
  @RequestMapping(value = "/deleteUsers.action", method = RequestMethod.POST)
  public @ResponseBody Map<String, Boolean> deleteUsersAction(
      @RequestParam(value = "users") String users, HttpServletRequest request) {
    List<Long> userList = JsonUtils.toList(users, Long.class);
    boolean isSuccessful =
        BulkActions.run("delete users " + userList, () -> userService.deleteUsers(userList));
    if (isSuccessful) {
      LOGGER.info(
          String.format(
              "Users: %s were deleted by administrator at %s.",
              userList, LogSanitizer.forLog(request.getRemoteAddr())));
    }
    Map<String, Boolean> result = new HashMap<>(2, 1);
    result.put("isSuccessful", isSuccessful);
    return result;
  }

  /**
   * Loads the page for editing user information.
   *
   * @param userId - the unique identifier of the user
   * @param request - the HttpServletRequest object
   * @param response - the HttpServletResponse object
   * @return a ModelAndView object containing the user information to edit
   */
  @RequestMapping(value = "/edit-user/{userId}", method = RequestMethod.GET)
  public ModelAndView editUserView(
      @PathVariable(value = "userId") long userId,
      HttpServletRequest request,
      HttpServletResponse response) {
    User user = userService.getUserUsingUid(userId);
    Map<String, Object> userMeta = userService.getUserMetaUsingUid(user);
    if (user == null) {
      throw new ResourceNotFoundException();
    }

    List<UserGroup> userGroups = userService.getUserGroups();
    List<Language> languages = languageService.getAllLanguages();

    // STATS: solved-problem count and global rank (shared with the user list), the total
    // number of submissions, and the most recent submissions for the activity feed.
    Map<String, Long> solvedCountAndRank = submissionService.getSolvedCountAndRankOfUser(userId);
    long solvedCount = solvedCountAndRank.get("solvedCount");
    Long userRank = solvedCountAndRank.get("rank");
    Map<String, Long> submissionStats =
        submissionService.getSubmissionStatsOfUser(userId, HttpSessionParser.getCurrentUser());
    List<Submission> recentSubmissions =
        submissionService.getSubmissions(
            0, user.getUsername(), NUMBER_OF_RECENT_SUBMISSIONS, HttpSessionParser.getCurrentUser());
    boolean isBanned = "forbidden".equals(user.getUserGroup().getUserGroupSlug());
    Date joinedDate = userService.getRegisterTimes(Collections.singletonList(userId)).get(userId);

    ModelAndView view = new ModelAndView("pages/administration/edit-user");
    view.addObject("isCreate", false);
    view.addObject("user", user);
    view.addAllObjects(userMeta);
    view.addObject("userGroups", userGroups);
    view.addObject("languages", languages);
    view.addObject("solvedCount", solvedCount);
    view.addObject("userRank", userRank);
    view.addObject("totalSubmissions", submissionStats.get("totalSubmission"));
    view.addObject("recentSubmissions", recentSubmissions);
    view.addObject("isBanned", isBanned);
    view.addObject("joinedDate", joinedDate);
    return view;
  }

  /**
   * Edits a user's profile information.
   *
   * @param uid - the unique identifier of the user
   * @param password - the user's password (not encrypted with MD5)
   * @param email - the user's email address
   * @param userGroupSlug - the slug of the user group
   * @param preferLanguageSlug - the slug of the user's preferred language
   * @param displayName - the user's display name
   * @param aboutMe - the user's personal bio
   * @param request - the HttpServletRequest object
   * @param response - the HttpServletResponse object
   * @return a Map<String, Boolean> object containing the profile update result
   */
  @RequestMapping(value = "/editUser.action", method = RequestMethod.POST)
  public @ResponseBody Map<String, Boolean> editUserAction(
      @RequestParam(value = "uid") long uid,
      @RequestParam(value = "password") String password,
      @RequestParam(value = "email") String email,
      @RequestParam(value = "userGroup") String userGroupSlug,
      @RequestParam(value = "preferLanguage") String preferLanguageSlug,
      @RequestParam(value = "displayName") String displayName,
      @RequestParam(value = "aboutMe") String aboutMe,
      HttpServletRequest request,
      HttpServletResponse response) {
    User user = userService.getUserUsingUid(uid);
    Map<String, Boolean> result = new HashMap<>(16, 1);
    if (user != null) {
      result.putAll(
          userService.updateUserByAdministrator(
              user, password, userGroupSlug, preferLanguageSlug, email, displayName, aboutMe));
      // An administrator who sets their own password keeps this session (their other sessions
      // end, see CurrentUserRefreshFilter).
      User currentUser = HttpSessionParser.getCurrentUser();
      if (result.get("isSuccessful") && user.equals(currentUser)) {
        currentUser.setPassword(user.getPassword());
        securityContextRepository.saveContext(
            SecurityContextHolder.getContext(), request, response);
      }
    } else {
      result.put("isSuccessful", false);
    }
    result.put("isUserExists", user != null);
    return result;
  }

  /**
   * Loads the create user page.
   *
   * @param request - the HttpServletRequest object
   * @param response - the HttpServletResponse object
   * @return a ModelAndView object containing the information of the create user page
   */
  @RequestMapping(value = "/new-user", method = RequestMethod.GET)
  public ModelAndView newUserView(HttpServletRequest request, HttpServletResponse response) {
    List<UserGroup> userGroups = userService.getUserGroups();
    List<Language> languages = languageService.getAllLanguages();
    ModelAndView view = new ModelAndView("pages/administration/edit-user");
    view.addObject("isCreate", true);
    view.addObject("userGroups", userGroups);
    view.addObject("languages", languages);
    return view;
  }

  /**
   * Creates a new user.
   *
   * @param username - the username
   * @param password - the password
   * @param email - the email address
   * @param userGroupSlug - the slug of the user group
   * @param preferLanguageSlug - the slug of the preferred language
   * @param displayName - the optional display name
   * @param aboutMe - the optional personal bio
   * @param request - the HttpServletRequest object
   * @return a Map<String, Boolean> object containing the account creation result
   */
  @RequestMapping(value = "/newUser.action", method = RequestMethod.POST)
  public @ResponseBody Map<String, Boolean> newUserAction(
      @RequestParam(value = "username") String username,
      @RequestParam(value = "password") String password,
      @RequestParam(value = "email") String email,
      @RequestParam(value = "userGroup") String userGroupSlug,
      @RequestParam(value = "preferLanguage") String preferLanguageSlug,
      @RequestParam(value = "displayName", required = false, defaultValue = "") String displayName,
      @RequestParam(value = "aboutMe", required = false, defaultValue = "") String aboutMe,
      HttpServletRequest request) {
    Map<String, Boolean> result =
        userService.createUser(
            username,
            password,
            email,
            userGroupSlug,
            preferLanguageSlug,
            displayName,
            aboutMe,
            true);

    if (result.get("isSuccessful")) {
      String ipAddress = request.getRemoteAddr();
      LOGGER.info(
          String.format(
              "User: [Username=%s] was created by administrator at %s.",
              new Object[] {LogSanitizer.forLog(username), LogSanitizer.forLog(ipAddress)}));
    }
    return result;
  }

  /**
   * Bans a user by moving them to the {@code forbidden} user group. Used by the moderation review
   * page to ban the author of flagged content.
   *
   * @param userId - the unique identifier of the user to ban
   * @param request - the HttpServletRequest object
   * @return the ban result
   */
  @RequestMapping(value = "/banUser.action", method = RequestMethod.POST)
  public @ResponseBody Map<String, Boolean> banUserAction(
      @RequestParam(value = "userId") long userId, HttpServletRequest request) {
    return userService.banUser(userId);
  }

  /** The autowired UserService object. */
  @Autowired private UserService userService;

  /** Stores the refreshed security context when an administrator changes their own password. */
  private final SecurityContextRepository securityContextRepository =
      new HttpSessionSecurityContextRepository();

  /** The autowired SubmissionService object. Used for getting submission record information. */
  @Autowired private SubmissionService submissionService;

  /** The autowired OptionService object. Used for getting the settings options of the system. */
  @Autowired private OptionService optionService;

  /**
   * The autowired LanguageService object. Used for getting the programming language options of the
   * system.
   */
  @Autowired private LanguageService languageService;

  /** The number of recent submissions shown on the user editor's activity feed. */
  private static final int NUMBER_OF_RECENT_SUBMISSIONS = 5;

  /** The logger. */
  private static final Logger LOGGER = LogManager.getLogger(UserAdministrationController.class);
}
