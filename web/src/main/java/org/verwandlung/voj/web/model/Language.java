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
package org.verwandlung.voj.web.model;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * 编程语言的Model.
 * 对应数据库中的voj_languages数据表.
 * 
 * @author Haozhe Xie
 */
public class Language implements Serializable {
	/**
	 * 编程语言的默认构造函数.
	 */
	public Language() { }
	
	/**
	 * 编程语言的构造函数.
	 * @param languageSlug - 编程语言的英文缩写
	 * @param languageName - 编程语言的名称
	 * @param compileCommand - 编程语言的编译命令
	 * @param runCommand - 编程语言对应程序执行命令
	 */
	public Language(String languageSlug, String languageName, String compileCommand, String runCommand) {
		this.languageSlug = languageSlug;
		this.languageName = languageName;
		this.compileCommand = compileCommand;
		this.runCommand = runCommand;
	}
	
	/**
	 * 编程语言的构造函数.
	 * @param languageId - 编程语言的唯一标识符
	 * @param languageSlug - 编程语言的英文缩写
	 * @param languageName - 编程语言的名称
	 * @param compileCommand - 编程语言的编译命令
	 * @param runCommand - 编程语言对应程序执行命令
	 */
	public Language(int languageId, String languageSlug, String languageName, String compileCommand, String runCommand) {
		this(languageSlug, languageName, compileCommand, runCommand);
		this.languageId = languageId;
	}
	
	/**
	 * 获取编程语言的唯一标识符.
	 * @return 编程语言的唯一标识符
	 */
	public int getLanguageId() {
		return languageId;
	}
	
	/**
	 * 设置编程语言的唯一标识符.
	 * @param languageId - 编程语言的唯一标识符
	 */
	public void setLanguageId(int languageId) {
		this.languageId = languageId;
	}
	
	/**
	 * 获取编程语言的英文缩写
	 * @return 编程语言的英文缩写
	 */
	public String getLanguageSlug() {
		return languageSlug;
	}
	
	/**
	 * 设置编程语言的英文缩写.
	 * @param languageSlug - 编程语言的英文缩写
	 */
	public void setLanguageSlug(String languageSlug) {
		this.languageSlug = languageSlug;
	}
	
	/**
	 * 获取编程语言的名称.
	 * @return 编程语言的名称
	 */
	public String getLanguageName() {
		return languageName;
	}
	
	/**
	 * 设置编程语言的名称.
	 * @param languageName - 编程语言的名称
	 */
	public void setLanguageName(String languageName) {
		this.languageName = languageName;
	}
	
	/**
	 * 获取编程语言的编译命令.
	 * @return
	 */
	public String getCompileCommand() {
		return compileCommand;
	}

	/**
	 * 设置编程语言的编译命令.
	 * @param compileCommand
	 */
	public void setCompileCommand(String compileCommand) {
		this.compileCommand = compileCommand;
	}

	/**
	 * 获取编程语言的运行命令.
	 * @return 编程语言的运行命令
	 */
	public String getRunCommand() {
		return runCommand;
	}

	/**
	 * 设置编程语言的运行命令.
	 * @param runCommand - 编程语言的运行命令
	 */
	public void setRunCommand(String runCommand) {
		this.runCommand = runCommand;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return String.format("Language [ID=%d, Slug=%s, Name=%s, CompileCommand=%s, runCommand=%s]",
				new Object[] { languageId, languageSlug, languageName, compileCommand, runCommand });
	}
	
	/**
	 * 编程语言的唯一标识符. 
	 */
	private int languageId;
	
	/**
	 * 编程语言的别名.
	 */
	private String languageSlug;
	
	/**
	 * 编程语言的名称.
	 */
	private String languageName;
	
	/**
	 * 编程语言的编译命令.
	 */
	@JsonIgnore
	private String compileCommand;
	
	/**
	 * 编程语言的运行命令.
	 */
	@JsonIgnore
	private String runCommand;
	
	/**
	 * 唯一的序列化标识符
	 */
	private static final long serialVersionUID = 9065824880175832696L;
}
