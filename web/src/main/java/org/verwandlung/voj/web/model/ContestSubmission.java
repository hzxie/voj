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