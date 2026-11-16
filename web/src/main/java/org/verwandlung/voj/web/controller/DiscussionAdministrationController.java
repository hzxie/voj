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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

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
import org.verwandlung.voj.web.model.DiscussionReply;
import org.verwandlung.voj.web.model.DiscussionThread;
import org.verwandlung.voj.web.service.DiscussionService;
import org.verwandlung.voj.web.service.OptionService;
import org.verwandlung.voj.web.util.HttpSessionParser;
import org.verwandlung.voj.web.util.JsonUtils;
import org.verwandlung.voj.web.util.Pagination;

/**
 * The discussion administration: threads, replies, topics and the moderation queue.
 *
 * @author Haozhe Xie
 */
@Controller
@RequestMapping(value = "/administration")
@AdministrationSection("discussion")
public class DiscussionAdministrationController {
  /**
   * Loads the discussion thread list page.
   *
   * @param pageNumber - the page number of the current page
   * @param request - the HttpServletRequest object
   * @param response - the HttpServletResponse object
   * @return a ModelAndView object containing the discussion threads
   */
  @RequestMapping(value = "/all-discussion", method = RequestMethod.GET)
  public ModelAndView allDiscussionView(
      @RequestParam(value = "page", required = false, defaultValue = "1") long pageNumber,
      HttpServletRequest request,
      HttpServletResponse response) {
    final int NUMBER_OF_THREADS_PER_PAGE = optionService.getIntOption("discussionsPerPage", 50);
    long totalThreads = discussionService.getNumberOfDiscussionThreads();
    long offset = Pagination.getOffset(pageNumber, NUMBER_OF_THREADS_PER_PAGE);

    ModelAndView view = new ModelAndView("pages/administration/all-discussion");
    view.addObject("threads", discussionService.getAllDiscussionThreads(offset, NUMBER_OF_THREADS_PER_PAGE));
    view.addObject("currentPage", pageNumber);
    view.addObject("totalPages", (long) Math.ceil(totalThreads * 1.0 / NUMBER_OF_THREADS_PER_PAGE));
    return view;
  }

  /**
   * Loads the discussion thread moderation page.
   *
   * @param threadId - the unique identifier of the discussion thread
   * @param request - the HttpServletRequest object
   * @param response - the HttpServletResponse object
   * @return a ModelAndView object containing the thread, its replies and reports
   */
  @RequestMapping(value = "/edit-thread/{threadId}", method = RequestMethod.GET)
  public ModelAndView editThreadView(
      @PathVariable(value = "threadId") long threadId,
      HttpServletRequest request,
      HttpServletResponse response) {
    DiscussionThread thread = discussionService.getDiscussionThreadUsingThreadId(threadId);
    if (thread == null) {
      throw new ResourceNotFoundException();
    }
    ModelAndView view = new ModelAndView("pages/administration/edit-thread");
    view.addObject("thread", thread);
    view.addObject(
        "replies", discussionService.getDiscussionRepliesOfThread(threadId, -1, 0, 200, false));
    view.addObject("topics", discussionService.getDiscussionTopics());
    view.addObject("reporters", discussionService.getReportersOfThread(threadId));
    view.addObject("sectionLabel", thread.getDiscussionThreadTitle());
    return view;
  }

  /**
   * Saves the moderation-editable fields of a discussion thread: its title, category, visibility,
   * pinned and locked flags.
   *
   * @param threadId - the unique identifier of the discussion thread
   * @param title - the new title of the thread
   * @param topicId - the unique identifier of the new category (topic)
   * @param visible - whether the thread is visible to ordinary users
   * @param pinned - whether the thread is pinned to the top of its list
   * @param locked - whether the thread is locked against new replies
   * @param request - the HttpServletRequest object
   * @return the update result
   */
  @RequestMapping(value = "/saveThread.action", method = RequestMethod.POST)
  public @ResponseBody Map<String, Boolean> saveThreadAction(
      @RequestParam(value = "threadId") long threadId,
      @RequestParam(value = "title") String title,
      @RequestParam(value = "topicId") int topicId,
      @RequestParam(value = "visible") boolean visible,
      @RequestParam(value = "pinned") boolean pinned,
      @RequestParam(value = "locked") boolean locked,
      HttpServletRequest request) {
    Map<String, Boolean> result = new HashMap<>(2, 1);
    result.put(
        "isSuccessful",
        discussionService.saveThread(threadId, title, topicId, visible, pinned, locked));
    return result;
  }

