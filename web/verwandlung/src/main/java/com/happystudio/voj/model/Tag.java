package com.happystudio.voj.model;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * 试题标签的Model.
 * @author Xie Haozhe
 */
@Entity
@Table(name = "voj_tags")
public class Tag implements Serializable {
	/**
	 * 试题标签的默认构造函数.
	 */
	public Tag() { }
	
	/**
	 * 试题标签的构造函数.
	 * @param tagID - 试题标签的唯一标识符
	 * @param TagSlug - 试题标签的唯一英文缩写
	 * @param tagName - 试题标签的名称
	 */
	public Tag(int tagID, String tagSlug, String tagName) { 
		this.tagID = tagID;
		this.tagSlug = tagSlug;
		this.tagName = tagName;
	}
	
	/**
	 * 获取试题标签的唯一标识符.
	 * @return 试题标签的唯一标识符
	 */
	public int getTagID() {
		return tagID;
	}

	/**
	 * 设置试题标签的唯一标识符
	 * @param tagID - 试题标签的唯一标识符
	 */
	public void setTagID(int tagID) {
		this.tagID = tagID;
	}

	/**
	 * 获取试题标签的唯一英文缩写
	 * @return 试题标签的唯一英文缩写
	 */
	public String getTagSlug() {
		return tagSlug;
	}

	/**
	 * 设置试题标签的唯一英文缩写.
	 * @param tagSlug - 试题标签的唯一英文缩写
	 */
	public void setTagSlug(String tagSlug) {
		this.tagSlug = tagSlug;
	}

	/**
	 * 获取试题标签的名称.
	 * @return 试题标签的名称
	 */
	public String getTagName() {
		return tagName;
	}

	/**
	 * 设置试题标签的名称.
	 * @param tagName - 试题标签的名称
	 */
	public void setTagName(String tagName) {
		this.tagName = tagName;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return String.format("Tag [ID=%d, Slug=%s, Name=%s]",
                new Object[] { tagID, tagSlug, tagName });
	}
	
	/**
	 * 试题标签的唯一标识符. 
	 */
	@Id
    @GeneratedValue
	@Column(name = "tag_id")
	private int tagID;
	
	/**
	 * 试题标签的唯一英文缩写.
	 */
	@Column(name = "tag_slug")
	private String tagSlug;
	
	/**
	 * 试题标签的名称. 
	 */
	@Column(name = "tag_name")
	private String tagName;

	/**
	 * 唯一的序列化标识符.
	 */
	private static final long serialVersionUID = 2873232221417856603L;
}
