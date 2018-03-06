/* Verwandlung Online Judge - A cross-platform judge online system
 * Copyright (C) 2018 Haozhe Xie <cshzxie@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 *
 *                              _ooOoo_  
 *                             o8888888o  
 *                             88" . "88  
 *                             (| -_- |)  
 *                             O\  =  /O  
 *                          ____/`---'\____  
 *                        .'  \\|     |//  `.  
 *                       /  \\|||  :  |||//  \  
 *                      /  _||||| -:- |||||-  \  
 *                      |   | \\\  -  /// |   |  
 *                      | \_|  ''\---/''  |   |  
 *                      \  .-\__  `-`  ___/-. /  
 *                    ___`. .'  /--.--\  `. . __  
 *                 ."" '<  `.___\_<|>_/___.'  >'"".  
 *                | | :  `- \`.;`\ _ /`;.`/ - ` : | |  
 *                \  \ `-.   \_ __\ /__ _/   .-` /  /  
 *           ======`-.____`-.___\_____/___.-`____.-'======  
 *                              `=---=' 
 *
 *                          HERE BE BUDDHA
 *
 */
package org.verwandlung.voj.web.model;

import java.io.Serializable;
import java.util.Date;

/**
 * 电子邮件验证类.
 * 
 * @author Haozhe Xie
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
