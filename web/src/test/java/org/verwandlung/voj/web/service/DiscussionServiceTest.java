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

import org.verwandlung.voj.web.mapper.OptionMapper;
import org.verwandlung.voj.web.mapper.UserMapper;
import org.verwandlung.voj.web.model.DiscussionReply;
import org.verwandlung.voj.web.model.DiscussionThread;
import org.verwandlung.voj.web.model.DiscussionTopic;
import org.verwandlung.voj.web.model.Option;
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

    // Problem 1000 owns threads #1 and #2 (thread #3 has no related problem); a
    // broken problem predicate could return a same-sized but wrong set of threads.
    Assertions.assertEquals(java.util.Set.of("Thread #1", "Thread #2"), threadTitlesOf(threads));
  }

  /** Test case: tests the getDiscussionThreadsOfTopic(String, long, int) method. Test data: the solutions topic. Expected: the discussion threads under that topic. */
  @Test
  public void testGetDiscussionThreadsOfTopic() {
    List<DiscussionThread> threads = discussionService.getDiscussionThreadsOfTopic("solutions", 0, 10);
    Assertions.assertEquals(2, threads.size());

    // The "solutions" slug must resolve to its topic and return that topic's
    // threads (#1 and #3), not another same-sized topic's threads.
    Assertions.assertEquals(java.util.Set.of("Thread #1", "Thread #3"), threadTitlesOf(threads));
  }

  /** Collects the titles of a list of threads, for order-independent membership assertions. */
  private java.util.Set<String> threadTitlesOf(List<DiscussionThread> threads) {
    java.util.Set<String> titles = new java.util.HashSet<>();
    for (DiscussionThread thread : threads) {
      titles.add(thread.getDiscussionThreadTitle());
    }
    return titles;
  }

  /** Test case: tests the getDiscussionThreadsOfTopic(String, long, int) method. Test data: an empty topic slug. Expected: all discussion threads. */
  @Test
  public void testGetDiscussionThreadsOfTopicWithEmptySlug() {
    List<DiscussionThread> threads = discussionService.getDiscussionThreadsOfTopic("", 0, 10);
    Assertions.assertEquals(3, threads.size());
  }

  /** Test case: tests the getDiscussionRepliesOfThread(...) method. Test data: discussion thread 2. Expected: all the replies of the thread with their vote statistics populated. */
  @Test
  public void testGetDiscussionRepliesOfThread() {
    List<DiscussionReply> replies = discussionService.getDiscussionRepliesOfThread(2, 1000, 0, 10, false);
    Assertions.assertEquals(2, replies.size());
    // Reply 2 has one upvote (from user 1000) and one downvote (from user 1001).
    DiscussionReply firstReply = replies.get(0);
    Assertions.assertEquals(1, firstReply.getNumberOfVoteUp());
    Assertions.assertEquals(1, firstReply.getNumberOfVoteDown());
    Assertions.assertTrue(firstReply.getIsVotedUp());
    Assertions.assertFalse(firstReply.getIsVotedDown());
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
    // Disable the reputation gate so a regular user without solved problems can vote.
    setOption("discussionMinSolvedToVote", "0");
    // Reply 3 belongs to thread 2 and initially has no votes. User 1002 upvotes it.
    Map<String, Boolean> result =
        discussionService.voteDiscussionReply(2, 3, userWithUid(1002), 1, 0);
    Assertions.assertTrue(result.get("isSuccessful"));

    DiscussionReply votedReply = findReplyById(2, 1002, 3);
    Assertions.assertNotNull(votedReply);
    Assertions.assertEquals(1, votedReply.getNumberOfVoteUp());
    Assertions.assertTrue(votedReply.getIsVotedUp());
  }

  /** Test case: tests the voteDiscussionReply(...) method. Test data: a user without enough solved problems. Expected: the vote is rejected by the reputation gate. */
  @Test
  public void testVoteDiscussionReplyBlockedByReputation() {
    // Require at least one solved problem; user 1002 has solved none.
    setOption("discussionMinSolvedToVote", "1");
    Map<String, Boolean> result =
        discussionService.voteDiscussionReply(2, 3, userWithUid(1002), 1, 0);
    Assertions.assertFalse(result.get("isReputationEnough"));
    Assertions.assertFalse(result.get("isSuccessful"));
  }

  /** Test case: tests the voteDiscussionReply(...) method. Test data: a user voting on their own reply. Expected: the vote is rejected. */
  @Test
  public void testVoteDiscussionReplyOnOwnReplyBlocked() {
    setOption("discussionMinSolvedToVote", "0");
    // Reply 3 of thread 2 was created by user 1001, who cannot vote on it.
    Map<String, Boolean> result =
        discussionService.voteDiscussionReply(2, 3, userWithUid(1001), 1, 0);
    Assertions.assertFalse(result.get("isNotSelfVote"));
    Assertions.assertFalse(result.get("isSuccessful"));
  }

  /** Test case: tests the reportDiscussionReply(...) method. Test data: a valid report that crosses the hide threshold. Expected: the report succeeds and the thread becomes hidden. */
  @Test
  public void testReportDiscussionReplySuccessfullyHidesThread() {
    setOption("discussionMinSolvedToReport", "0");
    setOption("discussionReportHideThreshold", "1");
    // Reply 1 of thread 1 was created by user 1001; user 1002 reports it.
    Map<String, Boolean> result =
        discussionService.reportDiscussionReply(1, 1, userWithUid(1002));
    Assertions.assertTrue(result.get("isSuccessful"));
    Assertions.assertTrue(discussionService.isDiscussionThreadHidden(1));
  }

  /** Test case: tests the reportDiscussionReply(...) method. Test data: a user reporting their own reply. Expected: the report is rejected. */
  @Test
  public void testReportDiscussionReplyOnOwnReplyBlocked() {
    setOption("discussionMinSolvedToReport", "0");
    // Reply 1 of thread 1 was created by user 1001, who cannot report it.
    Map<String, Boolean> result =
        discussionService.reportDiscussionReply(1, 1, userWithUid(1001));
    Assertions.assertFalse(result.get("isNotSelfReport"));
    Assertions.assertFalse(result.get("isSuccessful"));
  }

  /** Test case: tests the reportDiscussionReply(...) method. Test data: a user without enough solved problems. Expected: the report is rejected by the reputation gate. */
  @Test
  public void testReportDiscussionReplyBlockedByReputation() {
    setOption("discussionMinSolvedToReport", "1");
    Map<String, Boolean> result =
        discussionService.reportDiscussionReply(1, 1, userWithUid(1002));
    Assertions.assertFalse(result.get("isReputationEnough"));
    Assertions.assertFalse(result.get("isSuccessful"));
  }

  /** Test case: tests the voteDiscussionReply(...) method. Test data: an upvote that is switched to a downvote. Expected: the upvote is replaced by a single downvote. */
  @Test
  public void testVoteDiscussionReplySwitchUpToDown() {
    setOption("discussionMinSolvedToVote", "0");
    discussionService.voteDiscussionReply(2, 3, userWithUid(1002), 1, 0);
    discussionService.voteDiscussionReply(2, 3, userWithUid(1002), -1, 1);
    DiscussionReply reply = findReplyById(2, 1002, 3);
    Assertions.assertEquals(0, reply.getNumberOfVoteUp());
    Assertions.assertEquals(1, reply.getNumberOfVoteDown());
    Assertions.assertFalse(reply.getIsVotedUp());
    Assertions.assertTrue(reply.getIsVotedDown());
  }

  /** Test case: tests the voteDiscussionReply(...) method. Test data: an upvote that is then cancelled. Expected: the reply has no votes left. */
  @Test
  public void testVoteDiscussionReplyCancel() {
    setOption("discussionMinSolvedToVote", "0");
    discussionService.voteDiscussionReply(2, 3, userWithUid(1002), 1, 0);
    discussionService.voteDiscussionReply(2, 3, userWithUid(1002), -1, 0);
    DiscussionReply reply = findReplyById(2, 1002, 3);
    Assertions.assertEquals(0, reply.getNumberOfVoteUp());
    Assertions.assertFalse(reply.getIsVotedUp());
  }

  /** Test case: tests that a vote and a report by the same user on the same reply are independent. Expected: reporting does not erase the existing vote. */
  @Test
  public void testVoteAndReportCoexist() {
    setOption("discussionMinSolvedToVote", "0");
    setOption("discussionMinSolvedToReport", "0");
    // Reply 3 of thread 2 was created by user 1001; user 1002 upvotes and then reports it.
    Assertions.assertTrue(
        discussionService.voteDiscussionReply(2, 3, userWithUid(1002), 1, 0).get("isSuccessful"));
    Assertions.assertTrue(
        discussionService.reportDiscussionReply(2, 3, userWithUid(1002)).get("isSuccessful"));
    DiscussionReply reply = findReplyById(2, 1002, 3);
    Assertions.assertEquals(1, reply.getNumberOfVoteUp());
    Assertions.assertTrue(reply.getIsVotedUp());
  }

  /** Test case: tests the reportDiscussionReply(...) method. Test data: the same reply reported twice. Expected: the report counts only once. */
  @Test
  public void testReportDiscussionReplyIsIdempotent() {
    setOption("discussionMinSolvedToReport", "0");
    discussionService.reportDiscussionReply(2, 3, userWithUid(1002));
    discussionService.reportDiscussionReply(2, 3, userWithUid(1002));
    // With only one effective report, a threshold of two does not hide the thread.
    setOption("discussionReportHideThreshold", "2");
    Assertions.assertFalse(discussionService.isDiscussionThreadHidden(2));
  }

  /** Test case: tests that administrators bypass the reputation gate. Test data: an admin with the gate set very high. Expected: the vote still succeeds. */
  @Test
  public void testAdministratorBypassesReputationGate() {
    setOption("discussionMinSolvedToVote", "99");
    // Admin user 1000 votes on reply 3 (created by user 1001).
    Map<String, Boolean> result = discussionService.voteDiscussionReply(2, 3, adminUser(), 1, 0);
    Assertions.assertTrue(result.get("isReputationEnough"));
    Assertions.assertTrue(result.get("isSuccessful"));
  }

  /** Test case: tests that a thread whose reports reach the threshold is excluded from the listing. Expected: the hidden thread is not returned. */
  @Test
  public void testHiddenThreadExcludedFromList() {
    setOption("discussionMinSolvedToReport", "0");
    setOption("discussionReportHideThreshold", "1");
    // Problem 1000 has threads 1 and 2; reporting a reply of thread 1 hides that thread.
    discussionService.reportDiscussionReply(1, 1, userWithUid(1002));
    List<DiscussionThread> threads = discussionService.getDiscussionThreadsOfProblem(1000, 0, 10);
    Assertions.assertEquals(1, threads.size());
    Assertions.assertEquals(2, threads.get(0).getDiscussionThreadId());
  }

  /** Test case: tests that replies are ordered with the original post pinned first, then by net votes descending. Expected: OP first, the higher-voted reply next. */
  @Test
  public void testGetDiscussionRepliesOrderedByVotes() {
    setOption("discussionMinSolvedToVote", "0");
    // Thread 2's original post is reply 2; reply 3 already exists with no votes. Add a third reply
    // and upvote it so it outranks reply 3 among the non-original replies.
    DiscussionReply newReply =
        (DiscussionReply)
            discussionService
                .createDiscussionReply(2, adminUser(), "Highly voted reply")
                .get("discussionReply");
    long newReplyId = newReply.getDiscussionReplyId();
    discussionService.voteDiscussionReply(2, newReplyId, userWithUid(1002), 1, 0);

    List<DiscussionReply> replies = discussionService.getDiscussionRepliesOfThread(2, -1, 0, 10, false);
    Assertions.assertEquals(3, replies.size());
    Assertions.assertEquals(2, replies.get(0).getDiscussionReplyId());
    Assertions.assertEquals(newReplyId, replies.get(1).getDiscussionReplyId());
    Assertions.assertEquals(3, replies.get(2).getDiscussionReplyId());
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

  /** Sets a system option value (rolled back after each test by the transactional context). */
  private void setOption(String optionName, String optionValue) {
    Option option = optionMapper.getOption(optionName);
    option.setOptionValue(optionValue);
    optionMapper.updateOption(option);
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

  /**
   * Fetches the replies of a thread (as seen by the given user) and returns the one with the given
   * reply identifier, or a null reference if it is not found.
   */
  private DiscussionReply findReplyById(long threadId, long currentUserUid, long replyId) {
    List<DiscussionReply> replies =
        discussionService.getDiscussionRepliesOfThread(threadId, currentUserUid, 0, 50, false);
    for (DiscussionReply reply : replies) {
      if (reply.getDiscussionReplyId() == replyId) {
        return reply;
      }
    }
    return null;
  }

  /** The DiscussionService object under test. */
  @Autowired private DiscussionService discussionService;

  /** Used to get real user objects with user group information. */
  @Autowired private UserMapper userMapper;

  /** Used to adjust the configurable discussion thresholds within a test. */
  @Autowired private OptionMapper optionMapper;
}
