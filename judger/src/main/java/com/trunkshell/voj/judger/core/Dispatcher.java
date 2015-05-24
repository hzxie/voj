package com.trunkshell.voj.judger.core;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.trunkshell.voj.judger.application.ApplicationDispatcher;
import com.trunkshell.voj.judger.exception.IllgealSubmissionException;
import com.trunkshell.voj.judger.mapper.SubmissionMapper;
import com.trunkshell.voj.judger.model.Submission;
import com.trunkshell.voj.judger.util.RandomStringGenerator;

/**
 * 评测机调度器.
 * 用于完成评测机的评测流程.
 * 每个阶段结束后推送消息至消息队列; 评测结束后写入数据库.
 * 
 * @author Xie Haozhe
 */
@Component
public class Dispatcher {
	/**
	 * 创建新的评测任务.
	 * 每次只运行一个评测任务.
	 * @param submissionId - 提交记录的唯一标识符
	 * @throws IllgealSubmissionException 
	 */
	public void createNewTask(long submissionId) throws IllgealSubmissionException {
		synchronized(this) {
			String baseDirectory = String.format("%s/%s", new Object[] {workBaseDirectory, submissionId});
			String baseFileName = RandomStringGenerator.getRandomString(12, RandomStringGenerator.Mode.ALPHANUMERIC);
			Submission submission = submissionMapper.getSubmission(submissionId);
			
			if ( submission == null ) {
				throw new IllgealSubmissionException(
						String.format("Illegal submission #%s", 
						new Object[] { submissionId }));
			}
			compile(submission, baseDirectory, baseFileName);
			fetchTestPoints(submission.getProblemId());
			runProgram(baseDirectory, baseFileName);
			compareOutput(baseDirectory);
			cleanUp(baseDirectory);
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
	private void compile(Submission submission, 
			String workDirectory, String baseFileName) {
		
	}

	/**
	 * 从数据库抓取评测数据.
	 * @param problemId - 试题的唯一标识符
	 */
	private void fetchTestPoints(long problemId) {
		// Check if Exists
		// Dump Data
	}
	
	/**
	 * 执行程序.
	 * @param workDirectory - 编译生成结果的目录以及程序输出的目录
	 * @param baseFileName - 待执行的应用程序文件名(不包含文件后缀)
	 */
	private void runProgram(String workDirectory, String baseFileName) {
		
	}
	
	/**
	 * 比对输出结果.
	 * @param workDirectory
	 */
	private void compareOutput(String workDirectory) {
		
	}
	
	/**
	 * 评测完成后, 清理所生成的文件.
	 * @param baseDirectory - 用于产生输出结果目录
	 */
	private void cleanUp(String baseDirectory) {
		
	}
	
	/**
	 * 自动注入的SubmissionMapper对象.
	 */
	@Autowired
	private SubmissionMapper submissionMapper;
	
	/**
	 * 自动注入的ApplicationDispatcher对象.
	 * 完成每个阶段的任务后推送消息至消息队列.
	 */
	@Autowired
	private ApplicationDispatcher applicationDispatcher;
	
	/**
	 * 评测机的工作目录.
	 * 用于存储编译结果以及程序输出结果.
	 */
	@Value("${judger.workDir}")
    private String workBaseDirectory;
	
	/**
	 * 测试数据目录.
	 */
	private static final String TEST_POINTS_DIRECTORY = "testpoints/";
}
