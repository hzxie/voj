package com.trunkshell.voj.judger.core;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.transaction.TransactionConfiguration;
import org.springframework.transaction.annotation.Transactional;

import com.trunkshell.voj.judger.exception.IllgealSubmissionException;

/**
 * 评测机调度器的测试类.
 * 
 * @author Xie Haozhe
 */
@RunWith(SpringJUnit4ClassRunner.class)
@Transactional
@TransactionConfiguration(defaultRollback = true)
@ContextConfiguration({"classpath:test-spring-context.xml"})
public class DispatcherTest {
	/**
	 * 测试用例: 测试createNewTask(long)函数
	 * 测试数据: 正常的A+B Problem测试任务
	 * 预期结果: 评测机正常执行, 并返回AC
	 * @throws IllgealSubmissionException 
	 */
	@Test
	public void testCreateNewTaskNormal() throws IllgealSubmissionException {
		// long submissionId = 1000;
		// judgerDispatcher.createNewTask(submissionId);
	}
	
	/**
	 * 待测试的Dispatcher对象.
	 */
	@Autowired
	private Dispatcher judgerDispatcher;
}
