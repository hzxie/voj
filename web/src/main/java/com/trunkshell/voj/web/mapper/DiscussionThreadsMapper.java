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

import com.trunkshell.voj.web.model.DiscussionThreads;

/**
 * DiscussionThreads Data Access Object.
 * 
 * @author Luo Guofu
 */
@CacheNamespace(implementation = org.mybatis.caches.ehcache.EhcacheCache.class)
public interface DiscussionThreadsMapper {
	
	public DiscussionThreads getSingleDiscussionThreads(long discussionThreadsId);
	
	
	public List<DiscussionThreads> getDiscussionThreads(@Param("offset") long offset, @Param("limit") int limit);
	
	
	public List<DiscussionThreads> getDiscussionThreadsUsingTopicId(@Param("discussionTopicId") int discussionTopicId, @Param("offset") long offset, @Param("limit") int limit);
	
	
	public int createDiscussionThreads(DiscussionThreads discussionThreads);
	
	
	public int updateDiscussionThreads(DiscussionThreads discussionThreads);
	
	
	public int deleteDiscussionThreads(long discussionThreadsId);
}
