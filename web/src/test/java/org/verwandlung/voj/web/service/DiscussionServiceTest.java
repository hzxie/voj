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

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

import org.verwandlung.voj.web.mapper.UserMapper;
import org.verwandlung.voj.web.model.DiscussionReply;
import org.verwandlung.voj.web.model.DiscussionThread;
import org.verwandlung.voj.web.model.DiscussionTopic;
import org.verwandlung.voj.web.model.User;

/**
 * The test class for DiscussionService.
 *
 * <p>Seed data: 4 discussion topics (all top-level), 3 discussion threads (threads 1/2 are related
 * to problem 1000), 3 replies. User 1000 is an administrator, 1001 is a judger, and 1002 is a
 * regular user.
 *
 * @author Haozhe Xie
 */
@ExtendWith(SpringExtension.class)
@Transactional
@ContextConfiguration({"classpath:test-spring-context.xml"})
public class DiscussionServiceTest {
  /** Test case: tests the getDiscussionTopics() method. Test data: N/a. Expected: all discussion topics. */
  @Test
  public void testGetDiscussionTopics() {
    List<DiscussionTopic> topics = discussionService.getDiscussionTopics();
    Assertions.assertEquals(4, topics.size());
  }

  /** Test case: tests the getDiscussionTopicsWithHierarchy() method. Test data: N/a. Expected: the top-level topics and their (empty) subtopic lists. */
  @Test
  public void testGetDiscussionTopicsWithHierarchy() {
    Map<DiscussionTopic, List<DiscussionTopic>> hierarchy =
        discussionService.getDiscussionTopicsWithHierarchy();
    Assertions.assertEquals(4, hierarchy.size());
    for (List<DiscussionTopic> children : hierarchy.values()) {
      Assertions.assertTrue(children.isEmpty());
    }
  }

  /** Test case: tests the getSolutionThreadOfProblem(long) method. Test data: a problem that has a solution. Expected: the corresponding solution discussion thread. */
  @Test
  public void testGetSolutionThreadOfProblemExists() {
    DiscussionThread thread = discussionService.getSolutionThreadOfProblem(1000);
    Assertions.assertNotNull(thread);
    Assertions.assertEquals(1, thread.getDiscussionThreadId());
  }

  /** Test case: tests the getSolutionThreadOfProblem(long) method. Test data: a problem with no solution. Expected: a null reference. */
  @Test
  public void testGetSolutionThreadOfProblemNotExists() {
    Assertions.assertNull(discussionService.getSolutionThreadOfProblem(99999));
  }

  /** Test case: tests the getDiscussionThreadsOfProblem(long, long, int) method. Test data: problem 1000. Expected: the discussion threads related to the problem. */
  @Test
  public void testGetDiscussionThreadsOfProblem() {
    List<DiscussionThread> threads = discussionService.getDiscussionThreadsOfProblem(1000, 0, 10);
    Assertions.assertEquals(2, threads.size());
  }

  /** Test case: tests the getDiscussionThreadsOfTopic(String, long, int) method. Test data: the solutions topic. Expected: the discussion threads under that topic. */
  @Test
  public void testGetDiscussionThreadsOfTopic() {
    List<DiscussionThread> threads = discussionService.getDiscussionThreadsOfTopic("solutions", 0, 10);
    Assertions.assertEquals(2, threads.size());
  }

  /** Test case: tests the getDiscussionThreadsOfTopic(String, long, int) method. Test data: an empty topic slug. Expected: all discussion threads. */
  @Test
  public void testGetDiscussionThreadsOfTopicWithEmptySlug() {
    List<DiscussionThread> threads = discussionService.getDiscussionThreadsOfTopic("", 0, 10);
    Assertions.assertEquals(3, threads.size());
  }

  /** Test case: tests the getDiscussionRepliesOfThread(...) method. Test data: discussion thread 2. Expected: all the replies of the thread. */
  @Test
  public void testGetDiscussionRepliesOfThread() {
    List<DiscussionReply> replies = discussionService.getDiscussionRepliesOfThread(2, 1000, 0, 10);
    Assertions.assertEquals(2, replies.size());
    // The vote information has been replaced with a JSON string of the statistics.
    Assertions.assertTrue(replies.get(0).getDiscussionReplyVotes().contains("numberOfVoteUp"));
  }

  /** Test case: tests the getDiscussionThreadUsingThreadId(long) method. Test data: an existing / non-existing thread identifier. Expected: the thread or a null reference. */
  @Test
  public void testGetDiscussionThreadUsingThreadId() {
    Assertions.assertNotNull(discussionService.getDiscussionThreadUsingThreadId(1));
    Assertions.assertNull(discussionService.getDiscussionThreadUsingThreadId(99999));
  }

  /** Test case: tests the getDiscussionReplyUsingReplyId(long) method. Test data: an existing / non-existing reply identifier. Expected: the reply or a null reference. */
  @Test
  public void testGetDiscussionReplyUsingReplyId() {
    Assertions.assertNotNull(discussionService.getDiscussionReplyUsingReplyId(1));
    Assertions.assertNull(discussionService.getDiscussionReplyUsingReplyId(99999));
  }

