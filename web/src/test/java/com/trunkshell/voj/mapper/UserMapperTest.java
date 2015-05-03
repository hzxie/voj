package com.trunkshell.voj.mapper;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.transaction.TransactionConfiguration;
import org.springframework.transaction.annotation.Transactional;

import com.trunkshell.voj.model.Language;
import com.trunkshell.voj.model.User;
import com.trunkshell.voj.model.UserGroup;

/**
 * UserMapper测试类.
 * @author Xie Haozhe
 */
@RunWith(SpringJUnit4ClassRunner.class)
@Transactional
@TransactionConfiguration(defaultRollback = true)
@ContextConfiguration({"classpath:test-spring-context.xml"})
public class UserMapperTest {
	/**
	 * 测试用例: 测试getUserUsingUid(long)方法
	 * 测试数据: 使用用户名为zjhzxhz的用户的唯一标识符
	 * 预期结果: 返回预期的用户对象
	 */
	@Test
	public void testGetUserUsingUidExists() {
		User user = userMapper.getUserUsingUid(1);
		Assert.assertNotNull(user);
		
		String username = user.getUsername();
		Assert.assertEquals("zjhzxhz", username);
	}
	
	/**
	 * 测试用例: 测试getUserUsingUid(long)方法
	 * 测试数据: 使用不存在的用户唯一标识符 
	 * 预期结果: 返回空引用
	 */
	@Test
	public void testGetUserUsingUidNotExists() {
		User user = userMapper.getUserUsingUid(0);
		Assert.assertNull(user);
	}
	
	/**
	 * 测试用例: 测试getUserUsingUsername(String)方法
	 * 测试数据: 使用用户名为zjhzxhz的用户
	 * 预期结果: 返回预期的用户对象
	 */
	@Test
	public void testGetUserUsingUsernameExists() {
		User user = userMapper.getUserUsingUsername("Zjhzxhz");
		Assert.assertNotNull(user);
		
		long uid = user.getUid();
		Assert.assertEquals(1, uid);
	}
	
	/**
	 * 测试用例: 测试getUserUsingUsername(String)方法
	 * 测试数据: 使用不存在的用户名
	 * 预期结果: 返回空引用
	 */
	@Test
	public void testGetUserUsingUsernameNotExists() {
		User user = userMapper.getUserUsingUsername("Not-Exists");
		Assert.assertNull(user);
	}
	
	/**
	 * 测试用例: 测试getUserUsingEmail(String)方法
	 * 测试数据: 使用用户名为zjhzxhz的用户的电子邮件地址
	 * 预期结果: 返回预期的用户对象
	 */
	@Test
	public void testGetUserUsingEmailExists() {
		User user = userMapper.getUserUsingEmail("Zjhzxhz@gmail.com");
		Assert.assertNotNull(user);
		
		long uid = user.getUid();
		Assert.assertEquals(1, uid);
	}
	
	/**
	 * 测试用例: 测试getUserUsingEmail(String)方法
	 * 测试数据: 使用不存在的电子邮件地址
	 * 预期结果: 返回空引用
	 */
	@Test
	public void testGetUserUsingEmailNotExists() {
		User user = userMapper.getUserUsingEmail("not-exists@zjhzxhz.com");
		Assert.assertNull(user);
	}
	
	/**
	 * 测试用例: 测试createUser(User)方法
	 * 测试数据: 使用合法的数据集, 并且数据表中不存在相同用户名和电子邮件地址
	 * 预期结果: 数据插入操作成功完成
	 */
	@Test
	public void testCreateUserNormal() {
		UserGroup userGroup = new UserGroup(1, "users", "Users");
		Language language = new Language(2, "text/x-c++", "C++");
		User user = new User("new-user-0xff", "Password","noreply@zjhzxhz.com", userGroup, language);
		
		userMapper.createUser(user);
	}
	
	/**
	 * 测试用例: 测试createUser(User)方法
	 * 测试数据: 使用合法的数据集, 但数据表中已存在相同的用户名
	 * 预期结果: 抛出org.springframework.dao.DuplicateKeyException异常
	 */
	@Test(expected = org.springframework.dao.DuplicateKeyException.class)
	public void testCreateUserUsingExistingUsername() {
		UserGroup userGroup = new UserGroup(1, "users", "Users");
		Language language = new Language(2, "text/x-c++", "C++");
		User user = new User("zjhzxhz", "Password","noreply@zjhzxhz.com", userGroup, language);
		
		userMapper.createUser(user);
	}
	
