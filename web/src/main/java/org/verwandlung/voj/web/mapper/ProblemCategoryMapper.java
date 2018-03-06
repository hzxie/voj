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
import org.verwandlung.voj.web.model.ProblemCategory;
import org.verwandlung.voj.web.model.ProblemCategoryRelationship;

/**
 * ProblemCategory Data Access Object.
 * 
 * @author Haozhe Xie
 */
@CacheNamespace(implementation = org.mybatis.caches.ehcache.EhcacheCache.class)
public interface ProblemCategoryMapper {
	/**
	 * 获取全部的试题分类.
	 * @return 包含全部试题分类的列表
	 */
	List<ProblemCategory> getProblemCategories();
	
	/**
	 * 通过试题分类的唯一标识符获取试题分类对象.
	 * @param problemCategoryId - 试题分类的唯一标识符
	 * @return 预期的试题分类对象或空引用
	 */
	ProblemCategory getProblemCategoryUsingCategoryId(int problemCategoryId);
	
	/**
	 * 获取试题的分类列表.
	 * @param problemId - 试题的唯一标识符.
	 * @return 包含试题分类的列表
	 */
	List<ProblemCategory> getProblemCategoriesUsingProblemId(long problemId);

	/**
	 * 获取某个区间内各试题的分类.
	 * @param problemIdLowerBound - 试题ID区间的下界
	 * @param problemIdUpperBound - 试题ID区间的上界
	 * @return 包含试题分类信息的列表
	 */
	List<ProblemCategoryRelationship> getProblemCategoriesOfProblems(
			@Param(value = "problemIdLowerBound") long problemIdLowerBound,
			@Param(value = "problemIdUpperBound") long problemIdUpperBound);
	
	/**
	 * 通过试题分类的别名获取试题分类对象.
	 * @param problemCategorySlug - 试题分类的别名
	 * @return 预期的试题分类对象或空引用
	 */
	ProblemCategory getProblemCategoryUsingCategorySlug(String problemCategorySlug);
	
	/**
	 * 创建试题分类对象.
	 * @param problemCategory - 待创建的试题分类对象
	 */
	int createProblemCategory(ProblemCategory problemCategory);
	
	/**
	 * 创建试题及试题分类的关系.
	 * @param problemId - 试题的唯一标识符
	 * @param problemCategory - 试题分类对象
	 */
	int createProblemCategoryRelationship(@Param(value="problemId") long problemId, @Param(value="problemCategory") ProblemCategory problemCategory);
	
	/**
	 * 更新试题分类对象.
	 * @param problemCategory - 待更新的试题分类对象
	 */
	int updateProblemCategory(ProblemCategory problemCategory);
	
	/**
	 * 删除试题分类对象.
	 * @param problemCategoryId - 待删除试题分类对象的唯一标识符
	 */
	int deleteProblemCategory(int problemCategoryId);
	
	/**
	 * 删除试题的全部分类关系.
	 * @param problemId - 试题的唯一标识符
	 */
	int deleteProblemCategoryRelationship(long problemId);
}