  /** Test case: tests the voteDiscussionReply(...) method. Test data: a valid upvote operation. Expected: the vote succeeds and is persisted. */
  @Test
  public void testVoteDiscussionReplySuccessfully() {
    // Reply 3 belongs to thread 2 and initially has no votes. User 1002 upvotes it.
    Map<String, Boolean> result =
        discussionService.voteDiscussionReply(2, 3, userWithUid(1002), 1, 0);
    Assertions.assertTrue(result.get("isSuccessful"));

    DiscussionReply reply = discussionService.getDiscussionReplyUsingReplyId(3);
    Assertions.assertTrue(reply.getDiscussionReplyVotes().contains("1002"));
  }

  /** Test case: tests the voteDiscussionReply(...) method. Test data: the reply and the thread do not match. Expected: the vote fails. */
  @Test
  public void testVoteDiscussionReplyWithMismatchedThread() {
    // Reply 1 belongs to thread 1, but thread 2 is passed in here.
    Map<String, Boolean> result =
        discussionService.voteDiscussionReply(2, 1, userWithUid(1002), 1, 0);
    Assertions.assertFalse(result.get("isDiscussionReplyExists"));
    Assertions.assertFalse(result.get("isSuccessful"));
  }

  /** Test case: tests the voteDiscussionReply(...) method. Test data: an illegal vote value. Expected: the vote fails. */
  @Test
  public void testVoteDiscussionReplyWithInvalidVote() {
    Map<String, Boolean> result =
        discussionService.voteDiscussionReply(2, 3, userWithUid(1002), 2, 0);
    Assertions.assertFalse(result.get("isVoteValid"));
    Assertions.assertFalse(result.get("isSuccessful"));
  }

  /** Test case: tests the voteDiscussionReply(...) method. Test data: the user is not logged in. Expected: the vote fails. */
  @Test
  public void testVoteDiscussionReplyWithoutLogin() {
    Map<String, Boolean> result = discussionService.voteDiscussionReply(2, 3, null, 1, 0);
    Assertions.assertFalse(result.get("isLoggedIn"));
    Assertions.assertFalse(result.get("isSuccessful"));
  }

  /** Test case: tests the createDiscussionTopic(...) method. Test data: a valid topic. Expected: creation succeeds and the number of topics increases by one. */
  @Test
  public void testCreateDiscussionTopicSuccessfully() {
    Map<String, Boolean> result =
        discussionService.createDiscussionTopic("feedback", "Feedback", null);
    Assertions.assertTrue(result.get("isSuccessful"));
    Assertions.assertEquals(5, discussionService.getDiscussionTopics().size());
  }

  /** Test case: tests the createDiscussionTopic(...) method. Test data: the topic slug is empty. Expected: creation fails. */
  @Test
  public void testCreateDiscussionTopicWithEmptySlug() {
    Map<String, Boolean> result = discussionService.createDiscussionTopic("", "Feedback", null);
    Assertions.assertTrue(result.get("isDiscussionTopicSlugEmpty"));
    Assertions.assertFalse(result.get("isSuccessful"));
  }

  /** Test case: tests the updateDiscussionTopic(...) method. Test data: an existing topic. Expected: the update succeeds. */
  @Test
  public void testUpdateDiscussionTopicSuccessfully() {
    Map<String, Boolean> result =
        discussionService.updateDiscussionTopic(3, "general", "General Discussion", null);
    Assertions.assertTrue(result.get("isSuccessful"));
  }

  /** Test case: tests the updateDiscussionTopic(...) method. Test data: a non-existing topic. Expected: the update fails. */
  @Test
  public void testUpdateDiscussionTopicNotExists() {
    Map<String, Boolean> result =
        discussionService.updateDiscussionTopic(999, "ghost", "Ghost", null);
    Assertions.assertFalse(result.get("isDiscussionTopicExists"));
    Assertions.assertFalse(result.get("isSuccessful"));
  }

  /** Test case: tests the deleteDiscussionTopic(int) method. Test data: a topic that exists and is not referenced. Expected: deletion succeeds. */
  @Test
  public void testDeleteDiscussionTopicSuccessfully() {
    // Topic 4 (support) is not referenced by any discussion thread.
    Map<String, Boolean> result = discussionService.deleteDiscussionTopic(4);
    Assertions.assertTrue(result.get("isSuccessful"));
    Assertions.assertEquals(3, discussionService.getDiscussionTopics().size());
  }

  /** Test case: tests the deleteDiscussionTopic(int) method. Test data: a non-existing topic. Expected: deletion fails. */
  @Test
  public void testDeleteDiscussionTopicNotExists() {
    Map<String, Boolean> result = discussionService.deleteDiscussionTopic(999);
    Assertions.assertFalse(result.get("isSuccessful"));
  }

  /** Test case: tests the createDiscussionThread(...) method. Test data: a valid discussion thread. Expected: creation succeeds and the thread identifier is returned. */
  @Test
  public void testCreateDiscussionThreadSuccessfully() {
    Map<String, Object> result =
        discussionService.createDiscussionThread(
            adminUser(), "qa", 0, "How to solve this?", "Any hints?");
    Assertions.assertTrue((Boolean) result.get("isSuccessful"));
    Assertions.assertTrue(result.containsKey("discussionThreadId"));
  }

