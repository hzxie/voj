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
package org.verwandlung.voj.web.mapper;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;
import org.verwandlung.voj.web.model.DiscussionReply;
import org.verwandlung.voj.web.model.User;

import java.util.List;

/**
 * DiscussionThreadMapper的测试类.
 *
 * @author Haozhe Xie
 */
@ExtendWith(SpringExtension.class)
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
		Assertions.assertEquals(2, discussionReplies.size());

		DiscussionReply firstDiscussionReply = discussionReplies.get(0);
		long replyId = firstDiscussionReply.getDiscussionReplyId();
		Assertions.assertEquals(2, replyId);

		String replyContent = firstDiscussionReply.getDiscussionReplyContent();
		Assertions.assertEquals("Reply content for thread #2", replyContent);
	}

	/**
	 * 测试用例: 测试getDiscussionRepliesUsingThreadId(long, long, int)方法.
	 * 测试数据: ThreadId: 2, Offset: 1, Limit: 1
	 * 预期结果: 返回第二个讨论帖子的第2条评论
	 */
	@Test
	public void testGetDiscussionRepliesOfThread2WithOffsetFrom1WithLimit1() {
		List<DiscussionReply> discussionReplies = discussionReplyMapper.getDiscussionRepliesUsingThreadId(2, 1, 1);
		Assertions.assertEquals(1, discussionReplies.size());

		DiscussionReply firstDiscussionReply = discussionReplies.get(0);
		long replyId = firstDiscussionReply.getDiscussionReplyId();
		Assertions.assertEquals(3, replyId);

		String replyContent = firstDiscussionReply.getDiscussionReplyContent();
		Assertions.assertEquals("Reply content for thread #2", replyContent);
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
		Assertions.assertNotNull(creator);

		DiscussionReply discussionReply = new DiscussionReply(threadId, creator, "Content", "{}");
		int numberOfRowsAffected = discussionReplyMapper.createDiscussionReply(discussionReply);
		Assertions.assertEquals(1, numberOfRowsAffected);
	}

	/**
	 * 测试用例: 测试createDiscussionReply(DiscussionReply)方法.
	 * 测试数据: 使用不存在的用户
	 * 预期结果: 抛出DataIntegrityViolationException异常
	 */
	@Test
	public void testCreateDiscussionReplyWithNotExistingUser() {
		long threadId = 1;
		User creator = new User(); creator.setUid(0);

		DiscussionReply discussionReply = new DiscussionReply(threadId, creator, "Content", "{}");
		Executable e = () -> {
			discussionReplyMapper.createDiscussionReply(discussionReply);
		};
		Assertions.assertThrows(org.springframework.dao.DataIntegrityViolationException.class, e);
	}

	/**
	 * 测试用例: 测试updateDiscussionReply(DiscussionReply)方法.
	 * 测试数据: 使用合法数据更新
	 * 预期结果: 数据更新操作成功完成
	 */
	@Test
	public void testUpdateDiscussionReplyNormally() {
		List<DiscussionReply> discussionReplies = discussionReplyMapper.getDiscussionRepliesUsingThreadId(1, 0, 1);
		Assertions.assertEquals(1, discussionReplies.size());

		DiscussionReply discussionReply = discussionReplies.get(0);
		discussionReply.setDiscussionReplyContent("New Reply Content");
		int numberOfRowsAffected = discussionReplyMapper.updateDiscussionReply(discussionReply);
		Assertions.assertEquals(1, numberOfRowsAffected);
	}

	/**
	 * 测试用例: 测试deleteDiscussionReplyUsingReplyId(long)方法.
	 * 测试数据: 存在的讨论回复唯一标识符
	 * 预期结果: 数据删除操作成功完成
	 */
	@Test
	public void testDeleteDiscussionReplyUsingExistingReplyId() {
		int numberOfRowsAffected = discussionReplyMapper.deleteDiscussionReplyUsingReplyId(1);
		Assertions.assertEquals(1, numberOfRowsAffected);
	}

	/**
	 * 测试用例: 测试deleteDiscussionReplyUsingReplyId(long)方法.
	 * 测试数据: 不存在的讨论回复唯一标识符
	 * 预期结果: 方法正常执行, 未影响数据表中的数据
	 */
	@Test
	public void testDeleteDiscussionReplyUsingReplyIdNotExists() {
		int numberOfRowsAffected = discussionReplyMapper.deleteDiscussionReplyUsingReplyId(0);
		Assertions.assertEquals(0, numberOfRowsAffected);
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