	/**
	 * 测试用例: 测试createUser(User)方法
	 * 测试数据: 使用合法的数据集, 但数据表中已存在相同的电子邮件地址
	 * 预期结果: 抛出org.springframework.dao.DuplicateKeyException异常
	 */
	@Test(expected = org.springframework.dao.DuplicateKeyException.class)
	public void testCreateUserUsingExistingEmail() {
		UserGroup userGroup = new UserGroup(1, "users", "Users");
		Language language = new Language(2, "text/x-c++", "C++");
		User user = new User("new-user-0xfe", "Password","zjhzxhz@gmail.com", userGroup, language);
		
		userMapper.createUser(user);
	}
	
	/**
	 * 测试用例: 测试createUser(User)方法
	 * 测试数据: 使用不合法的数据集(过长的用户名)
	 * 预期结果: 抛出org.springframework.dao.DataIntegrityViolationException异常
	 */
	@Test(expected = org.springframework.dao.DataIntegrityViolationException.class)
	public void testCreateUserUsingNotExistsLanguage() {
		UserGroup userGroup = new UserGroup(1, "users", "Users");
		Language language = new Language(2, "text/x-c++", "C++");
		User user = new User("new-user-0xffffffff", "Password","noreply@zjhzxhz.com", userGroup, language);
		
		userMapper.createUser(user);
	}
	
	/**
	 * 测试用例: 测试createUser(User)方法
	 * 测试数据: 使用不合法的数据集(不存在的编程语言)
	 * 预期结果: 抛出org.springframework.dao.DataIntegrityViolationException异常
	 */
	@Test(expected = org.springframework.dao.DataIntegrityViolationException.class)
	public void testCreateUserUsingTooLongUsername() {
		UserGroup userGroup = new UserGroup(1, "users", "Users");
		Language language = new Language(0, "text/x-c++", "C++");
		User user = new User("new-user-0xfe", "Password","noreply@zjhzxhz.com", userGroup, language);
		
		userMapper.createUser(user);
	}
	
	/**
	 * 测试用例: 测试updateUser(User)方法
	 * 测试数据: 使用合法的数据集, 并且数据表中不存在相同用户名和电子邮件地址
	 * 预期结果: 数据插入操作成功完成
	 */
	@Test
	public void testUpdateUserNormal() {
		User user = userMapper.getUserUsingUid(1);
		Assert.assertNotNull(user);
		
		user.setEmail("zjhzxhz@hit.edu.cn");
		userMapper.updateUser(user);
	}
	
	/**
	 * 测试用例: 测试updateUser(User)方法
	 * 测试数据: 使用合法的数据集, 并且数据表中不存在相同用户名和电子邮件地址
	 * 预期结果: 数据插入操作成功完成
	 */
	@Test(expected = org.springframework.dao.DuplicateKeyException.class)
	public void testUpdateUserUsingExistingEmail() {
		User user = userMapper.getUserUsingUid(1);
		Assert.assertNotNull(user);
		
		user.setEmail("support@zjhzxhz.com");
		userMapper.updateUser(user);
	}
	
	/**
	 * 测试用例: 测试deleteUser(long)方法
	 * 测试数据: 不存在的用户唯一标识符
	 * 预期结果: 方法正常执行, 未影响数据表中的数据
	 */
	@Test
	public void testDeleteUserExists() {
		User user = userMapper.getUserUsingUid(1);
		Assert.assertNotNull(user);
		
		userMapper.deleteUser(3);
		
		user = userMapper.getUserUsingUid(3);
		Assert.assertNull(user);
	}
	
	/**
	 * 测试用例: 测试deleteUser(long)方法
	 * 测试数据: 不存在的用户唯一标识符
	 * 预期结果: 方法正常执行, 未影响数据表中的数据
	 */
	@Test
	public void testDeleteUserNotExists() {
		User user = userMapper.getUserUsingUid(0);
		Assert.assertNull(user);
		
		userMapper.deleteUser(0);
	}
	
	/**
	 * 待测试的UserMapper对象.
	 */
	@Autowired
	private UserMapper userMapper;
}
