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

import java.io.IOException;
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
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import org.verwandlung.voj.web.exception.ResourceNotFoundException;
import org.verwandlung.voj.web.messenger.ApplicationEventListener;
import org.verwandlung.voj.web.model.Submission;
import org.verwandlung.voj.web.model.User;
import org.verwandlung.voj.web.service.SubmissionService;
import org.verwandlung.voj.web.util.HttpSessionParser;

/**
 * Loads/displays information related to submissions.
 *
 * @author Haozhe Xie
 */
@Controller
@RequestMapping(value = "/submission")
public class SubmissionController {
  /**
   * Displays the page of the submission list.
   *
   * @param problemId - the unique identifier of the problem
   * @param username - the username of the user
   * @param request - the HttpRequest object
   * @param response - the HttpResponse object
   * @return a ModelAndView object containing the submission list
   */
  @RequestMapping(value = "", method = RequestMethod.GET)
  public ModelAndView submissionsView(
      @RequestParam(value = "problemId", required = false, defaultValue = "0") long problemId,
      @RequestParam(value = "username", required = false, defaultValue = "") String username,
      HttpServletRequest request,
      HttpServletResponse response) {
    List<Submission> submissions =
        submissionService.getSubmissions(problemId, username, NUMBER_OF_SUBMISSION_PER_PAGE);
    return new ModelAndView("pages/submissions/submissions").addObject("submissions", submissions);
  }

  /**
   * Gets the list of historical submissions.
   *
   * @param problemId - the unique identifier of the problem
   * @param username - the username of the user
   * @param startIndex - the submission unique identifier of the last currently-loaded record
   * @param request - the HttpRequest object
   * @return a HashMap object containing the list of submission records
   */
  @RequestMapping(value = "/getSubmissions.action", method = RequestMethod.GET)
  public @ResponseBody Map<String, Object> getSubmissionsAction(
      @RequestParam(value = "problemId", required = false, defaultValue = "0") long problemId,
      @RequestParam(value = "username", required = false, defaultValue = "") String username,
      @RequestParam(value = "startIndex") long startIndex,
      HttpServletRequest request) {
    Map<String, Object> result = new HashMap<>(3, 1);

    List<Submission> submissions =
        submissionService.getSubmissions(
            problemId, username, startIndex, NUMBER_OF_SUBMISSION_PER_PAGE);
    result.put("isSuccessful", submissions != null && !submissions.isEmpty());
    result.put("submissions", submissions);

    return result;
  }

  /**
   * Gets the list of the latest submissions.
   *
   * @param problemId - the unique identifier of the problem
   * @param username - the username of the user
   * @param startIndex - the submission unique identifier of the latest currently-loaded record
   * @param request - the HttpRequest object
   * @return a HashMap object containing the list of submission records
   */
  @RequestMapping(value = "/getLatestSubmissions.action", method = RequestMethod.GET)
  public @ResponseBody Map<String, Object> getLatestSubmissionsAction(
      @RequestParam(value = "problemId", required = false, defaultValue = "0") long problemId,
      @RequestParam(value = "username", required = false, defaultValue = "") String username,
      @RequestParam(value = "startIndex") long startIndex,
      HttpServletRequest request) {
    Map<String, Object> result = new HashMap<>(3, 1);

    List<Submission> submissions =
        submissionService.getLatestSubmissions(
            problemId, username, startIndex, NUMBER_OF_SUBMISSION_PER_PAGE);
    result.put("isSuccessful", submissions != null && !submissions.isEmpty());
    result.put("submissions", submissions);

    return result;
  }

  /**
   * Displays the page of the detailed information of a submission record.
   *
   * @param submissionId - the unique identifier of the submission record
   * @param request - the HttpRequest object
   * @param response - the HttpResponse object
   * @return a ModelAndView object containing the detailed information of the submission
   */
  @RequestMapping(value = "/{submissionId}", method = RequestMethod.GET)
  public ModelAndView submissionView(
      @PathVariable("submissionId") long submissionId,
      HttpServletRequest request,
      HttpServletResponse response) {
    Submission submission = submissionService.getSubmission(submissionId);
    if (submission == null) {
      throw new ResourceNotFoundException();
    }
    ModelAndView view = new ModelAndView("pages/submissions/submission");
    view.addObject("submission", submission);
    return view;
  }

  /**
   * Gets the real-time judge result.
   *
   * @param submissionId - the unique identifier of the submission record
   * @return an SseEmitter object containing the judge result information
   * @throws IOException
   */
  @RequestMapping("/getRealTimeJudgeResult.action")
  public SseEmitter getRealTimeJudgeResultAction(
      @RequestParam(value = "submissionId") long submissionId,
      HttpServletRequest request,
      HttpServletResponse response)
      throws IOException {
    User currentUser = HttpSessionParser.getCurrentUser(request.getSession());
    Submission submission = submissionService.getSubmission(submissionId);

    if (submission == null
        || !submission.getUser().equals(currentUser)
        || !submission.getJudgeResult().getJudgeResultSlug().equals("PD")) {
      throw new ResourceNotFoundException();
    }

    response.addHeader("X-Accel-Buffering", "no");
    SseEmitter sseEmitter = new SseEmitter();
    submissionEventListener.addSseEmitters(submissionId, sseEmitter);
    sseEmitter.send("Established");
    return sseEmitter;
  }

  /**
   * Gets the detailed information of a submission record.
   *
   * @param submissionId - the unique identifier of the submission record
   * @param request - the HttpRequest object
   * @return a HashMap object containing the detailed information of the submission record
   */
  @RequestMapping(value = "/getSubmission.action", method = RequestMethod.GET)
  public @ResponseBody Map<String, Object> getSubmissionAction(
      @RequestParam(value = "submissionId") long submissionId, HttpServletRequest request) {
    Map<String, Object> result = new HashMap<>(3, 1);

    Submission submission = submissionService.getSubmission(submissionId);
    result.put("isSuccessful", submission != null);
    result.put("submission", submission);

    return result;
  }

  /** The number of submission records to load per request. */
  private static final int NUMBER_OF_SUBMISSION_PER_PAGE = 100;

  /** The autowired SubmissionService object. */
  @Autowired private SubmissionService submissionService;

  /** The autowired ApplicationEventListener object. Used for registering the sseEmitter. */
  @Autowired private ApplicationEventListener submissionEventListener;

  /** The logger. */
  @SuppressWarnings("unused")
  private static final Logger LOGGER = LogManager.getLogger(SubmissionController.class);
}
