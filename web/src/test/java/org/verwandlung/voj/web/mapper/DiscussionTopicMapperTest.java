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
import org.verwandlung.voj.web.model.DiscussionTopic;

import java.util.List;

/**
 * The test class for DiscussionTopicMapper.
 *
 * @author Haozhe Xie
 */
@ExtendWith(SpringExtension.class)
@Transactional
@ContextConfiguration({"classpath:test-spring-context.xml"})
public class DiscussionTopicMapperTest {
  /** Test case: tests the getDiscussionTopics() method. Test data: N/a. Expected: a list containing the DiscussionTopic objects. */
  @Test
  public void testGetDiscussionTopics() {
    List<DiscussionTopic> topics = discussionTopicMapper.getDiscussionTopics();
    Assertions.assertEquals(4, topics.size());

    DiscussionTopic firstTopic = topics.get(0);
    Assertions.assertEquals("solutions", firstTopic.getDiscussionTopicSlug());
  }

  /**
   * Test case: tests the getDiscussionTopicUsingId(int) method. Test data: the unique identifier of a
   * DiscussionTopic that exists in the database. Expected: the expected DiscussionTopic object.
   */
  @Test
  public void testGetDiscussionTopicUsingIdExists() {
    DiscussionTopic topic = discussionTopicMapper.getDiscussionTopicUsingId(1);
    Assertions.assertNotNull(topic);
    Assertions.assertEquals("solutions", topic.getDiscussionTopicSlug());
  }

  /** Test case: tests the getDiscussionTopicUsingId(int) method. Test data: the unique identifier of a DiscussionTopic that does not exist in the database. Expected: a null reference. */
  @Test
  public void testGetDiscussionTopicUsingIdNotExists() {
    DiscussionTopic topic = discussionTopicMapper.getDiscussionTopicUsingId(0);
    Assertions.assertNull(topic);
  }

  /**
   * Test case: tests the getDiscussionTopicUsingSlug(String) method. Test data: the slug of a
   * DiscussionTopic that exists in the database. Expected: the expected DiscussionTopic object.
   */
  @Test
  public void testGetDiscussionTopicUsingSlugExists() {
    DiscussionTopic topic = discussionTopicMapper.getDiscussionTopicUsingSlug("solutions");
    Assertions.assertNotNull(topic);
    Assertions.assertEquals(1, topic.getDiscussionTopicId());
  }

  /** Test case: tests the getDiscussionTopicUsingSlug(String) method. Test data: the slug of a DiscussionTopic that does not exist in the database. Expected: a null reference. */
  @Test
  public void testGetDiscussionTopicUsingSlugNotExists() {
    DiscussionTopic topic = discussionTopicMapper.getDiscussionTopicUsingSlug("NotExistingTopic");
    Assertions.assertNull(topic);
  }

  /**
   * Test case: tests the createDiscussionTopic(DiscussionTopic) method. Test data: a valid data set,
   * and no record exists for this slug. Expected: the DiscussionTopic is created successfully.
   */
  @Test
  public void testCreateDiscussionTopicNormally() {
    DiscussionTopic topic = new DiscussionTopic("new-topic", "New Topic", 0);
    int numberOfRowsAffected = discussionTopicMapper.createDiscussionTopic(topic);
    Assertions.assertEquals(1, numberOfRowsAffected);
  }

  /**
   * Test case: tests the createDiscussionTopic(DiscussionTopic) method. Test data: a valid data set,
   * but a record exists for this slug. Expected: a DuplicateKeyException is thrown.
   */
  @Test
  public void testCreateDiscussionTopicWithDupliateSlug() {
    DiscussionTopic topic = new DiscussionTopic("general", "New Topic", 0);
    Executable e =
        () -> {
          discussionTopicMapper.createDiscussionTopic(topic);
        };
    Assertions.assertThrows(org.springframework.dao.DuplicateKeyException.class, e);
  }

  /**
   * Test case: tests the createDiscussionTopic(DiscussionTopic) method. Test data: an invalid data
   * set, the slug is too long. Expected: a DataIntegrityViolationException is thrown.
   */
  @Test
  public void testCreateDiscussionTopicWithTooLongSlug() {
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < 31; ++i) {
      sb.append("Very");
    }
    DiscussionTopic topic = new DiscussionTopic(sb.toString() + "LongSlug", "New Topic", 0);
    Executable e =
        () -> {
          discussionTopicMapper.createDiscussionTopic(topic);
        };
    Assertions.assertThrows(org.springframework.dao.DataIntegrityViolationException.class, e);
  }

  /**
   * Test case: tests the updateDiscussionTopic(DiscussionTopic) method. Test data: a valid data set, and no record exists for this slug. Expected: the data update operation completes successfully.
   */
  @Test
  public void testUpdateDiscussionTopicNormally() {
    DiscussionTopic topic = discussionTopicMapper.getDiscussionTopicUsingSlug("general");
    Assertions.assertNotNull(topic);

    topic.setDiscussionTopicName("New Topic Name");
    int numberOfRowsAffected = discussionTopicMapper.updateDiscussionTopic(topic);
    Assertions.assertEquals(1, numberOfRowsAffected);

    topic = discussionTopicMapper.getDiscussionTopicUsingSlug("general");
    Assertions.assertEquals("New Topic Name", topic.getDiscussionTopicName());
  }

  /**
   * Test case: tests the updateDiscussionTopic(DiscussionTopic) method. Test data: a valid data set,
   * but a record exists for this slug. Expected: a DuplicateKeyException is thrown.
   */
  @Test
  public void testUpdateDiscussionTopicWithDupliateSlug() {
    DiscussionTopic topic = discussionTopicMapper.getDiscussionTopicUsingSlug("general");
    Assertions.assertNotNull(topic);

    topic.setDiscussionTopicSlug("support");
    Executable e =
        () -> {
          discussionTopicMapper.updateDiscussionTopic(topic);
        };
    Assertions.assertThrows(org.springframework.dao.DuplicateKeyException.class, e);
  }

  /** Test case: tests the deleteDiscussionTopicUsingId(int) method. Test data: the unique identifier of an existing DiscussionTopic. Expected: the data deletion operation completes successfully. */
  @Test
  public void testDeleteDiscussionTopicExists() {
    DiscussionTopic topic = discussionTopicMapper.getDiscussionTopicUsingId(1);
    Assertions.assertNotNull(topic);

    int numberOfRowsAffected = discussionTopicMapper.deleteDiscussionTopicUsingId(1);
    Assertions.assertEquals(1, numberOfRowsAffected);

    topic = discussionTopicMapper.getDiscussionTopicUsingId(1);
    Assertions.assertNull(topic);
  }

  /**
   * Test case: tests the deleteDiscussionTopicUsingId(int) method. Test data: the unique identifier of
   * a non-existing DiscussionTopic. Expected: the method executes normally without affecting the
   * data in the table.
   */
  @Test
  public void testDeleteDiscussionTopicNotExists() {
    DiscussionTopic topic = discussionTopicMapper.getDiscussionTopicUsingId(0);
    Assertions.assertNull(topic);

    int numberOfRowsAffected = discussionTopicMapper.deleteDiscussionTopicUsingId(0);
    Assertions.assertEquals(0, numberOfRowsAffected);
  }

  /** The DiscussionTopicMapper object under test. */
  @Autowired private DiscussionTopicMapper discussionTopicMapper;
}
