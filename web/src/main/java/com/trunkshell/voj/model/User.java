package com.trunkshell.voj.model;

import java.io.Serializable;

public class User implements Serializable {
	/**
     * 获取用户唯一标识符.
     * @return 用户唯一标识符
     */
    public long getUid() {
        return uid;
    }
    
    /**
     * 设置用户唯一标识符.
     * @param uid - 用户唯一标识符
     */
    public void setUid(long uid) {
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
     * 用户的唯一标识符.
     */
    private long uid;
    
    /**
     * 用户名.
     */
    private String username;

    /**
     * 密码(已采用MD5加密).
     */
    private String password;

    /**
     * 电子邮件地址.
     */
    private String email;
	
	/**
	 * 唯一的序列化标识符.
	 */
	private static final long serialVersionUID = 2264535351943252507L;

}
