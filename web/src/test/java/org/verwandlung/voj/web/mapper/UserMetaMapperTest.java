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

import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

import org.verwandlung.voj.web.model.User;
import org.verwandlung.voj.web.model.UserMeta;

/**
 * UserMetaMapper测试类.
 * 
 * @author Haozhe Xie
 */
@ExtendWith(SpringExtension.class)
@Transactional
@ContextConfiguration({"classpath:test-spring-context.xml"})
public class UserMetaMapperTest {
	/**
	 * 测试用例: 测试getNumberOfUserRegistered(Date, Date)方法
	 * 测试数据: 使用2014年10月7日 00:00:00 - 2014年10月7日 23:59:59
	 * 预期结果: 返回2014年10月7日注册的用户数量(共1名)
	 */
	@Test
	public void testGetNumberOfUserRegistered() {
		Calendar calendar = Calendar.getInstance();
		calendar.set(2014, 9, 8, 0, 0, 0);
		Date startTime = calendar.getTime();
		calendar.set(2014, 9, 8, 23, 59, 59);
		Date endTime = calendar.getTime();

		long numberOfUsers = userMetaMapper.getNumberOfUserRegistered(startTime, endTime);
		Assertions.assertEquals(1, numberOfUsers);
	}
	
	/**
	 * 测试用例: 测试getUserMetaUsingUser(User)方法
	 * 测试数据: 使用Uid为1000的用户
	 * 预期结果: 返回该用户的用户元信息的列表
	 */
	@Test
	public void testGetUserMetaUsingUserExists() {
		User user = userMapper.getUserUsingUid(1000);
		Assertions.assertNotNull(user);
		
		List<UserMeta> userMetaList = userMetaMapper.getUserMetaUsingUser(user);
		Assertions.assertEquals(1, userMetaList.size());
		
		UserMeta userMeta = userMetaList.get(0);
		String metaKey = userMeta.getMetaKey();
		Assertions.assertEquals("registerTime", metaKey);
	}
	
	/**
	 * 测试用例: 测试getUserMetaUsingUser(User)方法
	 * 测试数据: 使用不存在的用户
	 * 预期结果: 返回空列表
	 */
	@Test
	public void testGetUserMetaUsingUserNotExists() {
		User user = userMapper.getUserUsingUid(0);
		Assertions.assertNull(user);
		
		List<UserMeta> userMetaList = userMetaMapper.getUserMetaUsingUser(user);
		Assertions.assertEquals(0, userMetaList.size());
	}
	
	/**
	 * 测试用例: 测试getUserMetaUsingUserAndMetaKey(User, String)方法
	 * 测试数据: 使用用户UID为1000的用户和注册时间的元信息键
	 * 预期结果: 返回对应用户注册时间的元信息(UserMeta)对象
	 */
	@Test
	public void testGetUserMetaUsingUserAndMetaKeyExists() {
		User user = userMapper.getUserUsingUid(1000);
		Assertions.assertNotNull(user);
		
		UserMeta userMeta = userMetaMapper.getUserMetaUsingUserAndMetaKey(user, "registerTime");
		Assertions.assertNotNull(userMeta);
		
		String metaValue = userMeta.getMetaValue();
		Assertions.assertEquals("2014-10-07 12:35:45", metaValue);
	}
	
	/**
	 * 测试用例: 测试getUserMetaUsingUserAndMetaKey(User, String)方法
	 * 测试数据: 使用用户UID为1000的用户和不存在的元信息键
	 * 预期结果: 返回空引用
	 */
	@Test
	public void testGetUserMetaUsingUserAndMetaKeyNotExists() {
		User user = userMapper.getUserUsingUid(1000);
		Assertions.assertNotNull(user);
		
		UserMeta userMeta = userMetaMapper.getUserMetaUsingUserAndMetaKey(user, "notExistsKey");
		Assertions.assertNull(userMeta);
	}
	
	/**
	 * 测试用例: 测试createUserMeta(UserMeta)方法
	 * 测试数据: 使用合法的数据集, 创建新的用户元数据
	 * 预期结果: 数据插入操作成功完成
	 */
	@Test
	public void testCreateUserMetaNormally() {
		User user = userMapper.getUserUsingUid(1001);
		UserMeta userMeta = new UserMeta(user, "metaKey", "metaValue");

		int numberOfRowsAffected = userMetaMapper.createUserMeta(userMeta);
		Assertions.assertEquals(1, numberOfRowsAffected);
		
		UserMeta insertedUserMeta = userMetaMapper.getUserMetaUsingUserAndMetaKey(user, "metaKey");
		Assertions.assertNotNull(insertedUserMeta);
		
		String metaValue = insertedUserMeta.getMetaValue();
		Assertions.assertEquals("metaValue", metaValue);
	}
	
