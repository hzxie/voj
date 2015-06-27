package com.trunkshell.voj.web.util;

import javax.jms.JMSException;
import javax.jms.MapMessage;
import javax.jms.Message;
import javax.jms.MessageListener;

import org.springframework.stereotype.Component;

/**
 * 消息接收服务.
 * 用于接收来自评测机的实时评测结果.
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
			} catch (JMSException e) {
				
			}
		}
	}
}
