package com.trunkshell.voj.web.model;

import java.io.Serializable;
import java.util.Date;

/**
 * 电子邮件验证类.
 * 
 * @author Xie Haozhe
 */
public class EmailValidation implements Serializable {
    /**
     * EmailValidation类的默认构造函数.
     */
    public EmailValidation() { }
    
    /**
     * EmailValidation类的构造函数.
     * @param email - 待验证的电子邮件地址
     * @param token - 验证所用的Token
     * @param expireTime - Token的过期时间
     */
    public EmailValidation(String email, String token, Date expireTime) { 
        this.email = email;
        this.token = token;
        this.expireTime = expireTime;
    }
    
    /**
     * 获取待验证的电子邮件地址.
     * @return 待验证的电子邮件地址
     */
    public String getEmail() {
        return email;
    }

    /**
     * 设置待验证的电子邮件地址.
     * @param email - 待验证的电子邮件地址
     */
    public void setEmail(String email) {
        this.email = email;
    }

    /**
     * 获取验证所用的Token.
     * @return 验证所用的Token
     */
    public String getToken() {
        return token;
    }

    /**
     * 设置验证所用的Token.
     * @param token - 验证所用的Token
     */
    public void setToken(String token) {
        this.token = token;
    }

    /**
     * 获取Token的过期时间.
     * @return Token的过期时间
     */
    public Date getExpireTime() {
        return expireTime;
    }

    /**
     * 设置Token的过期时间.
     * @param expireTime - Token的过期时间
     */
    public void setExpireTime(Date expireTime) {
        this.expireTime = expireTime;
    }
    
    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    public String toString() {
        return String.format("EmailValidation [Email=%s, Token=%s, ExpireTime=%s]", 
                new Object[] { email, token, expireTime });
    }

    /**
     * 待验证的电子邮件地址.
     */
    private String email;
    
    /**
     * 验证所用的Token.
     */
    private String token;
    
    /**
     * Token的过期时间.
     */
    private Date expireTime;
    
    /**
     * 唯一的序列化标识符.
     */
    private static final long serialVersionUID = -2452767046421078474L;

}
