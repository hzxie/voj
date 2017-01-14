package org.verwandlung.voj.web.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import jdk.nashorn.api.scripting.JSObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.verwandlung.voj.web.mapper.DiscussionReplyMapper;
import org.verwandlung.voj.web.mapper.DiscussionThreadMapper;
import org.verwandlung.voj.web.mapper.DiscussionTopicMapper;
import org.verwandlung.voj.web.model.DiscussionReply;
import org.verwandlung.voj.web.model.DiscussionThread;
import org.verwandlung.voj.web.model.DiscussionTopic;
import org.verwandlung.voj.web.util.HtmlTextFilter;
import org.verwandlung.voj.web.util.OffensiveWordFilter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 讨论(Discussion)的业务逻辑层.
 * @author Haozhe Xie
 */
@Service
@Transactional
public class DiscussionService {
	/**
	 * 获取全部的讨论话题.
	 * @return 包含全部讨论话题的List对象.
	 */
	public List<DiscussionTopic> getDiscussionTopics() {
		return discussionTopicMapper.getDiscussionTopics();
	}

	/**
	 * 获得具有层次关系的讨论话题列表.
	 * @return 包含讨论话题及其继承关系的Map对象
	 */
	public Map<DiscussionTopic, List<DiscussionTopic>> getDiscussionTopicsWithHierarchy() {
		List<DiscussionTopic> DiscussionTopics = getDiscussionTopics();
		Map<Integer, List<DiscussionTopic>> DiscussionTopicsIndexer = new HashMap<Integer, List<DiscussionTopic>>();
		Map<DiscussionTopic, List<DiscussionTopic>> DiscussionTopicsHierarchy = new HashMap<DiscussionTopic, List<DiscussionTopic>>();

		// 将无父亲的讨论话题加入列表
		for ( DiscussionTopic dt : DiscussionTopics ) {
			if ( dt.getParentDiscussionTopicId() == 0 ) {
				List<DiscussionTopic> subDiscussionTopics = new ArrayList<DiscussionTopic>();
				DiscussionTopicsHierarchy.put(dt, subDiscussionTopics);
				DiscussionTopicsIndexer.put(dt.getDiscussionTopicId(), subDiscussionTopics);
			}
		}
		// 将其他讨论话题加入列表
		for ( DiscussionTopic dt : DiscussionTopics ) {
			int parentDiscussionTopicId = dt.getParentDiscussionTopicId() ;
			if ( parentDiscussionTopicId != 0 ) {
				List<DiscussionTopic> subDiscussionTopics = DiscussionTopicsIndexer.get(parentDiscussionTopicId);
				if ( subDiscussionTopics != null ) {
					subDiscussionTopics.add(dt);
				}
			}
		}
		return DiscussionTopicsHierarchy;
	}

	/**
	 * 获取某个试题的题解讨论.
	 * @param problemId - 试题的唯一标识符
	 * @return 对应试题的题解讨论DiscussionThread对象
	 */
	public DiscussionThread getSolutionThreadOfProblem(long problemId) {
		return discussionThreadMapper.getSolutionThreadOfProblem(problemId);
	}

	/**
	 * 获取某个主题下的全部讨论(DiscussionThread).
	 * @param problemId - 试题的唯一标识符
	 * @param offset - 起始讨论的游标
	 * @param limit - 获取讨论的数量
	 * @return 包含DiscussionThread对象的List对象
	 */
	public List<DiscussionThread> getDiscussionThreadsOfProblem(long problemId, long offset, int limit) {
		return discussionThreadMapper.getDiscussionThreads(problemId, 0, offset, limit);
	}

	/**
	 * 获取某个讨论主题中的全部讨论(DiscussionThread).
	 * @param discussionTopicSlug - 讨论主题的唯一英文缩写
	 * @param offset - 起始讨论的游标
	 * @param limit - 获取讨论的数量
	 * @return 包含DiscussionThread对象的List对象
	 */
	public List<DiscussionThread> getDiscussionThreadsOfTopic(String discussionTopicSlug, long offset, int limit) {
		int discussionTopicId = 0;
		if ( discussionTopicSlug != null && !discussionTopicSlug.isEmpty() ) {
			DiscussionTopic dt = discussionTopicMapper.getDiscussionTopicUsingSlug(discussionTopicSlug);
			discussionTopicId = dt.getDiscussionTopicId();
		}
		return discussionThreadMapper.getDiscussionThreads(0, discussionTopicId, offset, limit);
	}

	/**
	 * 获取某个讨论话题的回复.
	 * @param discussionThreadId - 讨论话题的唯一标识符
	 * @param currentUserUid - 当前登录用户的用户唯一标识符(-1表示未登录)
	 * @param offset - 起始回复的游标
	 * @param limit - 获取回复的数量
	 * @return 包含讨论话题回复的List对象
	 */
	public List<DiscussionReply> getDiscussionRepliesOfThread(long discussionThreadId, long currentUserUid, long offset, int limit) {
		List<DiscussionReply> replies = discussionReplyMapper.getDiscussionRepliesUsingThreadId(discussionThreadId, offset, limit);
		for ( DiscussionReply dr : replies ) {
			// 过滤回复中的敏感内容
			String replyContent = dr.getDiscussionReplyContent();
			replyContent = offensiveWordFilter.filter(HtmlTextFilter.filter(replyContent));
			dr.setDiscussionReplyContent(replyContent);
			// 获取回复中的投票信息
			Map<String, Object> votesStatistics = getVoteStatisticsOfDiscussionReply(
					dr.getDiscussionReplyVotes(), currentUserUid);
			dr.setDiscussionReplyVotes(JSON.toJSONString(votesStatistics));
		}
		return replies;
	}

