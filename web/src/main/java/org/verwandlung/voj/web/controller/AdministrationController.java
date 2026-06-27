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

import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import org.verwandlung.voj.web.exception.ResourceNotFoundException;
import org.verwandlung.voj.web.messenger.ApplicationEventListener;
import org.verwandlung.voj.web.model.BulletinBoardMessage;
import org.verwandlung.voj.web.model.Checkpoint;
import org.verwandlung.voj.web.model.Contest;
import org.verwandlung.voj.web.model.DiscussionThread;
import org.verwandlung.voj.web.model.Language;
import org.verwandlung.voj.web.model.Option;
import org.verwandlung.voj.web.model.Problem;
import org.verwandlung.voj.web.model.ProblemCategory;
import org.verwandlung.voj.web.model.ProblemTag;
import org.verwandlung.voj.web.model.Submission;
import org.verwandlung.voj.web.model.User;
import org.verwandlung.voj.web.model.UserGroup;
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
import org.verwandlung.voj.web.util.DateUtils;
import org.verwandlung.voj.web.util.HttpRequestParser;
import org.verwandlung.voj.web.util.HttpSessionParser;
import org.verwandlung.voj.web.util.JsonUtils;

/**
 * Handles the requests for system administration.
 *
 * @author Haozhe Xie
 */
@Controller
@RequestMapping(value = "/administration")
public class AdministrationController {
  /**
   * Populates the chrome shared by every administration page: the active sidebar section (derived
   * from the request URI, so the sidebar/header need no per-page parameter) and the cheap sidebar
   * badge counts. A page may override the heading by adding {@code sectionLabel} / {@code sectionCmd}
   * to its own model.
   *
   * @param request - the HttpServletRequest object
   * @param model - the Spring MVC model to populate
   */
  @ModelAttribute
  public void populateAdminChrome(HttpServletRequest request, Model model) {
    String uri = request.getRequestURI();
    String section = "dashboard";
    if (uri.contains("problem")) {
      section = "problems";
    } else if (uri.contains("contest")) {
      section = "contests";
    } else if (uri.contains("submission")) {
      section = "submissions";
    } else if (uri.contains("discussion")) {
      section = "discussion";
    } else if (uri.contains("bulletin")) {
      section = "bulletins";
    } else if (uri.contains("user")) {
      section = "users";
    } else if (uri.contains("judger")) {
      section = "judgers";
    } else if (uri.contains("settings")) {
      section = "settings";
    }
    model.addAttribute("adminSection", section);
    model.addAttribute("navProblemsBadge", toCompactNumber(getTotalProblems()));

    long liveContests = contestService.getNumberOfLiveContests();
    if (liveContests > 0) {
      model.addAttribute("navContestsBadge", liveContests);
    }
    int reportedReplies = discussionService.getNumberOfReportedReplies();
    if (reportedReplies > 0) {
      model.addAttribute("navDiscussionBadge", reportedReplies);
      model.addAttribute("navAlertCount", reportedReplies);
    }
    long onlineJudgers = getOnlineJudgers();
    if (onlineJudgers > 0) {
      model.addAttribute("navJudgersBadge", onlineJudgers);
    }
  }

  /**
   * Formats a count compactly for the sidebar badges (e.g. 2348 -&gt; "2.3k").
   *
   * @param number - the number to format
   * @return the compact string representation of the number
   */
  private static String toCompactNumber(long number) {
    if (number >= 1000) {
      return String.format("%.1fk", number / 1000.0);
    }
    return String.valueOf(number);
  }

  /**
   * Loads the system administration home page.
   *
   * @param request - the HttpRequest object
   * @param response - the HttpResponse object
   * @return a ModelAndView object containing the information of the system administration page
   */
  @RequestMapping(value = "", method = RequestMethod.GET)
  public ModelAndView indexView(HttpServletRequest request, HttpServletResponse response) {
    ModelAndView view = new ModelAndView("pages/administration/index");
    Date today = new Date();

    // KPI cards.
    view.addObject("totalUsers", getTotalUsers());
    view.addObject("newUsersToday", getNumberOfUserRegisteredToday());
    view.addObject("onlineUsers", getOnlineUsers());
    view.addObject("totalProblems", getTotalProblems());
    view.addObject("privateProblems", getPrivateProblems());
    view.addObject("submissionsToday", getSubmissionsToday());
    long pendingSubmissions = submissionService.getNumberOfPendingSubmissions();
    view.addObject("pendingSubmissions", pendingSubmissions);
    view.addObject("liveContests", contestService.getNumberOfLiveContests());

    // Submissions over the last 14 days (bar chart).
    Map<String, Long> dailySubmissions =
        submissionService.getNumberOfSubmissionsGroupByDay(
            DateUtils.getDateBefore(DASHBOARD_CHART_DAYS), today, 0, false);
    List<Map<String, Object>> submissionBars = new ArrayList<>();
    long maxDaily = 1;
    long totalDaily = 0;
    for (long value : dailySubmissions.values()) {
      maxDaily = Math.max(maxDaily, value);
    }
    for (Map.Entry<String, Long> entry : dailySubmissions.entrySet()) {
      long value = entry.getValue();
      totalDaily += value;
      Map<String, Object> bar = new HashMap<>(4, 1);
      bar.put("value", value);
      bar.put("height", Math.round(value * 100.0 / maxDaily));
      bar.put("label", getDayOfWeekLetter(entry.getKey()));
      submissionBars.add(bar);
    }
    view.addObject("submissionBars", submissionBars);
    view.addObject("submissionsTotal", totalDaily);

    // Verdict mix over the last 30 days.
    Map<String, Long> verdictMix =
        submissionService.getNumberOfSubmissionsGroupByJudgeResult(
            DateUtils.getDateBefore(DASHBOARD_VERDICT_DAYS), today);
    long verdictTotal = 0;
    for (long value : verdictMix.values()) {
      verdictTotal += value;
    }
    List<Map<String, Object>> verdicts = new ArrayList<>();
    for (Map.Entry<String, Long> entry : verdictMix.entrySet()) {
      String slug = entry.getKey();
      Map<String, Object> verdict = new HashMap<>(5, 1);
      verdict.put("slug", slug);
      verdict.put("name", VERDICT_NAMES.getOrDefault(slug, slug));
      verdict.put("cls", VERDICT_CLASSES.getOrDefault(slug, "c-muted"));
      verdict.put("count", entry.getValue());
      verdict.put("percentage", verdictTotal == 0 ? 0 : Math.round(entry.getValue() * 100.0 / verdictTotal));
      verdicts.add(verdict);
    }
    view.addObject("verdicts", verdicts);

    // Moderation queue (most-reported discussion replies).
    view.addObject("moderationQueue", discussionService.getReportedReplies(DASHBOARD_QUEUE_SIZE));
    view.addObject("moderationCount", discussionService.getNumberOfReportedReplies());

    // System status.
    view.addObject("onlineJudgers", getOnlineJudgers());
    double averageCpuLoad = eventListener.getAverageCpuLoad();
    view.addObject("averageCpuLoad", averageCpuLoad < 0 ? -1 : (int) Math.round(averageCpuLoad));
    view.addObject("storageUsage", getStorageUsagePercentage());
    view.addObject("memoryUsage", getCurrentMemoryUsage());
    return view;
  }

  /**
   * Maps a {@code yyyy/MM/dd} date string to a single-letter day-of-week label for the dashboard's
   * submissions chart.
   *
   * @param date - the date string in {@code yyyy/MM/dd} format
   * @return the single-letter day-of-week label, or an empty string when the date cannot be parsed
   */
  private static String getDayOfWeekLetter(String date) {
    try {
      Calendar calendar = Calendar.getInstance();
      calendar.setTime(new SimpleDateFormat("yyyy/MM/dd").parse(date));
      return DAY_OF_WEEK_LETTERS[calendar.get(Calendar.DAY_OF_WEEK) - 1];
    } catch (ParseException e) {
      return "";
    }
  }

