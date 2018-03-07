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

import org.apache.ibatis.annotations.Param;
import org.verwandlung.voj.web.model.ContestSubmission;

import java.util.List;

/**
 * ContestSubmission Data Access Object.
 *
 * @author Haozhe Xie
 */
public interface ContestSubmissionMapper {
	/**
	 * 获取某场竞赛的提交记录.
	 * @param contestId - 竞赛的唯一标识符
	 * @return 包含竞赛提交记录(ContestSubmission)的List对象
	 */
	List<ContestSubmission> getSubmissionsOfContest(@Param("contestId") long contestId);

	/**
	 * 获取某场竞赛AC的提交记录.
	 * @param contestId - 竞赛的唯一标识符
	 * @return 包含AC竞赛提交记录(ContestSubmission)的List对象
	 */
	List<ContestSubmission> getAcceptedSubmissionsOfContest(@Param("contestId") long contestId);

	/**
	 * 获取某场竞赛某个参赛者的提交记录.
	 * @param contestId - 竞赛的唯一标识符
	 * @param  contestantUid - 参赛者用户的唯一标识符
	 * @return 包含某竞赛某参赛者提交记录(ContestSubmission)的List对象
	 */
	List<ContestSubmission> getSubmissionOfContestOfContest(
		@Param("contestId") long contestId, @Param("contestantUid") long contestantUid);

	/**
	 * 获取某场竞赛某个参赛者对某个试题的提交记录.
	 * @param contestId - 竞赛的唯一标识符
	 * @param problemId - 试题的唯一标识符
	 * @param  contestantUid - 参赛者用户的唯一标识符
	 * @return 包含某竞赛某参赛者提交记录(ContestSubmission)的List对象
	 */
	List<ContestSubmission> getSubmissionOfContestOfContestProblem(
			@Param("contestId") long contestId, @Param("problemId") long problemId,
			@Param("contestantUid") long contestantUid);

	/**
	 * 创建提交记录.
	 * @param contestSubmission - 待创建的提交记录
	 */
	int createContestSubmission(ContestSubmission contestSubmission);

	/**
	 * 删除竞赛提交记录.
	 * @param contestId - 竞赛的唯一标识符
	 * @param submissionId - 提交记录的唯一标识符
	 */
	int deleteContestSubssion(@Param("contestId") long contestId, @Param("submissionId") long submissionId);
}
