package com.trunkshell.voj.judger.core;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.trunkshell.voj.judger.mapper.CheckpointMapper;
import com.trunkshell.voj.judger.mapper.LanguageMapper;
import com.trunkshell.voj.judger.model.Checkpoint;
import com.trunkshell.voj.judger.model.Language;
import com.trunkshell.voj.judger.model.Submission;

/**
 * 预处理器, 用于完成评测前准备工作.
 * 
 * @author Xie Haozhe
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
		if ( !workDirFile.exists() ) {
			if ( !workDirFile.mkdirs() ) {
				throw new Exception("Failed to create the work directory");
			}
		}
		
		Language language = submission.getLanguage();
		String code = replaceClassName(language, submission.getCode(), baseFileName);
		String codeFilePath = String.format("%s/%s.%s", 
				new Object[] {workDirectory, baseFileName, getCodeFileSuffix(language)});
		
		PrintWriter writer = new PrintWriter(codeFilePath, "UTF-8");
		writer.print(code);
		writer.close();
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
	 * 从数据库抓取评测数据.
	 * @param problemId - 试题的唯一标识符
	 * @throws Exception 
	 */
	public void fetchTestPoints(long problemId) throws Exception {
		String checkpointsFilePath = String.format("%s/%s", 
				new Object[] {TEST_POINTS_DIRECTORY, problemId});
		File checkpointsDirFile = new File(checkpointsFilePath);
		if ( !checkpointsDirFile.exists() ) {
			if ( !checkpointsDirFile.mkdirs() ) {
				throw new Exception("Failed to create the checkpoints directory");
			}
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
			}
			{ // Standard Output File
				String filePath = String.format("%s/output#%s.txt", 
						new Object[] { checkpointsFilePath, checkpointId });
				FileOutputStream outputStream = new FileOutputStream(new File(filePath));
				String output = checkpoint.getOutput();
				IOUtils.write(output, outputStream);
			}
		}
	}
	
	/**
	 * 自动注入的LanguageMapper对象.
	 * 用于获取代码文件的后缀名.
	 */
	@Autowired
	private LanguageMapper languageMapper;
	
	/**
	 * 自动注入的CheckpointMapper对象.
	 * 用于获取试题的测试点.
	 */
	@Autowired
	private CheckpointMapper checkpointMapper;
	
	/**
	 * 测试数据目录.
	 */
	private static final String TEST_POINTS_DIRECTORY = "cache/";
}
