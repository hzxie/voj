package com.trunkshell.voj.judger.mapper;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import com.trunkshell.voj.judger.model.User;

/**
 * UserMapper测试类.
 * 
 * @author Xie Haozhe
 */
@RunWith(SpringJUnit4ClassRunner.class)
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
     * 待测试的UserMapper对象.
     */
    @Autowired
    private UserMapper userMapper;
}
