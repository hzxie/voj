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

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import org.verwandlung.voj.web.exception.ResourceNotFoundException;
import org.verwandlung.voj.web.model.*;
import org.verwandlung.voj.web.service.DiscussionService;
import org.verwandlung.voj.web.service.LanguageService;
import org.verwandlung.voj.web.service.ProblemService;
import org.verwandlung.voj.web.service.SubmissionService;
import org.verwandlung.voj.web.util.CsrfProtector;
import org.verwandlung.voj.web.util.HttpRequestParser;
import org.verwandlung.voj.web.util.HttpSessionParser;

/**
 * Handles users' requests of viewing problems / making submissions, etc.
 *
 * @author Haozhe Xie
 */
@Controller
@RequestMapping(value = "/p")
public class ProblemsController {
  /**
   * Displays all the problems in the problem bank.
   *
   * @param startIndex - the start index of the problems
   * @param keyword - the keyword
   * @param problemCategorySlug - the slug of the problem category
   * @param request - the HttpRequest object
   * @param response - the HttpResponse object
   * @return a ModelAndView object containing the information of the problem bank page
   * @throws UnsupportedEncodingException
   */
  @RequestMapping(value = "", method = RequestMethod.GET)
  public ModelAndView problemsView(
      @RequestParam(value = "start", required = false, defaultValue = "1") long startIndex,
      @RequestParam(value = "keyword", required = false) String keyword,
      @RequestParam(value = "category", required = false) String problemCategorySlug,
      HttpServletRequest request,
      HttpServletResponse response)
      throws UnsupportedEncodingException {
    long startIndexOfProblems = getFirstIndexOfProblems();
    if (startIndex < startIndexOfProblems) {
      startIndex = startIndexOfProblems;
    }

    List<Problem> problems =
        problemService.getProblemsUsingFilters(
            startIndex, keyword, problemCategorySlug, null, true, NUMBER_OF_PROBLEMS_PER_PAGE);
    long totalProblems =
        problemService.getNumberOfProblemsUsingFilters(keyword, problemCategorySlug, true);
    ModelAndView view = new ModelAndView("problems/problems");
    view.addObject("problems", problems)
        .addObject("startIndexOfProblems", startIndexOfProblems)
        .addObject("numberOfProblemsPerPage", NUMBER_OF_PROBLEMS_PER_PAGE)
        .addObject("totalProblems", totalProblems)
        .addObject("problemCategories", problemService.getProblemCategoriesWithHierarchy());

    HttpSession session = request.getSession();
    if (isLoggedIn(session)) {
      long userId = (Long) session.getAttribute("uid");
      long endIndex =
          problemService.getLastIndexOfProblems(true, startIndex, NUMBER_OF_PROBLEMS_PER_PAGE);
      Map<Long, Submission> submissionOfProblems =
          submissionService.getSubmissionOfProblems(userId, startIndex, endIndex);
      view.addObject("submissionOfProblems", submissionOfProblems);
    }
    return view;
  }

  /**
   * Gets the start index of the problems.
   *
   * @return the start index of the problems
   */
  private long getFirstIndexOfProblems() {
    return problemService.getFirstIndexOfProblems();
  }

  /**
   * Gets the list of problems.
   *
   * @param startIndex - the start index of the problems
   * @param request - the HttpRequest object
   * @return a HashMap object containing the list of problems
   */
  @RequestMapping(value = "/getProblems.action", method = RequestMethod.GET)
  public @ResponseBody Map<String, Object> getProblemsAction(
      @RequestParam(value = "startIndex") long startIndex,
      @RequestParam(value = "keyword", required = false) String keyword,
      @RequestParam(value = "category", required = false) String problemCategorySlug,
      HttpServletRequest request) {
    HttpSession session = request.getSession();
    List<Problem> problems =
        problemService.getProblemsUsingFilters(
            startIndex, keyword, problemCategorySlug, null, true, NUMBER_OF_PROBLEMS_PER_PAGE);
    Map<Long, Submission> submissionOfProblems = null;
    if (isLoggedIn(session)) {
      long userId = (Long) session.getAttribute("uid");
      submissionOfProblems =
          submissionService.getSubmissionOfProblems(
              userId, startIndex, startIndex + NUMBER_OF_PROBLEMS_PER_PAGE);
    }

    Map<String, Object> result = new HashMap<>(4, 1);
    result.put("isSuccessful", problems != null && !problems.isEmpty());
    result.put("problems", problems);
    result.put("submissionOfProblems", submissionOfProblems);
    return result;
  }

  /**
   * Checks whether the user has logged in.
   *
   * @param session - the HttpSession object
   * @return whether the user has logged in
   */
  private boolean isLoggedIn(HttpSession session) {
    Boolean isLoggedIn = (Boolean) session.getAttribute("isLoggedIn");
    if (isLoggedIn == null || !isLoggedIn.booleanValue()) {
      return false;
    }
    return true;
  }

