package org.verwandlung.voj.web.mapper;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;
import org.verwandlung.voj.web.model.Contest;
import org.verwandlung.voj.web.model.ContestContestant;
import org.verwandlung.voj.web.model.User;

import java.util.List;

/**
 * ContestMapper的测试类.
 *
 * @author Haozhe Xie
 */
@RunWith(SpringJUnit4ClassRunner.class)
@Transactional
@ContextConfiguration({"classpath:test-spring-context.xml"})
public class ContestContestantMapperTest {
	/**
	 * 测试用例: 测试getNumberOfContestantsOfContest(long)方法.
	 * 测试数据: 使用存在的考试唯一标识符.
	 * 预期结果: 返回对应的考试参加的人数.
	 */
	@Test
	public void testGetNumberOfContestantsOfContestUsingExistingContest() {
		long numberOfContestants = contestContestantMapper.getNumberOfContestantsOfContest(1);
		Assert.assertEquals(2, numberOfContestants);
	}

	/**
	 * 测试用例: 测试getNumberOfContestantsOfContest(long)方法.
	 * 测试数据: 使用不存在的考试唯一标识符.
	 * 预期结果: 返回0
	 */
	@Test
	public void testGetNumberOfContestantsOfContestUsingNotExistingContest() {
		long numberOfContestants = contestContestantMapper.getNumberOfContestantsOfContest(0);
		Assert.assertEquals(0, numberOfContestants);
	}

	/**
	 * 测试用例: 测试getContestantOfContest(long, long)方法.
	 * 测试数据: ContestID = 1, ContestantUid = 1000
	 * 预期结果: 返回UID = 1000的用户在第1场考试的ContestContestant对象.
	 */
	@Test
	public void testGetContestantOfContestExists() {
		ContestContestant cc = contestContestantMapper.getContestantOfContest(1, 1000);
		Assert.assertNotNull(cc);

		String usernameOfFirstContestant = cc.getContestant().getUsername();
		Assert.assertEquals("zjhzxhz", usernameOfFirstContestant);

		int score = cc.getScore();
		Assert.assertEquals(100, score);
		int time = cc.getTime();
		Assert.assertEquals(30, time);
	}

	/**
	 * 测试用例: 测试getContestantsOfContestForOi(long, long ,int)方法.
	 * 测试数据: ContestID = 1, offset = 0, limit = 2
	 * 预期结果: 返回第1场考试的从0开始的2个参赛者.
	 */
	@Test
	public void testGetContestantsOfFirstContestForOiFrom0WithLimit2() {
		List<ContestContestant> contestants = contestContestantMapper.getContestantsOfContestForOi(1, 0, 2);
		Assert.assertEquals(2, contestants.size());

		ContestContestant firstContestant = contestants.get(0);
		String usernameOfFirstContestant = firstContestant.getContestant().getUsername();
		Assert.assertEquals("zjhzxhz", usernameOfFirstContestant);

		int score = firstContestant.getScore();
		Assert.assertEquals(110, score);
		int time = firstContestant.getTime();
		Assert.assertEquals(60, time);
	}

	/**
	 * 测试用例: 测试getContestantsOfContestForOi(long, long ,int)方法.
	 * 测试数据: ContestID = 2, offset = 0, limit = 1
	 * 预期结果: 返回第2场考试的从0开始的1个参赛者.
	 */
	@Test
	public void testGetContestantsOfSecondContestForOiFrom1WithLimit1() {
		List<ContestContestant> contestants = contestContestantMapper.getContestantsOfContestForOi(2, 0, 1);
		Assert.assertEquals(1, contestants.size());

		ContestContestant firstContestant = contestants.get(0);
		String usernameOfFirstContestant = firstContestant.getContestant().getUsername();
		Assert.assertEquals("zjhzxhz", usernameOfFirstContestant);
	}

