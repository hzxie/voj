package org.verwandlung.voj.web.util;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

/**
 * SensitiveWordFilter的测试类
 * @author YiHao Zhou
 */
@ExtendWith(SpringExtension.class)
@Transactional
@ContextConfiguration({"classpath:test-spring-context.xml"})
public class OffensiveWordFilterTest {
	/**
	 * 测试用例：测试filter()方法
	 * 测试数据: 包含敏感词的数据(法轮大法)
	 * 预期结果: 过滤敏感词后的数据
	 */
	@Test
	public void testFilterUsingOffensiveWord() {
		Assertions.assertNotNull(filter);
		Assertions.assertEquals("**大法好 你好", filter.filter("法轮大法好 你好"));
	}
	
	/**
	 * 自动注入的SensitiveWordFilter对象.
	 */
	@Autowired
	private OffensiveWordFilter filter;
}
