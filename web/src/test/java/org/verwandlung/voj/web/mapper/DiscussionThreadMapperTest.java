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
import org.verwandlung.voj.web.model.DiscussionThread;
import org.verwandlung.voj.web.model.DiscussionTopic;
import org.verwandlung.voj.web.model.Problem;
import org.verwandlung.voj.web.model.User;

import java.util.List;

/**
 * The test class for DiscussionThreadMapper.
 *
 * @author Haozhe Xie
 */
@ExtendWith(SpringExtension.class)
@Transactional
@ContextConfiguration({"classpath:test-spring-context.xml"})
public class DiscussionThreadMapperTest {
  /**
   * Test case: tests the getDiscussionThreads(long, int, long, int) method. Test data: Problem: A+B
   * Problem, DiscussionTopic: null, get the first 2 records. Expected: the 2 most recently created
   * DiscussionThread objects.
   */
  @Test
  public void testGetDiscussionThreadsOfProblem1000WithOffsetFrom0WithLimit2() {
    List<DiscussionThread> discussionThreads =
        discussionThreadMapper.getDiscussionThreads(1000, 0, 0, 2);
    Assertions.assertEquals(2, discussionThreads.size());

    DiscussionThread firstThread = discussionThreads.get(0);
    Assertions.assertNotNull(firstThread);

    long problemId = firstThread.getProblem().getProblemId();
    Assertions.assertEquals(1000, problemId);

    String firstThreadTitle = firstThread.getDiscussionThreadTitle();
    Assertions.assertEquals("Thread #2", firstThreadTitle);

    long threadReplies = firstThread.getNumberOfReplies();
    Assertions.assertEquals(2, threadReplies);
  }

  /**
   * Test case: tests the getDiscussionThreads(long, int, long, int) method. Test data: Problem: null,
   * DiscussionTopic: General, get the first record. Expected: the most recently created
   * DiscussionThread object.
   */
  @Test
  public void testGetDiscussionThreadsOfNoRelatedProblemWithOffsetFrom0WithLimit1() {
    List<DiscussionThread> discussionThreads =
        discussionThreadMapper.getDiscussionThreads(0, 1, 0, 1);
    DiscussionThread thread = discussionThreads.get(0);
    Assertions.assertNotNull(thread);

    String threadTitle = thread.getDiscussionThreadTitle();
    Assertions.assertEquals("Thread #1", threadTitle);
  }

  /**
   * Test case: tests the getSolutionThreadOfProblem(long) method. Test data: Problem: A+B Problem.
   * Expected: the first discussion related to Problem A+B Problem.
   */
  @Test
  public void testGetSolutionThreadOfProblemUsingExistingProblemId() {
    DiscussionThread thread = discussionThreadMapper.getSolutionThreadOfProblem(1000);
    String threadTitle = thread.getDiscussionThreadTitle();
    Assertions.assertEquals("Thread #1", threadTitle);
  }

  /**
   * Test case: tests the getDiscussionThreadUsingThreadId(long) method. Test data: an existing
   * discussion thread unique identifier. Expected: the DiscussionThread object with the title
   * "Thread #2".
   */
  @Test
  public void testGetDiscussionThreadUsingThreadIdExists() {
    DiscussionThread thread = discussionThreadMapper.getDiscussionThreadUsingThreadId(2);
    Assertions.assertNotNull(thread);

    String threadTitle = thread.getDiscussionThreadTitle();
    Assertions.assertEquals("Thread #2", threadTitle);

    long numberOfReplies = thread.getNumberOfReplies();
    Assertions.assertEquals(2, numberOfReplies);
  }

  /**
   * Test case: tests the getDiscussionThreadUsingThreadId(long) method. Test data: a non-existing discussion thread unique identifier. Expected: a null DiscussionThread reference.
   */
  @Test
  public void testGetDiscussionThreadUsingThreadIdNotExists() {
    DiscussionThread thread = discussionThreadMapper.getDiscussionThreadUsingThreadId(0);
    Assertions.assertNull(thread);
  }

  /**
   * Test case: tests the createDiscussionThread(DiscussionThread) method. Test data: a valid data set
   * (Problem: A+B Problem). Expected: the data insertion operation completes successfully.
   */
  @Test
  public void testCreateDiscussionThreadNormally() {
    User creator = userMapper.getUserUsingUid(1000);
    DiscussionTopic topic = discussionTopicMapper.getDiscussionTopicUsingId(1);
    Problem problem = problemMapper.getProblem(1000);

    Assertions.assertNotNull(creator);
    Assertions.assertNotNull(problem);

    DiscussionThread thread = new DiscussionThread(creator, topic, problem, "title");
    int numberOfRowsAffected = discussionThreadMapper.createDiscussionThread(thread);
    Assertions.assertEquals(1, numberOfRowsAffected);
  }

  /**
   * Test case: tests the createDiscussionThread(DiscussionThread) method. Test data: a valid data set
   * (Problem: null). Expected: the data insertion operation completes successfully.
   */
  @Test
  public void testCreateDiscussionThreadWithNullProblem() {
    User creator = userMapper.getUserUsingUid(1000);
    DiscussionTopic topic = discussionTopicMapper.getDiscussionTopicUsingId(2);
    Assertions.assertNotNull(creator);

    DiscussionThread thread = new DiscussionThread(creator, topic, null, "title");
    int numberOfRowsAffected = discussionThreadMapper.createDiscussionThread(thread);
    Assertions.assertEquals(1, numberOfRowsAffected);
  }