  /**
   * Gets the disk-usage percentage of the file store hosting the application's working directory,
   * feeding the dashboard's "storage used" status row.
   *
   * @return the disk usage as a whole-number percentage (0..100)
   */
  private static int getStorageUsagePercentage() {
    File root = new File(System.getProperty("user.dir"));
    long totalSpace = root.getTotalSpace();
    long usableSpace = root.getUsableSpace();
    if (totalSpace <= 0) {
      return 0;
    }
    return (int) Math.round((totalSpace - usableSpace) * 100.0 / totalSpace);
  }

  /**
   * Gets the total number of registered users in the system.
   *
   * @return the total number of registered users in the system
   */
  private long getTotalUsers() {
    UserGroup userGroup = userService.getUserGroupUsingSlug("users");
    return userService.getNumberOfUsers(userGroup);
  }

  /**
   * Gets the number of users registered today.
   *
   * @return the number of users registered today
   */
  public long getNumberOfUserRegisteredToday() {
    return userService.getNumberOfUserRegisteredToday();
  }

  /**
   * Gets the number of online users.
   *
   * @return the number of online users
   */
  private long getOnlineUsers() {
    return sessionRegistry.getAllPrincipals().size();
  }

  /**
   * Gets the total number of all problems.
   *
   * @return the total number of all problems
   */
  private long getTotalProblems() {
    return problemService.getNumberOfProblems();
  }

  /**
   * Gets the number of private problems.
   *
   * @return the number of private problems
   */
  private long getPrivateProblems() {
    long numberOfTotalProblems = getTotalProblems();
    long numberOfPublicProblems = problemService.getNumberOfProblemsUsingFilters(null, "", true);
    return numberOfTotalProblems - numberOfPublicProblems;
  }

  /**
   * Gets the total number of checkpoints of all problems (including private problems).
   *
   * @return the total number of checkpoints of all problems
   */
  private long getNumberOfCheckpoints() {
    return problemService.getNumberOfCheckpoints();
  }

  /**
   * Gets the number of submissions today.
   *
   * @return the number of submissions today
   */
  private long getSubmissionsToday() {
    Calendar calendar = Calendar.getInstance();
    int year = calendar.get(Calendar.YEAR);
    int month = calendar.get(Calendar.MONTH);
    int date = calendar.get(Calendar.DAY_OF_MONTH);

    calendar.set(year, month, date, 0, 0, 0);
    Date startTime = calendar.getTime();

    calendar.set(year, month, date + 1, 0, 0, 0);
    Date endTime = calendar.getTime();
    return submissionService.getNumberOfSubmissionsUsingDate(startTime, endTime);
  }

  /**
   * Gets the current memory usage of the web application.
   *
   * @return the current memory usage of the web application (MB)
   */
  private long getCurrentMemoryUsage() {
    long totalMemory = Runtime.getRuntime().totalMemory();
    long freeMemory = Runtime.getRuntime().freeMemory();

    return (totalMemory - freeMemory) / 1048576;
  }

  /**
   * Gets the number of online judgers, by getting the number of consumers listening to the message
   * queue.
   *
   * @return the number of online judgers
   */
  private long getOnlineJudgers() {
    return eventListener.getOnlineJudgers();
  }

  /**
   * Gets the system's daily submission counts over the last year, feeding the activity heat-map.
   *
   * @param request - the HttpServletRequest object
   * @return a Map object containing the system's daily submission counts keyed by date
   */
  @RequestMapping(value = "/getNumberOfSubmissions.action", method = RequestMethod.GET)
  public @ResponseBody Map<String, Object> getNumberOfSubmissionsAction(HttpServletRequest request) {
    Date today = new Date();
    Date startDate = DateUtils.getDateBefore(ACTIVITY_HEAT_MAP_DAYS);
    Map<String, Object> submissions = new HashMap<>(1, 1);
    submissions.put(
        "totalSubmissions",
        submissionService.getNumberOfSubmissionsGroupByDay(startDate, today, 0, false));
    return submissions;
  }

  /**
   * Loads the judge-cluster page, showing the live telemetry of every online judger node.
   *
   * @param request - the HttpServletRequest object
   * @param response - the HttpServletResponse object
   * @return a ModelAndView object containing the online judgers' telemetry
   */
  @RequestMapping(value = "/judgers", method = RequestMethod.GET)
  public ModelAndView judgersView(HttpServletRequest request, HttpServletResponse response) {
    List<Map<String, Object>> nodes = new ArrayList<>();
    for (Map<String, Object> judger : eventListener.getOnlineJudgerList()) {
      int cpuLoad = toInt(judger.get("cpuLoad"));
      int memoryUsage = toInt(judger.get("memoryUsage"));
      long uptime = judger.get("uptime") instanceof Number ? ((Number) judger.get("uptime")).longValue() : 0;

      Map<String, Object> node = new HashMap<>(7, 1);
      node.put("name", judger.get("username"));
      node.put("description", judger.get("description"));
      node.put("cpuLoad", cpuLoad);
      node.put("memoryUsage", memoryUsage);
      node.put("uptime", formatUptime(uptime));
      node.put("loadClass", cpuLoad > 85 ? "c-err" : (cpuLoad > 60 ? "c-warn" : "c-accent"));
      nodes.add(node);
    }

    ModelAndView view = new ModelAndView("pages/administration/judgers");
    view.addObject("nodes", nodes);
    view.addObject("onlineJudgers", nodes.size());
    view.addObject("queueDepth", submissionService.getNumberOfPendingSubmissions());
    double averageCpuLoad = eventListener.getAverageCpuLoad();
    view.addObject("averageCpuLoad", averageCpuLoad < 0 ? -1 : (int) Math.round(averageCpuLoad));
    return view;
  }

  /**
   * Coerces a possibly-null Number telemetry value to an int.
   *
   * @param value - the value to coerce
   * @return the int value, or 0 when not a Number
   */
  private static int toInt(Object value) {
    return value instanceof Number ? ((Number) value).intValue() : 0;
  }

  /**
   * Formats an uptime duration (in milliseconds) into a compact {@code Nd Nh}, {@code Nh Nm} or
   * {@code Nm} string for the judgers page.
   *
   * @param uptimeMillis - the uptime in milliseconds
   * @return the formatted uptime string
   */
  private static String formatUptime(long uptimeMillis) {
    long totalMinutes = uptimeMillis / 60000L;
    long days = totalMinutes / 1440L;
    long hours = (totalMinutes % 1440L) / 60L;
    long minutes = totalMinutes % 60L;
    if (days > 0) {
      return days + "d " + hours + "h";
    }
    if (hours > 0) {
      return hours + "h " + minutes + "m";
    }
    return minutes + "m";
  }

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
    final int NUMBER_OF_USERS_PER_PAGE = 100;
    List<UserGroup> userGroups = userService.getUserGroups();
    UserGroup userGroup = userService.getUserGroupUsingSlug(userGroupSlug);
    long totalUsers = userService.getNumberOfUsersUsingUserGroupAndUsername(userGroup, username);
    long offset = (pageNumber >= 1 ? pageNumber - 1 : 0) * NUMBER_OF_USERS_PER_PAGE;
    List<User> users =
        userService.getUserUsingUserGroupAndUsername(
            userGroup, username, offset, NUMBER_OF_USERS_PER_PAGE);

    ModelAndView view = new ModelAndView("pages/administration/all-users");
    view.addObject("userGroups", userGroups);
    view.addObject("selectedUserGroup", userGroupSlug);
    view.addObject("username", username);
    view.addObject("currentPage", pageNumber);
    view.addObject("totalPages", (long) Math.ceil(totalUsers * 1.0 / NUMBER_OF_USERS_PER_PAGE));
    view.addObject("users", users);
    return view;
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
    Map<String, Boolean> result = new HashMap<>(2, 1);
    List<Long> userList = JsonUtils.toList(users, Long.class);

