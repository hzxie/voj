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
import org.verwandlung.voj.web.model.ContestContestant;

import java.util.List;

/**
 * ContestContestant Data Access Object.
 *
 * @author Haozhe Xie
 */
public interface ContestContestantMapper {
	/**
	 * 获取某个竞赛的参赛人数.
	 * @param contestId - 竞赛的唯一标识符
	 * @return 某个竞赛的参赛人数
	 */
	long getNumberOfContestantsOfContest(long contestId);

	/**
	 * 获取某个竞赛的参赛者列表 (OI赛制).
	 * @param contestId - 竞赛的唯一标识符
	 * @param offset - 起始参赛者的偏移量(offset)
	 * @param limit - 需要获取参赛者的数量
	 * @return 某个竞赛的参赛者列表
	 */
	List<ContestContestant> getContestantsOfContestForOi(
			@Param("contestId") long contestId,
			@Param("offset") long offset, @Param("limit") int limit);

	/**
	 * 获取某个竞赛的参赛者列表 (ACM赛制).
	 * 其中, ContestContestant中的Score字段用于存储Accepted的试题数,
	 * Time字段用于存储Rejcted的试题数 (Rejected试题数与罚时相关).
	 * @param contestId - 竞赛的唯一标识符
	 * @param offset - 起始参赛者的偏移量(offset)
	 * @param limit - 需要获取参赛者的数量
	 * @return 某个竞赛的参赛者列表
	 */
	List<ContestContestant> getContestantsOfContestForAcm(
			@Param("contestId") long contestId,
			@Param("offset") long offset, @Param("limit") int limit);

	/**
	 * 获取某个选手在某个竞赛的参赛记录.
	 * @param contestId - 竞赛的唯一标识符
	 * @param contestantUid - 参赛者的用户唯一标识符
	 * @return 对应的某个选手在某个竞赛的参赛记录
	 */
	ContestContestant getContestantOfContest(
			@Param("contestId") long contestId,
			@Param("contestantUid") long contestantUid);

	/**
	 * 创建参赛记录(用于参加竞赛).
	 * @param contestContestant - 待创建的参赛记录
	 */
	int createContestContestant(ContestContestant contestContestant);

	/**
	 * 更新参赛记录.
	 * 用于参加OI竞赛, 更新提交的源代码.
	 * @param contestContestant - 待更新的参赛记录
	 */
	int updateContestContestant(ContestContestant contestContestant);

	/**
	 * 删除参赛记录.
	 * @param contestId - 竞赛的唯一标识符
	 * @param contestantUid - 参赛者的用户唯一标识符
	 */
	int deleteContestContestant(
			@Param("contestId") long contestId,
			@Param("contestantUid") long contestantUid);
}
