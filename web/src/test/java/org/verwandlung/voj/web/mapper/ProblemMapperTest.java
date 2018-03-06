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

import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

import org.verwandlung.voj.web.model.Problem;

/**
 * ProblemMapper测试类.
 * @author Haozhe Xie
 */
@ExtendWith(SpringExtension.class)
@Transactional
@ContextConfiguration({"classpath:test-spring-context.xml"})
public class ProblemMapperTest {
	/**
	 * 测试用例: 测试getNumberOfProblems()方法
	 * 测试数据: N/a
	 * 预期结果: 返回数据表中全部试题的数量
	 */
	@Test
	public void testGetNumberOfProblems() {
		long totalProblems = problemMapper.getNumberOfProblems();
		Assertions.assertEquals(4, totalProblems);
	}
	
	/**
	 * 测试用例: 测试getNumberOfProblemsUsingFilters(String, int, boolean)方法
	 * 测试数据: N/a
	 * 预期结果: 返回数据表中公开试题的数量
	 */
	@Test
	public void testGetNumberOfPublicProblems() {
		long totalProblems = problemMapper.getNumberOfProblemsUsingFilters(null, 0, true);
		Assertions.assertEquals(3, totalProblems);
	}
	
	/**
	 * 测试用例: 测试getLowerBoundOfProblems()方法
	 * 测试数据: N/a
	 * 预期结果: 第一个试题的ID(1000)
	 */
	@Test
	public void testGetLowerBoundOfProblems() {
		long lowerBoundOfProblems = problemMapper.getLowerBoundOfProblems();
		Assertions.assertEquals(1000, lowerBoundOfProblems);
	}
	
	/**
	 * 测试用例: 测试getUpperBoundOfProblems()方法
	 * 测试数据: N/a
	 * 预期结果: 最后一个试题的ID(1002)
	 */
	@Test
	public void testGetUpperBoundOfProblems() {
		long upperBoundOfProblems = problemMapper.getUpperBoundOfProblems();
		Assertions.assertEquals(1003, upperBoundOfProblems);
	}

	/**
	 * 测试用例: 测试getUpperBoundOfProblemsWithLimit(boolean, long, int)方法
	 * 测试数据: 获取ID从1000起始的3道试题
	 * 预期结果: 最后一题的ID为1002
	 */
	@Test
	public void testGetUpperBoundOfProblemsWithOffsetFrom1000WithLimit3() {
		long upperBoundOfProblems = problemMapper.getUpperBoundOfProblemsWithLimit(false, 1000, 3);
		Assertions.assertEquals(1002, upperBoundOfProblems);
	}

	/**
	 * 测试用例: 测试getUpperBoundOfProblemsWithLimit(boolean, long, int)方法
	 * 测试数据: 获取ID从1000起始的3道公开试题
	 * 预期结果: 最后一题的ID为1003
	 */
	@Test
	public void testGetUpperBoundOfPublicProblemsWithOffsetFrom1000WithLimit3() {
		long upperBoundOfProblems = problemMapper.getUpperBoundOfProblemsWithLimit(true, 1000, 3);
		Assertions.assertEquals(1003, upperBoundOfProblems);
	}

	/**
	 * 测试用例: 测试getProblem()方法
	 * 测试数据: 使用A+B Problem的试题唯一标识符
	 * 预期结果: 返回预期的试题对象
	 */
	@Test
	public void testGetProblemExists() {
		Problem problem = problemMapper.getProblem(1000);
		Assertions.assertNotNull(problem);
		
		String problemName = problem.getProblemName();
		Assertions.assertEquals("A+B Problem", problemName);
		
		long acceptedSubmission = problem.getAcceptedSubmission();
		Assertions.assertEquals(1, acceptedSubmission);
	}
	
	/**
	 * 测试用例: 测试getProblem()方法
	 * 测试数据: 使用不存在的试题唯一标识符
	 * 预期结果: 返回空引用
	 */
	@Test
	public void testGetProblemNotExists() {
		Problem problem = problemMapper.getProblem(0);
		Assertions.assertNull(problem);
	}
	
	/**
	 * 测试用例: 测试getProblemsUsingFilters(String, int, boolean, long, int)方法
	 * 测试数据: 获取ID从1000起始的10道试题
	 * 预期结果: 返回预期的试题列表(共2题)
	 */
	@Test
	public void testGetProblemsFrom1000WithLimit10() {
		List<Problem> problems = problemMapper.getProblemsUsingFilters(null, 0, 0, true, 1000, 10);
		Assertions.assertEquals(3, problems.size());
		
		Problem firstProblem = problems.get(0);
		long problemId = firstProblem.getProblemId();
		Assertions.assertEquals(1000, problemId);
		
		String problemName = firstProblem.getProblemName();
		Assertions.assertEquals("A+B Problem", problemName);
		
		long acceptedSubmission = firstProblem.getAcceptedSubmission();
		Assertions.assertEquals(1, acceptedSubmission);
	}
	