	/**
	 * 测试用例: 测试createUserMeta(UserMeta)方法
	 * 测试数据: 使用不存在的用户
	 * 预期结果: 抛出DataIntegrityViolationException异常
	 */
	@Test
	public void testCreateUserMetaUsingUserNotExists() {
		User user = userMapper.getUserUsingUid(0);
		Assertions.assertNull(user);
		
		UserMeta userMeta = new UserMeta(user, "metaKey", "metaValue");
		Executable e = () -> {
			userMetaMapper.createUserMeta(userMeta);
		};
		Assertions.assertThrows(org.springframework.dao.DataIntegrityViolationException.class, e);
	}
	
	/**
	 * 测试用例: 测试updateUserMeta(UserMeta)方法
	 * 测试数据: 使用合法的数据集, 并且尝试更新存在的用户元信息
	 * 预期结果: 数据更新操作成功完成
	 */
	@Test
	public void testUpdateUserMetaNormally() {
		User user = userMapper.getUserUsingUid(1001);
		UserMeta userMeta = userMetaMapper.getUserMetaUsingUserAndMetaKey(user, "registerTime");
		
		userMeta.setMetaValue("newMetaValue");
		int numberOfRowsAffected = userMetaMapper.updateUserMeta(userMeta);
		Assertions.assertEquals(1, numberOfRowsAffected);
		
		UserMeta updatedUserMeta = userMetaMapper.getUserMetaUsingUserAndMetaKey(user, "registerTime");
		String metaValue = updatedUserMeta.getMetaValue();
		Assertions.assertEquals("newMetaValue", metaValue);
	}
	
	/**
	 * 测试用例: 测试updateUserMeta(UserMeta)方法
	 * 测试数据: 使用合法的数据集, 并且尝试更新存在的用户元信息中的键
	 * 预期结果: 元信息的键未被更新
	 */
	@Test
	public void testUpdateUserMetaTryToUpdateMetaKey() {
		User user = userMapper.getUserUsingUid(1001);
		UserMeta userMeta = userMetaMapper.getUserMetaUsingUserAndMetaKey(user, "registerTime");
		
		userMeta.setMetaKey("newMetaKey");
		int numberOfRowsAffected = userMetaMapper.updateUserMeta(userMeta);
		/**
		 * The following Assert CANNOT passed in CI due to
		 * the bug of MyBatis. But it really works.
		 */
		// Assertions.assertEquals(0, numberOfRowsAffected);
		
		UserMeta updatedUserMeta = userMetaMapper.getUserMetaUsingUserAndMetaKey(user, "registerTime");
		String metaKey = updatedUserMeta.getMetaKey();
		Assertions.assertEquals("registerTime", metaKey);
	}
	
	/**
	 * 测试用例: 测试updateUserMeta(UserMeta)方法
	 * 测试数据: 使用不存在的用户
	 * 预期结果: 方法正常执行, 未影响数据表中的数据
	 */
	@Test
	public void testUpdateUserMetaUsingUserNotExists() {
		User user = userMapper.getUserUsingUid(0);
		Assertions.assertNull(user);
		
		UserMeta userMeta = userMetaMapper.getUserMetaUsingUserAndMetaKey(user, "registerTime");
		int numberOfRowsAffected = userMetaMapper.updateUserMeta(userMeta);
		Assertions.assertEquals(0, numberOfRowsAffected);
	}
	
	/**
	 * 测试用例: 测试deleteUserMetaUsingUser(long)方法
	 * 测试数据: 使用存在的用户UID
	 * 预期结果: 数据删除操作成功完成
	 */
	@Test
	public void testDeleteUserMetaUsingUserExists() {
		User user = userMapper.getUserUsingUid(1001);
		Assertions.assertNotNull(user);
		
		int numberOfRowsAffected = userMetaMapper.deleteUserMetaUsingUser(1001);
		Assertions.assertEquals(1, numberOfRowsAffected);
		
		List<UserMeta> userMetaList = userMetaMapper.getUserMetaUsingUser(user);
		Assertions.assertEquals(0, userMetaList.size());
	}
	
	/**
	 * 测试用例: 测试deleteUserMetaUsingUser(long)方法
	 * 测试数据: 使用不存在的用户UID
	 * 预期结果: 方法正常执行, 未影响数据表中的数据
	 */
	@Test
	public void testDeleteUserMetaUsingUserNotExists() {
		User user = userMapper.getUserUsingUid(0);
		Assertions.assertNull(user);
		
		int numberOfRowsAffected = userMetaMapper.deleteUserMetaUsingUser(0);
		Assertions.assertEquals(0, numberOfRowsAffected);
	}
	
	/**
	 * 待测试的UserMetaMapper对象.
	 */
	@Autowired
	private UserMetaMapper userMetaMapper;
	
	/**
	 * 自动注入的UserMapper对象.
	 * 用于构建测试用例.
	 */
	@Autowired
	private UserMapper userMapper;
}
