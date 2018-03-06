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

import java.util.Date;

import org.springframework.context.ApplicationEvent;

/**
 * 评测机心跳事件消息.
 * 
 * @author Haozhe Xie
 */
public class KeepAliveEvent extends ApplicationEvent {
	/**
	 * KeepAliveEvent的构造函数.
	 * @param source - 消息发布源
	 * @param judgerUsername - 评测机的用户名
	 * @param judgerDescription - 评测机的描述信息
	 */
	public KeepAliveEvent(Object source, String judgerUsername, String judgerDescription, Date heartbeatTime) {
		super(source);
		this.judgerUsername = judgerUsername;
		this.judgerDescription = judgerDescription;
		this.heartbeatTime = heartbeatTime;
	}
	
	/**
	 * 获取评测机的用户名.
	 * @return 评测机的用户名
	 */
	public String getJudgerUsername() {
		return judgerUsername;
	}

	/**
	 * 获取评测机的描述信息.
	 * @return 评测机的描述信息
	 */
	public String getJudgerDescription() {
		return judgerDescription;
	}
	
	/**
	 * 获取评测机发送心跳的时间.
	 * @return 评测机发送心跳的时间
	 */
	public Date getHeartbeatTime() {
		return heartbeatTime;
	}

	/**
	 * 评测机的用户名.
	 */
	private final String judgerUsername;
	
	/**
	 * 评测机的描述信息.
	 */
	private final String judgerDescription;
	
	/**
	 * 评测机心跳的时间.
	 */
	private final Date heartbeatTime;
	
	/**
	 * 唯一的序列化标识符.
	 */
	private static final long serialVersionUID = -9218788192064705664L;
}
