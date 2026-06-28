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
import org.springframework.web.servlet.view.RedirectView;
import org.verwandlung.voj.web.exception.ResourceNotFoundException;
import org.verwandlung.voj.web.model.*;
import org.verwandlung.voj.web.service.DiscussionService;
import org.verwandlung.voj.web.service.OptionService;
import org.verwandlung.voj.web.service.ProblemService;
import org.verwandlung.voj.web.util.HttpRequestParser;
import org.verwandlung.voj.web.util.HttpSessionParser;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Handles the requests related to discussions.
 *
 * @author Haozhe Xie
 */
@Controller
@RequestMapping(value = "/discussion")
public class DiscussionController {
  /**
   * Displays the discussion list page.
   *
   * @param discussionTopicSlug - the unique slug of the discussion topic
   * @param problemId - the unique identifier of the problem
   * @param request - the HttpRequest object
   * @param response - the HttpResponse object
   * @return a ModelAndView object containing the content of the discussion list page
   */
  @RequestMapping(value = "", method = RequestMethod.GET)
  public ModelAndView discussionThreadsView(
      @RequestParam(value = "topicSlug", required = false, defaultValue = "")
          String discussionTopicSlug,
      @RequestParam(value = "problemId", required = false, defaultValue = "-1") long problemId,
      HttpServletRequest request,
      HttpServletResponse response) {
    final int NUMBER_OF_THREADS_PER_REQUEST =
        optionService.getIntOption("discussionsPerPage", DEFAULT_NUMBER_OF_THREADS_PER_REQUEST);
    List<DiscussionThread> discussionThreads = null;
    if (problemId != -1) {
      discussionThreads =
          discussionService.getDiscussionThreadsOfProblem(
              problemId, 0, NUMBER_OF_THREADS_PER_REQUEST);
    } else {
      discussionThreads =
          discussionService.getDiscussionThreadsOfTopic(
              discussionTopicSlug, 0, NUMBER_OF_THREADS_PER_REQUEST);
    }

    ModelAndView view = new ModelAndView("pages/discussion/threads");
    view.addObject("selectedTopicSlug", discussionTopicSlug);
    view.addObject("problemId", problemId);
    view.addObject("discussionThreads", discussionThreads);
    view.addObject("discussionTopics", discussionService.getDiscussionTopicsWithHierarchy());
    return view;
  }

  /**
   * Gets the list of discussion threads.
   *
   * @param startIndex - the offset for getting the discussion threads
   * @param discussionTopicSlug - the unique slug of the discussion topic
   * @param problemId - the unique identifier of the problem
   * @return a HashMap object containing the list of discussion threads
   */
  @RequestMapping(value = "/getDiscussionThreads.action", method = RequestMethod.GET)
  public @ResponseBody Map<String, Object> getDiscussionThreadsAction(
      @RequestParam(value = "startIndex") long startIndex,
      @RequestParam(value = "topicSlug", required = false, defaultValue = "")
          String discussionTopicSlug,
      @RequestParam(value = "problemId", required = false, defaultValue = "-1") long problemId,
      HttpServletRequest request) {
    if (startIndex < 0) {
      startIndex = 0;
    }
    final int NUMBER_OF_THREADS_PER_REQUEST =
        optionService.getIntOption("discussionsPerPage", DEFAULT_NUMBER_OF_THREADS_PER_REQUEST);
    List<DiscussionThread> discussionThreads = null;
    if (problemId != -1) {
      discussionThreads =
          discussionService.getDiscussionThreadsOfProblem(
              problemId, startIndex, NUMBER_OF_THREADS_PER_REQUEST);
    } else {
      discussionThreads =
          discussionService.getDiscussionThreadsOfTopic(
              discussionTopicSlug, startIndex, NUMBER_OF_THREADS_PER_REQUEST);
    }

    Map<String, Object> result = new HashMap<>(3, 1);
    result.put("isSuccessful", discussionThreads != null && !discussionThreads.isEmpty());
    result.put("discussionThreads", discussionThreads);
    return result;
  }

