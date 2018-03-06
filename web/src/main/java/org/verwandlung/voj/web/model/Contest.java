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

import java.util.Date;

public class Contest {
	/**
	 * Contest的默认构造函数. 
	 */
	public Contest() { }
	
	/**
	 * Contest的构造函数.
	 * @param contestName - 竞赛的名称
	 * @param contestNotes -  竞赛的说明
	 * @param startTime - 竞赛开始时间
	 * @param endTime - 竞赛结束时间
	 * @param mode - 竞赛的赛制
	 * @param problems - 竞赛包含的试题
	 */
	public Contest(String contestName, String contestNotes, 
		Date startTime, Date endTime, String mode, String problems) {
		this.contestName = contestName;
		this.contestNotes = contestNotes;
		this.startTime = startTime;
		this.endTime = endTime;
		this.contestMode = mode;
		this.problems = problems;
	}

	/**
	 * Contest的构造函数.
	 * @param contestId - 竞赛的唯一标识符
	 * @param contestName - 竞赛的名称
	 * @param contestNotes -  竞赛的说明
	 * @param startTime - 竞赛开始时间
	 * @param endTime - 竞赛结束时间
	 * @param mode - 竞赛的赛制
	 * @param problems - 竞赛中包含的试题
	 */
	public Contest(long contestId, String contestNotes, String contestName, 
		Date startTime, Date endTime, String mode, String problems) {
		this(contestName, contestNotes, startTime, endTime, mode, problems);
		this.contestId = contestId;
	}

	/**
	 * 获取竞赛的唯一标识符.
	 * @return 竞赛的唯一标识符
	 */
	public long getContestId() {
		return contestId;
	}

	/**
	 * 设置竞赛的唯一标识符.
	 * @param contestId - 竞赛的唯一标识符
	 */
	public void setContestId(long contestId) {
		this.contestId = contestId;
	}
	
	/**
	 * 获取竞赛的名称.
	 * @return 竞赛的名称
	 */
	public String getContestName() {
		return contestName;
	}

	/**
	 * 设置竞赛的名称.
	 * @param contestName - 竞赛的名称
	 */
	public void setContestName(String contestName) {
		this.contestName = contestName;
	}

	/**
	 * 获取竞赛说明.
	 * @return 竞赛的说明
	 */
	public String getContestNotes() {
		return contestNotes;
	}

	/**
	 * 设置竞赛说明.
	 * @param contestNotes - 竞赛的说明
	 */
	public void setContestNotes(String contestNotes) {
		this.contestNotes = contestNotes;
	}

	/**
	 * 获取竞赛的开始时间.
	 * @return 竞赛的开始时间
	 */
	public Date getStartTime() {
		return startTime;
	}

	/**
	 * 设置竞赛的开始时间.
	 * @param startTime - 竞赛的开始时间
	 */
	public void setStartTime(Date startTime) {
		this.startTime = startTime;
	}

	/**
	 * 获取竞赛的结束时间.
	 * @return 竞赛的结束时间
	 */
	public Date getEndTime() {
		return endTime;
	}

	/**
	 * 设置竞赛的结束时间.
	 * @param endTime - 竞赛的结束时间
	 */
	public void setEndTime(Date endTime) {
		this.endTime = endTime;
	}

	/**
	 * 获取竞赛的赛制.
	 * @return 竞赛的赛制
	 */
	public String getContestMode() {
		return contestMode;
	}

	/**
	 * 设置竞赛的赛制.
	 * @param contestMode - 竞赛的赛制
	 */
	public void setContestMode(String contestMode) {
		this.contestMode = contestMode;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	public int hashCode() {
		return (int) (contestId % 37 + contestId % 1001);
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	public boolean equals(Object o) {
		if ( !(o instanceof Contest) ) {
			return false;
		}
		Contest c = (Contest)o;
		return c.hashCode() == hashCode();
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		return String.format("Contest [ID=%d, Name=%s, StartTime=%s, EndTime=%s, Mode=%s]", 
				new Object[] { contestId, contestName, startTime, endTime, contestMode });
	}

	/**
	 * 获取竞赛中包含的试题.
	 * @return 竞赛中包含的试题
	 */
	public String getProblems() {
		return problems;
	}

	/**
	 * 设置竞赛中包含的试题.
	 * @param problems - 竞赛中包含的试题
	 */
	public void setProblems(String problems) {
		this.problems = problems;
	}

	/**
	 * 竞赛的唯一标识符.
	 */
	private long contestId;
	
	/**
	 * 竞赛的名称.
	 */
	private String contestName;

	/**
	 * 竞赛的说明.
	 */
	private String contestNotes;
	
	/**
	 * 竞赛的开始时间.
	 */
	private Date startTime;
	
	/**
	 * 竞赛的结束时间.
	 */
	private Date endTime;
	
	/**
	 * 竞赛的赛制.
	 * 可选择的赛制有: ACM和OI.
	 */
	private String contestMode;
	
	/**
	 * 竞赛中包含的试题 (JSON格式的字符串).
	 */
	private String problems;

	/**
	 * 竞赛状态.
	 * 分别表示未开始、进行中和已结束.
	 */
	public enum CONTEST_STATUS { READY, LIVE, DONE }
}
