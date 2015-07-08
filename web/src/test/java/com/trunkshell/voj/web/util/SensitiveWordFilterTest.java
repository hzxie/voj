package com.trunkshell.voj.web.util;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.transaction.TransactionConfiguration;
import org.springframework.transaction.annotation.Transactional;

/**
 * SensitiveWordFilter的测试类
 * @author Zhou YiHao
 */
@RunWith(SpringJUnit4ClassRunner.class)
@Transactional
@TransactionConfiguration(defaultRollback = true)
@ContextConfiguration({"classpath:test-spring-context.xml"})
public class SensitiveWordFilterTest {
    /**
     * 测试用例：测试filter()方法
     * 测试数据: 包含敏感词的数据(法轮大法)
     * 预期结果: 过滤敏感词后的数据
     */
    @Test
    public void testFilterUsingSensitiveWord() {
        Assert.assertNotNull(filter);
        Assert.assertEquals("**大法好 你好", filter.filter("法轮大法好 你好"));
    }
    
    /**
     * 自动注入的SensitiveWordFilter对象.
     */
    @Autowired
    private SensitiveWordFilter filter;
}
