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
package org.verwandlung.voj.web.mapper;

import java.util.List;

import org.apache.ibatis.annotations.CacheNamespace;
import org.apache.ibatis.annotations.Param;

import org.verwandlung.voj.web.model.User;
import org.verwandlung.voj.web.model.UserGroup;

/**
 * User Data Access Object.
 * 
 * @author Haozhe Xie
 */
@CacheNamespace(implementation = org.mybatis.caches.ehcache.EhcacheCache.class)
public interface UserMapper {
	/**
	 * [此方法仅供管理员使用]
	 * 获取系统中某个用户组中用户的总数.
	 * @param userGroup - 用户所属的用户组对象
	 * @return 系统中某个用户组中用户的总数
	 */
	long getNumberOfUsersUsingUserGroup(@Param("userGroup") UserGroup userGroup);
	
	/**
	 * [此方法仅供管理员使用]
	 * 使用用户组和用户名获取符合条件的用户的总数.
	 * @param userGroup - 用户组对象
	 * @param username - 部分或全部用户名
	 * @return 某个用户组中用户名中包含某个字符串的用户的总数
	 */
	long getNumberOfUsersUsingUserGroupAndUsername(@Param("userGroup") UserGroup userGroup, @Param("username") String username);
	
	/**
	 * [此方法仅供管理员使用]
	 * 获取选取某个语言偏好设置的用户总数.
	 * @param languageId - 编程语言的唯一标识符
	 * @return 选取某个语言偏好设置的用户总数
	 */
	long getNumberOfUsersUsingLanguage(@Param("languageId") int languageId);
	
	/**
	 * 通过用户唯一标识符获取用户对象.
	 * @param uid - 用户唯一标识符
	 * @return 预期的用户对象或空引用
	 */
	User getUserUsingUid(@Param("uid") long uid);
	
	/**
	 * 通过用户名获取用户对象.
	 * @param username - 用户名
	 * @return 预期的用户对象或空引用
	 */
	User getUserUsingUsername(@Param("username") String username);
	
	/**
	 * 通过电子邮件地址获取用户对象.
	 * @param email - 电子邮件地址
	 * @return 预期的用户对象或空引用
	 */
	User getUserUsingEmail(@Param("email") String email);
	
	/**
	 * 获取某个用户组中的用户列表.
	 * @param userGroup - 用户所属的用户组对象
	 * @param offset - 用户唯一标识符的起始编号
	 * @param limit - 需要获取的用户的数量
	 * @return 符合条件的用户列表
	 */
	List<User> getUserUsingUserGroup(@Param("userGroup") UserGroup userGroup, @Param("uid") long offset, @Param("limit") int limit);
	
	/**
	 * [此方法仅供管理员使用]
	 * 根据用户组和用户名筛选用户对象.
	 * @param userGroup - 用户组对象
	 * @param username - 部分或全部用户名
	 * @param offset - 用户唯一标识符的起始编号
	 * @param limit - 需要获取的用户的数量
	 * @return 符合条件的用户列表
	 */
	List<User> getUserUsingUserGroupAndUsername(@Param("userGroup") UserGroup userGroup, @Param("username") String username, @Param("offset") long offset, @Param("limit") int limit);
	
	/**
	 * 创建新用户对象.
	 * @param user - 待创建的用户对象
	 */
	int createUser(User user);
	
	/**
	 * 更新用户对象.
	 * @param user - 待更新信息的用户对象
	 */
	int updateUser(User user);
	
	/**
	 * 删除用户对象.
	 * @param uid - 待删除用户的用户唯一标识符
	 */
	int deleteUser(@Param("uid") long uid);
}
