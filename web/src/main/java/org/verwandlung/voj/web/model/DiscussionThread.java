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
	 * @param discussionThreadVotes - 讨论帖子的投票结果
	 */
	public DiscussionThread(User discussionThreadCreator, 
			Date discussionThreadCreateTime, DiscussionTopic discussionTopic,
			Problem problem, String discussionThreadTitle, 
			String discussionThreadContent, String discussionThreadVotes) {
		this.discussionThreadCreator = discussionThreadCreator;
		this.discussionThreadCreateTime = discussionThreadCreateTime;
		this.discussionTopic = discussionTopic;
		this.problem = problem;
		this.discussionThreadTitle = discussionThreadTitle;
		this.discussionThreadContent = discussionThreadContent;
		this.discussionThreadVotes = discussionThreadVotes;
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
	 * @param discussionThreadVote - 讨论帖子的投票结果
	 */
	public DiscussionThread(long discussionThreadId, 
			User discussionThreadCreator, Date discussionThreadCreateTime,
			 Problem problem, DiscussionTopic discussionTopic, 
			String discussionThreadTitle, String discussionThreadContent, 
			String discussionThreadVote) {
		this(discussionThreadCreator, discussionThreadCreateTime, 
				discussionTopic, problem, discussionThreadTitle,
				discussionThreadContent, discussionThreadVote);
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
	
	/**
	 * 获取讨论帖子的投票结果.
	 * @return 讨论帖子的投票结果
	 */
	public String getDiscussionThreadVotes() {
		return discussionThreadVotes;
	}

	/**
	 * 设置讨论帖子的投票结果.
	 * @param discussionThreadVotes - 讨论帖子的投票结果
	 */
	public void setDiscussionThreadVotes(String discussionThreadVotes) {
		this.discussionThreadVotes = discussionThreadVotes;
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
		return String.format("DiscussionThread [ThreadID=%d, Creator={%s}, CreateTime=%s, Problem={%s}, Title=%s]", 
				new Object[] { discussionThreadId, discussionThreadCreator, 
						discussionThreadCreateTime, problem, 
						discussionThreadTitle });
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
	 * 讨论帖子的投票结果.
	 */
	private String discussionThreadVotes;

	/**
	 * 讨论帖子的回复数量.
	 */
	private long numberOfReplies;
	
	/**
	 * 唯一的序列化标识符.
	 */
	private static final long serialVersionUID = -616554199104563280L;
}