    for (Long userId : userList) {
      userService.deleteUser(userId);
    }
    result.put("isSuccessful", true);
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
    ModelAndView view = new ModelAndView("pages/administration/edit-user");
    view.addObject("user", user);
    view.addAllObjects(userMeta);
    view.addObject("userGroups", userGroups);
    view.addObject("languages", languages);
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
   * @param location - the user's location
   * @param website - the user's personal homepage
   * @param socialLinks - the user's social network information
   * @param aboutMe - the user's personal bio
   * @param request - the HttpServletRequest object
   * @return a Map<String, Boolean> object containing the profile update result
   */
  @RequestMapping(value = "/editUser.action", method = RequestMethod.POST)
  public @ResponseBody Map<String, Boolean> editUserAction(
      @RequestParam(value = "uid") long uid,
      @RequestParam(value = "password") String password,
      @RequestParam(value = "email") String email,
      @RequestParam(value = "userGroup") String userGroupSlug,
      @RequestParam(value = "preferLanguage") String preferLanguageSlug,
      @RequestParam(value = "location") String location,
      @RequestParam(value = "website") String website,
      @RequestParam(value = "socialLinks") String socialLinks,
      @RequestParam(value = "aboutMe") String aboutMe,
      HttpServletRequest request) {
    User user = userService.getUserUsingUid(uid);
    Map<String, Boolean> result = new HashMap<>(12, 1);
    result.put("isSuccessful", false);
    result.put("isUserExists", false);

    if (user != null) {
      Map<String, Boolean> updateProfileResult =
          userService.updateProfile(user, password, userGroupSlug, preferLanguageSlug);
      Map<String, Boolean> updateUserMetaResult =
          userService.updateProfile(user, email, location, website, socialLinks, aboutMe);
      boolean isUpdateProfileSuccessful = updateProfileResult.get("isSuccessful");
      boolean isUpdateUserMetaSuccessful = updateUserMetaResult.get("isSuccessful");

      result.putAll(updateProfileResult);
      result.putAll(updateUserMetaResult);
      result.put("isUserExists", true);
      result.put("isSuccessful", isUpdateProfileSuccessful && isUpdateUserMetaSuccessful);
    }
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
    ModelAndView view = new ModelAndView("pages/administration/new-user");
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
      HttpServletRequest request) {
    Map<String, Boolean> result =
        userService.createUser(username, password, email, userGroupSlug, preferLanguageSlug);

    if (result.get("isSuccessful")) {
      String ipAddress = HttpRequestParser.getRemoteAddr(request);
      LOGGER.info(
          String.format(
              "User: [Username=%s] was created by administrator at %s.",
              new Object[] {username, ipAddress}));
    }
    return result;
  }

  /**
   * Loads the problem list page.
   *
   * @param request - the HttpServletRequest object
   * @param response - the HttpServletResponse object
   * @return a ModelAndView object containing the information of the problem list page
   */
  @RequestMapping(value = "/all-problems", method = RequestMethod.GET)
  public ModelAndView allProblemsView(
      @RequestParam(value = "keyword", required = false, defaultValue = "") String keyword,
      @RequestParam(value = "problemCategory", required = false, defaultValue = "")
          String problemCategorySlug,
      @RequestParam(value = "problemTag", required = false, defaultValue = "")
          String problemTagSlug,
      @RequestParam(value = "page", required = false, defaultValue = "1") long pageNumber,
      HttpServletRequest request,
      HttpServletResponse response) {
    final int NUMBER_OF_PROBLEMS_PER_PAGE = 100;
    List<ProblemCategory> problemCategories = problemService.getProblemCategories();
    long totalProblems =
        problemService.getNumberOfProblemsUsingFilters(keyword, problemCategorySlug, false);

    long offset = (pageNumber >= 1 ? pageNumber - 1 : 0) * NUMBER_OF_PROBLEMS_PER_PAGE;
    long problemIdLowerBound = problemService.getFirstIndexOfProblems() + offset;
    long problemIdUpperBound = problemIdLowerBound + NUMBER_OF_PROBLEMS_PER_PAGE - 1;

    List<Problem> problems =
        problemService.getProblemsUsingFilters(
            problemIdLowerBound,
            keyword,
            problemCategorySlug,
            problemTagSlug,
            false,
            NUMBER_OF_PROBLEMS_PER_PAGE);
    Map<Long, List<ProblemCategory>> problemCategoryRelationships =
        problemService.getProblemCategoriesOfProblems(problemIdLowerBound, problemIdUpperBound);
    Map<Long, List<ProblemTag>> problemTagRelationships =
        problemService.getProblemTagsOfProblems(problemIdLowerBound, problemIdUpperBound);

    ModelAndView view = new ModelAndView("pages/administration/all-problems");
    view.addObject("problemCategories", problemCategories);
    view.addObject("selectedProblemCategory", problemCategorySlug);
    view.addObject("keyword", keyword);
    view.addObject("currentPage", pageNumber);
    view.addObject(
        "totalPages", (long) Math.ceil(totalProblems * 1.0 / NUMBER_OF_PROBLEMS_PER_PAGE));
    view.addObject("problems", problems);
    view.addObject("problemCategoryRelationships", problemCategoryRelationships);
    view.addObject("problemTagRelationships", problemTagRelationships);
    return view;
  }

  /**
   * Deletes the selected problems.
   *
   * @param problems - the set of problem IDs, separated by commas (, )
   * @param request - the HttpServletRequest object
   * @return the deletion result of the problems
   */
  @RequestMapping(value = "/deleteProblems.action", method = RequestMethod.POST)
  public @ResponseBody Map<String, Boolean> deleteProblemsAction(
      @RequestParam(value = "problems") String problems, HttpServletRequest request) {
    Map<String, Boolean> result = new HashMap<>(2, 1);
    List<Long> problemList = JsonUtils.toList(problems, Long.class);

    for (Long problemId : problemList) {
      problemService.deleteProblem(problemId);

      String ipAddress = HttpRequestParser.getRemoteAddr(request);
      LOGGER.info(
          String.format(
              "Problem: [ProblemId=%s] was deleted by administrator at %s.",
              new Object[] {problemId, ipAddress}));
    }
    result.put("isSuccessful", true);
    return result;
  }

  /**
   * Loads the create problem page.
   *
   * @param request - the HttpServletRequest object
   * @param response - the HttpServletResponse object
   * @return a ModelAndView object containing the information of the create problem page
   */
  @RequestMapping(value = "/new-problem", method = RequestMethod.GET)
  public ModelAndView newProblemView(HttpServletRequest request, HttpServletResponse response) {
    Map<ProblemCategory, List<ProblemCategory>> problemCategories =
        problemService.getProblemCategoriesWithHierarchy();

    ModelAndView view = new ModelAndView("pages/administration/new-problem");
    view.addObject("problemCategories", problemCategories);
    view.addObject("problemDifficulties", problemService.getProblemDifficulties());
    return view;
  }

