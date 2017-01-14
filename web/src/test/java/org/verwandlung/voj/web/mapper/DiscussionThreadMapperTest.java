package org.verwandlung.voj.web.mapper;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;
import org.verwandlung.voj.web.model.DiscussionThread;
import org.verwandlung.voj.web.model.DiscussionTopic;
import org.verwandlung.voj.web.model.Problem;
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
public class DiscussionThreadMapperTest {
	/**
	 * 测试用例: 测试getDiscussionThreads(long, int, long, int)方法
	 * 测试数据: Problem: A+B Problem, DiscussionTopic: null, 获取除前2条数据
	 * 预期结果: 返回最新创建的2个DiscussionThread
	 */
	@Test
	public void testGetDiscussionThreadsOfProblem1000WithOffsetFrom0WithLimit2() {
		List<DiscussionThread> discussionThreads = discussionThreadMapper.getDiscussionThreads(1000, 0, 0, 2);
		Assert.assertEquals(2, discussionThreads.size());

		DiscussionThread firstThread = discussionThreads.get(0);
		Assert.assertNotNull(firstThread);

		long problemId = firstThread.getProblem().getProblemId();
		Assert.assertEquals(1000, problemId);

		String firstThreadTitle = firstThread.getDiscussionThreadTitle();
		Assert.assertEquals("Thread #2", firstThreadTitle);

		long threadReplies = firstThread.getNumberOfReplies();
		Assert.assertEquals(2, threadReplies);
	}

	/**
	 * 测试用例: 测试getDiscussionThreads(long, int, long, int)方法
	 * 测试数据: Problem: null, DiscussionTopic: General, 获取除第1条数据
	 * 预期结果: 返回最新创建的1个DiscussionThread
	 */
	@Test
	public void testGetDiscussionThreadsOfNoRelatedProblemWithOffsetFrom0WithLimit1() {
		List<DiscussionThread> discussionThreads = discussionThreadMapper.getDiscussionThreads(0, 1, 0, 1);
		DiscussionThread thread = discussionThreads.get(0);
		Assert.assertNotNull(thread);

		String threadTitle = thread.getDiscussionThreadTitle();
		Assert.assertEquals("Thread #1", threadTitle);
	}

	/**
	 * 测试用例: 测试getSolutionThreadOfProblem(long)方法.
	 * 测试数据: Problem: A+B Problem
	 * 预期结果: 返回Problem A+B Problem相关的第一条讨论
	 */
	@Test
	public void testGetSolutionThreadOfProblemUsingExistingProblemId() {
		DiscussionThread thread = discussionThreadMapper.getSolutionThreadOfProblem(1000);
		String threadTitle = thread.getDiscussionThreadTitle();
		Assert.assertEquals("Thread #1", threadTitle);
	}

	/**
	 * 测试用例: getDiscussionThreadUsingThreadId(long)方法
	 * 测试数据: 存在的讨论帖子唯一标识符
	 * 预期结果: 标题为"Thread #2"的DiscussionThread对象
	 */
	@Test
	public void testGetDiscussionThreadUsingThreadIdExists() {
		DiscussionThread thread = discussionThreadMapper.getDiscussionThreadUsingThreadId(2);
		Assert.assertNotNull(thread);

		String threadTitle = thread.getDiscussionThreadTitle();
		Assert.assertEquals("Thread #2", threadTitle);

		long numberOfReplies = thread.getNumberOfReplies();
		Assert.assertEquals(2, numberOfReplies);
	}

	/**
	 * 测试用例: getDiscussionThreadUsingThreadId(long)方法
	 * 测试数据: 不存在的讨论帖子唯一标识符
	 * 预期结果: 空的DiscussionThread引用
	 */
	@Test
	public void testGetDiscussionThreadUsingThreadIdNotExists() {
		DiscussionThread thread = discussionThreadMapper.getDiscussionThreadUsingThreadId(0);
		Assert.assertNull(thread);
	}