  /**
   * Displays the discussion detail page.
   *
   * @param discussionThreadId - the unique identifier of the discussion thread
   * @param request - the HttpRequest object
   * @param response - the HttpResponse object
   * @return a ModelAndView object containing the content of the discussion detail page
   */
  @RequestMapping(value = "/{threadId}", method = RequestMethod.GET)
  public ModelAndView discussionThreadView(
      @PathVariable("threadId") long discussionThreadId,
      HttpServletRequest request,
      HttpServletResponse response) {
    DiscussionThread discussionThread =
        discussionService.getDiscussionThreadForDisplay(discussionThreadId);
    if (discussionThread == null) {
      throw new ResourceNotFoundException();
    }
    // A thread hidden by reports or by an administrator is treated as not found for everyone
    // except administrators.
    if ((discussionService.isDiscussionThreadHidden(discussionThreadId)
            || !discussionThread.isVisible())
        && !isAdministrator(HttpSessionParser.getCurrentUser())) {
      throw new ResourceNotFoundException();
    }

    HttpSession session = request.getSession();
    ModelAndView view = new ModelAndView("pages/discussion/thread");
    view.addObject("discussionThread", discussionThread);
    view.addObject("minSolvedToVote", discussionService.getMinSolvedToVote());
    view.addObject("minSolvedToReport", discussionService.getMinSolvedToReport());
    view.addObject("postDelayMinutes", discussionService.getNewUserPostDelay());
    if (isLoggedIn(session)) {
      List<DiscussionTopic> discussionTopics = discussionService.getDiscussionTopics();
      view.addObject("discussionTopics", discussionTopics);
    }
    return view;
  }

  /**
   * Displays the create discussion page.
   *
   * @param request - the HttpRequest object
   * @param response - the HttpResponse object
   * @return a ModelAndView object containing the content of the create discussion page
   */
  @RequestMapping(value = "/new", method = RequestMethod.GET)
  public ModelAndView newDiscussionThreadView(
      @RequestParam(value = "problemId", required = false, defaultValue = "-1") long problemId,
      HttpServletRequest request,
      HttpServletResponse response) {
    HttpSession session = request.getSession();
    ModelAndView view = null;
    if (!isLoggedIn(session)) {
      RedirectView redirectView = new RedirectView(request.getContextPath() + "/accounts/login");
      redirectView.setExposeModelAttributes(false);
      view = new ModelAndView(redirectView);
    } else {
      List<DiscussionTopic> discussionTopics = discussionService.getDiscussionTopics();
      Problem problem = null;
      if (problemId != -1) {
        problem = problemService.getProblem(problemId);
      }

      view = new ModelAndView("pages/discussion/new-thread");
      view.addObject("discussionTopics", discussionTopics);
      view.addObject("relatedProblem", problem);
      view.addObject("postDelayMinutes", discussionService.getNewUserPostDelay());
    }
    return view;
  }

  /**
   * Checks whether the user has logged in.
   *
   * @param session - the HttpSession object
   * @return whether the user has logged in
   */
  private boolean isLoggedIn(HttpSession session) {
    return HttpSessionParser.getCurrentUser() != null;
  }

  /**
   * Gets the discussion replies.
   *
   * @param discussionThreadId - the unique identifier of the discussion thread
   * @param startIndex - the start offset of the discussion replies (the number of replies already
   *     fetched)
   * @param request - the HttpServletRequest object
   * @return a Map object containing the list of discussion replies (DiscussionReply)
   */
  @RequestMapping(value = "/{threadId}/getDiscussionReplies.action", method = RequestMethod.GET)
  public @ResponseBody Map<String, Object> getDiscussionRepliesAction(
      @PathVariable("threadId") long discussionThreadId,
      @RequestParam(value = "startIndex") long startIndex,
      HttpServletRequest request) {
    long currentUserUid = getUidOfUserLoggedIn(request.getSession());
    if (startIndex < 0) {
      startIndex = 0;
    }

    List<DiscussionReply> discussionReplies =
        discussionService.getDiscussionRepliesOfThread(
            discussionThreadId, currentUserUid, startIndex, NUMBER_OF_REPLIES_PER_REQUEST, true);
    Map<String, Object> result = new HashMap<>(3, 1);
    result.put("isSuccessful", discussionReplies != null && !discussionReplies.isEmpty());
    result.put("discussionReplies", discussionReplies);
    return result;
  }

