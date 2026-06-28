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
import java.util.Date;

/**
 * The model of a discussion reply.
 *
 * @author Haozhe Xie
 */
public class DiscussionReply implements Serializable {
  /** Default constructor of the discussion reply. */
  public DiscussionReply() {}

  /**
   * Constructor of the discussion reply.
   *
   * @param discussionThreadId - the unique identifier of the discussion thread
   * @param discussionReplyCreator - the creator of the discussion reply
   * @param discussionReplyContent - the content of the discussion reply
   */
  public DiscussionReply(
      long discussionThreadId, User discussionReplyCreator, String discussionReplyContent) {
    this.discussionThreadId = discussionThreadId;
    this.discussionReplyCreator = discussionReplyCreator;
    this.discussionReplyContent = discussionReplyContent;
  }

  /**
   * Constructor of the discussion reply.
   *
   * @param discussionReplyId - the unique identifier of the discussion reply
   * @param discussionThreadId - the unique identifier of the discussion thread
   * @param discussionReplyCreator - the creator of the discussion reply
   * @param discussionReplyContent - the content of the discussion reply
   */
  public DiscussionReply(
      long discussionReplyId,
      long discussionThreadId,
      User discussionReplyCreator,
      String discussionReplyContent) {
    this(discussionThreadId, discussionReplyCreator, discussionReplyContent);
    this.discussionReplyId = discussionReplyId;
  }

  /**
   * Gets the unique identifier of the discussion reply.
   *
   * @return the unique identifier of the discussion reply
   */
  public long getDiscussionReplyId() {
    return discussionReplyId;
  }

  /**
   * Sets the unique identifier of the discussion reply.
   *
   * @param discussionReplyId - the unique identifier of the discussion reply
   */
  public void setDiscussionReplyId(long discussionReplyId) {
    this.discussionReplyId = discussionReplyId;
  }

  /**
   * Gets the unique identifier of the discussion thread.
   *
   * @return the unique identifier of the discussion thread
   */
  public long getDiscussionThreadId() {
    return discussionThreadId;
  }

  /**
   * Sets the unique identifier of the discussion thread.
   *
   * @param discussionThreadId - the unique identifier of the discussion thread
   */
  public void setDiscussionThreadId(long discussionThreadId) {
    this.discussionThreadId = discussionThreadId;
  }

  /**
   * Gets the creator of the discussion reply.
   *
   * @return the creator of the discussion reply
   */
  public User getDiscussionReplyCreator() {
    return discussionReplyCreator;
  }

  /**
   * Sets the creator of the discussion reply.
   *
   * @param discussionReplyCreator - the creator of the discussion reply
   */
  public void setDiscussionReplyCreator(User discussionReplyCreator) {
    this.discussionReplyCreator = discussionReplyCreator;
  }

  /**
   * Gets the creation time of the discussion reply.
   *
   * @return the creation time of the discussion reply
   */
  public Date getDiscussionReplyCreateTime() {
    return discussionReplyCreateTime;
  }

  /**
   * Sets the creation time of the discussion reply.
   *
   * @param discussionReplyCreateTime - the creation time of the discussion reply
   */
  public void setDiscussionReplyCreateTime(Date discussionReplyCreateTime) {
    this.discussionReplyCreateTime = discussionReplyCreateTime;
  }

  /**
   * Gets the content of the discussion reply.
   *
   * @return the content of the discussion reply
   */
  public String getDiscussionReplyContent() {
    return discussionReplyContent;
  }

  /**
   * Sets the content of the discussion reply.
   *
   * @param discussionReplyContent - the content of the discussion reply
   */
  public void setDiscussionReplyContent(String discussionReplyContent) {
    this.discussionReplyContent = discussionReplyContent;
  }

  /**
   * Gets the number of upvotes of the discussion reply.
   *
   * @return the number of upvotes of the discussion reply
   */
  public int getNumberOfVoteUp() {
    return numberOfVoteUp;
  }

  /**
   * Sets the number of upvotes of the discussion reply.
   *
   * @param numberOfVoteUp - the number of upvotes of the discussion reply
   */
  public void setNumberOfVoteUp(int numberOfVoteUp) {
    this.numberOfVoteUp = numberOfVoteUp;
  }

  /**
   * Gets the number of downvotes of the discussion reply.
   *
   * @return the number of downvotes of the discussion reply
   */
  public int getNumberOfVoteDown() {
    return numberOfVoteDown;
  }

  /**
   * Sets the number of downvotes of the discussion reply.
   *
   * @param numberOfVoteDown - the number of downvotes of the discussion reply
   */
  public void setNumberOfVoteDown(int numberOfVoteDown) {
    this.numberOfVoteDown = numberOfVoteDown;
  }

  /**
   * Gets whether the current user has upvoted the discussion reply.
   *
   * @return whether the current user has upvoted the discussion reply
   */
  public boolean getIsVotedUp() {
    return isVotedUp;
  }

  /**
   * Sets whether the current user has upvoted the discussion reply.
   *
   * @param isVotedUp - whether the current user has upvoted the discussion reply
   */
  public void setIsVotedUp(boolean isVotedUp) {
    this.isVotedUp = isVotedUp;
  }

  /**
   * Gets whether the current user has downvoted the discussion reply.
   *
   * @return whether the current user has downvoted the discussion reply
   */
  public boolean getIsVotedDown() {
    return isVotedDown;
  }

  /**
   * Sets whether the current user has downvoted the discussion reply.
   *
   * @param isVotedDown - whether the current user has downvoted the discussion reply
   */
  public void setIsVotedDown(boolean isVotedDown) {
    this.isVotedDown = isVotedDown;
  }

  /**
   * Gets whether the discussion reply is visible to ordinary users.
   *
   * @return whether the discussion reply is visible
   */
  public boolean isVisible() {
    return visible;
  }

  /**
   * Sets whether the discussion reply is visible to ordinary users.
   *
   * @param visible - whether the discussion reply is visible
   */
  public void setVisible(boolean visible) {
    this.visible = visible;
  }

  /* (non-Javadoc)
   * @see java.lang.Object#toString()
   */
  public String toString() {
    return String.format(
        "DiscussionReply [ReplyID=%d, ThreadID=%d, Creator={%s}, CreateTime=%s]",
        new Object[] {
          discussionReplyId, discussionThreadId, discussionReplyCreator, discussionReplyCreateTime
        });
  }

  /** The unique identifier of the discussion reply. */
  private long discussionReplyId;

  /** The unique identifier of the discussion thread. */
  private long discussionThreadId;

  /** The creator of the discussion reply. */
  private User discussionReplyCreator;

  /** The creation time of the discussion reply. */
  private Date discussionReplyCreateTime;

  /** The content of the discussion reply. */
  private String discussionReplyContent;

  /** The number of upvotes of the discussion reply (a computed, non-persistent view value). */
  private int numberOfVoteUp;

  /** The number of downvotes of the discussion reply (a computed, non-persistent view value). */
  private int numberOfVoteDown;

  /**
   * Whether the current user has upvoted the discussion reply (a computed, non-persistent view
   * value).
   */
  private boolean isVotedUp;

  /**
   * Whether the current user has downvoted the discussion reply (a computed, non-persistent view
   * value).
   */
  private boolean isVotedDown;

  /** Whether the discussion reply is visible to ordinary users. */
  private boolean visible = true;

  /** The unique serialization identifier. */
  private static final long serialVersionUID = 3380540873295113998L;
}