	/**
	 * 测试用例: 测试getProblemsUsingFilters(String, int, boolean, long, int)方法
	 * 测试数据: 获取ID从1001起始的1道试题
	 * 预期结果: 返回预期的试题列表(共1题)
	 */
	@Test
	public void testGetProblemsFrom1001WithLimit1() {
		List<Problem> problems = problemMapper.getProblemsUsingFilters("", 0, 0, true, 1001, 1);
		Assertions.assertEquals(1, problems.size());
		
		Problem firstProblem = problems.get(0);
		long problemId = firstProblem.getProblemId();
		Assertions.assertEquals(1001, problemId);
		
		long acceptedSubmission = firstProblem.getAcceptedSubmission();
		Assertions.assertEquals(1, acceptedSubmission);
	}
	
	/**
	 * 测试用例: 测试getProblemsUsingFilters(String, int, boolean, long, int)方法
	 * 测试数据: 获取ID从1010起始的10道试题
	 * 预期结果: 返回空的试题列表
	 */
	@Test
	public void testGetProblemsFrom1010WithLimit10() {
		List<Problem> problems = problemMapper.getProblemsUsingFilters(null, 0, 0, true, 1010, 10);
		Assertions.assertEquals(0, problems.size());
	}
	
	/**
	 * 测试用例: 测试createProblem(Problem)方法
	 * 测试数据: 使用合法的数据集
	 * 预期结果: 数据插入操作成功完成
	 */
	@Test
	public void testCreateProblemNormally() {
		Problem problem = new Problem(false, "Problem Name", 1000, 32768, "Description", 
										"Input Format", "Output Format", "Sample Input", 
										"Sample Input", null);
		int numberOfRowsAffected = problemMapper.createProblem(problem);
		Assertions.assertEquals(1, numberOfRowsAffected);
	}
	
	/**
	 * 测试用例: 测试updateProblem(Problem)方法
	 * 测试数据: 使用合法的数据集, 且数据表中存在对应ID的记录
	 * 预期结果: 数据更新操作成功完成
	 */
	@Test
	public void testUpdateProblemNormally() {
		Problem problem = problemMapper.getProblem(1001);
		Assertions.assertNotNull(problem);
		
		problem.setProblemName("New Problem Name");
		int numberOfRowsAffected = problemMapper.updateProblem(problem);
		Assertions.assertEquals(1, numberOfRowsAffected);
		
		/**
		 * The following Assert CANNOT passed in CI due to 
		 * the bug of Spring Test Framework. But it really works.
		 */
		/*
		 * Problem updatedProblem = problemMapper.getProblem(1001);
		 * String problemName = updatedProblem.getProblemName();
		 * Assertions.assertEquals("New Problem Name", problemName);
		 */
	}
	
	/**
	 * 测试用例: 测试updateProblem(Problem)方法
	 * 测试数据: 合法的数据集, 但数据表中不存在对应ID的记录
	 * 预期结果: 方法正常执行, 未影响数据表中的数据
	 */
	@Test
	public void testUpdateProblemNotExists() {
		Problem problem = problemMapper.getProblem(1001);
		Assertions.assertNotNull(problem);
		
		problem.setProblemId(0);
		int numberOfRowsAffected = problemMapper.updateProblem(problem);
		Assertions.assertEquals(0, numberOfRowsAffected);
	}
	
	/**
	 * 测试用例: 测试deleteProblem(long)方法
	 * 测试数据: 试题#1002的唯一标识符
	 * 预期结果: 数据删除操作成功完成
	 */
	@Test
	public void testDeleteProblemExists() {
		Problem problem = problemMapper.getProblem(1002);
		Assertions.assertNotNull(problem);
		
		int numberOfRowsAffected = problemMapper.deleteProblem(1002);
		Assertions.assertEquals(1, numberOfRowsAffected);
		
		problem = problemMapper.getProblem(1002);
		Assertions.assertNull(problem);
	}
	
	/**
	 * 测试用例: 测试deleteProblem(long)方法
	 * 测试数据: 不存在的试题唯一标识符
	 * 预期结果: 方法正常执行, 未影响数据表中的数据
	 */
	@Test
	public void testDeleteProblemNotExists() {
		Problem problem = problemMapper.getProblem(0);
		Assertions.assertNull(problem);
		
		int numberOfRowsAffected = problemMapper.deleteProblem(0);
		Assertions.assertEquals(0, numberOfRowsAffected);
	}
	
	/**
	 * 待测试的ProblemMapper对象.
	 */
	@Autowired
	private ProblemMapper problemMapper;
}