  /**
   * Gets the unique user identifier of the currently logged-in user.
   *
   * @param session - the HttpSession object
   * @return the unique user identifier of the currently logged-in user
   */
  private long getUidOfUserLoggedIn(HttpSession session) {
    User currentUser = HttpSessionParser.getCurrentUser();
    if (currentUser == null) {
      return -1;
    }
    return currentUser.getUid();
  }

  /**
   * Handles the request of a user voting on a discussion reply.
   *
   * @param discussionThreadId - the unique identifier of the discussion thread
   * @param discussionReplyId - the unique identifier of the discussion reply
   * @param voteUp - the vote-up status (+1 means the user upvoted this reply, -1 means the user
   *     canceled the upvote, 0 means no operation)
   * @param voteDown - the vote-down status (+1 means the user downvoted this reply, -1 means the user
   *     canceled the downvote, 0 means no operation)
   * @param request - the HttpServletRequest object
   * @return a JSON object containing the result of handling the discussion reply vote request
   */
  @RequestMapping(value = "/{threadId}/voteDiscussionReply.action", method = RequestMethod.POST)
  public @ResponseBody Map<String, Boolean> voteDiscussionReplyAction(
      @PathVariable("threadId") long discussionThreadId,
      @RequestParam(value = "discussionReplyId") long discussionReplyId,
      @RequestParam(value = "voteUp") int voteUp,
      @RequestParam(value = "voteDown") int voteDown,
      HttpServletRequest request) {
    String ipAddress = HttpRequestParser.getRemoteAddr(request);
    User currentUser = HttpSessionParser.getCurrentUser();

    Map<String, Boolean> result =
        discussionService.voteDiscussionReply(
            discussionThreadId, discussionReplyId, currentUser, voteUp, voteDown);
    if (result.get("isSuccessful")) {
      LOGGER.info(
          String.format(
              "User: {%s} voted discussion reply #%d {Up: %d, Down: %d} at %s",
              new Object[] {currentUser, discussionReplyId, voteUp, voteDown, ipAddress}));
    }
    return result;
  }

  /**
   * Handles the request of a user reporting a discussion reply for abuse.
   *
   * @param discussionThreadId - the unique identifier of the discussion thread
   * @param discussionReplyId - the unique identifier of the discussion reply
   * @param request - the HttpServletRequest object
   * @return a JSON object containing the result of handling the report request
   */
  @RequestMapping(value = "/{threadId}/reportDiscussionReply.action", method = RequestMethod.POST)
  public @ResponseBody Map<String, Boolean> reportDiscussionReplyAction(
      @PathVariable("threadId") long discussionThreadId,
      @RequestParam(value = "discussionReplyId") long discussionReplyId,
      HttpServletRequest request) {
    String ipAddress = HttpRequestParser.getRemoteAddr(request);
    User currentUser = HttpSessionParser.getCurrentUser();

    Map<String, Boolean> result =
        discussionService.reportDiscussionReply(
            discussionThreadId, discussionReplyId, currentUser);
    if (result.get("isSuccessful")) {
      LOGGER.info(
          String.format(
              "User: {%s} reported discussion reply #%d at %s",
              new Object[] {currentUser, discussionReplyId, ipAddress}));
    }
    return result;
  }

  /**
   * Checks whether a user belongs to the administrators user group.
   *
   * @param user - the user to check (may be null)
   * @return whether the user is an administrator
   */
  private boolean isAdministrator(User user) {
    return user != null
        && user.getUserGroup() != null
        && "administrators".equals(user.getUserGroup().getUserGroupSlug());
  }

