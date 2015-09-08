package com.trunkshell.voj.web.model;

import java.io.Serializable;
import java.util.Date;

public class DiscussionReply implements Serializable {
	
	public DiscussionReply() { }
		
	public DiscussionReply(long discussionThreadsId, long discussionReplyUid,
			Date discussionReplyTime, String discussionReplyContent) {
		this.discussionThreadsId = discussionThreadsId;
		this.discussionReplyUid = discussionReplyUid;
		this.discussionReplyTime = discussionReplyTime;
		this.discussionReplyContent = discussionReplyContent;
	}

	public DiscussionReply(long discussionReplyId, long discussionThreadsId,
			long discussionReplyUid, Date discussionReplyTime,
			String discussionReplyContent) {
		this(discussionThreadsId, discussionReplyUid, discussionReplyTime, discussionReplyContent);
		this.discussionReplyId = discussionReplyId;
	}

	public long getDiscussionReplyId() {
		return discussionReplyId;
	}

	public void setDiscussionReplyId(long discussionReplyId) {
		this.discussionReplyId = discussionReplyId;
	}

	public long getDiscussionThreadsId() {
		return discussionThreadsId;
	}

	public void setDiscussionThreadsId(long discussionThreadsId) {
		this.discussionThreadsId = discussionThreadsId;
	}

	public long getDiscussionReplyUid() {
		return discussionReplyUid;
	}

	public void setDiscussionReplyUid(long discussionReplyUid) {
		this.discussionReplyUid = discussionReplyUid;
	}

	public Date getDiscussionReplyTime() {
		return discussionReplyTime;
	}

	public void setDiscussionReplyTime(Date discussionReplyTime) {
		this.discussionReplyTime = discussionReplyTime;
	}

	public String getDiscussionReplyContent() {
		return discussionReplyContent;
	}

	public void setDiscussionReplyContent(String discussionReplyContent) {
		this.discussionReplyContent = discussionReplyContent;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		return String.format("Reply [ID=%d, ThreadsId=%d, ReplyUid=%d, ReplyTime=%tc, ReplyContent=%s]", 
				new Object[]{ discussionReplyId, discussionThreadsId, discussionReplyUid, discussionReplyTime, discussionReplyContent });
	}

	/**
	 * 讨论回复唯一标识符
	 */
	private long discussionReplyId;
	
	/**
	 * 讨论文章唯一标识符
	 */
	private long discussionThreadsId;
	
	/**
	 * 讨论回复的用户唯一标识符 
	 */
	private long discussionReplyUid;
	
	/**
	 * 讨论回复时间 
	 */
	private Date discussionReplyTime;
	
	/**
	 * 讨论回复内容
	 */
	private String discussionReplyContent;

	/**
	 * 唯一的序列化标识符.
	 */
	private static final long serialVersionUID = -7598475012831965445L;

}
