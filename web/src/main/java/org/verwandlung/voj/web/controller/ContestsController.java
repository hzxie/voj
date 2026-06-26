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

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;
import org.verwandlung.voj.web.exception.ResourceNotFoundException;
import org.verwandlung.voj.web.model.*;
import org.verwandlung.voj.web.service.ContestService;
import org.verwandlung.voj.web.service.LanguageService;
import org.verwandlung.voj.web.service.ProblemService;
import org.verwandlung.voj.web.service.SubmissionService;
import org.verwandlung.voj.web.util.HttpRequestParser;
import org.verwandlung.voj.web.util.HttpSessionParser;
import org.verwandlung.voj.web.util.JsonUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Handles the requests related to contests.
 *
 * @author Haozhe Xie
 */
@Controller
@RequestMapping(value = "/contest")
public class ContestsController {
  /**
   * Displays the contest list page.
   *
   * @param keyword - the keyword of the contest
   * @param request - the HttpRequest object
   * @param response - the HttpResponse object
   * @return a ModelAndView object containing the content of the contest list page
   */
  @RequestMapping(value = "", method = RequestMethod.GET)
  public ModelAndView contestsView(
      @RequestParam(value = "keyword", required = false) String keyword,
      HttpServletRequest request,
      HttpServletResponse response) {
    List<Contest> contests = contestService.getContests(keyword, 0, NUMBER_OF_CONTESTS_PER_PAGE);
    Date currentTime = new Date();
    List<Contest> liveContests = new ArrayList<>();
    List<Contest> pastContests = new ArrayList<>();
    for (Contest contest : contests) {
      if (currentTime.after(contest.getEndTime())) {
        pastContests.add(contest);
      } else {
        liveContests.add(contest);
      }
    }

    ModelAndView view = new ModelAndView("pages/contests/contests");
    view.addObject("liveContests", liveContests);
    view.addObject("pastContests", pastContests);
    view.addObject("loadedContests", contests.size());
    view.addObject("currentTime", currentTime);
    view.addObject("entrantsOfContests", getEntrantsOfContests(contests));
    view.addObject("problemCountOfContests", getProblemCountOfContests(contests));
    return view;
  }

  /**
   * Builds a map of contest id to its number of entrants, used by the contest list to render the
   * "entrants" field of the design without an N+1 lookup in the view layer.
   *
   * @param contests - the list of contests to summarise
   * @return a Map indexed by contest id whose value is the number of entrants
   */
  private Map<Long, Long> getEntrantsOfContests(List<Contest> contests) {
    Map<Long, Long> entrants = new HashMap<>();
    if (contests != null) {
      for (Contest contest : contests) {
        entrants.put(
            contest.getContestId(),
            contestService.getNumberOfContestantsOfContest(contest.getContestId()));
      }
    }
    return entrants;
  }

  /**
   * Builds a map of contest id to its number of problems (derived from the JSON-encoded problem list
   * stored on the contest), used to render the "problems" field of the design.
   *
   * @param contests - the list of contests to summarise
   * @return a Map indexed by contest id whose value is the number of problems
   */
  private Map<Long, Integer> getProblemCountOfContests(List<Contest> contests) {
    Map<Long, Integer> problemCount = new HashMap<>();
    if (contests != null) {
      for (Contest contest : contests) {
        List<Long> problemIds = JsonUtils.toList(contest.getProblems(), Long.class);
        problemCount.put(contest.getContestId(), problemIds == null ? 0 : problemIds.size());
      }
    }
    return problemCount;
  }

  /**
   * Gets the list of contests.
   *
   * @param keyword - the keyword of the contest
   * @param startIndex - the index value of the last currently-loaded record
   * @param request - the HttpRequest object
   * @return a HashMap object containing the list of contests
   */
  @RequestMapping(value = "/getContests.action", method = RequestMethod.GET)
  public @ResponseBody Map<String, Object> getContestsAction(
      @RequestParam(value = "keyword", required = false) String keyword,
      @RequestParam(value = "startIndex") long startIndex,
      HttpServletRequest request) {
    Map<String, Object> result = new HashMap<>(3, 1);

    List<Contest> contests =
        contestService.getContests(keyword, startIndex, NUMBER_OF_CONTESTS_PER_PAGE);
    result.put("isSuccessful", contests != null && !contests.isEmpty());
    result.put("contests", contests);
    result.put("entrants", getEntrantsOfContests(contests));
    result.put("problemCount", getProblemCountOfContests(contests));

    return result;
  }