	/**
	 * 测试用例: 测试createDiscussionThread(DiscussionThread)方法
	 * 测试数据: 使用满足条件的数据集 (Problem: A+B Problem)
	 * 预期结果: 数据插入操作成功完成
	 */
	@Test
	public void testCreateDiscussionThreadNormally() {
		User creator = userMapper.getUserUsingUid(1000);
		DiscussionTopic topic = discussionTopicMapper.getDiscussionTopicUsingId(1);
		Problem problem = problemMapper.getProblem(1000);

		Assert.assertNotNull(creator);
		Assert.assertNotNull(problem);

		DiscussionThread thread = new DiscussionThread(creator, topic, problem, "title");
		int numberOfRowsAffected = discussionThreadMapper.createDiscussionThread(thread);
		Assert.assertEquals(1, numberOfRowsAffected);
	}

	/**
	 * 测试用例: 测试createDiscussionThread(DiscussionThread)方法
	 * 测试数据: 使用满足条件的数据集 (Problem: null)
	 * 预期结果: 数据插入操作成功完成
	 */
	@Test
	public void testCreateDiscussionThreadWithNullProblem() {
		User creator = userMapper.getUserUsingUid(1000);
		DiscussionTopic topic = discussionTopicMapper.getDiscussionTopicUsingId(2);
		Assert.assertNotNull(creator);

		DiscussionThread thread = new DiscussionThread(creator, topic, null, "title");
		int numberOfRowsAffected = discussionThreadMapper.createDiscussionThread(thread);
		Assert.assertEquals(1, numberOfRowsAffected);
	}

	/**
	 * 测试用例: 测试createDiscussionThread(DiscussionThread)方法
	 * 测试数据: 使用过长的标题
	 * 预期结果: 抛出DataIntegrityViolationException异常
	 */
	@Test(expected = org.springframework.dao.DataIntegrityViolationException.class)
	public void testCreateDiscussionThreadWithTooLongTitle() {
		User creator = userMapper.getUserUsingUid(1000);
		DiscussionTopic topic = discussionTopicMapper.getDiscussionTopicUsingId(1);
		Assert.assertNotNull(creator);

		StringBuilder sb = new StringBuilder();
		for ( int i = 0; i < 31; ++ i ) {
			sb.append("Very");
		}
		DiscussionThread thread = new DiscussionThread(creator, topic, null, sb.toString() + "LongTitle");
		discussionThreadMapper.createDiscussionThread(thread);
	}

	/**
	 * 测试用例: 测试createDiscussionThread(DiscussionThread)方法
	 * 测试数据: 使用不存在的用户
	 * 预期结果: 抛出DataIntegrityViolationException异常
	 */
	@Test(expected = org.springframework.dao.DataIntegrityViolationException.class)
	public void testCreateDiscussionThreadWithNotExistingUser() {
		User creator = new User(); creator.setUid(0);
		DiscussionTopic topic = discussionTopicMapper.getDiscussionTopicUsingId(1);

		DiscussionThread thread = new DiscussionThread(creator, topic, null, "title");
		discussionThreadMapper.createDiscussionThread(thread);
	}

	/**
	 * 测试用例: 测试createDiscussionThread(DiscussionThread)方法
	 * 测试数据: 使用不存在的讨论话题
 	 * 预期结果: 抛出DataIntegrityViolationException异常
	 */
	@Test(expected = org.springframework.dao.DataIntegrityViolationException.class)
	public void testCreateDiscussionThreadWithNotExistingThreadTopic() {
		User creator = userMapper.getUserUsingUid(1000);
		Date createTime = new Date();
		DiscussionTopic topic = new DiscussionTopic(); topic.setDiscussionTopicId(0);

		DiscussionThread thread = new DiscussionThread(creator, topic, null, "title");
		discussionThreadMapper.createDiscussionThread(thread);
	}

