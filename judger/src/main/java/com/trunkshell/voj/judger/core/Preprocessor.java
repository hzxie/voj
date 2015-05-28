package com.trunkshell.voj.judger.core;

import org.springframework.stereotype.Component;

/**
 * 预处理器, 用于完成评测前准备工作.
 * 
 * @author Xie Haozhe
 */
@Component
public class Preprocessor {
	/**
	 * 创建测试代码至本地磁盘.
	 */
	public void createTestCode() {
		
	}
	
	/**
	 * 从数据库抓取评测数据.
	 * @param problemId - 试题的唯一标识符
	 */
	public void fetchTestPoints(long problemId) {
		// Check if Exists
		// Dump Data
	}
	
	/**
	 * 测试数据目录.
	 */
	private static final String TEST_POINTS_DIRECTORY = "cache/";
}
