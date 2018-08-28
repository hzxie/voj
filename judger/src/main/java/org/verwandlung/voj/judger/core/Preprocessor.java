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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.attribute.PosixFilePermission;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import org.verwandlung.voj.judger.exception.CreateDirectoryException;
import org.verwandlung.voj.judger.mapper.CheckpointMapper;
import org.verwandlung.voj.judger.model.Checkpoint;
import org.verwandlung.voj.judger.model.Language;
import org.verwandlung.voj.judger.model.Submission;

/**
 * 预处理器, 用于完成评测前准备工作.
 * 
 * @author Haozhe Xie
 */
@Component
public class Preprocessor {
	/**
	 * 创建测试代码至本地磁盘.
	 * 
	 * @param submission - 评测记录对象
	 * @param workDirectory - 用于产生编译输出的目录
	 * @param baseFileName - 随机文件名(不包含后缀)
	 * @throws Exception 
	 */
	public void createTestCode(Submission submission, 
			String workDirectory, String baseFileName) throws Exception {
		File workDirFile = new File(workDirectory);
		if ( !workDirFile.exists() && !workDirFile.mkdirs() ) {
			throw new CreateDirectoryException("Failed to create directory: " + workDirectory);
		}
		setWorkDirectoryPermission(workDirFile);
		
		Language language = submission.getLanguage();
		String code = replaceClassName(language, submission.getCode(), baseFileName);
		String codeFilePath = String.format("%s/%s.%s", 
				new Object[] {workDirectory, baseFileName, getCodeFileSuffix(language)});
		
		FileOutputStream outputStream = new FileOutputStream(new File(codeFilePath));
		IOUtils.write(code, outputStream);
		IOUtils.closeQuietly(outputStream);
	}
	
	/**
	 * 获取代码文件的后缀名.
	 * @param language - 编程语言对象
	 * @return 代码文件的后缀名
	 */
	private String getCodeFileSuffix(Language language) {
		String compileCommand = language.getCompileCommand();
		
		Pattern pattern = Pattern.compile("\\{filename\\}\\.((?!exe| ).)+");
		Matcher matcher = pattern.matcher(compileCommand);
		
		if ( matcher.find() ) {
			String sourceFileName = matcher.group();
			return sourceFileName.replaceAll("\\{filename\\}\\.", "");
		}
		return "";
	}
	
	/**
	 * 替换部分语言中的类名(如Java), 以保证正常通过编译.
	 * @param language - 编程语言对象
	 * @param code - 待替换的代码
	 * @param newClassName - 新的类名
	 */
	private String replaceClassName(Language language, String code, String newClassName) {
		if ( !language.getLanguageName().equalsIgnoreCase("Java") ) {
			return code;
		}
		return code.replaceAll("class[ \n]+Main", "class " + newClassName);
	}

	/**
	 * 设置代码文件所在目录的读写权限.
	 * 在Linux下, 代码以UID=1536的用户运行, 因此需要为Others用户组分配写权限.
	 * @param workDirectory 用于产生编译输出的目录
	 */
	private void setWorkDirectoryPermission(File workDirectory) throws IOException {
		if ( !System.getProperty("os.name").contains("Windows") ) {
			Set<PosixFilePermission> permissions = new HashSet<>();

			permissions.add(PosixFilePermission.OWNER_READ);
			permissions.add(PosixFilePermission.OWNER_WRITE);
			permissions.add(PosixFilePermission.OWNER_EXECUTE);

			permissions.add(PosixFilePermission.GROUP_READ);
			permissions.add(PosixFilePermission.GROUP_WRITE);
			permissions.add(PosixFilePermission.GROUP_EXECUTE);

			permissions.add(PosixFilePermission.OTHERS_READ);
			permissions.add(PosixFilePermission.OTHERS_WRITE);
			permissions.add(PosixFilePermission.OTHERS_EXECUTE);
			Files.setPosixFilePermissions(workDirectory.toPath(), permissions);
		}
	}
	
	/**
	 * 从数据库抓取评测数据.
	 * @param problemId - 试题的唯一标识符
	 * @throws Exception 
	 */
	public void fetchTestPoints(long problemId) throws Exception {
		String checkpointsFilePath = String.format("%s/%s", 
				new Object[] {checkpointDirectory, problemId});
		File checkpointsDirFile = new File(checkpointsFilePath);
		if ( !checkpointsDirFile.exists() && !checkpointsDirFile.mkdirs() ) {
			throw new CreateDirectoryException("Failed to create the checkpoints directory: " + checkpointsFilePath);
		}
		
		List<Checkpoint> checkpoints = 
				checkpointMapper.getCheckpointsUsingProblemId(problemId);
		for ( Checkpoint checkpoint : checkpoints ) {
			long checkpointId = checkpoint.getCheckpointId();
			{ // Standard Input File
				String filePath = String.format("%s/input#%s.txt", 
						new Object[] { checkpointsFilePath, checkpointId });
				FileOutputStream outputStream = new FileOutputStream(new File(filePath));
				String input = checkpoint.getInput();
				IOUtils.write(input, outputStream);
				IOUtils.closeQuietly(outputStream);
			}
			{ // Standard Output File
				String filePath = String.format("%s/output#%s.txt", 
						new Object[] { checkpointsFilePath, checkpointId });
				FileOutputStream outputStream = new FileOutputStream(new File(filePath));
				String output = checkpoint.getOutput();
				IOUtils.write(output, outputStream);
				IOUtils.closeQuietly(outputStream);
			}
		}
	}
	
	/**
	 * 自动注入的CheckpointMapper对象.
	 * 用于获取试题的测试点.
	 */
	@Autowired
	private CheckpointMapper checkpointMapper;
	
	/**
	 * 测试点的存储目录.
	 * 用于存储测试点的输入输出数据.
	 */
	@Value("${judger.checkpointDir}")
	private String checkpointDirectory;
}
