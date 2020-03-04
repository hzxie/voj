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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.alibaba.fastjson.JSON;
import org.verwandlung.voj.web.mapper.OptionMapper;
import org.verwandlung.voj.web.model.Option;

/**
 * 系统管理服务.
 * 用于完成整个系统的管理功能.
 * 
 * @author Haozhe Xie
 */
@Service
@Transactional
public class OptionService {
	/**
	 * 获取全部系统选项.
	 * @return 一个包含全部系统选项的列表
	 */
	public List<Option> getOptions() {
		return optionMapper.getOptions();
	}
	
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
	 * 更新系统选项.
	 * @param websiteName - 网站名称
	 * @param websiteDescription - 网站描述
	 * @param copyright - 网站版权信息
	 * @param allowUserRegister - 是否允许用户注册
	 * @param icpNumber - 网站备案号
	 * @param policeIcpNumber - 公安备案号
	 * @param googleAnalyticsCode - Google Analytics代码
	 * @param offensiveWords - 敏感词列表
	 * @return 包含选项更新结果的Map对象
	 */
	public Map<String, Boolean> updateOptions(String websiteName, 
			String websiteDescription,  String copyright, boolean allowUserRegister, 
			String icpNumber, String policeIcpNumber, String googleAnalyticsCode, 
			String offensiveWords) {
		Map<String, Boolean> result = new HashMap<>();
		result.put("isWebsiteNameEmpty", websiteName.isEmpty());
		result.put("isWebisteNameLegal", isWebsiteNameLegal(websiteName));
		result.put("isDescriptionEmpty", websiteDescription.isEmpty());
		result.put("isDescriptionLegal", isWebsiteDescriptionLegal(websiteDescription));
		result.put("isCopyrightEmpty", copyright.isEmpty());
		result.put("isCopyrightLegal", isCopyrightLegal(copyright));
		result.put("isIcpNumberLegal", isIcpNumberLegal(icpNumber));
		result.put("isPoliceIcpNumberLegal", isPoliceIcpNumberLegal(policeIcpNumber));
		result.put("isAnalyticsCodeLegal", isGoogleAnalyticsCodeLegal(googleAnalyticsCode));
		
		boolean isSuccessful = !result.get("isWebsiteNameEmpty") && result.get("isWebisteNameLegal")     &&
							   !result.get("isDescriptionEmpty") && result.get("isDescriptionLegal")     &&
							   !result.get("isCopyrightEmpty")   && result.get("isCopyrightLegal")       &&
								result.get("isIcpNumberLegal")   && result.get("isPoliceIcpNumberLegal") && 
								result.get("isAnalyticsCodeLegal");
		if ( isSuccessful ) {
			Map<String, String> optionMap = new HashMap<>();
			optionMap.put("websiteName", websiteName);
			optionMap.put("description", websiteDescription);
			optionMap.put("copyright", copyright);
			optionMap.put("googleAnalyticsCode", googleAnalyticsCode);
			optionMap.put("icpNumber", icpNumber);
			optionMap.put("policeIcpNumber", policeIcpNumber);
			optionMap.put("allowUserRegister", allowUserRegister ? "1" : "0");
			optionMap.put("offensiveWords", JSON.toJSONString(offensiveWords.split(",")));
			updateOptions(optionMap);
		}
		result.put("isSuccessful", isSuccessful);
		return result;
	}
	
	/**
	 * 更新系统选项.
	 * @param optionMap - 包含系统选项的键值对
	 */
	public void updateOptions(Map<String, String> optionMap) {
		for ( Entry<String, String> e : optionMap.entrySet() ) {
			String optionName = e.getKey();
			String optionValue = e.getValue();
			
			Option option = optionMapper.getOption(optionName);
			option.setOptionValue(optionValue);
			optionMapper.updateOption(option);
		}
	}
	
	/**
	 * 检查网站名称是否合法.
	 * 规则: 合法的网站名称长度不应该超过32个字符
	 * @param websiteName - 网站名称
	 * @return 网站名称的合法性
	 */
	private boolean isWebsiteNameLegal(String websiteName) {
		return websiteName.length() <= 32;
	}
	
	/**
	 * 检查网站的描述信息是否合法.
	 * 规则: 合法的网站描述信息不应该超过128个字符
	 * @param websiteDescription - 网站的描述信息
	 * @return 网站描述信息的合法性
	 */
	private boolean isWebsiteDescriptionLegal(String websiteDescription) {
		return websiteDescription.length() <= 128;
	}
	
	/**
	 * 检查网站版权信息是否合法.
	 * 规则: 合法的版权信息的长度不应该超过128个字符
	 * @param copyright - 网站的版权信息
	 * @return 版权信息的合法性
	 */
	private boolean isCopyrightLegal(String copyright) {
		return copyright.length() <= 128;
	}
	
	/**
	 * 检查网站备案号是否合法.
	 * 规则: 合法的网站备案号形如: 浙ICP备15017174号
	 * @param icpNumber - 网站备案号 
	 * @return 网站备案号的合法性
	 */
	private boolean isIcpNumberLegal(String icpNumber) {
		boolean isIcpNumberEmpty = icpNumber.isEmpty();
		boolean isIcpNumberLegal = icpNumber.matches("^.ICP备[0-9]{8}号$");
		return isIcpNumberEmpty || isIcpNumberLegal;
	}

	/**
	 * 检查公安机关备案号是否合法.
	 * 规则: 合法的公安机关备案号形如: 浙公网安备33010202000766号
	 * @param policeIcpNumber - 公安机关备案号 
	 * @return 公安机关备案号的合法性
	 */
	private boolean isPoliceIcpNumberLegal(String policeIcpNumber) {
		boolean isIcpNumberEmpty = policeIcpNumber.isEmpty();
		boolean isIcpNumberLegal = policeIcpNumber.matches("^.公网安备[\\s]*[0-9]{14}号$");
		return isIcpNumberEmpty || isIcpNumberLegal;
	}
	
	/**
	 * 检查GoogleAnalytics代码是否合法.
	 * 规则: 合法的GoogleAnalyticsCode应该被<script>标签所包括
	 * @param googleAnalyticsCode - GoogleAnalytics代码
	 * @return GoogleAnalytics代码的合法性
	 */
	private boolean isGoogleAnalyticsCodeLegal(String googleAnalyticsCode) {
		boolean isAnalyticsCodeEmpty = googleAnalyticsCode.isEmpty();
		Pattern p = Pattern.compile("<script.*>.*</script>", Pattern.DOTALL);
		boolean isAnalyticsCodeLegal = p.matcher(googleAnalyticsCode).matches();
		return isAnalyticsCodeEmpty || isAnalyticsCodeLegal;
	}
	
	/**
	 * 自动注入的OptionMapper对象.
	 */
	@Autowired
	private OptionMapper optionMapper;
}
