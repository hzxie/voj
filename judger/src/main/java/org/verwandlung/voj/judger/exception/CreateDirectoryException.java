package org.verwandlung.voj.judger.exception;

/**
 * 创建文件夹失败的IO异常.
 * 当java.io.File.File.mkdirs()返回false时被抛出.
 * 
 * @author Haozhe Xie
 */
public class CreateDirectoryException extends Exception {
	/**
	 * IOException的构造函数.
	 * @param message - 错误消息
	 */
	public CreateDirectoryException(String message) {
		super(message);
	}
	
	/**
	 * 唯一的序列化标识符.
	 */
	private static final long serialVersionUID = 7430055519184434330L;
}
