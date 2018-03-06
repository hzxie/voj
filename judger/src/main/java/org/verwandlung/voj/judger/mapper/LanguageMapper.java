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
package org.verwandlung.voj.judger.mapper;

import java.util.List;

import org.apache.ibatis.annotations.CacheNamespace;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.Select;

import org.verwandlung.voj.judger.model.Language;

/**
 * Language Data Access Object.
 * @author Haozhe Xie
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
		 @Result(property = "languageName", column = "language_name"),
		 @Result(property = "compileCommand", column = "language_compile_command"),
		 @Result(property = "runCommand", column = "language_run_command"),
	})
	Language getLanguageUsingId(@Param("languageId") int languageId);
	
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
		 @Result(property = "languageName", column = "language_name"),
		 @Result(property = "compileCommand", column = "language_compile_command"),
		 @Result(property = "runCommand", column = "language_run_command"),
	})
	Language getLanguageUsingSlug(@Param("languageSlug") String languageSlug);
	
	/**
	 * 获取支持的编程语言.
	 * @return 编程语言列表(List<Language>对象)
	 */
	@Select("SELECT * FROM voj_languages")
	@Options(useCache = true)
	@Results({
		 @Result(property = "languageId", column = "language_id"),
		 @Result(property = "languageSlug", column = "language_slug"),
		 @Result(property = "languageName", column = "language_name"),
		 @Result(property = "compileCommand", column = "language_compile_command"),
		 @Result(property = "runCommand", column = "language_run_command"),
	})
	List<Language> getAllLanguages();
}
