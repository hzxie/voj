package org.verwandlung.voj.web.model;

import java.util.Date;

public class Contest {
	/**
	 * Contest的默认构造函数. 
	 */
	public Contest() { }
	
	/**
	 * Contest的构造函数.
	 * @param contestName - 比赛的名称
	 * @param startTime - 比赛开始时间
	 * @param endTime - 比赛结束时间
	 * @param mode - 比赛的赛制
	 * @param problems - 比赛包含的试题
	 */
	public Contest(String contestName, Date startTime, Date endTime, 
			String mode, String problems) {
		this.contestName = contestName;
		this.startTime = startTime;
		this.endTime = endTime;
		this.contestMode = mode;
		this.problems = problems;
	}

	/**
	 * Contest的构造函数.
	 * @param contestId - 比赛的唯一标识符
	 * @param contestName - 比赛的名称
	 * @param startTime - 比赛开始时间
	 * @param endTime - 比赛结束时间
	 * @param mode - 比赛的赛制
	 * @param problems - 比赛中包含的试题
	 */
	public Contest(long contestId, String contestName, Date startTime, 
			Date endTime, String mode, String problems) {
		this(contestName, startTime, endTime, mode, problems);
		this.contestId = contestId;
	}

	/**
	 * 获取比赛的唯一标识符.
	 * @return 比赛的唯一标识符
	 */
	public long getContestId() {
		return contestId;
	}

	/**
	 * 设置比赛的唯一标识符.
	 * @param contestId - 比赛的唯一标识符
	 */
	public void setContestId(long contestId) {
		this.contestId = contestId;
	}
	
	/**
	 * 获取比赛的名称.
	 * @return 比赛的名称
	 */
	public String getContestName() {
		return contestName;
	}

	/**
	 * 设置比赛的名称.
	 * @param contestName - 比赛的名称
	 */
	public void setContestName(String contestName) {
		this.contestName = contestName;
	}

	/**
	 * 获取比赛的开始时间.
	 * @return 比赛的开始时间
	 */
	public Date getStartTime() {
		return startTime;
	}

	/**
	 * 设置比赛的开始时间.
	 * @param contestName - 比赛的开始时间
	 */
	public void setStartTime(Date startTime) {
		this.startTime = startTime;
	}

	/**
	 * 获取比赛的结束时间.
	 * @return 比赛的结束时间
	 */
	public Date getEndTime() {
		return endTime;
	}

	/**
	 * 设置比赛的结束时间.
	 * @param startTime - 比赛的结束时间
	 */
	public void setEndTime(Date endTime) {
		this.endTime = endTime;
	}

	/**
	 * 获取比赛的赛制.
	 * @return 比赛的赛制
	 */
	public String getContestMode() {
		return contestMode;
	}

	/**
	 * 设置比赛的赛制.
	 * @param contestMode - 比赛的赛制
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
	 * 获取比赛中包含的试题.
	 * @return 比赛中包含的试题
	 */
	public String getProblems() {
		return problems;
	}

	/**
	 * 设置比赛中包含的试题.
	 * @param problems - 比赛中包含的试题
	 */
	public void setProblems(String problems) {
		this.problems = problems;
	}

	/**
	 * 比赛的唯一标识符.
	 */
	private long contestId;
	
	/**
	 * 比赛的名称.
	 */
	private String contestName;
	
	/**
	 * 比赛的开始时间.
	 */
	private Date startTime;
	
	/**
	 * 比赛的结束时间.
	 */
	private Date endTime;
	
	/**
	 * 比赛的赛制.
	 * 可选择的赛制有: ACM和OI.
	 */
	private String contestMode;
	
	/**
	 * 比赛中包含的试题 (JSON格式的字符串).
	 */
	private String problems;
}
