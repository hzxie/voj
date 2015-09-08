package com.trunkshell.voj.web.mapper;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.transaction.TransactionConfiguration;
import org.springframework.transaction.annotation.Transactional;

import com.trunkshell.voj.web.model.DiscussionTopic;

/**
 * DiscussionTopicMapper测试类
 * @author Luo Guofu
 */
@RunWith(SpringJUnit4ClassRunner.class)
@Transactional
@TransactionConfiguration(defaultRollback = true)
@ContextConfiguration({"classpath:test-spring-context.xml"})
public class DiscussionTopicMapperTest {	
	/**
	 * 测试用例: 测试getDiscussionTopics()方法
	 * 测试数据: 无
	 * 预期结果: 返回所有记录
	 */
	@Test
	public void testGetAllTopics() {
		List<DiscussionTopic> topics = discussionTopicMapper.getDiscussionTopics();
		System.out.println(topics);
		Assert.assertNotNull(topics);
		System.out.println("--------------" + topics.size());
		Assert.assertNotEquals(0, topics.size());
	}
	
	/**
	 * 测试用例: 测试createDiscussionTopic(DiscussionTopic)方法
	 * 测试数据: 合法的数据集
	 * 预期结果: 数据插入操作成功完成
	 */
	@Test
	public void testCreateTopicNormal() {
		DiscussionTopic topic = new DiscussionTopic("BFS", "广度优先搜索");
		discussionTopicMapper.createDiscussionTopic(topic);
		Assert.assertNotEquals(0, topic.getTopicId());
	}
	
	/**
	 * 测试用例: 测试createDiscussionTopic(DiscussionTopic)方法
	 * 测试数据: 非法的数据集（不存在的讨论节点的父节点唯一标识符）
	 * 预期结果: 抛出org.springframework.dao.DataIntegrityViolationException异常
	 */
	@Test(expected = org.springframework.dao.DataIntegrityViolationException.class)
	public void testCreateTopicUsingPidNotExists() {
		DiscussionTopic topic = new DiscussionTopic("BFS", "广度优先搜索", 0);
		discussionTopicMapper.createDiscussionTopic(topic);
		testGetAllTopics();
	}
	
	/**
	 * 测试用例: 测试updateDiscussionTopic(DiscussionTopic)方法
	 * 测试数据: 使用存在的讨论节点唯一标识符
	 * 预期结果: 方法正常执行，影响一条记录
	 */
	@Test
	public void testUpdateTopicUsingExistsId() {
		DiscussionTopic topic = new DiscussionTopic(1, "BFS", "Breadth First Search", null);
		int count = discussionTopicMapper.updateDiscussionTopic(topic);
		Assert.assertEquals(1, count);
	}
	
	/**
	 * 测试用例: 测试updateDiscussionTopic(DiscussionTopic)方法
	 * 测试数据: 使用不存在的讨论节点唯一标识符
	 * 预期结果: 方法正常执行, 未影响数据表中的数据
	 */
	@Test
	public void testUpdateTopicUsingNotExistsId() {
		DiscussionTopic topic = new DiscussionTopic(1000, "BFS", "Breadth First Search", null);
		int count = discussionTopicMapper.updateDiscussionTopic(topic);
		Assert.assertEquals(0, count);
	}
	
	/**
	 * 测试用例: 测试deleteDiscussionTopic(long)方法
	 * 测试数据: 存在的讨论节点唯一标识符
	 * 预期结果: 方法正常执行，影响一条记录
	 */
	@Test
	public void testDeleteTopicUsingExistsId() {
		int count = discussionTopicMapper.deleteDiscussionTopic(1);
		Assert.assertEquals(1, count);
	}
	
	/**
	 * 测试用例: 测试deleteDiscussionTopic(long)方法
	 * 测试数据: 不存在的讨论节点唯一标识符
	 * 预期结果: 方法正常执行, 未影响数据表中的数据
	 */
	@Test
	public void testDeleteTopicUsingNotExistsId() {
		int count = discussionTopicMapper.deleteDiscussionTopic(1000);
		Assert.assertEquals(0, count);
	}

	@Autowired
	DiscussionTopicMapper discussionTopicMapper;
}
