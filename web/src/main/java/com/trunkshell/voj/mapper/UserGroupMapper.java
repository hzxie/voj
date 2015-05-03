package com.trunkshell.voj.mapper;

import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.Select;

import com.trunkshell.voj.model.UserGroup;

/**
 * UserGroup Data Access Object.
 * @author Xie Haozhe
 */
public interface UserGroupMapper {
	/**
	 * 通过用户组的唯一标识符获取用户组对象.
	 * @param userGroupId - 用户组的唯一标识符
	 * @return 预期的用户组对象或空引用
	 */
	@Select("SELECT * FROM voj_user_groups WHERE user_group_id = #{userGroupId}")
	@Results({
		 @Result(property = "userGroupId", column = "user_group_id"),
		 @Result(property = "userGroupSlug", column = "user_group_slug"),
		 @Result(property = "userGroupName", column = "user_group_name")
	})
	public UserGroup getUserGroupUsingId(int userGroupId);
	
	/**
	 * 通过用户组的唯一英文缩写获取用户组对象.
	 * @param userGroupSlug - 用户组的唯一英文缩写
	 * @return 预期的用户组对象或空引用
	 */
	@Select("SELECT * FROM voj_user_groups WHERE user_group_slug = #{userGroupSlug}")
	@Results({
		 @Result(property = "userGroupId", column = "user_group_id"),
		 @Result(property = "userGroupSlug", column = "user_group_slug"),
		 @Result(property = "userGroupName", column = "user_group_name")
	})
	public UserGroup getUserGroupUsingSlug(String userGroupSlug);
}
