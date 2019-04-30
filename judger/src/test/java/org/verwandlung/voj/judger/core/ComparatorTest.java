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
package org.verwandlung.voj.judger.core;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * 输出比对器的测试类.
 *
 * @author Haozhe Xie
 */
@ExtendWith(SpringExtension.class)
@Transactional
@ContextConfiguration({ "classpath:test-spring-context.xml" })
public class ComparatorTest {
	/**
	 * 构建测试用例.
	 */
	@BeforeAll
	public static void setUp() throws IOException {
		FileOutputStream stdOutputStream = null;
		FileOutputStream outputStream = null;
		// Make Directory for TestCases
		File checkpointsDirFile = new File("/tmp/voj-matcher-tests");
		if (!checkpointsDirFile.exists()) {
			checkpointsDirFile.mkdirs();
		}
		// TestCase: MatchExactly
		stdOutputStream = new FileOutputStream(new File(
				"/tmp/voj-matcher-tests/match-exactly-std.txt"));
		outputStream = new FileOutputStream(new File(
				"/tmp/voj-matcher-tests/match-exactly.txt"));
		String matchExactlyString = "32768";
		IOUtils.write(matchExactlyString, stdOutputStream);
		IOUtils.write(matchExactlyString, outputStream);
		// TestCase: MatchWithSpaces
		stdOutputStream = new FileOutputStream(new File(
				"/tmp/voj-matcher-tests/match-with-spaces-std.txt"));
		outputStream = new FileOutputStream(new File(
				"/tmp/voj-matcher-tests/match-with-spaces.txt"));
		String matchWithSpacesString = "Output Test with Spaces";
		IOUtils.write(matchWithSpacesString, stdOutputStream);
		IOUtils.write(matchWithSpacesString + " \n  \n", outputStream);
		// TestCase: Mismatch
		stdOutputStream = new FileOutputStream(new File(
				"/tmp/voj-matcher-tests/mismatch-std.txt"));
		outputStream = new FileOutputStream(new File(
				"/tmp/voj-matcher-tests/mismatch.txt"));
		String mismatchString1 = "45652  \n\n";
		String mismatchString2 = "24334";
		IOUtils.write(mismatchString1, stdOutputStream);
		IOUtils.write(mismatchString2, outputStream);
		// TestCase: MismatchEmptyString
		outputStream = new FileOutputStream(new File(
				"/tmp/voj-matcher-tests/mismatch-empty.txt"));
		String mismatchString3 = "\n";
		IOUtils.write(mismatchString3, outputStream);
	}

	/**
	 * 清除测试用例.
	 */
	@AfterAll
	public static void tearDown() {
		File checkpointsDirFile = new File("/tmp/voj-matcher-tests");
		if (checkpointsDirFile.exists()) {
			checkpointsDirFile.delete();
		}
	}

	/**
	 * 测试用例: 测试isOutputTheSame(String, String)方法 
	 * 测试数据: 输入和输出完全相同 
	 * 测试结果: 返回true, 表示输出结果正确
	 */
	@Test
	public void testMatchExactly() throws IOException {
		String standardOutputFilePath = "/tmp/voj-matcher-tests/match-exactly-std.txt";
		String outputFilePath = "/tmp/voj-matcher-tests/match-exactly.txt";
		Assertions.assertTrue(comparator.isOutputTheSame(standardOutputFilePath, outputFilePath));
	}

	/**
	 * 测试用例: 测试isOutputTheSame(String, String)方法 
	 * 测试数据: 输入和输出基本相同, 在行尾存在多余的空白字符
	 * 测试结果: 返回true, 表示输出结果正确
	 */
	@Test
	public void testMatchWithSpaces() throws IOException {
		String standardOutputFilePath = "/tmp/voj-matcher-tests/match-with-spaces-std.txt";
		String outputFilePath = "/tmp/voj-matcher-tests/match-with-spaces.txt";
		Assertions.assertTrue(comparator.isOutputTheSame(standardOutputFilePath,
				outputFilePath));
		Assertions.assertTrue(comparator.isOutputTheSame(outputFilePath, standardOutputFilePath));
	}

	/**
	 * 测试用例: 测试isOutputTheSame(String, String)方法 
	 * 测试数据: 输入和输出不完全相同 
	 * 测试结果: 返回false, 表示输出结果不正确
	 */
	@Test
	public void testMismatch() throws IOException {
		String standardOutputFilePath = "/tmp/voj-matcher-tests/mismatch-std.txt";
		String outputFilePath = "/tmp/voj-matcher-tests/mismatch.txt";
		Assertions.assertFalse(comparator.isOutputTheSame(standardOutputFilePath, outputFilePath));
	}

	/**
	 * 测试用例: 测试isOutputTheSame(String, String)方法 
	 * 测试数据: 输入和输出不完全相同 
	 * 测试结果: 返回false, 表示输出结果不正确
	 */
	@Test
	public void testMismatchEmptyString() throws IOException {
		String standardOutputFilePath = "/tmp/voj-matcher-tests/mismatch-std.txt";
		String outputFilePath = "/tmp/voj-matcher-tests/mismatch-empty.txt";
		Assertions.assertFalse(comparator.isOutputTheSame(standardOutputFilePath, outputFilePath));
	}

	/**
	 * 待测试的Matcher对象.
	 */
	@Autowired
	private Comparator comparator;
}
