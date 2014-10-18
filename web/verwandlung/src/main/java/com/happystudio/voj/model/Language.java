package com.happystudio.voj.model;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;

/**
 * 编程语言的Model.
 * @author Xie Haozhe
 */
@Entity
@Table(name = "voj_languages")
public class Language {
    /**
     * 编程语言的默认构造函数.
     */
    public Language() { }
    
    /**
     * 编程语言的构造函数.
     * @param languageID - 编程语言的唯一标识符
     * @param languageSlug - 编程语言的英文缩写
     * @param languageName - 编程语言的名称
     */
    public Language(int languageID, String languageSlug, String languageName) {
        this.languageID = languageID;
        this.languageSlug = languageSlug;
        this.languageName = languageName;
    }
    
    /**
     * 获取编程语言的唯一标识符.
     * @return 编程语言的唯一标识符
     */
    public int getLanguageID() {
        return languageID;
    }
    
    /**
     * 设置编程语言的唯一标识符.
     * @param languageID - 编程语言的唯一标识符
     */
    public void setLanguageID(int languageID) {
        this.languageID = languageID;
    }
    
    /**
     * 获取编程语言的英文缩写
     * @return 编程语言的英文缩写
     */
    public String getLanguageSlug() {
        return languageSlug;
    }
    
    /**
     * 设置编程语言的英文缩写.
     * @param languageSlug - 编程语言的英文缩写
     */
    public void setLanguageSlug(String languageSlug) {
        this.languageSlug = languageSlug;
    }
    
    /**
     * 获取编程语言的名称.
     * @return 编程语言的名称
     */
    public String getLanguageName() {
        return languageName;
    }
    
    /**
     * 设置编程语言的名称.
     * @param languageName - 编程语言的名称
     */
    public void setLanguageName(String languageName) {
        this.languageName = languageName;
    }
    
    /**
     * 获取用户列表(用于1-N关联).
     * @return 用户列表
     */
    public List<User> getUsers() {
        return users;
    }

    /**
     * 设置用户列表.
     * @param users - 用户列表
     */
    public void setUsers(List<User> users) {
        this.users = users;
    }
    
    /**
     * 获取评测记录集合(用于1-N关联).
     * @return 评测记录集合
     */
    public List<Submission> getSubmission() {
    	return submission;
    }
    
    /**
     * 设置评测记录集合.
     * @param submission - 评测记录集合
     */
    public void setSubmission(List<Submission> submission) {
    	this.submission = submission;
    }
    
    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return String.format("Language [ID=%d, Slug=%s, Name=%s]",
                new Object[] { languageID, languageSlug, languageName });
    }
    
    /**
     * 编程语言的唯一标识符. 
     */
    @Id
    @GeneratedValue
    @Column(name = "language_id")
    private int languageID;
    
    /**
     * 编程语言的唯一英文简称.
     */
    @Column(name = "language_slug")
    private String languageSlug;
    
    /**
     * 编程语言的名称.
     */
    @Column(name = "language_name")
    private String languageName;
    
    /**
     * 用户列表(以便1-N关联).
     */
    @OneToMany(targetEntity = User.class, 
                fetch = FetchType.LAZY, mappedBy = "preferLanguage")
    private List<User> users = new ArrayList<User>();
    
    /**
     * 评测记录集合(以便1-N关联).
     */
	@OneToMany(targetEntity = Submission.class, 
            fetch = FetchType.LAZY, mappedBy = "language")
	private List<Submission> submission = new ArrayList<Submission>();
}
