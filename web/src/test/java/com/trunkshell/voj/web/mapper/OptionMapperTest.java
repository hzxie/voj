package com.trunkshell.voj.web.mapper;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.transaction.TransactionConfiguration;
import org.springframework.transaction.annotation.Transactional;

import com.trunkshell.voj.web.model.Option;

/**
 * OptionMapper测试类.
 * 
 * @author Xie Haozhe
 */
@RunWith(SpringJUnit4ClassRunner.class)
@Transactional
@TransactionConfiguration(defaultRollback = true)
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
		Assert.assertEquals(5, options.size());
		
		Option firstOption = options.get(0);
		int optionId = firstOption.getOptionId();
		Assert.assertEquals(1, optionId);
		
		String optionName = firstOption.getOptionName();
		Assert.assertEquals("WebsiteName", optionName);
	}
	
	/**
	 * 测试用例: 测试getOption(String)方法
	 * 测试数据: 使用存在的选项名称
	 * 预期结果: 返回预期的Option对象
	 */
	@Test
	public void testGetOptionExists() {
		Option option = optionMapper.getOption("WebsiteName");
		Assert.assertNotNull(option);
		
		int optionId = option.getOptionId();
		Assert.assertEquals(1, optionId);
	}
	
	/**
	 * 测试用例: 测试getOption(String)方法
	 * 测试数据: 使用不存在的选项名称
	 * 预期结果: 返回空引用
	 */
	@Test
	public void testGetOptionNotExists() {
		Option option = optionMapper.getOption("NotExistOption");
		Assert.assertNull(option);
	}
	
	/**
	 * 测试用例: 测试updateOption(Option)方法
	 * 测试数据: 存在的OptionId
	 * 预期结果: 数据更新操作成功完成
	 */
	@Test
	public void testUpdateOptionExists() {
		Option option = optionMapper.getOption("WebsiteName");
		option.setOptionValue("New OJ Platform");
		optionMapper.updateOption(option);
		
		Option newOption = optionMapper.getOption("WebsiteName");
		String optionValue = newOption.getOptionValue();
		Assert.assertEquals("New OJ Platform", optionValue);
	}
	
	/**
	 * 测试用例: 测试updateOption(Option)方法
	 * 测试数据: 不存在的OptionId
	 * 预期结果: 方法正常执行, 未影响数据表中的数据
	 */
	@Test
	public void testUpdateOptionNotExists() {
		Option option = optionMapper.getOption("WebsiteName");
		option.setOptionId(0);
		optionMapper.updateOption(option);
	}
	
	/**
	 * 待测试的OptionMapper对象.
	 */
	@Autowired
	private OptionMapper optionMapper;
}
