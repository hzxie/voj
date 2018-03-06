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

public class ContestSubmission {
	/**
	 * ContestSubmission的默认构造函数.
	 */
	public ContestSubmission() { }
	
	/**
	 * ContestSubmission的构造函数.
	 * @param contest - 竞赛
	 * @param submission - 提交记录
	 */
	public ContestSubmission(Contest contest, Submission submission) {
		this.contest = contest;
		this.submission = submission;
	}

	/**
	 * 获取竞赛.
	 * @return 竞赛
	 */
	public Contest getContest() {
		return contest;
	}

	/**
	 * 设置竞赛.
	 * @param contest - 竞赛
	 */
	public void setContest(Contest contest) {
		this.contest = contest;
	}
	/**
	 * 获取参提交记录.
	 * @return 提交记录
	 */
	public Submission getSubmission() {
		return submission;
	}

	/**
	 * 设置提交记录.
	 * @param submission - 提交记录
	 */
	public void setSubmission(Submission submission) {
		this.submission = submission;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		return String.format("ContestSubmission [Contest={%s}, Submission={%s}]", 
				new Object[] { contest, submission });
	}

	/**
	 * 竞赛.
	 */
	private Contest contest;

	/**
	 * 提交记录.
	 */
	private Submission submission;
}