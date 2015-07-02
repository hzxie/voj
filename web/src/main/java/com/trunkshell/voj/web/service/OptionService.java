package com.trunkshell.voj.web.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.trunkshell.voj.web.mapper.OptionMapper;
import com.trunkshell.voj.web.model.Option;

/**
 * 系统管理服务.
 * 用于完成整个系统的管理功能.
 * 
 * @author Xie Haozhe
 */
@Service
@Transactional
public class OptionService {
	/**
	 * 获取自动加载的系统选项.
	 * @return 一个包含自动加载系统选项的列表
	 */
	public List<Option> getAutoloadOptions() {
		return optionMapper.getAutoloadOptions();
	}
	
	/**
	 * 根据系统选项的名称获取选项的值.
	 * @param optionName - 系统选项的名称 
	 * @return 对应的Option对象
	 */
	public Option getOption(String optionName) {
		return optionMapper.getOption(optionName);
	}
	
	/**
	 * 自动注入的OptionMapper对象.
	 */
	@Autowired
	private OptionMapper optionMapper;
}
