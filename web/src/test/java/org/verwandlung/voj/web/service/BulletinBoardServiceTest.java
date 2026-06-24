/* Verwandlung Online Judge - A cross-platform judge online system
 * Copyright (C) 2014-2026 Haozhe Xie <root@haozhexie.com>
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
 */
package org.verwandlung.voj.web.service;

import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

import org.verwandlung.voj.web.mapper.BulletinBoardMessageMapper;
import org.verwandlung.voj.web.model.BulletinBoardMessage;

/**
 * BulletinBoardService测试类.
 *
 * @author Haozhe Xie
 */
@ExtendWith(SpringExtension.class)
@Transactional
@ContextConfiguration({"classpath:test-spring-context.xml"})
public class BulletinBoardServiceTest {
  /** 测试用例: 测试getNumberOfBulletinBoardMessages()方法 测试数据: 数据表为空 预期结果: 返回0 */
  @Test
  public void testGetNumberOfBulletinBoardMessagesWhenEmpty() {
    Assertions.assertEquals(0, bulletinBoardService.getNumberOfBulletinBoardMessages());
  }

  /** 测试用例: 测试getNumberOfBulletinBoardMessages()方法 测试数据: 创建两条消息 预期结果: 返回2 */
  @Test
  public void testGetNumberOfBulletinBoardMessagesAfterCreate() {
    createMessage("First");
    createMessage("Second");
    Assertions.assertEquals(2, bulletinBoardService.getNumberOfBulletinBoardMessages());
  }

  /** 测试用例: 测试getBulletinBoardMessages(long, int)方法 测试数据: 创建多条消息 预期结果: 按messageId降序分页返回 */
  @Test
  public void testGetBulletinBoardMessages() {
    createMessage("First");
    createMessage("Second");

    List<BulletinBoardMessage> messages = bulletinBoardService.getBulletinBoardMessages(0, 10);
    Assertions.assertEquals(2, messages.size());
    Assertions.assertEquals("Second", messages.get(0).getMessageTitle());
  }

  /** 测试用例: 测试getBulletinBoardMessage(long)方法 测试数据: 存在的消息标识符 预期结果: 返回对应的布告栏消息 */
  @Test
  public void testGetBulletinBoardMessageExists() {
    BulletinBoardMessage created = createMessage("Title");

    BulletinBoardMessage message = bulletinBoardService.getBulletinBoardMessage(created.getMessageId());
    Assertions.assertNotNull(message);
    Assertions.assertEquals("Title", message.getMessageTitle());
  }

  /** 测试用例: 测试getBulletinBoardMessage(long)方法 测试数据: 不存在的消息标识符 预期结果: 返回空引用 */
  @Test
  public void testGetBulletinBoardMessageNotExists() {
    Assertions.assertNull(bulletinBoardService.getBulletinBoardMessage(0));
  }

  /** 通过Mapper创建一条布告栏消息以便测试Service的读取逻辑. */
  private BulletinBoardMessage createMessage(String title) {
    BulletinBoardMessage message = new BulletinBoardMessage(title, title + " Body", new Date());
    bulletinBoardMessageMapper.createBulletinBoardMessage(message);
    return message;
  }

  /** 待测试的BulletinBoardService对象. */
  @Autowired private BulletinBoardService bulletinBoardService;

  /** 用于在测试事务内构造布告栏消息数据的Mapper. */
  @Autowired private BulletinBoardMessageMapper bulletinBoardMessageMapper;
}
