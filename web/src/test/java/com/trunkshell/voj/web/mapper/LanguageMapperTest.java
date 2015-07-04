package com.trunkshell.voj.web.mapper;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.transaction.TransactionConfiguration;
import org.springframework.transaction.annotation.Transactional;

import com.trunkshell.voj.web.mapper.LanguageMapper;
import com.trunkshell.voj.web.model.Language;

/**
 * LanguageMapper测试类.
 * @author Xie Haozhe
 */
@RunWith(SpringJUnit4ClassRunner.class)
@Transactional
@TransactionConfiguration(defaultRollback = true)
@ContextConfiguration({"classpath:test-spring-context.xml"})
public class LanguageMapperTest {
	/**
	 * 测试用例: 测试getLanguageUsingId(int)方法
	 * 测试数据: C语言的编程语言唯一标识符
	 * 预期结果: 返回C语言的编程语言对象
	 */
	@Test
	public void testGetLanguageUsingIdExists() {
		Language language = languageMapper.getLanguageUsingId(1);
		Assert.assertNotNull(language);
		
		System.out.println(language);
		
		String languageName = language.getLanguageName();
		Assert.assertEquals("C", languageName);
	}
	
	/**
	 * 测试用例: 测试getLanguageUsingId(int)方法
	 * 测试数据: 不存在的编程语言唯一标识符
	 * 预期结果: 返回空引用
	 */
	@Test
	public void testGetLanguageUsingIdNotExists() {
		Language language = languageMapper.getLanguageUsingId(0);
		Assert.assertNull(language);
	}
	
	/**
	 * 测试用例: 测试getLanguageUsingSlug(String)方法
	 * 测试数据: C语言的编程语言唯一英文缩写
	 * 预期结果: 返回C语言的编程语言对象
	 */
	@Test
	public void testGetLanguageUsingSlugExists() {
		Language language = languageMapper.getLanguageUsingSlug("text/x-csrc");
		Assert.assertNotNull(language);
		
		String languageName = language.getLanguageName();
		Assert.assertEquals("C", languageName);
	}
	
	/**
	 * 测试用例: 测试getLanguageUsingSlug(String)方法
	 * 测试数据: 不存在的编程语言唯一英文缩写
	 * 预期结果: 返回空引用
	 */
	@Test
	public void testGetLanguageUsingSlugNotExists() {
		Language language = languageMapper.getLanguageUsingSlug("Not-Exists");
		Assert.assertNull(language);
	}
	
	/**
	 * 测试用例: 测试createLanguage(Language)方法
	 * 测试数据: 合法的数据集
	 * 预期结果: 数据插入操作成功完成
	 */
	@Test
	public void testCreateLanguageNormally() {
		Language language = new Language("text/x-php", "PHP", "php foo.php", "php foo.php");
		languageMapper.createLanguage(language);
	}
	
	/**
	 * 测试用例: 测试createLanguage(Language)方法
	 * 测试数据: 不合法的数据集(过长的编程语言英文缩写)
	 * 预期结果: 抛出DataIntegrityViolationException异常
	 */
	@Test(expected = org.springframework.dao.DataIntegrityViolationException.class)
	public void testCreateLanguageUsingTooLongSlug() {
		Language language = new Language("TooLongLanguageSlug", "Invalid Langauge", "Compile Command", "Run Command");
		languageMapper.createLanguage(language);
	}
	
	/**
	 * 测试用例: 测试updateLanguage(Language)方法
	 * 测试数据: 合法的数据集, 且数据表中存在对应ID的记录
	 * 预期结果: 数据更新操作成功完成
	 */
	@Test
	public void testUpdateLanguageNormally() {
		Language language = languageMapper.getLanguageUsingId(2);
		Assert.assertNotNull(language);
		
		language.setLanguageName("D");
		languageMapper.updateLanguage(language);
		
		language = languageMapper.getLanguageUsingId(2);
		Assert.assertEquals("D", language.getLanguageName());
	}
	
	/**
	 * 测试用例: 测试updateLanguage(Language)方法
	 * 测试数据: 合法的数据集, 但数据表中不存在该编程语言
	 * 预期结果: 方法正常执行, 未影响数据表中的数据
	 */
	@Test
	public void testUpdateLanguageNotExists() {
		Language language = new Language(0, "not-exist", "Not Exist", "Not Exist", "Not Exist");
		languageMapper.updateLanguage(language);
	}
	
	/**
	 * 测试用例: 测试deleteLanguage(int)方法
	 * 测试数据: Ruby语言的编程语言唯一标识符
	 * 预期结果: 数据删除操作成功完成
	 */
	@Test
	public void testDeleteLanguageExists() {
		Language language = languageMapper.getLanguageUsingId(6);
		Assert.assertNotNull(language);
		
		languageMapper.deleteLanguage(6);
		
		language = languageMapper.getLanguageUsingId(6);
		Assert.assertNull(language);
	}
	
	/**
	 * 测试用例: 测试deleteLanguage(int)方法
	 * 测试数据: 不存在的编程语言唯一标识符
	 * 预期结果: 方法正常执行, 未影响数据表中的数据
	 */
	@Test
	public void testDeleteLanguageNotExists() {
		Language language = languageMapper.getLanguageUsingId(0);
		Assert.assertNull(language);
		
		languageMapper.deleteLanguage(0);
	}
	
	/**
	 * 待测试的LanguageMapper对象.
	 */
	@Autowired
	private LanguageMapper languageMapper;
}
