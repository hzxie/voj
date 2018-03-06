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
package org.verwandlung.voj.web.model;

import java.io.Serializable;

/**
 * 程序评测结果的Model.
 * 对应数据库中的voj_judge_results数据表.
 * 
 * @author Haozhe Xie
 */
public class JudgeResult implements Serializable {
	/**
	 * 评测结果类的默认构造函数.
	 */
	public JudgeResult() { }
	
	/**
	 * 评测结果类的构造函数.
	 * @param judgeResultId - 评测结果的唯一标识符
	 * @param judgeResultSlug - 评测结果的别名
	 * @param judgeResultName - 评测结果的名称
	 */
	public JudgeResult(int judgeResultId, String judgeResultSlug, String judgeResultName) {
		this.judgeResultId = judgeResultId;
		this.judgeResultSlug = judgeResultSlug;
		this.judgeResultName = judgeResultName;
	}
	
	/**
	 * 获取评测结果的唯一标识符.
	 * @return 评测结果的唯一标识符
	 */
	public int getJudgeResultId() {
		return judgeResultId;
	}

	/**
	 * 设置评测结果的唯一标识符.
	 * @param judgeResultId - 评测结果的唯一标识符
	 */
	public void setJudgeResultId(int judgeResultId) {
		this.judgeResultId = judgeResultId;
	}

	/**
	 * 获取评测结果的别名.
	 * @return - 评测结果的别名
	 */
	public String getJudgeResultSlug() {
		return judgeResultSlug;
	}

	/**
	 * 设置评测结果的别名.
	 * @param judgeResultSlug - 评测结果的别名
	 */
	public void setJudgeResultSlug(String judgeResultSlug) {
		this.judgeResultSlug = judgeResultSlug;
	}

	/**
	 * 获取评测结果的名称.
	 * @return 评测结果的名称
	 */
	public String getJudgeResultName() {
		return judgeResultName;
	}

	/**
	 * 设置评测结果的名称.
	 * @param judgeResultName - 评测结果的名称
	 */
	public void setJudgeResultName(String judgeResultName) {
		this.judgeResultName = judgeResultName;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return String.format("JudgeResult [Id=%d, Slug=%s, Name=%s]",
				new Object[] { judgeResultId, judgeResultSlug, judgeResultName });
	}

	/**
	 * 评测结果的唯一标识符.
	 */
	private int judgeResultId;
	
	/**
	 * 评测结果的英文唯一缩写.
	 */
	private String judgeResultSlug;
	
	/**
	 * 评测结果的名称.
	 */
	private String judgeResultName;
	
	/**
	 * 唯一的序列化标识符.
	 */
	private static final long serialVersionUID = -1572920076997918781L;
}
