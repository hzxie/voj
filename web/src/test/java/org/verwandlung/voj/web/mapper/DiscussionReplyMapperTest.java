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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;
import org.verwandlung.voj.web.model.DiscussionReply;
import org.verwandlung.voj.web.model.User;

import java.util.List;

/**
 * The test class for DiscussionReplyMapper.
 *
 * @author Haozhe Xie
 */
@ExtendWith(SpringExtension.class)
@Transactional
@ContextConfiguration({"classpath:test-spring-context.xml"})
public class DiscussionReplyMapperTest {
  /**
   * Test case: tests the getDiscussionRepliesUsingThreadId(long, long, int) method. Test data:
   * ThreadId: 2, Offset: 0, Limit: 2. Expected: the first 2 replies of the second discussion thread.
   */
  @Test
  public void testGetDiscussionRepliesOfThread2WithOffsetFrom0WithLimit2() {
    List<DiscussionReply> discussionReplies =
        discussionReplyMapper.getDiscussionRepliesUsingThreadId(2, 0, 2, false);
    Assertions.assertEquals(2, discussionReplies.size());

    DiscussionReply firstDiscussionReply = discussionReplies.get(0);
    long replyId = firstDiscussionReply.getDiscussionReplyId();
    Assertions.assertEquals(2, replyId);

    String replyContent = firstDiscussionReply.getDiscussionReplyContent();
    Assertions.assertEquals("Reply content for thread #2", replyContent);
  }

  /**
   * Test case: tests the getDiscussionRepliesUsingThreadId(long, long, int) method. Test data:
   * ThreadId: 2, Offset: 1, Limit: 1. Expected: the 2nd reply of the second discussion thread.
   */
  @Test
  public void testGetDiscussionRepliesOfThread2WithOffsetFrom1WithLimit1() {
    List<DiscussionReply> discussionReplies =
        discussionReplyMapper.getDiscussionRepliesUsingThreadId(2, 1, 1, false);
    Assertions.assertEquals(1, discussionReplies.size());

    DiscussionReply firstDiscussionReply = discussionReplies.get(0);
    long replyId = firstDiscussionReply.getDiscussionReplyId();
    Assertions.assertEquals(3, replyId);

    String replyContent = firstDiscussionReply.getDiscussionReplyContent();
    Assertions.assertEquals("Reply content for thread #2", replyContent);
  }

  /** Test case: tests the createDiscussionReply(DiscussionReply) method. Test data: a valid data set. Expected: the data insertion operation completes successfully. */
  @Test
  public void testCreateDiscussionReplyNormally() {
    long threadId = 1;
    User creator = userMapper.getUserUsingUid(1000);
    Assertions.assertNotNull(creator);

    DiscussionReply discussionReply = new DiscussionReply(threadId, creator, "Content");
    int numberOfRowsAffected = discussionReplyMapper.createDiscussionReply(discussionReply);
    Assertions.assertEquals(1, numberOfRowsAffected);
  }

  /**
   * Test case: tests the createDiscussionReply(DiscussionReply) method. Test data: a non-existing user.
   * Expected: a DataIntegrityViolationException is thrown.
   */
  @Test
  public void testCreateDiscussionReplyWithNotExistingUser() {
    long threadId = 1;
    User creator = new User();
    creator.setUid(0);

    DiscussionReply discussionReply = new DiscussionReply(threadId, creator, "Content");
    Executable e =
        () -> {
          discussionReplyMapper.createDiscussionReply(discussionReply);
        };
    Assertions.assertThrows(org.springframework.dao.DataIntegrityViolationException.class, e);
  }

  /** Test case: tests the updateDiscussionReply(DiscussionReply) method. Test data: updates with valid data. Expected: the data update operation completes successfully. */
  @Test
  public void testUpdateDiscussionReplyNormally() {
    List<DiscussionReply> discussionReplies =
        discussionReplyMapper.getDiscussionRepliesUsingThreadId(1, 0, 1, false);
    Assertions.assertEquals(1, discussionReplies.size());

    DiscussionReply discussionReply = discussionReplies.get(0);
    discussionReply.setDiscussionReplyContent("New Reply Content");
    int numberOfRowsAffected = discussionReplyMapper.updateDiscussionReply(discussionReply);
    Assertions.assertEquals(1, numberOfRowsAffected);
  }

  /** Test case: tests the deleteDiscussionReplyUsingReplyId(long) method. Test data: an existing discussion reply unique identifier. Expected: the data deletion operation completes successfully. */
  @Test
  public void testDeleteDiscussionReplyUsingExistingReplyId() {
    int numberOfRowsAffected = discussionReplyMapper.deleteDiscussionReplyUsingReplyId(1);
    Assertions.assertEquals(1, numberOfRowsAffected);
  }

  /**
   * Test case: tests the deleteDiscussionReplyUsingReplyId(long) method. Test data: a non-existing discussion reply unique identifier. Expected: the method executes normally without affecting the data in the table.
   */
  @Test
  public void testDeleteDiscussionReplyUsingReplyIdNotExists() {
    int numberOfRowsAffected = discussionReplyMapper.deleteDiscussionReplyUsingReplyId(0);
    Assertions.assertEquals(0, numberOfRowsAffected);
  }

  /** The DiscussionReplyMapper object under test. */
  @Autowired private DiscussionReplyMapper discussionReplyMapper;

  /** The UserMapper object under test. */
  @Autowired private UserMapper userMapper;
}
