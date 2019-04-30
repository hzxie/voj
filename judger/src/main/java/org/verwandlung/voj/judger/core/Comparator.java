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
import org.apache.commons.io.LineIterator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;

/**
 * 用于比对用户输出结果和标准结果.
 * 
 * @author Haozhe Xie
 */
@Component
public class Comparator {
	/**
	 * 获取用户输出和标准输出的比对结果.
	 * @param standardOutputFilePath - 标准输出文件路径
	 * @param outputFilePath - 用户输出文件路径
	 * @return 用户输出和标准输出是否相同
	 */
	public boolean isOutputTheSame(String standardOutputFilePath,
			String outputFilePath) throws IOException {
		File stdFile = new File(standardOutputFilePath);
		File file = new File(outputFilePath);

		LineIterator stdFileItr = FileUtils.lineIterator(stdFile, "UTF-8");
		LineIterator fileItr = FileUtils.lineIterator(file, "UTF-8");
		boolean isFileOutputTheSame = isFileOutputTheSame(stdFileItr, fileItr);
		
		LineIterator.closeQuietly(stdFileItr);
		LineIterator.closeQuietly(fileItr);
		return isFileOutputTheSame;
	}
	
	/**
	 * 比对标准输出和用户输出是否相同.
	 * @param stdFileItr - 标准输出文件的迭代器
	 * @param fileItr - 用户输出文件的迭代器
	 * @return 标准输出和用户输出是否相同
	 */
	private boolean isFileOutputTheSame(LineIterator stdFileItr, LineIterator fileItr) {
		try {
			while ( stdFileItr.hasNext() && fileItr.hasNext() ) {
				String stdLine = stdFileItr.nextLine();
				String line = fileItr.nextLine();
				
				if ( !isLineOutputTheSame(stdLine, line) ) {
					return false;
				}
			}
			while ( stdFileItr.hasNext() ) {
				String line = stdFileItr.nextLine();
				if ( !isLineEmpty(line, 0) ) {
					return false;
				}
			}
			while ( fileItr.hasNext() ) {
				String line = fileItr.nextLine();
				if ( !isLineEmpty(line, 0) ) {
					return false;
				}
			}
		} catch ( OutOfMemoryError ex ) {
			LOGGER.catching(ex);
			return false;
		}
		return true;
	}
	
	/**
	 * 比对某行的标准输出和用户输出(忽略行尾空格).
	 * @param stdLine - 标准输出中的某一行
	 * @param line - 用户输出中的某一行
	 * @return 某行的标准输出和用户输出是否相同
	 */
	private boolean isLineOutputTheSame(String stdLine, String line) {
		int i = 0, j = 0;
		for ( ; i < stdLine.length() && j < line.length(); ++ i, ++ j ) {
			if (  stdLine.charAt(i) != line.charAt(j) ) {
				if ( stdLine.charAt(i) == '\n' ) {
					if ( !isLineEmpty(line, j) ) {
						return false;
					}
					return true;
				} else if ( line.charAt(j) == '\n' ) {
					if ( !isLineEmpty(stdLine, i) ) {
						return false;
					}
					return true;
				}
				return false;
			}
		}
		while ( i < stdLine.length() ) {
			if ( !isLineEmpty(stdLine, i) ) {
				return false;
			}
			++ i;
		}
		while ( j < line.length() ) {
			if ( !isLineEmpty(line, j) ) {
				return false;
			}
			++ j;
		}
		return true;
	}
	
	/**
	 * 忽略文件结尾的空行与空格.
	 * @param line - 某行文件内容
	 * @param startIndex - 开始检查位置的索引
	 * @return 该行内容中是否只包含空格和换行符
	 */
	private boolean isLineEmpty(String line, int startIndex) {
		for ( int i = startIndex; i < line.length(); ++ i ) {
			if ( !(line.charAt(i) == ' ' || line.charAt(i) == '\n') ) {
				return false;
			}
		}
		return true;
	}
	
	/**
	 * 日志记录器.
	 */
	private static final Logger LOGGER = LogManager.getLogger(Comparator.class);
}