  /**
   * Handles the request of a user creating a discussion thread.
   *
   * @param discussionTopicSlug - the unique slug of the topic the discussion thread belongs to
   * @param relatedProblemIdString - the unique identifier of the problem the discussion thread is
   *     related to
   * @param discussionThreadTitle - the title of the discussion thread
   * @param request - the HttpServletRequest object
   * @return a JSON object containing the result of the discussion thread creation
   */
  @RequestMapping(value = "/createDiscussionThread.action", method = RequestMethod.POST)
  public @ResponseBody Map<String, Object> createDiscussionThreadAction(
      @RequestParam(value = "discussionTopicSlug") String discussionTopicSlug,
      @RequestParam(value = "relatedProblemId") String relatedProblemIdString,
      @RequestParam(value = "threadTitle") String discussionThreadTitle,
      @RequestParam(value = "threadContent") String discussionThreadContent,
      HttpServletRequest request) {
    String ipAddress = HttpRequestParser.getRemoteAddr(request);
    User currentUser = HttpSessionParser.getCurrentUser();
    long relatedProblemId = parseRelatedProblemId(relatedProblemIdString);

    Map<String, Object> result =
        discussionService.createDiscussionThread(
            currentUser,
            discussionTopicSlug,
            relatedProblemId,
            discussionThreadTitle,
            discussionThreadContent);
    if ((Boolean) result.get("isSuccessful")) {
      LOGGER.info(
          String.format(
              "User: {%s} created discussion thread[Title=%s] at %s",
              new Object[] {currentUser, discussionThreadTitle, ipAddress}));
    }
    return result;
  }

  /**
   * Parses the optional related problem identifier submitted with a new discussion thread. The value
   * may be empty (no related problem) or contain surrounding whitespace, so it is trimmed before
   * parsing; any non-numeric value is treated as "no related problem".
   *
   * @param relatedProblemIdString - the raw {@code relatedProblemId} request parameter
   * @return the related problem identifier, or {@code -1} if absent or malformed
   */
  private long parseRelatedProblemId(String relatedProblemIdString) {
    if (relatedProblemIdString == null) {
      return -1;
    }
    String trimmed = relatedProblemIdString.trim();
    if (trimmed.isEmpty()) {
      return -1;
    }
    try {
      return Long.parseLong(trimmed);
    } catch (NumberFormatException e) {
      return -1;
    }
  }

  /**
   * Handles the request of a user editing a discussion thread.
   *
   * @param discussionThreadId - the unique identifier of the discussion thread
   * @param discussionTopicSlug - the unique slug of the topic the discussion thread belongs to
   * @param discussionThreadTitle - the title of the discussion thread
   * @param request - the HttpServletRequest object
   * @return a JSON object containing the result of the discussion thread edit
   */
  @RequestMapping(value = "/editDiscussionThread.action", method = RequestMethod.POST)
  public @ResponseBody Map<String, Boolean> editDiscussionThreadAction(
      @RequestParam(value = "discussionThreadId") long discussionThreadId,
      @RequestParam(value = "discussionTopicSlug") String discussionTopicSlug,
      @RequestParam(value = "discussionThreadTitle") String discussionThreadTitle,
      HttpServletRequest request) {
    String ipAddress = HttpRequestParser.getRemoteAddr(request);
    User currentUser = HttpSessionParser.getCurrentUser();

    Map<String, Boolean> result =
        discussionService.editDiscussionThread(
            discussionThreadId, currentUser, discussionTopicSlug, discussionThreadTitle);
    if (result.get("isSuccessful")) {
      LOGGER.info(
          String.format(
              "User: {%s} voted discussion thread #%d at %s",
              new Object[] {currentUser, discussionThreadId, ipAddress}));
    }
    return result;
  }