  /**
   * Toggles the visibility of a discussion reply.
   *
   * @param replyId - the unique identifier of the discussion reply
   * @param visible - whether the reply should be visible to ordinary users
   * @param request - the HttpServletRequest object
   * @return the update result
   */
  @RequestMapping(value = "/toggleReplyVisibility.action", method = RequestMethod.POST)
  public @ResponseBody Map<String, Boolean> toggleReplyVisibilityAction(
      @RequestParam(value = "replyId") long replyId,
      @RequestParam(value = "visible") boolean visible,
      HttpServletRequest request) {
    Map<String, Boolean> result = new HashMap<>(2, 1);
    result.put("isSuccessful", discussionService.setReplyVisibility(replyId, visible));
    return result;
  }

  /**
   * Deletes the selected discussion threads.
   *
   * @param threads - the set of thread IDs, separated by commas
   * @param request - the HttpServletRequest object
   * @return the deletion result
   */
  @RequestMapping(value = "/deleteThreads.action", method = RequestMethod.POST)
  public @ResponseBody Map<String, Boolean> deleteThreadsAction(
      @RequestParam(value = "threads") String threads, HttpServletRequest request) {
    List<Long> threadList = JsonUtils.toList(threads, Long.class);
    Map<String, Boolean> result = new HashMap<>(2, 1);
    result.put(
        "isSuccessful",
        BulkActions.run(
            "delete discussion threads " + threadList,
            () -> discussionService.deleteDiscussionThreads(threadList)));
    return result;
  }

  /**
   * Toggles the visibility of a discussion thread.
   *
   * @param threadId - the unique identifier of the discussion thread
   * @param visible - whether the thread should be visible to ordinary users
   * @param request - the HttpServletRequest object
   * @return the update result
   */
  @RequestMapping(value = "/toggleThreadVisibility.action", method = RequestMethod.POST)
  public @ResponseBody Map<String, Boolean> toggleThreadVisibilityAction(
      @RequestParam(value = "threadId") long threadId,
      @RequestParam(value = "visible") boolean visible,
      HttpServletRequest request) {
    Map<String, Boolean> result = new HashMap<>(2, 1);
    result.put("isSuccessful", discussionService.setThreadVisibility(threadId, visible));
    return result;
  }

  /**
   * Deletes a single discussion reply.
   *
   * @param replyId - the unique identifier of the discussion reply
   * @param request - the HttpServletRequest object
   * @return the deletion result
   */
  @RequestMapping(value = "/deleteReply.action", method = RequestMethod.POST)
  public @ResponseBody Map<String, Boolean> deleteReplyAction(
      @RequestParam(value = "replyId") long replyId, HttpServletRequest request) {
    DiscussionReply reply = discussionService.getDiscussionReplyUsingReplyId(replyId);
    if (reply == null) {
      Map<String, Boolean> result = new HashMap<>(2, 1);
      result.put("isSuccessful", false);
      return result;
    }
    return discussionService.deleteDiscussionReply(
        reply.getDiscussionThreadId(), replyId, HttpSessionParser.getCurrentUser());
  }

  /**
   * Loads the moderation queue: every discussion reply with at least one outstanding report.
   *
   * @param request - the HttpServletRequest object
   * @param response - the HttpServletResponse object
   * @return a ModelAndView object containing the reported replies awaiting review
   */
  @AdministrationSection("moderation")
  @RequestMapping(value = "/moderation", method = RequestMethod.GET)
  public ModelAndView moderationView(
      HttpServletRequest request, HttpServletResponse response) {
    ModelAndView view = new ModelAndView("pages/administration/moderation");
    view.addObject("reportedReplies", discussionService.getReportedReplies(MODERATION_QUEUE_LIMIT));
    view.addObject("moderationCount", discussionService.getNumberOfReportedReplies());
    return view;
  }

