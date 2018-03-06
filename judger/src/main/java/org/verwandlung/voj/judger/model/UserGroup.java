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
package org.verwandlung.voj.judger.model;

import java.io.Serializable;

/**
 * 用户组的Model.
 * 对应数据库中的voj_user_groups数据表.
 * 
 * @author Haozhe Xie
 */
public class UserGroup implements Serializable {
	/**
	 * 用户组的默认构造函数.
	 */
	public UserGroup() { }
	
	/**
	 * 用户组的构造函数.
	 * @param userGroupId - 用户组的唯一标识符
	 * @param userGroupSlug - 用户组的唯一英文简称
	 * @param userGroupName - 用户组名称
	 */
	public UserGroup(int userGroupId, String userGroupSlug, String userGroupName) {
		this.userGroupId = userGroupId;
		this.userGroupSlug = userGroupSlug;
		this.userGroupName = userGroupName;
	}
	
	/**
	 * 获取用户组唯一标识符.
	 * @return 用户组唯一标识符
	 */
	public int getUserGroupId() {
		return userGroupId;
	}

	/**
	 * 设置用户组唯一标识符.
	 * @param userGroupId - 用户组唯一标识符
	 */
	public void setUserGroupId(int userGroupId) {
		this.userGroupId = userGroupId;
	}

	/**
	 * 获取用户组唯一英文简称.
	 * @return 用户组唯一英文简称
	 */
	public String getUserGroupSlug() {
		return userGroupSlug;
	}

	/**
	 * 设置用户组唯一英文简称.
	 * @param userGroupSlug - 用户组唯一英文简称
	 */
	public void setUserGroupSlug(String userGroupSlug) {
		this.userGroupSlug = userGroupSlug;
	}

	/**
	 * 获取用户组名称.
	 * @return 用户组名称
	 */
	public String getUserGroupName() {
		return userGroupName;
	}

	/**
	 * 设置用户组名称.
	 * @param userGroupName - 用户组名称
	 */
	public void setUserGroupName(String userGroupName) {
		this.userGroupName = userGroupName;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return String.format("UserGroup [ID=%d, Slug=%s, Name=%s]",
				new Object[] { userGroupId, userGroupSlug, userGroupName });
	}

	/**
	 * 用户组唯一标识符.
	 */
	private int userGroupId;

	/**
	 * 用户组唯一英文简称.
	 */
	private String userGroupSlug;

	/**
	 * 用户组名称.
	 */
	private String userGroupName;

	/**
	 * 唯一的序列化标识符.
	 */
	private static final long serialVersionUID = -4267702283058634035L;
}
