package org.verwandlung.voj.judger.model;

import java.io.Serializable;

/**
 * 编程语言的Model.
 * 对应数据库中的voj_languages数据表.
 * 
 * @author Xie Haozhe
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
     * 编程语言的唯一英文简称.
     */
    private String languageSlug;
    
    /**
     * 编程语言的名称.
     */
    private String languageName;
    
    /**
     * 编程语言的编译命令.
     */
    private String compileCommand;
    
    /**
     * 编程语言的运行命令.
     */
    private String runCommand;
    
    /**
     * 唯一的序列化标识符
     */
    private static final long serialVersionUID = 9065824880175832696L;
}
