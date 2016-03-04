package org.verwandlung.voj.judger.model;

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
	 * @param judgeResultSlug - 评测结果的唯一英文缩写
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
	 * 获取评测结果的唯一英文缩写.
	 * @return - 评测结果的唯一英文缩写
	 */
	public String getJudgeResultSlug() {
		return judgeResultSlug;
	}

	/**
	 * 设置评测结果的唯一英文缩写.
	 * @param judgeResultSlug - 评测结果的唯一英文缩写
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
