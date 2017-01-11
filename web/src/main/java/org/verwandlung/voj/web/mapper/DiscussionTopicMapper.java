package org.verwandlung.voj.web.mapper;

import org.apache.ibatis.annotations.CacheNamespace;
import org.verwandlung.voj.web.model.DiscussionTopic;

import java.util.List;

/**
 * DiscussionTopic Data Access Object.
 *
 * @author Haozhe Xie
 */
@CacheNamespace(implementation = org.mybatis.caches.ehcache.EhcacheCache.class)
public interface DiscussionTopicMapper {
	/**
	 * 获取全部的讨论话题.
	 * @return 包含全部讨论话题的List<DiscussionTopic>对象
	 */
	List<DiscussionTopic> getDiscussionTopics();

	/**
	 * 根据讨论话题的唯一标识符获取讨论话题对象.
	 * @param discussionTopicId - 讨论话题的唯一标识符
	 * @return 一个讨论话题对象
	 */
	DiscussionTopic getDiscussionTopicUsingId(int discussionTopicId);

	/**
	 * 根据讨论话题的唯一别名获取讨论话题对象.
	 * @param discussionTopicSlug - 讨论话题的别名
	 * @return 一个讨论话题对象
	 */
	DiscussionTopic getDiscussionTopicUsingSlug(String discussionTopicSlug);

	/**
	 * 创建讨论话题.
	 * @param discussionTopic - 待创建的讨论话题对象
	 */
	int createDiscussionTopic(DiscussionTopic discussionTopic);

	/**
	 * 更新讨论话题.
	 * @param discussionTopic - 待更新的讨论话题对象
	 */
	int updateDiscussionTopic(DiscussionTopic discussionTopic);

	/**
	 * 删除讨论话题.
	 * @param discussionTopicId - 待删除讨论话题的唯一标识符
	 */
	int deleteDiscussionTopicUsingId(int discussionTopicId);
}
