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
package org.verwandlung.voj.web.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.verwandlung.voj.web.mapper.LanguageMapper;
import org.verwandlung.voj.web.mapper.SubmissionMapper;
import org.verwandlung.voj.web.mapper.UserMapper;
import org.verwandlung.voj.web.model.Language;

/**
 * 编程语言类(Language)的业务逻辑层.
 * @author Haozhe Xie
 */
@Service
@Transactional
public class LanguageService {
	/**
	 * 获取支持的编程语言.
	 * @return 编程语言列表(List<Language>对象)
	 */
	public List<Language> getAllLanguages() {
		return languageMapper.getAllLanguages();
	}
	
	/**
	 * 通过编程语言的别名获取编程语言对象.
	 * @param languageSlug - 编程语言的别名
	 * @return 编程语言对象
	 */
	public Language getLanguageUsingSlug(String languageSlug) {
		return languageMapper.getLanguageUsingSlug(languageSlug);
	}
	
	/**
	 * 更新编程语言选项.
	 * @param languages - 包含编程语言设置的数组
	 * @return 编程语言选项的更新结果
	 */
	public Map<String, Object> updateLanguageSettings(List<Language> languages) {
		List<Language> previousLanguages = languageMapper.getAllLanguages();
		Map<String, Object> result = new HashMap<>();
		Map<String, Object> validationResult = getUpdateLanguageSettingsResult(previousLanguages, languages);
		boolean isSuccessful = (Boolean) validationResult.get("isSuccessful");
		
		if ( isSuccessful ) {
			Map<String, List<Language>> languageChanges = getLanguageChanges(previousLanguages, languages);
			
			createLanguages(languageChanges.get("languageCreated"));
			updateLanguages(languageChanges.get("languageUpdated"));
			deleteLanguages(languageChanges.get("languageDeleted"));
			result.putAll(languageChanges);
		} else {
			result.putAll(validationResult);
		}
		result.put("isSuccessful", isSuccessful);
		return result;
	}
	
	/**
	 * 检查编程语言设置的正确性.
	 * @param previousLanguages - 更新前的语言设置列表
	 * @param newLanguages - 更新后的语言设置列表
	 * @return 编程语言设置的验证结果
	 */
	private Map<String, Object> getUpdateLanguageSettingsResult(List<Language> previousLanguages, List<Language> newLanguages) {
		boolean isSuccessful = true;
		Map<String, Object> result = new HashMap<>();
		
		for ( Language language : previousLanguages ) {
			Map<String, Boolean> languageResult = new HashMap<>(4, 1);
			languageResult.put("isLanguageDeleted", isLanguageDeleted(newLanguages, language));
			languageResult.put("isLangaugeInUse", isLanguageInUse(language));
			
			boolean isLanguageSuccessful = !languageResult.get("isLanguageDeleted") || !languageResult.get("isLangaugeInUse");
			languageResult.put("isSuccessful", isLanguageSuccessful);
			result.put(language.getLanguageName(), languageResult);
			
			isSuccessful &= isLanguageSuccessful;
		}
		for ( Language language : newLanguages ) {
			Map<String, Boolean> languageResult = new HashMap<>(10, 1);
			languageResult.put("isLanguageSlugEmpty", language.getLanguageSlug().isEmpty());
			languageResult.put("isLanguageSlugLegal", isLanguageSlugLegal(language.getLanguageSlug()));
			languageResult.put("isLanguageSlugExists", isLanguageSlugExists(language.getLanguageSlug(), language.getLanguageId()));
			languageResult.put("isLanguageNameEmpty", language.getLanguageName().isEmpty());
			languageResult.put("isLanguageNameLegal", isLanguageNameLegal(language.getLanguageName()));
			languageResult.put("isCompileCommandEmpty", language.getCompileCommand().isEmpty());
			languageResult.put("isCompileCommandLegal", isCompileCommandLegal(language.getCompileCommand()));
			languageResult.put("isRunCommandEmpty", language.getRunCommand().isEmpty());
			languageResult.put("isRunCommandLegal", isRunCommandLegal(language.getRunCommand()));
			
			boolean isLanguageSuccessful = !languageResult.get("isLanguageSlugEmpty")   &&  languageResult.get("isLanguageSlugLegal")   &&
										   !languageResult.get("isLanguageSlugExists")  && !languageResult.get("isLanguageNameEmpty")   && 
											languageResult.get("isLanguageNameLegal")   && !languageResult.get("isCompileCommandEmpty") && 
											languageResult.get("isCompileCommandLegal") && !languageResult.get("isRunCommandEmpty")	 &&  
											languageResult.get("isRunCommandLegal");
			languageResult.put("isSuccessful", isLanguageSuccessful);
			result.put(language.getLanguageName(), languageResult);
			
			isSuccessful &= isLanguageSuccessful;
		}
		result.put("isSuccessful", isSuccessful);
		return result;
	}
	
