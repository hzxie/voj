package com.trunkshell.voj.judger.core;

import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.transaction.TransactionConfiguration;
import org.springframework.transaction.annotation.Transactional;

import com.trunkshell.voj.judger.mapper.SubmissionMapper;
import com.trunkshell.voj.judger.model.Submission;

/**
 * 程序编译器的测试类.
 * 
 * @author Xie Haozhe
 */
@RunWith(SpringJUnit4ClassRunner.class)
@Transactional
@TransactionConfiguration(defaultRollback = true)
@ContextConfiguration({"classpath:test-spring-context.xml"})
public class CompilerTest {
	@Test
	public void testGetCompileResult() {
		String workDirectory = "/tmp/1000/";
		String baseFileName = "random-name";
		Submission submission = submissionMapper.getSubmission(1000);
		
		// Map<String, Object> result = compiler.getCompileResult(submission, workDirectory, baseFileName);
		// System.out.println(result.get("timeUsage"));
		// System.out.println(result.get("memoryUsage"));
		// System.out.println(result.get("exitCode"));
	}
	
	/**
	 * 待测试的Compiler对象.
	 */
	@Autowired
	private Compiler compiler;
	
	/**
	 * 自动注入的SubmissionMapper对象.
	 * 用于构建测试用例.
	 */
	@Autowired
	private SubmissionMapper submissionMapper;
}
