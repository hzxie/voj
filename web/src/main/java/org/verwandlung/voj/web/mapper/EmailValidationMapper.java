package org.verwandlung.voj.web.mapper;

import org.apache.ibatis.annotations.CacheNamespace;
import org.apache.ibatis.annotations.Param;

import org.verwandlung.voj.web.model.EmailValidation;

/**
 * EmailValidation Data Access Object.
 * 
 * @author Xie Haozhe
 */
@CacheNamespace(implementation = org.mybatis.caches.ehcache.EhcacheCache.class)
public interface EmailValidationMapper {
    /**
     * 获取某个电子邮件地址对应的EmailValidation对象.
     * @param email - 电子邮件地址
     * @return 对应的EmailValidation对象
     */
    public EmailValidation getEmailValidation(@Param("email") String email);
    
    /**
     * 创建新的电子邮件验证凭据.
     * @param emailValidation - 电子邮件验证凭据
     */
    public void createEmailValidation(EmailValidation emailValidation);
    
    /**
     * 删除电子邮件验证凭据.
     * @param email - 电子邮件地址
     */
    public void deleteEmailValidation(@Param("email") String email);
}
