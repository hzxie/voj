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

import org.verwandlung.voj.web.model.Problem;

/**
 * Problem Data Access Object.
 * 
 * @author Haozhe Xie
 */
@CacheNamespace(implementation = org.mybatis.caches.ehcache.EhcacheCache.class)
public interface ProblemMapper {
	/**
	 * [此方法仅供管理员使用]
	 * 获取全部试题的总数量.
	 * @return 全部试题的总数量
	 */
	long getNumberOfProblems();
	
	/**
	 * 根据筛选条件获取试题的总数量.
	 * @param keyword - 关键词
	 * @param problemCategoryId - 试题分类的唯一标识符
	 * @param isPublicOnly - 是否只筛选公开试题
	 * @return 符合筛选条件试题的总数量
	 */
	long getNumberOfProblemsUsingFilters(
			@Param("keyword") String keyword, 
			@Param("problemCategoryId") int problemCategoryId,
			@Param("isPublicOnly") boolean isPublicOnly);
	
	/**
	 * 获取第一个试题的ID.
	 * @return 第一个试题的ID
	 */
	long getLowerBoundOfProblems();
	
	/**
	 * 获取最后一个试题的ID.
	 * @return 最后一个试题的ID
	 */
	long getUpperBoundOfProblems();

	/**
	 * 获取某个试题区间内最后一个试题的ID.
	 * @param isPublicOnly - 是否只筛选公开试题
	 * @param offset - 试题唯一标识符的起始编号
	 * @param limit - 需要获取的试题的数量
	 * @return 某个试题区间内最后一个试题的ID
	 */
	long getUpperBoundOfProblemsWithLimit(
		@Param("isPublicOnly") boolean isPublicOnly,
		@Param("problemId") long offset, @Param("limit") int limit);
	
	/**
	 * 通过试题唯一标识符获取试题对象.
	 * @param problemId - 试题的唯一标识符
	 * @return 一个试题对象
	 */
	Problem getProblem(@Param("problemId") long problemId);
	
	/**
	 * 通过试题唯一标识符和关键字获取某个范围内的所有试题.
	 * @param keyword - 关键词
	 * @param problemCategoryId - 试题分类的唯一标识符
	 * @param problemTagId - 试题标签的唯一标识符
	 * @param isPublicOnly - 是否只筛选公开试题
	 * @param offset - 试题唯一标识符的起始编号
	 * @param limit - 需要获取的试题的数量
	 * @return 某个范围内的符合条件的试题
	 */
	List<Problem> getProblemsUsingFilters(
			@Param("keyword") String keyword, 
			@Param("problemCategoryId") int problemCategoryId,
			@Param("problemTagId") long problemTagId,
			@Param("isPublicOnly") boolean isPublicOnly,
			@Param("problemId") long offset, @Param("limit") int limit);
	
	/**
	 * 创建一个新的试题对象.
	 * @param problem - 试题对象
	 * @return 操作是否成功完成
	 */
	int createProblem(Problem problem);
	
	/**
	 * 更新试题信息.
	 * @param problem - 试题对象
	 * @return 操作是否成功完成
	 */
	int updateProblem(Problem problem);
	
	/**
	 * 通过试题的唯一标识符删除一个试题对象.
	 * @param problemId - 试题的唯一标识符
	 * @return 操作是否成功完成
	 */
	int deleteProblem(@Param("problemId") long problemId);
}
