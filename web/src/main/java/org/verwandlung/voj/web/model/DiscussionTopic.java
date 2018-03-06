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
package org.verwandlung.voj.web.model;

import java.io.Serializable;

/**
 * 讨论话题的Model.
 *
 * @author Haozhe Xie
 */
public class DiscussionTopic implements Serializable {
	/**
	 * DiscussionTopic的默认构造函数.
	 */
	public DiscussionTopic() { }
	
	/**
	 * DiscussionTopic的构造函数.
	 * @param discussionTopicSlug - 讨论话题的别名
	 * @param discussionTopicName - 讨论话题的名称
	 * @param parentDiscussionTopicId - 父级讨论话题的唯一标识符
	 */
	public DiscussionTopic(String discussionTopicSlug, 
			String discussionTopicName, int parentDiscussionTopicId) {
		this.discussionTopicSlug = discussionTopicSlug;
		this.discussionTopicName = discussionTopicName;
		this.parentDiscussionTopicId = parentDiscussionTopicId;
	}
	
	/**
	 * DiscussionTopic的构造函数.
	 * @param discussionTopicId - 讨论话题的唯一标识符
	 * @param discussionTopicSlug - 讨论话题的别名
	 * @param discussionTopicName - 讨论话题的名称
	 * @param parentDiscussionTopicId - 父级讨论话题的唯一标识符
	 */
	public DiscussionTopic(int discussionTopicId, String discussionTopicSlug, 
			String discussionTopicName, int parentDiscussionTopicId) {
		this(discussionTopicSlug, discussionTopicName, parentDiscussionTopicId);
		this.discussionTopicId = discussionTopicId;
	}
	
	/**
	 * 获取讨论话题的唯一标识符.
	 * @return 讨论话题的唯一标识符
	 */
	public int getDiscussionTopicId() {
		return discussionTopicId;
	}

	/**
	 * 设置讨论话题的唯一标识符.
	 * @param discussionTopicId - 讨论话题的唯一标识符
	 */
	public void setDiscussionTopicId(int discussionTopicId) {
		this.discussionTopicId = discussionTopicId;
	}

	/**
	 * 获取讨论话题的别名.
	 * @return 讨论话题的别名
	 */
	public String getDiscussionTopicSlug() {
		return discussionTopicSlug;
	}

	/**
	 * 设置讨论话题的别名.
	 * @param discussionTopicSlug - 讨论话题的别名
	 */
	public void setDiscussionTopicSlug(String discussionTopicSlug) {
		this.discussionTopicSlug = discussionTopicSlug;
	}

	/**
	 * 获取讨论话题的名称.
	 * @return 讨论话题的名称
	 */
	public String getDiscussionTopicName() {
		return discussionTopicName;
	}

	/**
	 * 设置讨论话题的名称.
	 * @param getDiscussionTopicName - 讨论话题的名称
	 */
	public void setDiscussionTopicName(String getDiscussionTopicName) {
		this.discussionTopicName = getDiscussionTopicName;
	}

	/**
	 * 获取父级讨论话题的唯一标识符.
	 * @return 父级讨论话题的唯一标识符
	 */
	public int getParentDiscussionTopicId() {
		return parentDiscussionTopicId;
	}

	/**
	 * 设置父级讨论话题的唯一标识符.
	 * @param parentDiscussionTopicId - 父级讨论话题的唯一标识符
	 */
	public void setParentDiscussionTopicId(int parentDiscussionTopicId) {
		this.parentDiscussionTopicId = parentDiscussionTopicId;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return String.format("DiscussionTopic [TopicId=%d, Slug=%s, Name=%s, parentId=%d]", 
				new Object[] {discussionTopicId, discussionTopicSlug, 
						discussionTopicName, parentDiscussionTopicId});
	}

	/**
	 * 讨论话题的唯一标识符.
	 */
	private int discussionTopicId;

	/**
	 * 讨论话题的别名.
	 */
	private String discussionTopicSlug;

	/**
	 * 讨论话题的名称.
	 */
	private String discussionTopicName;

	/**
	 * 父级讨论话题的唯一标识符.
	 */
	private int parentDiscussionTopicId;
	
	/**
	 * 唯一的序列化标识符.
	 */
	private static final long serialVersionUID = 3905550118246821324L;
}
