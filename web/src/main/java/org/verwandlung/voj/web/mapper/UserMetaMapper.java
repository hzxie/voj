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

import java.util.Date;
import java.util.List;

import org.apache.ibatis.annotations.CacheNamespace;
import org.apache.ibatis.annotations.Param;

import org.verwandlung.voj.web.model.User;
import org.verwandlung.voj.web.model.UserMeta;

/**
 * UserMeta Data Access Object.
 * 
 * @author Haozhe Xie
 */
@CacheNamespace(implementation = org.mybatis.caches.ehcache.EhcacheCache.class)
public interface UserMetaMapper {
	/**
	 * [此方法仅供管理员使用]
	 * 获取系统中某段时间内注册用户的总数.
	 * @param startTime - 统计起始时间(包含)
	 * @param endTime - 统计结束时间(包含)
	 * @return 系统中某段时间内注册用户的总数
	 */
	long getNumberOfUserRegistered(@Param("startTime") Date startTime, @Param("endTime") Date endTime);
	
	/**
	 * 获取某个用户的全部元信息.
	 * @param user - 某个用户对象
	 * @return 一个包含元信息的列表
	 */
	List<UserMeta> getUserMetaUsingUser(@Param("user") User user);
	
	/**
	 * 获取某个用户的某个特定的元信息.
	 * @param user - 某个用户对象
	 * @param metaKey - 元信息的键
	 * @return 预期的元信息对象
	 */
	UserMeta getUserMetaUsingUserAndMetaKey(@Param("user") User user, @Param("metaKey") String metaKey);
	
	/**
	 * 插入新的用户元数据.
	 * @param userMeta
	 */
	int createUserMeta(UserMeta userMeta);
	
	/**
	 * 更新某个用户的元数据.
	 * @param userMeta - 待更新的元数据
	 */
	int updateUserMeta(UserMeta userMeta);
	
	/**
	 * 删除某个用户的全部元数据.
	 * @param uid - 用户的唯一标识符
	 */
	int deleteUserMetaUsingUser(@Param("uid") long uid);
}
