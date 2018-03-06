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

import org.apache.ibatis.annotations.CacheNamespace;
import org.apache.ibatis.annotations.Param;
import org.verwandlung.voj.web.model.DiscussionThread;

import java.util.List;

/**
 * DiscussionThread Data Access Object.
 *
 * @author Haozhe Xie
 */
@CacheNamespace(implementation = org.mybatis.caches.ehcache.EhcacheCache.class)
public interface DiscussionThreadMapper {
	/**
	 * 获取全部的讨论帖子
	 * @param problemId - 试题的唯一标识符
	 * @param discussionTopicId - 讨论话题的唯一标识符
	 * @param offset - 讨论帖子的起始ID
	 * @param limit - 要获取帖子的数量
	 * @return 包含讨论帖子对象的列表
	 */
	List<DiscussionThread> getDiscussionThreads(
			@Param("problemId") long problemId,
			@Param("discussionTopicId") int discussionTopicId,
			@Param("offset") long offset, @Param("limit") int limit);

	/**
	 * 获取某个试题的题解讨论.
	 * 题解讨论在创建试题时自动创建, 即为该试题相关的第1个讨论.
	 * @param problemId - 试题的唯一标识符
	 * @return 某个试题的题解讨论DiscussionThread对象
	 */
	DiscussionThread getSolutionThreadOfProblem(@Param("problemId") long problemId);

	/**
	 * 根据讨论帖子的唯一标识符获取讨论帖子对象.
	 * @param  discussionThreadId - 讨论帖子的唯一标识符
	 * @return 对应的讨论帖子对象
	 */
	DiscussionThread getDiscussionThreadUsingThreadId(@Param("discussionThreadId") long discussionThreadId);

	/**
	 * 创建讨论帖子.
	 * @param discussionThread - 待创建的讨论帖子
	 */
	int createDiscussionThread(DiscussionThread discussionThread);

	/**
	 * 更新讨论帖子.
	 * @param discussionThread - 待更新的讨论帖子
	 */
	int updateDiscussionThread(DiscussionThread discussionThread);

	/**
	 * 删除讨论帖子.
	 * @param discussionThreadId - 待删除讨论帖子的唯一标识符
	 */
	int deleteDiscussionThreadUsingThreadId(long discussionThreadId);
}