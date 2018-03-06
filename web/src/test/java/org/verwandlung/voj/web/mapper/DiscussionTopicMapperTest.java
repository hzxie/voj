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
import org.verwandlung.voj.web.model.DiscussionTopic;

import java.util.List;

/**
 * DiscussionTopicMapper的测试类.
 *
 * @author Haozhe Xie
 */
@ExtendWith(SpringExtension.class)
@Transactional
@ContextConfiguration({"classpath:test-spring-context.xml"})
public class DiscussionTopicMapperTest {
	/**
	 * 测试用例: 测试getDiscussionTopics()方法
	 * 测试数据: N/a
	 * 预期结果: 返回一个包含两个DiscussionTopic的列表
	 */
	@Test
	public void testGetDiscussionTopics() {
		List<DiscussionTopic> topics = discussionTopicMapper.getDiscussionTopics();
		Assertions.assertEquals(4, topics.size());

		DiscussionTopic firstTopic = topics.get(0);
		Assertions.assertEquals("solutions", firstTopic.getDiscussionTopicSlug());
	}

	/**
	 * 测试用例: 测试getDiscussionTopicUsingId(int)方法
	 * 测试数据: 数据库中存在的DiscussionTopic的唯一标识符
	 * 预期结果: 返回预期的DiscussionTopic对象
	 */
	@Test
	public void testGetDiscussionTopicUsingIdExists() {
		DiscussionTopic topic = discussionTopicMapper.getDiscussionTopicUsingId(1);
		Assertions.assertNotNull(topic);
		Assertions.assertEquals("solutions", topic.getDiscussionTopicSlug());
	}

	/**
	 * 测试用例: 测试getDiscussionTopicUsingId(int)方法
	 * 测试数据: 数据库中不存在的DiscussionTopic的唯一标识符
	 * 预期结果: 返回空引用
	 */
	@Test
	public void testGetDiscussionTopicUsingIdNotExists() {
		DiscussionTopic topic = discussionTopicMapper.getDiscussionTopicUsingId(0);
		Assertions.assertNull(topic);
	}

	/**
	 * 测试用例: getDiscussionTopicUsingSlug(String)方法
	 * 测试数据: 数据库中存在的DiscussionTopic的别名
	 * 预期结果: 返回预期的DiscussionTopic对象
	 */
	@Test
	public void testGetDiscussionTopicUsingSlugExists() {
		DiscussionTopic topic = discussionTopicMapper.getDiscussionTopicUsingSlug("solutions");
		Assertions.assertNotNull(topic);
		Assertions.assertEquals(1, topic.getDiscussionTopicId());
	}

	/**
	 * 测试用例: getDiscussionTopicUsingSlug(String)方法
	 * 测试数据: 数据库中不存在的DiscussionTopic的别名
	 * 预期结果: 返回空引用
	 */
	@Test
	public void testGetDiscussionTopicUsingSlugNotExists() {
		DiscussionTopic topic = discussionTopicMapper.getDiscussionTopicUsingSlug("NotExistingTopic");
		Assertions.assertNull(topic);
	}

	/**
	 * 测试用例: 测试createDiscussionTopic(DiscussionTopic)方法
	 * 测试数据: 使用合法的数据集, 并且该别名不存在相应的记录
	 * 预期结果: DiscussionTopic被成功创建
	 */
	@Test
	public void testCreateDiscussionTopicNormally() {
		DiscussionTopic topic = new DiscussionTopic("new-topic", "New Topic", 0);
		int numberOfRowsAffected = discussionTopicMapper.createDiscussionTopic(topic);
		Assertions.assertEquals(1, numberOfRowsAffected);
	}

	/**
	 * 测试用例: 测试createDiscussionTopic(DiscussionTopic)方法
	 * 测试数据: 使用合法的数据集, 但是该别名存在相应的记录
	 * 预期结果: 抛出DuplicateKeyException异常
	 */
	@Test
	public void testCreateDiscussionTopicWithDupliateSlug() {
		DiscussionTopic topic = new DiscussionTopic("general", "New Topic", 0);
		Executable e = () -> {
			discussionTopicMapper.createDiscussionTopic(topic);
		};
		Assertions.assertThrows(org.springframework.dao.DuplicateKeyException.class, e);
	}


