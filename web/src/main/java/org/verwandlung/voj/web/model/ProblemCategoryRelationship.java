package org.verwandlung.voj.web.model;

import java.io.Serializable;

/**
 * 试题分类关系的 Model.
 * 对应数据库中的voj_problem_category_relationships数据表.
 * 
 * @author Haozhe Xie
 */
public class ProblemCategoryRelationship implements Serializable {
	/**
	 * ProblemCategoryRelationship的默认构造函数.
	 */
	public ProblemCategoryRelationship() { }
	
	/**
	 * ProblemCategoryRelationship的构造函数.
	 * @param problemId - 试题的唯一标识符
	 * @param problemCategoryId - 试题分类的唯一标识符
	 * @param problemCategorySlug - 试题分类的别名
	 * @param problemCategoryName - 试题分类的名称
	 */
	public ProblemCategoryRelationship(long problemId, int problemCategoryId, String problemCategorySlug, String problemCategoryName) {
		this.problemId = problemId;
		this.problemCategoryId = problemCategoryId;
		this.problemCategorySlug = problemCategorySlug;
		this.problemCategoryName = problemCategoryName;
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
	 * 获取试题分类的唯一标识符.
	 * @return 试题分类的唯一标识符
	 */
	public int getProblemCategoryId() {
		return problemCategoryId;
	}

	/**
	 * 设置试题分类的唯一标识符.
	 * @param problemCategoryId - 试题分类的唯一标识符
	 */
	public void setProblemCategoryId(int problemCategoryId) {
		this.problemCategoryId = problemCategoryId;
	}
	
	/**
	 * 获取试题分类的别名.
	 * @return 试题分类的别名
	 */
	public String getProblemCategorySlug() {
		return problemCategorySlug;
	}

	/**
	 * 设置试题分类的别名.
	 * @param problemCategorySlug - 试题分类的别名
	 */
	public void setProblemCategorySlug(String problemCategorySlug) {
		this.problemCategorySlug = problemCategorySlug;
	}

	/**
	 * 获取试题分类的名称.
	 * @return 试题分类的名称
	 */
	public String getProblemCategoryName() {
		return problemCategoryName;
	}

	/**
	 * 设置试题分类的名称.
	 * @param problemCategoryName - 试题分类的名称
	 */
	public void setProblemCategoryName(String problemCategoryName) {
		this.problemCategoryName = problemCategoryName;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	public int hashCode() {
		return (int) problemId + (int) problemCategoryId;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	public boolean equals(Object obj) {
		if ( obj instanceof ProblemCategoryRelationship ) {
			ProblemCategoryRelationship anotherCategoryRelationship = (ProblemCategoryRelationship)obj;
			return anotherCategoryRelationship.getProblemId() == problemId && 
				anotherCategoryRelationship.getProblemCategoryId() == problemCategoryId;
		}
		return false;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		return String.format("ProblemCategoryRelationship [ProblemID=%d, CategoryID=%d, Slug=%s, Name=%s]", 
				new Object[] { problemId, problemCategoryId, problemCategorySlug, problemCategoryName });
	}

	/**
	 * 试题的唯一标识符.
	 */
	private long problemId;

	/**
	 * 试题分类的唯一标识符.
	 */
	private int problemCategoryId;
	
	/**
	 * 试题分类的别名.
	 */
	private String problemCategorySlug;
	
	/**
	 * 试题分类的名称.
	 */
	private String problemCategoryName;

	/**
	 * 唯一的序列化标识符.
	 */
	private static final long serialVersionUID = 5108958918621777517L;
}
