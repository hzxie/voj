package com.happystudio.voj.service;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import com.happystudio.voj.dao.UserDao;
import com.happystudio.voj.model.Language;
import com.happystudio.voj.model.User;
import com.happystudio.voj.model.UserGroup;
import com.happystudio.voj.util.DigestUtils;

/**
 * UserService测试类.
 * @author Xie Haozhe
 */
public class UserServiceTest {
    /**
     * 初始化Mock对象.
     */
    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }
    
    /**
     * 测试用例: 测试isAccountValid方法
     * 测试数据: 使用正确的用户名和密码
     * 预期结果: 返回预期的User对象
     */
    @Test
    public void testIsAccountValidCorrect() {
        String username = "zjhzxhz";
        String password = DigestUtils.md5Hex("zjhzxhz");
        Language preferLanguage = new Language(2, "text/x-c++-src", "C++");
        UserGroup userGroup = new UserGroup(3, "administrators", "Administrators");
        
        User user = new User(username, password, "zjhzxhz@gmail.com", userGroup, preferLanguage);
        Mockito.when(userDao.getUserUsingUsername("zjhzxhz")).thenReturn(user);
        
        Assert.assertEquals(user, userService.isAccountValid(username, password));
    }
    
    /**
     * 测试用例: 测试isAccountValid方法
     * 测试数据: 不存在的用户名
     * 预期结果: 返回空引用
     */
    @Test
    public void testIsAccountValidUserNotExists() {
        String username = "zjhzxhz";
        String password = DigestUtils.md5Hex("zjhzxhz");
        Language preferLanguage = new Language(2, "text/x-c++-src", "C++");
        UserGroup userGroup = new UserGroup(3, "administrators", "Administrators");
        
        User user = new User(username, password, "zjhzxhz@gmail.com", userGroup, preferLanguage);
        Mockito.when(userDao.getUserUsingUsername("zjhzxhz")).thenReturn(user);
        
        Assert.assertNull(userService.isAccountValid("NotExists", password));
    }
    
    /**
     * 测试用例: 测试isAccountValid方法
     * 测试数据: 正确的用户名和错误的密码
     * 预期结果: 返回空引用
     */
    @Test
    public void testIsAccountValidWrongPassword() {
        String username = "zjhzxhz";
        String password = DigestUtils.md5Hex("zjhzxhz");
        Language preferLanguage = new Language(2, "text/x-c++-src", "C++");
        UserGroup userGroup = new UserGroup(3, "administrators", "Administrators");
        
        User user = new User(username, password, "zjhzxhz@gmail.com", userGroup, preferLanguage);
        Mockito.when(userDao.getUserUsingUsername("zjhzxhz")).thenReturn(user);
        
        Assert.assertNull(userService.isAccountValid(username, "WrongPassword"));
    }
    
    /**
     * 待测试的UserService对象.
     */
    @InjectMocks
    private UserService userService;
    
    /**
     * Mock的UserService对象.
     */
    @Mock
    private UserDao userDao;
}
