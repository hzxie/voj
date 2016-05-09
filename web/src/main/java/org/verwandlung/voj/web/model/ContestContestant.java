package org.verwandlung.voj.web.model;

import java.util.List;

public class ContestContestant {
	/**
	 * ContestContestant的默认构造函数.
	 */
	public ContestContestant() { }
	
	/**
	 * ContestContestant的构造函数.
	 * @param contest - 比赛
	 * @param contestant - 参赛者
	 */
	public ContestContestant(Contest contest, User contestant) {
		this.contest = contest;
		this.contestant = contestant;
	}

	/**
	 * 获取比赛对象.
	 * @return 比赛对象
	 */
	public Contest getContest() {
		return contest;
	}

	/**
	 * 设置比赛对象.
	 * @param contest - 比赛对象
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
	public int getTime() {
		return time;
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
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		return String.format("ContestContestant [Contest={%s}, Contestant={%s}, Score=%s, Time=%s (ms)]", 
				new Object[] { contest, contestant, score, time });
	}

	/**
	 * 比赛.
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
	private int time;
	
	/**
	 * 代码片段 (JSON格式).
	 * 用于OI赛制中中临时保存参赛者的代码.
	 */
	private String codeSnippet;
}
