package com.trunkshell.voj.web.mapper;

import java.util.List;

import org.apache.ibatis.annotations.CacheNamespace;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import com.trunkshell.voj.web.model.DiscussionTopic;

/**
 * DiscussionTopic Data Access Object.
 * 
 * @author Luo Guofu
 */
@CacheNamespace(implementation = org.mybatis.caches.ehcache.EhcacheCache.class)
public interface DiscussionTopicMapper {
	
	/**
	 * @return
	 */
	@Select("SELECT * FROM voj_discussion_topics")
	@Results(value = {
			@Result(property = "topicId", column = "discussion_topic_id"),
			@Result(property = "topicSlug", column = "discussion_topic_slug"),
			@Result(property = "topicName", column = "discussion_topic_name"),
			@Result(property = "topicParentId", column = "discussion_parent_topic_id"),
	})
	public List<DiscussionTopic> getDiscussionTopics();
	
	@Insert("INSERT INTO voj_discussion_topics (discussion_topic_slug, discussion_topic_name, discussion_parent_topic_id) VALUES (#{topicSlug}, #{topicName}, #{topicParentId})")
	@Options(useGeneratedKeys = true, keyProperty = "topicId", keyColumn = "discussion_topic_id", flushCache = true)
	public void createDiscussionTopic(DiscussionTopic discussionTopic);
	
	
	/**
	 * @param discussionTopic
	 */
	@Update("UPDATE voj_discussion_topics SET discussion_topic_slug = #{topicSlug}, discussion_topic_name = #{topicName}, discussion_parent_topic_id = #{topicParentId} WHERE discussion_topic_id = #{topicId}")
	@Options(flushCache = true)
	public void updateDiscussionTopic(DiscussionTopic discussionTopic);
	
	/**
	 * 删除讨论节点.
	 * @param uid - 待删除讨论节点的用户唯一标识符
	 */
	@Delete("DELETE FROM voj_discussion_topics WHERE discussion_topic_id = #{topicId}")
	@Options(flushCache = true)
	public void deleteDiscussionTopic(int topicId);
}
