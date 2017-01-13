package org.verwandlung.voj.web.controller;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;
import org.verwandlung.voj.web.exception.ResourceNotFoundException;
import org.verwandlung.voj.web.model.DiscussionThread;
import org.verwandlung.voj.web.model.DiscussionTopic;
import org.verwandlung.voj.web.service.DiscussionService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 处理讨论的相关请求.
 * 
 * @author Haozhe Xie
 */
@Controller
@RequestMapping(value = "/discussion")
public class DiscussionController {
	/**
	 * 显示讨论列表页面.
	 * @param discussionTopicSlug - 讨论话题的唯一英文缩写
	 * @param request - HttpRequest对象
	 * @param response - HttpResponse对象
	 * @return 包含讨论列表页面内容的ModelAndView对象
	 */
	@RequestMapping(value = "", method = RequestMethod.GET)
	public ModelAndView discussionThreadsView(
			@RequestParam(value = "topicSlug", required = false, defaultValue = "") String discussionTopicSlug,
			HttpServletRequest request, HttpServletResponse response) {
		List<DiscussionThread> discussionThreads = discussionService.
				getDiscussionThreadsOfTopic(discussionTopicSlug, 0, NUMBER_OF_THREADS_PER_REQUEST);

		ModelAndView view = new ModelAndView("discussion/threads");
		view.addObject("selectedTopicSlug", discussionTopicSlug);
		view.addObject("discussionThreads", discussionThreads);
		view.addObject("discussionTopics", discussionService.getDiscussionTopicsWithHierarchy());
		return view;
	}

	/**
	 * 获取讨论帖子列表.
	 * @param startIndex - 获取讨论帖子的Offset
	 * @param discussionTopicSlug - 讨论话题的唯一英文缩写
	 * @return 一个包含讨论帖子列表的HashMap对象
	 */
	@RequestMapping(value = "/getDiscussionThreads.action", method = RequestMethod.GET)
	public @ResponseBody Map<String, Object> getProblemsAction(
			@RequestParam(value = "startIndex", required = true) long startIndex,
			@RequestParam(value = "topicSlug", required = false) String discussionTopicSlug,
			HttpServletRequest request) {
		if ( startIndex < 0 ) {
			startIndex = 0;
		}
		List<DiscussionThread> discussionThreads = discussionService.getDiscussionThreadsOfTopic(
				discussionTopicSlug, startIndex, NUMBER_OF_THREADS_PER_REQUEST);

		Map<String, Object> result = new HashMap<String, Object>(3, 1);
		result.put("isSuccessful", discussionThreads != null && !discussionThreads.isEmpty());
		result.put("discussionThreads", discussionThreads);
		return result;
	}

	/**
	 * 显示讨论详情页面.
	 * @param threadId - 讨论帖子的唯一标识符
	 * @param request - HttpRequest对象
	 * @param response - HttpResponse对象
	 * @return 包含讨论详情页面内容的ModelAndView对象
	 */
	@RequestMapping(value = "/{threadId}", method = RequestMethod.GET)
	public ModelAndView discussionThreadView(
			@PathVariable("threadId") long threadId,
			HttpServletRequest request, HttpServletResponse response) {
		DiscussionThread discussionThread = discussionService.getDiscussionThreadUsingThreadId(threadId);
		if ( discussionThread == null ) {
			throw new ResourceNotFoundException();
		}

		ModelAndView view = new ModelAndView("discussion/thread");
		view.addObject("discussionThread", discussionThread);
		return view;
	}

	/**
	 * 每一页显示的讨论帖子数量.
	 */
	private static final int NUMBER_OF_THREADS_PER_REQUEST = 25;

	/**
	 * 每一次请求的讨论回复数量.
	 */
	private static final int NUMBER_OF_REPLIES_PER_REQUEST = 50;

	/**
	 * 自动注入的DiscussionService对象.
	 */
	@Autowired
	private DiscussionService discussionService;
}
