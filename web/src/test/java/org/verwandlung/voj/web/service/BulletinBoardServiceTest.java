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
 * The test class for BulletinBoardService.
 *
 * @author Haozhe Xie
 */
@ExtendWith(SpringExtension.class)
@Transactional
@ContextConfiguration({"classpath:test-spring-context.xml"})
public class BulletinBoardServiceTest {
  /** Test case: tests the getNumberOfBulletinBoardMessages() method. Test data: the table is empty. Expected: returns 0. */
  @Test
  public void testGetNumberOfBulletinBoardMessagesWhenEmpty() {
    Assertions.assertEquals(0, bulletinBoardService.getNumberOfBulletinBoardMessages());
  }

  /** Test case: tests the getNumberOfBulletinBoardMessages() method. Test data: create two messages. Expected: returns 2. */
  @Test
  public void testGetNumberOfBulletinBoardMessagesAfterCreate() {
    createMessage("First");
    createMessage("Second");
    Assertions.assertEquals(2, bulletinBoardService.getNumberOfBulletinBoardMessages());
  }

  /** Test case: tests the getBulletinBoardMessages(long, int) method. Test data: create multiple messages. Expected: paginated results ordered by messageId descending. */
  @Test
  public void testGetBulletinBoardMessages() {
    createMessage("First");
    createMessage("Second");

    List<BulletinBoardMessage> messages = bulletinBoardService.getBulletinBoardMessages(0, 10);
    Assertions.assertEquals(2, messages.size());
    Assertions.assertEquals("Second", messages.get(0).getMessageTitle());
  }

  /** Test case: tests the getBulletinBoardMessage(long) method. Test data: an existing message identifier. Expected: the corresponding bulletin board message. */
  @Test
  public void testGetBulletinBoardMessageExists() {
    BulletinBoardMessage created = createMessage("Title");

    BulletinBoardMessage message = bulletinBoardService.getBulletinBoardMessage(created.getMessageId());
    Assertions.assertNotNull(message);
    Assertions.assertEquals("Title", message.getMessageTitle());
  }

  /** Test case: tests the getBulletinBoardMessage(long) method. Test data: a non-existing message identifier. Expected: a null reference. */
  @Test
  public void testGetBulletinBoardMessageNotExists() {
    Assertions.assertNull(bulletinBoardService.getBulletinBoardMessage(0));
  }

  /** Creates a bulletin board message through the Mapper in order to test the read logic of the Service. */
  private BulletinBoardMessage createMessage(String title) {
    BulletinBoardMessage message = new BulletinBoardMessage(title, title + " Body", new Date());
    bulletinBoardMessageMapper.createBulletinBoardMessage(message);
    return message;
  }

  /** The BulletinBoardService object under test. */
  @Autowired private BulletinBoardService bulletinBoardService;

  /** The Mapper used to construct bulletin board message data within the test transaction. */
  @Autowired private BulletinBoardMessageMapper bulletinBoardMessageMapper;
}
