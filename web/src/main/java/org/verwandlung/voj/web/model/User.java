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

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * 用户的Model.
 * 对应数据库中的voj_users数据表.
 * 
 * @author Haozhe Xie
 */
public class User implements Serializable {
	/**
	 * 用户的默认构造函数. 
	 */
	public User() { }
	
	/**
	 * 用户的构造函数.
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
		this(username, password, email, userGroup, preferLanguage);
		this.uid = uid;
	}
	
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
	 * @param username - 用户名
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
	
	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	public int hashCode() {
		return (int) uid;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	public boolean equals(Object obj) {
		if ( obj instanceof User ) {
			User anotherUser = (User)obj;
			return anotherUser.getUid() == uid;
		}
		return false;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		return String.format("User: [Uid=%s, Username=%s, Email=%s, UserGroup={%s}]",
				new Object[] { uid, username, email, userGroup });
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
	@JsonIgnore
	private String password;

	/**
	 * 电子邮件地址.
	 */
	private String email;
	
	/**
	 * 用户组对象.
	 */
	@JsonIgnore
	private UserGroup userGroup;
	
	/**
	 * 用户偏好语言对象.
	 */
	@JsonIgnore
	private Language preferLanguage;
	
	/**
	 * 唯一的序列化标识符.
	 */
	private static final long serialVersionUID = 2264535351943252507L;
}
