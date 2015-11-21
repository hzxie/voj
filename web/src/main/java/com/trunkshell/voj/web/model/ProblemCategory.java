package com.trunkshell.voj.web.model;

import java.io.Serializable;

public class ProblemCategory implements Serializable {
	/**
	 * ProblemCategory的默认构造函数
	 */
	public ProblemCategory() { }
	
	/**
	 * 获取试题分类的唯一标识符.
	 * @return 试题分类的唯一标识符
	 */
	public long getProblemCategoryId() {
		return ProblemCategoryId;
	}

	/**
	 * 设置试题分类的唯一标识符.
	 * @param problemCategoryId - 试题分类的唯一标识符
	 */
	public void setProblemCategoryId(long problemCategoryId) {
		ProblemCategoryId = problemCategoryId;
	}
	
	/**
	 * 获取试题分类的唯一英文缩写.
	 * @return 试题分类的唯一英文缩写
	 */
	public String getProblemCategorySlug() {
		return problemCategorySlug;
	}

	/**
	 * 设置试题分类的唯一英文缩写.
	 * @param problemCategorySlug - 试题分类的唯一英文缩写
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

	/**
	 * 获取试题分类的父类的唯一标识符.
	 * @return 试题分类的父类的唯一标识符
	 */
	public long getParentProblemCategoryId() {
		return parentProblemCategoryId;
	}

	/**
	 * 设置试题分类的父类的唯一标识符.
	 * @param parentProblemCategory - 试题分类的父类的唯一标识符
	 */
	public void setParentProblemCategory(long parentProblemCategoryId) {
		this.parentProblemCategoryId = parentProblemCategoryId;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	public int hashCode() {
		return (int) ProblemCategoryId;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	public boolean equals(Object obj) {
		if ( obj instanceof ProblemCategory ) {
			ProblemCategory anotherCategory = (ProblemCategory)obj;
            return anotherCategory.getProblemCategoryId() == ProblemCategoryId;
        }
        return false;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		return String.format("ProblemCategory [ID=%d, Slug=%s, Name=%s, ParentID=%d]", 
				new Object[] { ProblemCategoryId, problemCategorySlug, problemCategoryName, parentProblemCategoryId });
	}

	/**
	 * 试题分类的唯一标识符.
	 */
	private long ProblemCategoryId;
	
	/**
	 * 试题分类的唯一英文缩写.
	 */
	private String problemCategorySlug;
	
	/**
	 * 试题分类的名称.
	 */
	private String problemCategoryName;
	
	/**
	 * 试题分类的父类.
	 */
	private long parentProblemCategoryId;

	/**
	 * 唯一的序列化标识符.
	 */
	private static final long serialVersionUID = 7093703712360669823L;
}
