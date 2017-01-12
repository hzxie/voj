package org.verwandlung.voj.web.controller;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import org.verwandlung.voj.web.exception.ResourceNotFoundException;
import org.verwandlung.voj.web.model.DiscussionThread;
import org.verwandlung.voj.web.model.DiscussionTopic;
import org.verwandlung.voj.web.service.DiscussionService;

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
	 * @param discussionTopicSlug - 讨论话题的唯一标识符
	 * @param request - HttpRequest对象
	 * @param response - HttpResponse对象
	 * @return 包含讨论列表页面内容的ModelAndView对象
	 */
	@RequestMapping(value = "", method = RequestMethod.GET)
	public ModelAndView discussionThreadsView(
			@RequestParam(value = "topicSlug", required = false, defaultValue = "") String discussionTopicSlug,
			HttpServletRequest request, HttpServletResponse response) {
		List<DiscussionThread> discussionThreads = discussionService.getDiscussionThreadsOfTopic(discussionTopicSlug, 0, NUMBER_OF_THREADS_PER_PAGE);

		ModelAndView view = new ModelAndView("discussion/threads");
		view.addObject("selectedTopicSlug", discussionTopicSlug);
		view.addObject("discussionThreads", discussionThreads);
		view.addObject("discussionTopics", discussionService.getDiscussionTopicsWithHierarchy());
		return view;
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
	private static final int NUMBER_OF_THREADS_PER_PAGE = 25;

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
