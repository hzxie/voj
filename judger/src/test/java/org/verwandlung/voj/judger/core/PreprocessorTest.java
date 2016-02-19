package org.verwandlung.voj.judger.core;

import java.io.FileInputStream;

import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import org.verwandlung.voj.judger.mapper.SubmissionMapper;
import org.verwandlung.voj.judger.model.Submission;

/**
 * 预处理器的测试类.
 * 
 * @author Haozhe Xie
 */
@RunWith(SpringJUnit4ClassRunner.class)
@Transactional
@ContextConfiguration({"classpath:test-spring-context.xml"})
public class PreprocessorTest {
    /**
     *  测试用例: 测试createTestCode(Submission, String, String)方法
     *  测试数据: 使用存在的提交记录(Submission#1000)
     *  预期结果: 在指定位置创建源代码文件
     * @throws Exception 
     */
    @Test
    public void testCreateTestCodeCpp() throws Exception {
        Submission submission = submissionMapper.getSubmission(1000);
        String workDirectory = "/tmp/voj-1000/";
        String baseFileName = "random-name";
        
        preprocessor.createTestCode(submission, workDirectory, baseFileName);
        FileInputStream inputStream = new FileInputStream("/tmp/voj-1000/random-name.cpp");
        String code = IOUtils.toString(inputStream);
        inputStream.close();
        Assert.assertEquals(submission.getCode(), code);
    }
    
    /**
     *  测试用例: 测试createTestCode(Submission, String, String)方法
     *  测试数据: 使用存在的提交记录(Submission#1001)
     *  预期结果: 在指定位置创建源代码文件
     * @throws Exception 
     */
    @Test
    public void testCreateTestCodeJava() throws Exception {
        Submission submission = submissionMapper.getSubmission(1001);
        String workDirectory = "/tmp/voj-1001/";
        String baseFileName = "RandomName";
        
        preprocessor.createTestCode(submission, workDirectory, baseFileName);
        FileInputStream inputStream = new FileInputStream("/tmp/voj-1001/RandomName.java");
        String code = IOUtils.toString(inputStream).replace("RandomName", "Main");
        inputStream.close();
        Assert.assertEquals(submission.getCode(), code);
    }
    
    /**
     * 待测试的Preprocessor对象.
     */
    @Autowired
    private Preprocessor preprocessor;
    
    /**
     * 自动注入的SubmissionMapper对象.
     * 用于构造测试用例.
     */
    @Autowired
    private SubmissionMapper submissionMapper;
}
