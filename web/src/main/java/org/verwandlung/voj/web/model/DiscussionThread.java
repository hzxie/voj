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
 * The model of a discussion thread.
 *
 * @author Haozhe Xie
 */
public class DiscussionThread implements Serializable {
  /** Default constructor of the discussion thread. */
  public DiscussionThread() {}

  /**
   * Constructor of the discussion thread.
   *
   * @param discussionThreadCreator - the user who created the discussion thread
   * @param problem - the problem the discussion thread corresponds to
   * @param discussionTopic - the topic the discussion thread belongs to
   * @param discussionThreadTitle - the title of the discussion thread
   */
  public DiscussionThread(
      User discussionThreadCreator,
      DiscussionTopic discussionTopic,
      Problem problem,
      String discussionThreadTitle) {
    this.discussionThreadCreator = discussionThreadCreator;
    this.discussionTopic = discussionTopic;
    this.problem = problem;
    this.discussionThreadTitle = discussionThreadTitle;
  }

  /**
   * Constructor of the discussion thread.
   *
   * @param discussionThreadId - the unique identifier of the discussion thread
   * @param discussionThreadCreator - the user who created the discussion thread
   * @param problem - the problem the discussion thread corresponds to
   * @param discussionTopic - the topic the discussion thread belongs to
   * @param discussionThreadTitle - the title of the discussion thread
   */
  public DiscussionThread(
      long discussionThreadId,
      User discussionThreadCreator,
      Problem problem,
      DiscussionTopic discussionTopic,
      String discussionThreadTitle) {
    this(discussionThreadCreator, discussionTopic, problem, discussionThreadTitle);
    this.discussionThreadId = discussionThreadId;
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
   * Gets the user who created the discussion thread.
   *
   * @return the user who created the discussion thread
   */
  public User getDiscussionThreadCreator() {
    return discussionThreadCreator;
  }

  /**
   * Sets the user who created the discussion thread.
   *
   * @param discussionThreadCreator - the user who created the discussion thread
   */
  public void setDiscussionThreadCreator(User discussionThreadCreator) {
    this.discussionThreadCreator = discussionThreadCreator;
  }

  /**
   * Gets the creation time of the discussion thread.
   *
   * @return the creation time of the discussion thread
   */
  public Date getDiscussionThreadCreateTime() {
    return discussionThreadCreateTime;
  }

  /**
   * Sets the creation time of the discussion thread.
   *
   * @param discussionThreadCreateTime - the creation time of the discussion thread
   */
  public void setDiscussionThreadCreateTime(Date discussionThreadCreateTime) {
    this.discussionThreadCreateTime = discussionThreadCreateTime;
  }

  /**
   * Gets the problem the discussion thread corresponds to.
   *
   * @return the problem the discussion thread corresponds to
   */
  public Problem getProblem() {
    return problem;
  }

  /**
   * Sets the problem the discussion thread corresponds to.
   *
   * @param problem - the problem the discussion thread corresponds to
   */
  public void setProblem(Problem problem) {
    this.problem = problem;
  }

  /**
   * Gets the topic the discussion thread belongs to.
   *
   * @return the topic the discussion thread belongs to
   */
  public DiscussionTopic getDiscussionTopic() {
    return discussionTopic;
  }

  /**
   * Sets the topic the discussion thread belongs to.
   *
   * @param discussionTopic - the topic the discussion thread belongs to
   */
  public void setDiscussionTopic(DiscussionTopic discussionTopic) {
    this.discussionTopic = discussionTopic;
  }

  /**
   * Gets the title of the discussion thread.
   *
   * @return the title of the discussion thread
   */
  public String getDiscussionThreadTitle() {
    return discussionThreadTitle;
  }

  /**
   * Sets the title of the discussion thread.
   *
   * @param discussionThreadTitle - the title of the discussion thread
   */
  public void setDiscussionThreadTitle(String discussionThreadTitle) {
    this.discussionThreadTitle = discussionThreadTitle;
  }

  /**
   * Gets the latest discussion reply.
   *
   * @return the latest discussion reply
   */
  public DiscussionReply getLatestDiscussionReply() {
    return latestDiscussionReply;
  }

  /**
   * Sets the latest discussion reply.
   *
   * @param latestDiscussionReply - the latest discussion reply
   */
  public void setLatestDiscussionReply(DiscussionReply latestDiscussionReply) {
    this.latestDiscussionReply = latestDiscussionReply;
  }

  /**
   * Gets the number of replies of the discussion thread.
   *
   * @return the number of replies of the discussion thread
   */
  public long getNumberOfReplies() {
    return numberOfReplies;
  }

  /**
   * Sets the number of replies of the discussion thread.
   *
   * @param numberOfReplies - the number of replies of the discussion thread
   */
  public void setNumberOfReplies(long numberOfReplies) {
    this.numberOfReplies = numberOfReplies;
  }

  /**
   * Gets the number of reports filed against replies in the discussion thread.
   *
   * @return the number of reports of the discussion thread
   */
  public long getNumberOfReports() {
    return numberOfReports;
  }

  /**
   * Sets the number of reports filed against replies in the discussion thread.
   *
   * @param numberOfReports - the number of reports of the discussion thread
   */
  public void setNumberOfReports(long numberOfReports) {
    this.numberOfReports = numberOfReports;
  }

  /**
   * Gets whether the discussion thread is visible to ordinary users.
   *
   * @return whether the discussion thread is visible
   */
  public boolean isVisible() {
    return visible;
  }

  /**
   * Sets whether the discussion thread is visible to ordinary users.
   *
   * @param visible - whether the discussion thread is visible
   */
  public void setVisible(boolean visible) {
    this.visible = visible;
  }

  /**
   * Gets whether the discussion thread is pinned to the top of its list.
   *
   * @return whether the discussion thread is pinned
   */
  public boolean isPinned() {
    return pinned;
  }

  /**
   * Sets whether the discussion thread is pinned to the top of its list.
   *
   * @param pinned - whether the discussion thread is pinned
   */
  public void setPinned(boolean pinned) {
    this.pinned = pinned;
  }

  /**
   * Gets whether the discussion thread is locked (no new replies allowed).
   *
   * @return whether the discussion thread is locked
   */
  public boolean isLocked() {
    return locked;
  }

  /**
   * Sets whether the discussion thread is locked (no new replies allowed).
   *
   * @param locked - whether the discussion thread is locked
   */
  public void setLocked(boolean locked) {
    this.locked = locked;
  }

  public String toString() {
    return String.format(
        "DiscussionThread [ThreadID=%d, Creator={%s}, Problem={%s}, Title=%s]",
        new Object[] {discussionThreadId, discussionThreadCreator, problem, discussionThreadTitle});
  }

  /** The unique identifier of the discussion thread. */
  private long discussionThreadId;

  /** The user who created the discussion thread. */
  private User discussionThreadCreator;

  /** The creation time of the discussion thread. */
  private Date discussionThreadCreateTime;

  /** The problem the discussion thread corresponds to. */
  private Problem problem;

  /** The topic the discussion thread belongs to. */
  private DiscussionTopic discussionTopic;

  /** The title of the discussion thread. */
  private String discussionThreadTitle;

  /** The latest reply of the discussion thread. */
  private DiscussionReply latestDiscussionReply;

  /** The number of replies of the discussion thread. */
  private long numberOfReplies;

  /** The number of reports filed against replies in the discussion thread. */
  private long numberOfReports;

  /** Whether the discussion thread is visible to ordinary users. */
  private boolean visible = true;

  /** Whether the discussion thread is pinned to the top of its list. */
  private boolean pinned = false;

  /** Whether the discussion thread is locked (no new replies allowed). */
  private boolean locked = false;

  /** The unique serialization identifier. */
  private static final long serialVersionUID = -616554199104563280L;
}
