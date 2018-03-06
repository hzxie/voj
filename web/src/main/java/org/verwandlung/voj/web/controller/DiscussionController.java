/* Verwandlung Online Judge - A cross-platform judge online system
 * Copyright (C) 2018 Haozhe Xie <cshzxie@gmail.com>
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
 *
 *
 *                              _ooOoo_  
 *                             o8888888o  
 *                             88" . "88  
 *                             (| -_- |)  
 *                             O\  =  /O  
 *                          ____/`---'\____  
 *                        .'  \\|     |//  `.  
 *                       /  \\|||  :  |||//  \  
 *                      /  _||||| -:- |||||-  \  
 *                      |   | \\\  -  /// |   |  
 *                      | \_|  ''\---/''  |   |  
 *                      \  .-\__  `-`  ___/-. /  
 *                    ___`. .'  /--.--\  `. . __  
 *                 ."" '<  `.___\_<|>_/___.'  >'"".  
 *                | | :  `- \`.;`\ _ /`;.`/ - ` : | |  
 *                \  \ `-.   \_ __\ /__ _/   .-` /  /  
 *           ======`-.____`-.___\_____/___.-`____.-'======  
 *                              `=---=' 
 *
 *                          HERE BE BUDDHA
 *
 */
package org.verwandlung.voj.web.controller;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

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
import org.verwandlung.voj.web.service.ProblemService;
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
@RequestMapping(value="/discussion")
public class DiscussionController {
	/**
	 * 显示讨论列表页面.
	 * @param discussionTopicSlug - 讨论话题的唯一英文缩写
	 * @param problemId - 试题的唯一标识符
	 * @param request - HttpRequest对象
	 * @param response - HttpResponse对象
	 * @return 包含讨论列表页面内容的ModelAndView对象
	 */
	@RequestMapping(value="", method=RequestMethod.GET)
	public ModelAndView discussionThreadsView(
			@RequestParam(value="topicSlug", required=false, defaultValue="") String discussionTopicSlug,
			@RequestParam(value="problemId", required=false, defaultValue="-1") long problemId,
			HttpServletRequest request, HttpServletResponse response) {
		List<DiscussionThread> discussionThreads = null;
		if ( problemId != -1 ) {
			discussionThreads = discussionService.getDiscussionThreadsOfProblem(
					problemId, 0, NUMBER_OF_THREADS_PER_REQUEST);
		} else {
			discussionThreads = discussionService.getDiscussionThreadsOfTopic(
					discussionTopicSlug, 0, NUMBER_OF_THREADS_PER_REQUEST);
		}

		ModelAndView view = new ModelAndView("discussion/threads");
		view.addObject("selectedTopicSlug", discussionTopicSlug);
		view.addObject("problemId", problemId);
		view.addObject("discussionThreads", discussionThreads);
		view.addObject("discussionTopics", discussionService.getDiscussionTopicsWithHierarchy());
		return view;
	}

