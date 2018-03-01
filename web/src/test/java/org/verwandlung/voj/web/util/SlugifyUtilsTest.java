package org.verwandlung.voj.web.util;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * SlugifyUtils测试类.
 * 
 * @author Haozhe Xie
 */
public class SlugifyUtilsTest {
	/**
	 * 测试用例: 测试getSlug(String)方法
	 * 测试数据: Latin字符组成的字符串
	 * 预期结果: 返回将Latin字符转为小写并除去其中的空格的字符串
	 */
	@Test
	public void testGetSlugWithAsciiChars() {
		Assertions.assertEquals("a-b--c", SlugifyUtils.getSlug("a b -C"));
	}
	
	/**
	 * 测试用例: 测试getSlug(String)方法
	 * 测试数据: Latin字符和非Latin字符组成的字符串
	 * 预期结果: 返回将非Latin字符转义后并除去其中的空格的字符串
	 */
	@Test
	public void testGetSlugWithChineseCharacters() {
		Assertions.assertEquals("a%e4%b8%ad%e6%96%87-b", SlugifyUtils.getSlug("a中文 b"));
	}
}
