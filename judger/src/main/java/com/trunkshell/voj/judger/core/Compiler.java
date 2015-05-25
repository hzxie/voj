package com.trunkshell.voj.judger.core;

import java.io.FileInputStream;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.trunkshell.voj.judger.model.Submission;

/**
 * 程序编译器, 用于编译用户提交的代码.
 * 
 * @author Xie Haozhe
 */
@Component
public class Compiler {
	/**
	 * 获取编译输出结果.
	 * @param submission - 提交记录对象
	 * @param workDirectory - 编译输出目录
	 * @param baseFileName - 编译输出文件名
	 * @return 包含编译输出结果的Map<String, Object>对象
	 */
	public Map<String, Object> getCompileResult(Submission submission, 
			String workDirectory, String baseFileName) {
		String commandLine = getCompileCommandLine(submission, workDirectory, baseFileName);
		String compileLogPath = getCompileLogPath(workDirectory, baseFileName);

		return getCompileResult(commandLine, compileLogPath);
	}
	
	/**
	 * 获取编译命令.
	 * @param submission - 提交记录对象
	 * @param workDirectory - 编译输出目录
	 * @param baseFileName - 编译输出文件名
	 * @return 编译命令
	 */
	private String getCompileCommandLine(Submission submission, 
			String workDirectory, String baseFileName) {
		String filePathWithoutExtension = String.format("%s/%s", 
											new Object[] {workDirectory, baseFileName});
		String compileCommand = submission.getLanguage()
											.getCompileCommand()
											.replaceAll("\\{filename\\}", filePathWithoutExtension);
		return compileCommand;
	}
	
	/**
	 * 获取编译日志输出的文件路径.
	 * @param workDirectory - 编译输出目录
	 * @param baseFileName - 编译输出文件名
	 * @return 编译日志输出的文件路径
	 */
	private String getCompileLogPath(String workDirectory, String baseFileName) {
		return String.format("%s/%s-compile.log", 
				new Object[] {workDirectory, baseFileName});
	}
	
	/**
	 * 获取编译输出结果.
	 * @param commandLine - 编译命令
	 * @param outputFilePath - 编译日志输出路径
	 * @return 包含编译输出结果的Map<String, Object>对象
	 */
	private Map<String, Object> getCompileResult(String commandLine, String compileLogPath) {
		String inputFilePath = null;
		int timeLimit = 5000;
		int memoryLimit = 0;
		
		logger.info("Start compiling with command: " + commandLine);
		Map<String, Object> runningResult = compilerRunner.getRuntimeResult(
				commandLine, inputFilePath, compileLogPath, timeLimit, memoryLimit);
		
		boolean isSuccessful = false;
		if ( runningResult != null ) {
			Object runtimeResultAttribute = runningResult.get("runtimeResult");
			if ( runtimeResultAttribute != null ) {
				String runtimeResultSlug = (String)runtimeResultAttribute;
				if ( runtimeResultSlug.equalsIgnoreCase("AC") ) {
					isSuccessful = true;
				}
			}
		}
		Map<String, Object> compileResult = new HashMap<String, Object>(3, 1);
		compileResult.put("isSuccessful", isSuccessful);
		compileResult.put("log", getCompileOutput(compileLogPath));
		return compileResult;
	}
	
	/**
	 * 获取编译日志内容.
	 * @param compileLogPath - 编译日志路径
	 * @return 编译日志内容
	 */
	private String getCompileOutput(String compileLogPath) {
		FileInputStream inputStream = null;
		String compileLog = null;
	    try {
	    	inputStream = new FileInputStream(compileLogPath);
	        compileLog = IOUtils.toString(inputStream);
	        inputStream.close();
	    } catch( Exception ex ) {
	    	// Do nothing
	    }
	    return compileLog;
	}
	
	/**
	 * 自动注入的Runner对象.
	 * 用于执行编译命令.
	 */
	@Autowired
	private Runner compilerRunner;
	
	/**
	 * 日志记录器.
	 */
	private static final Logger logger = LogManager.getLogger(Runner.class);
}
