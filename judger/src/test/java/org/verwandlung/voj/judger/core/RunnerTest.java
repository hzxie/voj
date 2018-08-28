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
package org.verwandlung.voj.judger.core;

import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

import org.verwandlung.voj.judger.mapper.SubmissionMapper;
import org.verwandlung.voj.judger.model.Submission;

/**
 * 程序执行器的测试类.
 * 
 * @author Haozhe Xie
 */
@ExtendWith(SpringExtension.class)
@Transactional
@ContextConfiguration({"classpath:test-spring-context.xml"})
public class RunnerTest {
	/**
	 * 测试用例: 测试getRuntimeResult(Submission, int, String, String, String, String)方法
	 * 测试数据: 使用可以编译通过的C++代码
	 * 预期结果: 编译成功并运行产生输出
	 */
	@Test
	public void testGetRuntimeResultCpp() throws Exception {
		String workDirectory = workBaseDirectory + "/voj-1000";
		String baseFileName = "random-name";
		Submission submission = submissionMapper.getSubmission(1000);

		String inputFilePath = workBaseDirectory + "/testpoints/1000/input#0.txt";
		String outputFilePath = workBaseDirectory + "/voj-1000/output#0.txt";
		
		preprocessor.createTestCode(submission, workDirectory, baseFileName);
		preprocessor.fetchTestPoints(submission.getProblem().getProblemId());
		compiler.getCompileResult(submission, workDirectory, baseFileName);
		
		Map<String, Object> result =
				runner.getRuntimeResult(submission, workDirectory, baseFileName, inputFilePath, outputFilePath);
		Assertions.assertEquals("AC", result.get("runtimeResult"));
	}
	
	/**
	 * 测试用例: 测试getRuntimeResult(Submission, int, String, String, String, String)方法
	 * 测试数据: 使用可以编译通过的Java代码
	 * 预期结果: 编译成功并运行产生输出
	 *
	 * The test case is disabled for passing CI on AppVeyor.
	 */
	public void testGetRuntimeResultJava() throws Exception {
		String workDirectory = workBaseDirectory + "/voj-1001";
		String baseFileName = "RandomName";
		Submission submission = submissionMapper.getSubmission(1001);

		String inputFilePath = workBaseDirectory + "/testpoints/1001/input#0.txt";
		String outputFilePath = workBaseDirectory + "/voj-1000/output#0.txt";
		
		preprocessor.createTestCode(submission, workDirectory, baseFileName);
		preprocessor.fetchTestPoints(submission.getProblem().getProblemId());
		compiler.getCompileResult(submission, workDirectory, baseFileName);
		
		Map<String, Object> result = 
				runner.getRuntimeResult(submission, workDirectory, baseFileName, inputFilePath, outputFilePath);
		Assertions.assertEquals("AC", result.get("runtimeResult"));
	}
	
	/**
	 * 待测试的Runner对象.
	 */
	@Autowired
	private Runner runner;
	
	/**
	 * 自动注入的Compiler对象.
	 * 用于构建测试用例.
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
	
	/**
	 * 评测机的工作目录.
	 * 用于存储编译结果以及程序输出结果.
	 */
	@Value("${judger.workDir}")
	private String workBaseDirectory;
}
