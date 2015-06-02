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
	
	@Test
	public void testGetAllTopics() {
		List<DiscussionTopic> topics = discussionTopicMapper.getDiscussionTopics();
		System.out.println(topics);
		Assert.assertNotNull(topics);
	}
	
	@Test
	public void testCreateTopicNormal() {
		DiscussionTopic topic = new DiscussionTopic("BFS", "广度优先搜索");
		discussionTopicMapper.createDiscussionTopic(topic);
		Assert.assertNotEquals(0, topic.getTopicId());
	}
	
	/**
	 * 讨论节点的父节点唯一标识符
	 */
	@Test(expected = org.springframework.dao.DataIntegrityViolationException.class)
	public void testCreateTopicUsingPidNotExists() {
		DiscussionTopic topic = new DiscussionTopic("BFS", "广度优先搜索", 0);
		discussionTopicMapper.createDiscussionTopic(topic);
		testGetAllTopics();
	}

	@Autowired
	DiscussionTopicMapper discussionTopicMapper;
}
