package org.verwandlung.voj.web.service;

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
	 * @param offset - 起始回复的游标
	 * @param limit - 获取回复的数量
	 * @return 包含讨论话题回复的List对象
	 */
	public List<DiscussionReply> getDiscussionRepliesOfThread(long discussionThreadId, long offset, int limit) {
		List<DiscussionReply> replies = discussionReplyMapper.getDiscussionRepliesUsingThreadId(discussionThreadId, offset, limit);
		// 过滤回复中的敏感内容
		for ( DiscussionReply reply : replies ) {
			String replyContent = reply.getDiscussionReplyContent();
			replyContent = offensiveWordFilter.filter(HtmlTextFilter.filter(replyContent));
			reply.setDiscussionReplyContent(replyContent);
		}
		return replies;
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
