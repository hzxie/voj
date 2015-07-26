package com.trunkshell.voj.web.mapper;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.transaction.TransactionConfiguration;
import org.springframework.transaction.annotation.Transactional;

import com.trunkshell.voj.web.model.Language;
import com.trunkshell.voj.web.model.User;
import com.trunkshell.voj.web.model.UserGroup;

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
     * 测试用例: 测试getNumberOfUsers(UserGroup)方法
     * 测试数据: 使用用户(Users)的用户组
     * 预期结果: 返回该用户组中用户的数量
     */
    @Test
    public void testGetNumberOfUsers() {
        UserGroup userGroup = new UserGroup(1, "users", "Users");
        long totalUsers = userMapper.getNumberOfUsersUsingUserGroup(userGroup);
        
        Assert.assertEquals(2, totalUsers);
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
        Assert.assertEquals(2, totalUsers);
    }
    
    /**
     * 测试用例: 测试getUserUsingUid(long)方法
     * 测试数据: 使用用户名为zjhzxhz的用户的唯一标识符
     * 预期结果: 返回预期的用户对象
     */
    @Test
    public void testGetUserUsingUidExists() {
        User user = userMapper.getUserUsingUid(1000);
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
        Assert.assertEquals(1000, uid);
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
        Assert.assertEquals(1000, uid);
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
     * 测试用例: 测试getUserUsingUserGroup(UserGroup, long, int)方法
     * 测试数据: 使用用户(Users)的用户组
     * 预期结果: 返回预期的用户列表(共1名用户)
     */
    @Test
    public void testGetUserUsingUserGroup() {
        UserGroup userGroup = new UserGroup(1, "users", "Users");
        List<User> users = userMapper.getUserUsingUserGroup(userGroup, 0, 1);
        Assert.assertEquals(1, users.size());
        
        User firstUser = users.get(0);
        String username = firstUser.getUsername();
        Assert.assertEquals("voj-tester", username);
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
        Language language = new Language(2, "text/x-c++", "C++", "g++ foo.cpp -o foo", "./foo");
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
        Language language = new Language(2, "text/x-c++", "C++", "g++ foo.cpp -o foo", "./foo");
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
        Language language = new Language(2, "text/x-c++", "C++", "g++ foo.cpp -o foo", "./foo");
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
        Language language = new Language(0, "not-exists", "Not Exists", "Not Exists", "Not Exists");
        User user = new User("new-user-0xfe", "Password","noreply@zjhzxhz.com", userGroup, language);
        
        userMapper.createUser(user);
    }
    
    /**
     * 测试用例: 测试updateUser(User)方法
     * 测试数据: 使用合法的数据集, 并且数据表中不存在相同用户名和电子邮件地址
     * 预期结果: 数据更新操作成功完成
     */
    @Test
    public void testUpdateUserNormally() {
        User user = userMapper.getUserUsingUid(1000);
        Assert.assertNotNull(user);
        
        user.setEmail("zjhzxhz@hit.edu.cn");
        userMapper.updateUser(user);
    }
    
    /**
     * 测试用例: 测试updateUser(User)方法
     * 测试数据: 使用合法的数据集, 并且数据表中不存在相同用户名和电子邮件地址
     * 预期结果: 数据更新操作成功完成
     */
    @Test(expected = org.springframework.dao.DuplicateKeyException.class)
    public void testUpdateUserUsingExistingEmail() {
        User user = userMapper.getUserUsingUid(1000);
        Assert.assertNotNull(user);
        
        user.setEmail("support@zjhzxhz.com");
        userMapper.updateUser(user);
    }
    
    /**
     * 测试用例: 测试deleteUser(long)方法
     * 测试数据: 存在的用户唯一标识符
     * 预期结果: 数据删除操作成功完成
     */
    @Test
    public void testDeleteUserExists() {
        User user = userMapper.getUserUsingUid(1002);
        Assert.assertNotNull(user);
        
        userMapper.deleteUser(1002);
        
        user = userMapper.getUserUsingUid(1002);
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
