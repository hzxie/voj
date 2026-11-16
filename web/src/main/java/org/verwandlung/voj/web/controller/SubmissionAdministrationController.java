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
import org.verwandlung.voj.web.model.Submission;
import org.verwandlung.voj.web.service.OptionService;
import org.verwandlung.voj.web.service.SubmissionService;
import org.verwandlung.voj.web.util.JsonUtils;
import org.verwandlung.voj.web.util.LogSanitizer;

/**
 * The submission administration: listing, viewing, rejudging and deleting submissions.
 *
 * @author Haozhe Xie
 */
@Controller
@RequestMapping(value = "/administration")
@AdministrationSection("submissions")
public class SubmissionAdministrationController {
  /**
   * Loads the submission list page.
   *
   * @param problemId - the unique identifier of the problem the submission corresponds to
   * @param username - the username of the submitter
   * @param judgeResult - the slug of the judge result to filter by, or empty for all
   * @param pageNumber - the page number of the current page
   * @param request - the HttpServletRequest object
   * @param response - the HttpServletResponse object
   * @return a ModelAndView object containing the information of the submission list page
   */
  @RequestMapping(value = "/all-submissions", method = RequestMethod.GET)
  public ModelAndView allSubmissionsView(
      @RequestParam(value = "problemId", required = false, defaultValue = "0") long problemId,
      @RequestParam(value = "username", required = false, defaultValue = "") String username,
      @RequestParam(value = "judgeResult", required = false, defaultValue = "") String judgeResult,
      @RequestParam(value = "page", required = false, defaultValue = "1") long pageNumber,
      HttpServletRequest request,
      HttpServletResponse response) {
    final int NUMBER_OF_SUBMISSIONS_PER_PAGE = optionService.getIntOption("submissionsPerPage", 100);

    long totalSubmissions =
        submissionService.getNumberOfSubmissionsUsingProblemIdAndUsername(
            problemId, username, judgeResult);
    List<Submission> submissions =
        submissionService.getSubmissionsByPage(
            problemId, username, judgeResult, pageNumber, NUMBER_OF_SUBMISSIONS_PER_PAGE);

    ModelAndView view = new ModelAndView("pages/administration/all-submissions");
    view.addObject("problemId", problemId);
    view.addObject("username", username);
    view.addObject("judgeResult", judgeResult);
    view.addObject("judgeResults", submissionService.getJudgeResults());
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
    List<Long> submissionList = JsonUtils.toList(submissions, Long.class);
    List<Long> deletedSubmissions = new ArrayList<>();
    boolean isSuccessful =
        BulkActions.run(
            "delete submissions " + submissionList,
            () -> deletedSubmissions.addAll(submissionService.deleteSubmissions(submissionList)));
    if (isSuccessful) {
      LOGGER.info(
          String.format(
              "Submissions: %s were deleted by administrator at %s.",
              deletedSubmissions, LogSanitizer.forLog(request.getRemoteAddr())));
    }
    Map<String, Object> result = new HashMap<>(3, 1);
    result.put("isSuccessful", isSuccessful);
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
    List<Long> submissionList = JsonUtils.toList(submissions, Long.class);
    Map<String, Boolean> result = new HashMap<>(2, 1);
    result.put(
        "isSuccessful",
        BulkActions.run(
            "rejudge submissions " + submissionList,
            () -> submissionService.rejudgeSubmissions(submissionList)));
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

  /** The autowired SubmissionService object. Used for getting submission record information. */
  @Autowired private SubmissionService submissionService;

  /** The autowired OptionService object. Used for getting the settings options of the system. */
  @Autowired private OptionService optionService;

  /** The logger. */
  private static final Logger LOGGER = LogManager.getLogger(SubmissionAdministrationController.class);
}
