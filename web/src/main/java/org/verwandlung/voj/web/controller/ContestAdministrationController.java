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

import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

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
import org.verwandlung.voj.web.interceptor.AdministrationSection;
import org.verwandlung.voj.web.model.Contest;
import org.verwandlung.voj.web.model.Problem;
import org.verwandlung.voj.web.model.ProblemDifficulty;
import org.verwandlung.voj.web.model.ProblemFilter;
import org.verwandlung.voj.web.model.User;
import org.verwandlung.voj.web.service.ContestService;
import org.verwandlung.voj.web.service.OptionService;
import org.verwandlung.voj.web.service.ProblemService;
import org.verwandlung.voj.web.util.JsonUtils;
import org.verwandlung.voj.web.util.LogSanitizer;
import org.verwandlung.voj.web.util.Pagination;

/**
 * The contest administration: contests and their contestants.
 *
 * @author Haozhe Xie
 */
@Controller
@RequestMapping(value = "/administration")
@AdministrationSection("contests")
public class ContestAdministrationController {
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
    final int NUMBER_OF_CONTESTS_PER_PAGE = optionService.getIntOption("contestsPerPage", 50);
    long totalContests = contestService.getNumberOfContests(keyword);
    long offset = Pagination.getOffset(pageNumber, NUMBER_OF_CONTESTS_PER_PAGE);
    List<Contest> contests =
        contestService.getContests(keyword, false, offset, NUMBER_OF_CONTESTS_PER_PAGE);

    Map<Long, String> contestPhases = new HashMap<>();
    Map<Long, Long> contestEntrants = contestService.getNumberOfContestantsOfContests(contests);
    Map<Long, String> contestDurations = new HashMap<>();
    for (Contest contest : contests) {
      long contestId = contest.getContestId();
      contestPhases.put(contestId, contestService.getContestStatus(contest).name());
      contestDurations.put(
          contestId, formatDuration(contest.getStartTime(), contest.getEndTime()));
    }

