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
