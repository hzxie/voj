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

import com.trunkshell.voj.web.model.DiscussionThreads;

/**
 * DiscussionThreadsMapper测试类
 * @author Luo Guofu
 */
@RunWith(SpringJUnit4ClassRunner.class)
@Transactional
@TransactionConfiguration(defaultRollback = true)
@ContextConfiguration({"classpath:test-spring-context.xml"})
public class DiscussionThreadsMapperTest {
	/**
	 * 测试用例: 测试getSingleDiscussionThreads(long)方法
	 * 测试数据: 存在的讨论文章唯一标识符
	 * 预期结果: 返回预期的讨论文章对象
	 */
	@Test
	public void testSelectUsingExistsId() {
		DiscussionThreads dt = discussionThreadsMapper.getSingleDiscussionThreads(1);
		Assert.assertNotEquals(null, dt);
	}
	
	/**
	 * 测试用例: 测试getSingleDiscussionThreads(long)方法
	 * 测试数据: 不存在的讨论文章唯一标识符
	 * 预期结果: 返回空引用
	 */
	@Test
	public void testSelectUsingNotExistsId() {
		DiscussionThreads dt = discussionThreadsMapper.getSingleDiscussionThreads(1000);
		Assert.assertEquals(null, dt);
	}
	
	/**
	 * 测试用例: 测试deleteDiscussionThreads(long)方法
	 * 测试数据: 存在的讨论文章唯一标识符
	 * 预期结果: 方法正常执行，影响一条记录
	 */
	@Test
	public void testDeleteUsingExistsId() {
		int count = discussionThreadsMapper.deleteDiscussionThreads(1);
		Assert.assertEquals(1, count);
	}
	
	/**
	 * 测试用例: 测试deleteDiscussionThreads(long)方法
	 * 测试数据: 不存在的讨论文章唯一标识符
	 * 预期结果: 方法正常执行，未影响数据表中的数据
	 */
	@Test
	public void testDeleteUsingNotExistsId() {
		int count = discussionThreadsMapper.deleteDiscussionThreads(1000);
		Assert.assertEquals(0, count);
	}
	
	/**
	 * 测试用例: 测试getDiscussionThreads(long, int)方法
	 * 测试数据: 合法的数据集
	 * 预期结果: 返回预期分页数据
	 */
	@Test
	public void testGetDiscussionThreads() {
		List<DiscussionThreads> dts = discussionThreadsMapper.getDiscussionThreads(0, 12);
		Assert.assertEquals(2, dts.size());
		System.out.println(dts);
	}
	
	/**
	 * 测试用例: 测试getDiscussionThreads(long, int)方法
	 * 测试数据: 非法的数据集（不合法的分页参数）
	 * 预期结果: 抛出org.springframework.jdbc.BadSqlGrammarException异常
	 */
	@Test(expected = org.springframework.jdbc.BadSqlGrammarException.class)
	// SQL语法不对，参数要求两个正整数
	public void testGetDiscussionThreadsWithErrorParams() {
		discussionThreadsMapper.getDiscussionThreads(-1, -2);
	}
	
	/**
	 * 测试用例: 测试getDiscussionThreadsUsingTopicId(int, long, int)方法
	 * 测试数据: 合法的数据集
	 * 预期结果: 返回预期分页数据
	 */
	@Test
	public void testGetDiscussionThreadsUsingExistsTopicId() {
		List<DiscussionThreads> dts = discussionThreadsMapper.getDiscussionThreadsUsingTopicId(1, 0, 2);
		Assert.assertEquals(2, dts.size());
	}
	
	/**
	 * 测试用例: 测试getDiscussionThreadsUsingTopicId(int, long, int)方法
	 * 测试数据: 不存在的讨论节点的唯一标识符
	 * 预期结果: 分页数据条数为0
	 */
	@Test
	public void testGetDiscussionThreadsUsingNotExistsTopicId() {
		List<DiscussionThreads> dts = discussionThreadsMapper.getDiscussionThreadsUsingTopicId(0, 0, 2);
		Assert.assertEquals(0, dts.size());
	}
	
	/**
	 * 测试用例: 测试createDiscussionThreads(DiscussionThreads)方法
	 * 测试数据: 合法的数据集
	 * 预期结果: 数据插入操作成功完成
	 */
	@Test
	public void testCreateDiscussionThreads() {
		DiscussionThreads dt = new DiscussionThreads(1, 1002, 1000, "Test-Thread", new Date(), "Test-Content");
		int count = discussionThreadsMapper.createDiscussionThreads(dt);
		Assert.assertEquals(1, count);
	}
	
	/**
	 * 测试用例: 测试updateDiscussionThreads(DiscussionThreads)方法
	 * 测试数据: 合法的数据集
	 * 预期结果: 数据更新操作成功完成
	 */
	@Test
	public void testUpdateDiscussionThreads() {
		DiscussionThreads dt = discussionThreadsMapper.getSingleDiscussionThreads(1);
		Assert.assertNotNull(dt);
		
		String name = "Update Threads Name";
		dt.setDiscussionThreadsName(name);
		int count = discussionThreadsMapper.updateDiscussionThreads(dt);
		Assert.assertEquals(1, count);
		
		DiscussionThreads udt = discussionThreadsMapper.getSingleDiscussionThreads(1);
		Assert.assertEquals(name, udt.getDiscussionThreadsName());		
	}

	@Autowired
	private DiscussionThreadsMapper discussionThreadsMapper;
}
