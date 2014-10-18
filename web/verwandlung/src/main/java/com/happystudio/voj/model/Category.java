package com.happystudio.voj.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * 试题分类类的Model.
 * @author Xie Haozhe
 */
@Entity
@Table(name = "voj_categories")
public class Category {
	/**
	 * 试题分类的默认构造函数.
	 */
	public Category() { }
	
	/**
	 * 试题分类的构造函数
	 * @param categoryID - 试题分类的唯一标识符
	 * @param categorySlug - 试题分类的唯一英文缩写
	 * @param categoryName - 试题分类的名称
	 */
	public Category(int categoryID, String categorySlug, String categoryName) {
		this.categoryID = categoryID;
		this.categorySlug = categorySlug;
		this.categoryName = categoryName;
	}
	
	/**
	 * 获取试题分类的唯一标识符.
	 * @return 试题分类的唯一标识符
	 */
	public int getCategoryID() {
		return categoryID;
	}

	/**
	 * 设置试题分类的唯一标识符.
	 * @param categoryID - 试题分类的唯一标识符
	 */
	public void setCategoryID(int categoryID) {
		this.categoryID = categoryID;
	}

	/**
	 * 获取试题分类的唯一英文缩写.
	 * @return 试题分类的唯一英文缩写
	 */
	public String getCategorySlug() {
		return categorySlug;
	}

	/**
	 * 设置试题分类的唯一英文缩写
	 * @param categorySlug - 试题分类的唯一英文缩写
	 */
	public void setCategorySlug(String categorySlug) {
		this.categorySlug = categorySlug;
	}

	/**
	 * 获取试题分类的名称.
	 * @return 试题分类的名称
	 */
	public String getCategoryName() {
		return categoryName;
	}

	/**
	 * 设置试题分类的名称.
	 * @param categoryName - 试题分类的名称
	 */
	public void setCategoryName(String categoryName) {
		this.categoryName = categoryName;
	}

	/**
	 * 试题分类的唯一标识符.
	 */
	@Id
    @GeneratedValue
	@Column(name = "category_id")
	private int categoryID;
	
	/**
	 * 试题分类的唯一英文缩写.
	 */
	@Column(name = "category_slug")
	private String categorySlug;
	
	/**
	 * 试题分类的名称.
	 */
	@Column(name = "category_name")
	private String categoryName;
}
