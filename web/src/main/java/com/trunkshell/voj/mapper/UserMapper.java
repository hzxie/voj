package com.trunkshell.voj.mapper;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.trunkshell.voj.model.User;

public interface UserMapper {
	/**
	 * 通过用户唯一标识符获取用户对象.
	 * @param uid - 用户唯一标识符
	 * @return 预期的用户对象或空引用
	 */
	@Select("SELECT * FROM voj_users WHERE uid = #{uid}")
	public User getUserUsingUid(@Param("uid") long uid);
	
	/**
	 * 通过用户名获取用户对象.
	 * @param username - 用户名
	 * @return 预期的用户对象或空引用
	 */
	@Select("SELECT * FROM voj_users WHERE username = #{username}")
	public User getUserUsingUsername(@Param("username") String username);
}
