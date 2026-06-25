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

import org.verwandlung.voj.web.model.DiscussionTopic;

import java.util.List;

/**
 * DiscussionTopic Data Access Object.
 *
 * @author Haozhe Xie
 */
public interface DiscussionTopicMapper {
  /**
   * Gets all discussion topics.
   *
   * @return a List<DiscussionTopic> object containing all discussion topics
   */
  List<DiscussionTopic> getDiscussionTopics();

  /**
   * Gets a discussion topic object by the unique identifier of the discussion topic.
   *
   * @param discussionTopicId - the unique identifier of the discussion topic
   * @return a discussion topic object
   */
  DiscussionTopic getDiscussionTopicUsingId(int discussionTopicId);

  /**
   * Gets a discussion topic object by the unique alias of the discussion topic.
   *
   * @param discussionTopicSlug - the alias of the discussion topic
   * @return a discussion topic object
   */
  DiscussionTopic getDiscussionTopicUsingSlug(String discussionTopicSlug);

  /**
   * Creates a discussion topic.
   *
   * @param discussionTopic - the discussion topic object to create
   */
  int createDiscussionTopic(DiscussionTopic discussionTopic);

  /**
   * Updates a discussion topic.
   *
   * @param discussionTopic - the discussion topic object to update
   */
  int updateDiscussionTopic(DiscussionTopic discussionTopic);

  /**
   * Deletes a discussion topic.
   *
   * @param discussionTopicId - the unique identifier of the discussion topic to delete
   */
  int deleteDiscussionTopicUsingId(int discussionTopicId);
}
