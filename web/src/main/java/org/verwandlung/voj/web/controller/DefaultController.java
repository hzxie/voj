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
import org.verwandlung.voj.web.util.LocaleUtils;

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
        bulletinBoardService.getBulletinBoardMessages(0, NUMBER_OF_BULLETIN_MESSAGES_PER_REQUEST);

    ModelAndView view = new ModelAndView("pages/index");
    view.addObject("currentTime", new Date());
    view.addObject("contests", contests);
    view.addObject("discussionThreads", discussionThreads);
    view.addObject("bulletinBoardMessages", bulletinBoardMessages);
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
   * Displays the judger information page.
   *
   * @param request - the HttpRequest object
   * @param response - the HttpResponse object
   * @return a ModelAndView object containing the content of the judger information page
   */
  @RequestMapping(value = "/judgers", method = RequestMethod.GET)
  public ModelAndView judgersView(HttpServletRequest request, HttpServletResponse response) {
    ModelAndView view = new ModelAndView("pages/judgers");
    view.addObject("languages", languageService.getAllLanguages());
    return view;
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
   * Displays the language switching page.
   *
   * @param request - the HttpRequest object
   * @param response - the HttpResponse object
   * @return a ModelAndView object containing the content of the language switching page
   */
  @RequestMapping(value = "/worldwide", method = RequestMethod.GET)
  public ModelAndView worldwideView(
      @RequestParam(value = "forward", required = false, defaultValue = "") String forwardUrl,
      HttpServletRequest request,
      HttpServletResponse response) {
    ModelAndView view = new ModelAndView("pages/worldwide");
    view.addObject("forwardUrl", forwardUrl);
    return view;
  }

  /**
   * Handles the request of a user switching the language.
   *
   * @param language - the language code to switch to
   * @param request - the HttpRequest object
   * @param response - the HttpResponse object
   * @return a HashMap<String, Boolean> object containing the result of the language switch operation
   */
  @RequestMapping(value = "/worldwide.action", method = RequestMethod.GET)
  public @ResponseBody Map<String, Boolean> localizationAction(
      @RequestParam(value = "language") String language,
      HttpServletRequest request,
      HttpServletResponse response) {
    LocaleUtils.setLocale(request, response, language);

    Map<String, Boolean> result = new HashMap<>(2, 1);
    result.put("isSuccessful", true);
    return result;
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

  /**
   * Displays the browser upgrade page.
   *
   * @param request - the HttpRequest object
   * @param response - the HttpResponse object
   * @return a ModelAndView object containing the content of the browser upgrade page
   */
  @RequestMapping(value = "/not-supported", method = RequestMethod.GET)
  public ModelAndView notSupportedView(HttpServletRequest request, HttpServletResponse response) {
    ModelAndView view = new ModelAndView("pages/errors/not-supported");
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
