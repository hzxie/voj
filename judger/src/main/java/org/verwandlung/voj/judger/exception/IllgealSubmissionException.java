package org.verwandlung.voj.judger.exception;

/**
 * 无效的提交记录异常.
 * 当getSubmission(long)操作返回null时被抛出.
 * 
 * @author Haozhe Xie
 */
public class IllgealSubmissionException extends Exception {
    /**
     * IllgealSubmissionException的构造函数.
     * @param message - 错误消息
     */
    public IllgealSubmissionException(String message) {
        super(message);
    }

    /**
     * 唯一的序列化标识符.
     */
    private static final long serialVersionUID = -9019235951964656553L;
}
