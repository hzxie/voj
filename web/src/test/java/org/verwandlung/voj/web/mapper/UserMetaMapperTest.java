package org.verwandlung.voj.web.mapper;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import org.verwandlung.voj.web.model.User;
import org.verwandlung.voj.web.model.UserMeta;

/**
 * UserMetaMapper测试类.
 * 
 * @author Haozhe Xie
 */
@RunWith(SpringJUnit4ClassRunner.class)
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
		calendar.set(2014, 9, 7, 0, 0, 0);
		Date startTime = calendar.getTime();
		calendar.set(2014, 9, 7, 23, 59, 59);
		Date endTime = calendar.getTime();
		
		long numberOfUsers = userMetaMapper.getNumberOfUserRegistered(startTime, endTime);
		Assert.assertEquals(1, numberOfUsers);
	}
	
	/**
	 * 测试用例: 测试getUserMetaUsingUser(User)方法
	 * 测试数据: 使用Uid为1000的用户
	 * 预期结果: 返回该用户的用户元信息的列表
	 */
	@Test
	public void testGetUserMetaUsingUserExists() {
		User user = userMapper.getUserUsingUid(1000);
		Assert.assertNotNull(user);
		
		List<UserMeta> userMetaList = userMetaMapper.getUserMetaUsingUser(user);
		Assert.assertEquals(1, userMetaList.size());
		
		UserMeta userMeta = userMetaList.get(0);
		String metaKey = userMeta.getMetaKey();
		Assert.assertEquals("registerTime", metaKey);
	}
	
	/**
	 * 测试用例: 测试getUserMetaUsingUser(User)方法
	 * 测试数据: 使用不存在的用户
	 * 预期结果: 返回空列表
	 */
	@Test
	public void testGetUserMetaUsingUserNotExists() {
		User user = userMapper.getUserUsingUid(0);
		Assert.assertNull(user);
		
		List<UserMeta> userMetaList = userMetaMapper.getUserMetaUsingUser(user);
		Assert.assertEquals(0, userMetaList.size());
	}
	
	/**
	 * 测试用例: 测试getUserMetaUsingUserAndMetaKey(User, String)方法
	 * 测试数据: 使用用户UID为1000的用户和注册时间的元信息键
	 * 预期结果: 返回对应用户注册时间的元信息(UserMeta)对象
	 */
	@Test
	public void testGetUserMetaUsingUserAndMetaKeyExists() {
		User user = userMapper.getUserUsingUid(1000);
		Assert.assertNotNull(user);
		
		UserMeta userMeta = userMetaMapper.getUserMetaUsingUserAndMetaKey(user, "registerTime");
		Assert.assertNotNull(userMeta);
		
		String metaValue = userMeta.getMetaValue();
		Assert.assertEquals("2014-10-07 12:35:45", metaValue);
	}
	
	/**
	 * 测试用例: 测试getUserMetaUsingUserAndMetaKey(User, String)方法
	 * 测试数据: 使用用户UID为1000的用户和不存在的元信息键
	 * 预期结果: 返回空引用
	 */
	@Test
	public void testGetUserMetaUsingUserAndMetaKeyNotExists() {
		User user = userMapper.getUserUsingUid(1000);
		Assert.assertNotNull(user);
		
		UserMeta userMeta = userMetaMapper.getUserMetaUsingUserAndMetaKey(user, "notExistsKey");
		Assert.assertNull(userMeta);
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
		userMetaMapper.createUserMeta(userMeta);
		
		UserMeta insertedUserMeta = userMetaMapper.getUserMetaUsingUserAndMetaKey(user, "metaKey");
		Assert.assertNotNull(insertedUserMeta);
		
		String metaValue = insertedUserMeta.getMetaValue();
		Assert.assertEquals("metaValue", metaValue);
	}
	
	/**
	 * 测试用例: 测试createUserMeta(UserMeta)方法
	 * 测试数据: 使用不存在的用户
	 * 预期结果: 抛出DataIntegrityViolationException异常
	 */
	@Test(expected = org.springframework.dao.DataIntegrityViolationException.class)
	public void testCreateUserMetaUsingUserNotExists() {
		User user = userMapper.getUserUsingUid(0);
		Assert.assertNull(user);
		
		UserMeta userMeta = new UserMeta(user, "metaKey", "metaValue");
		userMetaMapper.createUserMeta(userMeta);
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
		userMetaMapper.updateUserMeta(userMeta);
		
		UserMeta updatedUserMeta = userMetaMapper.getUserMetaUsingUserAndMetaKey(user, "registerTime");
		String metaValue = updatedUserMeta.getMetaValue();
		Assert.assertEquals("newMetaValue", metaValue);
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
		userMetaMapper.updateUserMeta(userMeta);
		
		UserMeta updatedUserMeta = userMetaMapper.getUserMetaUsingUserAndMetaKey(user, "registerTime");
		String metaKey = updatedUserMeta.getMetaKey();
		Assert.assertEquals("registerTime", metaKey);
	}
	
	/**
	 * 测试用例: 测试updateUserMeta(UserMeta)方法
	 * 测试数据: 使用不存在的用户
	 * 预期结果: 方法正常执行, 未影响数据表中的数据
	 */
	@Test
	public void testUpdateUserMetaUsingUserNotExists() {
		User user = userMapper.getUserUsingUid(0);
		Assert.assertNull(user);
		
		UserMeta userMeta = userMetaMapper.getUserMetaUsingUserAndMetaKey(user, "registerTime");
		userMetaMapper.updateUserMeta(userMeta);
	}
	
	/**
	 * 测试用例: 测试deleteUserMetaUsingUser(long)方法
	 * 测试数据: 使用存在的用户UID
	 * 预期结果: 数据删除操作成功完成
	 */
	@Test
	public void testDeleteUserMetaUsingUserExists() {
		User user = userMapper.getUserUsingUid(1002);
		Assert.assertNotNull(user);
		
		userMetaMapper.deleteUserMetaUsingUser(1002);
		
		List<UserMeta> userMetaList = userMetaMapper.getUserMetaUsingUser(user);
		Assert.assertEquals(0, userMetaList.size());
	}
	
	/**
	 * 测试用例: 测试deleteUserMetaUsingUser(long)方法
	 * 测试数据: 使用不存在的用户UID
	 * 预期结果: 方法正常执行, 未影响数据表中的数据
	 */
	@Test
	public void testDeleteUserMetaUsingUserNotExists() {
		User user = userMapper.getUserUsingUid(0);
		Assert.assertNull(user);
		
		userMetaMapper.deleteUserMetaUsingUser(0);
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