  /**
   * Displays the page of the detailed information of a contest.
   *
   * @param contestId - the unique identifier of the contest
   * @param request - the HttpRequest object
   * @param response - the HttpResponse object
   * @return a ModelAndView object containing the detailed information of the contest
   */
  @RequestMapping(value = "/{contestId}", method = RequestMethod.GET)
  public ModelAndView contestView(
      @PathVariable("contestId") long contestId,
      HttpServletRequest request,
      HttpServletResponse response) {
    HttpSession session = request.getSession();
    User currentUser = HttpSessionParser.getCurrentUser(session);
    Contest contest = contestService.getContest(contestId);
    if (contest == null) {
      throw new ResourceNotFoundException();
    }

    Date currentTime = new Date();
    boolean isAttended = contestService.isAttendContest(contestId, currentUser);
    long numberOfContestants = contestService.getNumberOfContestantsOfContest(contestId);
    List<Long> problemIdList = JsonUtils.toList(contest.getProblems(), Long.class);
    List<Problem> problems = contestService.getProblemsOfContests(problemIdList);
    Map<Long, ContestSubmission> submissions =
        contestService.getSubmissionsOfContestantOfContest(contestId, currentUser);

    // Top of the standings for the design's // STANDINGS panel. Only computed once the
    // contest has started (the leaderboard is meaningless before then).
    List<ContestContestant> standings = null;
    if (!currentTime.before(contest.getStartTime())) {
      Map<String, Object> leaderBoard =
          "OI".equals(contest.getContestMode())
              ? contestService.getLeaderBoardForOi(contestId)
              : contestService.getLeaderBoardForAcm(contestId);
      List<ContestContestant> contestants =
          (List<ContestContestant>) leaderBoard.get("contestants");
      if (contestants != null) {
        standings = contestants.size() > 10 ? contestants.subList(0, 10) : contestants;
      }
    }

    ModelAndView view = new ModelAndView("pages/contests/contest");
    view.addObject("currentTime", currentTime)
        .addObject("contest", contest)
        .addObject("problems", problems)
        .addObject("submissions", submissions)
        .addObject("isAttended", isAttended)
        .addObject("numberOfContestants", numberOfContestants)
        .addObject("standings", standings);
    return view;
  }

  /**
   * Handles the request of a user attending a contest.
   *
   * @param contestId - the unique identifier of the contest
   * @param request - the HttpRequest object
   * @param response - the HttpResponse object
   * @return a Map object containing the status information of whether the contest was attended
   *     successfully
   */
  @RequestMapping(value = "/{contestId}/attend.action", method = RequestMethod.POST)
  public @ResponseBody Map<String, Boolean> attendContestAction(
      @PathVariable("contestId") long contestId,
      HttpServletRequest request,
      HttpServletResponse response) {
    String ipAddress = HttpRequestParser.getRemoteAddr(request);
    User currentUser = HttpSessionParser.getCurrentUser();

    Map<String, Boolean> result = contestService.attendContest(contestId, currentUser);
    if (result.get("isSuccessful")) {
      LOGGER.info(
          String.format(
              "User: {%s} attended contest #%d at %s",
              new Object[] {currentUser, contestId, ipAddress}));
    }
    return result;
  }