	/**
	 * 测试用例: 测试createContestContestant(Contest, User)方法.
	 * 测试数据: 使用合法的数据, 并且数据库中不存在相同的记录.
	 * 预期结果: 参赛记录被成功创建.
	 */
	@Test
	public void testCreateContestContestantNormally() {
		Contest contest = contestMapper.getContest(2);
		User contestant = userMapper.getUserUsingUid(1002);

		ContestContestant cc = new ContestContestant(contest, contestant);
		int numberOfRowsAffected = contestContestantMapper.createContestContestant(cc);
		Assert.assertEquals(1, numberOfRowsAffected);
	}

	/**
	 * 测试用例: 测试createContestContestant(Contest, User)方法.
	 * 测试数据: 使用合法的数据, 但数据库中存在相同的记录.
	 * 预期结果: 抛出DuplicateKeyException异常
	 */
	@Test(expected = org.springframework.dao.DuplicateKeyException.class)
	public void testCreateContestContestantWithExistingRecord() {
		Contest contest = contestMapper.getContest(1);
		User contestant = userMapper.getUserUsingUid(1001);

		ContestContestant cc = new ContestContestant(contest, contestant);
		contestContestantMapper.createContestContestant(cc);
	}

	/**
	 * 测试用例: 测试createContestContestant(Contest, User)方法.
	 * 测试数据: 使用不存在的考试ID
	 * 预期结果: DataIntegrityViolationException
	 */
	@Test(expected = org.springframework.dao.DataIntegrityViolationException.class)
	public void testCreateContestContestantWithContestContestant被成功更新NotExistingContest() {
		Contest contest = contestMapper.getContest(0);
		User contestant = userMapper.getUserUsingUid(1001);

		ContestContestant cc = new ContestContestant(contest, contestant);
		contestContestantMapper.createContestContestant(cc);
	}

	/**
	 * 测试用例: 测试createContestContestant(Contest, User)方法.
	 * 测试数据: 使用不存在的用户ID
	 * 预期结果: DataIntegrityViolationException
	 */
	@Test(expected = org.springframework.dao.DataIntegrityViolationException.class)
	public void testCreateContestContestantWithNotExistingContestant() {
		Contest contest = contestMapper.getContest(1);
		User contestant = userMapper.getUserUsingUid(0);

		ContestContestant cc = new ContestContestant(contest, contestant);
		contestContestantMapper.createContestContestant(cc);
	}

	/**
	 * 测试用例: 测试updateContestContestant(ContestContestant).
	 * 测试数据: 使用合法的数据集.
	 * 预期结果: ContestContestant被成功更新.
	 */
	@Test
	public void testUpdateContestContestantNormally() {
		ContestContestant cc = contestContestantMapper.getContestantOfContest(1, 1001);
		Assert.assertNotNull(cc);
		cc.setCodeSnippet("Test Code Snippet");

		contestContestantMapper.updateContestContestant(cc);
		cc = contestContestantMapper.getContestantOfContest(1, 1001);
		String codeSnippet = cc.getCodeSnippet();
		Assert.assertEquals("Test Code Snippet", codeSnippet);
	}

	/**
	 * 测试用例: deleteContestContestant(long, long)
	 * 测试数据: ContestID = 2, UserID = 1000
	 * 预期结果: ContestContestant被成功删除
	 */
	@Test
	public void testDeleteContestContestantExists() {
		ContestContestant cc = contestContestantMapper.getContestantOfContest(2, 1000);
		Assert.assertNotNull(cc);

		int numberOfRowsAffected = contestContestantMapper.deleteContestContestant(2, 1000);
		Assert.assertEquals(1, numberOfRowsAffected);
		cc = contestContestantMapper.getContestantOfContest(2, 1000);
		Assert.assertNull(cc);
	}

	/**
	 * 待测试的ContestContestantMapper对象.
	 */
	@Autowired
	private ContestContestantMapper contestContestantMapper;

	/**
	 * 自动注入的ContestMapper对象.
	 * 用于构建测试用例.
	 */
	@Autowired
	private ContestMapper contestMapper;

	/**
	 * 自动注入的UserMapper对象.
	 * 用于构建测试用例.
	 */
	@Autowired
	private UserMapper userMapper;
}
