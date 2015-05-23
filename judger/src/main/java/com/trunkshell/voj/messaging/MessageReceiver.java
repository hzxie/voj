package com.trunkshell.voj.messaging;

import javax.jms.JMSException;
import javax.jms.MapMessage;
import javax.jms.Message;
import javax.jms.MessageListener;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.trunkshell.voj.application.ApplicationDispatcher;

/**
 * 消息接收服务.
 * @author Xie Haozhe
 */
@Component
public class MessageReceiver implements MessageListener {
	/* (non-Javadoc)
	 * @see javax.jms.MessageListener#onMessage(javax.jms.Message)
	 */
	public void onMessage(Message message) {
		if ( message instanceof MapMessage ) {
			final MapMessage mapMessage = (MapMessage) message;
			
			try {
				long submissionId = (Long)mapMessage.getObject("submissionId");
				dispatcher.onSubmissionCreated(submissionId);
				logger.info(String.format("Received new submission task #%s", 
								new Object[] {submissionId}));
			} catch (JMSException ex) {
				logger.catching(ex);
			}
		}
	}
	
	/**
	 * 自动注入的ApplicationDispatcher对象.
	 * 用于完成接收消息后的回调操作.
	 */
	@Autowired
	private ApplicationDispatcher dispatcher;
	
	/**
	 * 日志记录器.
	 */
	private static final Logger logger = LogManager.getLogger(MessageReceiver.class);
}
