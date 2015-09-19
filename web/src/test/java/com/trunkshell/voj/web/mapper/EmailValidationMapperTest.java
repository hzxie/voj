package com.trunkshell.voj.web.mapper;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import com.trunkshell.voj.web.model.EmailValidation;

import java.util.Date;

/**
 * EmailValidationMapper的测试类.
 * 
 * @author Xie Haozhe
 */
@RunWith(SpringJUnit4ClassRunner.class)
@Transactional
@ContextConfiguration({"classpath:test-spring-context.xml"})
public class EmailValidationMapperTest {
    /**
     * 测试用例: 测试getEmailValidation(String)方法
     * 测试数据: 使用存在的电子邮件地址
     * 预期结果: 返回预期的EmailValidation对象
     */
    @Test
    public void testGetEmailValidationExists() {
        EmailValidation emailValidation = emailValidationMapper.getEmailValidation("support@zjhzxhz.com");
        Assert.assertNotNull(emailValidation);
        
        String token = emailValidation.getToken();
        Assert.assertEquals("Random-String-Generated", token);
    }

    /**
     * 测试用例: 测试getEmailValidation(String)方法
     * 测试数据: 使用不存在的电子邮件地址
     * 预期结果: 返回空引用
     */
    @Test
    public void testGetEmailValidationNotExists() {
        EmailValidation emailValidation = emailValidationMapper.getEmailValidation("not-exists@zjhzxhz.com");
        Assert.assertNull(emailValidation);
    }

    /**
     * 测试用例: 测试createEmailValidation(EmailValidation)方法
     * 测试数据: 使用合法的数据集, 并且该电子邮件地址不存在相应的记录
     * 预期结果: 数据插入操作成功完成
     */
    @Test
    public void testCreateEmailValidationNormally() {
        EmailValidation emailValidation = new EmailValidation("zjhzxhz@gmail.com", "RandomToken", new Date());
        emailValidationMapper.createEmailValidation(emailValidation);

        EmailValidation insertedEmailValidation = emailValidationMapper.getEmailValidation("zjhzxhz@gmail.com");
        Assert.assertNotNull(insertedEmailValidation);

        String token = emailValidation.getToken();
        Assert.assertEquals("RandomToken", token);
    }

    /**
     * 测试用例: 测试createEmailValidation(EmailValidation)方法
     * 测试数据: 使用合法的数据集, 但已存在该电子邮件地址对应的记录
     * 预期结果: 抛出DuplicateKeyException异常
     */
    @Test(expected = org.springframework.dao.DuplicateKeyException.class)
    public void testCreateEmailValidationUsingExistingEmail() {
        EmailValidation emailValidation = new EmailValidation("support@zjhzxhz.com", "RandomToken", new Date());
        emailValidationMapper.createEmailValidation(emailValidation);
    }

    /**
     * 测试用例: 测试createEmailValidation(EmailValidation)方法
     * 测试数据: 使用合法的数据集, 但该电子邮件地址无用户使用(不满足外键参照完整性)
     * 预期结果: 抛出DataIntegrityViolationException异常
     */
    @Test(expected = org.springframework.dao.DataIntegrityViolationException.class)
    public void testCreateEmailValidationUsingNotExistingEmail() {
        EmailValidation emailValidation = new EmailValidation("not-exists@zjhzxhz.com", "RandomToken", new Date());
        emailValidationMapper.createEmailValidation(emailValidation);
    }

    /**
     * 测试用例: 测试deleteEmailValidation(String)方法
     * 测试数据: 存在的电子邮件地址
     * 预期结果: 数据删除操作成功完成
     */
    @Test
    public void testDeleteEmailValidationExists() {
        EmailValidation emailValidation = emailValidationMapper.getEmailValidation("support@zjhzxhz.com");
        Assert.assertNotNull(emailValidation);

        emailValidationMapper.deleteEmailValidation("support@zjhzxhz.com");

        emailValidation = emailValidationMapper.getEmailValidation("support@zjhzxhz.com");
        Assert.assertNull(emailValidation);
    }

    /**
     * 测试用例: 测试deleteEmailValidation(String)方法
     * 测试数据: 不存在的电子邮件地址
     * 预期结果: 方法正常执行, 未影响数据表中的数据
     */
    @Test
    public void testDeleteEmailValidationNotExists() {
        emailValidationMapper.deleteEmailValidation("not-exist@zjhzxhz.com");
    }
    
    /**
     * 待测试的EmailValidationMapper对象.
     */
    @Autowired
    private EmailValidationMapper emailValidationMapper;
}