  /**
   * Test case: tests the createDiscussionThread(DiscussionThread) method. Test data: a title that is
   * too long. Expected: a DataIntegrityViolationException is thrown.
   */
  @Test
  public void testCreateDiscussionThreadWithTooLongTitle() {
    User creator = userMapper.getUserUsingUid(1000);
    DiscussionTopic topic = discussionTopicMapper.getDiscussionTopicUsingId(1);
    Assertions.assertNotNull(creator);

    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < 31; ++i) {
      sb.append("Very");
    }
    DiscussionThread thread =
        new DiscussionThread(creator, topic, null, sb.toString() + "LongTitle");
    Executable e =
        () -> {
          discussionThreadMapper.createDiscussionThread(thread);
        };
    Assertions.assertThrows(org.springframework.dao.DataIntegrityViolationException.class, e);
  }

  /**
   * Test case: tests the createDiscussionThread(DiscussionThread) method. Test data: a non-existing
   * user. Expected: a DataIntegrityViolationException is thrown.
   */
  @Test
  public void testCreateDiscussionThreadWithNotExistingUser() {
    User creator = new User();
    creator.setUid(0);
    DiscussionTopic topic = discussionTopicMapper.getDiscussionTopicUsingId(1);

    DiscussionThread thread = new DiscussionThread(creator, topic, null, "title");
    Executable e =
        () -> {
          discussionThreadMapper.createDiscussionThread(thread);
        };
    Assertions.assertThrows(org.springframework.dao.DataIntegrityViolationException.class, e);
  }

  /**
   * Test case: tests the createDiscussionThread(DiscussionThread) method. Test data: a non-existing
   * discussion topic. Expected: a DataIntegrityViolationException is thrown.
   */
  @Test
  public void testCreateDiscussionThreadWithNotExistingThreadTopic() {
    User creator = userMapper.getUserUsingUid(1000);
    DiscussionTopic topic = new DiscussionTopic();
    topic.setDiscussionTopicId(0);

    DiscussionThread thread = new DiscussionThread(creator, topic, null, "title");
    Executable e =
        () -> {
          discussionThreadMapper.createDiscussionThread(thread);
        };
    Assertions.assertThrows(org.springframework.dao.DataIntegrityViolationException.class, e);
  }

  /**
   * Test case: tests the updateDiscussionThread(DiscussionThread) method. Test data: update a
   * DiscussionThread object with normal data. Expected: the data update operation completes
   * successfully.
   */
  @Test
  public void testUpdateDiscussionThreadNormally() {
    DiscussionThread thread = discussionThreadMapper.getDiscussionThreadUsingThreadId(1);
    Assertions.assertNotNull(thread);
    Assertions.assertEquals("Thread #1", thread.getDiscussionThreadTitle());

    thread.setDiscussionThreadTitle("New Thread Title");
    int numberOfRowsAffected = discussionThreadMapper.updateDiscussionThread(thread);
    Assertions.assertEquals(1, numberOfRowsAffected);

    thread = discussionThreadMapper.getDiscussionThreadUsingThreadId(1);
    Assertions.assertNotNull(thread);
    Assertions.assertEquals("New Thread Title", thread.getDiscussionThreadTitle());
  }

  /**
   * Test case: tests the updateDiscussionThread(DiscussionThread) method. Test data: a title that is
   * too long. Expected: a DataIntegrityViolationException is thrown.
   */
  @Test
  public void testUpdateDiscussionThreadWithTooLongTitle() {
    DiscussionThread thread = discussionThreadMapper.getDiscussionThreadUsingThreadId(1);
    Assertions.assertNotNull(thread);
    Assertions.assertEquals("Thread #1", thread.getDiscussionThreadTitle());

    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < 31; ++i) {
      sb.append("Very");
    }
    thread.setDiscussionThreadTitle(sb.toString() + "Long Title");
    Executable e =
        () -> {
          discussionThreadMapper.updateDiscussionThread(thread);
        };
    Assertions.assertThrows(org.springframework.dao.DataIntegrityViolationException.class, e);
  }

  /** Test case: tests the deleteDiscussionThreadUsingThreadId(long) method. Test data: an existing discussion thread unique identifier. Expected: the data deletion operation completes successfully. */
  @Test
  public void testDeleteDiscussionThreadUsingExistingThreadId() {
    DiscussionThread thread = discussionThreadMapper.getDiscussionThreadUsingThreadId(1);
    Assertions.assertNotNull(thread);

    int numberOfRowsAffected = discussionThreadMapper.deleteDiscussionThreadUsingThreadId(1);
    Assertions.assertEquals(1, numberOfRowsAffected);

    thread = discussionThreadMapper.getDiscussionThreadUsingThreadId(1);
    Assertions.assertNull(thread);
  }

  /**
   * Test case: tests the deleteDiscussionThreadUsingThreadId(long) method. Test data: a non-existing discussion thread unique identifier. Expected: the method executes normally without affecting the data in the table.
   */
  @Test
  public void testDeleteDiscussionThreadUsingNotExistingThreadId() {
    DiscussionThread thread = discussionThreadMapper.getDiscussionThreadUsingThreadId(0);
    Assertions.assertNull(thread);

    int numberOfRowsAffected = discussionThreadMapper.deleteDiscussionThreadUsingThreadId(0);
    Assertions.assertEquals(0, numberOfRowsAffected);
  }

  /** The DiscussionThreadMapper object under test. */
  @Autowired private DiscussionThreadMapper discussionThreadMapper;

  /** The UserMapper object used to build test cases. */
  @Autowired private UserMapper userMapper;

  /** The DiscussionTopicMapper object used to build test cases. */
  @Autowired private DiscussionTopicMapper discussionTopicMapper;

  /** The ProblemMapper object used to build test cases. */
  @Autowired private ProblemMapper problemMapper;
}
