package com.trunkshell.voj.web.mapper;

import org.apache.ibatis.annotations.CacheNamespace;
import org.apache.ibatis.annotations.Param;

import com.trunkshell.voj.web.model.UserGroup;

/**
 * UserGroup Data Access Object.
 * 
 * @author Xie Haozhe
 */
@CacheNamespace(implementation = org.mybatis.caches.ehcache.EhcacheCache.class)
public interface UserGroupMapper {
    /**
     * 通过用户组的唯一标识符获取用户组对象.
     * @param userGroupId - 用户组的唯一标识符
     * @return 预期的用户组对象或空引用
     */
    public UserGroup getUserGroupUsingId(@Param("userGroupId") int userGroupId);
    
    /**
     * 通过用户组的唯一英文缩写获取用户组对象.
     * @param userGroupSlug - 用户组的唯一英文缩写
     * @return 预期的用户组对象或空引用
     */
    public UserGroup getUserGroupUsingSlug(@Param("userGroupSlug") String userGroupSlug);
}
