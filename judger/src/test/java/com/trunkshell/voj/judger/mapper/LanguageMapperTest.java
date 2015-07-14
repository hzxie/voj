package com.trunkshell.voj.judger.mapper;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.transaction.TransactionConfiguration;
import org.springframework.transaction.annotation.Transactional;

import com.trunkshell.voj.judger.model.Language;

/**
 * LanguageMapper测试类.
 * 
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
     * 测试用例: 测试getAllLanguages()方法
     * 测试数据: N/a
     * 预期结果: 返回全部的编程语言列表(共6种语言)
     */
    @Test
    public void testGetAllLanguages() {
        List<Language> languages = languageMapper.getAllLanguages();
        Assert.assertNotNull(languages);
        Assert.assertEquals(6, languages.size());
        
        Language firstLanguage = languages.get(0);
        Assert.assertNotNull(firstLanguage);
        
        String languageName = firstLanguage.getLanguageName();
        Assert.assertEquals("C", languageName);
    }
    /**
     * 待测试的LanguageMapper对象.
     */
    @Autowired
    private LanguageMapper languageMapper;
}
