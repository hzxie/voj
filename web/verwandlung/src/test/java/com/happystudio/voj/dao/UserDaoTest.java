package com.happystudio.voj.dao;

import javax.transaction.Transactional;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.transaction.TransactionConfiguration;

import com.happystudio.voj.model.Language;
import com.happystudio.voj.model.User;
import com.happystudio.voj.model.UserGroup;
import com.happystudio.voj.util.DigestUtils;

/**
 * UserDao的测试类.
 * @author Xie Haozhe
 */
@RunWith(SpringJUnit4ClassRunner.class)
@Transactional
@TransactionConfiguration(defaultRollback = true)
@ContextConfiguration({"classpath:test-spring-context.xml"})
public class UserDaoTest {
    /**
     * 测试用例: 测试getUserUsingUid方法
     * 测试数据: 存在的用户唯一标识符
     * 预期结果: 返回预期的User对象
     */
    @Test
    public void testGetUserUsingUidExists() {
        User user = userDao.getUserUsingUid(1);
        Assert.assertNotNull(user);
        
        String username = user.getUsername();
        Assert.assertEquals("zjhzxhz", username);
    }
    
    /**
     * 测试用例: 测试getUserUsingUid方法
     * 测试数据: 不存在的用户唯一标识符
     * 预期结果: 返回空引用
     */
    @Test
    public void testGetUserUsingUidNotExists() {
    	User user = userDao.getUserUsingUid(-65535);
        Assert.assertNull(user);
    }
    
    /**
     * 测试用例: 测试getUserUsingUsername方法
     * 测试数据: 存在的用户名
     * 预期结果: 返回预期的User对象
     */
    @Test
    public void testGetUserUsingUsernameExists() {
        User user = userDao.getUserUsingUsername("zjhzxhz");
        Assert.assertNotNull(user);
        
        String username = user.getUsername();
        Assert.assertEquals("zjhzxhz", username);
    }
    
    /**
     * 测试用例: 测试getUserUsingUsername方法
     * 测试数据: 不存在的用户名
     * 预期结果: 返回空引用
     */
    @Test
    public void testGetUserNotExists() {
        User user = userDao.getUserUsingUsername("NotExists");
        Assert.assertNull(user);
    }
    
    /**
     * 测试用例: 测试createUser方法
     * 测试数据: 使用合法的数据集, 且系统中不存在该用户
     * 预期结果: 返回true, 表示操作成功完成
     */
    @Test
    public void testcreateUserNormal() {
        Language preferLanguage = new Language(1, "text/x-c", "C");
    	UserGroup userGroup = new UserGroup(1, "users", "User");
        User user = new User("NewUser", DigestUtils.md5Hex("Password"), 
        						"user@zjhzxhz.com", userGroup, preferLanguage);
        Assert.assertTrue(userDao.createUser(user));
        
        user = userDao.getUserUsingUsername("NewUser");
        Assert.assertNotNull(user);
        
        String username = user.getUsername();
        Assert.assertEquals("NewUser", username);
    }
    
    /**
     * 测试用例: 测试createUser方法
     * 测试数据: 使用合法的数据集, 但系统中已存在该用户
     * 预期结果: 返回false, 表示操作未成功完成
     */
    @Test(expected = org.hibernate.exception.ConstraintViolationException.class)
    public void testcreateUserExists() {
    	Language preferLanguage = new Language(1, "text/x-c", "C");
        UserGroup userGroup = new UserGroup(1, "users", "User");
        User user = new User("zjhzxhz", DigestUtils.md5Hex("Password"), 
								"user@zjhzxhz.com", userGroup, preferLanguage);
        userDao.createUser(user);
    }
    
    /**
     * 测试用例: 测试createUser方法
     * 测试数据: 使用不合法的数据(缺少必填项)
     * 预期结果: 抛出DataException异常
     */
    @Test
    public void testcreateUserEmptyUsername() {
    	Language preferLanguage = new Language(1, "text/x-c", "C");
        UserGroup userGroup = new UserGroup(1, "users", "User");
        User user = new User("", DigestUtils.md5Hex("Password"), 
								"user@zjhzxhz.com", userGroup, preferLanguage);
        Assert.assertFalse(userDao.createUser(user));
    }
    
    /**
     * 测试用例: 测试createUser方法
     * 测试数据: 使用不合法的数据(过长的用户名)
     * 预期结果: 抛出DataException异常
     */
    @Test(expected = org.hibernate.exception.DataException.class)
    public void testcreateUserIllegalUsername() {
    	Language preferLanguage = new Language(1, "text/x-c", "C");
        UserGroup userGroup = new UserGroup(1, "users", "User");
        User user = new User("VeryVeryLongUsername", DigestUtils.md5Hex("Password"), 
								"user@zjhzxhz.com", userGroup, preferLanguage);
        userDao.createUser(user);
    }
    
