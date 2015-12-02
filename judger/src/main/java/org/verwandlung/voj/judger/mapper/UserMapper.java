package org.verwandlung.voj.judger.mapper;

import org.apache.ibatis.annotations.CacheNamespace;
import org.apache.ibatis.annotations.One;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.Select;

import org.verwandlung.voj.judger.model.Language;
import org.verwandlung.voj.judger.model.User;
import org.verwandlung.voj.judger.model.UserGroup;

/**
 * User Data Access Object.
 * 
 * @author Xie Haozhe
 */
@CacheNamespace(implementation = org.mybatis.caches.ehcache.EhcacheCache.class)
public interface UserMapper {
    /**
     * 通过用户名获取用户对象.
     * @param username - 用户名
     * @return 预期的用户对象或空引用
     */
    @Select("SELECT * FROM voj_users WHERE username = #{username}")
    @Options(useCache = false)
    @Results(value = {
        @Result(property = "userGroup", column = "user_group_id", javaType = UserGroup.class, one = @One(select="org.verwandlung.voj.judger.mapper.UserGroupMapper.getUserGroupUsingId")),
        @Result(property = "preferLanguage", column = "prefer_language_id", javaType = Language.class, one = @One(select="org.verwandlung.voj.judger.mapper.LanguageMapper.getLanguageUsingId"))
    })
    public User getUserUsingUsername(@Param("username") String username);
}
