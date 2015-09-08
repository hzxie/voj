package com.trunkshell.voj.web.model;

import java.io.Serializable;
import java.util.Date;

/**
 * 讨论文章的Model
 * 对应数据库中的voj_discussion_threads数据表.
 * 
 * @author Luo Guofu
 */
public class DiscussionThreads implements Serializable {
	
	public DiscussionThreads() { }

	public DiscussionThreads(long discussionTopicId, long discussionCreatorUid,
			long problemId, String discussionThreadsName,
			Date discussionCreateTime, String discussionContent) {
		this.discussionTopicId = discussionTopicId;
		this.discussionCreatorUid = discussionCreatorUid;
		this.problemId = problemId;
		this.discussionThreadsName = discussionThreadsName;
		this.discussionCreateTime = discussionCreateTime;
		this.discussionContent = discussionContent;
	}

	public DiscussionThreads(long discussionThreadsId, long discussionTopicId,
			long discussionCreatorUid, long problemId,
			String discussionThreadsName, Date discussionCreateTime,
			String discussionContent) {
		this(discussionTopicId, discussionCreatorUid, problemId, discussionThreadsName, discussionCreateTime, discussionContent);
		this.discussionThreadsId = discussionThreadsId;
	}

	public long getDiscussionThreadsId() {
		return discussionThreadsId;
	}

	public void setDiscussionThreadsId(long discussionThreadsId) {
		this.discussionThreadsId = discussionThreadsId;
	}

	public long getDiscussionTopicId() {
		return discussionTopicId;
	}

	public void setDiscussionTopicId(long discussionTopicId) {
		this.discussionTopicId = discussionTopicId;
	}

	public long getDiscussionCreatorUid() {
		return discussionCreatorUid;
	}

	public void setDiscussionCreatorUid(long discussionCreatorUid) {
		this.discussionCreatorUid = discussionCreatorUid;
	}

	public long getProblemId() {
		return problemId;
	}

	public void setProblemId(long problemId) {
		this.problemId = problemId;
	}

	public String getDiscussionThreadsName() {
		return discussionThreadsName;
	}

	public void setDiscussionThreadsName(String discussionThreadsName) {
		this.discussionThreadsName = discussionThreadsName;
	}

	public Date getDiscussionCreateTime() {
		return discussionCreateTime;
	}

	public void setDiscussionCreateTime(Date discussionCreateTime) {
		this.discussionCreateTime = discussionCreateTime;
	}

	public String getDiscussionContent() {
		return discussionContent;
	}

	public void setDiscussionContent(String discussionContent) {
		this.discussionContent = discussionContent;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		return String.format("Threads [ID=%d, TopicId=%d, CreatorUid=%d, ProblemId=%d, ThreadsName=%s, CreateTime=%tc, Content=%s]", 
				new Object[]{ discussionThreadsId, discussionTopicId, discussionCreatorUid, problemId, discussionThreadsName, discussionCreateTime, discussionContent });
	}

	/**
	 * 讨论文章的唯一标识符
	 */
	private long discussionThreadsId;
	
	/**
	 * 讨论节点的的唯一标识符
	 */
	private long discussionTopicId;
	
	/**
	 * 创建者唯一标识符
	 */
	private long discussionCreatorUid;
	
	/**
	 * 试题唯一标识符
	 */
	private long problemId;
	
	/**
	 * 讨论文章标题
	 */
	private String discussionThreadsName;
	
	/**
	 * 讨论文章创建时间
	 */
	private Date discussionCreateTime;
	
	/**讨论文章内容
	 * 
	 */
	private String discussionContent;

	/**
	 * 唯一的序列化标识符.
	 */
	private static final long serialVersionUID = 874343642206503175L;

}
