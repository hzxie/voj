package org.verwandlung.voj.web.mapper;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;
import org.verwandlung.voj.web.model.DiscussionReply;
import org.verwandlung.voj.web.model.User;

import java.util.Date;
import java.util.List;

/**
 * DiscussionThreadMapper的测试类.
 *
 * @author Haozhe Xie
 */
@RunWith(SpringJUnit4ClassRunner.class)
@Transactional
@ContextConfiguration({"classpath:test-spring-context.xml"})
public class DiscussionReplyMapperTest {
	/**
	 * 测试用例: 测试getDiscussionRepliesUsingThreadId(long, long, int)方法.
	 * 测试数据: ThreadId: 2, Offset: 0, Limit: 2
	 * 预期结果: 返回第二个讨论帖子的前2条评论
	 */
	@Test
	public void testGetDiscussionRepliesOfThread2WithOffsetFrom0WithLimit2() {
		List<DiscussionReply> discussionReplies = discussionReplyMapper.getDiscussionRepliesUsingThreadId(2, 0, 2);
		Assert.assertEquals(2, discussionReplies.size());

		DiscussionReply firstDiscussionReply = discussionReplies.get(0);
		long replyId = firstDiscussionReply.getDiscussionReplyId();
		Assert.assertEquals(2, replyId);

		String replyContent = firstDiscussionReply.getDiscussionReplyContent();
		Assert.assertEquals("Reply content for thread #2", replyContent);
	}

	/**
	 * 测试用例: 测试getDiscussionRepliesUsingThreadId(long, long, int)方法.
	 * 测试数据: ThreadId: 2, Offset: 1, Limit: 1
	 * 预期结果: 返回第二个讨论帖子的第2条评论
	 */
	@Test
	public void testGetDiscussionRepliesOfThread2WithOffsetFrom1WithLimit1() {
		List<DiscussionReply> discussionReplies = discussionReplyMapper.getDiscussionRepliesUsingThreadId(2, 1, 1);
		Assert.assertEquals(1, discussionReplies.size());

		DiscussionReply firstDiscussionReply = discussionReplies.get(0);
		long replyId = firstDiscussionReply.getDiscussionReplyId();
		Assert.assertEquals(3, replyId);

		String replyContent = firstDiscussionReply.getDiscussionReplyContent();
		Assert.assertEquals("Reply content for thread #2", replyContent);
	}

	/**
	 * 测试用例: 测试createDiscussionReply(DiscussionReply)方法.
	 * 测试数据: 合法数据集
	 * 预期结果: 数据插入操作成功完成
	 */
	@Test
	public void testCreateDiscussionReplyNormally() {
		long threadId = 1;
		User creator = userMapper.getUserUsingUid(1000);
		Date createTime = new Date();

		Assert.assertNotNull(creator);

		DiscussionReply discussionReply = new DiscussionReply(threadId, creator, createTime, "Content", "{}");
		int numberOfRowsAffected = discussionReplyMapper.createDiscussionReply(discussionReply);
		Assert.assertEquals(1, numberOfRowsAffected);
	}

	/**
	 * 测试用例: 测试createDiscussionReply(DiscussionReply)方法.
	 * 测试数据: 使用不存在的用户
	 * 预期结果: 抛出DataIntegrityViolationException异常
	 */
	@Test(expected = org.springframework.dao.DataIntegrityViolationException.class)
	public void testCreateDiscussionReplyWithNotExistingUser() {
		long threadId = 1;
		User creator = new User(); creator.setUid(0);
		Date createTime = new Date();

		DiscussionReply discussionReply = new DiscussionReply(threadId, creator, createTime, "Content", "{}");
		discussionReplyMapper.createDiscussionReply(discussionReply);
	}

	/**
	 * 测试用例: 测试updateDiscussionReply(DiscussionReply)方法.
	 * 测试数据: 使用合法数据更新
	 * 预期结果: 数据更新操作成功完成
	 */
	@Test
	public void testUpdateDiscussionReplyNormally() {
		List<DiscussionReply> discussionReplies = discussionReplyMapper.getDiscussionRepliesUsingThreadId(1, 0, 1);
		Assert.assertEquals(1, discussionReplies.size());

		DiscussionReply discussionReply = discussionReplies.get(0);
		discussionReply.setDiscussionReplyContent("New Reply Content");
		int numberOfRowsAffected = discussionReplyMapper.updateDiscussionReply(discussionReply);
		Assert.assertEquals(1, numberOfRowsAffected);
	}

	/**
	 * 测试用例: 测试deleteDiscussionReplyUsingReplyId(long)方法.
	 * 测试数据: 存在的讨论回复唯一标识符
	 * 预期结果: 数据删除操作成功完成
	 */
	@Test
	public void testDeleteDiscussionReplyUsingExistingReplyId() {
		int numberOfRowsAffected = discussionReplyMapper.deleteDiscussionReplyUsingReplyId(1);
		Assert.assertEquals(1, numberOfRowsAffected);
	}

	/**
	 * 测试用例: 测试deleteDiscussionReplyUsingReplyId(long)方法.
	 * 测试数据: 不存在的讨论回复唯一标识符
	 * 预期结果: 方法正常执行, 未影响数据表中的数据
	 */
	@Test
	public void testDeleteDiscussionReplyUsingReplyIdNotExists() {
		int numberOfRowsAffected = discussionReplyMapper.deleteDiscussionReplyUsingReplyId(0);
		Assert.assertEquals(0, numberOfRowsAffected);
	}

	/**
	 * 待测试的DiscussionReplyMapper对象.
	 */
	@Autowired
	private DiscussionReplyMapper discussionReplyMapper;

	/**
	 * 待测试的UserMapper对象.
	 */
	@Autowired
	private UserMapper userMapper;
}