package com.trunkshell.voj.judger.application;

import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.trunkshell.voj.judger.core.Dispatcher;
import com.trunkshell.voj.judger.exception.IllgealSubmissionException;
import com.trunkshell.voj.judger.messaging.MessageSender;

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
		try {
			judgerDispatcher.createNewTask(submissionId);
		} catch (IllgealSubmissionException ex) {
			logger.catching(ex);
		}
	}
	
	/**
	 * 当编译阶段结束时通知用户.
	 * @param submissionId - 评测记录的唯一标识符
	 * @param result - 编译结果
	 */
	public void onCompileFinished(long submissionId, Map<String, Object> result) {
		Map<String, Object> mapMessage = new HashMap<String, Object>();
		mapMessage.put("event", "compileFinished");
		mapMessage.put("submissionId", submissionId);
		mapMessage.put("isSuccessful", result.get("isSuccessful"));
		mapMessage.put("log", result.get("log"));
		
		messageSender.sendMessage(mapMessage);
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
	 * 日志记录器.
	 */
	private static final Logger logger = LogManager.getLogger(ApplicationDispatcher.class);
}
