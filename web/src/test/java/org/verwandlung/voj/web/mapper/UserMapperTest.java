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
import org.junit.jupiter.api.function.Executable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

import org.verwandlung.voj.web.model.Language;
import org.verwandlung.voj.web.model.User;
import org.verwandlung.voj.web.model.UserGroup;

/**
 * UserMapper测试类.
 * @author Haozhe Xie
 */
@ExtendWith(SpringExtension.class)
@Transactional
@ContextConfiguration({"classpath:test-spring-context.xml"})
public class UserMapperTest {
	/**
	 * 测试用例: 测试getNumberOfUsers(UserGroup)方法
	 * 测试数据: 使用用户(Users)的用户组
	 * 预期结果: 返回该用户组中用户的数量
	 */
	@Test
	public void testGetNumberOfUsers() {
		UserGroup userGroup = new UserGroup(2, "users", "Users");
		long totalUsers = userMapper.getNumberOfUsersUsingUserGroup(userGroup);
		
		Assertions.assertEquals(1, totalUsers);
	}
	
	/**
	 * 测试用例: 测试getNumberOfUsersUsingLanguage(int)方法
	 * 测试数据: 使用C++语言的唯一标识符
	 * 预期结果: 返回使用C++作为偏好语言的用户数量()
	 */
	@Test
	public void testGetNumberOfUsersUsingLanguage() {
		int languageId = 2;
		long totalUsers = userMapper.getNumberOfUsersUsingLanguage(languageId);
		Assertions.assertEquals(2, totalUsers);
	}
	
	/**
	 * 测试用例: 测试getUserUsingUid(long)方法
	 * 测试数据: 使用用户名为zjhzxhz的用户的唯一标识符
	 * 预期结果: 返回预期的用户对象
	 */
	@Test
	public void testGetUserUsingUidExists() {
		User user = userMapper.getUserUsingUid(1000);
		Assertions.assertNotNull(user);
		
		String username = user.getUsername();
		Assertions.assertEquals("zjhzxhz", username);
	}
	
	/**
	 * 测试用例: 测试getUserUsingUid(long)方法
	 * 测试数据: 使用不存在的用户唯一标识符 
	 * 预期结果: 返回空引用
	 */
	@Test
	public void testGetUserUsingUidNotExists() {
		User user = userMapper.getUserUsingUid(0);
		Assertions.assertNull(user);
	}
	
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
	 * 测试用例: 测试getUserUsingEmail(String)方法
	 * 测试数据: 使用用户名为zjhzxhz的用户的电子邮件地址
	 * 预期结果: 返回预期的用户对象
	 */
	@Test
	public void testGetUserUsingEmailExists() {
		User user = userMapper.getUserUsingEmail("cshzxie@gmail.com");
		Assertions.assertNotNull(user);
		
		long uid = user.getUid();
		Assertions.assertEquals(1000, uid);
	}
	
	/**
	 * 测试用例: 测试getUserUsingEmail(String)方法
	 * 测试数据: 使用不存在的电子邮件地址
	 * 预期结果: 返回空引用
	 */
	@Test
	public void testGetUserUsingEmailNotExists() {
		User user = userMapper.getUserUsingEmail("not-exists@verwandlung.org");
		Assertions.assertNull(user);
	}
	
	/**
	 * 测试用例: 测试getUserUsingUserGroup(UserGroup, long, int)方法
	 * 测试数据: 使用用户(Users)的用户组
	 * 预期结果: 返回预期的用户列表(共1名用户)
	 */
	@Test
	public void testGetUserUsingUserGroup() {
		UserGroup userGroup = new UserGroup(2, "users", "Users");
		List<User> users = userMapper.getUserUsingUserGroup(userGroup, 0, 1);
		Assertions.assertEquals(1, users.size());
		
		User firstUser = users.get(0);
		String username = firstUser.getUsername();
		Assertions.assertEquals("another-user", username);
	}
	
	/**
	 * 测试用例: 测试createUser(User)方法
	 * 测试数据: 使用合法的数据集, 并且数据表中不存在相同用户名和电子邮件地址
	 * 预期结果: 数据插入操作成功完成
	 */
	@Test
	public void testCreateUserNormally() {
		UserGroup userGroup = new UserGroup(1, "users", "Users");
		Language language = new Language(2, "text/x-c++", "C++", "g++ foo.cpp -o foo", "./foo");
		User user = new User("new-user-0xff", "Password","new-user-0xff@verwandlung.org", userGroup, language);
		
		int numberOfRowsAffected = userMapper.createUser(user);
		Assertions.assertEquals(1, numberOfRowsAffected);
	}
	
	/**
	 * 测试用例: 测试createUser(User)方法
	 * 测试数据: 使用合法的数据集, 但数据表中已存在相同的用户名
	 * 预期结果: 抛出org.springframework.dao.DuplicateKeyException异常
	 */
	@Test
	public void testCreateUserUsingExistingUsername() {
		UserGroup userGroup = new UserGroup(1, "users", "Users");
		Language language = new Language(2, "text/x-c++", "C++", "g++ foo.cpp -o foo", "./foo");
		User user = new User("zjhzxhz", "Password","noreply@verwandlung.org", userGroup, language);
		Executable e = () -> {
			userMapper.createUser(user);
		};
		Assertions.assertThrows(org.springframework.dao.DuplicateKeyException.class, e);
	}
	
