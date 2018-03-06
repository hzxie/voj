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
package org.verwandlung.voj.web.util;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import org.verwandlung.voj.web.util.DigestUtils;

/**
 * DigestUtils的测试类.
 * @author Haozhe Xie
 */
public class DigestUtilsTest {
	/**
	 * 测试用例: 测试md5Hex方法
	 * 测试数据: 有效的密码
	 * 测试用例: 返回MD5加密后的密码
	 */
	@Test
	public void testMd5HexCase1() {
		String password = "zjhzxhz";
		String runtimeValue = DigestUtils.md5Hex(password);
		String expectedValue = "785ee107c11dfe36de668b1ae7baacbb";
		
		Assertions.assertEquals(expectedValue, runtimeValue);
	}
	
	/**
	 * 测试用例: 测试md5Hex方法
	 * 测试数据: 有效的密码
	 * 测试用例: 返回MD5加密后的密码
	 */
	@Test
	public void testMd5HexCase2() {
		String password = "meigaofei0926";
		String runtimeValue = DigestUtils.md5Hex(password);
		String expectedValue = "0adb82835139278edb78d1e8c4548600";
		
		Assertions.assertEquals(expectedValue, runtimeValue);
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
		
		Assertions.assertEquals(expectedValue, runtimeValue);
	}
}