	/**
	 * 统计讨论回复中投票信息.
	 * @param votes - 原始讨论回复中投票信息的JSON格式字符串
	 * @param currentUserUid - 当前登录用户的用户唯一标识符(-1表示未登录)
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
	 * 通过讨论帖子的唯一标识符获取讨论帖子对象.
	 * @param discussionThreadId - 讨论帖子的唯一标识符
	 * @return 对应的讨论帖子对象或空引用
	 */
	public DiscussionThread getDiscussionThreadUsingThreadId(long discussionThreadId) {
		return discussionThreadMapper.getDiscussionThreadUsingThreadId(discussionThreadId);
	}

	/**
	 * 通过讨论回复的唯一标识符获取讨论帖子对象.
	 * @param discussionReplyId - 讨论回复的唯一标识符
	 * @return 对应的讨论回复对象或空引用
	 */
	public DiscussionReply getDiscussionReplyUsingReplyId(long discussionReplyId) {
		return discussionReplyMapper.getDiscussionReplyUsingReplyId(discussionReplyId);
	}

	/**
	 * 对讨论回复进行投票.
	 * @param discussionThreadId - 讨论帖子的唯一标识符
	 * @param discussionReplyId - 讨论回复的唯一标识符
	 * @param currentUserUid - 当前登录用户的用户唯一标识符(-1表示未登录)
	 * @param voteUp - Vote Up状态 (+1 表示用户赞了这个回答, -1 表示用户取消赞了这个回答, 0表示没有操作)
	 * @param voteDown - Vote Up状态 (+1 表示用户踩了这个回答, -1 表示用户取消踩了这个回答, 0表示没有操作)
	 * @param isCsrfTokenValid - CSRF Token是否有效
	 * @return 讨论回复的投票结果
	 */
	public Map<String, Boolean> voteDiscussionReply(long discussionThreadId, long discussionReplyId,
	    long currentUserUid, int voteUp, int voteDown, boolean isCsrfTokenValid) {
		DiscussionReply discussionReply = discussionReplyMapper.getDiscussionReplyUsingReplyId(discussionReplyId);
		Map<String, Boolean> result = new HashMap<String, Boolean>();
		result.put("isDiscussionReplyExists", discussionReply != null && discussionReply.getDiscussionThreadId() == discussionThreadId);
		result.put("isVoteValid", voteUp >= -1 && voteUp <= 1 && voteDown >=-1 && voteDown <= 1);
		result.put("isCsrfTokenValid", isCsrfTokenValid);
		result.put("isLoggedIn", currentUserUid != -1);

		boolean isSuccessful = result.get("isDiscussionReplyExists") && result.get("isVoteValid") &&
				               result.get("isCsrfTokenValid")        && result.get("isLoggedIn");
		result.put("isSuccessful", isSuccessful);

		if ( result.get("isSuccessful") ) {
			synchronized ( this ) {
				// 设置新的投票结果
				JSONObject voteUsers = JSON.parseObject(discussionReply.getDiscussionReplyVotes());
				JSONArray voteUpUsers = voteUsers.getJSONArray("up");
				JSONArray voteDownUsers = voteUsers.getJSONArray("down");
				boolean isVotedUp = contains(voteUpUsers, currentUserUid);
				boolean isVotedDown = contains(voteDownUsers, currentUserUid);

				if ( voteUp == 1 && !isVotedUp ) {
					if ( isVotedDown ) remove(voteDownUsers, currentUserUid);
					voteUpUsers.add(currentUserUid);
				} else if ( voteUp == -1 ) {
					remove(voteUpUsers, currentUserUid);
				}
				if ( voteDown == 1 && !isVotedDown ) {
					if ( isVotedUp ) remove(voteUpUsers, currentUserUid);
					voteDownUsers.add(currentUserUid);
				} else if ( voteDown == -1 ) {
					remove(voteDownUsers, currentUserUid);
				}
				discussionReply.setDiscussionReplyVotes(JSON.toJSONString(voteUsers));
				discussionReplyMapper.updateDiscussionReply(discussionReply);
			}
		}
		return result;
	}

	/**
	 * 移除JSONArray对象中的一个值.
	 * 为了修复JSONArray自带contains方法的Bug.
	 * 使用场景: 从Vote列表中移除某个用户的UID.
	 * @param jsonArray - 待移除值的JSONArray对象
	 * @param value - 待移除的值
	 */
	private void remove(JSONArray jsonArray, long value) {
		for ( int i = 0; i < jsonArray.size(); ++ i ) {
			if ( jsonArray.getLong(i) == value ) {
				jsonArray.remove(i);
			}
		}
	}

	/**
	 * 自动注入的DiscussionTopicMapper对象.
	 * 用于获取讨论主题.
	 */
	@Autowired
	private DiscussionTopicMapper discussionTopicMapper;

	/**
	 * 自动注入的DiscussionThreadMapper对象.
	 * 用于获取讨论帖子.
	 */
	@Autowired
	private DiscussionThreadMapper discussionThreadMapper;

	/**
	 * 自动注入的DiscussionReplyMapper对象.
	 * 用于获取讨论回复.
	 */
	@Autowired
	private DiscussionReplyMapper discussionReplyMapper;

	/**
	 * 自动注入的SensitiveWordFilter对象.
	 * 用于过滤用户个人信息中的敏感词.
	 */
	@Autowired
	private OffensiveWordFilter offensiveWordFilter;
}