	/**
	 * 测试用例: 测试createDiscussionThread(DiscussionThread)方法
	 * 测试数据: 使用正常的数据更新DiscussionThread对象
	 * 预期结果: 数据更新操作成功完成
	 */
	@Test
	public void testUpdateDiscussionThreadNormally() {
		DiscussionThread thread = discussionThreadMapper.getDiscussionThreadUsingThreadId(1);
		Assert.assertNotNull(thread);
		Assert.assertEquals("Thread #1", thread.getDiscussionThreadTitle());

		thread.setDiscussionThreadTitle("New Thread Title");
		int numberOfRowsAffected = discussionThreadMapper.updateDiscussionThread(thread);
		Assert.assertEquals(1, numberOfRowsAffected);

		thread = discussionThreadMapper.getDiscussionThreadUsingThreadId(1);
		Assert.assertNotNull(thread);
		Assert.assertEquals("New Thread Title", thread.getDiscussionThreadTitle());
	}

	/**
	 * 测试用例: 测试createDiscussionThread(DiscussionThread)方法
	 * 测试数据: 使用过长的标题
	 * 预期结果: 抛出DataIntegrityViolationException异常
	 */
	@Test(expected = org.springframework.dao.DataIntegrityViolationException.class)
	public void testUpdateDiscussionThreadWithTooLongTitle() {
		DiscussionThread thread = discussionThreadMapper.getDiscussionThreadUsingThreadId(1);
		Assert.assertNotNull(thread);
		Assert.assertEquals("Thread #1", thread.getDiscussionThreadTitle());

		StringBuilder sb = new StringBuilder();
		for ( int i = 0; i < 31; ++ i ) {
			sb.append("Very");
		}
		thread.setDiscussionThreadTitle(sb.toString() + "Long Title");
		discussionThreadMapper.updateDiscussionThread(thread);
	}

	/**
	 * 测试用例: 测试getDiscussionThreadUsingThreadId(long)方法
	 * 测试数据: 存在的讨论帖子唯一标识符
	 * 预期结果: 数据删除操作成功完成
	 */
	@Test
	public void testDeleteDiscussionThreadUsingExistingThreadId() {
		DiscussionThread thread = discussionThreadMapper.getDiscussionThreadUsingThreadId(1);
		Assert.assertNotNull(thread);

		int numberOfRowsAffected = discussionThreadMapper.deleteDiscussionThreadUsingThreadId(1);
		Assert.assertEquals(1, numberOfRowsAffected);

		thread = discussionThreadMapper.getDiscussionThreadUsingThreadId(1);
		Assert.assertNull(thread);
	}

	/**
	 * 测试用例: 测试getDiscussionThreadUsingThreadId(long)方法
	 * 测试数据: 不存在的讨论帖子唯一标识符
	 * 预期结果: 方法正常执行, 未影响数据表中的数据
	 */
	@Test
	public void testDeleteDiscussionThreadUsingNotExistingThreadId() {
		DiscussionThread thread = discussionThreadMapper.getDiscussionThreadUsingThreadId(0);
		Assert.assertNull(thread);

		int numberOfRowsAffected = discussionThreadMapper.deleteDiscussionThreadUsingThreadId(0);
		Assert.assertEquals(0, numberOfRowsAffected);
	}

	/**
	 * 待测试的DiscussionThreadMapper对象.
	 */
	@Autowired
	private DiscussionThreadMapper discussionThreadMapper;

	/**
	 * 用于构建测试用例的UserMapper对象.
	 */
	@Autowired
	private UserMapper userMapper;

	/**
	 * 用于构建测试用例的DiscussionTopicMapper对象.
	 */
	@Autowired
	private DiscussionTopicMapper discussionTopicMapper;

	/**
	 * 用于构建测试用例的ProblemMapper对象.
	 */
	@Autowired
	private ProblemMapper problemMapper;
}