  /**
   * Loads the moderation review page for a single reported reply: the flagged content, the list of
   * reporters and the resolution actions (dismiss reports, remove content, ban author).
   *
   * @param replyId - the unique identifier of the reported reply
   * @param request - the HttpServletRequest object
   * @param response - the HttpServletResponse object
   * @return a ModelAndView object containing the reported reply and its reports
   */
  @AdministrationSection("moderation")
  @RequestMapping(value = "/moderation/{replyId}", method = RequestMethod.GET)
  public ModelAndView moderationReviewView(
      @PathVariable(value = "replyId") long replyId,
      HttpServletRequest request,
      HttpServletResponse response) {
    DiscussionReply reply = discussionService.getDiscussionReplyUsingReplyId(replyId);
    if (reply == null) {
      throw new ResourceNotFoundException();
    }
    DiscussionThread thread =
        discussionService.getDiscussionThreadUsingThreadId(reply.getDiscussionThreadId());
    List<Map<String, Object>> reporters = discussionService.getReportersOfReply(replyId);
    ModelAndView view = new ModelAndView("pages/administration/moderation-review");
    view.addObject("reply", reply);
    view.addObject("thread", thread);
    view.addObject("reporters", reporters);
    view.addObject("reportCount", reporters.size());
    return view;
  }

  /**
   * Dismisses every report on a discussion reply, clearing it from the moderation queue while
   * keeping the reply itself intact.
   *
   * @param replyId - the unique identifier of the reply whose reports are dismissed
   * @param request - the HttpServletRequest object
   * @return the dismissal result
   */
  @RequestMapping(value = "/dismissReports.action", method = RequestMethod.POST)
  public @ResponseBody Map<String, Boolean> dismissReportsAction(
      @RequestParam(value = "replyId") long replyId, HttpServletRequest request) {
    return discussionService.dismissReportsOfReply(replyId);
  }

  /**
   * Loads the discussion topics page.
   *
   * @param request - the HttpServletRequest object
   * @param response - the HttpServletResponse object
   * @return a ModelAndView object containing the discussion topics
   */
  @RequestMapping(value = "/discussion-topics", method = RequestMethod.GET)
  public ModelAndView discussionTopicsView(
      HttpServletRequest request, HttpServletResponse response) {
    ModelAndView view = new ModelAndView("pages/administration/discussion-topics");
    view.addObject("discussionTopics", discussionService.getDiscussionTopics());
    return view;
  }

  /**
   * Creates a top-level discussion topic.
   *
   * @param discussionTopicSlug - the slug of the topic
   * @param discussionTopicName - the name of the topic
   * @param request - the HttpServletRequest object
   * @return the creation result
   */
  @RequestMapping(value = "/createTopic.action", method = RequestMethod.POST)
  public @ResponseBody Map<String, Boolean> createTopicAction(
      @RequestParam(value = "discussionTopicSlug") String discussionTopicSlug,
      @RequestParam(value = "discussionTopicName") String discussionTopicName,
      HttpServletRequest request) {
    return discussionService.createDiscussionTopic(
        discussionTopicSlug, discussionTopicName, null);
  }

  /**
   * Deletes a discussion topic.
   *
   * @param discussionTopicId - the unique identifier of the topic
   * @param request - the HttpServletRequest object
   * @return the deletion result
   */
  @RequestMapping(value = "/deleteTopic.action", method = RequestMethod.POST)
  public @ResponseBody Map<String, Boolean> deleteTopicAction(
      @RequestParam(value = "discussionTopicId") int discussionTopicId, HttpServletRequest request) {
    return discussionService.deleteDiscussionTopic(discussionTopicId);
  }

  /** The autowired DiscussionService object. Used for the moderation queue and discussion admin. */
  @Autowired private DiscussionService discussionService;

  /** The autowired OptionService object. Used for getting the settings options of the system. */
  @Autowired private OptionService optionService;

  /** The maximum number of reported replies listed on the dedicated moderation page. */
  private static final int MODERATION_QUEUE_LIMIT = 200;
}
