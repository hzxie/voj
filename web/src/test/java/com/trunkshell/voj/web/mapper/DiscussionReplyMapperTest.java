package com.trunkshell.voj.web.mapper;

import java.util.Date;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.transaction.TransactionConfiguration;
import org.springframework.transaction.annotation.Transactional;

import com.trunkshell.voj.web.model.DiscussionReply;

/**
 * DiscussionReplyMapper测试类
 * @author Luo Guofu
 */
@RunWith(SpringJUnit4ClassRunner.class)
@Transactional
@TransactionConfiguration(defaultRollback = true)
@ContextConfiguration({"classpath:test-spring-context.xml"})
public class DiscussionReplyMapperTest {
	@Test
	public void testGetDiscussionRepliesUsingThreadsId() {
		List<DiscussionReply> drs = discussionReplyMapper.getDiscussionRepliesUsingThreadsId(1, 0, 5);
		Assert.assertEquals(2, drs.size());
		System.out.println(drs);
	}
	
	@Test
	public void testCreateDiscussionReply() {
		DiscussionReply dr = new DiscussionReply(1, 1002, new Date(), "reply-content-3");
		int count = discussionReplyMapper.createDiscussionReply(dr);
		Assert.assertEquals(1, count);
		System.out.println(dr);
	}
	
	@Test
	public void testCreateDiscussionReplyUsingNotExistsThreadsId() {
		DiscussionReply dr = new DiscussionReply(88888, 1002, new Date(), "reply-content-3");
		int count = discussionReplyMapper.createDiscussionReply(dr);
	}
	
	@Test
	public void testUpdateDiscussionReply() {
		DiscussionReply dr = discussionReplyMapper.getDiscussionReply(1);
		dr.setDiscussionReplyContent("content-xxxxxxxxxxxxxx");
		int count = discussionReplyMapper.updateDiscussionReply(dr);
		Assert.assertEquals(1, count);
	}
	
	@Test
	public void testDeleteDiscussionReplyUsingExistsReplyId() {
		int count = discussionReplyMapper.deleteDiscussionReply(1);
		Assert.assertEquals(1, count);
	}
	
	@Test
	public void testDeleteDiscussionReplyUsingNotExistsReplyId() {
		int count = discussionReplyMapper.deleteDiscussionReply(1000);
		Assert.assertEquals(0, count);
	}

	@Autowired
	private DiscussionReplyMapper discussionReplyMapper;
}
