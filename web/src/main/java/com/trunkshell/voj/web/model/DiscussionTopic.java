package com.trunkshell.voj.web.model;

import java.io.Serializable;

/**
 * 讨论节点的Model
 * 对应数据库中的voj_discussion_topics数据表.
 * 
 * @author Luo Guofu
 */
public class DiscussionTopic implements Serializable {
	
	public DiscussionTopic() { }
	
	/**
	 * 创建根讨论节点
	 * @param topicSlug - 讨论节点的英文唯一缩写
	 * @param topicName - 讨论节点的名称
	 */
	public DiscussionTopic(String topicSlug, String topicName) {
		this(topicSlug, topicName, null);
	}

	/**
	 * 创建根讨论节点
	 * @param topicSlug - 讨论节点的英文唯一缩写
	 * @param topicName - 讨论节点的名称
	 * @param topicParentId - 讨论节点的父节点唯一标识符
	 */
	public DiscussionTopic(String topicSlug, String topicName,
			Integer topicParentId) {
		this.topicSlug = topicSlug;
		this.topicName = topicName;
		this.topicParentId = topicParentId;
	}

	/**
	 * 获取讨论节点唯一标识符
	 * @return 讨论节点唯一标识符
	 */
	public int getTopicId() {
		return topicId;
	}

	/**
	 * 设置讨论节点唯一标识符
	 * @param topicId - 讨论节点唯一标识符
	 */
	public void setTopicId(int topicId) {
		this.topicId = topicId;
	}

	/**
	 * 获取讨论节点的英文唯一缩写
	 * @return 讨论节点的英文唯一缩写
	 */
	public String getTopicSlug() {
		return topicSlug;
	}

	/**
	 * 设置讨论节点的英文唯一缩写
	 * @param topicSlug - 讨论节点的英文唯一缩写
	 */
	public void setTopicSlug(String topicSlug) {
		this.topicSlug = topicSlug;
	}

	/**
	 * 获取讨论节点的名称
	 * @return 讨论节点的名称
	 */
	public String getTopicName() {
		return topicName;
	}

	/**
	 * 设置讨论节点的名称
	 * @param topicName - 讨论节点的名称
	 */
	public void setTopicName(String topicName) {
		this.topicName = topicName;
	}

	/**
	 * 
	 * @return
	 */
	public Integer getTopicParentId() {
		return topicParentId;
	}

	/**
	 * 
	 * @param topicParentId
	 */
	public void setTopicParentId(int topicParentId) {
		this.topicParentId = topicParentId;
	}
	
	/**
	 * 判断是否为根讨论节点
	 * @return 根节点为true, 子节点为false
	 */
	public boolean isRootTopic() {
		return topicParentId == null;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return String.format("Topic [ID=%d, Slug=%s, Name=%s, Pid=%d]",
				topicId, topicSlug, topicName, topicParentId);
	}

	/**
	 * 讨论节点的唯一标识符
	 */
	private int topicId;
	
	/**
	 * 讨论节点的英文唯一缩写
	 */
	private String topicSlug;
	
	/**
	 * 讨论节点的名称
	 */
	private String topicName;
	
	/**
	 * 讨论节点的父节点唯一标识符
	 */
	private Integer topicParentId;

	/**
	 * 唯一的序列化标识符.
	 */
	private static final long serialVersionUID = -5535986464762831282L;

}
