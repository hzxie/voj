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
import org.verwandlung.voj.web.model.Contest;

import java.util.List;

/**
 * Contest Data Access Object.
 *
 * @author Haozhe Xie
 */
public interface ContestMapper {
	/**
	 * [此方法仅供管理员使用]
	 * 获取竞赛的总数量.
	 * @param keyword - 竞赛的关键词
	 * @return 竞赛的总数量
	 */
	long getNumberOfContests(@Param("keyword") String keyword);

	/**
	 * 获取竞赛列表.
	 * @param keyword - 竞赛的关键词
	 * @param offset - 起始竞赛的偏移量(offset)
	 * @param limit - 需要获取竞赛的数量
	 * @return 预期的竞赛对象
	 */
	List<Contest> getContests(@Param("keyword") String keyword, 
		@Param("offset") long offset, @Param("limit") int limit);

	/**
	 * 根据竞赛的唯一标识符获取竞赛.
	 * @param contestId - 竞赛的唯一标识符
	 * @return 预期的竞赛对象
	 */
	Contest getContest(long contestId);

	/**
	 * 创建竞赛.
	 * @param contest - 待创建的竞赛对象
	 */
	int createContest(Contest contest);

	/**
	 * 更新竞赛.
	 * @param contest - 待更新的竞赛对象
	 */
	int updateContest(Contest contest);

	/**
	 * 删除竞赛.
	 * @param contestId - 竞赛的唯一标识符
	 */
	int deleteContest(long contestId);
}
