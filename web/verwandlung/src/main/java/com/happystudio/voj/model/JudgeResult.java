package com.happystudio.voj.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;

/**
 * 评测结果类的Model.
 * @author Xie Haozhe
 */
@Entity
@Table(name = "voj_judge_results")
public class JudgeResult implements Serializable {
	/**
	 * 评测结果类的默认构造函数.
	 */
	public JudgeResult() { }
	
	/**
	 * 评测结果类的构造函数.
	 * @param judgeResultID - 评测结果的唯一标识符
	 * @param judgeResultSlug - 评测结果的唯一英文缩写
	 * @param judgeResultName - 评测结果的名称
	 */
	public JudgeResult(int judgeResultID, String judgeResultSlug, String judgeResultName) {
		this.judgeResultID = judgeResultID;
		this.judgeResultSlug = judgeResultSlug;
		this.judgeResultName = judgeResultName;
	}
	
	/**
	 * 获取评测结果的唯一标识符.
	 * @return 评测结果的唯一标识符
	 */
	public int getJudgeResultID() {
		return judgeResultID;
	}

	/**
	 * 设置评测结果的唯一标识符.
	 * @param judgeResultID - 评测结果的唯一标识符
	 */
	public void setJudgeResultID(int judgeResultID) {
		this.judgeResultID = judgeResultID;
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

    /**
     * 获取评测记录集合(用于1-N关联).
     * @return 评测记录集合
     */
    public List<Submission> getSubmission() {
    	return submission;
    }
    
    /**
     * 设置评测记录集合.
     * @param submission - 评测记录集合
     */
    public void setSubmission(List<Submission> submission) {
    	this.submission = submission;
    }
    
    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return String.format("JudgeResult [ID=%d, Slug=%s, Name=%s]",
                new Object[] { judgeResultID, judgeResultSlug, judgeResultName });
    }

	/**
	 * 评测结果的唯一标识符.
	 */
    @Id
    @GeneratedValue
	@Column(name = "judge_result_id")
	private int judgeResultID;
	
	/**
	 * 评测结果的英文唯一缩写.
	 */
    @Column(name = "judge_result_slug")
	private String judgeResultSlug;
	
	/**
	 * 评测结果的名称.
	 */
    @Column(name = "judge_result_name")
	private String judgeResultName;
	
	/**
     * 评测记录集合(以便1-N关联).
     */
	@OneToMany(targetEntity = Submission.class, 
            	fetch = FetchType.LAZY, mappedBy = "judgeResult")
	private List<Submission> submission = new ArrayList<Submission>();
	
	/**
	 * 唯一的序列化标识符.
	 */
	private static final long serialVersionUID = -147228716940874954L;
}
