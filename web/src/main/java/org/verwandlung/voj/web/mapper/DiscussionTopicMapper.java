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
import org.verwandlung.voj.web.model.DiscussionTopic;

import java.util.List;

/**
 * DiscussionTopic Data Access Object.
 *
 * @author Haozhe Xie
 */
@CacheNamespace(implementation = org.mybatis.caches.ehcache.EhcacheCache.class)
public interface DiscussionTopicMapper {
	/**
	 * 获取全部的讨论话题.
	 * @return 包含全部讨论话题的List<DiscussionTopic>对象
	 */
	List<DiscussionTopic> getDiscussionTopics();

	/**
	 * 根据讨论话题的唯一标识符获取讨论话题对象.
	 * @param discussionTopicId - 讨论话题的唯一标识符
	 * @return 一个讨论话题对象
	 */
	DiscussionTopic getDiscussionTopicUsingId(int discussionTopicId);

	/**
	 * 根据讨论话题的唯一别名获取讨论话题对象.
	 * @param discussionTopicSlug - 讨论话题的别名
	 * @return 一个讨论话题对象
	 */
	DiscussionTopic getDiscussionTopicUsingSlug(String discussionTopicSlug);

	/**
	 * 创建讨论话题.
	 * @param discussionTopic - 待创建的讨论话题对象
	 */
	int createDiscussionTopic(DiscussionTopic discussionTopic);

	/**
	 * 更新讨论话题.
	 * @param discussionTopic - 待更新的讨论话题对象
	 */
	int updateDiscussionTopic(DiscussionTopic discussionTopic);

	/**
	 * 删除讨论话题.
	 * @param discussionTopicId - 待删除讨论话题的唯一标识符
	 */
	int deleteDiscussionTopicUsingId(int discussionTopicId);
}
