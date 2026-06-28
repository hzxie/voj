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
 * The test class for BulletinBoardMessageMapper.
 *
 * @author Haozhe Xie
 */
@ExtendWith(SpringExtension.class)
@Transactional
@ContextConfiguration({"classpath:test-spring-context.xml"})
public class BulletinBoardMessageMapperTest {
  /** Test case: tests the getNumberOfBulletinBoardMessages() method. Test data: N/a. Expected: the bulletin board messages in the table are empty, returns 0. */
  @Test
  public void testGetNumberOfBulletinBoardMessagesWhenEmpty() {
    long numberOfMessages = bulletinBoardMessageMapper.getNumberOfBulletinBoardMessages();
    Assertions.assertEquals(0, numberOfMessages);
  }

  /** Test case: tests the getNumberOfBulletinBoardMessages() method. Test data: creates one bulletin board message. Expected: returns 1. */
  @Test
  public void testGetNumberOfBulletinBoardMessagesAfterCreate() {
    bulletinBoardMessageMapper.createBulletinBoardMessage(
        new BulletinBoardMessage("Title", "Body", new Date()));

    long numberOfMessages = bulletinBoardMessageMapper.getNumberOfBulletinBoardMessages();
    Assertions.assertEquals(1, numberOfMessages);
  }

  /** Test case: tests the getBulletinBoardMessages(long, int) method. Test data: N/a. Expected: the table is empty, returns an empty list. */
  @Test
  public void testGetBulletinBoardMessagesWhenEmpty() {
    List<BulletinBoardMessage> messages =
        bulletinBoardMessageMapper.getBulletinBoardMessages(0, 10);
    Assertions.assertNotNull(messages);
    Assertions.assertTrue(messages.isEmpty());
  }

  /** Test case: tests the getBulletinBoardMessages(long, int) method. Test data: creates multiple messages. Expected: returns them ordered by messageId descending. */
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

  /** Test case: tests the getBulletinBoardMessages(long, int) method. Test data: creates two messages, with limit=1. Expected: returns only 1 message. */
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

  /** Test case: tests the getPublishedBulletinBoardMessages(long, int) method. Test data: messages with mixed statuses and pins. Expected: only PUBLISHED messages are returned, pinned ones first. */
  @Test
  public void testGetPublishedBulletinBoardMessagesFiltersAndPins() {
    createMessage("Hidden", false, BulletinBoardMessage.STATUS_HIDDEN);
    createMessage("Draft", false, BulletinBoardMessage.STATUS_DRAFT);
    createMessage("Plain", false, BulletinBoardMessage.STATUS_PUBLISHED);
    createMessage("Pinned", true, BulletinBoardMessage.STATUS_PUBLISHED);

    List<BulletinBoardMessage> messages =
        bulletinBoardMessageMapper.getPublishedBulletinBoardMessages(0, 10);
    Assertions.assertEquals(2, messages.size());
    Assertions.assertEquals("Pinned", messages.get(0).getMessageTitle());
    Assertions.assertEquals("Plain", messages.get(1).getMessageTitle());
  }

  /** Creates a bulletin board message with the given pin flag and status, used by the published-list test. */
  private void createMessage(String title, boolean pinned, String status) {
    BulletinBoardMessage message = new BulletinBoardMessage(title, title + " Body", new Date());
    message.setPinned(pinned);
    message.setStatus(status);
    bulletinBoardMessageMapper.createBulletinBoardMessage(message);
  }

  /** Test case: tests the getBulletinBoardMessageUsingId(long) method. Test data: a non-existing message identifier. Expected: a null reference. */
  @Test
  public void testGetBulletinBoardMessageUsingIdNotExists() {
    BulletinBoardMessage message = bulletinBoardMessageMapper.getBulletinBoardMessageUsingId(0);
    Assertions.assertNull(message);
  }

  /** Test case: tests the getBulletinBoardMessageUsingId(long) method. Test data: an existing message identifier. Expected: the expected bulletin board message. */
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

