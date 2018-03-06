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

import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

import org.verwandlung.voj.web.model.Language;

/**
 * LanguageMapper测试类.
 * @author Haozhe Xie
 */
@ExtendWith(SpringExtension.class)
@Transactional
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
		Assertions.assertNotNull(language);
		
		String languageName = language.getLanguageName();
		Assertions.assertEquals("C", languageName);
	}
	
	/**
	 * 测试用例: 测试getLanguageUsingId(int)方法
	 * 测试数据: 不存在的编程语言唯一标识符
	 * 预期结果: 返回空引用
	 */
	@Test
	public void testGetLanguageUsingIdNotExists() {
		Language language = languageMapper.getLanguageUsingId(0);
		Assertions.assertNull(language);
	}
	
	/**
	 * 测试用例: 测试getLanguageUsingSlug(String)方法
	 * 测试数据: C语言的编程语言别名
	 * 预期结果: 返回C语言的编程语言对象
	 */
	@Test
	public void testGetLanguageUsingSlugExists() {
		Language language = languageMapper.getLanguageUsingSlug("text/x-csrc");
		Assertions.assertNotNull(language);
		
		String languageName = language.getLanguageName();
		Assertions.assertEquals("C", languageName);
	}
	
	/**
	 * 测试用例: 测试getLanguageUsingSlug(String)方法
	 * 测试数据: 不存在的编程语言别名
	 * 预期结果: 返回空引用
	 */
	@Test
	public void testGetLanguageUsingSlugNotExists() {
		Language language = languageMapper.getLanguageUsingSlug("Not-Exists");
		Assertions.assertNull(language);
	}
	
	/**
	 * 测试用例: 测试getAllLanguages()方法
	 * 测试数据: N/a
	 * 预期结果: 返回全部的编程语言列表(共6种语言)
	 */
	@Test
	public void testGetAllLanguages() {
		List<Language> languages = languageMapper.getAllLanguages();
		Assertions.assertNotNull(languages);
		Assertions.assertEquals(6, languages.size());
		
		Language firstLanguage = languages.get(0);
		Assertions.assertNotNull(firstLanguage);
		
		String languageName = firstLanguage.getLanguageName();
		Assertions.assertEquals("C", languageName);
	}
	
	/**
	 * 测试用例: 测试createLanguage(Language)方法
	 * 测试数据: 合法的数据集
	 * 预期结果: 数据插入操作成功完成
	 */
	@Test
	public void testCreateLanguageNormally() {
		Language language = new Language("text/x-php", "PHP", "php foo.php", "php foo.php");
		int numberOfRowsAffected = languageMapper.createLanguage(language);
		Assertions.assertEquals(1, numberOfRowsAffected);
	}
	
	/**
	 * 测试用例: 测试createLanguage(Language)方法
	 * 测试数据: 不合法的数据集(过长的编程语言英文缩写)
	 * 预期结果: 抛出DataIntegrityViolationException异常
	 */
	@Test
	public void testCreateLanguageUsingTooLongSlug() {
		Language language = new Language("TooLongLanguageSlug", "Invalid Langauge", "Compile Command", "Run Command");
		Executable e = () -> {
			languageMapper.createLanguage(language);
		};
		Assertions.assertThrows(org.springframework.dao.DataIntegrityViolationException.class, e);
	}
	
	/**
	 * 测试用例: 测试updateLanguage(Language)方法
	 * 测试数据: 合法的数据集, 且数据表中存在对应ID的记录
	 * 预期结果: 数据更新操作成功完成
	 */
	@Test
	public void testUpdateLanguageNormally() {
		Language language = languageMapper.getLanguageUsingId(2);
		Assertions.assertNotNull(language);
		
		language.setLanguageName("D");
		int numberOfRowsAffected = languageMapper.updateLanguage(language);
		Assertions.assertEquals(1, numberOfRowsAffected);
		
		language = languageMapper.getLanguageUsingId(2);
		Assertions.assertEquals("D", language.getLanguageName());
	}
	
	/**
	 * 测试用例: 测试updateLanguage(Language)方法
	 * 测试数据: 合法的数据集, 但数据表中不存在该编程语言
	 * 预期结果: 方法正常执行, 未影响数据表中的数据
	 */
	@Test
	public void testUpdateLanguageNotExists() {
		Language language = new Language(0, "not-exist", "Not Exist", "Not Exist", "Not Exist");
		int numberOfRowsAffected = languageMapper.updateLanguage(language);
		Assertions.assertEquals(0, numberOfRowsAffected);
	}
	
	/**
	 * 测试用例: 测试deleteLanguage(int)方法
	 * 测试数据: Ruby语言的编程语言唯一标识符
	 * 预期结果: 数据删除操作成功完成
	 */
	@Test
	public void testDeleteLanguageExists() {
		Language language = languageMapper.getLanguageUsingId(6);
		Assertions.assertNotNull(language);
		
		int numberOfRowsAffected = languageMapper.deleteLanguage(6);
		Assertions.assertEquals(1, numberOfRowsAffected);
		
		language = languageMapper.getLanguageUsingId(6);
		Assertions.assertNull(language);
	}
	
	/**
	 * 测试用例: 测试deleteLanguage(int)方法
	 * 测试数据: 不存在的编程语言唯一标识符
	 * 预期结果: 方法正常执行, 未影响数据表中的数据
	 */
	@Test
	public void testDeleteLanguageNotExists() {
		Language language = languageMapper.getLanguageUsingId(0);
		Assertions.assertNull(language);
		
		int numberOfRowsAffected = languageMapper.deleteLanguage(0);
		Assertions.assertEquals(0, numberOfRowsAffected);
	}
	
	/**
	 * 待测试的LanguageMapper对象.
	 */
	@Autowired
	private LanguageMapper languageMapper;
}
