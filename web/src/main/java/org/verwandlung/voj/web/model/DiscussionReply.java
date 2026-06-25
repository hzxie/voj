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
   * @param discussionReplyVotes - the voting result of the discussion reply
   */
  public DiscussionReply(
      long discussionThreadId,
      User discussionReplyCreator,
      String discussionReplyContent,
      String discussionReplyVotes) {
    this.discussionThreadId = discussionThreadId;
    this.discussionReplyCreator = discussionReplyCreator;
    this.discussionReplyContent = discussionReplyContent;
    this.discussionReplyVotes = discussionReplyVotes;
  }

  /**
   * Constructor of the discussion reply.
   *
   * @param discussionReplyId - the unique identifier of the discussion reply
   * @param discussionThreadId - the unique identifier of the discussion thread
   * @param discussionReplyCreator - the creator of the discussion reply
   * @param discussionReplyContent - the content of the discussion reply
   * @param discussionReplyVote - the voting result of the discussion reply
   */
  public DiscussionReply(
      long discussionReplyId,
      long discussionThreadId,
      User discussionReplyCreator,
      String discussionReplyContent,
      String discussionReplyVote) {
    this(discussionThreadId, discussionReplyCreator, discussionReplyContent, discussionReplyVote);
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
   * Gets the voting result of the discussion reply.
   *
   * @return the voting result of the discussion reply
   */
  public String getDiscussionReplyVotes() {
    return discussionReplyVotes;
  }

  /**
   * Sets the voting result of the discussion reply.
   *
   * @param discussionReplyVote - the voting result of the discussion reply
   */
  public void setDiscussionReplyVotes(String discussionReplyVote) {
    this.discussionReplyVotes = discussionReplyVote;
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

  /** The voting result of the discussion reply. */
  private String discussionReplyVotes;

  /** The unique serialization identifier. */
  private static final long serialVersionUID = 3380540873295113998L;
}
