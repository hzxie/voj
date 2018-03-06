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
package org.verwandlung.voj.judger.messenger;

import javax.jms.JMSException;
import javax.jms.MapMessage;
import javax.jms.Message;
import javax.jms.MessageListener;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import org.verwandlung.voj.judger.application.ApplicationDispatcher;

/**
 * 消息接收服务.
 * @author Haozhe Xie
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
				String event = mapMessage.getString("event");
				
				if ( "SubmissionCreated".equals(event) ) {
					newSubmissionHandler(mapMessage);
				} else {
					LOGGER.warn(String.format("Unknown Event Received. [Event = %s]", 
							new Object[] { event }));
				}
			} catch (Exception ex) {
				LOGGER.catching(ex);
			}
		}
	}
	
	/**
	 * 处理新提交请求.
	 * @param mapMessage - 消息队列中收到的MapMessage对象
	 * @throws JMSException
	 */
	private void newSubmissionHandler(MapMessage mapMessage) throws JMSException {
		long submissionId = mapMessage.getLong("submissionId");
		LOGGER.info(String.format("Received new submission task #%d", 
						new Object[] {submissionId}));
		
		dispatcher.onSubmissionCreated(submissionId);
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
	private static final Logger LOGGER = LogManager.getLogger(MessageReceiver.class);
}
