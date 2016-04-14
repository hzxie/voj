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
	 * @param discussionThreadCreateTime - 讨论帖子创建的时间
	 * @param problem - 讨论帖子对应的试题
	 * @param discussionTopic - 讨论帖子所属的话题
	 * @param discussionThreadTitle - 讨论帖子的标题
	 * @param discussionThreadContent - 讨论帖子的内容
	 */
	public DiscussionThread(User discussionThreadCreator, 
			Date discussionThreadCreateTime, DiscussionTopic discussionTopic,
			Problem problem, String discussionThreadTitle, 
			String discussionThreadContent) {
		this.setDiscussionThreadCreator(discussionThreadCreator);
		this.setDiscussionThreadCreateTime(discussionThreadCreateTime);
		this.setDiscussionTopic(discussionTopic);
		this.setProblem(problem);
		this.setDiscussionThreadTitle(discussionThreadTitle);
		this.setDiscussionThreadContent(discussionThreadContent);
	}
	
	/**
	 * DiscussionThread的构造函数.
	 * @param discussionThreadId - 讨论帖子的唯一标识符
	 * @param discussionThreadCreator - 讨论帖子的创建用户
	 * @param discussionThreadCreateTime - 讨论帖子创建的时间
	 * @param problem - 讨论帖子对应的试题
	 * @param discussionTopic - 讨论帖子所属的话题
	 * @param discussionThreadTitle - 讨论帖子的标题
	 * @param discussionThreadContent - 讨论帖子的内容
	 */
	public DiscussionThread(long discussionThreadId, 
			User discussionThreadCreator, Date discussionThreadCreateTime,
			 Problem problem, DiscussionTopic discussionTopic, 
			String discussionThreadTitle, String discussionThreadContent) {
		this(discussionThreadCreator, discussionThreadCreateTime, 
				discussionTopic, problem, discussionThreadTitle,
				discussionThreadContent);
		this.setDiscussionThreadId(discussionThreadId);
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
	 * 获取讨论帖子创建的时间.
	 * @return 讨论帖子创建的时间
	 */
	public Date getDiscussionThreadCreateTime() {
		return discussionThreadCreateTime;
	}

	/**
	 * 设置讨论帖子创建的时间.
	 * @param discussionThreadCreateTime - 讨论帖子创建的时间
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
	 * 获取讨论帖子的内容.
	 * @return 讨论帖子的内容
	 */
	public String getDiscussionThreadContent() {
		return discussionThreadContent;
	}

	/**
	 * 设置讨论帖子的内容.
	 * @param discussionThreadContent - 讨论帖子的内容
	 */
	public void setDiscussionThreadContent(String discussionThreadContent) {
		this.discussionThreadContent = discussionThreadContent;
	}
	
	public String toString() {
		return String.format("DiscussionThread [ThreadID=%d, Creator={%s}, CreateTime=%s, Problem={%s}, Title=%s, Content=%s]", 
				new Object[] { discussionThreadId, discussionThreadCreator, 
						discussionThreadCreateTime, problem, 
						discussionThreadTitle, discussionThreadContent });
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
	 * 讨论帖子创建的时间.
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
	 * 讨论帖子的内容.
	 */
	private String discussionThreadContent;
	
	/**
	 * 唯一的序列化标识符.
	 */
	private static final long serialVersionUID = -616554199104563280L;
}
