package com.happystudio.voj.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;

/**
 * 用户的Model.
 * @author Xie Haozhe
 */
@Entity
@Table(name = "voj_users")
public class User implements Serializable {
	/**
     * User类的默认构造函数. 
     */
    public User() { }
    
    /**
     * User类的构造函数.
     * @param username - 用户名
     * @param password - 密码
     * @param email - 电子邮件地址
     * @param userGroup - 用户组
     * @param preferLanguage - 用户偏好语言
     */
    public User(String username, String password, String email, 
                UserGroup userGroup, Language preferLanguage) {
        this.username = username;
        this.password = password;
        this.email = email;
        this.userGroup = userGroup;
        this.preferLanguage = preferLanguage;
    }
    
    /**
     * User类的构造函数.
     * @param uid - 用户唯一标识符
     * @param username - 用户名
     * @param password - 密码
     * @param email - 电子邮件地址
     * @param userGroup - 用户组
     * @param preferLanguage - 用户偏好语言
     */
    public User(int uid, String username, String password, String email, 
                UserGroup userGroup, Language preferLanguage) {
        this.uid = uid;
        this.username = username;
        this.password = password;
        this.email = email;
        this.userGroup = userGroup;
        this.preferLanguage = preferLanguage;
    }
    
    /**
     * 获取用户唯一标识符.
     * @return 用户唯一标识符
     */
    public int getUid() {
        return uid;
    }
    
    /**
     * 设置用户唯一标识符.
     * @param uid - 用户唯一标识符
     */
    public void setUid(int uid) {
        this.uid = uid;
    }
    
    /**
     * 获取用户名.
     * @return 用户名
     */
    public String getUsername() {
        return username;
    }

    /**
     * 设置用户名.
     * @param Username - 用户名
     */
    public void setUsername(String username) {
        this.username = username;
    }
    
    /**
     * 获取密码(已采用MD5加密).
     * @return 密码
     */
    public String getPassword() {
        return password;
    }

    /**
     * 设置密码.
     * @param password - 密码
     */
    public void setPassword(String password) {
        this.password = password;
    }

    /**
     * 获取电子邮件地址.
     * @return 电子邮件地址
     */
    public String getEmail() {
        return email;
    }

    /**
     * 设置电子邮件地址
     * @param email - 电子邮件地址
     */
    public void setEmail(String email) {
        this.email = email;
    }

    /**
     * 获取用户组.
     * @return 用户组对象
     */
    public UserGroup getUserGroup() {
        return userGroup;
    }

    /**
     * 设置用户组.
     * @param userGroup - 用户组对象
     */
    public void setUserGroup(UserGroup userGroup) {
        this.userGroup = userGroup;
    }
    
    /**
     * 获取用户偏好语言对象.
     * @return 用户偏好语言对象
     */
    public Language getPreferLanguage() {
        return preferLanguage;
    }
    
    /**
     * 设置用户偏好语言对象
     * @param preferLanguage - 用户偏好语言对象
     */
    public void setPreferLanguage(Language preferLanguage) {
        this.preferLanguage = preferLanguage;
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
    public String toString() {
        return String.format("User: [Uid=%s, Username=%s, Password=%s, Email=%s, UserGroup={%s}, Language={%s}]", 
                new Object[] { uid, username, password, email, userGroup, preferLanguage });
    }

	/**
     * 用户的唯一标识符.
     */
    @Id
    @GeneratedValue
    @Column(name = "uid")
    private int uid;
    
    /**
     * 用户名.
     */
    @Column(name = "username")
    private String username;

    /**
     * 密码(已采用MD5加密).
     */
    @Column(name = "password")
    private String password;

    /**
     * 电子邮件地址.
     */
    @Column(name = "email")
    private String email;
    
    /**
     * 用户组对象.
     */
    @ManyToOne(targetEntity = UserGroup.class)
    @JoinColumn(name = "user_group_id")
    private UserGroup userGroup;
    
    /**
     * 用户偏好语言对象.
     */
    @ManyToOne(targetEntity = Language.class)
    @JoinColumn(name = "prefer_language_id", referencedColumnName="language_id")
    private Language preferLanguage;
    
    /**
     * 用户评测记录集合(用于1-N连接).
     */
    @OneToMany(targetEntity = Submission.class, 
            	fetch = FetchType.LAZY, mappedBy = "user")
    private List<Submission> submission = new ArrayList<Submission>();
    
    /**
	 * 唯一的序列化标识符.
	 */
	private static final long serialVersionUID = 798599195431882295L;
}