  /** Test case: tests the createBulletinBoardMessage(BulletinBoardMessage) method. Test data: a valid bulletin board message. Expected: the data creation operation completes successfully, and the auto-increment primary key is filled back. */
  @Test
  public void testCreateBulletinBoardMessage() {
    BulletinBoardMessage message = new BulletinBoardMessage("New Message", "Message Body", new Date());
    int numberOfRowsAffected = bulletinBoardMessageMapper.createBulletinBoardMessage(message);
    Assertions.assertEquals(1, numberOfRowsAffected);
    Assertions.assertTrue(message.getMessageId() > 0);
  }

  /** Test case: tests the createBulletinBoardMessage(BulletinBoardMessage) method. Test data: a message with author, pinned and status set. Expected: the author identifier, pinned flag and status are persisted and read back. */
  @Test
  public void testCreatePersistsAuthorPinnedAndStatus() {
    BulletinBoardMessage message = new BulletinBoardMessage("Pinned", "Body", new Date());
    message.setMessageAuthorId(1000);
    message.setPinned(true);
    message.setStatus(BulletinBoardMessage.STATUS_DRAFT);
    bulletinBoardMessageMapper.createBulletinBoardMessage(message);

    BulletinBoardMessage stored =
        bulletinBoardMessageMapper.getBulletinBoardMessageUsingId(message.getMessageId());
    Assertions.assertEquals(1000, stored.getMessageAuthorId());
    Assertions.assertTrue(stored.isPinned());
    Assertions.assertEquals(BulletinBoardMessage.STATUS_DRAFT, stored.getStatus());
  }

  /** Test case: tests the updateBulletinBoardMessage(BulletinBoardMessage) method. Test data: an existing message whose pinned flag and status are changed. Expected: the new pinned flag and status are persisted. */
  @Test
  public void testUpdatePersistsPinnedAndStatus() {
    BulletinBoardMessage message = new BulletinBoardMessage("Title", "Body", new Date());
    bulletinBoardMessageMapper.createBulletinBoardMessage(message);

    message.setPinned(true);
    message.setStatus(BulletinBoardMessage.STATUS_HIDDEN);
    bulletinBoardMessageMapper.updateBulletinBoardMessage(message);

    BulletinBoardMessage stored =
        bulletinBoardMessageMapper.getBulletinBoardMessageUsingId(message.getMessageId());
    Assertions.assertTrue(stored.isPinned());
    Assertions.assertEquals(BulletinBoardMessage.STATUS_HIDDEN, stored.getStatus());
  }

  /** Test case: tests the updateBulletinBoardMessage(BulletinBoardMessage) method. Test data: an existing message identifier. Expected: the data update operation completes successfully. */
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

  /** Test case: tests the updateBulletinBoardMessage(BulletinBoardMessage) method. Test data: a non-existing message identifier. Expected: the method executes normally without affecting the data in the table. */
  @Test
  public void testUpdateBulletinBoardMessageNotExists() {
    BulletinBoardMessage message = new BulletinBoardMessage(0, "Title", "Body", new Date());
    int numberOfRowsAffected = bulletinBoardMessageMapper.updateBulletinBoardMessage(message);
    Assertions.assertEquals(0, numberOfRowsAffected);
  }

  /** Test case: tests the deleteBulletinBoardMessage(long) method. Test data: an existing message identifier. Expected: the data deletion operation completes successfully. */
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

  /** Test case: tests the deleteBulletinBoardMessage(long) method. Test data: a non-existing message identifier. Expected: the method executes normally without affecting the data in the table. */
  @Test
  public void testDeleteBulletinBoardMessageNotExists() {
    int numberOfRowsAffected = bulletinBoardMessageMapper.deleteBulletinBoardMessage(0);
    Assertions.assertEquals(0, numberOfRowsAffected);
  }

  /** The BulletinBoardMessageMapper object under test. */
  @Autowired private BulletinBoardMessageMapper bulletinBoardMessageMapper;
}
