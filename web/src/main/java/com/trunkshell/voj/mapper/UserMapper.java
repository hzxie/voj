package com.trunkshell.voj.mapper;

import org.apache.ibatis.annotations.CacheNamespace;
import org.apache.ibatis.annotations.One;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.Select;

import com.trunkshell.voj.model.Language;
import com.trunkshell.voj.model.User;
import com.trunkshell.voj.model.UserGroup;

/**
 * User Data Access Object.
 * @author Xie Haozhe
 */
@CacheNamespace(implementation = org.mybatis.caches.ehcache.EhcacheCache.class)
public interface UserMapper {
	/**
	 * 通过用户唯一标识符获取用户对象.
	 * @param uid - 用户唯一标识符
	 * @return 预期的用户对象或空引用
	 */
	@Select("SELECT * FROM voj_users WHERE uid = #{uid}")
	@Results(value = {
		@Result(property = "userGroup", column = "user_group_id", javaType=UserGroup.class, one = @One(select="com.trunkshell.voj.mapper.UserGroupMapper.getUserGroupUsingId")),
		@Result(property = "preferLanguage", column = "prefer_language_id", javaType=Language.class, one = @One(select="com.trunkshell.voj.mapper.LanguageMapper.getLanguageUsingId"))
	})
	public User getUserUsingUid(@Param("uid") long uid);
	
	/**
	 * 通过用户名获取用户对象.
	 * @param username - 用户名
	 * @return 预期的用户对象或空引用
	 */
	@Select("SELECT * FROM voj_users WHERE username = #{username}")
	@Results(value = {
		@Result(property = "userGroup", column = "user_group_id", javaType=UserGroup.class, one = @One(select="com.trunkshell.voj.mapper.UserGroupMapper.getUserGroupUsingId")),
		@Result(property = "preferLanguage", column = "prefer_language_id", javaType=Language.class, one = @One(select="com.trunkshell.voj.mapper.LanguageMapper.getLanguageUsingId"))
	})
	public User getUserUsingUsername(@Param("username") String username);
	
	/**
	 * 通过电子邮件地址获取用户对象.
	 * @param username - 用户名
	 * @return 预期的用户对象或空引用
	 */
	@Select("SELECT * FROM voj_users WHERE email = #{email}")
	@Results(value = {
		@Result(property = "userGroup", column = "user_group_id", javaType=UserGroup.class, one = @One(select="com.trunkshell.voj.mapper.UserGroupMapper.getUserGroupUsingId")),
		@Result(property = "preferLanguage", column = "prefer_language_id", javaType=Language.class, one = @One(select="com.trunkshell.voj.mapper.LanguageMapper.getLanguageUsingId"))
	})
	public User getUserUsingEmail(@Param("email") String email);
	
	@Options(useGeneratedKeys = true, keyProperty = "uid")
	public void createUser(User user);
}
