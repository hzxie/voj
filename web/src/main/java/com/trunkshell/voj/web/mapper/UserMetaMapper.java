package com.trunkshell.voj.web.mapper;

import java.util.Date;
import java.util.List;

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

import com.trunkshell.voj.web.model.User;
import com.trunkshell.voj.web.model.UserMeta;

/**
 * UserMeta Data Access Object.
 * 
 * @author Xie Haozhe
 */
@CacheNamespace(implementation = org.mybatis.caches.ehcache.EhcacheCache.class)
public interface UserMetaMapper {
    /**
     * [此方法仅供管理员使用]
     * 获取系统中某段时间内注册用户的总数.
     * @param startTime - 统计起始时间
     * @param endTime - 统计结束时间
     * @return 系统中某段时间内注册用户的总数
     */
    @Select("SELECT COUNT(*) FROM voj_usermeta WHERE meta_key = 'RegisterTime' AND meta_value >= #{startTime} AND meta_value <= #{endTime}")
    @Options(useCache = true)
    public long getNumberOfUserRegistered(@Param("startTime") Date startTime, @Param("endTime") Date endTime);
    
    /**
     * 获取某个用户的全部元信息.
     * @param user - 某个用户对象
     * @return 一个包含元信息的列表
     */
    @Select("SELECT * FROM voj_usermeta WHERE uid = #{user.uid}")
    @Options(useCache = true)
    @Results({
        @Result(property = "metaId", column = "meta_id"),
        @Result(property = "user", column = "uid", javaType=User.class, one = @One(select="com.trunkshell.voj.web.mapper.UserMapper.getUserUsingUid")),
        @Result(property = "metaKey", column = "meta_key"),
        @Result(property = "metaValue", column = "meta_value")
    })
    public List<UserMeta> getUserMetaUsingUser(@Param("user") User user);
    
    /**
     * 获取某个用户的某个特定的元信息.
     * @param user - 某个用户对象
     * @param metaKey - 元信息的键
     * @return 预期的元信息对象
     */
    @Select("SELECT * FROM voj_usermeta WHERE uid = #{user.uid} AND meta_key = #{metaKey}")
    @Options(useCache = true)
    @Results({
        @Result(property = "metaId", column = "meta_id"),
        @Result(property = "user", column = "uid", javaType=User.class, one = @One(select="com.trunkshell.voj.web.mapper.UserMapper.getUserUsingUid")),
        @Result(property = "metaKey", column = "meta_key"),
        @Result(property = "metaValue", column = "meta_value")
    })
    public UserMeta getUserMetaUsingUserAndMetaKey(@Param("user") User user, @Param("metaKey") String metaKey);
    
    /**
     * 插入新的用户元数据.
     * @param userMeta
     */
    @Insert("INSERT INTO voj_usermeta (uid, meta_key, meta_value) VALUES (#{user.uid}, #{metaKey}, #{metaValue})")
    @Options(flushCache = true)
    public void createUserMeta(UserMeta userMeta);
    
    /**
     * 更新某个用户的元数据.
     * @param userMeta - 待更新的元数据
     */
    @Update("UPDATE voj_usermeta SET meta_value = #{metaValue} WHERE meta_id = #{metaId}")
    @Options(flushCache = true)
    public void updateUserMeta(UserMeta userMeta);
    
    /**
     * 删除某个用户的全部元数据.
     * @param uid - 用户的唯一标识符
     */
    @Delete("DELETE FROM voj_usermeta WHERE uid = #{uid}")
    @Options(flushCache = true)
    public void deleteUserMetaUsingUser(long uid);
}
