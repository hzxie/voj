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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.verwandlung.voj.judger.model.Checkpoint;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import org.verwandlung.voj.judger.application.ApplicationDispatcher;
import org.verwandlung.voj.judger.exception.IllgealSubmissionException;
import org.verwandlung.voj.judger.mapper.CheckpointMapper;
import org.verwandlung.voj.judger.mapper.SubmissionMapper;
import org.verwandlung.voj.judger.model.Submission;
import org.verwandlung.voj.judger.util.DigestUtils;

/**
 * 评测机调度器.
 * 用于完成评测机的评测流程.
 * 每个阶段结束后推送消息至消息队列; 评测结束后写入数据库.
 *
 * @author Haozhe Xie
 */
@Component
public class Dispatcher {
	/**
	 * 创建新的评测任务.
	 * 每次只运行一个评测任务.
	 * @param submissionId - 提交记录的唯一标识符
	 * @throws IllgealSubmissionException
	 * @throws InterruptedException 
	 */
	public void createNewTask(long submissionId) throws IllgealSubmissionException, InterruptedException {
		synchronized(this) {
			String baseDirectory = String.format("%s/voj-%s", new Object[] {workBaseDirectory, submissionId});
			String baseFileName = DigestUtils.getRandomString(12, DigestUtils.Mode.ALPHA);
			
			// 解决由于未知原因无法获取到数据记录的问题
			int tryTimes = 0;
			Submission submission = null;
			do {
				Thread.sleep(1000);
				submission = submissionMapper.getSubmission(submissionId);
			} while ( submission == null && ++ tryTimes <= 3 );
			
			if ( submission == null ) {
				throw new IllgealSubmissionException(
						String.format("Illegal submission #%s",
								new Object[] { submissionId }));
			}
			preprocess(submission, baseDirectory, baseFileName);
			if ( compile(submission, baseDirectory, baseFileName) ) {
				runProgram(submission, baseDirectory, baseFileName);
			}
			cleanUp(baseDirectory);
		}
	}

	/**
	 * 完成评测前的预处理工作.
	 * 说明: 随机文件名用于防止应用程序自身递归调用.
	 *
	 * @param submission - 评测记录对象
	 * @param workDirectory - 用于产生编译输出的目录
	 * @param baseFileName - 随机文件名(不包含后缀)
	 */
	private void preprocess(Submission submission,
							String workDirectory, String baseFileName) {
		try {
			long problemId = submission.getProblem().getProblemId();
			preprocessor.createTestCode(submission, workDirectory, baseFileName);
			preprocessor.fetchTestPoints(problemId);
		} catch (Exception ex) {
			LOGGER.catching(ex);

			long submissionId = submission.getSubmissionId();
			applicationDispatcher.onErrorOccurred(submissionId);
		}
	}

	/**
	 * 创建编译任务.
	 * 说明: 随机文件名用于防止应用程序自身递归调用.
	 *
	 * @param submission - 评测记录对象
	 * @param workDirectory - 用于产生编译输出的目录
	 * @param baseFileName - 随机文件名(不包含后缀)
	 */
	private boolean compile(Submission submission,
							String workDirectory, String baseFileName) {
		long submissionId = submission.getSubmissionId();
		Map<String, Object> result =
				compiler.getCompileResult(submission, workDirectory, baseFileName);

		applicationDispatcher.onCompileFinished(submissionId, result);
		return (Boolean)result.get("isSuccessful");
	}

	/**
	 * 执行程序.
	 * @param submission - 评测记录对象
	 * @param workDirectory - 编译生成结果的目录以及程序输出的目录
	 * @param baseFileName - 待执行的应用程序文件名(不包含文件后缀)
	 */
	private void runProgram(Submission submission,
							String workDirectory, String baseFileName) {
		List<Map<String, Object>> runtimeResults = new ArrayList<Map<String, Object>>();
		long submissionId = submission.getSubmissionId();
		long problemId = submission.getProblem().getProblemId();

		List<Checkpoint> checkpoints = checkpointMapper.getCheckpointsUsingProblemId(problemId);
		for ( Checkpoint checkpoint : checkpoints ) {
			int checkpointId = checkpoint.getCheckpointId();
			int checkpointScore = checkpoint.getScore();
			String inputFilePath = String.format("%s/%s/input#%s.txt",
					new Object[] { checkpointDirectory, problemId, checkpointId });
			String stdOutputFilePath = String.format("%s/%s/output#%s.txt",
					new Object[] { checkpointDirectory, problemId, checkpointId });
			String outputFilePath = getOutputFilePath(workDirectory, checkpointId);

			Map<String, Object> runtimeResult = getRuntimeResult(
					runner.getRuntimeResult(submission, workDirectory, baseFileName, inputFilePath, outputFilePath),
					stdOutputFilePath, outputFilePath);
			runtimeResult.put("score", checkpointScore);
			runtimeResults.add(runtimeResult);
			applicationDispatcher.onOneTestPointFinished(submissionId, checkpointId, runtimeResult);
		}
		applicationDispatcher.onAllTestPointsFinished(submissionId, runtimeResults);
	}