  /**
   * Handles the user's request to create a problem.
   *
   * @param problemName - the name of the problem
   * @param timeLimit - the time limit
   * @param memoryLimit - the memory usage limit
   * @param description - the description of the problem
   * @param hint - the hint of the problem
   * @param inputFormat - the input format
   * @param outputFormat - the output format
   * @param inputSample - the input sample
   * @param outputSample - the output sample
   * @param testCases - the test cases (in JSON format)
   * @param problemCategories - the problem categories (in JSON format)
   * @param problemTags - the problem tags (in JSON format)
   * @param isPublic - whether the problem is public
   * @param isExactlyMatch - whether the checkpoints are matched exactly
   * @param request - the HttpServletRequest object
   * @return a Map<String, Boolean> object containing the problem creation result
   */
  @RequestMapping(value = "/createProblem.action", method = RequestMethod.POST)
  public @ResponseBody Map<String, Object> createProblemAction(
      @RequestParam(value = "problemName") String problemName,
      @RequestParam(value = "timeLimit") String timeLimit,
      @RequestParam(value = "memoryLimit") String memoryLimit,
      @RequestParam(value = "description") String description,
      @RequestParam(value = "hint") String hint,
      @RequestParam(value = "inputFormat") String inputFormat,
      @RequestParam(value = "outputFormat") String outputFormat,
      @RequestParam(value = "inputSample") String inputSample,
      @RequestParam(value = "outputSample") String outputSample,
      @RequestParam(value = "testCases") String testCases,
      @RequestParam(value = "problemCategories") String problemCategories,
      @RequestParam(value = "problemTags") String problemTags,
      @RequestParam(value = "isPublic") boolean isPublic,
      @RequestParam(value = "isExactlyMatch") boolean isExactlyMatch,
      @RequestParam(value = "problemDifficulty", required = false, defaultValue = "") String problemDifficulty,
      HttpServletRequest request) {
    if (timeLimit.isEmpty() || !StringUtils.isNumeric(timeLimit)) {
      timeLimit = "-1";
    }
    if (memoryLimit.isEmpty() || !StringUtils.isNumeric(memoryLimit)) {
      memoryLimit = "-1";
    }
    Map<String, Object> result =
        problemService.createProblem(
            problemName,
            Integer.parseInt(timeLimit),
            Integer.parseInt(memoryLimit),
            description,
            hint,
            inputFormat,
            outputFormat,
            inputSample,
            outputSample,
            testCases,
            problemCategories,
            problemTags,
            isPublic,
            isExactlyMatch,
            problemDifficulty);

    if ((boolean) result.get("isSuccessful")) {
      long problemId = (Long) result.get("problemId");
      String ipAddress = HttpRequestParser.getRemoteAddr(request);

      LOGGER.info(
          String.format(
              "Problem: [ProblemId=%s] was created by administrator at %s.",
              new Object[] {problemId, ipAddress}));
    }
    return result;
  }

  /**
   * Loads the edit problem page.
   *
   * @param problemId - the unique identifier of the problem
   * @param request - the HttpServletRequest object
   * @param response - the HttpServletResponse object
   * @return a ModelAndView object containing the information of the edit problem page
   */
  @RequestMapping(value = "/edit-problem/{problemId}", method = RequestMethod.GET)
  public ModelAndView editProblemsView(
      @PathVariable(value = "problemId") long problemId,
      HttpServletRequest request,
      HttpServletResponse response) {
    Problem problem = problemService.getProblem(problemId);

    if (problem == null) {
      throw new ResourceNotFoundException();
    }
    List<Checkpoint> checkpoints = problemService.getCheckpointsUsingProblemId(problemId);
    List<ProblemCategory> selectedProblemCategories =
        problemService.getProblemCategoriesUsingProblemId(problemId);
    Map<ProblemCategory, List<ProblemCategory>> problemCategories =
        problemService.getProblemCategoriesWithHierarchy();
    List<ProblemTag> problemTags = problemService.getProblemTagsUsingProblemId(problemId);

    ModelAndView view = new ModelAndView("pages/administration/edit-problem");
    view.addObject("problem", problem);
    view.addObject("checkpoints", checkpoints);
    view.addObject("problemCategories", problemCategories);
    view.addObject("selectedProblemCategories", selectedProblemCategories);
    view.addObject("problemTags", problemTags);
    view.addObject("problemDifficulties", problemService.getProblemDifficulties());
    return view;
  }

  /**
   * Handles the user's request to edit a problem.
   *
   * @param problemName - the name of the problem
   * @param timeLimit - the time limit
   * @param memoryLimit - the memory usage limit
   * @param description - the description of the problem
   * @param hint - the hint of the problem
   * @param inputFormat - the input format
   * @param outputFormat - the output format
   * @param inputSample - the input sample
   * @param outputSample - the output sample
   * @param testCases - the test cases (in JSON format)
   * @param problemCategories - the problem categories (in JSON format)
   * @param problemTags - the problem tags (in JSON format)
   * @param isPublic - whether the problem is public
   * @param isExactlyMatch - whether the checkpoints are matched exactly
   * @param request - the HttpServletRequest object
   * @return a Map<String, Boolean> object containing the problem edit result
   */
  @RequestMapping(value = "/editProblem.action", method = RequestMethod.POST)
  public @ResponseBody Map<String, Boolean> editProblemAction(
      @RequestParam(value = "problemId") long problemId,
      @RequestParam(value = "problemName") String problemName,
      @RequestParam(value = "timeLimit") String timeLimit,
      @RequestParam(value = "memoryLimit") String memoryLimit,
      @RequestParam(value = "description") String description,
      @RequestParam(value = "hint") String hint,
      @RequestParam(value = "inputFormat") String inputFormat,
      @RequestParam(value = "outputFormat") String outputFormat,
      @RequestParam(value = "inputSample") String inputSample,
      @RequestParam(value = "outputSample") String outputSample,
      @RequestParam(value = "testCases") String testCases,
      @RequestParam(value = "problemCategories") String problemCategories,
      @RequestParam(value = "problemTags") String problemTags,
      @RequestParam(value = "isPublic") boolean isPublic,
      @RequestParam(value = "isExactlyMatch") boolean isExactlyMatch,
      @RequestParam(value = "problemDifficulty", required = false, defaultValue = "") String problemDifficulty,
      HttpServletRequest request) {
    if (timeLimit.isEmpty() || !StringUtils.isNumeric(timeLimit)) {
      timeLimit = "-1";
    }
    if (memoryLimit.isEmpty() || !StringUtils.isNumeric(memoryLimit)) {
      memoryLimit = "-1";
    }
    Map<String, Boolean> result =
        problemService.editProblem(
            problemId,
            problemName,
            Integer.parseInt(timeLimit),
            Integer.parseInt(memoryLimit),
            description,
            hint,
            inputFormat,
            outputFormat,
            inputSample,
            outputSample,
            testCases,
            problemCategories,
            problemTags,
            isPublic,
            isExactlyMatch,
            problemDifficulty);

    if (result.get("isSuccessful")) {
      String ipAddress = HttpRequestParser.getRemoteAddr(request);

      LOGGER.info(
          String.format(
              "Problem: [ProblemId=%s] was edited by administrator at %s.",
              new Object[] {problemId, ipAddress}));
    }
    return result;
  }

  /**
   * Loads the problem categories page.
   *
   * @param request - the HttpServletRequest object
   * @param response - the HttpServletResponse object
   * @return a ModelAndView object containing the information of the problem categories page
   */
  @RequestMapping(value = "/problem-categories", method = RequestMethod.GET)
  public ModelAndView problemCategoriesView(
      HttpServletRequest request, HttpServletResponse response) {
    List<ProblemCategory> problemCategories = problemService.getProblemCategories();

    ModelAndView view = new ModelAndView("pages/administration/problem-categories");
    view.addObject("problemCategories", problemCategories);
    return view;
  }

  /**
   * Creates a problem category.
   *
   * @param problemCategorySlug - the slug of the problem category
   * @param problemCategoryName - the name of the problem category
   * @param parentProblemCategorySlug - the slug of the parent problem category
   * @param request - the HttpServletRequest object
   * @return a Map<String, Object> object containing the problem category creation result
   */
  @RequestMapping(value = "/createProblemCategory.action", method = RequestMethod.POST)
  public @ResponseBody Map<String, Object> createProblemCategoryAction(
      @RequestParam(value = "problemCategorySlug") String problemCategorySlug,
      @RequestParam(value = "problemCategoryName") String problemCategoryName,
      @RequestParam(value = "parentProblemCategory") String parentProblemCategorySlug,
      HttpServletRequest request) {
    Map<String, Object> result =
        problemService.createProblemCategory(
            problemCategorySlug, problemCategoryName, parentProblemCategorySlug);

    if ((boolean) result.get("isSuccessful")) {
      long problemCategoryId = (Long) result.get("problemCategoryId");
      String ipAddress = HttpRequestParser.getRemoteAddr(request);

      LOGGER.info(
          String.format(
              "ProblemCategory: [ProblemCategoryId=%s] was created by administrator at %s.",
              new Object[] {problemCategoryId, ipAddress}));
    }
    return result;
  }

