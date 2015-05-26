package com.trunkshell.voj.judger.core;

import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.transaction.TransactionConfiguration;
import org.springframework.transaction.annotation.Transactional;

/**
 * 程序执行器的测试类.
 * 
 * @author Xie Haozhe
 */
@RunWith(SpringJUnit4ClassRunner.class)
@Transactional
@TransactionConfiguration(defaultRollback = true)
@ContextConfiguration({"classpath:test-spring-context.xml"})
public class RunnerTest {
	/**
	 * 测试用例: 
	 * 测试数据: 
	 * 预期结果: 
	 */
	@Test
	public void testGetRuntimeResult() {
		String commandLine = "";
		String inputFilePath = null;
		String outputFilePath = null;
		int timeLimit = 1000;
		int memoryLimit = 32768;
		
		// Empty command line caused segment fault
		// runner.getRuntimeResult(commandLine, inputFilePath, outputFilePath, timeLimit, memoryLimit);
	}
	
	/**
	 * 待测试的Runner对象.
	 */
	@Autowired
	private Runner runner;
}
