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
package org.verwandlung.voj.web.mapper;

import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

import org.verwandlung.voj.web.model.BulletinBoardMessage;

/**
 * BulletinBoardMessageMapper测试类.
 *
 * @author Haozhe Xie
 */
@ExtendWith(SpringExtension.class)
@Transactional
@ContextConfiguration({"classpath:test-spring-context.xml"})
public class BulletinBoardMessageMapperTest {
  /** 测试用例: 测试getNumberOfBulletinBoardMessages()方法 测试数据: N/a 预期结果: 数据表中布告栏消息为空, 返回0 */
  @Test
  public void testGetNumberOfBulletinBoardMessagesWhenEmpty() {
    long numberOfMessages = bulletinBoardMessageMapper.getNumberOfBulletinBoardMessages();
    Assertions.assertEquals(0, numberOfMessages);
  }

  /** 测试用例: 测试getNumberOfBulletinBoardMessages()方法 测试数据: 创建一条布告栏消息 预期结果: 返回1 */
  @Test
  public void testGetNumberOfBulletinBoardMessagesAfterCreate() {
    bulletinBoardMessageMapper.createBulletinBoardMessage(
        new BulletinBoardMessage("Title", "Body", new Date()));

    long numberOfMessages = bulletinBoardMessageMapper.getNumberOfBulletinBoardMessages();
    Assertions.assertEquals(1, numberOfMessages);
  }

  /** 测试用例: 测试getBulletinBoardMessages(long, int)方法 测试数据: N/a 预期结果: 数据表为空, 返回空列表 */
  @Test
  public void testGetBulletinBoardMessagesWhenEmpty() {
    List<BulletinBoardMessage> messages =
        bulletinBoardMessageMapper.getBulletinBoardMessages(0, 10);
    Assertions.assertNotNull(messages);
    Assertions.assertTrue(messages.isEmpty());
  }

  /** 测试用例: 测试getBulletinBoardMessages(long, int)方法 测试数据: 创建多条消息 预期结果: 按messageId降序返回 */
  @Test
  public void testGetBulletinBoardMessagesOrderedByIdDesc() {
    bulletinBoardMessageMapper.createBulletinBoardMessage(
        new BulletinBoardMessage("First", "First Body", new Date()));
    bulletinBoardMessageMapper.createBulletinBoardMessage(
        new BulletinBoardMessage("Second", "Second Body", new Date()));

    List<BulletinBoardMessage> messages =
        bulletinBoardMessageMapper.getBulletinBoardMessages(0, 10);
    Assertions.assertEquals(2, messages.size());
    Assertions.assertEquals("Second", messages.get(0).getMessageTitle());
    Assertions.assertEquals("First", messages.get(1).getMessageTitle());
  }

  /** 测试用例: 测试getBulletinBoardMessages(long, int)方法 测试数据: 创建两条消息, 使用limit=1 预期结果: 仅返回1条消息 */
  @Test
  public void testGetBulletinBoardMessagesWithLimit() {
    bulletinBoardMessageMapper.createBulletinBoardMessage(
        new BulletinBoardMessage("First", "First Body", new Date()));
    bulletinBoardMessageMapper.createBulletinBoardMessage(
        new BulletinBoardMessage("Second", "Second Body", new Date()));

    List<BulletinBoardMessage> messages =
        bulletinBoardMessageMapper.getBulletinBoardMessages(0, 1);
    Assertions.assertEquals(1, messages.size());
    Assertions.assertEquals("Second", messages.get(0).getMessageTitle());
  }

  /** 测试用例: 测试getBulletinBoardMessageUsingId(long)方法 测试数据: 使用不存在的消息标识符 预期结果: 返回空引用 */
  @Test
  public void testGetBulletinBoardMessageUsingIdNotExists() {
    BulletinBoardMessage message = bulletinBoardMessageMapper.getBulletinBoardMessageUsingId(0);
    Assertions.assertNull(message);
  }

