/* Verwandlung Online Judge - A cross-platform judge online system
 * Copyright (C) 2018 Haozhe Xie <cshzxie@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 *
 *                              _ooOoo_  
 *                             o8888888o  
 *                             88" . "88  
 *                             (| -_- |)  
 *                             O\  =  /O  
 *                          ____/`---'\____  
 *                        .'  \\|     |//  `.  
 *                       /  \\|||  :  |||//  \  
 *                      /  _||||| -:- |||||-  \  
 *                      |   | \\\  -  /// |   |  
 *                      | \_|  ''\---/''  |   |  
 *                      \  .-\__  `-`  ___/-. /  
 *                    ___`. .'  /--.--\  `. . __  
 *                 ."" '<  `.___\_<|>_/___.'  >'"".  
 *                | | :  `- \`.;`\ _ /`;.`/ - ` : | |  
 *                \  \ `-.   \_ __\ /__ _/   .-` /  /  
 *           ======`-.____`-.___\_____/___.-`____.-'======  
 *                              `=---=' 
 *
 *                          HERE BE BUDDHA
 *
 */
package org.verwandlung.voj.web.mapper;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

import org.verwandlung.voj.web.model.EmailValidation;

import java.util.Date;

/**
 * EmailValidationMapper的测试类.
 * 
 * @author Haozhe Xie
 */
@ExtendWith(SpringExtension.class)
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
		Assertions.assertNotNull(emailValidation);
		
		String token = emailValidation.getToken();
		Assertions.assertEquals("Random-String-Generated", token);
	}

	/**
	 * 测试用例: 测试getEmailValidation(String)方法
	 * 测试数据: 使用不存在的电子邮件地址
	 * 预期结果: 返回空引用
	 */
	@Test
	public void testGetEmailValidationNotExists() {
		EmailValidation emailValidation = emailValidationMapper.getEmailValidation("not-exists@verwandlung.org");
		Assertions.assertNull(emailValidation);
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
		Assertions.assertEquals(1, numberOfRowsAffected);

		EmailValidation insertedEmailValidation = emailValidationMapper.getEmailValidation("cshzxie@gmail.com");
		Assertions.assertNotNull(insertedEmailValidation);

		String token = emailValidation.getToken();
		Assertions.assertEquals("RandomToken", token);
	}

	/**
	 * 测试用例: 测试createEmailValidation(EmailValidation)方法
	 * 测试数据: 使用合法的数据集, 但已存在该电子邮件地址对应的记录
	 * 预期结果: 抛出DuplicateKeyException异常
	 */
	@Test
	public void testCreateEmailValidationUsingExistingEmail() {
		EmailValidation emailValidation = new EmailValidation("support@verwandlung.org", "RandomToken", new Date());
		Executable e = () -> {
			emailValidationMapper.createEmailValidation(emailValidation);
		};
		Assertions.assertThrows(org.springframework.dao.DuplicateKeyException.class, e);
	}

	/**
	 * 测试用例: 测试createEmailValidation(EmailValidation)方法
	 * 测试数据: 使用合法的数据集, 但该电子邮件地址无用户使用(不满足外键参照完整性)
	 * 预期结果: 抛出DataIntegrityViolationException异常
	 */
	@Test
	public void testCreateEmailValidationUsingNotExistingEmail() {
		EmailValidation emailValidation = new EmailValidation("not-exists@verwandlung.org", "RandomToken", new Date());
		Executable e = () -> {
			emailValidationMapper.createEmailValidation(emailValidation);
		};
		Assertions.assertThrows(org.springframework.dao.DataIntegrityViolationException.class, e);
	}

	/**
	 * 测试用例: 测试deleteEmailValidation(String)方法
	 * 测试数据: 存在的电子邮件地址
	 * 预期结果: 数据删除操作成功完成
	 */
	@Test
	public void testDeleteEmailValidationExists() {
		EmailValidation emailValidation = emailValidationMapper.getEmailValidation("support@verwandlung.org");
		Assertions.assertNotNull(emailValidation);

		int numberOfRowsAffected = emailValidationMapper.deleteEmailValidation("support@verwandlung.org");
		Assertions.assertEquals(1, numberOfRowsAffected);

		emailValidation = emailValidationMapper.getEmailValidation("support@verwandlung.org");
		Assertions.assertNull(emailValidation);
	}

	/**
	 * 测试用例: 测试deleteEmailValidation(String)方法
	 * 测试数据: 不存在的电子邮件地址
	 * 预期结果: 方法正常执行, 未影响数据表中的数据
	 */
	@Test
	public void testDeleteEmailValidationNotExists() {
		int numberOfRowsAffected = emailValidationMapper.deleteEmailValidation("not-exist@verwandlung.org");
		Assertions.assertEquals(0, numberOfRowsAffected);
	}
	
	/**
	 * 待测试的EmailValidationMapper对象.
	 */
	@Autowired
	private EmailValidationMapper emailValidationMapper;
}
