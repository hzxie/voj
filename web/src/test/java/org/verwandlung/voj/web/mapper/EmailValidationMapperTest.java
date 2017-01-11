package org.verwandlung.voj.web.mapper;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import org.verwandlung.voj.web.model.EmailValidation;

import java.util.Date;

/**
 * EmailValidationMapper的测试类.
 * 
 * @author Haozhe Xie
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
		EmailValidation emailValidation = emailValidationMapper.getEmailValidation("support@verwandlung.org");
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
		EmailValidation emailValidation = emailValidationMapper.getEmailValidation("not-exists@verwandlung.org");
		Assert.assertNull(emailValidation);
	}

	/**
	 * 测试用例: 测试createEmailValidation(EmailValidation)方法
	 * 测试数据: 使用合法的数据集, 并且该电子邮件地址不存在相应的记录
	 * 预期结果: 数据插入操作成功完成
	 */
	@Test
	public void testCreateEmailValidationNormally() {
		EmailValidation emailValidation = new EmailValidation("cshzxie@gmail.com", "RandomToken", new Date());
		int numberOfRowsAffected = emailValidationMapper.createEmailValidation(emailValidation);
		Assert.assertEquals(1, numberOfRowsAffected);

		EmailValidation insertedEmailValidation = emailValidationMapper.getEmailValidation("cshzxie@gmail.com");
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
		EmailValidation emailValidation = new EmailValidation("support@verwandlung.org", "RandomToken", new Date());
		emailValidationMapper.createEmailValidation(emailValidation);
	}

	/**
	 * 测试用例: 测试createEmailValidation(EmailValidation)方法
	 * 测试数据: 使用合法的数据集, 但该电子邮件地址无用户使用(不满足外键参照完整性)
	 * 预期结果: 抛出DataIntegrityViolationException异常
	 */
	@Test(expected = org.springframework.dao.DataIntegrityViolationException.class)
	public void testCreateEmailValidationUsingNotExistingEmail() {
		EmailValidation emailValidation = new EmailValidation("not-exists@verwandlung.org", "RandomToken", new Date());
		emailValidationMapper.createEmailValidation(emailValidation);
	}

	/**
	 * 测试用例: 测试deleteEmailValidation(String)方法
	 * 测试数据: 存在的电子邮件地址
	 * 预期结果: 数据删除操作成功完成
	 */
	@Test
	public void testDeleteEmailValidationExists() {
		EmailValidation emailValidation = emailValidationMapper.getEmailValidation("support@verwandlung.org");
		Assert.assertNotNull(emailValidation);

		int numberOfRowsAffected = emailValidationMapper.deleteEmailValidation("support@verwandlung.org");
		Assert.assertEquals(1, numberOfRowsAffected);

		emailValidation = emailValidationMapper.getEmailValidation("support@verwandlung.org");
		Assert.assertNull(emailValidation);
	}

	/**
	 * 测试用例: 测试deleteEmailValidation(String)方法
	 * 测试数据: 不存在的电子邮件地址
	 * 预期结果: 方法正常执行, 未影响数据表中的数据
	 */
	@Test
	public void testDeleteEmailValidationNotExists() {
		int numberOfRowsAffected = emailValidationMapper.deleteEmailValidation("not-exist@verwandlung.org");
		Assert.assertEquals(0, numberOfRowsAffected);
	}
	
	/**
	 * 待测试的EmailValidationMapper对象.
	 */
	@Autowired
	private EmailValidationMapper emailValidationMapper;
}
