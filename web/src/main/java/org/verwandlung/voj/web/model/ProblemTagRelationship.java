package org.verwandlung.voj.web.model;

import java.io.Serializable;

/**
 * 试题标签关系的 Model.
 * 对应数据库中的voj_problem_tag_relationships数据表.
 * 
 * @author Haozhe Xie
 */
public class ProblemTagRelationship implements Serializable {
	/**
	 * ProblemTagRelationship的默认构造函数.
	 */
	public ProblemTagRelationship() { }
	
	/**
	 * ProblemTagRelationship的构造函数.
	 * @param problemId - 试题的唯一标识符
	 * @param problemTagId - 试题标签的唯一标识符
	 * @param problemTagSlug - 试题标签的别名
	 * @param problemTagName - 试题标签的名称
	 */
	public ProblemTagRelationship(long problemId, long problemTagId, String problemTagSlug, String problemTagName) { 
		this.problemId = problemId;
		this.problemTagId = problemTagId;
		this.problemTagSlug = problemTagSlug;
		this.problemTagName = problemTagName;
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
	 * 获取试题标签的唯一标识符.
	 * @return 试题标签的唯一标识符
	 */
	public long getProblemTagId() {
		return problemTagId;
	}

	/**
	 * 设置试题标签的唯一标识符.
	 * @param problemTagId - 试题标签的唯一标识符
	 */
	public void setProblemTagId(long problemTagId) {
		this.problemTagId = problemTagId;
	}
	
	/**
	 * 获取试题标签的别名.
	 * @return 试题标签的别名
	 */
	public String getProblemTagSlug() {
		return problemTagSlug;
	}

	/**
	 * 设置试题标签的别名.
	 * @param problemTagSlug - 试题标签的别名
	 */
	public void setProblemTagSlug(String problemTagSlug) {
		this.problemTagSlug = problemTagSlug;
	}

	/**
	 * 获取试题标签的名称.
	 * @return 试题标签的名称
	 */
	public String getProblemTagName() {
		return problemTagName;
	}

	/**
	 * 设置试题标签的名称.
	 * @param problemTagName - 试题标签的名称
	 */
	public void setProblemTagName(String problemTagName) {
		this.problemTagName = problemTagName;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	public int hashCode() {
		return (int) problemId + (int) problemTagId;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	public boolean equals(Object obj) {
		if ( obj instanceof ProblemTagRelationship ) {
			ProblemTagRelationship anotherTagRelationship = (ProblemTagRelationship)obj;
			return anotherTagRelationship.getProblemId() == problemId && 
				anotherTagRelationship.getProblemTagId() == problemTagId;
		}
		return false;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		return String.format("ProblemTagRelationship [ProblemID=%d, TagID=%d, Slug=%s, Name=%s]", 
				new Object[] { problemId, problemTagId, problemTagSlug, problemTagName });
	}

	/**
	 * 试题的唯一标识符.
	 */
	private long problemId;

	/**
	 * 试题标签的唯一标识符.
	 */
	private long problemTagId;
	
	/**
	 * 试题标签的别名.
	 */
	private String problemTagSlug;
	
	/**
	 * 试题标签的名称.
	 */
	private String problemTagName;

	/**
	 * 唯一的序列化标识符.
	 */
	private static final long serialVersionUID = -1636820984815816993L;
}
