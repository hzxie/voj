package com.trunkshell.voj.mapper;

import org.apache.ibatis.annotations.CacheNamespace;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import com.trunkshell.voj.model.Language;

/**
 * Language Data Access Object.
 * @author Xie Haozhe
 */
@CacheNamespace(implementation = org.mybatis.caches.ehcache.EhcacheCache.class)
public interface LanguageMapper {
	/**
	 * 通过编程语言的唯一标识符获取编程语言对象.
	 * @param languageId - 编程语言的唯一标识符
	 * @return 预期的编程语言对象或空引用
	 */
	@Select("SELECT * FROM voj_languages WHERE language_id = #{languageId}")
	@Options(useCache = true)
	@Results({
		 @Result(property = "languageId", column = "language_id"),
		 @Result(property = "languageSlug", column = "language_slug"),
		 @Result(property = "languageName", column = "language_name")
	})
	public Language getLanguageUsingId(@Param("languageId") int languageId);
	
	/**
	 * 通过编程语言的唯一英文缩写获取编程语言对象.
	 * @param languageSlug - 编程语言的唯一英文缩写
	 * @return 预期的编程语言对象或空引用
	 */
	@Select("SELECT * FROM voj_languages WHERE language_slug = #{languageSlug}")
	@Options(useCache = true)
	@Results({
		 @Result(property = "languageId", column = "language_id"),
		 @Result(property = "languageSlug", column = "language_slug"),
		 @Result(property = "languageName", column = "language_name")
	})
	public Language getLanguageUsingSlug(@Param("languageSlug") String languageSlug);
	
	/**
	 * 添加编程语言对象.
	 * @param language - 待添加的编程语言对象 
	 */
	@Insert("INSERT INTO voj_languages (language_id, language_slug, language_name) VALUES (#{languageId}, #{languageSlug}, #{languageName})")
	@Options(useGeneratedKeys = true, keyProperty = "languageId", keyColumn = "language_id", flushCache = true)
	public void createLanguage(Language language);
	
	/**
	 * 更新编程语言对象.
	 * @param language - 待更新的编程语言对象
	 */
	@Update("UPDATE voj_languages SET language_slug = #{languageSlug}, language_name = #{languageName} WHERE language_id = #{languageId}")
	@Options(flushCache = true)
	public void updateLanguage(Language language);
	
	/**
	 * 删除编程语言对象.
	 * @param languageId - 编程语言的唯一标识符
	 */
	@Delete("DELETE FROM voj_languages WHERE language_id = #{languageId}")
	@Options(flushCache = true)
	public void deleteLanguage(int languageId);
}
