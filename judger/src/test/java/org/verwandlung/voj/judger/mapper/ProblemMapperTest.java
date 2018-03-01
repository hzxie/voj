package org.verwandlung.voj.judger.mapper;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

import org.verwandlung.voj.judger.model.Problem;

/**
 * ProblemMapper测试类.
 * @author Haozhe Xie
 */
@ExtendWith(SpringExtension.class)
@Transactional
@ContextConfiguration({"classpath:test-spring-context.xml"})
public class ProblemMapperTest {
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
	 * 待测试的ProblemMapper对象.
	 */
	@Autowired
	private ProblemMapper problemMapper;
}
