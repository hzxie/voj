package com.happystudio.voj.util;

import org.junit.Assert;
import org.junit.Test;

/**
 * DigestUtils的测试类.
 * @author Xie Haozhe
 */
public class DigestUtilsTest {
	/**
	 * 测试用例: 测试md5Hex方法
	 * 测试数据: 有效的密码
	 * 测试用例: 返回MD5加密后的密码
	 */
	@Test
	public void testMd5HexNormal() {
		String password = "zjhzxhz";
		String runtimeValue = DigestUtils.md5Hex(password);
		String expectedValue = "785ee107c11dfe36de668b1ae7baacbb";
		
		Assert.assertEquals(expectedValue, runtimeValue);
	}
	
	/**
	 * 测试用例: 测试md5Hex方法
	 * 测试数据: 空密码
	 * 测试用例: 返回空字符串
	 */
	@Test
	public void testMd5HexEmpty() {
		String password = "";
		String runtimeValue = DigestUtils.md5Hex(password);
		String expectedValue = "";
		
		Assert.assertEquals(expectedValue, runtimeValue);
	}
}