  /**
   * Displays the leaderboard.
   *
   * @param contestId - the unique identifier of the contest
   * @param request - the HttpRequest object
   * @param response - the HttpResponse object
   * @return a ModelAndView object containing the contest leaderboard
   */
  @RequestMapping(value = "/{contestId}/leaderboard", method = RequestMethod.GET)
  public ModelAndView leaderboardView(
      @PathVariable("contestId") long contestId,
      HttpServletRequest request,
      HttpServletResponse response) {
    Contest contest = contestService.getContest(contestId);
    Date currentTime = new Date();
    if (contest == null
        || contest.getStartTime().after(currentTime)
        || !(contest.getContestMode().equals("OI") || contest.getContestMode().equals("ACM"))) {
      throw new ResourceNotFoundException();
    }

    List<Long> problemIdList = JsonUtils.toList(contest.getProblems(), Long.class);
    List<Problem> problems = contestService.getProblemsOfContests(problemIdList);
    ModelAndView view = null;
    Map<String, Object> result = null;

    if (contest.getContestMode().equals("OI")) {
      view = new ModelAndView("pages/contests/leaderboard-oi");
      result = contestService.getLeaderBoardForOi(contestId);
    } else if (contest.getContestMode().equals("ACM")) {
      view = new ModelAndView("pages/contests/leaderboard-acm");
      result = contestService.getLeaderBoardForAcm(contestId);
    } else {
      throw new ResourceNotFoundException();
    }
    List<ContestContestant> contestants = (List<ContestContestant>) result.get("contestants");
    Map<Long, Map<Long, Submission>> submissions =
        (Map<Long, Map<Long, Submission>>) result.get("submissions");
    view.addObject("contestants", contestants);
    view.addObject("submissions", submissions);
    view.addObject("contest", contest);
    view.addObject("problems", problems);
    return view;
  }

  /**
   * Displays the information of a problem in a contest.
   *
   * @param contestId - the unique identifier of the contest
   * @param problemId - the unique identifier of the problem
   * @param request - the HttpRequest object
   * @param response - the HttpResponse object
   * @return a ModelAndView object containing the information of the contest problem
   */
  @RequestMapping(value = "/{contestId}/p/{problemId}", method = RequestMethod.GET)
  public ModelAndView problemView(
      @PathVariable("contestId") long contestId,
      @PathVariable("problemId") long problemId,
      HttpServletRequest request,
      HttpServletResponse response) {
    HttpSession session = request.getSession();
    User currentUser = HttpSessionParser.getCurrentUser(session);
    Contest contest = contestService.getContest(contestId);
    if (contest == null) {
      throw new ResourceNotFoundException();
    }
    // The problem does not exist in the contest's problem list
    List<Long> problems = JsonUtils.toList(contest.getProblems(), Long.class);
    if (!problems.contains(problemId)) {
      throw new ResourceNotFoundException();
    }
    // Users who have not attended the contest cannot view the problem before the contest ends
    Date currentTime = new Date();
    boolean isAttended = contestService.isAttendContest(contestId, currentUser);
    if (contest.getEndTime().after(currentTime) && !isAttended) {
      throw new ResourceNotFoundException();
    }

    Problem problem = problemService.getProblem(problemId);
    List<Language> languages = languageService.getAllLanguages();
    Map<String, String> codeSnippet =
        contestService.getCodeSnippetOfContestProblem(contest, problemId, currentUser);
    List<Submission> submissions =
        contestService.getSubmissionsOfContestantOfContestProblem(contest, problemId, currentUser);
    ModelAndView view = new ModelAndView("pages/problems/problem");
    view.addObject("contest", contest);
    view.addObject("problem", problem);
    view.addObject("languages", languages);
    view.addObject("codeSnippet", codeSnippet);
    view.addObject("submissions", submissions);
    view.addObject("currentTime", currentTime);
    view.addObject("isContest", true);
    return view;
  }

  /** The number of contests to load per query. */
  private static final int NUMBER_OF_CONTESTS_PER_PAGE = 10;

  /** The autowired ContestService object. */
  @Autowired private ContestService contestService;

  /** The autowired ProblemService object. Used for querying problem information. */
  @Autowired private ProblemService problemService;

  /**
   * The autowired LanguageService object. Used for loading the language options of the problem
   * detail page.
   */
  @Autowired private LanguageService languageService;

  /** The autowired SubmissionService object. Used for creating submission records. */
  @Autowired private SubmissionService subsmissionService;

  /** The logger. */
  private static final Logger LOGGER = LogManager.getLogger(ContestsController.class);
}
