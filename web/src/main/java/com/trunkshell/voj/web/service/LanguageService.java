package com.trunkshell.voj.web.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.trunkshell.voj.web.mapper.LanguageMapper;
import com.trunkshell.voj.web.model.Language;

/**
 * 编程语言类(Language)的业务逻辑层.
 * @author Xie Haozhe
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
	 * 自动注入的LanguageMapper对象.
	 */
	@Autowired
	private LanguageMapper languageMapper;
}
