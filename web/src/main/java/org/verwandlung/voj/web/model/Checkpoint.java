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
 * 程序测试点的Model.
 * 对应数据库中的voj_problem_checkpoints数据表.
 * 
 * @author Haozhe Xie
 */
public class Checkpoint implements Serializable {
	/**
	 * CheckPoint的默认构造函数.
	 */
	public Checkpoint() { }
	
	/**
	 * CheckPoint的构造函数.
	 * @param problemId - 试题的唯一标识符
	 * @param checkpointId - 测试点的唯一标识符
	 * @param isExactlyMatch - 是否精确匹配测试点
	 * @param score - 测试点的分值
	 * @param input - 测试点的标准输入
	 * @param output - 测试点的标准输出
	 */
	public Checkpoint(long problemId, int checkpointId, boolean isExactlyMatch, int score, String input, String output) { 
		this.problemId = problemId;
		this.checkpointId = checkpointId;
		this.isExactlyMatch = isExactlyMatch;
		this.score = score;
		this.input = input;
		this.output = output;
	}

	/**
	 * 获取试题的唯一标识符.
	 * @return 试题的唯一标识符
	 */
	public long getProblemId() {
		return problemId;
	}

	/**
	 * 设置试题的唯一标识符.
	 * @param problemId - 试题的唯一标识符
	 */
	public void setProblemId(long problemId) {
		this.problemId = problemId;
	}
	
	/**
	 * 获取测试点的唯一标识符.
	 * @return 测试点的唯一标识符
	 */
	public int getCheckpointId() {
		return checkpointId;
	}

	/**
	 * 设置测试点的唯一标识符.
	 * @param checkpointId - 测试点的唯一标识符
	 */
	public void setCheckpointId(int checkpointId) {
		this.checkpointId = checkpointId;
	}

	/**
	 * 获取是否精确匹配测试点.
	 * @return 是否精确匹配测试点
	 */
	public boolean isExactlyMatch() {
		return isExactlyMatch;
	}

	/**
	 * 设置精确匹配测试点.
	 * @param isExactlyMatch - 是否精确匹配测试点
	 */
	public void setExactlyMatch(boolean isExactlyMatch) {
		this.isExactlyMatch = isExactlyMatch;
	}

	/**
	 * 获取测试点的分值.
	 * @return 测试点的分值
	 */
	public int getScore() {
		return score;
	}

	/**
	 * 设置测试点的分值.
	 * @param score - 测试点的分值
	 */
	public void setScore(int score) {
		this.score = score;
	}

	/**
	 * 获取测试点的输入.
	 * @return 测试点的输入
	 */
	public String getInput() {
		return input;
	}

	/**
	 * 设置测试点的输入.
	 * @param input - 测试点的输入
	 */
	public void setInput(String input) {
		this.input = input;
	}

	/**
	 * 获取测试点的输出.
	 * @return 测试点的输出 
	 */
	public String getOutput() {
		return output;
	}

	/**
	 * 设置测试点的输出.
	 * @param output - 测试点的输出
	 */
	public void setOutput(String output) {
		this.output = output;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		return String.format("CheckPoint [ProblemId=%d, CheckpointId=%d, Score=%d]", 
				new Object[] {problemId, checkpointId, score});
	}

	/**
	 * 试题的唯一标识符.
	 */
	private long problemId;
	
	/**
	 * 测试点的唯一标识符.
	 */
	private int checkpointId;
	
	/**
	 * 是否精确匹配测试点.
	 */
	private boolean isExactlyMatch;
	
	/**
	 * 测试点分值.
	 */
	private int score;
	
	/**
	 * 测试点的标准输入.
	 */
	private String input;
	
	/**
	 * 测试点的标准输出.
	 */
	private String output;
	
	/**
	 * 唯一的序列化标识符.
	 */
	private static final long serialVersionUID = -8129670699537187948L;
}
