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

import org.verwandlung.voj.web.model.Checkpoint;

/**
 * Checkpoint Data Access Object.
 * 
 * @author Haozhe Xie
 */
@CacheNamespace(implementation = org.mybatis.caches.ehcache.EhcacheCache.class)
public interface CheckpointMapper {
	/**
	 * [此方法仅供管理员使用]
	 * 获取系统中试题测试点的总数.
	 * @return 系统中试题测试点的总数
	 */
	long getNumberOfCheckpoints();
	
	/**
	 * 获取某个试题的全部测试点.
	 * @param problemId - 试题的唯一标识符
	 * @return 某个试题的全部测试点
	 */
	List<Checkpoint> getCheckpointsUsingProblemId(@Param("problemId") long problemId);
	
	/**
	 * [此方法仅供管理员使用]
	 * 创建测试点.
	 * @param checkpoint - 测试点
	 */
	int createCheckpoint(Checkpoint checkpoint);
	
	/**
	 * [此方法仅供管理员使用]
	 * 删除某个试题的全部测试点.
	 * @param problemId - 试题的唯一标识符
	 */
	int deleteCheckpoint(long problemId);
}
