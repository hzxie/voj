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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

import org.verwandlung.voj.web.model.Option;

/**
 * OptionMapper测试类.
 * 
 * @author Haozhe Xie
 */
@ExtendWith(SpringExtension.class)
@Transactional
@ContextConfiguration({"classpath:test-spring-context.xml"})
public class OptionMapperTest {
	/**
	 * 测试用例: 测试getAutoloadOptions()方法
	 * 测试数据: N/a
	 * 预期结果: 返回自动加载的系统选项的列表
	 */
	@Test
	public void testGetAutoloadOptions() {
		List<Option> options = optionMapper.getAutoloadOptions();
		Assertions.assertEquals(6, options.size());
		
		Option firstOption = options.get(0);
		int optionId = firstOption.getOptionId();
		Assertions.assertEquals(1, optionId);
		
		String optionName = firstOption.getOptionName();
		Assertions.assertEquals("websiteName", optionName);
	}
	
	/**
	 * 测试用例: 测试getOption(String)方法
	 * 测试数据: 使用存在的选项名称
	 * 预期结果: 返回预期的Option对象
	 */
	@Test
	public void testGetOptionExists() {
		Option option = optionMapper.getOption("websiteName");
		Assertions.assertNotNull(option);
		
		int optionId = option.getOptionId();
		Assertions.assertEquals(1, optionId);
	}
	
	/**
	 * 测试用例: 测试getOption(String)方法
	 * 测试数据: 使用不存在的选项名称
	 * 预期结果: 返回空引用
	 */
	@Test
	public void testGetOptionNotExists() {
		Option option = optionMapper.getOption("notExistOption");
		Assertions.assertNull(option);
	}
	
	/**
	 * 测试用例: 测试updateOption(Option)方法
	 * 测试数据: 存在的OptionId
	 * 预期结果: 数据更新操作成功完成
	 */
	@Test
	public void testUpdateOptionExists() {
		Option option = optionMapper.getOption("websiteName");
		option.setOptionValue("New OJ Platform");
		int numberOfRowsAffected = optionMapper.updateOption(option);
		Assertions.assertEquals(1, numberOfRowsAffected);
		
		Option newOption = optionMapper.getOption("websiteName");
		String optionValue = newOption.getOptionValue();
		Assertions.assertEquals("New OJ Platform", optionValue);
	}
	
	/**
	 * 测试用例: 测试updateOption(Option)方法
	 * 测试数据: 不存在的OptionId
	 * 预期结果: 方法正常执行, 未影响数据表中的数据
	 */
	@Test
	public void testUpdateOptionNotExists() {
		Option option = optionMapper.getOption("websiteName");
		option.setOptionId(0);
		int numberOfRowsAffected = optionMapper.updateOption(option);
		Assertions.assertEquals(0, numberOfRowsAffected);
	}
	
	/**
	 * 待测试的OptionMapper对象.
	 */
	@Autowired
	private OptionMapper optionMapper;
}
