package com.trunkshell.voj.web.model;

import java.io.Serializable;

/**
 * 系统设置选项类.
 * 
 * @author Xie Haozhe
 */
public class Option implements Serializable {
    /**
     * Option类的默认构造函数.
     */
    public Option() { }
    
    /**
     * Option类的构造函数.
     * @param optionName - 选项的名称
     * @param optionValue - 选项的值
     * @param isAutoload - 选项是否自动加载
     */
    public Option(String optionName, String optionValue, boolean isAutoload) { 
        this.optionName = optionName;
        this.optionValue = optionValue;
        this.isAutoload = isAutoload;
    }
    
    /**
     * Option类的构造函数.
     * @param optionId - 选项的唯一标识符
     * @param optionName - 选项的名称
     * @param optionValue - 选项的值
     * @param isAutoload - 选项是否自动加载
     */
    public Option(int optionId, String optionName, String optionValue, boolean isAutoload) { 
        this(optionName, optionValue, isAutoload);
        this.optionId = optionId;
    }

    /**
     * 获取选项的唯一标识符.
     * @return 选项的唯一标识符
     */
    public int getOptionId() {
        return optionId;
    }

    /**
     * 设置选项的唯一标识符.
     * @param optionId - 选项的唯一标识符
     */
    public void setOptionId(int optionId) {
        this.optionId = optionId;
    }
    
    /**
     * 获取选项的名称.
     * @return 选项的名称
     */
    public String getOptionName() {
        return optionName;
    }

    /**
     * 设置选项的名称.
     * @param optionName - 选项的名称
     */
    public void setOptionName(String optionName) {
        this.optionName = optionName;
    }

    /**
     * 获取选项的值.
     * @return 选项的值
     */
    public String getOptionValue() {
        return optionValue;
    }

    /**
     * 设置选项的值.
     * @param optionValue - 选项的值
     */
    public void setOptionValue(String optionValue) {
        this.optionValue = optionValue;
    }

    /**
     * 获取选项是否自动加载.
     * @return 选项是否自动加载
     */
    public boolean isAutoload() {
        return isAutoload;
    }

    /**
     * 设置选项是否自动加载.
     * @param isAutoload - 选项是否自动加载
     */
    public void setAutoload(boolean isAutoload) {
        this.isAutoload = isAutoload;
    }
    
    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    public String toString() {
        return String.format("Option [ID=%d, Name=%s, Value=%s, isAutoLoad=%s]", 
                new Object[] {optionId, optionName, optionValue, isAutoload});
    }

    /**
     * 选项的唯一标识符.
     */
    private int optionId;
    
    /**
     * 选项的名称.
     */
    private String optionName;
    
    /**
     * 选项的值.
     */
    private String optionValue;
    
    /**
     * 是否自动加载该选项.
     */
    private boolean isAutoload;
    
    /**
     * 唯一的序列化标识符.
     */
    private static final long serialVersionUID = -1533277464247886659L;
}
