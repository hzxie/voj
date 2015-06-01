package com.trunkshell.voj.judger.core;

import java.util.Map;

import org.junit.Assert;
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
	/**
	 * 测试用例: 测试getCompileResult()方法
	 * 测试数据: 使用可以编译通过的C++代码
	 * 预期结果: 编译通过
	 */
	@Test
	public void testGetCompileResultCppWithSuccess() throws Exception {
		String workDirectory = "/tmp/voj-1000/";
		String baseFileName = "random-name";
		Submission submission = submissionMapper.getSubmission(1000);
		preprocessor.createTestCode(submission, workDirectory, baseFileName);
		
		Map<String, Object> result = compiler.getCompileResult(submission, workDirectory, baseFileName);
		Assert.assertEquals(true, result.get("isSuccessful"));
	}
	
	/**
	 * 测试用例: 测试getCompileResult()方法
	 * 测试数据: 使用可以编译通过的Java代码
	 * 预期结果: 编译通过
	 */
	@Test
	public void testGetCompileResultJavaWithSuccess() throws Exception {
		String workDirectory = "/tmp/voj-1001/";
		String baseFileName = "RandomName";
		Submission submission = submissionMapper.getSubmission(1001);
		preprocessor.createTestCode(submission, workDirectory, baseFileName);
		
		Map<String, Object> result = compiler.getCompileResult(submission, workDirectory, baseFileName);
		Assert.assertEquals(true, result.get("isSuccessful"));
	}
	
	/**
	 * 测试用例: 测试getCompileResult()方法
	 * 测试数据: 使用可以无法编译通过的C++代码
	 * 预期结果: 编译失败
	 */
	@Test
	public void testGetCompileResultCppWithError() throws Exception {
		String workDirectory = "/tmp/voj-1002/";
		String baseFileName = "random-name";
		Submission submission = submissionMapper.getSubmission(1002);
		preprocessor.createTestCode(submission, workDirectory, baseFileName);
		
		Map<String, Object> result = compiler.getCompileResult(submission, workDirectory, baseFileName);
		Assert.assertEquals(false, result.get("isSuccessful"));
	}
	
	/**
	 * 待测试的Compiler对象.
	 */
	@Autowired
	private Compiler compiler;
	
	/**
	 * 自动注入的Preprocessor对象.
	 * 用于构建测试用例.
	 */
	@Autowired
	private Preprocessor preprocessor;
	
	/**
	 * 自动注入的SubmissionMapper对象.
	 * 用于构建测试用例.
	 */
	@Autowired
	private SubmissionMapper submissionMapper;
}