	/**
	 * 测试用例: 测试createDiscussionTopic(DiscussionTopic)方法
	 * 测试数据: 使用不合法的数据集, 别名的长度过长
	 * 预期结果: DataIntegrityViolationException
	 */
	@Test
	public void testCreateDiscussionTopicWithTooLongSlug() {
		StringBuilder sb = new StringBuilder();
		for ( int i = 0; i < 31; ++ i ) {
			sb.append("Very");
		}
		DiscussionTopic topic = new DiscussionTopic(sb.toString() + "LongSlug", "New Topic", 0);
		Executable e = () -> {
			discussionTopicMapper.createDiscussionTopic(topic);
		};
		Assertions.assertThrows(org.springframework.dao.DataIntegrityViolationException.class, e);
	}

	/**
	 * 测试用例: 测试updateDiscussionTopic(DiscussionTopic)
	 * 测试数据: 使用合法的数据集, 并且该别名不存在相应的记录
	 * 预期结果: 数据插入操作成功完成
	 */
	@Test
	public void testUpdateDiscussionTopicNormally() {
		DiscussionTopic topic = discussionTopicMapper.getDiscussionTopicUsingSlug("general");
		Assertions.assertNotNull(topic);

		topic.setDiscussionTopicName("New Topic Name");
		int numberOfRowsAffected = discussionTopicMapper.updateDiscussionTopic(topic);
		Assertions.assertEquals(1, numberOfRowsAffected);

		topic = discussionTopicMapper.getDiscussionTopicUsingSlug("general");
		Assertions.assertEquals("New Topic Name", topic.getDiscussionTopicName());
	}

	/**
	 * 测试用例: 测试updateDiscussionTopic(DiscussionTopic)
	 * 测试数据: 使用合法的数据集, 但是该别名存在相应的记录
	 * 预期结果: 抛出DuplicateKeyException异常
	 */
	@Test
	public void testUpdateDiscussionTopicWithDupliateSlug() {
		DiscussionTopic topic = discussionTopicMapper.getDiscussionTopicUsingSlug("general");
		Assertions.assertNotNull(topic);

		topic.setDiscussionTopicSlug("support");
		Executable e = () -> {
			discussionTopicMapper.updateDiscussionTopic(topic);
		};
		Assertions.assertThrows(org.springframework.dao.DuplicateKeyException.class, e);
	}

	/**
	 * 测试用例: deleteDiscussionTopicUsingId(int)
	 * 测试数据: 使用存在的DiscussionTopic的唯一标识符
	 * 预期结果: 数据删除操作成功完成
	 */
	@Test
	public void testDeleteDiscussionTopicExists() {
		DiscussionTopic topic = discussionTopicMapper.getDiscussionTopicUsingId(1);
		Assertions.assertNotNull(topic);

		int numberOfRowsAffected = discussionTopicMapper.deleteDiscussionTopicUsingId(1);
		Assertions.assertEquals(1, numberOfRowsAffected);

		topic = discussionTopicMapper.getDiscussionTopicUsingId(1);
		Assertions.assertNull(topic);
	}

	/**
	 * 测试用例: deleteDiscussionTopicUsingId(int)
	 * 测试数据: 使用不存在的DiscussionTopic的唯一标识符
	 * 预期结果: 方法正常执行, 未影响数据表中的数据
	 */
	@Test
	public void testDeleteDiscussionTopicNotExists() {
		DiscussionTopic topic = discussionTopicMapper.getDiscussionTopicUsingId(0);
		Assertions.assertNull(topic);

		int numberOfRowsAffected = discussionTopicMapper.deleteDiscussionTopicUsingId(0);
		Assertions.assertEquals(0, numberOfRowsAffected);
	}

	/**
	 * 待测试的DiscussionTopicMapper对象.
	 */
	@Autowired
	private DiscussionTopicMapper discussionTopicMapper;
}