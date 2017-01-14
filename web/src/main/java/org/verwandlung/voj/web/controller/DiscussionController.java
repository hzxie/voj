package org.verwandlung.voj.web.controller;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;
import org.verwandlung.voj.web.exception.ResourceNotFoundException;
import org.verwandlung.voj.web.model.DiscussionReply;
import org.verwandlung.voj.web.model.DiscussionThread;
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
	 * 获取讨论回复.
	 * @param threadId - 讨论帖子的唯一标识符
	 * @param startIndex - 讨论回复的起始Offset(已经获取的回复的数量).
	 * @param request - HttpServletRequest对象
	 * @return 包含讨论回复列表(DiscussionReply)的Map对象
	 */
	@RequestMapping(value = "/{threadId}/getDiscussionReplies.action", method = RequestMethod.GET)
	public @ResponseBody Map<String, Object> getProblemsAction(
			@PathVariable("threadId") long threadId,
			@RequestParam(value = "startIndex", required = true) long startIndex,
			HttpServletRequest request) {
		long currentUserUid = getUidOfUserLoggedIn(request.getSession());
		if ( startIndex < 0 ) {
			startIndex = 0;
		}

		List<DiscussionReply> discussionReplies = discussionService.getDiscussionRepliesOfThread(
				threadId, startIndex, NUMBER_OF_REPLIES_PER_REQUEST);
		for ( DiscussionReply dr : discussionReplies ) {
			Map<String, Object> votesStatistics = getVoteStatisticsOfDiscussionReply(
					dr.getDiscussionReplyVotes(), currentUserUid);
			dr.setDiscussionReplyVotes(JSON.toJSONString(votesStatistics));
		}

		Map<String, Object> result = new HashMap<String, Object>(3, 1);
		result.put("isSuccessful", discussionReplies != null && !discussionReplies.isEmpty());
		result.put("discussionReplies", discussionReplies);
		return result;
	}

	/**
	 * 统计讨论回复中投票信息.
	 * @param votes - 原始讨论回复中投票信息的JSON格式字符串
	 * @param currentUserUid 当前登录用户的用户唯一标识符(-1表示未登录)
	 * @return 包含讨论回复投票信息的Map对象
	 */
	private Map<String, Object> getVoteStatisticsOfDiscussionReply(String votes, long currentUserUid) {
		Map<String, Object> votesStatistics = new HashMap<String, Object>(5, 1);
		JSONObject voteUsers = JSON.parseObject(votes);

		JSONArray voteUpUsers = voteUsers.getJSONArray("up");
		JSONArray voteDownUsers = voteUsers.getJSONArray("down");
		boolean isVotedUp = currentUserUid == -1 ? false : contains(voteUpUsers, currentUserUid);
		boolean isVotedDown = currentUserUid == -1 ? false : contains(voteDownUsers, currentUserUid);

		votesStatistics.put("isVotedUp", isVotedUp);
		votesStatistics.put("isVotedDown", isVotedDown);
		votesStatistics.put("numberOfVoteUp", voteUpUsers.size());
		votesStatistics.put("numberOfVoteDown", voteDownUsers.size());
		return votesStatistics;
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
	 * 判断一个值是否存在于一个JSONArray对象中.
	 * 为了修复JSONArray自带contains方法的Bug.
	 * 使用场景: 判断一个用户的UID是否存在于Vote列表中.
	 * @param jsonArray - 待判断的JSONArray对象
	 * @param value - 待检查的值
	 * @return 一个值是否存在于一个JSONArray对象中
	 */
	private boolean contains(JSONArray jsonArray, long value) {
		for ( int i = 0; i < jsonArray.size(); ++ i ) {
			if ( jsonArray.getLong(i) == value ) {
				return true;
			}
		}
		return false;
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
