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
package org.verwandlung.voj.web.model;

import java.io.Serializable;

/**
 * The model of a discussion topic.
 *
 * @author Haozhe Xie
 */
public class DiscussionTopic implements Serializable {
  /** Default constructor of the discussion topic. */
  public DiscussionTopic() {}

  /**
   * Constructor of the discussion topic.
   *
   * @param discussionTopicSlug - the alias of the discussion topic
   * @param discussionTopicName - the name of the discussion topic
   * @param parentDiscussionTopicId - the unique identifier of the parent discussion topic
   */
  public DiscussionTopic(
      String discussionTopicSlug, String discussionTopicName, int parentDiscussionTopicId) {
    this.discussionTopicSlug = discussionTopicSlug;
    this.discussionTopicName = discussionTopicName;
    this.parentDiscussionTopicId = parentDiscussionTopicId;
  }

  /**
   * Constructor of the discussion topic.
   *
   * @param discussionTopicId - the unique identifier of the discussion topic
   * @param discussionTopicSlug - the alias of the discussion topic
   * @param discussionTopicName - the name of the discussion topic
   * @param parentDiscussionTopicId - the unique identifier of the parent discussion topic
   */
  public DiscussionTopic(
      int discussionTopicId,
      String discussionTopicSlug,
      String discussionTopicName,
      int parentDiscussionTopicId) {
    this(discussionTopicSlug, discussionTopicName, parentDiscussionTopicId);
    this.discussionTopicId = discussionTopicId;
  }

  /**
   * Gets the unique identifier of the discussion topic.
   *
   * @return the unique identifier of the discussion topic
   */
  public int getDiscussionTopicId() {
    return discussionTopicId;
  }

  /**
   * Sets the unique identifier of the discussion topic.
   *
   * @param discussionTopicId - the unique identifier of the discussion topic
   */
  public void setDiscussionTopicId(int discussionTopicId) {
    this.discussionTopicId = discussionTopicId;
  }

  /**
   * Gets the alias of the discussion topic.
   *
   * @return the alias of the discussion topic
   */
  public String getDiscussionTopicSlug() {
    return discussionTopicSlug;
  }

  /**
   * Sets the alias of the discussion topic.
   *
   * @param discussionTopicSlug - the alias of the discussion topic
   */
  public void setDiscussionTopicSlug(String discussionTopicSlug) {
    this.discussionTopicSlug = discussionTopicSlug;
  }

  /**
   * Gets the name of the discussion topic.
   *
   * @return the name of the discussion topic
   */
  public String getDiscussionTopicName() {
    return discussionTopicName;
  }

  /**
   * Sets the name of the discussion topic.
   *
   * @param getDiscussionTopicName - the name of the discussion topic
   */
  public void setDiscussionTopicName(String getDiscussionTopicName) {
    this.discussionTopicName = getDiscussionTopicName;
  }

  /**
   * Gets the unique identifier of the parent discussion topic.
   *
   * @return the unique identifier of the parent discussion topic
   */
  public int getParentDiscussionTopicId() {
    return parentDiscussionTopicId;
  }

  /**
   * Sets the unique identifier of the parent discussion topic.
   *
   * @param parentDiscussionTopicId - the unique identifier of the parent discussion topic
   */
  public void setParentDiscussionTopicId(int parentDiscussionTopicId) {
    this.parentDiscussionTopicId = parentDiscussionTopicId;
  }

  /* (non-Javadoc)
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    return String.format(
        "DiscussionTopic [TopicId=%d, Slug=%s, Name=%s, parentId=%d]",
        new Object[] {
          discussionTopicId, discussionTopicSlug, discussionTopicName, parentDiscussionTopicId
        });
  }

  /** The unique identifier of the discussion topic. */
  private int discussionTopicId;

  /** The alias of the discussion topic. */
  private String discussionTopicSlug;

  /** The name of the discussion topic. */
  private String discussionTopicName;

  /** The unique identifier of the parent discussion topic. */
  private int parentDiscussionTopicId;

  /** The unique serialization identifier. */
  private static final long serialVersionUID = 3905550118246821324L;
}
