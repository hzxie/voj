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

import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * 应用程序事件监听器.
 * 负责将消息队列中的消息转发至控制器.
 * 
 * @author Haozhe Xie
 */
@Component
public class ApplicationEventListener {
	/**
	 * ApplicationEventListener的构造函数.
	 */
	public ApplicationEventListener() {
		synchronized (this) {
			if ( scheduler == null ) {
				final int INITIAL_DELAY = 0;
				final int PERIOD = 30;
				
				scheduler = Executors.newScheduledThreadPool(1);
				scheduler.scheduleAtFixedRate(new Runnable() {
					@Override
					public void run() {
						Calendar calendar = Calendar.getInstance();
						calendar.add(Calendar.MINUTE, -PERIOD);
						Date heartbeatTimeDeadline = calendar.getTime();
						
						for ( Iterator<Entry<String, Map<String, Object>>> 
								itr = onlineJudgers.entrySet().iterator(); itr.hasNext(); ) {
							Entry<String, Map<String, Object>> entry = itr.next();
							Date lastHeartbeatTime = (Date) entry.getValue().get("heartbeatTime");
							
							if ( !lastHeartbeatTime.after(heartbeatTimeDeadline) ) {
								itr.remove();
							}
						}
					}
				}, INITIAL_DELAY, PERIOD, TimeUnit.MINUTES);
			}
		}
	}
	
	/**
	 * 提交事件的处理器.
	 * @param event - 提交记录事件
	 * @throws IOException 
	 */
	@EventListener
	public void submissionEventHandler(SubmissionEvent event) throws IOException {
		long submissionId = event.getSubmissionId();
		String judgeResult = event.getJudgeResult();
		String message = event.getMessage();
		boolean isCompleted = event.isCompleted();
		SseEmitter sseEmitter = sseEmitters.get(submissionId);
		
		if ( sseEmitter == null ) {
			LOGGER.warn(String.format("CANNOT get the SseEmitter for submission #%d.", submissionId));
			return;
		}
		Map<String, String> mapMessage = new HashMap<>(3, 1);
		mapMessage.put("judgeResult", judgeResult);
		mapMessage.put("message", message);
		sseEmitter.send(mapMessage);
		
		if ( isCompleted ) {
			sseEmitter.complete();
			removeSseEmitters(submissionId);
		}
	}
	
	/**
	 * 注册Server Sent Event的发送者对象.
	 * @param submissionId - 提交记录的唯一标识符
	 * @param sseEmitter - Server Sent Event的发送者对象
	 */
	public void addSseEmitters(long submissionId, SseEmitter sseEmitter) {
		sseEmitters.put(submissionId, sseEmitter);
	}
	
	/**
	 * 移除Server Sent Event的发送者对象.
	 * @param submissionId - 提交记录的唯一标识符
	 */
	private void removeSseEmitters(long submissionId) {
		sseEmitters.remove(submissionId);
		
		for ( Entry<Long, SseEmitter> mapEntry : sseEmitters.entrySet() ) {
			long currentSubmissionId = mapEntry.getKey();
			if ( currentSubmissionId < submissionId ) {
				sseEmitters.remove(currentSubmissionId);
			}
		}
	}
	
	/**
	 * 处理评测机心跳事件.
	 * @param event - 评测机心跳事件
	 */
	@EventListener
	public void keepAliveEventHandler(KeepAliveEvent event) {
		String judgerUsername = event.getJudgerUsername(); 
		String judgerDescription = event.getJudgerDescription();
		Date heartbeatTime = event.getHeartbeatTime();
		
		Map<String, Object> judgerInformation = new HashMap<>();
		judgerInformation.put("description", judgerDescription);
		judgerInformation.put("heartbeatTime", heartbeatTime);
		
		onlineJudgers.put(judgerUsername, judgerInformation);
	}
	
	/**
	 * 获取评测机的描述信息.
	 * @param judgerUsername - 评测机的用户名
	 * @return 评测机的描述信息
	 */
	public String getJudgerDescription(String judgerUsername) {
		String judgerDescription = "[Offline]";
		
		if ( onlineJudgers.containsKey(judgerUsername) ) {
			String description = (String) onlineJudgers.get(judgerUsername).get("description");
			judgerDescription = "[Online] " + description;
		}
		return judgerDescription;
	}
	
	/**
	 * 获取在线评测机的数量.
	 * @return 在线评测机的数量
	 */
	public long getOnlineJudgers() {
		return onlineJudgers.size();
	}
	
	/**
	 * SseEmitter对象的列表.
	 * Map中的Key表示提交记录的唯一标识符.
	 * Map中的Value表示对应的SseEmitter对象, 用于推送实时评测信息.
	 */
	private static Map<Long, SseEmitter> sseEmitters = new Hashtable<>();
	
	/**
	 * 在线评测机的列表.
	 * Map中的Key表示评测机的用户名.
	 * Map中的Value表示对应评测机的信息.
	 */
	private static Map<String, Map<String, Object>> onlineJudgers = new Hashtable<String, Map<String, Object>>();
	
	/**
	 * ScheduledExecutorService对象.
	 * 用于定期移除离线的评测机.
	 */
	private static ScheduledExecutorService scheduler = null;
	
	/**
	 * 日志记录器.
	 */
	private static final Logger LOGGER = LogManager.getLogger(ApplicationEventListener.class);
}
