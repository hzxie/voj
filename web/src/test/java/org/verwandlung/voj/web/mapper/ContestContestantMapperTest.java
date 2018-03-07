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
import org.verwandlung.voj.web.model.Contest;
import org.verwandlung.voj.web.model.ContestContestant;
import org.verwandlung.voj.web.model.User;

import java.util.List;

/**
 * ContestMapper的测试类.
 *
 * @author Haozhe Xie
 */
@ExtendWith(SpringExtension.class)
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
		Assertions.assertEquals(2, numberOfContestants);
	}

	/**
	 * 测试用例: 测试getNumberOfContestantsOfContest(long)方法.
	 * 测试数据: 使用不存在的考试唯一标识符.
	 * 预期结果: 返回0
	 */
	@Test
	public void testGetNumberOfContestantsOfContestUsingNotExistingContest() {
		long numberOfContestants = contestContestantMapper.getNumberOfContestantsOfContest(0);
		Assertions.assertEquals(0, numberOfContestants);
	}

	/**
	 * 测试用例: 测试getContestantOfContest(long, long)方法.
	 * 测试数据: ContestID = 1, ContestantUid = 1000
	 * 预期结果: 返回UID = 1000的用户在第1场考试的ContestContestant对象.
	 */
	@Test
	public void testGetContestantOfContestExists() {
		ContestContestant cc = contestContestantMapper.getContestantOfContest(1, 1000);
		Assertions.assertNotNull(cc);

		String usernameOfFirstContestant = cc.getContestant().getUsername();
		Assertions.assertEquals("zjhzxhz", usernameOfFirstContestant);
	}

	/**
	 * 测试用例: 测试getContestantsOfContestForOi(long, long ,int)方法.
	 * 测试数据: ContestID = 1, offset = 0, limit = 2
	 * 预期结果: 返回第1场考试的从0开始的2个参赛者.
	 */
	@Test
	public void testGetContestantsOfFirstContestForOiFrom0WithLimit2() {
		List<ContestContestant> contestants = contestContestantMapper.getContestantsOfContestForOi(1, 0, 2);
		Assertions.assertEquals(2, contestants.size());

		ContestContestant firstContestant = contestants.get(0);
		String usernameOfFirstContestant = firstContestant.getContestant().getUsername();
		Assertions.assertEquals("zjhzxhz", usernameOfFirstContestant);

		int score = firstContestant.getScore();
		Assertions.assertEquals(200, score);
		long time = firstContestant.getTime();
		Assertions.assertEquals(60, time);
	}

	/**
	 * 测试用例: 测试getContestantsOfContestForOi(long, long ,int)方法.
	 * 测试数据: ContestID = 2, offset = 0, limit = 1
	 * 预期结果: 返回第2场考试的从0开始的1个参赛者.
	 */
	@Test
	public void testGetContestantsOfSecondContestForOiFrom1WithLimit1() {
		List<ContestContestant> contestants = contestContestantMapper.getContestantsOfContestForOi(2, 0, 1);
		Assertions.assertEquals(1, contestants.size());

		ContestContestant firstContestant = contestants.get(0);
		String usernameOfFirstContestant = firstContestant.getContestant().getUsername();
		Assertions.assertEquals("zjhzxhz", usernameOfFirstContestant);
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
		Assertions.assertEquals(1, numberOfRowsAffected);
	}

	/**
	 * 测试用例: 测试createContestContestant(Contest, User)方法.
	 * 测试数据: 使用合法的数据, 但数据库中存在相同的记录.
	 * 预期结果: 抛出DuplicateKeyException异常
	 */
	@Test
	public void testCreateContestContestantWithExistingRecord() {
		Contest contest = contestMapper.getContest(1);
		User contestant = userMapper.getUserUsingUid(1001);

		ContestContestant cc = new ContestContestant(contest, contestant);
		Executable e = () -> {
			contestContestantMapper.createContestContestant(cc);
		};
		Assertions.assertThrows(org.springframework.dao.DuplicateKeyException.class, e);
	}

	/**
	 * 测试用例: 测试createContestContestant(Contest, User)方法.
	 * 测试数据: 使用不存在的考试ID
	 * 预期结果: DataIntegrityViolationException
	 */
	@Test
	public void testCreateContestContestantWithContestContestantNotExistingContest() {
		Contest contest = contestMapper.getContest(0);
		User contestant = userMapper.getUserUsingUid(1001);

		ContestContestant cc = new ContestContestant(contest, contestant);
		Executable e = () -> {
			contestContestantMapper.createContestContestant(cc);
		};
		Assertions.assertThrows(org.springframework.dao.DataIntegrityViolationException.class, e);
	}

	/**
	 * 测试用例: 测试createContestContestant(Contest, User)方法.
	 * 测试数据: 使用不存在的用户ID
	 * 预期结果: DataIntegrityViolationException
	 */
	@Test
	public void testCreateContestContestantWithNotExistingContestant() {
		Contest contest = contestMapper.getContest(1);
		User contestant = userMapper.getUserUsingUid(0);

		ContestContestant cc = new ContestContestant(contest, contestant);
		Executable e = () -> {
			contestContestantMapper.createContestContestant(cc);
		};
		Assertions.assertThrows(org.springframework.dao.DataIntegrityViolationException.class, e);
	}

	/**
	 * 测试用例: 测试updateContestContestant(ContestContestant).
	 * 测试数据: 使用合法的数据集.
	 * 预期结果: ContestContestant被成功更新.
	 */
	@Test
	public void testUpdateContestContestantNormally() {
		ContestContestant cc = contestContestantMapper.getContestantOfContest(1, 1001);
		Assertions.assertNotNull(cc);
		cc.setCodeSnippet("Test Code Snippet");

		contestContestantMapper.updateContestContestant(cc);
		cc = contestContestantMapper.getContestantOfContest(1, 1001);
		String codeSnippet = cc.getCodeSnippet();
		Assertions.assertEquals("Test Code Snippet", codeSnippet);
	}

	/**
	 * 测试用例: deleteContestContestant(long, long)
	 * 测试数据: ContestID = 2, UserID = 1000
	 * 预期结果: ContestContestant被成功删除
	 */
	@Test
	public void testDeleteContestContestantExists() {
		ContestContestant cc = contestContestantMapper.getContestantOfContest(2, 1000);
		Assertions.assertNotNull(cc);

		int numberOfRowsAffected = contestContestantMapper.deleteContestContestant(2, 1000);
		Assertions.assertEquals(1, numberOfRowsAffected);
		cc = contestContestantMapper.getContestantOfContest(2, 1000);
		Assertions.assertNull(cc);
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
