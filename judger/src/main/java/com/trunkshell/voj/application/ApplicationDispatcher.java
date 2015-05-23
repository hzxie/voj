package com.trunkshell.voj.application;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.trunkshell.voj.messaging.MessageSender;

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
		Map<String, Object> mapMessage = new HashMap<String, Object>();
		mapMessage.put("submissionId", submissionId);
		
		try {
			Thread.sleep(1000);
			mapMessage.put("judgeResult", "Compiling");
			messageSender.sendMessage(mapMessage);
		
			Thread.sleep(1000);
			mapMessage.put("judgeResult", "Judging");
			messageSender.sendMessage(mapMessage);
		
			Thread.sleep(1000);
			mapMessage.put("judgeResult", "AC");
			messageSender.sendMessage(mapMessage);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * 自动注入的MessageSender对象.
	 * 用于向消息队列发送消息.
	 */
	@Autowired
	private MessageSender messageSender;
}
