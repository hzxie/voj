package com.trunkshell.voj.mapper;

import org.apache.ibatis.annotations.CacheNamespace;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.One;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import com.trunkshell.voj.model.Language;
import com.trunkshell.voj.model.User;
import com.trunkshell.voj.model.UserGroup;

/**
 * User Data Access Object.
 * @author Xie Haozhe
 */
/**
 * @author xiehaozhe
 *
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
	
	/**
	 * 创建新用户对象.
	 * @param user - 待创建的用户对象
	 */
	@Insert("INSERT INTO voj_users (username, password, email, user_group_id, prefer_language_id) VALUES (#{username}, #{password}, #{email}, #{userGroup.userGroupId}, #{preferLanguage.languageId})")
	@Options(useGeneratedKeys = true, keyProperty = "uid", keyColumn = "uid", flushCache = true)
	public void createUser(User user);
	
	/**
	 * 更新用户对象.
	 * @param user - 待更新信息的用户对象
	 */
	@Update("UPDATE voj_users SET username = #{username}, password = #{password}, email = #{email}, user_group_id = #{userGroup.userGroupId}, prefer_language_id = #{preferLanguage.languageId} WHERE uid = #{uid}")
	@Options(flushCache = true)
	public void updateUser(User user);
	
	/**
	 * 删除用户对象.
	 * @param uid - 待删除用户的用户唯一标识符
	 */
	@Delete("DELETE FROM voj_users WHERE uid = #{uid}")
	@Options(flushCache = true)
	public void deleteUser(long uid);
}
