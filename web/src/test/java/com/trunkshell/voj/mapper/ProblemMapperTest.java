package com.trunkshell.voj.mapper;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.transaction.TransactionConfiguration;
import org.springframework.transaction.annotation.Transactional;

import com.trunkshell.voj.model.Problem;

/**
 * ProblemMapper测试类.
 * @author Xie Haozhe
 */
@RunWith(SpringJUnit4ClassRunner.class)
@Transactional
@TransactionConfiguration(defaultRollback = true)
@ContextConfiguration({"classpath:test-spring-context.xml"})
public class ProblemMapperTest {
	/**
	 * 测试用例: 测试getNumberOfProblems()方法
	 * 测试数据: N/a
	 * 预期结果: 返回数据表中公开试题的数量
	 */
	@Test
	public void testGetNumberOfProblems() {
		long totalProblems = problemMapper.getNumberOfProblems();
		Assert.assertEquals(2, totalProblems);
	}
	
	/**
	 * 测试用例: 测试getProblem()方法
	 * 测试数据: 使用A+B Problem的试题唯一标识符
	 * 预期结果: 返回预期的试题对象
	 */
	@Test
	public void testGetProblemExists() {
		Problem problem = problemMapper.getProblem(1000);
		Assert.assertNotNull(problem);
		
		String problemName = problem.getProblemName();
		Assert.assertEquals("A+B Problem", problemName);
		
		long acceptedSubmission = problem.getAcceptedSubmission();
		Assert.assertEquals(1, acceptedSubmission);
	}
	
	/**
	 * 测试用例: 测试getProblem()方法
	 * 测试数据: 使用不存在的试题唯一标识符
	 * 预期结果: 返回空引用
	 */
	@Test
	public void testGetProblemNotExists() {
		Problem problem = problemMapper.getProblem(0);
		Assert.assertNull(problem);
	}
	
	/**
	 * 测试用例: 测试getProblems(long, int)方法
	 * 测试数据: 获取ID从1000起始的10道试题
	 * 预期结果: 返回预期的试题列表(共2题)
	 */
	@Test
	public void testGetProblemsFrom1000WithLimit10() {
		List<Problem> problems = problemMapper.getProblems(1000, 10);
		Assert.assertEquals(2, problems.size());
		
		Problem firstProblem = problems.get(0);
		long problemId = firstProblem.getProblemId();
		Assert.assertEquals(1000, problemId);
		
		String problemName = firstProblem.getProblemName();
		Assert.assertEquals("A+B Problem", problemName);
		
		long acceptedSubmission = firstProblem.getAcceptedSubmission();
		Assert.assertEquals(1, acceptedSubmission);
	}
	
	/**
	 * 测试用例: 测试getProblems(long, int)方法
	 * 测试数据: 获取ID从1001起始的1道试题
	 * 预期结果: 返回预期的试题列表(共1题)
	 */
	@Test
	public void testGetProblemsFrom1001WithLimit1() {
		List<Problem> problems = problemMapper.getProblems(1001, 1);
		Assert.assertEquals(1, problems.size());
		
		Problem firstProblem = problems.get(0);
		long problemId = firstProblem.getProblemId();
		Assert.assertEquals(1001, problemId);
		
		long acceptedSubmission = firstProblem.getAcceptedSubmission();
		Assert.assertEquals(1, acceptedSubmission);
	}
	
	/**
	 * 测试用例: 测试getProblems(long, int)方法
	 * 测试数据: 获取ID从1010起始的10道试题
	 * 预期结果: 返回空的试题列表
	 */
	@Test
	public void testGetProblemsFrom1010WithLimit10() {
		List<Problem> problems = problemMapper.getProblems(1010, 10);
		Assert.assertEquals(0, problems.size());
	}
	
	/**
	 * 待测试的ProblemMapper对象.
	 */
	@Autowired
	private ProblemMapper problemMapper;
}
