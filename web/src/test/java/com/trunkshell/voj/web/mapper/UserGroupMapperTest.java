package com.trunkshell.voj.web.mapper;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.transaction.TransactionConfiguration;
import org.springframework.transaction.annotation.Transactional;

import com.trunkshell.voj.web.mapper.UserGroupMapper;
import com.trunkshell.voj.web.model.UserGroup;

/**
 * UserGroupMapper测试类.
 * @author Xie Haozhe
 */
@RunWith(SpringJUnit4ClassRunner.class)
@Transactional
@TransactionConfiguration(defaultRollback = true)
@ContextConfiguration({"classpath:test-spring-context.xml"})
public class UserGroupMapperTest {
	/**
	 * 测试用例: 测试getUserGroupUsingId(int)方法
	 * 测试数据: 普通用户(User)的用户组唯一标识符
	 * 预期结果: 返回用户(User)的用户组对象
	 */
	@Test
	public void testGetUserGroupUsingIdExists() {
		UserGroup userGroup = userGroupMapper.getUserGroupUsingId(1);
		Assert.assertNotNull(userGroup);
		
		String userGroupSlug = userGroup.getUserGroupSlug();
		Assert.assertEquals("users", userGroupSlug);
	}
	
	/**
	 * 测试用例: 测试getUserGroupUsingId(int)方法
	 * 测试数据: 不存在的用户组唯一标识符
	 * 预期结果: 返回空引用
	 */
	@Test
	public void testGetUserGroupUsingIdNotExists() {
		UserGroup userGroup = userGroupMapper.getUserGroupUsingId(0);
		Assert.assertNull(userGroup);
	}
	
	/**
	 * 测试用例: 测试getUserGroupUsingSlug(String)方法
	 * 测试数据: 普通用户(User)的用户组唯一英文缩写
	 * 预期结果: 返回用户(User)的用户组对象
	 */
	@Test
	public void testGetUserGroupUsingSlugExists() {
		UserGroup userGroup = userGroupMapper.getUserGroupUsingSlug("users");
		Assert.assertNotNull(userGroup);
		
		int userGroupId = userGroup.getUserGroupId();
		Assert.assertEquals(1, userGroupId);
	}
	
	/**
	 * 测试用例: 测试getUserGroupUsingSlug(String)方法
	 * 测试数据: 不存在的用户组唯一英文缩写
	 * 预期结果: 返回空引用
	 */
	@Test
	public void testGetUserGroupUsingSlugNotExists() {
		UserGroup userGroup = userGroupMapper.getUserGroupUsingSlug("Not-Exists");
		Assert.assertNull(userGroup);
	}
	
	/**
	 * 待测试的UserGroupMapper对象.
	 */
	@Autowired
	private UserGroupMapper userGroupMapper;
}
