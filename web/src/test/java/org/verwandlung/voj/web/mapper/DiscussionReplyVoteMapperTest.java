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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;
import org.verwandlung.voj.web.model.DiscussionReplyVote;

/**
 * The test class for DiscussionReplyVoteMapper.
 *
 * <p>Seed votes: reply 1 has an upvote from user 1000 and a downvote from user 1002; reply 2 has an
 * upvote from user 1000 and a downvote from user 1001; reply 3 has no votes. There are no report
 * rows in the seed data.
 *
 * @author Haozhe Xie
 */
@ExtendWith(SpringExtension.class)
@Transactional
@ContextConfiguration({"classpath:test-spring-context.xml"})
public class DiscussionReplyVoteMapperTest {
  /** Test case: tests getNumberOfVotes(...). Test data: replies with and without votes. Expected: the per-type vote counts. */
  @Test
  public void testGetNumberOfVotes() {
    Assertions.assertEquals(
        1, discussionReplyVoteMapper.getNumberOfVotes(1, DiscussionReplyVote.VOTE_UP));
    Assertions.assertEquals(
        1, discussionReplyVoteMapper.getNumberOfVotes(1, DiscussionReplyVote.VOTE_DOWN));
    Assertions.assertEquals(
        0, discussionReplyVoteMapper.getNumberOfVotes(3, DiscussionReplyVote.VOTE_UP));
  }

  /** Test case: tests getVoteType(...). Test data: users who upvoted / downvoted / did not vote. Expected: the up/down vote type, or null. */
  @Test
  public void testGetVoteTypeReturnsUpOrDown() {
    Assertions.assertEquals(
        DiscussionReplyVote.VOTE_UP, discussionReplyVoteMapper.getVoteType(1, 1000).intValue());
    Assertions.assertEquals(
        DiscussionReplyVote.VOTE_DOWN, discussionReplyVoteMapper.getVoteType(1, 1002).intValue());
    Assertions.assertNull(discussionReplyVoteMapper.getVoteType(3, 1000));
  }

  /** Test case: tests castVote(...). Test data: a new vote on a reply with no votes. Expected: the vote is inserted. */
  @Test
  public void testCastVoteInsertsNewVote() {
    discussionReplyVoteMapper.castVote(new DiscussionReplyVote(3, 1002, DiscussionReplyVote.VOTE_UP));
    Assertions.assertEquals(
        1, discussionReplyVoteMapper.getNumberOfVotes(3, DiscussionReplyVote.VOTE_UP));
  }

  /** Test case: tests castVote(...). Test data: the same vote cast twice. Expected: only one vote is recorded (idempotent upsert). */
  @Test
  public void testCastVoteIsIdempotent() {
    discussionReplyVoteMapper.castVote(new DiscussionReplyVote(3, 1002, DiscussionReplyVote.VOTE_UP));
    discussionReplyVoteMapper.castVote(new DiscussionReplyVote(3, 1002, DiscussionReplyVote.VOTE_UP));
    Assertions.assertEquals(
        1, discussionReplyVoteMapper.getNumberOfVotes(3, DiscussionReplyVote.VOTE_UP));
  }

  /** Test case: tests withdrawVote(...). Test data: a withdrawal of a non-matching then matching type. Expected: only the matching vote type is removed. */
  @Test
  public void testWithdrawVoteRemovesOnlyMatchingType() {
    // User 1000 has an upvote on reply 1; withdrawing a downvote must leave the upvote untouched.
    discussionReplyVoteMapper.withdrawVote(1, 1000, DiscussionReplyVote.VOTE_DOWN);
    Assertions.assertEquals(
        DiscussionReplyVote.VOTE_UP, discussionReplyVoteMapper.getVoteType(1, 1000).intValue());
    // Withdrawing the upvote removes it.
    discussionReplyVoteMapper.withdrawVote(1, 1000, DiscussionReplyVote.VOTE_UP);
    Assertions.assertNull(discussionReplyVoteMapper.getVoteType(1, 1000));
  }

  /** Test case: tests that a vote and a report on the same reply by the same user coexist. Expected: the upvote survives and the report is counted separately. */
  @Test
  public void testVoteAndReportCoexist() {
    // User 1000 already upvoted reply 1; the same user now also reports it.
    discussionReplyVoteMapper.castVote(new DiscussionReplyVote(1, 1000, DiscussionReplyVote.VOTE_REPORT));
    // The upvote is untouched, and getVoteType ignores the report row.
    Assertions.assertEquals(
        DiscussionReplyVote.VOTE_UP, discussionReplyVoteMapper.getVoteType(1, 1000).intValue());
    Assertions.assertEquals(
        1, discussionReplyVoteMapper.getNumberOfVotes(1, DiscussionReplyVote.VOTE_UP));
    Assertions.assertEquals(1, discussionReplyVoteMapper.getNumberOfReportsOfThread(1));
  }

  /** Test case: tests getNumberOfReportsOfThread(...). Test data: a thread with no then one report. Expected: the report count of the thread. */
  @Test
  public void testGetNumberOfReportsOfThread() {
    Assertions.assertEquals(0, discussionReplyVoteMapper.getNumberOfReportsOfThread(1));
    discussionReplyVoteMapper.castVote(new DiscussionReplyVote(1, 1002, DiscussionReplyVote.VOTE_REPORT));
    Assertions.assertEquals(1, discussionReplyVoteMapper.getNumberOfReportsOfThread(1));
  }

  /** The DiscussionReplyVoteMapper object under test. */
  @Autowired private DiscussionReplyVoteMapper discussionReplyVoteMapper;
}
