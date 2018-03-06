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
package org.verwandlung.voj.web.mapper;

import java.util.List;

import org.apache.ibatis.annotations.CacheNamespace;
import org.apache.ibatis.annotations.Param;
import org.verwandlung.voj.web.model.ProblemTag;
import org.verwandlung.voj.web.model.ProblemTagRelationship;

/**
 * ProblemTag Data Access Object.
 * 
 * @author Haozhe Xie
 */
@CacheNamespace(implementation = org.mybatis.caches.ehcache.EhcacheCache.class)
public interface ProblemTagMapper {
	/**
	 * 获取全部的试题标签.
	 * @return 包含试题标签的列表
	 */
	List<ProblemTag> getProblemTags();
	
	/**
	 * 通过试题标签的唯一标识符获取试题标签对象.
	 * @param problemTagId - 试题标签的唯一标识符
	 * @return 预期的试题标签对象或空引用
	 */
	ProblemTag getProblemTagUsingTagId(long problemTagId);
	
	/**
	 * 通过试题的唯一标识符获取试题标签对象的列表.
	 * @param problemId - 试题的唯一标识符
	 * @return 预期的试题标签对象列表
	 */
	List<ProblemTag> getProblemTagsUsingProblemId(long problemId);

	/**
	 * 获取某个区间内各试题的标签.
	 * @param problemIdLowerBound - 试题ID区间的下界
	 * @param problemIdUpperBound - 试题ID区间的上界
	 * @return 包含试题标签信息的列表
	 */
	List<ProblemTagRelationship> getProblemTagsOfProblems(
			@Param(value = "problemIdLowerBound") long problemIdLowerBound,
			@Param(value = "problemIdUpperBound") long problemIdUpperBound);

	/**
	 * 通过试题标签的别名获取试题标签对象.
	 * @param problemTagSlug - 试题标签的别名
	 * @return 预期的试题标签对象或空引用
	 */
	ProblemTag getProblemTagUsingTagSlug(String problemTagSlug);
	
	/**
	 * 创建试题标签对象.
	 * @param problemTag - 待创建的试题标签对象
	 */
	int createProblemTag(ProblemTag problemTag);

	/**
	 * 创建试题及试题标签的关系.
	 * @param problemId - 试题的唯一标识符
	 * @param problemTag - 试题标签对象
	 */
	int createProblemTagRelationship(@Param(value="problemId") long problemId, @Param(value="problemTag") ProblemTag problemTag);
	
	/**
	 * 更新试题标签对象.
	 * @param problemTag - 待更新的试题标签对象
	 */
	int updateProblemTag(ProblemTag problemTag);
	
	/**
	 * 删除试题标签对象.
	 * @param problemTagId - 待删除试题标签对象的唯一标识符
	 */
	int deleteProblemTagUsingTagId(long problemTagId);

	/**
	 * 删除试题的全部标签.
	 * @param problemId - 试题的唯一标识符
	 */
	int deleteProblemTagRelationship(long problemId);
}
