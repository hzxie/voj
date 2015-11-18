package com.trunkshell.voj.judger.mapper;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import com.trunkshell.voj.judger.model.UserGroup;

/**
 * UserGroupMapper测试类.
 * 
 * @author Xie Haozhe
 */
@RunWith(SpringJUnit4ClassRunner.class)
@Transactional
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
        Assert.assertEquals("forbidden", userGroupSlug);
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
     * 待测试的UserGroupMapper对象.
     */
    @Autowired
    private UserGroupMapper userGroupMapper;
}
