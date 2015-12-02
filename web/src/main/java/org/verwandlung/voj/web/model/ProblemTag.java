package org.verwandlung.voj.web.model;

import java.io.Serializable;

/**
 * 试题标签的 Model.
 * 对应数据库中的voj_problem_tags数据表.
 * 
 * @author Xie Haozhe
 */
public class ProblemTag implements Serializable {
    /**
     * ProblemTag的默认构造函数.
     */
    public ProblemTag() { }
    
    /**
     * ProblemTag的构造函数.
     * @param problemTagSlug - 试题标签的唯一英文缩写
     * @param problemTagName - 试题标签的名称
     */
    public ProblemTag(String problemTagSlug, String problemTagName) {
        this.problemTagSlug = problemTagSlug;
        this.problemTagName = problemTagName;
    }
    
    /**
     * ProblemTag的构造函数.
     * @param problemTagId - 试题标签的唯一标识符
     * @param problemTagSlug - 试题标签的唯一英文缩写
     * @param problemTagName - 试题标签的名称
     */
    public ProblemTag(long problemTagId, String problemTagSlug, String problemTagName) { 
    	this(problemTagSlug, problemTagName);
        this.problemTagId = problemTagId;
    }
    
    /**
     * 获取试题标签的唯一标识符.
     * @return 试题标签的唯一标识符
     */
    public long getProblemTagId() {
        return problemTagId;
    }

    /**
     * 设置试题标签的唯一标识符.
     * @param problemTagId - 试题标签的唯一标识符
     */
    public void setProblemTagId(long problemTagId) {
        this.problemTagId = problemTagId;
    }
    
    /**
     * 获取试题标签的唯一英文缩写.
     * @return 试题标签的唯一英文缩写
     */
    public String getProblemTagSlug() {
        return problemTagSlug;
    }

    /**
     * 设置试题标签的唯一英文缩写.
     * @param problemTagSlug - 试题标签的唯一英文缩写
     */
    public void setProblemTagSlug(String problemTagSlug) {
        this.problemTagSlug = problemTagSlug;
    }

    /**
     * 获取试题标签的名称.
     * @return 试题标签的名称
     */
    public String getProblemTagName() {
        return problemTagName;
    }

    /**
     * 设置试题标签的名称.
     * @param problemTagName - 试题标签的名称
     */
    public void setProblemTagName(String problemTagName) {
        this.problemTagName = problemTagName;
    }
    
    /* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    public int hashCode() {
        return (int) problemTagId;
    }
    
    /* (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    public boolean equals(Object obj) {
        if ( obj instanceof ProblemTag ) {
            ProblemTag anotherTag = (ProblemTag)obj;
            return anotherTag.getProblemTagId() == problemTagId;
        }
        return false;
    }
    
    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    public String toString() {
        return String.format("ProblemTag [ID=%d, Slug=%s, Name=%s]", 
                new Object[] { problemTagId, problemTagSlug, problemTagName });
    }

    /**
     * 试题标签的唯一标识符.
     */
    private long problemTagId;
    
    /**
     * 试题标签的唯一英文缩写.
     */
    private String problemTagSlug;
    
    /**
     * 试题标签的名称.
     */
    private String problemTagName;
    
    /**
     * 唯一的序列化标识符.
     */
    private static final long serialVersionUID = 1636220984815816993L;
}
