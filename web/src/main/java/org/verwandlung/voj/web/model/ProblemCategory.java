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

import java.io.Serializable;

/**
 * 试题分类的Model.
 * 对应数据库中的voj_problem_categories数据表.
 * 
 * @author Haozhe Xie
 */
public class ProblemCategory implements Serializable {
	/**
	 * ProblemCategory的默认构造函数
	 */
	public ProblemCategory() { }
	
	/**
	 * ProblemCategory的构造函数
	 */
	public ProblemCategory(String problemCategorySlug, 
			String problemCategoryName, int parentProblemCategoryId) { 
		this.problemCategorySlug = problemCategorySlug;
		this.problemCategoryName = problemCategoryName;
		this.parentProblemCategoryId = parentProblemCategoryId;
	}
	
	/**
	 * ProblemCategory的构造函数
	 */
	public ProblemCategory(int problemCategoryId, String problemCategorySlug, 
			String problemCategoryName, int parentProblemCategoryId) { 
		this(problemCategorySlug, problemCategoryName, parentProblemCategoryId);
		this.problemCategoryId = problemCategoryId;
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

	/**
	 * 获取试题分类的父类的唯一标识符.
	 * @return 试题分类的父类的唯一标识符
	 */
	public int getParentProblemCategoryId() {
		return parentProblemCategoryId;
	}

	/**
	 * 设置试题分类的父类的唯一标识符.
	 * @param parentProblemCategoryId - 试题分类的父类的唯一标识符
	 */
	public void setParentProblemCategory(int parentProblemCategoryId) {
		this.parentProblemCategoryId = parentProblemCategoryId;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	public int hashCode() {
		return problemCategoryId;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	public boolean equals(Object obj) {
		if ( obj instanceof ProblemCategory ) {
			ProblemCategory anotherCategory = (ProblemCategory)obj;
			return anotherCategory.getProblemCategoryId() == problemCategoryId;
		}
		return false;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		return String.format("ProblemCategory [ID=%d, Slug=%s, Name=%s, ParentID=%d]", 
				new Object[] { problemCategoryId, problemCategorySlug, problemCategoryName, parentProblemCategoryId });
	}

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
	 * 试题分类的父类.
	 */
	private int parentProblemCategoryId;

	/**
	 * 唯一的序列化标识符.
	 */
	private static final long serialVersionUID = 7093703712360669823L;
}
