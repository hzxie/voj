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
@Table(name = "voj_runtime_results")
public class RuntimeResult implements Serializable {
	/**
	 * 评测结果类的默认构造函数.
	 */
	public RuntimeResult() { }
	
	/**
	 * 评测结果类的构造函数.
	 * @param runtimeResultID - 评测结果的唯一标识符
	 * @param runtimeResultSlug - 评测结果的唯一英文缩写
	 * @param runtimeResultName - 评测结果的名称
	 */
	public RuntimeResult(int runtimeResultID, String runtimeResultSlug, String runtimeResultName) {
		this.runtimeResultID = runtimeResultID;
		this.runtimeResultSlug = runtimeResultSlug;
		this.runtimeResultName = runtimeResultName;
	}
	
	/**
	 * 获取评测结果的唯一标识符.
	 * @return 评测结果的唯一标识符
	 */
	public int getRuntimeResultID() {
		return runtimeResultID;
	}

	/**
	 * 设置评测结果的唯一标识符.
	 * @param runtimeResultID - 评测结果的唯一标识符
	 */
	public void setRuntimeResultID(int runtimeResultID) {
		this.runtimeResultID = runtimeResultID;
	}

	/**
	 * 获取评测结果的唯一英文缩写.
	 * @return - 评测结果的唯一英文缩写
	 */
	public String getRuntimeResultSlug() {
		return runtimeResultSlug;
	}

	/**
	 * 设置评测结果的唯一英文缩写.
	 * @param runtimeResultSlug - 评测结果的唯一英文缩写
	 */
	public void setRuntimeResultSlug(String runtimeResultSlug) {
		this.runtimeResultSlug = runtimeResultSlug;
	}

	/**
	 * 获取评测结果的名称.
	 * @return 评测结果的名称
	 */
	public String getRuntimeResultName() {
		return runtimeResultName;
	}

	/**
	 * 设置评测结果的名称.
	 * @param runtimeResultName - 评测结果的名称
	 */
	public void setRuntimeResultName(String runtimeResultName) {
		this.runtimeResultName = runtimeResultName;
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
        return String.format("RuntimeResult [ID=%d, Slug=%s, Name=%s]",
                new Object[] { runtimeResultID, runtimeResultSlug, runtimeResultName });
    }

	/**
	 * 评测结果的唯一标识符.
	 */
    @Id
    @GeneratedValue
	@Column(name = "runtime_result_id")
	private int runtimeResultID;
	
	/**
	 * 评测结果的英文唯一缩写.
	 */
    @Column(name = "runtime_result_slug")
	private String runtimeResultSlug;
	
	/**
	 * 评测结果的名称.
	 */
    @Column(name = "runtime_result_name")
	private String runtimeResultName;
	
	/**
     * 评测记录集合(以便1-N关联).
     */
	@OneToMany(targetEntity = Submission.class, 
            	fetch = FetchType.LAZY, mappedBy = "runtimeResult")
	private List<Submission> submission = new ArrayList<Submission>();
	
	/**
	 * 唯一的序列化标识符.
	 */
	private static final long serialVersionUID = -147228716940874954L;
}