	/**
	 * 获取当前测试点输出路径.
	 * @param workDirectory - 编译生成结果的目录以及程序输出的目录
	 * @param checkpointId - 当前测试点编号
	 * @return 当前测试点输出路径
	 */
	private String getOutputFilePath(String workDirectory, int checkpointId) {
		return String.format("%s/output#%s.txt",
				new Object[] {workDirectory, checkpointId});
	}

	/**
	 * 获取程序运行结果(及答案比对结果).
	 * @param result - 包含程序运行结果的Map对象
	 * @param standardOutputFilePath - 标准输出文件路径
	 * @param outputFilePath - 用户输出文件路径
	 * @return 包含程序运行结果的Map对象
	 */
	private Map<String, Object> getRuntimeResult(Map<String, Object> result,
		String standardOutputFilePath, String outputFilePath) {
		String runtimeResultSlug = (String)result.get("runtimeResult");
		int usedTime = (Integer)result.get("usedTime");
		int usedMemory = (Integer)result.get("usedMemory");

		if ( runtimeResultSlug.equals("AC") && 
				!isOutputTheSame(standardOutputFilePath, outputFilePath) ) {
			runtimeResultSlug = "WA";
			result.put("runtimeResult", runtimeResultSlug);
		}
		LOGGER.info(String.format("RuntimeResult: [%s, Time: %d ms, Memory: %d KB]",
				new Object[] { runtimeResultSlug, usedTime, usedMemory }));

		return result;
	}

	/**
	 * 获取用户输出和标准输出的比对结果.
	 * @param standardOutputFilePath - 标准输出文件路径
	 * @param outputFilePath - 用户输出文件路径
	 * @return 用户输出和标准输出是否相同
	 */
	private boolean isOutputTheSame(String standardOutputFilePath, String outputFilePath) {
		try {
			return comparator.isOutputTheSame(standardOutputFilePath, outputFilePath);
		} catch (IOException ex) {
			LOGGER.catching(ex);
		}
		return false;
	}

	/**
	 * 评测完成后, 清理所生成的文件.
	 * @param baseDirectory - 用于产生输出结果目录
	 */
	private void cleanUp(String baseDirectory) {
		File baseDirFile = new File(baseDirectory);
		if ( baseDirFile.exists() ) {
			try {
				FileUtils.deleteDirectory(baseDirFile);
			} catch (IOException ex) {
				LOGGER.catching(ex);
			}
		}
	}

	/**
	 * 自动注入的ApplicationDispatcher对象.
	 * 完成每个阶段的任务后推送消息至消息队列.
	 */
	@Autowired
	private ApplicationDispatcher applicationDispatcher;

	/**
	 * 自动注入的Preprocessor对象.
	 * 完成编译前的准备工作.
	 */
	@Autowired
	private Preprocessor preprocessor;

	/**
	 * 自动注入的Compiler对象.
	 * 完成编译工作.
	 */
	@Autowired
	private Compiler compiler;

	/**
	 * 自动注入的Runner对象.
	 * 完成程序运行工作.
	 */
	@Autowired
	private Runner runner;

	/**
	 * 自动注入的Matcher对象.
	 * 完成输出结果比对工作.
	 */
	@Autowired
	private Comparator comparator;

	/**
	 * 自动注入的SubmissionMapper对象.
	 */
	@Autowired
	private SubmissionMapper submissionMapper;

	/**
	 * 自动注入的CheckpointMapper对象.
	 * 用于获取试题的测试点.
	 */
	@Autowired
	private CheckpointMapper checkpointMapper;

	/**
	 * 评测机的工作目录.
	 * 用于存储编译结果以及程序输出结果.
	 */
	@Value("${judger.workDir}")
	private String workBaseDirectory;

	/**
	 * 测试点的存储目录.
	 * 用于存储测试点的输入输出数据.
	 */
	@Value("${judger.checkpointDir}")
	private String checkpointDirectory;

	/**
	 * 日志记录器.
	 */
	private static final Logger LOGGER = LogManager.getLogger(Dispatcher.class);
}
