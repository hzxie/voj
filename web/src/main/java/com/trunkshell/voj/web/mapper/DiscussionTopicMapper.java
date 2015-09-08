package com.trunkshell.voj.web.mapper;

import java.util.List;

import org.apache.ibatis.annotations.CacheNamespace;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.ResultMap;
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
	
	
	public DiscussionTopic getDiscussionTopicUsingTid(int topicId);
	
	
	public List<DiscussionTopic> getDiscussionTopics();
	
	
	public void createDiscussionTopic(DiscussionTopic discussionTopic);
	
	
	public int updateDiscussionTopic(DiscussionTopic discussionTopic);
	
	/**
	 * 删除讨论节点.
	 * @param topicId - 待删除讨论节点的用户唯一标识符
	 */
	public int deleteDiscussionTopic(int topicId);
}