	/**
	 * 检查编程语言设置的更改情况.
	 * @param previousLanguages - 更新前的编程语言设置
	 * @param newLanguages - 更新后的编程语言设置
	 * @return 包含编程语言设置的更改情况的Map<String, List<Language>>对象
	 */
	private Map<String, List<Language>> getLanguageChanges(List<Language> previousLanguages, List<Language> newLanguages) {
		List<Language> languageCreated = new ArrayList<>();
		List<Language> languageUpdated = new ArrayList<>();
		List<Language> languageDeleted = new ArrayList<>();
		
		for ( Language language : newLanguages ) {
			int languageId = language.getLanguageId();
			if ( languageId == 0 ) {
				languageCreated.add(language);
			} else if ( isLanguageUpdated(previousLanguages, language) ) {
				languageUpdated.add(language);
			}
		}
		for ( Language pLanguage : previousLanguages ) {
			int languageId = pLanguage.getLanguageId();
			if ( languageId != 0 && isLanguageDeleted(newLanguages, pLanguage) ) {
				languageDeleted.add(pLanguage);
			}
		}
		Map<String, List<Language>> languageChanges = new HashMap<String, List<Language>>();
		languageChanges.put("languageCreated", languageCreated);
		languageChanges.put("languageUpdated", languageUpdated);
		languageChanges.put("languageDeleted", languageDeleted);
		return languageChanges;
	}
	
	/**
	 * 检查编程语言是否被更新.
	 * @param previousLanguages - 更新前的编程语言列表
	 * @param language - 需要被检查的编程语言对象
	 * @return 编程语言是否被更新
	 */
	private boolean isLanguageUpdated(List<Language> previousLanguages, Language language) {
		Language previousLanguage = null;
		for ( Language pLanguage : previousLanguages ) {
			if ( pLanguage.getLanguageId() == language.getLanguageId() ) {
				previousLanguage = pLanguage;
				break;
			}
		}
		
		if ( previousLanguage == null ) {
			return false;
		}
		return !previousLanguage.getLanguageSlug().equals(language.getLanguageSlug()) ||
			   !previousLanguage.getLanguageName().equals(language.getLanguageName()) ||
			   !previousLanguage.getCompileCommand().equals(language.getCompileCommand()) ||
			   !previousLanguage.getRunCommand().equals(language.getRunCommand());
	}
	
	/**
	 * 检查编程语言是否已从列表中删除.
	 * @param newLanguages - 更新后的编程语言列表
	 * @param language - 需要被检查的编程语言对象
	 * @return 编程语言是否已从列表中删除
	 */
	private boolean isLanguageDeleted(List<Language> newLanguages, Language language) {
		int languageId = language.getLanguageId();
		for ( Language newLanguage : newLanguages ) {
			if ( newLanguage.getLanguageId() == languageId ) {
				return false;
			}
		}
		return true;
	}
	
