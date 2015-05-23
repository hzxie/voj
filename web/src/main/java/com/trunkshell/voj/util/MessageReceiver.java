package com.trunkshell.voj.util;

import javax.jms.JMSException;
import javax.jms.MapMessage;
import javax.jms.Message;
import javax.jms.MessageListener;

import org.springframework.stereotype.Component;

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
				String judgeResult = (String)mapMessage.getObject("judgeResult");
				
				System.out.println(submissionId + " / " +judgeResult);
			} catch (JMSException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
}