  /**
   * Handles the request of a user creating a discussion reply.
   *
   * @param discussionThreadId - the unique identifier of the discussion thread
   * @param replyContent - the content of the discussion reply
   * @param request - the HttpServletRequest object
   * @return a JSON object containing the result of the discussion reply creation
   */
  @RequestMapping(value = "/{threadId}/createDiscussionReply.action", method = RequestMethod.POST)
  public @ResponseBody Map<String, Object> createDiscussionReplyAction(
      @PathVariable("threadId") long discussionThreadId,
      @RequestParam(value = "replyContent") String replyContent,
      HttpServletRequest request) {
    String ipAddress = HttpRequestParser.getRemoteAddr(request);
    User currentUser = HttpSessionParser.getCurrentUser();

    Map<String, Object> result =
        discussionService.createDiscussionReply(discussionThreadId, currentUser, replyContent);
    if ((Boolean) result.get("isSuccessful")) {
      LOGGER.info(
          String.format(
              "User: {%s} created discussion reply for thread #%d at %s",
              new Object[] {currentUser, discussionThreadId, ipAddress}));
    }
    return result;
  }

  /**
   * Handles the request of a user editing a discussion reply.
   *
   * @param discussionReplyId - the unique identifier of the discussion reply
   * @param replyContent - the content of the discussion reply
   * @param request - the HttpServletRequest object
   * @return a JSON object containing the result of the discussion reply edit
   */
  @RequestMapping(value = "/{threadId}/editDiscussionReply.action", method = RequestMethod.POST)
  public @ResponseBody Map<String, Boolean> editDiscussionReplyAction(
      @PathVariable("threadId") long discussionThreadId,
      @RequestParam(value = "discussionReplyId") long discussionReplyId,
      @RequestParam(value = "replyContent") String replyContent,
      HttpServletRequest request) {
    String ipAddress = HttpRequestParser.getRemoteAddr(request);
    User currentUser = HttpSessionParser.getCurrentUser();

    Map<String, Boolean> result =
        discussionService.editDiscussionReply(discussionReplyId, currentUser, replyContent);
    if (result.get("isSuccessful")) {
      LOGGER.info(
          String.format(
              "User: {%s} edited discussion reply #%d at %s",
              new Object[] {currentUser, discussionReplyId, ipAddress}));
    }
    return result;
  }

  /**
   * Handles the request of a user deleting a discussion reply.
   *
   * @param discussionReplyId - the unique identifier of the discussion reply
   * @param request - the HttpServletRequest object
   * @return a JSON object containing the result of the discussion reply deletion
   */
  @RequestMapping(value = "/{threadId}/deleteDiscussionReply.action", method = RequestMethod.POST)
  public @ResponseBody Map<String, Boolean> deleteDiscussionReplyAction(
      @PathVariable("threadId") long discussionThreadId,
      @RequestParam(value = "discussionReplyId") long discussionReplyId,
      HttpServletRequest request) {
    String ipAddress = HttpRequestParser.getRemoteAddr(request);
    User currentUser = HttpSessionParser.getCurrentUser();

    Map<String, Boolean> result =
        discussionService.deleteDiscussionReply(discussionReplyId, currentUser);
    if (result.get("isSuccessful")) {
      LOGGER.info(
          String.format(
              "User: {%s} deleted discussion reply #%d at %s",
              new Object[] {currentUser, discussionReplyId, ipAddress}));
    }
    return result;
  }

  /** The default number of discussion threads per request when the admin option is unset. */
  private static final int DEFAULT_NUMBER_OF_THREADS_PER_REQUEST = 50;

  /** The number of discussion replies per request. */
  private static final int NUMBER_OF_REPLIES_PER_REQUEST = 50;

  /** The autowired DiscussionService object. */
  @Autowired private DiscussionService discussionService;

  /** The autowired OptionService object. Used for reading the configurable list-display options. */
  @Autowired private OptionService optionService;

  /** The autowired ProblemService object. */
  @Autowired private ProblemService problemService;

  /** The logger. */
  private static final Logger LOGGER = LogManager.getLogger(DiscussionController.class);
}
