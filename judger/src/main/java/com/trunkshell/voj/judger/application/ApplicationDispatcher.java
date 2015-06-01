package com.trunkshell.voj.judger.application;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.trunkshell.voj.judger.core.Dispatcher;
import com.trunkshell.voj.judger.exception.IllgealSubmissionException;
import com.trunkshell.voj.judger.mapper.SubmissionMapper;
import com.trunkshell.voj.judger.messaging.MessageSender;
import com.trunkshell.voj.judger.model.Submission;

/**
 * 应用程序调度器.
 * 
 * @author Xie Haozhe
 */
@Component
public class ApplicationDispatcher {
	/**
	 * 收到消息队列的新的评测请求时的回调函数.
	 * @param submissionId - 评测记录的唯一标识符
	 */
	public void onSubmissionCreated(long submissionId) {
		System.out.println(1);
		try {
			judgerDispatcher.createNewTask(submissionId);
		} catch (IllgealSubmissionException ex) {
			logger.catching(ex);
		}
	}
	
	/**
	 * 当系统错误发生时通知用户.
	 * @param submissionId - 评测记录的唯一标识符
	 */
	public void onErrorOccurred(long submissionId) {
		updateSubmission(submissionId, 0, 0, 0, "SE", "Internal error occured.");
		
		Map<String, Object> mapMessage = new HashMap<String, Object>();
		mapMessage.put("event", "errorOccurred");
		mapMessage.put("submissionId", submissionId);
		
		messageSender.sendMessage(mapMessage);
	}
	
	/**
	 * 当编译阶段结束时通知用户.
	 * @param submissionId - 评测记录的唯一标识符
	 * @param result - 编译结果
	 */
	public void onCompileFinished(long submissionId, Map<String, Object> result) {
		boolean isSuccessful = (Boolean) result.get("isSuccessful");
		String log = (String) result.get("log");
		
		if ( !isSuccessful ) {
			updateSubmission(submissionId, 0, 0, 0, "CE", log);
		}
		Map<String, Object> mapMessage = new HashMap<String, Object>();
		mapMessage.put("event", "compileFinished");
		mapMessage.put("submissionId", submissionId);
		mapMessage.put("isSuccessful", isSuccessful);
		mapMessage.put("log", log);
		
		messageSender.sendMessage(mapMessage);
	}
	
	/**
	 * 更新提交记录信息.
	 * @param submissionId - 提交记录的唯一标识符
	 * @param usedTime - 提交运行使用时间(所有时间之和)
	 * @param usedMemory - 提交运行使用内存(最大内存占用)
	 * @param score - 运行得分
	 * @param judgeResult - 运行结果(JudgeResultSlug)
	 * @param log - 运行日志记录
	 */
	private void updateSubmission(long submissionId, int usedTime, 
			int usedMemory, int score, String judgeResult, String log) {
		Submission submission = submissionMapper.getSubmission(submissionId);
		submission.setExecuteTime(new Date());
		submission.setUsedTime(usedTime);
		submission.setUsedMemory(usedMemory);
		submission.setJudgeScore(score);
		submission.setJudgeResultSlug(judgeResult);
		submission.setJudgeLog(log);
		
		submissionMapper.updateSubmission(submission);
	}
	
	/**
	 * 自动注入的Dispatcher对象.
	 * 用于完成评测作业的任务调度.
	 */
	@Autowired
	private Dispatcher judgerDispatcher;
	
	/**
	 * 自动注入的MessageSender对象.
	 * 用于向消息队列发送消息.
	 */
	@Autowired
	private MessageSender messageSender;

	/**
	 * 自动注入的SubmissionMapper对象.
	 */
	@Autowired
	private SubmissionMapper submissionMapper;
	
	/**
	 * 日志记录器.
	 */
	private static final Logger logger = LogManager.getLogger(ApplicationDispatcher.class);
}
