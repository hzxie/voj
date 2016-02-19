package org.verwandlung.voj.judger.mapper;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import org.verwandlung.voj.judger.model.Submission;

/**
 * SubmissionMapper测试类.
 * @author Haozhe Xie
 */
@RunWith(SpringJUnit4ClassRunner.class)
@Transactional
@ContextConfiguration({"classpath:test-spring-context.xml"})
public class SubmissionMapperTest {
    /**
     * 测试用例: 测试getSubmission(long)方法
     * 测试数据: Problem#1000的提交记录的唯一标识符
     * 预期结果: 返回预期的Submission对象
     */
    @Test
    public void testGetSubmissionExists() {
        Submission submission = submissionMapper.getSubmission(1000);
        Assert.assertNotNull(submission);
        
        long problemId = submission.getProblem().getProblemId();
        Assert.assertEquals(1000, problemId);
    }
    
    /**
     * 测试用例: 测试getSubmission(long)方法
     * 测试数据: 不存在的的提交记录唯一标识符
     * 预期结果: 返回空引用
     */
    @Test
    public void testGetSubmissionNotExists() {
        Submission submission = submissionMapper.getSubmission(0);
        Assert.assertNull(submission);
    }
    
    /**
     * 待测试的SubmissionMapper对象.
     */
    @Autowired
    private SubmissionMapper submissionMapper;
}