    /**
     * 测试用例: 测试createUser方法
     * 测试数据: 使用不合法的数据(不存在的外键值)
     * 预期结果: 抛出DataException异常
     */
    @Test(expected = org.hibernate.exception.DataException.class)
    public void testcreateUserIllegalUserGroup() {
    	Language preferLanguage = new Language(1, "text/x-c", "C");
        UserGroup userGroup = new UserGroup(4, "NotExists", "不存在用户组");
        User user = new User("VeryVeryLongUsername", DigestUtils.md5Hex("Password"), 
								"user@zjhzxhz.com", userGroup, preferLanguage);
        userDao.createUser(user);
    }
    
    /**
     * 测试用例: 测试updateUser方法
     * 测试数据: 使用合法的数据集, 且系统中已存在该用户
     * 预期结果: 返回true, 表示操作成功完成
     */
    @Test
    public void testUpdateUserNormal() {
        User user = userDao.getUserUsingUid(1);
        Assert.assertNotNull(user);
        
        user.setPassword(DigestUtils.md5Hex("Password"));
        Assert.assertTrue(userDao.updateUser(user));
        
        user = userDao.getUserUsingUid(1);
        Assert.assertEquals(DigestUtils.md5Hex("Password"), user.getPassword());
    }
    
    /**
     * 测试用例: 测试updateUser方法
     * 测试数据: 使用合法的数据, 但系统中不存在该用户
     * 预期结果: 返回false, 表示操作未成功完成
     */
    @Test
    public void testUpdateNotExists() {
        User user = userDao.getUserUsingUid(-65535);
        Assert.assertNull(user);
        
        Language preferLanguage = new Language(1, "text/x-c", "C");
    	UserGroup userGroup = new UserGroup(1, "users", "User");
        user = new User(-65535, "NotExists", DigestUtils.md5Hex("Password"), 
							"user@zjhzxhz.com", userGroup, preferLanguage);
        Assert.assertFalse(userDao.updateUser(user));
    }
    
    /**
     * 测试用例: 测试updateUser方法
     * 测试数据: 使用不合法的数据(过长的密码)
     * 预期结果: 抛出DataException异常
     */
    @Test
    public void testUpdateUserIllegalPassword() {
    	Language preferLanguage = new Language(2, "text/x-c++-src", "C++");
    	UserGroup userGroup = new UserGroup(3, "administrators", "Administrators");
    	User user = new User("zjhzxhz", DigestUtils.md5Hex("Password") + "X", 
								"zjhzxhz@gmail.com", userGroup, preferLanguage);
    	Assert.assertFalse(userDao.updateUser(user));
    }
    
    /**
     * 测试用例: 测试updateUser方法
     * 测试数据: 使用不合法的数据(不存在的外键值)
     * 预期结果: 抛出DataException异常
     */
    @Test
    public void testUpdateUserIllegalUserGroup() {
    	Language preferLanguage = new Language(2, "text/x-c++-src", "C++");
        UserGroup userGroup = new UserGroup(4, "NotExists", "不存在用户组");
        User user = new User("zjhzxhz", DigestUtils.md5Hex("Password") + "X", 
								"zjhzxhz@gmail.com", userGroup, preferLanguage);
        Assert.assertFalse(userDao.updateUser(user));
    }
    
    /**
     * 测试用例: 测试deleteUser方法
     * 测试数据: 存在的用户唯一标识符
     * 预期结果: 返回true, 表示操作成功完成
     */
    @Test
    public void testDeleteUserExists() {
        User user = userDao.getUserUsingUid(1);
        Assert.assertNotNull(user);
        
        Assert.assertTrue(userDao.deleteUser(1));
        
        user = userDao.getUserUsingUid(1);
        Assert.assertNull(user);
    }
    
    /**
     * 测试用例: 测试deleteUser方法
     * 测试数据: 不存在的用户唯一标识符
     * 预期结果: 返回false, 表示操作未成功完成
     */
    @Test
    public void testDeleteUserNotExists() {
        User user = userDao.getUserUsingUid(-65535);
        Assert.assertNull(user);
        
        Assert.assertFalse(userDao.deleteUser(-65535));
    }
    
    /**
     * 待测试的UserDAO对象.
     */
    @Autowired
    private UserDao userDao;
}
