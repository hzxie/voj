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

public class ContestContestant implements Comparable {
	/**
	 * ContestContestant的默认构造函数.
	 */
	public ContestContestant() { }
	
	/**
	 * ContestContestant的构造函数.
	 * @param contest - 竞赛
	 * @param contestant - 参赛者
	 */
	public ContestContestant(Contest contest, User contestant) {
		this.contest = contest;
		this.contestant = contestant;
	}

	/**
	 * 获取竞赛对象.
	 * @return 竞赛对象
	 */
	public Contest getContest() {
		return contest;
	}

	/**
	 * 设置竞赛对象.
	 * @param contest - 竞赛对象
	 */
	public void setContest(Contest contest) {
		this.contest = contest;
	}
	
	/**
	 * 获取参赛者对象.
	 * @return 参赛者对象
	 */
	public User getContestant() {
		return contestant;
	}

	/**
	 * 设置参赛者对象.
	 * @param contestant - 参赛者对象
	 */
	public void setContestant(User contestant) {
		this.contestant = contestant;
	}

	/**
	 * 获取得分.
	 * @return 得分
	 */
	public int getScore() {
		return score;
	}

	/**
	 * 获取运行时间(OI)或罚时(ACM).
	 * @return 运行时间(OI)或罚时(ACM)
	 */
	public long getTime() {
		return time;
	}

	/**
	 * 设置罚时(ACM).
	 * @param time 罚时(ACM)
	 */
	public void setTime(long time) {
		this.time = time;
	}

	/**
	 * 设置竞赛中的排名.
	 * @param rank - 竞赛中的排名
	 */
	public void setRank(long rank) {
		this.rank = rank;
	}

	/**
	 * 获取竞赛中的排名.
	 * @return 竞赛中的排名
	 */
	public long getRank() {
		return rank;
	}

	/**
	 * 获取代码片段.
	 * @return 代码片段
	 */
	public String getCodeSnippet() {
		return codeSnippet;
	}

	/**
	 * 设置代码片段.
	 * @param codeSnippet - 代码片段
	 */
	public void setCodeSnippet(String codeSnippet) {
		this.codeSnippet = codeSnippet;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#compareTo(Object)
	 */
	public int compareTo(Object o) {
		if ( !(o instanceof ContestContestant) ) {
			throw new ClassCastException("A ContestContestant object expected.");
		}

		ContestContestant occ = (ContestContestant) o;
		if ( occ.getScore() == this.score ) {
			return this.time < occ.getTime() ? -1 : 1;
		}
		return occ.getScore() - this.score;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		return String.format("ContestContestant [Contest={%s}, Contestant={%s}, Score=%s, Time=%s (ms)]", 
				new Object[] { contest, contestant, score, time });
	}

	/**
	 * 竞赛.
	 */
	private Contest contest;
	
	/**
	 * 参赛者.
	 */
	private User contestant;
	
	/**
	 * 得分.
	 */
	private int score;

	/**
	 * OI赛制中的运行时间.
	 * ACM赛制中的罚时.
	 */
	private long time;

	/**
	 * 竞赛中的排名.
	 */
	private long rank;
	
	/**
	 * 代码片段 (JSON格式).
	 * 用于OI赛制中中临时保存参赛者的代码.
	 */
	private String codeSnippet;
}
