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
import java.util.Map;

import org.apache.ibatis.annotations.CacheNamespace;
import org.apache.ibatis.annotations.Param;

import org.verwandlung.voj.web.model.Submission;

/**
 * Submission Data Access Object.
 * 
 * @author Haozhe Xie
 */
@CacheNamespace(implementation = org.mybatis.caches.ehcache.EhcacheCache.class)
public interface SubmissionMapper {
	/**
	 * [此方法仅供管理员使用]
	 * 获取指定时间内提交的数量.
	 * @param startTime - 统计起始时间
	 * @param endTime - 统计结束时间
	 * @return 指定时间内提交的数量
	 */
	long getNumberOfSubmissionsUsingDate(@Param("startTime") String startTime, @Param("endTime") String endTime);

	/**
	 * 获取某个用户对某个试题的提交记录的数量.
	 * @param problemId - 试题的唯一标识符
	 * @param username - 用户的用户名
	 * @return 某个用户对某个试题提交的数量
	 */
	long getNumberOfSubmissionsUsingProblemIdAndUsername(@Param("problemId") long problemId, @Param("username") String username);

	/**
	 * [此方法仅供管理员使用]
	 * 获取某种编程语言的提交数量.
	 * @param languageId - 编程语言的唯一标识符
	 * @return 某种编程语言的提交数量
	 */
	long getNumberOfSubmissionsUsingLanguage(@Param("languageId") int languageId);
	
	/**
	 * 获取指定时间内提交的数量, 并按月份汇总.
	 * @param  startTime - 统计起始时间
	 * @param  endTime - 统计结束时间
	 * @param  uid - 用户的唯一标识符
	 * @param  isAcceptedOnly - 是否只统计通过的提交记录
	 * @return 包含月份和提交次数的键值对 Map
	 */
	List<Map<String, Object>> getNumberOfSubmissionsGroupByMonth(@Param("startTime") String startTime, @Param("endTime") String endTime, @Param("uid") long uid, @Param("isAcceptedOnly") boolean isAcceptedOnly);
	
	/**
	 * 获取指定时间内提交的数量, 并按天数汇总.
	 * @param  startTime - 统计起始时间
	 * @param  endTime - 统计结束时间
	 * @param  uid - 用户的唯一标识符
	 * @param  isAcceptedOnly - 是否只统计通过的提交记录
	 * @return 包含日期和提交次数的键值对 Map
	 */
	List<Map<String, Object>> getNumberOfSubmissionsGroupByDay(@Param("startTime") String startTime, @Param("endTime") String endTime, @Param("uid") long uid, @Param("isAcceptedOnly") boolean isAcceptedOnly);
	
	/**
	 * 获取最新提交记录的唯一标识符
	 * @return 最新提交记录的唯一标识符
	 */
	long getLatestSubmissionId();
	
	/**
	 * 通过评测记录唯一标识符获取试题对象.
	 * @param submissionId - 评测记录的唯一标识符
	 * @return 一个评测记录对象
	 */
	Submission getSubmission(@Param("submissionId") long submissionId);
	
	/**
	 * 获取某个用户对某个试题的提交记录.
	 * @param problemId - 试题的唯一标识符
	 * @param username - 用户的用户名
	 * @param limit - 每次加载评测记录的数量
	 * @return 某个范围内的所有提交记录
	 */
	List<Submission> getSubmissions(@Param("problemId") long problemId, @Param("username") String username, @Param("limit") int limit);
	
	/**
	 * 获取某个用户对某个试题的提交记录.
	 * @param problemId - 试题的唯一标识符
	 * @param username - 用户的用户名
	 * @param offset - 试题唯一标识符的起始编号
	 * @param limit - 每次加载评测记录的数量
	 * @return 某个范围内的所有提交记录
	 */
	List<Submission> getSubmissionsUsingOffset(@Param("problemId") long problemId, @Param("username") String username, @Param("submissionId") long offset, @Param("limit") int limit);
	
	/**
	 * 获取某个用户对某个试题的提交记录.
	 * @param problemId - 试题的唯一标识符
	 * @param username - 用户的用户名
	 * @param offset - 试题唯一标识符的起始编号
	 * @param limit - 每次加载评测记录的数量
	 * @return 某个范围内的所有提交记录
	 */
	List<Submission> getLatestSubmissionsUsingOffset(@Param("problemId") long problemId, @Param("username") String username, @Param("submissionId") long offset, @Param("limit") int limit);
	
	/**
	 * 获取某个用户对某个试题的提交记录.
	 * @param problemId - 试题的唯一标识符
	 * @param uid - 用户的唯一标识符
	 * @param limit - 每次加载评测记录的数量
	 * @return 某个用户对某个试题的提交记录
	 */
	List<Submission> getSubmissionUsingProblemIdAndUserId(@Param("problemId") long problemId, @Param("uid") long uid, @Param("limit") int limit);
	
	/**
	 * 获取某个用户在某个试题ID区间段内的最新的评测结果.
	 * @param uid - 用户的唯一标识符
	 * @param problemIdLowerBound - 试题ID区间的下界
	 * @param problemIdUpperBound - 试题ID区间的上界
	 * @return 某个试题ID区间段内的最新的评测结果
	 */
	List<Submission> getLatestSubmissionOfProblems(@Param("uid") long uid, @Param("problemIdLowerBound") long problemIdLowerBound, @Param("problemIdUpperBound") long problemIdUpperBound);
	
	/**
	 * 获取某个用户在某个试题ID区间段内的通过的评测结果.
	 * @param uid - 用户的唯一标识符
	 * @param problemIdLowerBound - 试题ID区间的下界
	 * @param problemIdUpperBound - 试题ID区间的上界
	 * @return 某个试题ID区间段内的通过的评测结果
	 */
	List<Submission> getAcceptedSubmissionOfProblems(@Param("uid") long uid, @Param("problemIdLowerBound") long problemIdLowerBound, @Param("problemIdUpperBound") long problemIdUpperBound);
	
	/**
	 * 获取某个用户通过(Accepted)提交记录的数量.
	 * @param uid - 用户的唯一标识符
	 * @return 某个用户通过(Accepted)提交记录的数量
	 */
	long getAcceptedSubmissionUsingUserId(@Param("uid") long uid);
	
	/**
	 * 获取某个用户全部提交记录的数量.
	 * @param uid - 用户的唯一标识符
	 * @return 某个用户全部提交记录的数量
	 */
	long getTotalSubmissionUsingUserId(@Param("uid") long uid);
	
	/**
	 * 创建提交记录.
	 * @param submission - 待创建的提交记录对象
	 */
	int createSubmission(Submission submission);
	
	/**
	 * 更新提交记录.
	 * @param submission - 待更新的提交记录对象
	 */
	int updateSubmission(Submission submission);
	
	/**
	 * 通过提交记录的唯一标识符删除提交记录.
	 * @param submissionId - 提交记录的唯一标识符
	 */
	int deleteSubmission(@Param("submissionId") long submissionId);
}
