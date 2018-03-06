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

import org.springframework.context.ApplicationEvent;

/**
 * 提交记录事件消息.
 * 
 * @author Haozhe Xie
 */
public class SubmissionEvent extends ApplicationEvent {
	/**
	 * SubmissionEvent的构造函数.
	 * @param source - 消息发布源
	 * @param submissionId - 提交记录的唯一标识符
	 * @param judgeResult - 当前评测结果
	 * @param message - 评测消息
	 * @param isCompleted - 评测是否完成
	 */
	public SubmissionEvent(Object source, long submissionId, String judgeResult, String message, boolean isCompleted) {
		super(source);
		this.submissionId = submissionId;
		this.judgeResult = judgeResult;
		this.message = message;
		this.isCompleted = isCompleted;
	}
	
	/**
	 * 获取提交记录的唯一标识符.
	 * @return 提交记录的唯一标识符
	 */
	public long getSubmissionId() {
		return submissionId;
	}

	/**
	 * 获取当前评测结果.
	 * @return 当前评测结果
	 */
	public String getJudgeResult() {
		return judgeResult;
	}
	
	/**
	 * 获取评测消息.
	 * @return 评测消息
	 */
	public String getMessage() {
		return message;
	}

	/**
	 * 获取评测是否已完成.
	 * @return 评测是否已完成
	 */
	public boolean isCompleted() {
		return isCompleted;
	}

	/**
	 * 评测记录的唯一标识符.
	 */
	private final long submissionId;
	
	/**
	 * 当前评测结果.
	 */
	private final String judgeResult;
	
	/**
	 * 评测消息.
	 */
	private final String message;
	
	/**
	 * 评测是否完成.
	 */
	private final boolean isCompleted;
	
	/**
	 * 唯一的序列化标识符.
	 */
	private static final long serialVersionUID = 4808514481370058683L;
}
