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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

import org.verwandlung.voj.web.model.UserGroup;

/**
 * UserGroupMapper测试类.
 * @author Haozhe Xie
 */
@ExtendWith(SpringExtension.class)
@Transactional
@ContextConfiguration({"classpath:test-spring-context.xml"})
public class UserGroupMapperTest {
	/**
	 * 测试用例: 测试getUserGroups()方法
	 * 测试数据: N/a
	 * 预期结果: 返回全部的用户组对象(包含4个用户组对象) 
	 */
	public void testGetUserGroups() {
		List<UserGroup> userGroups = userGroupMapper.getUserGroups();
		Assertions.assertEquals(4, userGroups.size());
		
		UserGroup firstUserGroup = userGroups.get(0);
		String firstUserGroupSlug = firstUserGroup.getUserGroupSlug();
		Assertions.assertEquals("forbidden", firstUserGroupSlug);
	}
	
	/**
	 * 测试用例: 测试getUserGroupUsingId(int)方法
	 * 测试数据: 普通用户组(UserGroup)的用户组唯一标识符
	 * 预期结果: 返回用户组(UserGroup)的用户组对象
	 */
	@Test
	public void testGetUserGroupUsingIdExists() {
		UserGroup userGroup = userGroupMapper.getUserGroupUsingId(2);
		Assertions.assertNotNull(userGroup);
		
		String userGroupSlug = userGroup.getUserGroupSlug();
		Assertions.assertEquals("users", userGroupSlug);
	}
	
	/**
	 * 测试用例: 测试getUserGroupUsingId(int)方法
	 * 测试数据: 不存在的用户组唯一标识符
	 * 预期结果: 返回空引用
	 */
	@Test
	public void testGetUserGroupUsingIdNotExists() {
		UserGroup userGroup = userGroupMapper.getUserGroupUsingId(0);
		Assertions.assertNull(userGroup);
	}
	
	/**
	 * 测试用例: 测试getUserGroupUsingSlug(String)方法
	 * 测试数据: 普通用户组(UserGroup)的用户组别名
	 * 预期结果: 返回用户组(UserGroup)的用户组对象
	 */
	@Test
	public void testGetUserGroupUsingSlugExists() {
		UserGroup userGroup = userGroupMapper.getUserGroupUsingSlug("users");
		Assertions.assertNotNull(userGroup);
		
		int userGroupId = userGroup.getUserGroupId();
		Assertions.assertEquals(2, userGroupId);
	}
	
	/**
	 * 测试用例: 测试getUserGroupUsingSlug(String)方法
	 * 测试数据: 不存在的用户组别名
	 * 预期结果: 返回空引用
	 */
	@Test
	public void testGetUserGroupUsingSlugNotExists() {
		UserGroup userGroup = userGroupMapper.getUserGroupUsingSlug("Not-Exists");
		Assertions.assertNull(userGroup);
	}
	
	/**
	 * 待测试的UserGroupMapper对象.
	 */
	@Autowired
	private UserGroupMapper userGroupMapper;
}
