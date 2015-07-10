package com.trunkshell.voj.web.mapper;

import org.apache.ibatis.annotations.CacheNamespace;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.Select;

import com.trunkshell.voj.web.model.EmailValidation;

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
    @Select("SELECT * FROM voj_email_validation WHERE email = #{email}")
    @Options(useCache = true)
    @Results(value = {
        @Result(property = "expireTime", column = "expire_time"),
    })
    public EmailValidation getEmailValidation(@Param("email") String email);
    
    /**
     * 创建新的电子邮件验证凭据.
     * @param emailValidation - 电子邮件验证凭据
     */
    @Insert("Insert INTO voj_email_validation (email, token, expire_time) VALUES (#{email}, #{token}, #{expireTime})")
    @Options(flushCache = true)
    public void createEmailValidation(EmailValidation emailValidation);
    
    /**
     * 删除电子邮件验证凭据.
     * @param email - 电子邮件地址
     */
    @Delete("DELETE FROM voj_email_validation WHERE email = #{email}")
    @Options(flushCache = true)
    public void deleteEmailValidation(String email);
}
