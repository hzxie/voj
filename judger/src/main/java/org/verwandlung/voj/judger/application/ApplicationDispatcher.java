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
package org.verwandlung.voj.judger.application;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import org.verwandlung.voj.judger.core.Dispatcher;
import org.verwandlung.voj.judger.mapper.JudgeResultMapper;
import org.verwandlung.voj.judger.mapper.SubmissionMapper;
import org.verwandlung.voj.judger.messenger.MessageSender;
import org.verwandlung.voj.judger.model.JudgeResult;
import org.verwandlung.voj.judger.model.Submission;

/**
 * 应用程序调度器.
 * 
 * @author Haozhe Xie
 */
@Component
public class ApplicationDispatcher {
	/**
	 * 收到消息队列的新的评测请求时的回调函数.
	 * @param submissionId - 评测记录的唯一标识符
	 */
	public void onSubmissionCreated(long submissionId) {
		try {
			judgerDispatcher.createNewTask(submissionId);
		} catch (Exception ex) {
			LOGGER.catching(ex);
		}
	}
	
	/**
	 * 当系统错误发生时通知用户.
	 * @param submissionId - 评测记录的唯一标识符
	 */
	public void onErrorOccurred(long submissionId) {
		updateSubmission(submissionId, 0, 0, 0, "SE", "Internal error occured.");
		
		Map<String, Object> mapMessage = new HashMap<>();
		mapMessage.put("event", "ErrorOccurred");
		mapMessage.put("submissionId", submissionId);
		
		messageSender.sendMessage(mapMessage);
	}
	
	/**
	 * 当编译阶段结束时通知用户.
	 * @param submissionId - 评测记录的唯一标识符
	 * @param result - 编译结果
	 */
	public void onCompileFinished(long submissionId, Map<String, Object> result) {
		boolean isSuccessful = (Boolean)result.get("isSuccessful");
		String log = log = getJudgeLog(result);

		if ( !isSuccessful ) {
			updateSubmission(submissionId, 0, 0, 0, "CE", log);
		}
		Map<String, Object> mapMessage = new HashMap<>();
		mapMessage.put("event", "CompileFinished");
		mapMessage.put("submissionId", submissionId);
		mapMessage.put("isSuccessful", isSuccessful);
		mapMessage.put("log", log);
		
		messageSender.sendMessage(mapMessage);
	}
	
	/**
	 * 实时返回评测结果.
	 * @param submissionId - 提交记录的编号
	 * @param checkpointId - 测试点的编号
	 * @param runtimeResult - 某个测试点的程序运行结果
	 */
	public void onOneTestPointFinished(long submissionId, int checkpointId, Map<String, Object> runtimeResult) {
		String runtimeResultSlug = getRuntimeResultSlug(runtimeResult);
		String runtimeResultName = getRuntimeResultName(runtimeResultSlug);
		int usedTime = getUsedTime(runtimeResult);
		int usedMemory = getUsedMemory(runtimeResult);
		int score = getScore(runtimeResult);
		
		Map<String, Object> mapMessage = new HashMap<>();
		mapMessage.put("event", "TestPointFinished");
		mapMessage.put("submissionId", submissionId);
		mapMessage.put("checkpointId", checkpointId);
		mapMessage.put("runtimeResult", runtimeResultName);
		mapMessage.put("usedTime", usedTime);
		mapMessage.put("usedMemory", usedMemory);
		mapMessage.put("score", score);
		
		messageSender.sendMessage(mapMessage);
	}
	
	/**
	 * 持久化程序评测结果
	 * @param submissionId - 提交记录的编号
	 * @param runtimeResults - 对各个测试点的评测结果集
	 */
	public void onAllTestPointsFinished(long submissionId, List<Map<String, Object>> runtimeResults) {
		int totalTime = 0;
		int maxMemory = 0;
		int totalScore = 0;
		String runtimeResultSlug = "AC";
		String log = "System Error.";
		
		for ( Map<String, Object> runtimeResult : runtimeResults ) {
			String currentRuntimeResultSlug = getRuntimeResultSlug(runtimeResult);
			int usedTime = getUsedTime(runtimeResult);
			int usedMemory = getUsedMemory(runtimeResult);
			int score = getScore(runtimeResult);
			
			totalTime += usedTime;
			if ( usedMemory > maxMemory ) {
				maxMemory = usedMemory;
			}
			if ( "AC".equals(currentRuntimeResultSlug) ) {
				totalScore += score;
			}
			if ( !"AC".equals(currentRuntimeResultSlug) ) {
				runtimeResultSlug = currentRuntimeResultSlug;
			}
		}
		log = getJudgeLog(runtimeResults, runtimeResultSlug, totalTime, maxMemory, totalScore);
		updateSubmission(submissionId, totalTime, maxMemory, totalScore, runtimeResultSlug, log);
		
		Map<String, Object> mapMessage = new HashMap<>();
		mapMessage.put("event", "AllTestPointsFinished");
		mapMessage.put("submissionId", submissionId);
		mapMessage.put("runtimeResult", getRuntimeResultName(runtimeResultSlug));
		mapMessage.put("totalTime", totalTime);
		mapMessage.put("maxMemory", maxMemory);
		mapMessage.put("totalScore", totalScore);
		
		messageSender.sendMessage(mapMessage);
	}
	
