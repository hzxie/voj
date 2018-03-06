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
import java.util.Date;

/**
 * 讨论帖子的Model.
 * 
 * @author Haozhe Xie
 */
public class DiscussionThread implements Serializable {
	/**
	 * DiscussionThread的默认构造函数.
	 */
	public DiscussionThread() { }
	
	/**
	 * DiscussionThread的构造函数.
	 * @param discussionThreadCreator - 讨论帖子的创建用户
	 * @param problem - 讨论帖子对应的试题
	 * @param discussionTopic - 讨论帖子所属的话题
	 * @param discussionThreadTitle - 讨论帖子的标题
	 */
	public DiscussionThread(User discussionThreadCreator, 
			DiscussionTopic discussionTopic, Problem problem,
			String discussionThreadTitle) {
		this.discussionThreadCreator = discussionThreadCreator;
		this.discussionTopic = discussionTopic;
		this.problem = problem;
		this.discussionThreadTitle = discussionThreadTitle;
	}
	
	/**
	 * DiscussionThread的构造函数.
	 * @param discussionThreadId - 讨论帖子的唯一标识符
	 * @param discussionThreadCreator - 讨论帖子的创建用户
	 * @param problem - 讨论帖子对应的试题
	 * @param discussionTopic - 讨论帖子所属的话题
	 * @param discussionThreadTitle - 讨论帖子的标题
	 */
	public DiscussionThread(long discussionThreadId, 
			User discussionThreadCreator, Problem problem,
			DiscussionTopic discussionTopic, String discussionThreadTitle) {
		this(discussionThreadCreator, discussionTopic, problem, discussionThreadTitle);
		this.discussionThreadId = discussionThreadId;
	}

	/**
	 * 获取讨论帖子的唯一标识符.
	 * @return 讨论帖子的唯一标识符
	 */
	public long getDiscussionThreadId() {
		return discussionThreadId;
	}

	/**
	 * 设置讨论帖子的唯一标识符.
	 * @param discussionThreadId - 讨论帖子的唯一标识符
	 */
	public void setDiscussionThreadId(long discussionThreadId) {
		this.discussionThreadId = discussionThreadId;
	}

	/**
	 * 获取讨论帖子的创建用户.
	 * @return 讨论帖子的创建用户
	 */
	public User getDiscussionThreadCreator() {
		return discussionThreadCreator;
	}

	/**
	 * 设置讨论帖子的创建用户.
	 * @param discussionThreadCreator - 讨论帖子的创建用户
	 */
	public void setDiscussionThreadCreator(User discussionThreadCreator) {
		this.discussionThreadCreator = discussionThreadCreator;
	}

	/**
	 * 获取讨论帖子的创建时间.
	 * @return 讨论帖子的创建时间
	 */
	public Date getDiscussionThreadCreateTime() {
		return discussionThreadCreateTime;
	}

	/**
	 * 设置讨论帖子的创建时间.
	 * @param discussionThreadCreateTime - 讨论帖子的创建时间
	 */
	public void setDiscussionThreadCreateTime(Date discussionThreadCreateTime) {
		this.discussionThreadCreateTime = discussionThreadCreateTime;
	}

	/**
	 * 获取讨论帖子对应的试题.
	 * @return 讨论帖子对应的试题
	 */
	public Problem getProblem() {
		return problem;
	}

	/**
	 * 设置讨论帖子对应的试题.
	 * @param problem - 讨论帖子对应的试题
	 */
	public void setProblem(Problem problem) {
		this.problem = problem;
	}

	/**
	 * 获取讨论帖子所属的话题.
	 * @return 讨论帖子所属的话题
	 */
	public DiscussionTopic getDiscussionTopic() {
		return discussionTopic;
	}

	/**
	 * 设置讨论帖子所属的话题.
	 * @param discussionTopic - 讨论帖子所属的话题
	 */
	public void setDiscussionTopic(DiscussionTopic discussionTopic) {
		this.discussionTopic = discussionTopic;
	}

	/**
	 * 获取讨论帖子的标题.
	 * @return 讨论帖子的标题
	 */
	public String getDiscussionThreadTitle() {
		return discussionThreadTitle;
	}

	/**
	 * 设置讨论帖子的标题.
	 * @param discussionThreadTitle - 讨论帖子的标题
	 */
	public void setDiscussionThreadTitle(String discussionThreadTitle) {
		this.discussionThreadTitle = discussionThreadTitle;
	}

	/**
	 * 获取最后一个讨论回复.
	 * @return 最后一个讨论回复
	 */
	public DiscussionReply getLatestDiscussionReply() {
		return latestDiscussionReply;
	}

	/**
	 * 设置最新的讨论回复.
	 * @param latestDiscussionReply - 最新的讨论回复
	 */
	public void setLatestDiscussionReply(DiscussionReply latestDiscussionReply) {
		this.latestDiscussionReply = latestDiscussionReply;
	}

	/**
	 * 获取讨论帖子的回复数.
	 * @return 讨论帖子的回复数
	 */
	public long getNumberOfReplies() {
		return numberOfReplies;
	}

	/**
	 * 设置讨论帖子的回复数
	 * @param numberOfReplies - 讨论帖子的回复数
	 */
	public void setNumberOfReplies(long numberOfReplies) {
		this.numberOfReplies = numberOfReplies;
	}

	public String toString() {
		return String.format("DiscussionThread [ThreadID=%d, Creator={%s}, Problem={%s}, Title=%s]",
				new Object[] { discussionThreadId, discussionThreadCreator, 
						problem, discussionThreadTitle });
	}

	/**
	 * 讨论帖子的唯一标识符.
	 */
	private long discussionThreadId;
	
	/**
	 * 讨论帖子的创建用户.
	 */
	private User discussionThreadCreator;

	/**
	 * 讨论帖子的创建时间.
	 */
	private Date discussionThreadCreateTime;
	
	/**
	 * 讨论帖子对应的试题. 
	 */
	private Problem problem;
	
	/**
	 * 讨论帖子所属的话题.
	 */
	private DiscussionTopic discussionTopic;
	
	/**
	 * 讨论帖子的标题.
	 */
	private String discussionThreadTitle;

	/**
	 * 讨论帖子最新的回复.
	 */
	private DiscussionReply latestDiscussionReply;
	
	/**
	 * 讨论帖子的回复数量.
	 */
	private long numberOfReplies;
	
	/**
	 * 唯一的序列化标识符.
	 */
	private static final long serialVersionUID = -616554199104563280L;
}
