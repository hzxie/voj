package com.trunkshell.voj.web.mapper;

import java.util.List;

import org.apache.ibatis.annotations.CacheNamespace;
import org.apache.ibatis.annotations.Param;

import com.trunkshell.voj.web.model.Language;

/**
 * Language Data Access Object.
 * 
 * @author Xie Haozhe
 */
@CacheNamespace(implementation = org.mybatis.caches.ehcache.EhcacheCache.class)
public interface LanguageMapper {
    /**
     * 通过编程语言的唯一标识符获取编程语言对象.
     * @param languageId - 编程语言的唯一标识符
     * @return 预期的编程语言对象或空引用
     */
    public Language getLanguageUsingId(@Param("languageId") int languageId);
    
    /**
     * 通过编程语言的唯一英文缩写获取编程语言对象.
     * @param languageSlug - 编程语言的唯一英文缩写
     * @return 预期的编程语言对象或空引用
     */
    public Language getLanguageUsingSlug(@Param("languageSlug") String languageSlug);
    
    /**
     * 获取支持的编程语言.
     * @return 编程语言列表(List<Language>对象)
     */
    public List<Language> getAllLanguages();
    
    /**
     * 添加编程语言对象.
     * @param language - 待添加的编程语言对象 
     */
    public void createLanguage(Language language);
    
    /**
     * 更新编程语言对象.
     * @param language - 待更新的编程语言对象
     */
    public void updateLanguage(Language language);
    
    /**
     * 删除编程语言对象.
     * @param languageId - 编程语言的唯一标识符
     */
    public void deleteLanguage(@Param("languageId") int languageId);
}