	/**
	 * 获取讨论帖子列表.
	 * @param startIndex - 获取讨论帖子的Offset
	 * @param discussionTopicSlug - 讨论话题的唯一英文缩写
	 * @param problemId - 试题的唯一标识符
	 * @return 一个包含讨论帖子列表的HashMap对象
	 */
	@RequestMapping(value="/getDiscussionThreads.action", method=RequestMethod.GET)
	public @ResponseBody Map<String, Object> getDiscussionThreadsAction(
			@RequestParam(value="startIndex") long startIndex,
			@RequestParam(value="topicSlug", required=false, defaultValue="") String discussionTopicSlug,
			@RequestParam(value="problemId", required=false, defaultValue="-1") long problemId,
			HttpServletRequest request) {
		if ( startIndex < 0 ) {
			startIndex = 0;
		}
		List<DiscussionThread> discussionThreads = null;
		if ( problemId != -1 ) {
			discussionThreads = discussionService.getDiscussionThreadsOfProblem(
					problemId, startIndex, NUMBER_OF_THREADS_PER_REQUEST);
		} else {
			discussionThreads = discussionService.getDiscussionThreadsOfTopic(
					discussionTopicSlug, startIndex, NUMBER_OF_THREADS_PER_REQUEST);
		}

		Map<String, Object> result = new HashMap<>(3, 1);
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
	@RequestMapping(value="/{threadId}", method=RequestMethod.GET)
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
			List<DiscussionTopic> discussionTopics = discussionService.getDiscussionTopics();
			view.addObject("discussionTopics", discussionTopics);
			view.addObject("csrfToken", CsrfProtector.getCsrfToken(session));
		}
		return view;
	}

	/**
	 * 显示创建讨论页面.
	 * @param request - HttpRequest对象
	 * @param response - HttpResponse对象
	 * @return 包含创建讨论页面内容的ModelAndView对象
	 */
	@RequestMapping(value="/new", method=RequestMethod.GET)
	public ModelAndView newDiscussionThreadView(
			@RequestParam(value="problemId", required=false, defaultValue="-1") long problemId,
			HttpServletRequest request, HttpServletResponse response) {
		HttpSession session = request.getSession();
		ModelAndView view = null;
		if ( !isLoggedIn(session) ) {
			RedirectView redirectView = new RedirectView(request.getContextPath() + "/accounts/login");
			redirectView.setExposeModelAttributes(false);
			view = new ModelAndView(redirectView);
		} else {
			List<DiscussionTopic> discussionTopics = discussionService.getDiscussionTopics();
			Problem problem = null;
			if ( problemId != -1 ) {
				problem = problemService.getProblem(problemId);
			}

			view = new ModelAndView("discussion/new-thread");
			view.addObject("discussionTopics", discussionTopics);
			view.addObject("relatedProblem", problem);
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
	@RequestMapping(value="/{threadId}/getDiscussionReplies.action", method=RequestMethod.GET)
	public @ResponseBody Map<String, Object> getDiscussionRepliesAction(
			@PathVariable("threadId") long discussionThreadId,
			@RequestParam(value="startIndex") long startIndex,
			HttpServletRequest request) {
		long currentUserUid = getUidOfUserLoggedIn(request.getSession());
		if ( startIndex < 0 ) {
			startIndex = 0;
		}

		List<DiscussionReply> discussionReplies = discussionService.getDiscussionRepliesOfThread(
				discussionThreadId, currentUserUid, startIndex, NUMBER_OF_REPLIES_PER_REQUEST);
		Map<String, Object> result = new HashMap<>(3, 1);
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
	 * @return 包含讨论回复投票请求处理结果的JSON对象
	 */
	@RequestMapping(value="/{threadId}/voteDiscussionReply.action", method=RequestMethod.POST)
	public @ResponseBody Map<String, Boolean> voteDiscussionReplyAction(
			@PathVariable("threadId") long discussionThreadId,
			@RequestParam(value="discussionReplyId") long discussionReplyId,
			@RequestParam(value="voteUp") int voteUp,
			@RequestParam(value="voteDown") int voteDown,
			@RequestParam(value="csrfToken") String csrfToken,
			HttpServletRequest request) {
		HttpSession session = request.getSession();
		String ipAddress = HttpRequestParser.getRemoteAddr(request);
		User currentUser = HttpSessionParser.getCurrentUser(session);
		boolean isCsrfTokenValid = CsrfProtector.isCsrfTokenValid(csrfToken, session);

		Map<String, Boolean> result = discussionService.voteDiscussionReply(discussionThreadId,
				discussionReplyId, currentUser, voteUp, voteDown, isCsrfTokenValid);
		if ( result.get("isSuccessful") ) {
			LOGGER.info(String.format("User: {%s} voted discussion reply #%d {Up: %d, Down: %d} at %s",
					new Object[] {currentUser, discussionReplyId, voteUp, voteDown, ipAddress}));
		}
		return result;
	}

	/**
	 * 处理用户创建讨论帖子的请求.
	 * @param discussionTopicSlug - 讨论帖子对应主题的唯一英文缩写
	 * @param relatedProblemIdString - 讨论帖子所关联问题的唯一标识符
	 * @param discussionThreadTitle - 讨论帖子的标题
	 * @param csrfToken 用于防止CSRF攻击的Token
	 * @param request - HttpServletRequest对象
	 * @return 包含讨论帖子创建结果的JSON对象
	 */
	@RequestMapping(value="/createDiscussionThread.action", method=RequestMethod.POST)
	public @ResponseBody Map<String, Object> createDiscussionThreadAction(
			@RequestParam(value="discussionTopicSlug") String discussionTopicSlug,
			@RequestParam(value="relatedProblemId") String relatedProblemIdString,
			@RequestParam(value="threadTitle") String discussionThreadTitle,
			@RequestParam(value="threadContent") String discussionThreadContent,
			@RequestParam(value="csrfToken") String csrfToken,
			HttpServletRequest request) {
		HttpSession session = request.getSession();
		String ipAddress = HttpRequestParser.getRemoteAddr(request);
		User currentUser = HttpSessionParser.getCurrentUser(session);
		long relatedProblemId = relatedProblemIdString.isEmpty() ? -1 : Integer.parseInt(relatedProblemIdString);
		boolean isCsrfTokenValid = CsrfProtector.isCsrfTokenValid(csrfToken, session);

		Map<String, Object> result = discussionService.createDiscussionThread(
				currentUser, discussionTopicSlug, relatedProblemId, discussionThreadTitle,
				discussionThreadContent, isCsrfTokenValid);
		if ( (Boolean) result.get("isSuccessful") ) {
			LOGGER.info(String.format("User: {%s} created discussion thread[Title=%s] at %s",
					new Object[] {currentUser, discussionThreadTitle, ipAddress}));
		}
		return result;
	}

	/**
	 * 处理用户编辑讨论帖子的请求.
	 * @param discussionThreadId - 讨论帖子的唯一标识符
	 * @param discussionTopicSlug - 讨论帖子对应主题的唯一英文缩写
	 * @param discussionThreadTitle - 讨论帖子的标题
	 * @param csrfToken 用于防止CSRF攻击的Token
	 * @param request - HttpServletRequest对象
	 * @return 包含讨论帖子编辑结果的JSON对象
	 */
	@RequestMapping(value="/editDiscussionThread.action", method=RequestMethod.POST)
	public @ResponseBody Map<String, Boolean> editDiscussionThreadAction(
			@RequestParam(value="discussionThreadId") long discussionThreadId,
			@RequestParam(value="discussionTopicSlug") String discussionTopicSlug,
			@RequestParam(value="discussionThreadTitle") String discussionThreadTitle,
			@RequestParam(value="csrfToken") String csrfToken,
			HttpServletRequest request) {
		HttpSession session = request.getSession();
		String ipAddress = HttpRequestParser.getRemoteAddr(request);
		User currentUser = HttpSessionParser.getCurrentUser(session);
		boolean isCsrfTokenValid = CsrfProtector.isCsrfTokenValid(csrfToken, session);

		Map<String, Boolean> result = discussionService.editDiscussionThread(
				discussionThreadId, currentUser, discussionTopicSlug, discussionThreadTitle, isCsrfTokenValid);
		if ( result.get("isSuccessful") ) {
			LOGGER.info(String.format("User: {%s} voted discussion thread #%d at %s",
					new Object[] {currentUser, discussionThreadId, ipAddress}));
		}
		return result;
	}

	/**
	 * 处理用户创建讨论回复的请求.
	 * @param discussionThreadId - 讨论帖子的唯一标识符
	 * @param replyContent - 讨论回复的内容
	 * @param csrfToken - 用于防止CSRF攻击的Token
	 * @param request - HttpServletRequest对象
	 * @return 包含讨论回复创建结果的JSON对象
	 */
	@RequestMapping(value="/{threadId}/createDiscussionReply.action", method=RequestMethod.POST)
	public @ResponseBody Map<String, Object> createDiscussionReplyAction(
			@PathVariable("threadId") long discussionThreadId,
			@RequestParam(value="replyContent") String replyContent,
			@RequestParam(value="csrfToken") String csrfToken,
			HttpServletRequest request) {
		HttpSession session = request.getSession();
		String ipAddress = HttpRequestParser.getRemoteAddr(request);
		User currentUser = HttpSessionParser.getCurrentUser(session);
		boolean isCsrfTokenValid = CsrfProtector.isCsrfTokenValid(csrfToken, session);

		Map<String, Object> result = discussionService.createDiscussionReply(
				discussionThreadId, currentUser, replyContent, isCsrfTokenValid);
		if ( (Boolean) result.get("isSuccessful") ) {
			LOGGER.info(String.format("User: {%s} created discussion reply for thread #%d at %s",
					new Object[] {currentUser, discussionThreadId, ipAddress}));
		}
		return result;
	}

	/**
	 * 处理用户编辑讨论回复的请求.
	 * @param discussionReplyId - 讨论回复的唯一标识符
	 * @param replyContent - 讨论回复的内容
	 * @param csrfToken - 用于防止CSRF攻击的Token
	 * @param request - HttpServletRequest对象
	 * @return 包含讨论回复编辑结果的JSON对象
	 */
	@RequestMapping(value="/{threadId}/editDiscussionReply.action", method=RequestMethod.POST)
	public @ResponseBody Map<String, Boolean> editDiscussionReplyAction(
			@PathVariable("threadId") long discussionThreadId,
			@RequestParam(value="discussionReplyId") long discussionReplyId,
			@RequestParam(value="replyContent") String replyContent,
			@RequestParam(value="csrfToken") String csrfToken,
			HttpServletRequest request) {
		HttpSession session = request.getSession();
		String ipAddress = HttpRequestParser.getRemoteAddr(request);
		User currentUser = HttpSessionParser.getCurrentUser(session);
		boolean isCsrfTokenValid = CsrfProtector.isCsrfTokenValid(csrfToken, session);

		Map<String, Boolean> result = discussionService.editDiscussionReply(
				discussionReplyId, currentUser, replyContent, isCsrfTokenValid);
		if ( result.get("isSuccessful") ) {
			LOGGER.info(String.format("User: {%s} edited discussion reply #%d at %s",
					new Object[] {currentUser, discussionReplyId, ipAddress}));
		}
		return result;
	}

	/**
	 * 处理用户删除讨论回复的请求.
	 * @param discussionReplyId - 讨论回复的唯一标识符
	 * @param csrfToken - 用于防止CSRF攻击的Token
	 * @param request - HttpServletRequest对象
	 * @return 包含讨论回复删除结果的JSON对象
	 */
	@RequestMapping(value="/{threadId}/deleteDiscussionReply.action", method=RequestMethod.POST)
	public @ResponseBody Map<String, Boolean> deleteDiscussionReplyAction(
			@PathVariable("threadId") long discussionThreadId,
			@RequestParam(value="discussionReplyId") long discussionReplyId,
			@RequestParam(value="csrfToken") String csrfToken,
			HttpServletRequest request) {
		HttpSession session = request.getSession();
		String ipAddress = HttpRequestParser.getRemoteAddr(request);
		User currentUser = HttpSessionParser.getCurrentUser(session);
		boolean isCsrfTokenValid = CsrfProtector.isCsrfTokenValid(csrfToken, session);

		Map<String, Boolean> result = discussionService.deleteDiscussionReply(
				discussionReplyId, currentUser, isCsrfTokenValid);
		if ( result.get("isSuccessful") ) {
			LOGGER.info(String.format("User: {%s} deleted discussion reply #%d at %s",
					new Object[] {currentUser, discussionReplyId, ipAddress}));
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
	 * 自动注入的ProblemService对象.
	 */
	@Autowired
	private ProblemService problemService;

	/**
	 * 日志记录器.
	 */
	private static final Logger LOGGER = LogManager.getLogger(DiscussionController.class);
}