	/**
	 * 创建编程语言.
	 * @param languages - 待创建的编程语言
	 */
	private void createLanguages(List<Language> languages) {
		if ( languages == null || languages.isEmpty() ) {
			return;
		}
		for ( Language language : languages ) {
			languageMapper.createLanguage(language);
		}
	}
	
	/**
	 * 更新编程语言.
	 * @param languages - 待更新的编程语言
	 */
	private void updateLanguages(List<Language> languages) {
		if ( languages == null || languages.isEmpty() ) {
			return;
		}
		for ( Language language : languages ) {
			languageMapper.updateLanguage(language);
		}
	}

	/**
	 * 删除编程语言.
	 * @param languages - 待删除的编程语言
	 */
	private void deleteLanguages(List<Language> languages) {
		if ( languages == null || languages.isEmpty() ) {
			return;
		}
		for ( Language language : languages ) {
			int languageId = language.getLanguageId();
			languageMapper.deleteLanguage(languageId);
		}
	}

	/**
	 * 检查编程语言别名是否合法.
	 * 规则: 合法的编程语言别名不应该超过16个字符
	 * @param languageSlug - 编程语言的别名
	 * @return 编程语言别名的合法性
	 */
	private boolean isLanguageSlugLegal(String languageSlug) {
		return languageSlug.length() <= 16;
	}
	
	/**
	 * 检查编程语言的别名是否存在.
	 * @param languageSlug - 编程语言的别名
	 * @param languageId - 编程语言的唯一标识符
	 * @return 编程语言的别名是否存在
	 */
	private boolean isLanguageSlugExists(String languageSlug, int languageId) {
		Language expectedLanguage = languageMapper.getLanguageUsingSlug(languageSlug);
		
		return expectedLanguage != null && 
				expectedLanguage.getLanguageId() != languageId;
	}

	/**
	 * 检查编程语言名称是否合法.
	 * 规则: 合法的编程语言名称不应该超过16个字符
	 * @param languageName - 编程语言的名称
	 * @return 编程语言名称的合法性
	 */
	private boolean isLanguageNameLegal(String languageName) {
		return languageName.length() <= 16;
	}

	/**
	 * 检查编程语言的编译命令是否合法.
	 * 规则: 编程语言的编译命令不应该超过128个字符, 且应包含{filename}字段
	 * @param compileCommand - 编程语言的编译命令
	 * @return 编程语言编译命令的合法性
	 */
	private boolean isCompileCommandLegal(String compileCommand) {
		return compileCommand.length() <= 128 && 
				compileCommand.matches(".*\\{filename\\}.*");
	}

	/**
	 * 检查编程语言的运行命令是否合法.
	 * 规则: 编程语言的运行命令不应该超过128个字符, 且应包含{filename}字段
	 * @param runCommand - 编程语言的运行命令
	 * @return 编程语言运行命令的合法性
	 */
	private boolean isRunCommandLegal(String runCommand) {
		return runCommand.length() <= 128 && 
				runCommand.matches(".*\\{filename\\}.*");
	}
	
	/**
	 * 检查是否存在该语言的其他记录, 若存在, 由于数据库外键完整性约束则无法删除.
	 * @param language - 待检查的编程语言
	 * @return 是否存在该语言的提交记录
	 */
	private boolean isLanguageInUse(Language language) {
		if ( language == null ) {
			return false;
		}
		
		int languageId = language.getLanguageId();
		return userMapper.getNumberOfUsersUsingLanguage(languageId) != 0 ||
				submissionMapper.getNumberOfSubmissionsUsingLanguage(languageId) != 0;
	}
	
	/**
	 * 自动注入的LanguageMapper对象.
	 */
	@Autowired
	private LanguageMapper languageMapper;
	
	/**
	 * 自动注入的SubmissionMapper对象.
	 * 用于查询和语言相关联的提交记录.
	 */
	@Autowired
	private SubmissionMapper submissionMapper;
	
	/**
	 * 自动注入的UserMapper对象.
	 * 用于查询和语言相关联的用户偏好语言.
	 */
	@Autowired
	private UserMapper userMapper;
}
