package com.trunkshell.voj.judger.mapper;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.transaction.TransactionConfiguration;
import org.springframework.transaction.annotation.Transactional;

import com.trunkshell.voj.judger.model.Problem;

/**
 * ProblemMapper测试类.
 * @author Xie Haozhe
 */
@RunWith(SpringJUnit4ClassRunner.class)
@Transactional
@TransactionConfiguration(defaultRollback = true)
@ContextConfiguration({"classpath:test-spring-context.xml"})
public class ProblemMapperTest {
    /**
     * 测试用例: 测试getProblem()方法
     * 测试数据: 使用A+B Problem的试题唯一标识符
     * 预期结果: 返回预期的试题对象
     */
    @Test
    public void testGetProblemExists() {
        Problem problem = problemMapper.getProblem(1000);
        Assert.assertNotNull(problem);
        
        String problemName = problem.getProblemName();
        Assert.assertEquals("A+B Problem", problemName);
    }
    
    /**
     * 测试用例: 测试getProblem()方法
     * 测试数据: 使用不存在的试题唯一标识符
     * 预期结果: 返回空引用
     */
    @Test
    public void testGetProblemNotExists() {
        Problem problem = problemMapper.getProblem(0);
        Assert.assertNull(problem);
    }
    
    /**
     * 待测试的ProblemMapper对象.
     */
    @Autowired
    private ProblemMapper problemMapper;
}