  /**
   * Edits a problem category.
   *
   * @param problemCategoryId - the unique identifier of the problem category
   * @param problemCategorySlug - the slug of the problem category
   * @param problemCategoryName - the name of the problem category
   * @param parentProblemCategorySlug - the slug of the parent problem category
   * @param request - the HttpServletRequest object
   * @return a Map<String, Boolean> object containing the problem category edit result
   */
  @RequestMapping(value = "/editProblemCategory.action", method = RequestMethod.POST)
  public @ResponseBody Map<String, Boolean> editProblemCategoryAction(
      @RequestParam(value = "problemCategoryId") String problemCategoryId,
      @RequestParam(value = "problemCategorySlug") String problemCategorySlug,
      @RequestParam(value = "problemCategoryName") String problemCategoryName,
      @RequestParam(value = "parentProblemCategory") String parentProblemCategorySlug,
      HttpServletRequest request) {
    Map<String, Boolean> result =
        problemService.editProblemCategory(
            Integer.parseInt(problemCategoryId),
            problemCategorySlug,
            problemCategoryName,
            parentProblemCategorySlug);

    if (result.get("isSuccessful")) {
      String ipAddress = HttpRequestParser.getRemoteAddr(request);

      LOGGER.info(
          String.format(
              "ProblemCategory: [ProblemCategoryId=%s] was edited by administrator at %s.",
              new Object[] {problemCategoryId, ipAddress}));
    }
    return result;
  }

  /**
   * Deletes problem categories.
   *
   * @param problemCategories - the set of unique identifiers of the problem categories
   * @param request - the HttpServletRequest object
   * @return a Map<String, Boolean> object containing the problem category deletion result
   */
  @RequestMapping(value = "/deleteProblemCategories.action", method = RequestMethod.POST)
  public @ResponseBody Map<String, Object> deleteProblemCategoryAction(
      @RequestParam(value = "problemCategories") String problemCategories,
      HttpServletRequest request) {
    Map<String, Object> result = new HashMap<>(3, 1);
    List<Integer> problemCategoryList = JsonUtils.toList(problemCategories, Integer.class);
    List<Integer> deletedProblemCategories = new ArrayList<>();

    for (int problemCategoryId : problemCategoryList) {
      if (problemService.deleteProblemCategory(problemCategoryId)) {
        deletedProblemCategories.add(problemCategoryId);
      }
      String ipAddress = HttpRequestParser.getRemoteAddr(request);
      LOGGER.info(
          String.format(
              "ProblemCategory: [ProblemCategoryId=%s] was deleted by administrator at %s.",
              new Object[] {problemCategoryId, ipAddress}));
    }
    result.put("isSuccessful", true);
    result.put("deletedProblemCategories", deletedProblemCategories);
    return result;
  }

  /**
   * Loads the submission list page.
   *
   * @param problemId - the unique identifier of the problem the submission corresponds to
   * @param username - the username of the submitter
   * @param pageNumber - the page number of the current page
   * @param request - the HttpServletRequest object
   * @param response - the HttpServletResponse object
   * @return a ModelAndView object containing the information of the submission list page
   */
  @RequestMapping(value = "/all-submissions", method = RequestMethod.GET)
  public ModelAndView allSubmissionsView(
      @RequestParam(value = "problemId", required = false, defaultValue = "0") long problemId,
      @RequestParam(value = "username", required = false, defaultValue = "") String username,
      @RequestParam(value = "page", required = false, defaultValue = "1") long pageNumber,
      HttpServletRequest request,
      HttpServletResponse response) {
    final int NUMBER_OF_SUBMISSIONS_PER_PAGE = 100;

    long totalSubmissions =
        submissionService.getNumberOfSubmissionsUsingProblemIdAndUsername(problemId, username);
    long latestSubmissionId = submissionService.getLatestSubmissionId();
    long offset =
        latestSubmissionId
            - (pageNumber >= 1 ? pageNumber - 1 : 0) * NUMBER_OF_SUBMISSIONS_PER_PAGE;
    List<Submission> submissions =
        submissionService.getSubmissions(
            problemId, username, offset, NUMBER_OF_SUBMISSIONS_PER_PAGE);

    ModelAndView view = new ModelAndView("pages/administration/all-submissions");
    view.addObject("problemId", problemId);
    view.addObject("username", username);
    view.addObject("currentPage", pageNumber);
    view.addObject(
        "totalPages", (long) Math.ceil(totalSubmissions * 1.0 / NUMBER_OF_SUBMISSIONS_PER_PAGE));
    view.addObject("submissions", submissions);
    return view;
  }

  /**
   * Deletes the selected submission records.
   *
   * @param submissions - the set of submission record IDs, separated by commas (, )
   * @param request - the HttpServletRequest object
   * @return the deletion result of the submission records
   */
  @RequestMapping(value = "/deleteSubmissions.action", method = RequestMethod.POST)
  public @ResponseBody Map<String, Object> deleteSubmissionsAction(
      @RequestParam(value = "submissions") String submissions, HttpServletRequest request) {
    Map<String, Object> result = new HashMap<>(3, 1);
    List<Long> submissionList = JsonUtils.toList(submissions, Long.class);
    List<Long> deletedSubmissions = new ArrayList<>();

    for (Long submissionId : submissionList) {
      if (submissionService.deleteSubmission(submissionId)) {
        deletedSubmissions.add(submissionId);
      }
      String ipAddress = HttpRequestParser.getRemoteAddr(request);
      LOGGER.info(
          String.format(
              "Submission: [SubmissionId=%s] deleted by administrator at %s.",
              new Object[] {submissionId, ipAddress}));
    }
    result.put("isSuccessful", true);
    result.put("deletedSubmissions", deletedSubmissions);
    return result;
  }

  /**
   * Rejudges the selected submission records.
   *
   * @param submissions - the set of submission record IDs, separated by commas (, )
   * @param request - the HttpServletRequest object
   * @return the execution result of the rejudge request
   */
  @RequestMapping(value = "/restartSubmissions.action", method = RequestMethod.POST)
  public @ResponseBody Map<String, Boolean> restartSubmissionsAction(
      @RequestParam(value = "submissions") String submissions, HttpServletRequest request) {
    Map<String, Boolean> result = new HashMap<>(2, 1);
    List<Long> submissionList = JsonUtils.toList(submissions, Long.class);

    for (Long submissionId : submissionList) {
      submissionService.createSubmissionTask(submissionId);
    }
    result.put("isSuccessful", true);
    return result;
  }

  /**
   * Views a submission record.
   *
   * @param submissionId - the unique identifier of the submission record
   * @param request - the HttpServletRequest object
   * @param response - the HttpServletResponse object
   * @return a ModelAndView object containing the submission record information
   */
  @RequestMapping(value = "/edit-submission/{submissionId}", method = RequestMethod.GET)
  public ModelAndView editSubmissionView(
      @PathVariable(value = "submissionId") long submissionId,
      HttpServletRequest request,
      HttpServletResponse response) {
    Submission submission = submissionService.getSubmission(submissionId);
    if (submission == null) {
      throw new ResourceNotFoundException();
    }
    ModelAndView view = new ModelAndView("pages/administration/edit-submission");
    view.addObject("submission", submission);
    return view;
  }

  /**
   * Loads the general settings page.
   *
   * @param request - the HttpRequest object
   * @param response - the HttpResponse object
   * @return a ModelAndView object containing the information of the general settings page
   */
  @RequestMapping(value = "/general-settings", method = RequestMethod.GET)
  public ModelAndView generalSettingsView(
      HttpServletRequest request, HttpServletResponse response) {
    ModelAndView view = new ModelAndView("pages/administration/general-settings");
    view.addObject("options", getOptions());
    view.addObject("offensiveWordCount", offensiveWordService.getNumberOfOffensiveWords());
    return view;
  }

  /**
   * Gets all the options of the system, returned as key-value pairs.
   *
   * @return the system options as key-value pairs
   */
  private Map<String, String> getOptions() {
    Map<String, String> optionMap = new HashMap<>();
    List<Option> options = optionService.getOptions();

    for (Option option : options) {
      optionMap.put(option.getOptionName(), option.getOptionValue());
    }
    return optionMap;
  }

