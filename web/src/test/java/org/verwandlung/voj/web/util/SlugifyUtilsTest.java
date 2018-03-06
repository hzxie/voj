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
