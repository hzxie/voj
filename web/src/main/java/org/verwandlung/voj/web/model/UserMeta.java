package org.verwandlung.voj.web.model;

import java.io.Serializable;

/**
 * 用户元信息.
 * 
 * @author Xie Haozhe
 */
public class UserMeta implements Serializable {
    /**
     * UserMeta类的默认构造函数.
     */
    public UserMeta() { }
    
    /**
     * UserMeta类的构造函数.
     * @param uid - 用户的唯一标识符
     * @param metaKey - 元信息的键
     * @param metaValue - 元信息的值
     */
    public UserMeta(User user, String metaKey, String metaValue) {
        this.user = user;
        this.metaKey = metaKey;
        this.metaValue = metaValue;
    }
    
    /**
     * UserMeta类的构造函数.
     * @param metaId - 用户元信息的唯一标识符
     * @param uid - 用户的唯一标识符
     * @param metaKey - 元信息的键
     * @param metaValue - 元信息的值
     */
    public UserMeta(long metaId, User user, String metaKey, String metaValue) {
        this(user, metaKey, metaValue);
        this.metaId = metaId;
    }
    
    /**
     * 获取用户元信息的唯一标识符.
     * @return 用户元信息的唯一标识符
     */
    public long getMetaId() {
        return metaId;
    }

    /**
     * 设置用户元信息的唯一标识符.
     * @param metaId - 用户元信息的唯一标识符
     */
    public void setMetaId(long metaId) {
        this.metaId = metaId;
    }
    
    /**
     * 获取元信息对应的用户对象.
     * @return 元信息对应的用户对象
     */
    public User getUser() {
        return user;
    }

    /**
     * 设置元信息对应的用户对象.
     * @param user - 元信息对应的用户对象
     */
    public void setUser(User user) {
        this.user = user;
    }

    /**
     * 获取元信息的键.
     * @return 元信息的键
     */
    public String getMetaKey() {
        return metaKey;
    }

    /**
     * 设置元信息的键.
     * @param metaKey - 元信息的键
     */
    public void setMetaKey(String metaKey) {
        this.metaKey = metaKey;
    }

    /**
     * 获取元信息的值.
     * @return 元信息的值
     */
    public String getMetaValue() {
        return metaValue;
    }

    /**
     * 设置元信息的值.
     * @param metaValue - 元信息的值
     */
    public void setMetaValue(String metaValue) {
        this.metaValue = metaValue;
    }
    
    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    public String toString() {
        return String.format("UserMeta: [ID=%s, User={%s}, Key=%s, Value=%s]", 
                new Object[] { metaId, user, metaKey, metaValue});
    }

    /**
     * 用户元信息的唯一标识符. 
     */
    private long metaId;
    
    /**
     * 元信息对应的用户对象.
     */
    private User user;
    
    /**
     * 元信息的键.
     */
    private String metaKey;
    
    /**
     * 元信息的值.
     */
    private String metaValue;
    
    /**
     * 唯一的序列化标识符.
     */
    private static final long serialVersionUID = 1362748371508853051L;
}
