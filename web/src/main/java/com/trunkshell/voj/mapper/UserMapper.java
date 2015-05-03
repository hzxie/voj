package com.trunkshell.voj.mapper;

import org.apache.ibatis.annotations.One;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.Select;

import com.trunkshell.voj.model.User;
import com.trunkshell.voj.model.UserGroup;

/**
 * User Data Access Object.
 * @author Xie Haozhe
 */
public interface UserMapper {
	/**
	 * 通过用户唯一标识符获取用户对象.
	 * @param uid - 用户唯一标识符
	 * @return 预期的用户对象或空引用
	 */
	@Select("SELECT * FROM voj_users WHERE uid = #{uid}")
	@Results(value = {
		@Result(property = "userGroup", column = "user_group_id", javaType=UserGroup.class, one = @One(select="com.trunkshell.voj.mapper.UserGroupMapper.getUserGroupUsingId"))
	})
	public User getUserUsingUid(@Param("uid") long uid);
	
	/**
	 * 通过用户名获取用户对象.
	 * @param username - 用户名
	 * @return 预期的用户对象或空引用
	 */
	@Select("SELECT * FROM voj_users WHERE username = #{username}")
	public User getUserUsingUsername(@Param("username") String username);
	
	/**
	 * 通过电子邮件地址获取用户对象.
	 * @param username - 用户名
	 * @return 预期的用户对象或空引用
	 */
	@Select("SELECT * FROM voj_users WHERE email = #{email}")
	public User getUserUsingEmail(@Param("email") String email);
	
	@Options(useGeneratedKeys = true, keyProperty = "uid")
	public void createUser(User user);
}