	/**
	 * 从评测结果集中获取程序评测结果的唯一英文缩写.
	 * @param runtimeResult - 程序评测结果 
	 * @return 程序评测结果的唯一英文缩写
	 */
	private String getRuntimeResultSlug(Map<String, Object> runtimeResult) {
		Object runtimeResultObject = runtimeResult.get("runtimeResult");
		
		if ( runtimeResultObject == null ) {
			return "SE";
		}
		return (String)runtimeResultObject;
	}
	
	/**
	 * 获取评测结果的全称.
	 * @param runtimeResultSlug - 评测结果的唯一英文缩写
	 * @return 评测结果的全称
	 */
	private String getRuntimeResultName(String runtimeResultSlug) {
		JudgeResult judgeResult = judgeResultMapper.getJudgeResultUsingSlug(runtimeResultSlug);
		
		if ( judgeResult == null ) {
			return "System Error";
		}
		return judgeResult.getJudgeResultName();
	}
	
	/**
	 * 从评测结果集中获取程序运行时间(ms).
	 * @param runtimeResult - 程序评测结果
	 * @return 程序运行时间(ms)
	 */
	private int getUsedTime(Map<String, Object> runtimeResult) {
		Object usedTimeObject = runtimeResult.get("usedTime");
		
		if ( usedTimeObject == null ) {
			return 0;
		}
		return (Integer)usedTimeObject;
	}
	
	/**
	 * 从评测结果集中获取内存使用量(KB).
	 * @param runtimeResult - 程序评测结果
	 * @return 内存使用量(KB)
	 */
	private int getUsedMemory(Map<String, Object> runtimeResult) {
		Object usedMemoryObject = runtimeResult.get("usedMemory");
		
		if ( usedMemoryObject == null ) {
			return 0;
		}
		return (Integer)usedMemoryObject;
	}
	
	/**
	 * 从评测结果集中获取测试点对应的分值
	 * @param runtimeResult - 程序评测结果
	 * @return 测试点对应的分值
	 */
	private int getScore(Map<String, Object> runtimeResult) {
		Object scoreObject = runtimeResult.get("score");
		
		if ( scoreObject == null ) {
			return 0;
		}
		return (Integer)scoreObject;
	}
	
	/**
	 * 格式化编译时日志.
	 * @param result - 包含编译状态的Map<String, Object>对象
	 * @return 格式化后的日志
	 */
	private String getJudgeLog(Map<String, Object> result) {
		boolean isSuccessful = (Boolean)result.get("isSuccessful");
		String compileLog = (String)result.get("log");

		StringBuilder formatedLogBuilder = new StringBuilder();
		formatedLogBuilder.append(String.format("Compile %s.\n\n", new Object[] { isSuccessful ? "Successful" : "Error" }));
		if ( !isSuccessful ) {
			formatedLogBuilder.append(compileLog.replace("\n", "\n\n"));
			formatedLogBuilder.append("\nCompile Error, Time = 0 ms, Memory = 0 KB, Score = 0.\n");
		}
		return formatedLogBuilder.toString();
	}
	
	/**
	 * 格式化运行时日志.
	 * @param runtimeResults - 对各个测试点的评测结果集
	 * @param runtimeResultSlug
	 * @param totalTime
	 * @param maxMemory
	 * @param totalScore
	 * @return
	 */
	private String getJudgeLog(List<Map<String, Object>> runtimeResults, 
			String runtimeResultSlug, int totalTime, int maxMemory, int totalScore) {
		int checkpointId = -1;
		String runtimeResultName = getRuntimeResultName(runtimeResultSlug);
		
		StringBuilder formatedLogBuilder = new StringBuilder();
		formatedLogBuilder.append("Compile Successfully.\n\n");
		for ( Map<String, Object> runtimeResult : runtimeResults ) {
			String currentRuntimeResultSlug = getRuntimeResultSlug(runtimeResult);
			String currentRuntimeResultName = getRuntimeResultName(currentRuntimeResultSlug);
			int usedTime = getUsedTime(runtimeResult);
			int usedMemory = getUsedMemory(runtimeResult);
			int score = getScore(runtimeResult);
			
			if ( !"AC".equals(currentRuntimeResultSlug) ) {
				score = 0;
			}
			formatedLogBuilder.append(String.format("- Test Point #%d: %s, Time = %d ms, Memory = %d KB, Score = %d\n", 
					new Object[] { ++ checkpointId, currentRuntimeResultName, usedTime, usedMemory, score }));
		}
		formatedLogBuilder.append(String.format("\n%s, Time = %d ms, Memory = %d KB, Score = %d\n", 
				new Object[] { runtimeResultName, totalTime, maxMemory, totalScore }));
		return formatedLogBuilder.toString();
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
	 * 用于查询/更新提交记录的相关信息.
	 */
	@Autowired
	private SubmissionMapper submissionMapper;
	
	/**
	 * 自动注入的JudgeResultMapper对象.
	 * 用于全部评测结果的信息.
	 */
	@Autowired
	private JudgeResultMapper judgeResultMapper;
	
	/**
	 * 日志记录器.
	 */
	private static final Logger LOGGER = LogManager.getLogger(ApplicationDispatcher.class);
}
