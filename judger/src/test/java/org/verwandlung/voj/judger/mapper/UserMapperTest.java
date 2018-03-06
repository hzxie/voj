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
package org.verwandlung.voj.judger.mapper;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

import org.verwandlung.voj.judger.model.User;

/**
 * UserMapper测试类.
 * 
 * @author Haozhe Xie
 */
@ExtendWith(SpringExtension.class)
@Transactional
@ContextConfiguration({"classpath:test-spring-context.xml"})
public class UserMapperTest {
	/**
	 * 测试用例: 测试getUserUsingUsername(String)方法
	 * 测试数据: 使用用户名为zjhzxhz的用户
	 * 预期结果: 返回预期的用户对象
	 */
	@Test
	public void testGetUserUsingUsernameExists() {
		User user = userMapper.getUserUsingUsername("Zjhzxhz");
		Assertions.assertNotNull(user);
		
		long uid = user.getUid();
		Assertions.assertEquals(1000, uid);
	}
	
	/**
	 * 测试用例: 测试getUserUsingUsername(String)方法
	 * 测试数据: 使用不存在的用户名
	 * 预期结果: 返回空引用
	 */
	@Test
	public void testGetUserUsingUsernameNotExists() {
		User user = userMapper.getUserUsingUsername("Not-Exists");
		Assertions.assertNull(user);
	}
	
	/**
	 * 待测试的UserMapper对象.
	 */
	@Autowired
	private UserMapper userMapper;
}