    ModelAndView view = new ModelAndView("pages/administration/all-contests");
    view.addObject("contests", contests);
    view.addObject("contestPhases", contestPhases);
    view.addObject("contestEntrants", contestEntrants);
    view.addObject("contestDurations", contestDurations);
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
    ModelAndView view = new ModelAndView("pages/administration/edit-contest");
    view.addObject("allProblems", getProblemsForPicker());
    return view;
  }

  /**
   * Builds the lightweight problem list consumed by the contest editor's problem picker.
   *
   * @return a List of {@code {id, title, diff, diffSlug}} maps, ordered by problem ID
   */
  private List<Map<String, Object>> getProblemsForPicker() {
    List<Problem> problems =
        problemService.getProblems(new ProblemFilter(), 0, Integer.MAX_VALUE);
    List<Map<String, Object>> picker = new ArrayList<>(problems.size());
    for (Problem problem : problems) {
      Map<String, Object> entry = new HashMap<>(4, 1);
      entry.put("id", problem.getProblemId());
      entry.put("title", problem.getProblemName());
      ProblemDifficulty difficulty = problem.getProblemDifficulty();
      entry.put("diff", difficulty != null ? difficulty.getProblemDifficultyName() : "Easy");
      entry.put("diffSlug", difficulty != null ? difficulty.getProblemDifficultySlug() : "easy");
      picker.add(entry);
    }
    return picker;
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
    view.addObject(
        "contestProblemIds",
        JsonUtils.toJsonString(contestService.getProblemIdsOfContest(contestId)));
    view.addObject("sectionLabel", contest.getContestName());
    view.addObject("contestPhase", contestService.getContestStatus(contest).name());
    view.addObject(
        "contestEntrants",
        contestService.getNumberOfContestantsOfContest(contest.getContestId()));
    view.addObject(
        "contestDuration", formatDuration(contest.getStartTime(), contest.getEndTime()));
    view.addObject("allProblems", getProblemsForPicker());
    view.addObject("contestants", contestService.getContestantsOfContest(contestId));
    return view;
  }

  /**
   * Registers a user for a contest, whatever its registration setting (the only way into a contest
   * whose registration is closed).
   *
   * @param contestId - the unique identifier of the contest
   * @param username - the username of the user to register
   * @param request - the HttpServletRequest object
   * @return the result of ContestService.addContestant
   */
  @RequestMapping(value = "/addContestant.action", method = RequestMethod.POST)
  public @ResponseBody Map<String, Object> addContestantAction(
      @RequestParam(value = "contestId") long contestId,
      @RequestParam(value = "username") String username,
      HttpServletRequest request) {
    Map<String, Object> result = contestService.addContestant(contestId, username);
    if ((Boolean) result.get("isSuccessful")) {
      LOGGER.info(
          String.format(
              "User[%s] was registered for contest #%d by an administrator.",
              LogSanitizer.forLog(username), contestId));
    }
    return result;
  }

  /**
   * Removes a contestant from a contest.
   *
   * @param contestId - the unique identifier of the contest
   * @param uid - the unique identifier of the contestant
   * @param request - the HttpServletRequest object
   * @return a Map whose {@code isSuccessful} tells whether the user was a contestant
   */
  @RequestMapping(value = "/removeContestant.action", method = RequestMethod.POST)
  public @ResponseBody Map<String, Boolean> removeContestantAction(
      @RequestParam(value = "contestId") long contestId,
      @RequestParam(value = "uid") long uid,
      HttpServletRequest request) {
    Map<String, Boolean> result = new HashMap<>(2, 1);
    result.put("isSuccessful", contestService.removeContestant(contestId, uid));
    return result;
  }

  /**
   * Creates a contest.
   *
   * @param contestName - the name of the contest
   * @param contestNotes - the notes/description of the contest
   * @param startTime - the start time (epoch milliseconds, see parseInstant)
   * @param endTime - the end time (epoch milliseconds, see parseInstant)
   * @param scoring - the scoring rule of the contest (ICPC / IOI)
   * @param problems - the contest problems (a JSON array of problem IDs, in order)
   * @param request - the HttpServletRequest object
   * @return a Map containing the creation result
   */
  @RequestMapping(value = "/createContest.action", method = RequestMethod.POST)
  public @ResponseBody Map<String, Object> createContestAction(
      @RequestParam(value = "contestName") String contestName,
      @RequestParam(value = "contestNotes", required = false, defaultValue = "") String contestNotes,
      @RequestParam(value = "startTime") String startTime,
      @RequestParam(value = "endTime") String endTime,
      @RequestParam(value = "scoring", required = false, defaultValue = "ICPC") String scoring,
      @RequestParam(value = "problems", required = false, defaultValue = "[]") String problems,
      @RequestParam(value = "status", required = false, defaultValue = "PUBLISHED") String status,
      @RequestParam(value = "penalty", required = false, defaultValue = "20") int penalty,
      @RequestParam(value = "freeze", required = false, defaultValue = "0") int freeze,
      @RequestParam(value = "openRegistration", required = false, defaultValue = "true")
          boolean openRegistration,
      HttpServletRequest request) {
    return contestService.createContest(
        contestName,
        contestNotes,
        parseInstant(startTime),
        parseInstant(endTime),
        scoring,
        problems,
        status,
        penalty,
        freeze,
        openRegistration);
  }

  /**
   * Edits a contest.
   *
   * @param contestId - the unique identifier of the contest
   * @param contestName - the name of the contest
   * @param contestNotes - the notes/description of the contest
   * @param startTime - the start time (epoch milliseconds, see parseInstant)
   * @param endTime - the end time (epoch milliseconds, see parseInstant)
   * @param scoring - the scoring rule of the contest (ICPC / IOI)
   * @param problems - the contest problems (a JSON array of problem IDs, in order)
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
      @RequestParam(value = "scoring", required = false, defaultValue = "ICPC") String scoring,
      @RequestParam(value = "problems", required = false, defaultValue = "[]") String problems,
      @RequestParam(value = "status", required = false, defaultValue = "PUBLISHED") String status,
      @RequestParam(value = "penalty", required = false, defaultValue = "20") int penalty,
      @RequestParam(value = "freeze", required = false, defaultValue = "0") int freeze,
      @RequestParam(value = "openRegistration", required = false, defaultValue = "true")
          boolean openRegistration,
      HttpServletRequest request) {
    return contestService.editContest(
        contestId,
        contestName,
        contestNotes,
        parseInstant(startTime),
        parseInstant(endTime),
        scoring,
        problems,
        status,
        penalty,
        freeze,
        openRegistration);
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
    List<Long> contestList = JsonUtils.toList(contests, Long.class);
    Map<String, Boolean> result = new HashMap<>(2, 1);
    result.put(
        "isSuccessful",
        BulkActions.run(
            "delete contests " + contestList, () -> contestService.deleteContests(contestList)));
    return result;
  }

  /**
   * Parses an instant posted by the contest editor: epoch milliseconds, or an ISO-8601 date-time
   * with an offset (e.g. {@code 2030-01-02T03:04:00+08:00}). A wall-clock time without an offset is
   * rejected: only the browser knows the admin's time zone, and reading such a value in the
   * server's zone would shift the contest by the difference between the two.
   *
   * @param value - the posted instant
   * @return the parsed Date, or null when empty or unparseable
   */
  static Date parseInstant(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    String instant = value.trim();
    try {
      return instant.chars().allMatch(Character::isDigit)
          ? new Date(Long.parseLong(instant))
          : Date.from(OffsetDateTime.parse(instant).toInstant());
    } catch (NumberFormatException | DateTimeParseException ex) {
      return null;
    }
  }

  /**
   * Formats a contest's duration (end - start) as an {@code HH:MM} string, where hours may exceed
   * 24 for multi-day contests.
   *
   * @param startTime - the start time of the contest
   * @param endTime - the end time of the contest
   * @return the duration formatted as {@code HH:MM}, or an empty string when either time is missing
   */
  private static String formatDuration(Date startTime, Date endTime) {
    if (startTime == null || endTime == null) {
      return "";
    }
    long minutes = (endTime.getTime() - startTime.getTime()) / 60000L;
    if (minutes < 0) {
      minutes = 0;
    }
    return String.format("%02d:%02d", minutes / 60, minutes % 60);
  }

  /** The autowired ProblemService object. Used for getting problem record information. */
  @Autowired private ProblemService problemService;

  /** The autowired ContestService object. Used for contest counts and the contest admin. */
  @Autowired private ContestService contestService;

  /** The autowired OptionService object. Used for getting the settings options of the system. */
  @Autowired private OptionService optionService;

  /** The logger. */
  private static final Logger LOGGER = LogManager.getLogger(ContestAdministrationController.class);
}