  /**
   * Loads the detailed information of a problem.
   *
   * @param problemId - the unique identifier of the problem
   * @param request - the HttpRequest object
   * @param response - the HttpResponse object
   * @return a ModelAndView object containing the detailed information of the problem
   */
  @RequestMapping(value = "/{problemId}", method = RequestMethod.GET)
  public ModelAndView problemView(
      @PathVariable("problemId") long problemId,
      HttpServletRequest request,
      HttpServletResponse response) {
    HttpSession session = request.getSession();
    boolean isLoggedIn = isLoggedIn(session);
    Problem problem = problemService.getProblem(problemId);

    if (problem == null) {
      throw new ResourceNotFoundException();
    } else if (!problem.isPublic()) {
      boolean isAllowToAccess = false;

      if (isLoggedIn) {
        User currentUser = HttpSessionParser.getCurrentUser(session);
        if (currentUser.getUserGroup().getUserGroupSlug().equals("administrators")) {
          isAllowToAccess = true;
        }
      }
      if (!isAllowToAccess) {
        throw new ResourceNotFoundException();
      }
    }

    ModelAndView view = new ModelAndView("problems/problem");
    view.addObject("problem", problem);
    view.addObject(
        "discussionThreads",
        discussionService.getDiscussionThreadsOfProblem(
            problemId, 0, NUMBER_OF_DISCUSSTION_THREADS_PER_PROBLEM));
    if (isLoggedIn) {
      long userId = (Long) session.getAttribute("uid");
      Map<Long, Submission> submissionOfProblems =
          submissionService.getSubmissionOfProblems(userId, problemId, problemId + 1);
      List<Submission> submissions =
          submissionService.getSubmissionUsingProblemIdAndUserId(
              problemId, userId, NUMBER_OF_SUBMISSIONS_PER_PROBLEM);
      List<Language> languages = languageService.getAllLanguages();

      view.addObject("latestSubmission", submissionOfProblems);
      view.addObject("submissions", submissions);
      view.addObject("languages", languages);
      view.addObject("csrfToken", CsrfProtector.getCsrfToken(session));
    }
    return view;
  }

  /**
   * Loads the problem solution page.
   *
   * @param problemId - the unique identifier of the problem
   * @param request - the HttpRequest object
   * @param response - the HttpResponse object
   * @return a ModelAndView object containing the problem solution information
   */
  @RequestMapping(value = "/{problemId}/solution", method = RequestMethod.GET)
  public ModelAndView solutionView(
      @PathVariable("problemId") long problemId,
      HttpServletRequest request,
      HttpServletResponse response) {
    DiscussionThread discussionThread = discussionService.getSolutionThreadOfProblem(problemId);
    if (discussionThread == null) {
      throw new ResourceNotFoundException();
    }

    ModelAndView view = new ModelAndView("discussion/thread");
    view.addObject("discussionThread", discussionThread);
    return view;
  }

  /**
   * Creates a submission.
   *
   * @param problemId - the unique identifier of the problem
   * @param languageSlug - the slug of the programming language
   * @param code - the code
   * @param csrfToken - the token used to prevent CSRF attacks
   * @param request - the HttpRequest object
   * @return a Map<String, Object> object containing the result of the submission record creation
   */
  @RequestMapping(value = "/createSubmission.action", method = RequestMethod.POST)
  public @ResponseBody Map<String, Object> createSubmissionAction(
      @RequestParam(value = "problemId") long problemId,
      @RequestParam(value = "languageSlug") String languageSlug,
      @RequestParam(value = "code") String code,
      @RequestParam(value = "csrfToken") String csrfToken,
      HttpServletRequest request) {
    HttpSession session = request.getSession();
    String ipAddress = HttpRequestParser.getRemoteAddr(request);
    User currentUser = HttpSessionParser.getCurrentUser(session);
    boolean isCsrfTokenValid = CsrfProtector.isCsrfTokenValid(csrfToken, session);

    Map<String, Object> result =
        submissionService.createSubmission(
            currentUser, problemId, languageSlug, code, isCsrfTokenValid);
    boolean isSuccessful = (Boolean) result.get("isSuccessful");
    if (isSuccessful) {
      long submissionId = (Long) result.get("submissionId");
      LOGGER.info(
          String.format(
              "User: {%s} submitted code with SubmissionId #%s at %s",
              new Object[] {currentUser, submissionId, ipAddress}));
    }
    return result;
  }

  /** The number of problems to load per request. */
  private static final int NUMBER_OF_PROBLEMS_PER_PAGE = 100;

  /** The number of recent submissions to load per problem. */
  private static final int NUMBER_OF_SUBMISSIONS_PER_PROBLEM = 10;

  /** The number of discussions to load per problem. */
  private static final int NUMBER_OF_DISCUSSTION_THREADS_PER_PROBLEM = 10;

  /** The autowired ProblemService object. Used for completing the logic operations of problems. */
  @Autowired private ProblemService problemService;

  /**
   * The autowired SubmissionService object. Used for handling the submission requests of the problem
   * detail page.
   */
  @Autowired private SubmissionService submissionService;

  /**
   * The autowired LanguageService object. Used for loading the language options of the problem
   * detail page.
   */
  @Autowired private LanguageService languageService;

  /** The autowired DiscussionService object. Used for getting the discussions related to problems. */
  @Autowired private DiscussionService discussionService;

  /** The logger. */
  private static final Logger LOGGER = LogManager.getLogger(ProblemsController.class);
}