  /** 测试用例: 测试getBulletinBoardMessageUsingId(long)方法 测试数据: 使用存在的消息标识符 预期结果: 返回预期的布告栏消息 */
  @Test
  public void testGetBulletinBoardMessageUsingIdExists() {
    BulletinBoardMessage newMessage = new BulletinBoardMessage("Title", "Body", new Date());
    bulletinBoardMessageMapper.createBulletinBoardMessage(newMessage);

    BulletinBoardMessage message =
        bulletinBoardMessageMapper.getBulletinBoardMessageUsingId(newMessage.getMessageId());
    Assertions.assertNotNull(message);
    Assertions.assertEquals("Title", message.getMessageTitle());
    Assertions.assertEquals("Body", message.getMessageBody());
  }

  /** 测试用例: 测试createBulletinBoardMessage(BulletinBoardMessage)方法 测试数据: 合法的布告栏消息 预期结果: 数据创建操作成功完成, 并回填自增主键 */
  @Test
  public void testCreateBulletinBoardMessage() {
    BulletinBoardMessage message = new BulletinBoardMessage("New Message", "Message Body", new Date());
    int numberOfRowsAffected = bulletinBoardMessageMapper.createBulletinBoardMessage(message);
    Assertions.assertEquals(1, numberOfRowsAffected);
    Assertions.assertTrue(message.getMessageId() > 0);
  }

  /** 测试用例: 测试updateBulletinBoardMessage(BulletinBoardMessage)方法 测试数据: 存在的消息标识符 预期结果: 数据更新操作成功完成 */
  @Test
  public void testUpdateBulletinBoardMessageExists() {
    BulletinBoardMessage message = new BulletinBoardMessage("Title", "Body", new Date());
    bulletinBoardMessageMapper.createBulletinBoardMessage(message);

    message.setMessageTitle("Updated Title");
    message.setMessageBody("Updated Body");
    int numberOfRowsAffected = bulletinBoardMessageMapper.updateBulletinBoardMessage(message);
    Assertions.assertEquals(1, numberOfRowsAffected);

    BulletinBoardMessage updatedMessage =
        bulletinBoardMessageMapper.getBulletinBoardMessageUsingId(message.getMessageId());
    Assertions.assertEquals("Updated Title", updatedMessage.getMessageTitle());
    Assertions.assertEquals("Updated Body", updatedMessage.getMessageBody());
  }

  /** 测试用例: 测试updateBulletinBoardMessage(BulletinBoardMessage)方法 测试数据: 不存在的消息标识符 预期结果: 方法正常执行, 未影响数据表中的数据 */
  @Test
  public void testUpdateBulletinBoardMessageNotExists() {
    BulletinBoardMessage message = new BulletinBoardMessage(0, "Title", "Body", new Date());
    int numberOfRowsAffected = bulletinBoardMessageMapper.updateBulletinBoardMessage(message);
    Assertions.assertEquals(0, numberOfRowsAffected);
  }

  /** 测试用例: 测试deleteBulletinBoardMessage(long)方法 测试数据: 存在的消息标识符 预期结果: 数据删除操作成功完成 */
  @Test
  public void testDeleteBulletinBoardMessageExists() {
    BulletinBoardMessage message = new BulletinBoardMessage("Title", "Body", new Date());
    bulletinBoardMessageMapper.createBulletinBoardMessage(message);

    int numberOfRowsAffected =
        bulletinBoardMessageMapper.deleteBulletinBoardMessage(message.getMessageId());
    Assertions.assertEquals(1, numberOfRowsAffected);
    Assertions.assertNull(
        bulletinBoardMessageMapper.getBulletinBoardMessageUsingId(message.getMessageId()));
  }

  /** 测试用例: 测试deleteBulletinBoardMessage(long)方法 测试数据: 不存在的消息标识符 预期结果: 方法正常执行, 未影响数据表中的数据 */
  @Test
  public void testDeleteBulletinBoardMessageNotExists() {
    int numberOfRowsAffected = bulletinBoardMessageMapper.deleteBulletinBoardMessage(0);
    Assertions.assertEquals(0, numberOfRowsAffected);
  }

  /** 待测试的BulletinBoardMessageMapper对象. */
  @Autowired private BulletinBoardMessageMapper bulletinBoardMessageMapper;
}
