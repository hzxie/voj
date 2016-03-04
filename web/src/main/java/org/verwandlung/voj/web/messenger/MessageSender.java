package org.verwandlung.voj.web.messenger;

import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;

/**
 * 消息发送服务.
 * 用于发送新的评测任务至消息队列.
 * @author Haozhe Xie
 */
@Component
public class MessageSender {
	/**
	 * 发送消息至消息队列.
	 * @param mapMessage - Key-Value格式的消息
	 */
	public void sendMessage(final Map<String, Object> mapMessage) {
		long submissionId = (Long) mapMessage.get("submissionId");
		
		jmsTemplate.convertAndSend(mapMessage);
		LOGGER.info(String.format("Submission task #%d has been created.", new Object[] {submissionId}));
	}

	/**
	 * 自动注入的JmsTemplate对象.
	 * 用于发送消息至消息队列.
	 */
	@Autowired
	private JmsTemplate jmsTemplate;
	
	/**
	 * 日志记录器.
	 */
	private static final Logger LOGGER = LogManager.getLogger(MessageSender.class);
}
