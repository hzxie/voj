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
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import org.verwandlung.voj.web.interceptor.AdministrationSection;
import org.verwandlung.voj.web.model.JudgerSummary;
import org.verwandlung.voj.web.model.Language;
import org.verwandlung.voj.web.model.ProblemFilter;
import org.verwandlung.voj.web.model.UserGroup;
import org.verwandlung.voj.web.service.ContestService;
import org.verwandlung.voj.web.service.DiscussionService;
import org.verwandlung.voj.web.service.JudgerService;
import org.verwandlung.voj.web.service.LanguageService;
import org.verwandlung.voj.web.service.ProblemService;
import org.verwandlung.voj.web.service.SubmissionService;
import org.verwandlung.voj.web.service.UserService;
import org.verwandlung.voj.web.util.DateUtils;
import org.verwandlung.voj.web.util.HttpSessionParser;
import org.verwandlung.voj.web.util.JsonUtils;

/**
 * The dashboard of the administration console, and the judgers and languages page.
 *
 * @author Haozhe Xie
 */
@Controller
@RequestMapping(value = "/administration")
@AdministrationSection("dashboard")
public class DashboardAdministrationController {
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
            DateUtils.getDateBefore(DASHBOARD_CHART_DAYS),
            today,
            0,
            false,
            HttpSessionParser.getCurrentUser());
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
    JudgerSummary judgerSummary = judgerService.getJudgerSummary();
    view.addObject("judgerSummary", judgerSummary);
    view.addObject("onlineJudgers", judgerSummary.getOnlineJudgers());
    view.addObject("averageCpuLoad", judgerSummary.getAverageCpuLoad());
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
  private long getNumberOfUserRegisteredToday() {
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
    long numberOfPublicProblems =
        problemService.getNumberOfProblems(new ProblemFilter().publicOnly());
    return numberOfTotalProblems - numberOfPublicProblems;
  }

  /**
   * Gets the number of submissions today.
   *
   * @return the number of submissions today
   */
  private long getSubmissionsToday() {
    ZoneId zone = ZoneId.systemDefault();
    LocalDate today = LocalDate.now(zone);
    Date startTime = Date.from(today.atStartOfDay(zone).toInstant());
    Date endTime = Date.from(today.plusDays(1).atStartOfDay(zone).toInstant());
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
   * Loads the judge-cluster page, showing the live telemetry of every online judger node.
   *
   * @param request - the HttpServletRequest object
   * @param response - the HttpServletResponse object
   * @return a ModelAndView object containing the online judgers' telemetry
   */
  @AdministrationSection("judgers")
  @RequestMapping(value = "/judgers", method = RequestMethod.GET)
  public ModelAndView judgersView(HttpServletRequest request, HttpServletResponse response) {
    JudgerSummary judgerSummary = judgerService.getJudgerSummary();
    ModelAndView view = new ModelAndView("pages/administration/judgers");
    view.addObject("judgerSummary", judgerSummary);
    view.addObject("nodes", judgerSummary.getNodes());
    view.addObject("onlineJudgers", judgerSummary.getOnlineJudgers());
    view.addObject("queueDepth", submissionService.getNumberOfPendingSubmissions());
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

  /**
   * The autowired LanguageService object. Used for getting the programming language options of the
   * system.
   */
  @Autowired private LanguageService languageService;

  /** The autowired JudgerService object. Used for getting the running judgers. */
  @Autowired private JudgerService judgerService;

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
}