  /** Test case: tests the createDiscussionThread(...) method. Test data: the title is empty. Expected: creation fails. */
  @Test
  public void testCreateDiscussionThreadWithEmptyTitle() {
    Map<String, Object> result =
        discussionService.createDiscussionThread(adminUser(), "qa", 0, "", "Content");
    Assertions.assertFalse((Boolean) result.get("isSuccessful"));
  }

  /** Test case: tests the createDiscussionThread(...) method. Test data: the topic does not exist. Expected: creation fails. */
  @Test
  public void testCreateDiscussionThreadWithNonExistentTopic() {
    Map<String, Object> result =
        discussionService.createDiscussionThread(
            adminUser(), "not-a-topic", 0, "Title", "Content");
    Assertions.assertFalse((Boolean) result.get("isDiscussionTopicExists"));
    Assertions.assertFalse((Boolean) result.get("isSuccessful"));
  }

  /** Test case: tests the editDiscussionThread(...) method. Test data: edited by the thread creator. Expected: editing succeeds. */
  @Test
  public void testEditDiscussionThreadSuccessfully() {
    Map<String, Boolean> result =
        discussionService.editDiscussionThread(1, adminUser(), "qa", "Edited Title");
    Assertions.assertTrue(result.get("isSuccessful"));
  }

  /** Test case: tests the editDiscussionThread(...) method. Test data: the thread does not exist. Expected: editing fails. */
  @Test
  public void testEditDiscussionThreadNotExists() {
    Map<String, Boolean> result =
        discussionService.editDiscussionThread(99999, adminUser(), "qa", "Title");
    Assertions.assertFalse(result.get("isDiscussionThreadExists"));
    Assertions.assertFalse(result.get("isSuccessful"));
  }

  /** Test case: tests the createDiscussionReply(...) method. Test data: a valid reply. Expected: creation succeeds. */
  @Test
  public void testCreateDiscussionReplySuccessfully() {
    Map<String, Object> result =
        discussionService.createDiscussionReply(1, adminUser(), "A helpful reply.");
    Assertions.assertTrue((Boolean) result.get("isSuccessful"));
    Assertions.assertTrue(result.containsKey("discussionReply"));
  }

  /** Test case: tests the createDiscussionReply(...) method. Test data: the reply content is empty. Expected: creation fails. */
  @Test
  public void testCreateDiscussionReplyWithEmptyContent() {
    Map<String, Object> result = discussionService.createDiscussionReply(1, adminUser(), "");
    Assertions.assertFalse((Boolean) result.get("isSuccessful"));
  }

  /** Test case: tests the createDiscussionReply(...) method. Test data: the discussion thread does not exist. Expected: creation fails. */
  @Test
  public void testCreateDiscussionReplyWithNonExistentThread() {
    Map<String, Object> result =
        discussionService.createDiscussionReply(99999, adminUser(), "Reply");
    Assertions.assertFalse((Boolean) result.get("isDiscussionThreadExists"));
    Assertions.assertFalse((Boolean) result.get("isSuccessful"));
  }

  /** Test case: tests the editDiscussionReply(...) method. Test data: edited by an administrator. Expected: editing succeeds. */
  @Test
  public void testEditDiscussionReplyByAdministrator() {
    // Reply 1 was created by user 1001; the administrator (user 1000) has permission to edit it.
    Map<String, Boolean> result =
        discussionService.editDiscussionReply(1, adminUser(), "Edited reply content.");
    Assertions.assertTrue(result.get("isSuccessful"));
  }

  /** Test case: tests the deleteDiscussionReply(...) method. Test data: deleted by an administrator. Expected: deletion succeeds. */
  @Test
  public void testDeleteDiscussionReplyByAdministrator() {
    Map<String, Boolean> result = discussionService.deleteDiscussionReply(3, adminUser());
    Assertions.assertTrue(result.get("isSuccessful"));
    Assertions.assertNull(discussionService.getDiscussionReplyUsingReplyId(3));
  }

  /** Test case: tests the deleteDiscussionReply(...) method. Test data: a regular user deletes another user's reply. Expected: deletion fails. */
  @Test
  public void testDeleteDiscussionReplyByUnauthorizedUser() {
    // Reply 1 was created by user 1001; regular user 1002 has no permission to delete it.
    Map<String, Boolean> result =
        discussionService.deleteDiscussionReply(1, userMapper.getUserUsingUid(1002));
    Assertions.assertFalse(result.get("isSuccessful"));
  }

  /** Returns the administrator user (user 1000). */
  private User adminUser() {
    return userMapper.getUserUsingUid(1000);
  }

  /** Constructs a User object with only the UID set, used as the voting user argument. */
  private User userWithUid(long uid) {
    User user = new User();
    user.setUid(uid);
    return user;
  }

  /** The DiscussionService object under test. */
  @Autowired private DiscussionService discussionService;

  /** Used to get real user objects with user group information. */
  @Autowired private UserMapper userMapper;
}
