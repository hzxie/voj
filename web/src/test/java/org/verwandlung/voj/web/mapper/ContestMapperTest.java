package org.verwandlung.voj.web.mapper;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;
import org.verwandlung.voj.web.model.Contest;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * ContestMapper的测试类.
 *
 * @author Haozhe Xie
 */
@RunWith(SpringJUnit4ClassRunner.class)
@Transactional
@ContextConfiguration({"classpath:test-spring-context.xml"})
public class ContestMapperTest {
	/**
	 * 测试用例: 测试getNumberOfContests()方法.
	 * 测试数据: N/a
	 * 预期结果: 返回数据库中考试的总数量(2)
	 */
	@Test
	public void testGetNumberOfContests() {
		long numberOfContests = contestMapper.getNumberOfContests();
		Assert.assertEquals(3, numberOfContests);
	}

	/**
	 * 测试用例: 测试getContests(long, int)方法.
	 * 测试数据: 获取从第1项开始的2个考试
	 * 预期结果: 返回[考试对象2, 考试对象1]形式的List
	 */
	@Test
	public void testGetContestsFrom0WithLimit2() {
		List<Contest> contests = contestMapper.getContests(0, 2);
		Assert.assertEquals(2, contests.size());

		Contest firstContest = contests.get(0);
		String contestName = firstContest.getContestName();
		Assert.assertEquals("Contest Test #3", contestName);
	}

	/**
	 * 测试用例: 测试getContests(long, int)方法.
	 * 测试数据: 获取从第2项开始的1个考试
	 * 预期结果: 返回[考试对象1]形式的List
	 */
	@Test
	public void testGetContestsFrom1WithLimit1() {
		List<Contest> contests = contestMapper.getContests(1, 1);
		Assert.assertEquals(1, contests.size());

		Contest contest = contests.get(0);
		String contestName = contest.getContestName();
		Assert.assertEquals("Contest Test #2", contestName);
	}

	/**
	 * 测试用例: 测试getContests(long, int)方法.
	 * 测试数据: 获取从第4项开始的1个考试
	 * 预期结果: 返回[]形式的List
	 */
	@Test
	public void testGetContestsFrom2WithLimit1() {
		List<Contest> contests = contestMapper.getContests(3, 1);
		Assert.assertEquals(0, contests.size());
	}

	/**
	 * 测试用例: 测试getContestsUsingId(long)方法.
	 * 测试数据: 获取第1个考试对象
	 * 预期结果: 第1个考试对象
	 */
	@Test
	public void testGetContestsUsingIdExists() {
		Contest contest = contestMapper.getContestsUsingId(1);
		Assert.assertNotNull(contest);

		String contestName = contest.getContestName();
		Assert.assertEquals("Contest Test #1", contestName);
	}

	/**
	 * 测试用例: 测试getContestsUsingId(long)方法.
	 * 测试数据: 获取第0个考试对象
	 * 预期结果: 空引用
	 */
	@Test
	public void testGetContestsUsingIdNotExists() {
		Contest contest = contestMapper.getContestsUsingId(0);
		Assert.assertNull(contest);
	}

	/**
	 * 测试用例: 测试createContest(Contest)方法.
	 * 测试数据: 包含正常数据值的Contest对象
	 * 预期结果: 考试被成功创建
	 */
	@Test
	public void testCreateContestNormally() {
		Calendar calendar = Calendar.getInstance();
		calendar.set(2016, Calendar.MAY, 7, 18, 0, 0);
		Date startTime = calendar.getTime();

		calendar.set(2016, Calendar.MAY, 7, 20, 0, 0);
		Date endTime = calendar.getTime();

		Contest contest = new Contest("Contest Test", startTime, endTime, "OI", "[]");
		contestMapper.createContest(contest);
	}

	/**
	 * 测试用例: 测试createContest(Contest)方法.
	 * 测试数据: 包含过长的考试赛制的字符串
	 * 预期结果: 抛出DataIntegrityViolationException异常
	 */
	@Test(expected = org.springframework.dao.DataIntegrityViolationException.class)
	public void testCreateContestUsingTooLongContestMode() {
		Calendar calendar = Calendar.getInstance();
		calendar.set(2016, Calendar.MAY, 7, 18, 0, 0);
		Date startTime = calendar.getTime();

		calendar.set(2016, Calendar.MAY, 7, 20, 0, 0);
		Date endTime = calendar.getTime();

		Contest contest = new Contest("Contest Test", startTime, endTime, "OOOOI", "[1000]");
		contestMapper.createContest(contest);
	}

	/**
	 * 测试用例: 测试updateContest(Contest)方法.
	 * 测试数据: 包含正常数据的Contest对象, 更新考试名称
	 * 预期结果: 考试对象被成功更新
	 */
	@Test
	public void testUpdateContestsNormally() {
		Contest contest = contestMapper.getContestsUsingId(2);
		Assert.assertNotNull(contest);

		System.out.println(contest.getProblems());

		contest.setContestName("New Contest Name");
		contestMapper.updateContest(contest);

		contest = contestMapper.getContestsUsingId(2);
		String contestName = contest.getContestName();
		Assert.assertEquals("New Contest Name", contestName);
	}

	/**
	 * 测试用例: 测试deleteContest(long)方法.
	 * 测试数据: 删除第2个考试对象
	 * 预期结果: 第2个考试对象被成功删除
	 */
	@Test
	public void testDeleteContestsExists() {
		Contest contest = contestMapper.getContestsUsingId(3);
		Assert.assertNotNull(contest);

		contestMapper.deleteContest(3);
		contest = contestMapper.getContestsUsingId(3);
		Assert.assertNull(contest);
	}

	/**
	 * 测试用例: 测试deleteContest(long)方法.
	 * 测试数据: 删除第0个考试对象
	 * 预期结果: 程序正常执行, 没有考试被删除
	 */
	@Test
	public void testDeleteContestsNotExists() {
		Contest contest = contestMapper.getContestsUsingId(0);
		Assert.assertNull(contest);
	}

	/**
	 * 待测试的ContestMapper对象.
	 */
	@Autowired
	private ContestMapper contestMapper;
}
