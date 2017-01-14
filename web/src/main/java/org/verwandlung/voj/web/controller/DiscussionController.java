package org.verwandlung.voj.web.controller;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;
import org.verwandlung.voj.web.exception.ResourceNotFoundException;
import org.verwandlung.voj.web.model.DiscussionReply;
import org.verwandlung.voj.web.model.DiscussionThread;
import org.verwandlung.voj.web.model.User;
import org.verwandlung.voj.web.service.DiscussionService;
import org.verwandlung.voj.web.util.CsrfProtector;
import org.verwandlung.voj.web.util.HttpRequestParser;
import org.verwandlung.voj.web.util.HttpSessionParser;

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
	public @ResponseBody Map<String, Object> getDiscussionThreadsAction(
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
	 * @param discussionThreadId - 讨论帖子的唯一标识符
	 * @param request - HttpRequest对象
	 * @param response - HttpResponse对象
	 * @return 包含讨论详情页面内容的ModelAndView对象
	 */
	@RequestMapping(value = "/{threadId}", method = RequestMethod.GET)
	public ModelAndView discussionThreadView(
			@PathVariable("threadId") long discussionThreadId,
			HttpServletRequest request, HttpServletResponse response) {
		DiscussionThread discussionThread = discussionService.getDiscussionThreadUsingThreadId(discussionThreadId);
		if ( discussionThread == null ) {
			throw new ResourceNotFoundException();
		}

		HttpSession session = request.getSession();
		ModelAndView view = new ModelAndView("discussion/thread");
		view.addObject("discussionThread", discussionThread);
		if ( isLoggedIn(session) ) {
			view.addObject("csrfToken", CsrfProtector.getCsrfToken(session));
		}
		return view;
	}

	/**
	 * 检查用户是否已经登录.
	 * @param session - HttpSession 对象
	 * @return 用户是否已经登录
	 */
	private boolean isLoggedIn(HttpSession session) {
		Boolean isLoggedIn = (Boolean)session.getAttribute("isLoggedIn");
		if ( isLoggedIn == null || !isLoggedIn.booleanValue() ) {
			return false;
		}
		return true;
	}

	/**
	 * 获取讨论回复.
	 * @param discussionThreadId - 讨论帖子的唯一标识符
	 * @param startIndex - 讨论回复的起始Offset(已经获取的回复的数量).
	 * @param request - HttpServletRequest对象
	 * @return 包含讨论回复列表(DiscussionReply)的Map对象
	 */
	@RequestMapping(value = "/{threadId}/getDiscussionReplies.action", method = RequestMethod.GET)
	public @ResponseBody Map<String, Object> getDiscussionRepliesAction(
			@PathVariable("threadId") long discussionThreadId,
			@RequestParam(value = "startIndex", required = true) long startIndex,
			HttpServletRequest request) {
		long currentUserUid = getUidOfUserLoggedIn(request.getSession());
		if ( startIndex < 0 ) {
			startIndex = 0;
		}

		List<DiscussionReply> discussionReplies = discussionService.getDiscussionRepliesOfThread(
				discussionThreadId, currentUserUid, startIndex, NUMBER_OF_REPLIES_PER_REQUEST);
		Map<String, Object> result = new HashMap<String, Object>(3, 1);
		result.put("isSuccessful", discussionReplies != null && !discussionReplies.isEmpty());
		result.put("discussionReplies", discussionReplies);
		return result;
	}

	/**
	 * 获取当前登录用户的用户唯一标识符.
	 * @param session - HttpSession 对象
	 * @return 当前登录用户的用户唯一标识符
	 */
	private long getUidOfUserLoggedIn(HttpSession session) {
		Boolean isLoggedIn = (Boolean)session.getAttribute("isLoggedIn");
		Long userId = (Long)session.getAttribute("uid");
		if ( isLoggedIn == null || !isLoggedIn.booleanValue() || userId == null ) {
			return -1;
		}
		return userId;
	}

	/**
	 * 处理用户对讨论回复投票的请求.
	 * @param discussionThreadId - 讨论帖子的唯一标识符
	 * @param discussionReplyId - 讨论回复的唯一标识符
	 * @param voteUp - Vote Up状态 (+1 表示用户赞了这个回答, -1 表示用户取消赞了这个回答, 0表示没有操作)
	 * @param voteDown - Vote Up状态 (+1 表示用户踩了这个回答, -1 表示用户取消踩了这个回答, 0表示没有操作)
	 * @param csrfToken - 用于防止CSRF攻击的Token
	 * @param request - HttpServletRequest对象
	 * @return 讨论回复投票请求的处理结果
	 */
	@RequestMapping(value = "/{threadId}/voteDiscussionReply.action", method = RequestMethod.POST)
	public @ResponseBody Map<String, Boolean> voteDiscussionReplyAction(
			@PathVariable("threadId") long discussionThreadId,
			@RequestParam(value = "discussionReplyId", required = true) long discussionReplyId,
			@RequestParam(value = "voteUp", required = true) int voteUp,
			@RequestParam(value = "voteDown", required = true) int voteDown,
			@RequestParam(value = "csrfToken", required = true) String csrfToken,
			HttpServletRequest request) {
		HttpSession session = request.getSession();
		String ipAddress = HttpRequestParser.getRemoteAddr(request);
		User currentUser = HttpSessionParser.getCurrentUser(session);
		boolean isCsrfTokenValid = CsrfProtector.isCsrfTokenValid(csrfToken, session);

		Map<String, Boolean> result = discussionService.voteDiscussionReply(discussionThreadId,
				discussionReplyId, currentUser.getUid(), voteUp, voteDown, isCsrfTokenValid);
		boolean isSuccessful = result.get("isSuccessful");
		if ( isSuccessful ) {
			LOGGER.info(String.format("User: {%s} voted discussion reply #%d {Up: %d, Down: %d} at %s",
					new Object[] {currentUser, discussionReplyId, voteUp, voteDown, ipAddress}));
		}
		return result;
	}

	/**
	 * 每一次请求的讨论帖子数量.
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

	/**
	 * 日志记录器.
	 */
	private static final Logger LOGGER = LogManager.getLogger(DiscussionController.class);
}