  /**
   * Updates the general settings of the website.
   *
   * @param websiteName - the name of the website
   * @param websiteDescription - the description of the website
   * @param copyright - the copyright information of the website
   * @param allowUserRegister - whether user registration is allowed
   * @param icpNumber - the ICP filing number of the website
   * @param policeIcpNumber - the public security filing number
   * @param googleAnalyticsCode - the Google Analytics code
   * @param offensiveWordSources - the offensive word import sources, one URL per line
   * @param request - the HttpServletRequest object
   * @return the update result of the general settings of the website
   */
  @RequestMapping(value = "/updateGeneralSettings.action", method = RequestMethod.POST)
  public @ResponseBody Map<String, Boolean> updateGeneralSettingsAction(
      @RequestParam(value = "websiteName") String websiteName,
      @RequestParam(value = "websiteDescription") String websiteDescription,
      @RequestParam(value = "copyright") String copyright,
      @RequestParam(value = "allowUserRegister") boolean allowUserRegister,
      @RequestParam(value = "icpNumber") String icpNumber,
      @RequestParam(value = "policeIcpNumber") String policeIcpNumber,
      @RequestParam(value = "googleAnalyticsCode") String googleAnalyticsCode,
      @RequestParam(value = "offensiveWordSources") String offensiveWordSources,
      HttpServletRequest request) {
    Map<String, Boolean> result =
        optionService.updateOptions(
            websiteName,
            websiteDescription,
            copyright,
            allowUserRegister,
            icpNumber,
            policeIcpNumber,
            googleAnalyticsCode,
            offensiveWordSources);
    return result;
  }

  /**
   * Saves the offensive word import sources and re-imports the offensive word dictionary from them.
   * The whole dictionary is replaced with the freshly fetched words; if every source fails the
   * existing dictionary is left untouched.
   *
   * @param offensiveWordSources - the offensive word import sources, one URL per line
   * @param request - the HttpServletRequest object
   * @return a result map describing the import outcome and a per-source breakdown
   */
  @RequestMapping(value = "/importOffensiveWords.action", method = RequestMethod.POST)
  public @ResponseBody Map<String, Object> importOffensiveWordsAction(
      @RequestParam(value = "offensiveWordSources") String offensiveWordSources,
      HttpServletRequest request) {
    offensiveWordService.updateOffensiveWordSources(offensiveWordSources);
    return offensiveWordImportService.importFromSources();
  }

  /**
   * Loads the programming language settings page.
   *
   * @param request - the HttpServletRequest object
   * @param response - the HttpServletResponse object
   * @return a ModelAndView object containing the programming language settings information
   */
  @RequestMapping(value = "/language-settings", method = RequestMethod.GET)
  public ModelAndView languageSettingsView(
      HttpServletRequest request, HttpServletResponse response) {
    ModelAndView view = new ModelAndView("pages/administration/language-settings");
    view.addObject("languages", languageService.getAllLanguages());
    return view;
  }

  /**
   * Updates the programming language settings of the website.
   *
   * @param languages - the array containing the programming language settings
   * @param request - the HttpServletRequest object
   * @return the update result of the programming language settings
   */
  @RequestMapping(value = "/updateLanguageSettings.action", method = RequestMethod.POST)
  public @ResponseBody Map<String, Object> updateLanguageSettingsAction(
      @RequestParam(value = "languages") String languages, HttpServletRequest request) {
    List<Language> languagesList = JsonUtils.toList(languages, Language.class);
    Map<String, Object> result = languageService.updateLanguageSettings(languagesList);
    return result;
  }

  /**
   * Loads the bulletin board message list page.
   *
   * @param pageNumber - the page number of the current page
   * @param request - the HttpServletRequest object
   * @param response - the HttpServletResponse object
   * @return a ModelAndView object containing the bulletin board messages
   */
  @RequestMapping(value = "/all-bulletins", method = RequestMethod.GET)
  public ModelAndView allBulletinsView(
      @RequestParam(value = "page", required = false, defaultValue = "1") long pageNumber,
      HttpServletRequest request,
      HttpServletResponse response) {
    final int NUMBER_OF_BULLETINS_PER_PAGE = 50;
    long totalBulletins = bulletinBoardService.getNumberOfBulletinBoardMessages();
    long offset = (pageNumber >= 1 ? pageNumber - 1 : 0) * NUMBER_OF_BULLETINS_PER_PAGE;
    List<BulletinBoardMessage> messages =
        bulletinBoardService.getBulletinBoardMessages(offset, NUMBER_OF_BULLETINS_PER_PAGE);

    ModelAndView view = new ModelAndView("pages/administration/all-bulletins");
    view.addObject("messages", messages);
    view.addObject("currentPage", pageNumber);
    view.addObject(
        "totalPages", (long) Math.ceil(totalBulletins * 1.0 / NUMBER_OF_BULLETINS_PER_PAGE));
    return view;
  }

  /**
   * Loads the create bulletin board message page.
   *
   * @param request - the HttpServletRequest object
   * @param response - the HttpServletResponse object
   * @return a ModelAndView object for creating a bulletin board message
   */
  @RequestMapping(value = "/new-bulletin", method = RequestMethod.GET)
  public ModelAndView newBulletinView(HttpServletRequest request, HttpServletResponse response) {
    return new ModelAndView("pages/administration/edit-bulletin");
  }

  /**
   * Loads the edit bulletin board message page.
   *
   * @param messageId - the unique identifier of the bulletin board message
   * @param request - the HttpServletRequest object
   * @param response - the HttpServletResponse object
   * @return a ModelAndView object containing the bulletin board message to edit
   */
  @RequestMapping(value = "/edit-bulletin/{messageId}", method = RequestMethod.GET)
  public ModelAndView editBulletinView(
      @PathVariable(value = "messageId") long messageId,
      HttpServletRequest request,
      HttpServletResponse response) {
    BulletinBoardMessage message = bulletinBoardService.getBulletinBoardMessage(messageId);
    if (message == null) {
      throw new ResourceNotFoundException();
    }
    ModelAndView view = new ModelAndView("pages/administration/edit-bulletin");
    view.addObject("message", message);
    view.addObject("sectionLabel", message.getMessageTitle());
    return view;
  }

  /**
   * Creates a bulletin board message.
   *
   * @param messageTitle - the title of the message
   * @param messageBody - the content of the message
   * @param request - the HttpServletRequest object
   * @return a Map containing the creation result
   */
  @RequestMapping(value = "/createBulletin.action", method = RequestMethod.POST)
  public @ResponseBody Map<String, Boolean> createBulletinAction(
      @RequestParam(value = "messageTitle") String messageTitle,
      @RequestParam(value = "messageBody") String messageBody,
      HttpServletRequest request) {
    return bulletinBoardService.createBulletinBoardMessage(messageTitle, messageBody);
  }

  /**
   * Edits a bulletin board message.
   *
   * @param messageId - the unique identifier of the message
   * @param messageTitle - the new title of the message
   * @param messageBody - the new content of the message
   * @param request - the HttpServletRequest object
   * @return a Map containing the edit result
   */
  @RequestMapping(value = "/editBulletin.action", method = RequestMethod.POST)
  public @ResponseBody Map<String, Boolean> editBulletinAction(
      @RequestParam(value = "messageId") long messageId,
      @RequestParam(value = "messageTitle") String messageTitle,
      @RequestParam(value = "messageBody") String messageBody,
      HttpServletRequest request) {
    return bulletinBoardService.editBulletinBoardMessage(messageId, messageTitle, messageBody);
  }

