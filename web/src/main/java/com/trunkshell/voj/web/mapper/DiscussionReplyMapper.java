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

import com.trunkshell.voj.web.model.DiscussionReply;

/**
 * DiscussionReply Data Access Object.
 * 
 * @author Luo Guofu
 */
@CacheNamespace(implementation = org.mybatis.caches.ehcache.EhcacheCache.class)
public interface DiscussionReplyMapper {
	
	public DiscussionReply getDiscussionReply(long discussionReplyId);
	
	
	public List<DiscussionReply> getDiscussionRepliesUsingThreadsId(@Param("discussionThreadsId") long discussionThreadsId, @Param("offset") long offset, @Param("limit") int limit);
	
	
	public int createDiscussionReply(DiscussionReply discussionReply);
	
	
	public int updateDiscussionReply(DiscussionReply discussionReply);
	
	
	public int deleteDiscussionReply(long discussionReplyId);
}
