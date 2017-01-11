package org.verwandlung.voj.web.mapper;

import java.util.List;

import org.apache.ibatis.annotations.CacheNamespace;
import org.apache.ibatis.annotations.Param;

import org.verwandlung.voj.web.model.UserGroup;

/**
 * UserGroup Data Access Object.
 * 
 * @author Haozhe Xie
 */
@CacheNamespace(implementation = org.mybatis.caches.ehcache.EhcacheCache.class)
public interface UserGroupMapper {
	/**
	 * 获取全部的用户组对象.
	 * @return 全部的用户组对象的列表
	 */
	List<UserGroup> getUserGroups();
	
	/**
	 * 通过用户组的唯一标识符获取用户组对象.
	 * @param userGroupId - 用户组的唯一标识符
	 * @return 预期的用户组对象或空引用
	 */
	UserGroup getUserGroupUsingId(@Param("userGroupId") int userGroupId);
	
	/**
	 * 通过用户组的别名获取用户组对象.
	 * @param userGroupSlug - 用户组的别名
	 * @return 预期的用户组对象或空引用
	 */
	UserGroup getUserGroupUsingSlug(@Param("userGroupSlug") String userGroupSlug);
}