  /**
   * Deletes the selected bulletin board messages.
   *
   * @param bulletins - the set of message IDs, separated by commas
   * @param request - the HttpServletRequest object
   * @return the deletion result
   */
  @RequestMapping(value = "/deleteBulletins.action", method = RequestMethod.POST)
  public @ResponseBody Map<String, Boolean> deleteBulletinsAction(
      @RequestParam(value = "bulletins") String bulletins, HttpServletRequest request) {
    Map<String, Boolean> result = new HashMap<>(2, 1);
    List<Long> bulletinList = JsonUtils.toList(bulletins, Long.class);
    for (Long messageId : bulletinList) {
      bulletinBoardService.deleteBulletinBoardMessage(messageId);
    }
    result.put("isSuccessful", true);
    return result;
  }

  /**
   * Loads the contest list page.
   *
   * @param keyword - the search keyword
   * @param pageNumber - the page number of the current page
   * @param request - the HttpServletRequest object
   * @param response - the HttpServletResponse object
   * @return a ModelAndView object containing the contest list
   */
  @RequestMapping(value = "/all-contests", method = RequestMethod.GET)
  public ModelAndView allContestsView(
      @RequestParam(value = "keyword", required = false, defaultValue = "") String keyword,
      @RequestParam(value = "page", required = false, defaultValue = "1") long pageNumber,
      HttpServletRequest request,
      HttpServletResponse response) {
    final int NUMBER_OF_CONTESTS_PER_PAGE = 50;
    long totalContests = contestService.getNumberOfContests(keyword);
    long offset = (pageNumber >= 1 ? pageNumber - 1 : 0) * NUMBER_OF_CONTESTS_PER_PAGE;
    List<Contest> contests =
        contestService.getContests(keyword, offset, NUMBER_OF_CONTESTS_PER_PAGE);

    ModelAndView view = new ModelAndView("pages/administration/all-contests");
    view.addObject("contests", contests);
    view.addObject("keyword", keyword);
    view.addObject("currentPage", pageNumber);
    view.addObject(
        "totalPages", (long) Math.ceil(totalContests * 1.0 / NUMBER_OF_CONTESTS_PER_PAGE));
    return view;
  }

  /**
   * Loads the create contest page.
   *
   * @param request - the HttpServletRequest object
   * @param response - the HttpServletResponse object
   * @return a ModelAndView object for creating a contest
   */
  @RequestMapping(value = "/new-contest", method = RequestMethod.GET)
  public ModelAndView newContestView(HttpServletRequest request, HttpServletResponse response) {
    return new ModelAndView("pages/administration/edit-contest");
  }

  /**
   * Loads the edit contest page.
   *
   * @param contestId - the unique identifier of the contest
   * @param request - the HttpServletRequest object
   * @param response - the HttpServletResponse object
   * @return a ModelAndView object containing the contest to edit
   */
  @RequestMapping(value = "/edit-contest/{contestId}", method = RequestMethod.GET)
  public ModelAndView editContestView(
      @PathVariable(value = "contestId") long contestId,
      HttpServletRequest request,
      HttpServletResponse response) {
    Contest contest = contestService.getContest(contestId);
    if (contest == null) {
      throw new ResourceNotFoundException();
    }
    ModelAndView view = new ModelAndView("pages/administration/edit-contest");
    view.addObject("contest", contest);
    view.addObject("sectionLabel", contest.getContestName());
    return view;
  }

  /**
   * Creates a contest.
   *
   * @param contestName - the name of the contest
   * @param contestNotes - the notes/description of the contest
   * @param startTime - the start time (local datetime string)
   * @param endTime - the end time (local datetime string)
   * @param contestMode - the mode of the contest (ACM / OI)
   * @param problems - the contest problems (a JSON-formatted array of problem IDs)
   * @param request - the HttpServletRequest object
   * @return a Map containing the creation result
   */
  @RequestMapping(value = "/createContest.action", method = RequestMethod.POST)
  public @ResponseBody Map<String, Object> createContestAction(
      @RequestParam(value = "contestName") String contestName,
      @RequestParam(value = "contestNotes", required = false, defaultValue = "") String contestNotes,
      @RequestParam(value = "startTime") String startTime,
      @RequestParam(value = "endTime") String endTime,
      @RequestParam(value = "contestMode", required = false, defaultValue = "ACM") String contestMode,
      @RequestParam(value = "problems", required = false, defaultValue = "[]") String problems,
      HttpServletRequest request) {
    return contestService.createContest(
        contestName,
        contestNotes,
        parseLocalDateTime(startTime),
        parseLocalDateTime(endTime),
        contestMode,
        problems);
  }

  /**
   * Edits a contest.
   *
   * @param contestId - the unique identifier of the contest
   * @param contestName - the name of the contest
   * @param contestNotes - the notes/description of the contest
   * @param startTime - the start time (local datetime string)
   * @param endTime - the end time (local datetime string)
   * @param contestMode - the mode of the contest (ACM / OI)
   * @param problems - the contest problems (a JSON-formatted array of problem IDs)
   * @param request - the HttpServletRequest object
   * @return a Map containing the edit result
   */
  @RequestMapping(value = "/editContest.action", method = RequestMethod.POST)
  public @ResponseBody Map<String, Object> editContestAction(
      @RequestParam(value = "contestId") long contestId,
      @RequestParam(value = "contestName") String contestName,
      @RequestParam(value = "contestNotes", required = false, defaultValue = "") String contestNotes,
      @RequestParam(value = "startTime") String startTime,
      @RequestParam(value = "endTime") String endTime,
      @RequestParam(value = "contestMode", required = false, defaultValue = "ACM") String contestMode,
      @RequestParam(value = "problems", required = false, defaultValue = "[]") String problems,
      HttpServletRequest request) {
    return contestService.editContest(
        contestId,
        contestName,
        contestNotes,
        parseLocalDateTime(startTime),
        parseLocalDateTime(endTime),
        contestMode,
        problems);
  }

  /**
   * Deletes the selected contests.
   *
   * @param contests - the set of contest IDs, separated by commas
   * @param request - the HttpServletRequest object
   * @return the deletion result
   */
  @RequestMapping(value = "/deleteContests.action", method = RequestMethod.POST)
  public @ResponseBody Map<String, Boolean> deleteContestsAction(
      @RequestParam(value = "contests") String contests, HttpServletRequest request) {
    Map<String, Boolean> result = new HashMap<>(2, 1);
    List<Long> contestList = JsonUtils.toList(contests, Long.class);
    for (Long contestId : contestList) {
      contestService.deleteContest(contestId);
    }
    result.put("isSuccessful", true);
    return result;
  }

  /**
   * Parses an HTML {@code datetime-local} value ({@code yyyy-MM-dd'T'HH:mm}) into a Date, falling
   * back to the {@code yyyy-MM-dd HH:mm:ss} format.
   *
   * @param value - the datetime string
   * @return the parsed Date, or null when empty or unparseable
   */
  private static Date parseLocalDateTime(String value) {
    if (value == null || value.trim().isEmpty()) {
      return null;
    }
    try {
      return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm").parse(value.trim());
    } catch (ParseException e) {
      try {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(value.trim());
      } catch (ParseException ex) {
        return null;
      }
    }
  }

  /**
   * Loads the discussion thread list page.
   *
   * @param pageNumber - the page number of the current page
   * @param request - the HttpServletRequest object
   * @param response - the HttpServletResponse object
   * @return a ModelAndView object containing the discussion threads
   */
  @RequestMapping(value = "/all-discussion", method = RequestMethod.GET)
  public ModelAndView allDiscussionView(
      @RequestParam(value = "page", required = false, defaultValue = "1") long pageNumber,
      HttpServletRequest request,
      HttpServletResponse response) {
    final int NUMBER_OF_THREADS_PER_PAGE = 50;
    long totalThreads = discussionService.getNumberOfDiscussionThreads();
    long offset = (pageNumber >= 1 ? pageNumber - 1 : 0) * NUMBER_OF_THREADS_PER_PAGE;

    ModelAndView view = new ModelAndView("pages/administration/all-discussion");
    view.addObject("threads", discussionService.getAllDiscussionThreads(offset, NUMBER_OF_THREADS_PER_PAGE));
    view.addObject("currentPage", pageNumber);
    view.addObject("totalPages", (long) Math.ceil(totalThreads * 1.0 / NUMBER_OF_THREADS_PER_PAGE));
    return view;
  }

