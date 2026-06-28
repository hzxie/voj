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

import java.util.*;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import org.verwandlung.voj.web.messenger.ApplicationEventListener;
import org.verwandlung.voj.web.model.*;
import org.verwandlung.voj.web.service.*;

/**
 * Handles the common requests of the application.
 *
 * @author Haozhe Xie
 */
@Controller
@RequestMapping(value = "/")
public class DefaultController {
  /**
   * Displays the home page of the application.
   *
   * @param request - the HttpRequest object
   * @param response - the HttpResponse object
   * @return a ModelAndView object containing the content of the home page
   */
  @RequestMapping(value = "/", method = RequestMethod.GET)
  public ModelAndView indexView(HttpServletRequest request, HttpServletResponse response) {
    List<Contest> contests = contestService.getContests(null, 0, NUMBER_OF_CONTESTS_PER_REQUEST);
    List<DiscussionThread> discussionThreads =
        discussionService.getDiscussionThreadsOfTopic(
            null, 0, NUMBER_OF_DISCUSSION_THREADS_PER_REQUEST);
    List<BulletinBoardMessage> bulletinBoardMessages =
        bulletinBoardService.getPublishedBulletinBoardMessages(
            0, NUMBER_OF_BULLETIN_MESSAGES_PER_REQUEST);

    ModelAndView view = new ModelAndView("pages/index");
    view.addObject("currentTime", new Date());
    view.addObject("contests", contests);
    view.addObject("discussionThreads", discussionThreads);
    view.addObject("bulletinBoardMessages", bulletinBoardMessages);
    // Stats band + supported-languages section (Home.dc.html).
    view.addObject("totalProblems", problemService.getNumberOfProblems());
    view.addObject(
        "totalUsers", userService.getNumberOfUsers(userService.getUserGroupUsingSlug("users")));
    view.addObject(
        "totalSubmissions",
        submissionService.getNumberOfSubmissionsUsingDate(new Date(0), new Date()));
    view.addObject("onlineJudgers", keepAliveEventListener.getOnlineJudgers());
    view.addObject("languages", languageService.getAllLanguages());
    return view;
  }

  /**
   * Displays the terms of use page.
   *
   * @param request - the HttpRequest object
   * @param response - the HttpResponse object
   * @return a ModelAndView object containing the content of the terms of use page
   */
  @RequestMapping(value = "/terms", method = RequestMethod.GET)
  public ModelAndView termsView(HttpServletRequest request, HttpServletResponse response) {
    ModelAndView view = new ModelAndView("pages/terms");
    return view;
  }

  /**
   * Displays the privacy page.
   *
   * @param request - the HttpRequest object
   * @param response - the HttpResponse object
   * @return a ModelAndView object containing the content of the privacy page
   */
  @RequestMapping(value = "/privacy", method = RequestMethod.GET)
  public ModelAndView privacyView(HttpServletRequest request, HttpServletResponse response) {
    ModelAndView view = new ModelAndView("pages/privacy");
    return view;
  }

  /**
   * Displays the maintenance page. Rendered for non-administrators while the site is in maintenance
   * mode (see {@code MaintenanceModeInterceptor}).
   *
   * @param request - the HttpRequest object
   * @param response - the HttpResponse object
   * @return a ModelAndView object containing the content of the maintenance page
   */
  @RequestMapping(value = "/maintenance", method = RequestMethod.GET)
  public ModelAndView maintenanceView(HttpServletRequest request, HttpServletResponse response) {
    return new ModelAndView("pages/errors/maintenance");
  }

  /**
   * Displays the judger information page.
   *
   * @param request - the HttpRequest object
   * @param response - the HttpResponse object
   * @return a ModelAndView object containing the content of the judger information page
   */
  @RequestMapping(value = "/judgers", method = RequestMethod.GET)
  public ModelAndView judgersView(HttpServletRequest request, HttpServletResponse response) {
    UserGroup userGroup = userService.getUserGroupUsingSlug("judgers");
    List<User> judgersList =
        userService.getUserUsingUserGroup(userGroup, 0, NUMBER_OF_JUDGERS_PER_REQUEST);

    List<Map<String, Object>> judgers = new ArrayList<>();
    long onlineJudgers = 0, cpuSum = 0, memorySum = 0, nodesWithMetrics = 0;
    for (User judger : judgersList) {
      String username = judger.getUsername();
      Map<String, Object> judgerInformation = keepAliveEventListener.getJudgerInformation(username);

      Map<String, Object> node = new HashMap<>();
      node.put("username", username);
      if (judgerInformation != null) {
        ++onlineJudgers;
        int cpuLoad = (Integer) judgerInformation.getOrDefault("cpuLoad", -1);
        int memoryUsage = (Integer) judgerInformation.getOrDefault("memoryUsage", -1);
        long uptime = (Long) judgerInformation.getOrDefault("uptime", 0L);

        node.put("online", true);
        node.put("description", judgerInformation.get("description"));
        node.put("cpuLoad", cpuLoad);
        node.put("memoryUsage", memoryUsage);
        node.put("uptime", formatUptime(uptime));
        if (cpuLoad >= 0) {
          cpuSum += cpuLoad;
          memorySum += Math.max(memoryUsage, 0);
          ++nodesWithMetrics;
        }
      } else {
        node.put("online", false);
      }
      judgers.add(node);
    }

    ModelAndView view = new ModelAndView("pages/judgers");
    view.addObject("judgers", judgers);
    view.addObject("onlineJudgers", onlineJudgers);
    view.addObject("totalJudgers", (long) judgers.size());
    view.addObject("averageCpuLoad", nodesWithMetrics > 0 ? cpuSum / nodesWithMetrics : 0);
    view.addObject("averageMemoryUsage", nodesWithMetrics > 0 ? memorySum / nodesWithMetrics : 0);
    return view;
  }

