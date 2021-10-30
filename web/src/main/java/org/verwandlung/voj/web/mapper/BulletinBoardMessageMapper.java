/* Verwandlung Online Judge - A cross-platform judge online system
 * Copyright (C) 2018 Haozhe Xie <cshzxie@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 *
 *                              _ooOoo_
 *                             o8888888o
 *                             88" . "88
 *                             (| -_- |)
 *                             O\  =  /O
 *                          ____/`---'\____
 *                        .'  \\|     |//  `.
 *                       /  \\|||  :  |||//  \
 *                      /  _||||| -:- |||||-  \
 *                      |   | \\\  -  /// |   |
 *                      | \_|  ''\---/''  |   |
 *                      \  .-\__  `-`  ___/-. /
 *                    ___`. .'  /--.--\  `. . __
 *                 ."" '<  `.___\_<|>_/___.'  >'"".
 *                | | :  `- \`.;`\ _ /`;.`/ - ` : | |
 *                \  \ `-.   \_ __\ /__ _/   .-` /  /
 *           ======`-.____`-.___\_____/___.-`____.-'======
 *                              `=---='
 *
 *                          HERE BE BUDDHA
 *
 */
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
   *
   * @return 布告栏消息的数量
   */
  long getNumberOfBulletinBoardMessages();

  /**
   * 获取部分布告栏消息.
   *
   * @param offset - 布告栏消息唯一标识符的起始编号
   * @param limit - 获取布告栏消息的数量
   * @return 包含布告栏消息的列表
   */
  List<BulletinBoardMessage> getBulletinBoardMessages(
      @Param("offset") long offset, @Param("limit") int limit);

  /**
   * 使用布告栏消息的唯一标识符获取布告栏消息.
   *
   * @param bulletinBoardMessageId - 布告栏消息的唯一标识符
   * @return 特定的布告栏消息
   */
  BulletinBoardMessage getBulletinBoardMessageUsingId(
      @Param("messageId") long bulletinBoardMessageId);

  /**
   * 创建布告栏消息.
   *
   * @param bbm - 待创建的布告栏消息
   * @return 受影响记录的数量
   */
  int createBulletinBoardMessage(BulletinBoardMessage bbm);

  /**
   * 更新布告栏消息.
   *
   * @param bbm - 待更新的布告栏消息
   * @return 受影响记录的数量
   */
  int updateBulletinBoardMessage(BulletinBoardMessage bbm);

  /**
   * 删除布告栏消息.
   *
   * @param bulletinBoardMessageId - 待删除布告栏消息的唯一标识符
   * @return 受影响记录的数量
   */
  int deleteBulletinBoardMessage(@Param("messageId") long bulletinBoardMessageId);
}