  /**
   * Loads the discussion thread moderation page.
   *
   * @param threadId - the unique identifier of the discussion thread
   * @param request - the HttpServletRequest object
   * @param response - the HttpServletResponse object
   * @return a ModelAndView object containing the thread, its replies and reports
   */
  @RequestMapping(value = "/edit-thread/{threadId}", method = RequestMethod.GET)
  public ModelAndView editThreadView(
      @PathVariable(value = "threadId") long threadId,
      HttpServletRequest request,
      HttpServletResponse response) {
    DiscussionThread thread = discussionService.getDiscussionThreadUsingThreadId(threadId);
    if (thread == null) {
      throw new ResourceNotFoundException();
    }
    ModelAndView view = new ModelAndView("pages/administration/edit-thread");
    view.addObject("thread", thread);
    view.addObject("replies", discussionService.getDiscussionRepliesOfThread(threadId, -1, 0, 200));
    view.addObject("sectionLabel", thread.getDiscussionThreadTitle());
    return view;
  }

  /**
   * Deletes the selected discussion threads.
   *
   * @param threads - the set of thread IDs, separated by commas
   * @param request - the HttpServletRequest object
   * @return the deletion result
   */
  @RequestMapping(value = "/deleteThreads.action", method = RequestMethod.POST)
  public @ResponseBody Map<String, Boolean> deleteThreadsAction(
      @RequestParam(value = "threads") String threads, HttpServletRequest request) {
    Map<String, Boolean> result = new HashMap<>(2, 1);
    List<Long> threadList = JsonUtils.toList(threads, Long.class);
    for (Long threadId : threadList) {
      discussionService.deleteDiscussionThread(threadId);
    }
    result.put("isSuccessful", true);
    return result;
  }

  /**
   * Deletes a single discussion reply.
   *
   * @param replyId - the unique identifier of the discussion reply
   * @param request - the HttpServletRequest object
   * @return the deletion result
   */
  @RequestMapping(value = "/deleteReply.action", method = RequestMethod.POST)
  public @ResponseBody Map<String, Boolean> deleteReplyAction(
      @RequestParam(value = "replyId") long replyId, HttpServletRequest request) {
    User currentUser = HttpSessionParser.getCurrentUser(request.getSession());
    return discussionService.deleteDiscussionReply(replyId, currentUser);
  }

  /**
   * Loads the discussion topics page.
   *
   * @param request - the HttpServletRequest object
   * @param response - the HttpServletResponse object
   * @return a ModelAndView object containing the discussion topics
   */
  @RequestMapping(value = "/discussion-topics", method = RequestMethod.GET)
  public ModelAndView discussionTopicsView(
      HttpServletRequest request, HttpServletResponse response) {
    ModelAndView view = new ModelAndView("pages/administration/discussion-topics");
    view.addObject("discussionTopics", discussionService.getDiscussionTopics());
    return view;
  }

  /**
   * Creates a top-level discussion topic.
   *
   * @param discussionTopicSlug - the slug of the topic
   * @param discussionTopicName - the name of the topic
   * @param request - the HttpServletRequest object
   * @return the creation result
   */
  @RequestMapping(value = "/createTopic.action", method = RequestMethod.POST)
  public @ResponseBody Map<String, Boolean> createTopicAction(
      @RequestParam(value = "discussionTopicSlug") String discussionTopicSlug,
      @RequestParam(value = "discussionTopicName") String discussionTopicName,
      HttpServletRequest request) {
    return discussionService.createDiscussionTopic(
        discussionTopicSlug, discussionTopicName, null);
  }

  /**
   * Deletes a discussion topic.
   *
   * @param discussionTopicId - the unique identifier of the topic
   * @param request - the HttpServletRequest object
   * @return the deletion result
   */
  @RequestMapping(value = "/deleteTopic.action", method = RequestMethod.POST)
  public @ResponseBody Map<String, Boolean> deleteTopicAction(
      @RequestParam(value = "discussionTopicId") int discussionTopicId, HttpServletRequest request) {
    return discussionService.deleteDiscussionTopic(discussionTopicId);
  }

  /** The autowired UserService object. */
  @Autowired private UserService userService;

  /** The autowired session registry. Used for counting the number of online users. */
  @Autowired private SessionRegistry sessionRegistry;

  /** The autowired ProblemService object. Used for getting problem record information. */
  @Autowired private ProblemService problemService;

  /** The autowired SubmissionService object. Used for getting submission record information. */
  @Autowired private SubmissionService submissionService;

  /** The autowired ContestService object. Used for contest counts and the contest admin. */
  @Autowired private ContestService contestService;

  /** The autowired DiscussionService object. Used for the moderation queue and discussion admin. */
  @Autowired private DiscussionService discussionService;

  /** The autowired BulletinBoardService object. Used for the bulletin board admin. */
  @Autowired private BulletinBoardService bulletinBoardService;

  /** The autowired OptionService object. Used for getting the settings options of the system. */
  @Autowired private OptionService optionService;

  /** The autowired OffensiveWordService object. Used for managing the offensive word dictionary. */
  @Autowired private OffensiveWordService offensiveWordService;

  /** The autowired OffensiveWordImportService object. Used for importing offensive words by URL. */
  @Autowired private OffensiveWordImportService offensiveWordImportService;

  /**
   * The autowired LanguageService object. Used for getting the programming language options of the
   * system.
   */
  @Autowired private LanguageService languageService;

  /**
   * The autowired ApplicationEventListener object. Used for getting the number of online judgers.
   */
  @Autowired private ApplicationEventListener eventListener;

  /** The number of days of daily activity returned for the heat-map (53 weeks). */
  private static final int ACTIVITY_HEAT_MAP_DAYS = 53 * 7;

  /** The number of days shown in the dashboard's submissions bar chart. */
  private static final int DASHBOARD_CHART_DAYS = 14;

  /** The window (in days) over which the dashboard's verdict mix is computed. */
  private static final int DASHBOARD_VERDICT_DAYS = 30;

  /** The number of moderation-queue rows shown on the dashboard. */
  private static final int DASHBOARD_QUEUE_SIZE = 6;

  /** Single-letter day-of-week labels (index 0 == Sunday, matching Calendar.DAY_OF_WEEK - 1). */
  private static final String[] DAY_OF_WEEK_LETTERS = {"S", "M", "T", "W", "T", "F", "S"};

  /** Human-readable names for the judge-result slugs shown in the dashboard's verdict mix. */
  private static final Map<String, String> VERDICT_NAMES = new HashMap<>();

  /** Colour classes (admin.css) for the judge-result slugs shown in the dashboard's verdict mix. */
  private static final Map<String, String> VERDICT_CLASSES = new HashMap<>();

  static {
    VERDICT_NAMES.put("AC", "Accepted");
    VERDICT_NAMES.put("WA", "Wrong Answer");
    VERDICT_NAMES.put("TLE", "Time Limit Exceeded");
    VERDICT_NAMES.put("MLE", "Memory Limit Exceeded");
    VERDICT_NAMES.put("OLE", "Output Limit Exceeded");
    VERDICT_NAMES.put("RE", "Runtime Error");
    VERDICT_NAMES.put("CE", "Compile Error");
    VERDICT_NAMES.put("PE", "Presentation Error");
    VERDICT_NAMES.put("SE", "System Error");

    VERDICT_CLASSES.put("AC", "c-accent");
    VERDICT_CLASSES.put("WA", "c-err");
    VERDICT_CLASSES.put("TLE", "c-warn");
    VERDICT_CLASSES.put("MLE", "c-warn");
    VERDICT_CLASSES.put("OLE", "c-warn");
    VERDICT_CLASSES.put("RE", "c-info");
    VERDICT_CLASSES.put("CE", "c-muted");
    VERDICT_CLASSES.put("PE", "c-info");
    VERDICT_CLASSES.put("SE", "c-muted");
  }

  /** The logger. */
  private static final Logger LOGGER = LogManager.getLogger(AdministrationController.class);
}
