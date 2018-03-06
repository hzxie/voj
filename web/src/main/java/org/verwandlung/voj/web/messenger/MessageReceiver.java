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
package org.verwandlung.voj.web.messenger;

import java.util.Calendar;
import java.util.Date;

import javax.jms.JMSException;
import javax.jms.MapMessage;
import javax.jms.Message;
import javax.jms.MessageListener;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * 消息接收服务.
 * 用于接收来自评测机的实时评测结果.
 * 
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
				
				if ( "ErrorOccurred".equals(event) ) {
					errorHandler(mapMessage);
				} else if ( "CompileFinished".equals(event) ) {
					compileFinishedHandler(mapMessage);
				} else if ( "TestPointFinished".equals(event) ) {
					testPointFinishedHandler(mapMessage);
				} else if ( "AllTestPointsFinished".equals(event) ) {
					allTestPointsFinishedHandler(mapMessage);
				} else if ( "KeepAlive".equals(event) ) {
					receiveFromAliveJudgersHandler(mapMessage);
				} else {
					LOGGER.warn(String.format("Unknown Event Received. [Event = %s]", 
							new Object[] { event }));
				}
			} catch (JMSException ex) {
				LOGGER.catching(ex);
			}
		}
	}
	
	/**
	 * 处理评测机发生内部错误的消息.
	 * @param mapMessage - 消息队列中收到的MapMessage对象
	 * @throws JMSException 
	 */
	private void errorHandler(MapMessage mapMessage) throws JMSException {
		long submissionId = mapMessage.getLong("submissionId");
		eventPublisher.publishEvent(new SubmissionEvent(this, submissionId, "System Error", "System Error.", true));
		LOGGER.info(String.format("Submission #%d returned [System Error].", submissionId));
	}
	
	/**
	 * 处理评测机编译完成时的消息.
	 * @param mapMessage - 消息队列中收到的MapMessage对象
	 * @throws JMSException 
	 */
	private void compileFinishedHandler(MapMessage mapMessage) throws JMSException {
		long submissionId = mapMessage.getLong("submissionId");
		boolean isSuccessful = mapMessage.getBoolean("isSuccessful");
		String log = mapMessage.getString("log");
		
		if ( isSuccessful ) {
			String message = "Compile Successfully.\n\n";
			eventPublisher.publishEvent(new SubmissionEvent(this, submissionId, "Running", message, false));
			LOGGER.info(String.format("Submission #%d returned [Compile Successfully].", submissionId));
		} else {
			eventPublisher.publishEvent(new SubmissionEvent(this, submissionId, "Compiler Error", log, true));
			LOGGER.info(String.format("Submission #%d returned [Compile Error].\n\tError Message:%s",
					new Object[] { submissionId, log }));
		}
	}
	
	/**
	 * 处理评测机完成单个测试点的消息.
	 * @param mapMessage - 消息队列中收到的MapMessage对象
	 * @throws JMSException
	 */
	private void testPointFinishedHandler(MapMessage mapMessage) throws JMSException {
		long submissionId = mapMessage.getLong("submissionId");
		int checkpointId = mapMessage.getInt("checkpointId");
		String runtimeResult = mapMessage.getString("runtimeResult");
		int usedTime = mapMessage.getInt("usedTime");
		int usedMemory = mapMessage.getInt("usedMemory");
		int score = mapMessage.getInt("score");
		
		String message = String.format("- Test Point #%d: %s, Time = %d ms, Memory = %d KB, Score = %d\n", 
							new Object[] { checkpointId, runtimeResult, usedTime, usedMemory, score });
		eventPublisher.publishEvent(new SubmissionEvent(this, submissionId, "Running", message, false));
		
		LOGGER.info(String.format("Submission #%d/ CheckPoint#%d returned [%s] (Time = %dms, Memory = %d KB, Score = %d).",
				new Object[] { submissionId, checkpointId, runtimeResult, usedTime, usedMemory, score }));
	}
	
	/**
	 * 处理评测机完成全部测试点的消息.
	 * @param mapMessage - 消息队列中收到的MapMessage对象
	 * @throws JMSException
	 */
	private void allTestPointsFinishedHandler(MapMessage mapMessage) throws JMSException {
		long submissionId = mapMessage.getLong("submissionId");
		String runtimeResult = mapMessage.getString("runtimeResult");
		int usedTime = mapMessage.getInt("totalTime");
		int usedMemory = mapMessage.getInt("maxMemory");
		int score = mapMessage.getInt("totalScore");
		
		String message = String.format("\n%s, Time = %d ms, Memory = %d KB, Score = %d\n", 
							new Object[] { runtimeResult, usedTime, usedMemory, score });
		eventPublisher.publishEvent(new SubmissionEvent(this, submissionId, runtimeResult, message, true));
		
		LOGGER.info(String.format("Submission #%d judge completed and returned [%s] (Time = %d ms, Memory = %d KB, Score = %d).",
				new Object[] { submissionId, runtimeResult, usedTime, usedMemory, score }));
	}
	
	/**
	 * 处理来自评测机的Keep-Alive消息.
	 * 用于在Web端获取后端评测机的信息.
	 * @param mapMessage - 消息队列中收到的MapMessage对象
	 * @throws JMSException 
	 */
	private void receiveFromAliveJudgersHandler(MapMessage mapMessage) throws JMSException {
		String judgerUsername = mapMessage.getString("username");
		String judgerDescription = mapMessage.getString("description");
		long heartbeatTimeInMillis = mapMessage.getLong("heartbeatTime");
		
		Calendar calendar = Calendar.getInstance();
		calendar.setTimeInMillis(heartbeatTimeInMillis);
		Date heartbeatTime = calendar.getTime();
				
		eventPublisher.publishEvent(new KeepAliveEvent(this, judgerUsername, judgerDescription, heartbeatTime));
		LOGGER.info(String.format("Received heartbeat from Judger[%s]", judgerUsername));
	}
	
	/**
	 * 自动注入的ApplicationEventPublisher对象.
	 * 用于完成接收到来自评测机的消息后的消息分发操作.
	 */
	@Autowired
	private ApplicationEventPublisher eventPublisher;
	
	/**
	 * 日志记录器.
	 */
	private static final Logger LOGGER = LogManager.getLogger(MessageReceiver.class);
}
