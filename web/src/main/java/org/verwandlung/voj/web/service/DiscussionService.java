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
package org.verwandlung.voj.web.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.ResponseBody;
import org.verwandlung.voj.web.mapper.DiscussionReplyMapper;
import org.verwandlung.voj.web.mapper.DiscussionThreadMapper;
import org.verwandlung.voj.web.mapper.DiscussionTopicMapper;
import org.verwandlung.voj.web.mapper.ProblemMapper;
import org.verwandlung.voj.web.model.*;
import org.verwandlung.voj.web.util.HtmlTextFilter;
import org.verwandlung.voj.web.util.OffensiveWordFilter;

import java.util.*;

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
		Map<Integer, List<DiscussionTopic>> DiscussionTopicsIndexer = new HashMap<>();
		Map<DiscussionTopic, List<DiscussionTopic>> DiscussionTopicsHierarchy = new LinkedHashMap<>();

		// 将无父亲的讨论话题加入列表
		for ( DiscussionTopic dt : DiscussionTopics ) {
			if ( dt.getParentDiscussionTopicId() == 0 ) {
				List<DiscussionTopic> subDiscussionTopics = new ArrayList<>();
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
		Map<String, Object> votesStatistics = new HashMap<>(5, 1);
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
	 * @param currentUser - 当前登录用户的用户
	 * @param voteUp - Vote Up状态 (+1 表示用户赞了这个回答, -1 表示用户取消赞了这个回答, 0表示没有操作)
	 * @param voteDown - Vote Up状态 (+1 表示用户踩了这个回答, -1 表示用户取消踩了这个回答, 0表示没有操作)
	 * @param isCsrfTokenValid - CSRF Token是否有效
	 * @return 讨论回复的投票结果
	 */
	public Map<String, Boolean> voteDiscussionReply(long discussionThreadId, long discussionReplyId,
			User currentUser, int voteUp, int voteDown, boolean isCsrfTokenValid) {
		DiscussionReply discussionReply = discussionReplyMapper.getDiscussionReplyUsingReplyId(discussionReplyId);
		Map<String, Boolean> result = new HashMap<>();
		result.put("isDiscussionReplyExists", discussionReply != null && discussionReply.getDiscussionThreadId() == discussionThreadId);
		result.put("isVoteValid", voteUp >= -1 && voteUp <= 1 && voteDown >=-1 && voteDown <= 1);
		result.put("isCsrfTokenValid", isCsrfTokenValid);
		result.put("isLoggedIn", currentUser != null);

		boolean isSuccessful = result.get("isDiscussionReplyExists") && result.get("isVoteValid") &&
				               result.get("isCsrfTokenValid")        && result.get("isLoggedIn");
		result.put("isSuccessful", isSuccessful);

		if ( result.get("isSuccessful") ) {
			synchronized ( this ) {
				// 设置新的投票结果
				JSONObject voteUsers = JSON.parseObject(discussionReply.getDiscussionReplyVotes());
				JSONArray voteUpUsers = voteUsers.getJSONArray("up");
				JSONArray voteDownUsers = voteUsers.getJSONArray("down");
				boolean isVotedUp = contains(voteUpUsers, currentUser.getUid());
				boolean isVotedDown = contains(voteDownUsers, currentUser.getUid());

				if ( voteUp == 1 && !isVotedUp ) {
					if ( isVotedDown ) remove(voteDownUsers, currentUser.getUid());
					voteUpUsers.add(currentUser.getUid());
				} else if ( voteUp == -1 ) {
					remove(voteUpUsers, currentUser.getUid());
				}
				if ( voteDown == 1 && !isVotedDown ) {
					if ( isVotedUp ) remove(voteUpUsers, currentUser.getUid());
					voteDownUsers.add(currentUser.getUid());
				} else if ( voteDown == -1 ) {
					remove(voteDownUsers, currentUser.getUid());
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
	 * [仅限管理员使用]
	 * 创建讨论主题.
	 * @param discussionTopicSlug - 讨论主题的唯一英文缩写
	 * @param discussionTopicName - 讨论主题的名称
	 * @param parentDiscussionTopic - 父级讨论主题对象 (可为空)
	 * @return 包含讨论主题创建结果的Map对象
	 */
	public Map<String, Boolean> createDiscussionTopic(String discussionTopicSlug,
			String discussionTopicName, DiscussionTopic parentDiscussionTopic) {
		Map<String, Boolean> result = new HashMap<>(6, 1);
		result.put("isDiscussionTopicSlugEmpty", discussionTopicSlug.isEmpty());
		result.put("isDiscussionTopicSlugLegal", discussionTopicSlug.length() <= 128);
		result.put("isDiscussionTopicNameEmpty", discussionTopicName.isEmpty());
		result.put("isDiscussionTopicNameLegal", discussionTopicName.length() <= 128);

		boolean isSuccessful = !result.get("isDiscussionTopicSlugEmpty") && result.get("isDiscussionTopicSlugLegal") &&
							   !result.get("isDiscussionTopicNameEmpty") && result.get("isDiscussionTopicNameLegal");
		result.put("isSuccessful", isSuccessful);
		if ( isSuccessful ) {
			int parentDiscussionTopicId = parentDiscussionTopic == null  ? 0 : parentDiscussionTopic.getParentDiscussionTopicId();
			DiscussionTopic dt = new DiscussionTopic(discussionTopicSlug, discussionTopicName, parentDiscussionTopicId);
			discussionTopicMapper.createDiscussionTopic(dt);
		}
		return result;
	}

	/**
	 * [仅限管理员使用]
	 * 编辑讨论主题.
	 * @param discussionTopicId - 讨论主题的唯一标识符
	 * @param discussionTopicSlug - 讨论主题的唯一英文缩写
	 * @param discussionTopicName - 讨论主题的名称
	 * @param parentDiscussionTopic - 父级讨论主题对象 (可为空)
	 * @return 包含讨论主题编辑结果的Map对象
	 */
	public Map<String, Boolean> updateDiscussionTopic(int discussionTopicId,
			String discussionTopicSlug, String discussionTopicName,
			DiscussionTopic parentDiscussionTopic) {
		DiscussionTopic dt = discussionTopicMapper.getDiscussionTopicUsingId(discussionTopicId);
		Map<String, Boolean> result = new HashMap<>(7, 1);
		result.put("isDiscussionTopicExists", dt != null);
		result.put("isDiscussionTopicSlugEmpty", discussionTopicSlug.isEmpty());
		result.put("isDiscussionTopicSlugLegal", discussionTopicSlug.length() <= 128);
		result.put("isDiscussionTopicNameEmpty", discussionTopicName.isEmpty());
		result.put("isDiscussionTopicNameLegal", discussionTopicName.length() <= 128);

		boolean isSuccessful = result.get("isDiscussionTopicExists")    && !result.get("isDiscussionTopicSlugEmpty") &&
							   result.get("isDiscussionTopicSlugLegal") && !result.get("isDiscussionTopicNameEmpty") &&
							   result.get("isDiscussionTopicNameLegal");
		result.put("isSuccessful", isSuccessful);
		if ( isSuccessful ) {
			int parentDiscussionTopicId = parentDiscussionTopic == null ? 0 : parentDiscussionTopic.getParentDiscussionTopicId();
			dt.setDiscussionTopicSlug(discussionTopicSlug);
			dt.setDiscussionTopicName(discussionTopicName);
			dt.setParentDiscussionTopicId(parentDiscussionTopicId);
			discussionTopicMapper.updateDiscussionTopic(dt);
		}
		return result;
	}

	/**
	 * [仅限管理员使用]
	 * 删除讨论主题.
	 * @param discussionTopicId - 讨论主题的唯一标识符.
	 * @return 包含讨论主题删除结果的Map对象
	 */
	public Map<String, Boolean> deleteDiscussionTopic(int discussionTopicId) {
		Map<String, Boolean> result = new HashMap<>(2, 1);
		long numberOfRowsAffected = discussionTopicMapper.deleteDiscussionTopicUsingId(discussionTopicId);
		result.put("isSuccessful", numberOfRowsAffected > 0);
		return result;
	}

	/**
	 * 创建讨论帖子.
	 * @param threadCreator - 讨论帖子的创建者
	 * @param discussionTopicSlug - 讨论帖子对应主题的唯一英文缩写
	 * @param relatedProblemId - 讨论帖子所关联的问题 (可为空)
	 * @param discussionThreadTitle - 讨论帖子的标题
	 * @param discussionThreadContent - 讨论帖子中第一个回复的内容
	 * @param isCsrfTokenValid - CSRF Token是否合法
	 * @return 包含讨论帖子创建结果的Map对象
	 */
	public Map<String, Object> createDiscussionThread(User threadCreator,
			String discussionTopicSlug, long relatedProblemId,
			String discussionThreadTitle, String discussionThreadContent,
			boolean isCsrfTokenValid) {
		DiscussionTopic discussionTopic = discussionTopicMapper.getDiscussionTopicUsingSlug(discussionTopicSlug);
		DiscussionThread dt = new DiscussionThread(threadCreator, discussionTopic,
				null, HtmlTextFilter.filter(discussionThreadTitle));
		Map<String, Object> result = (Map<String, Object>) getDiscussionThreadCreationResult(dt,
										discussionThreadContent, isCsrfTokenValid);
		if ( (Boolean) result.get("isSuccessful") ) {
			if ( relatedProblemId != 0 ) {
				Problem relatedProblem = problemMapper.getProblem(relatedProblemId);
				dt.setProblem(relatedProblem);
			}
			discussionThreadMapper.createDiscussionThread(dt);
			createDiscussionReply(dt.getDiscussionThreadId(), threadCreator, discussionThreadContent, isCsrfTokenValid);
			result.put("discussionThreadId", dt.getDiscussionThreadId());
		}
		return result;
	}

	/**
	 * 获取讨论帖子的创建结果.
	 * @param dt - 待创建的讨论帖
	 * @param discussionThreadContent - 待创建讨论帖中的内容 (内容以DiscussionReply对象存储)
	 * @param isCsrfTokenValid - CSRF Token是否合法
	 * @return 包含讨论帖子创建结果的Map<String, Boolean>对象
	 */
	private Map<String, ? extends Object> getDiscussionThreadCreationResult(
			DiscussionThread dt, String discussionThreadContent, boolean isCsrfTokenValid) {
		Map<String, Boolean> result = new HashMap<>(9, 1);
		result.put("isThreadCreatorExists", dt.getDiscussionThreadCreator() != null);
		result.put("isThreadCreatorLegal", dt.getDiscussionThreadCreator() != null &&
											!dt.getDiscussionThreadCreator().getUserGroup()
													.getUserGroupSlug().equals("forbidden"));
		result.put("isDiscussionTopicExists", dt.getDiscussionTopic() != null);
		result.put("isThreadTitleEmpty", dt.getDiscussionThreadTitle().isEmpty());
		result.put("isThreadTitleLegal", dt.getDiscussionThreadTitle().length() <= 128);
		result.put("isThreadContentEmpty", discussionThreadContent.isEmpty());
		result.put("isCsrfTokenValid", isCsrfTokenValid);

		boolean isSuccessful =  result.get("isThreadCreatorExists")   &&  result.get("isThreadCreatorLegal") &&
								result.get("isDiscussionTopicExists") && !result.get("isThreadTitleEmpty")   &&
								result.get("isThreadTitleLegal")      && !result.get("isThreadContentEmpty") &&
								result.get("isCsrfTokenValid");
		result.put("isSuccessful", isSuccessful);
		return result;
	}

	/**
	 * 编辑讨论帖子.
	 * 编辑条件: 当前用户为管理员或该帖子由用户自己创建.
	 * @param discussionThreadId - 讨论帖子的唯一标识符
	 * @param currentEditor - 当前的编辑者
	 * @param discussionTopicSlug - 讨论帖子对应主题的唯一英文缩写
	 * @param discussionThreadTitle - 讨论帖子的标题
	 * @param isCsrfTokenValid - CSRF Token是否合法
	 * @return 包含讨论帖子编辑结果的Map对象
	 */
	public Map<String, Boolean> editDiscussionThread(long discussionThreadId,
			User currentEditor, String discussionTopicSlug,
			String discussionThreadTitle, boolean isCsrfTokenValid) {
		DiscussionTopic discussionTopic = null;
		DiscussionThread dt = discussionThreadMapper.getDiscussionThreadUsingThreadId(discussionThreadId);;
		Map<String, Boolean> result = new HashMap<>(7, 1);
		result.put("isDiscussionThreadExists", dt != null);
		result.put("isThreadTitleEmpty", discussionThreadTitle.isEmpty());
		result.put("isThreadTitleLegal", discussionThreadTitle.length() <= 128);
		result.put("isCsrfTokenValid", isCsrfTokenValid);

		if ( isCsrfTokenValid ) {
			discussionTopic = discussionTopicMapper.getDiscussionTopicUsingSlug(discussionTopicSlug);
			result.put("isDiscussionTopicExists", discussionTopic != null);
		}
		boolean isSuccessful =  result.get("isDiscussionThreadExists") && result.get("isDiscussionTopicExists") &&
							   !result.get("isThreadTitleEmpty")       && result.get("isThreadTitleLegal")      &&
							    result.get("isCsrfTokenValid");
		result.put("isSuccessful", isSuccessful);
		if ( isSuccessful ) {
			if ( dt.getDiscussionThreadCreator().equals(currentEditor) ||
					currentEditor.getUserGroup().getUserGroupSlug().equals("administrators") ) {
				dt.setDiscussionTopic(discussionTopic);
				dt.setDiscussionThreadTitle(HtmlTextFilter.filter(discussionThreadTitle));
				discussionThreadMapper.updateDiscussionThread(dt);
			}
		}
		return result;
	}

	/**
	 * [仅限管理员使用]
	 * 删除讨论帖子.
	 * @param discussionThreadId - 讨论帖子的唯一标识符.
	 * @return 讨论帖子的删除结果
	 */
	public Map<String, Boolean> deleteDiscussionThread(long discussionThreadId) {
		Map<String, Boolean> result = new HashMap<>(2, 1);
		long numberOfRowsAffected = discussionThreadMapper.deleteDiscussionThreadUsingThreadId(discussionThreadId);

		result.put("isSuccessful", numberOfRowsAffected > 0);
		return result;
	}

	/**
	 * 创建讨论回复.
	 * @param discussionThreadId - 回复对应讨论主题的唯一标识符
	 * @param replyCreator - 回复的创建者
	 * @param replyContent - 回复内容
	 * @param isCsrfTokenValid - CSRF Token是否合法
	 * @return 包含讨论回复创建结果的Map对象.
	 */
	public Map<String, Object> createDiscussionReply(
			long discussionThreadId, User replyCreator,
			String replyContent, boolean isCsrfTokenValid) {
		String discussionReplyVotes = "{ \"up\": [], \"down\": [] }";
		DiscussionReply dr = new DiscussionReply(discussionThreadId, replyCreator,
				HtmlTextFilter.filter(replyContent), discussionReplyVotes);

		Map<String, Object> result = (Map<String, Object>) getDiscussionReplyCreationResult(dr, isCsrfTokenValid);
		if ( (Boolean) result.get("isSuccessful") ) {
			discussionReplyMapper.createDiscussionReply(dr);
			result.put("discussionReply", dr);
		}
		return result;
	}

	/**
	 * 验证讨论回复数据有效性.
	 * @param discussionReply - 待创建的讨论回复对象
	 * @param isCsrfTokenValid -  CSRF Token是否合法
	 * @return 包含讨论回复数据有效性的Map对象
	 */
	private Map<String, ? extends Object> getDiscussionReplyCreationResult(
			DiscussionReply discussionReply, boolean isCsrfTokenValid) {
		long discussionThreadId = discussionReply.getDiscussionThreadId();
		User replyCreator = discussionReply.getDiscussionReplyCreator();
		String replyContent = discussionReply.getDiscussionReplyContent();

		DiscussionThread discussionThread = discussionThreadMapper.getDiscussionThreadUsingThreadId(discussionThreadId);
		Map<String, Boolean> result = new HashMap<>(6, 1);
		result.put("isDiscussionThreadExists", discussionThread != null);
		result.put("isReplyCreatorExists", replyCreator != null);
		result.put("isReplyCreatorLegal", replyCreator != null &&
				!replyCreator.getUserGroup().getUserGroupSlug().equals("forbidden"));
		result.put("isReplyContentEmpty", replyContent.isEmpty());
		result.put("isCsrfTokenValid", isCsrfTokenValid);

		boolean isSuccessful = result.get("isDiscussionThreadExists") &&  result.get("isReplyCreatorExists")  &&
							   result.get("isReplyCreatorLegal")      && !result.get("isReplyContentEmpty")   &&
							   result.get("isCsrfTokenValid");
		result.put("isSuccessful", isSuccessful);
		return result;
	}

	/**
	 * 编辑讨论回复.
	 * 编辑条件: 当前用户为管理员或该回复由用户自己创建.
	 * @param discussionReplyId - 讨论回复的唯一标识符
	 * @param currentEditor - 当前的编辑者
	 * @param discussionReplyContent - 更新后讨论回复的内容
	 * @param isCsrfTokenValid - CSRF Token是否合法
	 * @return 包含讨论回复编辑结果的Map对象
	 */
	public Map<String, Boolean> editDiscussionReply(long discussionReplyId,
		User currentEditor, String discussionReplyContent, boolean isCsrfTokenValid) {
		Map<String, Boolean> result = new HashMap<>(2, 1);
		boolean isSuccessful = false;
		DiscussionReply dr = null;

		if ( isCsrfTokenValid ) {
			dr = discussionReplyMapper.getDiscussionReplyUsingReplyId(discussionReplyId);
		}
		if ( dr != null ) {
			if ( dr.getDiscussionReplyCreator().equals(currentEditor) ||
					currentEditor.getUserGroup().getUserGroupSlug().equals("administrators") ) {
				dr.setDiscussionReplyContent(HtmlTextFilter.filter(discussionReplyContent));
				discussionReplyMapper.updateDiscussionReply(dr);
				isSuccessful = true;
			}
		}
		result.put("isSuccessful", isSuccessful);
		return result;
	}

	/**
	 * 删除讨论回复.
	 * 删除条件: 当前用户为管理员或该回复由用户自己创建.
	 * @param discussionReplyId - 讨论回复的唯一标识符
	 * @param currentEditor - 当前的编辑者
	 * @param isCsrfTokenValid - CSRF Token是否合法
	 * @return 包含讨论回复删除结果的Map对象.
	 */
	public Map<String, Boolean> deleteDiscussionReply(long discussionReplyId,
			User currentEditor, boolean isCsrfTokenValid) {
		Map<String, Boolean> result = new HashMap<>(2, 1);
		boolean isSuccessful = false;
		DiscussionReply dr = null;

		if ( isCsrfTokenValid ) {
			dr = discussionReplyMapper.getDiscussionReplyUsingReplyId(discussionReplyId);
		}
		if ( dr != null ) {
			if ( dr.getDiscussionReplyCreator().equals(currentEditor) ||
					currentEditor.getUserGroup().getUserGroupSlug().equals("administrators") ) {
				discussionReplyMapper.deleteDiscussionReplyUsingReplyId(discussionReplyId);
				isSuccessful = true;
			}
		}
		result.put("isSuccessful", isSuccessful);
		return result;
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
	 * 自动注入的ProblemMapper对象.
	 * 用于获取试题.
	 */
	@Autowired
	private ProblemMapper problemMapper;

	/**
	 * 自动注入的SensitiveWordFilter对象.
	 * 用于过滤用户个人信息中的敏感词.
	 */
	@Autowired
	private OffensiveWordFilter offensiveWordFilter;
}
