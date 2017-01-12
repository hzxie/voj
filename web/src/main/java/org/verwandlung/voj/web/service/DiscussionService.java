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
	 * @return 包含讨论话题及其继承关系的Map<DiscussionTopic, List<DiscussionTopic>>对象
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
	 * 获取某个讨论话题的回复.
	 * @param discussionThreadId - 讨论话题的唯一标识符
	 * @param @param offset 起始回复的游标
	 * @param limit 获取回复的数量
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
