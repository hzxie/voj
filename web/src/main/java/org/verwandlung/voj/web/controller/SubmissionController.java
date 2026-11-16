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
import org.verwandlung.voj.web.model.PublicationStatus;
import org.verwandlung.voj.web.model.Submission;
import org.verwandlung.voj.web.model.User;
import org.verwandlung.voj.web.service.ContestService;
import org.verwandlung.voj.web.service.OptionService;
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
    final int NUMBER_OF_SUBMISSION_PER_PAGE =
        optionService.getIntOption("submissionsPerPage", DEFAULT_NUMBER_OF_SUBMISSION_PER_PAGE);
    User currentUser = HttpSessionParser.getCurrentUser();
    List<Submission> submissions =
        submissionService.getSubmissions(
            problemId, username, NUMBER_OF_SUBMISSION_PER_PAGE, currentUser);
    redactSubmissions(submissions, currentUser);
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

    final int NUMBER_OF_SUBMISSION_PER_PAGE =
        optionService.getIntOption("submissionsPerPage", DEFAULT_NUMBER_OF_SUBMISSION_PER_PAGE);
    User currentUser = HttpSessionParser.getCurrentUser();
    List<Submission> submissions =
        submissionService.getSubmissions(
            problemId, username, startIndex, NUMBER_OF_SUBMISSION_PER_PAGE, currentUser);
    redactSubmissions(submissions, currentUser);
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

    final int NUMBER_OF_SUBMISSION_PER_PAGE =
        optionService.getIntOption("submissionsPerPage", DEFAULT_NUMBER_OF_SUBMISSION_PER_PAGE);
    User currentUser = HttpSessionParser.getCurrentUser();
    List<Submission> submissions =
        submissionService.getLatestSubmissions(
            problemId, username, startIndex, NUMBER_OF_SUBMISSION_PER_PAGE, currentUser);
    redactSubmissions(submissions, currentUser);
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
    User currentUser = HttpSessionParser.getCurrentUser();
    if (submission == null || !contestService.isSubmissionVisible(submission, currentUser)) {
      throw new ResourceNotFoundException();
    }
    boolean canViewCode = canViewSourceCode(submission, currentUser);
    // The page template already gates the source code behind canViewCode (only its length stays
    // public), so the code itself is left in place here; only the compiler-error log, which echoes
    // the source verbatim, needs redacting for viewers who may not see the code.
    redactCompilerErrorLog(submission, canViewCode);
    ModelAndView view = new ModelAndView("pages/submissions/submission");
    view.addObject("submission", submission);
    view.addObject("canViewCode", canViewCode);
    return view;
  }

  /**
   * Strips the source code (and any source-echoing compiler-error log) from every submission in a
   * list the current viewer is not allowed to see, applying the same per-submission visibility rule
   * as the detail pages. The submission-list views never render the source, but the list records are
   * serialised verbatim to JSON by the {@code getSubmissions}/{@code getLatestSubmissions} actions,
   * so without this another user's code would leak through those endpoints.
   *
   * @param submissions - the submission records to redact in place, may be {@code null}
   * @param currentUser - the currently logged-in user, or null
   */
  private void redactSubmissions(List<Submission> submissions, User currentUser) {
    if (submissions == null) {
      return;
    }
    for (Submission submission : submissions) {
      boolean canViewCode = canViewSourceCode(submission, currentUser);
      if (!canViewCode) {
        submission.setCode(null);
      }
      redactCompilerErrorLog(submission, canViewCode);
    }
  }

  /**
   * Clears a submission's judge log when it echoes source the viewer may not see. The compiler-error
   * log reproduces the submitted source verbatim, so it must follow the same visibility rule as the
   * code itself; judge logs for other verdicts (compile success, runtime results) carry no source
   * and are left intact.
   *
   * @param submission - the submission record to redact in place, may be {@code null}
   * @param canViewCode - whether the current viewer may see the submission's source code
   */
  private void redactCompilerErrorLog(Submission submission, boolean canViewCode) {
    if (submission == null || canViewCode) {
      return;
    }
    if (submission.getJudgeResult() != null
        && "CE".equals(submission.getJudgeResult().getJudgeResultSlug())) {
      submission.setJudgeLog(null);
    }
  }

  /**
   * Determines whether the given viewer is allowed to see a submission's source code. A submission's
   * source is private: only its owner and administrators may ever see it. Everyone else - including
   * anonymous visitors - is denied, regardless of any site-wide option.
   *
   * @param submission - the submission record
   * @param currentUser - the currently logged-in user, or null
   * @return whether the viewer may see the submission's source code
   */
  private boolean canViewSourceCode(Submission submission, User currentUser) {
    if (currentUser == null) {
      return false;
    }
    if (currentUser.equals(submission.getUser())) {
      return true;
    }
    return PublicationStatus.isAdministrator(currentUser);
  }

  /**
   * Gets the real-time judge result. It is a GET, the only method an EventSource can send, so it
   * needs no CSRF token (Spring Security checks the token on state-changing methods only): the
   * stream only reads, and only to the submission's owner and administrators.
   *
   * @param submissionId - the unique identifier of the submission record
   * @return an SseEmitter object containing the judge result information
   * @throws IOException
   */
  @RequestMapping(value = "/getRealTimeJudgeResult.action", method = RequestMethod.GET)
  public SseEmitter getRealTimeJudgeResultAction(
      @RequestParam(value = "submissionId") long submissionId,
      HttpServletRequest request,
      HttpServletResponse response)
      throws IOException {
    User currentUser = HttpSessionParser.getCurrentUser();
    // The stream is registered before the submission is read: an outcome recorded after the read
    // found the submission pending is then sent to it, whereas one sent between the read and a
    // later registration would reach no stream, which would wait for nothing. It opens with a
    // comment, which the page ignores, so that the response starts at once (the stream holds it
    // until it is returned, and sends it first).
    SseEmitter sseEmitter = new SseEmitter(STREAM_TIMEOUT_MS);
    sseEmitter.send(SseEmitter.event().comment("connected"));
    submissionEventListener.addSseEmitters(submissionId, sseEmitter);
    // Unless the stream is returned, it never starts, so it never completes either: it is
    // unregistered here, whatever the checks throw (e.g. the database is unreachable).
    boolean isReturned = false;
    try {
      Submission submission = submissionService.getSubmission(submissionId);

      // Anyone allowed to see the source code may also follow the live judge log (which can echo
      // the source for compiler errors): the owner and administrators (see canViewSourceCode).
      // Without the administrators, the administration console's edit-submission page could never
      // subscribe to another user's pending submission.
      if (submission == null
          || !contestService.isSubmissionVisible(submission, currentUser)
          || !canViewSourceCode(submission, currentUser)
          || !submission.getJudgeResult().getJudgeResultSlug().equals("PD")) {
        throw new ResourceNotFoundException();
      }

      response.addHeader("X-Accel-Buffering", "no");
      isReturned = true;
      return sseEmitter;
    } finally {
      if (!isReturned) {
        submissionEventListener.removeSseEmitter(submissionId, sseEmitter);
      }
    }
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

    User currentUser = HttpSessionParser.getCurrentUser();
    Submission submission = submissionService.getSubmission(submissionId);
    if (submission != null && !contestService.isSubmissionVisible(submission, currentUser)) {
      submission = null;
    }
    if (submission != null) {
      boolean canViewCode = canViewSourceCode(submission, currentUser);
      if (!canViewCode) {
        submission.setCode(null);
      }
      redactCompilerErrorLog(submission, canViewCode);
    }
    result.put("isSuccessful", submission != null);
    result.put("submission", submission);

    return result;
  }

  /**
   * How long (ms) a live stream of a judging lasts. The browser then opens another one, which is
   * refused once the submission is no longer pending.
   */
  private static final long STREAM_TIMEOUT_MS = 600000;

  /** The default number of submission records to load per request when the admin option is unset. */
  private static final int DEFAULT_NUMBER_OF_SUBMISSION_PER_PAGE = 100;

  /** The autowired SubmissionService object. */
  @Autowired private SubmissionService submissionService;

  /**
   * The autowired ContestService object. Decides who may see a submission (unpublished problems,
   * running contests).
   */
  @Autowired private ContestService contestService;

  /** The autowired OptionService object. Used to read the number of submissions per page. */
  @Autowired private OptionService optionService;

  /** The autowired ApplicationEventListener object. Used for registering the sseEmitter. */
  @Autowired private ApplicationEventListener submissionEventListener;

  /** The logger. */
  @SuppressWarnings("unused")
  private static final Logger LOGGER = LogManager.getLogger(SubmissionController.class);
}
