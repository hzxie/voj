package org.verwandlung.voj.web.mapper;

import org.apache.ibatis.annotations.CacheNamespace;
import org.apache.ibatis.annotations.Param;
import org.verwandlung.voj.web.model.BulletinBoardMessage;

import java.util.List;

/**
 * BulletinBoardMessage Data Access Object.
 *
 * @author Haozhe Xie
 */
@CacheNamespace(implementation = org.mybatis.caches.ehcache.EhcacheCache.class)
public interface BulletinBoardMessageMapper {
	/**
	 * 获取布告栏消息的数量.
	 * @return 布告栏消息的数量
	 */
	long getNumberOfBulletinBoardMessages();

	/**
	 * 获取部分布告栏消息.
	 * @param offset - 布告栏消息唯一标识符的起始编号
	 * @param limit - 获取布告栏消息的数量
	 * @return 包含布告栏消息的列表
	 */
	List<BulletinBoardMessage> getBulletinBoardMessages(
			@Param("offset") long offset, @Param("limit") int limit);

	/**
	 * 使用布告栏消息的唯一标识符获取布告栏消息.
	 * @param bulletinBoardMessageId - 布告栏消息的唯一标识符
	 * @return 特定的布告栏消息
	 */
	BulletinBoardMessage getBulletinBoardMessageUsingId(
			@Param("messageId") long bulletinBoardMessageId);

	/**
	 * 创建布告栏消息.
	 * @param bbm - 待创建的布告栏消息
	 * @return 受影响记录的数量
	 */
	int createBulletinBoardMessage(BulletinBoardMessage bbm);

	/**
	 * 更新布告栏消息.
	 * @param bbm - 待更新的布告栏消息
	 * @return 受影响记录的数量
	 */
	int updateBulletinBoardMessage(BulletinBoardMessage bbm);

	/**
	 * 删除布告栏消息.
	 * @param bulletinBoardMessageId - 待删除布告栏消息的唯一标识符
	 * @return 受影响记录的数量
	 */
	int deleteBulletinBoardMessage(
			@Param("messageId") long bulletinBoardMessageId);
}
