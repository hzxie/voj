package org.verwandlung.voj.web.model;

import java.io.Serializable;
import java.util.Date;

/**
 * 讨论回复的Model.
 * 
 * @author Haozhe Xie
 */
public class DiscussionReply implements Serializable {
	/**
	 * DiscussionReply的默认构造函数.
	 */
	public DiscussionReply() {}
	
	/**
	 * DiscussionReply的构造函数.
	 * @param discussionThreadId - 讨论帖子的唯一标识符
	 * @param discussionReplyCreator - 讨论帖子的创建者
	 * @param discussionReplyCreateTime - 讨论帖子的创建时间
	 * @param discussionReplyContent - 讨论帖子的内容
	 * @param discussionReplyVotes - 讨论帖子的投票结果
	 */
	public DiscussionReply(long discussionThreadId, User discussionReplyCreator, 
			Date discussionReplyCreateTime, String discussionReplyContent,
			String discussionReplyVotes) {
		this.discussionThreadId = discussionThreadId;
		this.discussionReplyCreator = discussionReplyCreator;
		this.discussionReplyCreateTime = discussionReplyCreateTime;
		this.discussionReplyContent = discussionReplyContent;
		this.discussionReplyVotes = discussionReplyVotes;
	}
	
	/**
	 * DiscussionReply的构造函数.
	 * @param discussionReplyId - 讨论回复的唯一标识符.
	 * @param discussionThreadId - 讨论帖子的唯一标识符
	 * @param discussionReplyCreator - 讨论帖子的创建者
	 * @param discussionReplyCreateTime - 讨论帖子的创建时间
	 * @param discussionReplyContent - 讨论帖子的内容
	 * @param discussionReplyVote - 讨论帖子的投票结果
	 */
	public DiscussionReply(long discussionReplyId, long discussionThreadId, 
			User discussionReplyCreator, Date discussionReplyCreateTime,
			String discussionReplyContent, String discussionReplyVote) {
		this(discussionThreadId, discussionReplyCreator, 
				discussionReplyCreateTime, discussionReplyContent, 
				discussionReplyVote);
		this.discussionReplyId = discussionReplyId;
	}
	
	/**
	 * 获取讨论回复的唯一标识符..
	 * @return 讨论回复的唯一标识符
	 */
	public long getDiscussionReplyId() {
		return discussionReplyId;
	}

	/**
	 * 设置讨论回复的唯一标识符.
	 * @param discussionReplyId - 讨论回复的唯一标识符
	 */
	public void setDiscussionReplyId(long discussionReplyId) {
		this.discussionReplyId = discussionReplyId;
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
	 * 获取讨论帖子的创建者.
	 * @return 讨论帖子的创建者
	 */
	public User getDiscussionReplyCreator() {
		return discussionReplyCreator;
	}

	/**
	 * 设置讨论帖子的创建者.
	 * @param discussionReplyCreator - 讨论帖子的创建者
	 */
	public void setDiscussionReplyCreator(User discussionReplyCreator) {
		this.discussionReplyCreator = discussionReplyCreator;
	}

	/**
	 * 获取讨论帖子的创建时间.
	 * @return 讨论帖子的创建时间
	 */
	public Date getDiscussionReplyCreateTime() {
		return discussionReplyCreateTime;
	}

	/**
	 * 设置讨论帖子的创建时间.
	 * @param discussionReplyCreateTime - 讨论帖子的创建时间
	 */
	public void setDiscussionReplyCreateTime(Date discussionReplyCreateTime) {
		this.discussionReplyCreateTime = discussionReplyCreateTime;
	}

	/**
	 * 获取讨论帖子的内容.
	 * @return 讨论帖子的内容
	 */
	public String getDiscussionReplyContent() {
		return discussionReplyContent;
	}

	/**
	 * 设置讨论帖子的内容.
	 * @param discussionReplyContent - 讨论帖子的内容
	 */
	public void setDiscussionReplyContent(String discussionReplyContent) {
		this.discussionReplyContent = discussionReplyContent;
	}
	
	/**
	 * 获取讨论帖子的投票结果.
	 * @return 讨论帖子的投票结果
	 */
	public String getDiscussionReplyVotes() {
		return discussionReplyVotes;
	}

	/**
	 * 设置讨论帖子的投票结果.
	 * @param discussionReplyVote - 讨论帖子的投票结果
	 */
	public void setDiscussionReplyVotes(String discussionReplyVote) {
		this.discussionReplyVotes = discussionReplyVote;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		return String.format("DiscussionReply [ReplyID=%d, ThreadID=%d, Creator={%s}, CreateTime=%s]", 
				new Object[] { discussionReplyId, discussionThreadId, 
						discussionReplyCreator, discussionReplyCreateTime });
	}

	/**
	 * 讨论回复的唯一标识符.
	 */
	private long discussionReplyId;
	
	/**
	 * 讨论帖子的唯一标识符.
	 */
	private long discussionThreadId;
	
	/**
	 * 讨论帖子的创建者.
	 */
	private User discussionReplyCreator;
	
	/**
	 * 讨论帖子的创建时间.
	 */
	private Date discussionReplyCreateTime;
	
	/**
	 * 讨论帖子的内容.
	 */
	private String discussionReplyContent;
	
	/**
	 * 讨论帖子的投票结果.
	 */
	private String discussionReplyVotes;
	
	/**
	 * 唯一的序列化标识符. 
	 */
	private static final long serialVersionUID = 3380540873295113998L;
}
