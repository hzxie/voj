package com.trunkshell.voj.web.model;

import java.io.Serializable;

/**
 * 讨论节点的Model
 * 对应数据库中的voj_discussion_topics数据表.
 * 
 * @author Luo Guofu
 */
public class Topic implements Serializable {
	
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
	public int getTopicParentId() {
		return topicParentId;
	}

	/**
	 * 
	 * @param topicParentId
	 */
	public void setTopicParentId(int topicParentId) {
		this.topicParentId = topicParentId;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return String.format("Topic [ID=%d, Slug=%s, Name=%s]",
				topicId, topicSlug, topicName);
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
	 * 讨论节点的父节点
	 */
	private int topicParentId;

	/**
	 * 唯一的序列化标识符.
	 */
	private static final long serialVersionUID = -5535986464762831282L;

}