	/**
	 * 测试用例: 测试createUser(User)方法
	 * 测试数据: 使用合法的数据集, 但数据表中已存在相同的电子邮件地址
	 * 预期结果: 抛出org.springframework.dao.DuplicateKeyException异常
	 */
	@Test
	public void testCreateUserUsingExistingEmail() {
		UserGroup userGroup = new UserGroup(1, "users", "Users");
		Language language = new Language(2, "text/x-c++", "C++", "g++ foo.cpp -o foo", "./foo");
		User user = new User("new-user-0xfe", "Password","cshzxie@gmail.com", userGroup, language);
		Executable e = () -> {
			userMapper.createUser(user);
		};
		Assertions.assertThrows(org.springframework.dao.DuplicateKeyException.class, e);
	}
	
	/**
	 * 测试用例: 测试createUser(User)方法
	 * 测试数据: 使用不合法的数据集(过长的用户名)
	 * 预期结果: 抛出org.springframework.dao.DataIntegrityViolationException异常
	 */
	@Test
	public void testCreateUserUsingTooLongUsername() {
		UserGroup userGroup = new UserGroup(1, "users", "Users");
		Language language = new Language(2, "text/x-c++", "C++", "g++ foo.cpp -o foo", "./foo");
		User user = new User("new-user-0xffffffff", "Password","noreply@verwandlung.org", userGroup, language);
		Executable e = () -> {
			userMapper.createUser(user);
		};
		Assertions.assertThrows(org.springframework.dao.DataIntegrityViolationException.class, e);
	}
	
	/**
	 * 测试用例: 测试createUser(User)方法
	 * 测试数据: 使用不合法的数据集(不存在的编程语言)
	 * 预期结果: 抛出org.springframework.dao.DataIntegrityViolationException异常
	 */
	@Test
	public void testCreateUserUsingNotExistsLanguage() {
		UserGroup userGroup = new UserGroup(1, "users", "Users");
		Language language = new Language(0, "not-exists", "Not Exists", "Not Exists", "Not Exists");
		User user = new User("new-user-0xfe", "Password","noreply@verwandlung.org", userGroup, language);
		Executable e = () -> {
			userMapper.createUser(user);
		};
		Assertions.assertThrows(org.springframework.dao.DataIntegrityViolationException.class, e);
	}
	
	/**
	 * 测试用例: 测试updateUser(User)方法
	 * 测试数据: 使用合法的数据集, 并且数据表中不存在相同用户名和电子邮件地址
	 * 预期结果: 数据更新操作成功完成
	 */
	@Test
	public void testUpdateUserNormally() {
		User user = userMapper.getUserUsingUid(1000);
		Assertions.assertNotNull(user);
		
		user.setEmail("hzxie@hit.edu.cn");
		int numberOfRowsAffected = userMapper.updateUser(user);
		Assertions.assertEquals(1, numberOfRowsAffected);
	}
	
	/**
	 * 测试用例: 测试updateUser(User)方法
	 * 测试数据: 使用合法的数据集, 并且数据表中不存在相同用户名和电子邮件地址
	 * 预期结果: 数据更新操作成功完成
	 */
	@Test
	public void testUpdateUserUsingExistingEmail() {
		User user = userMapper.getUserUsingUid(1000);
		Assertions.assertNotNull(user);
		
		user.setEmail("support@verwandlung.org");
		Executable e = () -> {
			userMapper.updateUser(user);
		};
		Assertions.assertThrows(org.springframework.dao.DuplicateKeyException.class, e);
	}
	
	/**
	 * 测试用例: 测试deleteUser(long)方法
	 * 测试数据: 存在的用户唯一标识符
	 * 预期结果: 数据删除操作成功完成
	 */
	@Test
	public void testDeleteUserExists() {
		User user = userMapper.getUserUsingUid(1002);
		Assertions.assertNotNull(user);
		
		int numberOfRowsAffected = userMapper.deleteUser(1002);
		Assertions.assertEquals(1, numberOfRowsAffected);
		
		user = userMapper.getUserUsingUid(1002);
		Assertions.assertNull(user);
	}
	
	/**
	 * 测试用例: 测试deleteUser(long)方法
	 * 测试数据: 不存在的用户唯一标识符
	 * 预期结果: 方法正常执行, 未影响数据表中的数据
	 */
	@Test
	public void testDeleteUserNotExists() {
		User user = userMapper.getUserUsingUid(0);
		Assertions.assertNull(user);
		
		int numberOfRowsAffected = userMapper.deleteUser(0);
		Assertions.assertEquals(0, numberOfRowsAffected);
	}
	
	/**
	 * 待测试的UserMapper对象.
	 */
	@Autowired
	private UserMapper userMapper;
}
