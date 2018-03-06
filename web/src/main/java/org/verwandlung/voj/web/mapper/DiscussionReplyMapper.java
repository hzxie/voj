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
import org.verwandlung.voj.web.model.DiscussionReply;

import java.util.List;

/**
 * DiscussionReply Data Access Object.
 *
 * @author Haozhe Xie
 */
public interface DiscussionReplyMapper {
	/**
	 * 根据讨论回复的唯一标识符获取DiscussionReply对象.
	 * @param discussionReplyId - 讨论回复的唯一标识符
	 * @return 预期的DiscussionReply对象或空引用
	 */
	DiscussionReply getDiscussionReplyUsingReplyId(@Param("discussionReplyId") long discussionReplyId);

	/**
	 * 获取某个讨论帖子下的全部回复, 并分页显示.
	 * @param discussionThreadId - 讨论帖子的唯一标识符
	 * @param offset 起始回复的游标
	 * @param limit 获取回复的数量
	 * @return 包含讨论话题回复的List对象
	 */
	List<DiscussionReply> getDiscussionRepliesUsingThreadId(
		@Param("discussionThreadId") long discussionThreadId,
		@Param("offset") long offset, @Param("limit") int limit);

	/**
	 * 创建讨论回复.
	 * @param discussionReply - 待创建的DiscussionReply对象
	 */
	int createDiscussionReply(DiscussionReply discussionReply);

	/**
	 * 更新讨论回复.
	 * @param discussionReply - 待更新的DiscussionReply对象
	 */
	int updateDiscussionReply(DiscussionReply discussionReply);

	/**
	 * 删除讨论回复.
	 * @param discussionReplyId - 待删除回复的唯一标识符
	 */
	int deleteDiscussionReplyUsingReplyId(long discussionReplyId);
}