  /**
   * Formats a JVM uptime (in milliseconds) into a compact human-readable string.
   *
   * @param uptimeInMillis - the uptime in milliseconds
   * @return a compact uptime string such as "31d", "7h", "12m" or "45s"
   */
  private String formatUptime(long uptimeInMillis) {
    long minutes = uptimeInMillis / 60000;
    if (minutes >= 1440) {
      return (minutes / 1440) + "d";
    } else if (minutes >= 60) {
      return (minutes / 60) + "h";
    } else if (minutes >= 1) {
      return minutes + "m";
    }
    return (uptimeInMillis / 1000) + "s";
  }

  /**
   * Gets the list of judgers.
   *
   * @param offset - the UID of the currently-loaded judger
   * @param request - the HttpRequest object
   * @return a List<Map<String, String>> object containing the list of judgers
   */
  @RequestMapping(value = "/getJudgers.action", method = RequestMethod.GET)
  public @ResponseBody Map<String, Object> getJudgersAction(
      @RequestParam(value = "startIndex", required = false, defaultValue = "0") long offset,
      HttpServletRequest request) {
    Map<String, Object> result = new HashMap<>();
    List<Map<String, String>> judgers = getJudgers(offset);

    result.put("isSuccessful", judgers != null && !judgers.isEmpty());
    result.put("judgers", judgers);
    return result;
  }

  /**
   * Gets the detailed information of judgers.
   *
   * @param offset - the UID of the currently-loaded judger
   * @return a List<Map<String, String>> object containing the detailed information of judgers
   */
  private List<Map<String, String>> getJudgers(long offset) {
    UserGroup userGroup = userService.getUserGroupUsingSlug("judgers");
    List<User> judgersList =
        userService.getUserUsingUserGroup(userGroup, offset, NUMBER_OF_JUDGERS_PER_REQUEST);
    List<Map<String, String>> judgers = new ArrayList<Map<String, String>>();

    for (User judger : judgersList) {
      Map<String, String> judgerInformation = new HashMap<>(3, 1);
      String username = judger.getUsername();
      String description = keepAliveEventListener.getJudgerDescription(username);

      judgerInformation.put("username", username);
      judgerInformation.put("description", description);
      judgers.add(judgerInformation);
    }
    return judgers;
  }

  /**
   * Displays the help page.
   *
   * @param request - the HttpRequest object
   * @param response - the HttpResponse object
   * @return a ModelAndView object containing the content of the help page
   */
  @RequestMapping(value = "/help", method = RequestMethod.GET)
  public ModelAndView helpView(HttpServletRequest request, HttpServletResponse response) {
    ModelAndView view = new ModelAndView("pages/help");
    return view;
  }

  /**
   * Displays the about page.
   *
   * @param request - the HttpRequest object
   * @param response - the HttpResponse object
   * @return a ModelAndView object containing the content of the about page
   */
  @RequestMapping(value = "/about", method = RequestMethod.GET)
  public ModelAndView aboutView(HttpServletRequest request, HttpServletResponse response) {
    ModelAndView view = new ModelAndView("pages/about");
    return view;
  }

  /**
   * For all pages whose URLs are not properly mapped, displays the page not found.
   *
   * @param request - the HttpRequest object
   * @param response - the HttpResponse object
   * @return a ModelAndView object containing the exception information
   */
  @RequestMapping(value = "/*", method = RequestMethod.GET)
  public ModelAndView notFoundView(HttpServletRequest request, HttpServletResponse response) {
    ModelAndView view = new ModelAndView("pages/errors/404");
    return view;
  }

  /** The number of judgers to load per request. */
  private static final int NUMBER_OF_JUDGERS_PER_REQUEST = 10;

  /** The number of discussion threads to load per request. */
  private static final int NUMBER_OF_DISCUSSION_THREADS_PER_REQUEST = 10;

  /** The number of contests to load per request. */
  private static final int NUMBER_OF_CONTESTS_PER_REQUEST = 5;

  /** The number of bulletin board messages to load per request. */
  private static final int NUMBER_OF_BULLETIN_MESSAGES_PER_REQUEST = 10;

  /** The autowired UserService object. Used for getting the judger list of the judger page. */
  @Autowired private UserService userService;

  /** The autowired ProblemService object. Used for the home-page problem count. */
  @Autowired private ProblemService problemService;

  /** The autowired SubmissionService object. Used for the home-page submission count. */
  @Autowired private SubmissionService submissionService;

  /**
   * The autowired LanguageService object. Used for getting the compile commands of the judger page.
   */
  @Autowired private LanguageService languageService;

  /** The autowired ContestService object. Used for getting the latest contest information. */
  @Autowired private ContestService contestService;

  /**
   * The autowired DiscussionService object. Used for getting the discussion threads on the home
   * page.
   */
  @Autowired private DiscussionService discussionService;

  /**
   * The autowired BulletinBoardService object. Used for getting the latest messages of the bulletin
   * board.
   */
  @Autowired private BulletinBoardService bulletinBoardService;

  /**
   * The autowired ApplicationEventListener object. Used for getting the online status of judgers.
   */
  @Autowired private ApplicationEventListener keepAliveEventListener;
}
