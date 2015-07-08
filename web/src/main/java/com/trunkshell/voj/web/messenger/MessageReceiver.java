package com.trunkshell.voj.web.messenger;

import javax.jms.JMSException;
import javax.jms.MapMessage;
import javax.jms.Message;
import javax.jms.MessageListener;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

/**
 * 消息接收服务.
 * 用于接收来自评测机的实时评测结果.
 * 
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
            } catch (JMSException ex) {
                LOGGER.catching(ex);
            }
        }
    }
    
    /**
     * 日志记录器.
     */
    private static final Logger LOGGER = LogManager.getLogger(MessageReceiver.class);
}
