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
 * The model of a single vote a user casts on a discussion reply. Each row represents one user's vote
 * (an upvote or a downvote) on one reply, replacing the legacy approach of storing all voters as a
 * JSON blob in the discussion reply itself.
 *
 * @author Haozhe Xie
 */
public class DiscussionReplyVote implements Serializable {
  /** The vote type representing an upvote. */
  public static final int VOTE_UP = 1;

  /** The vote type representing a downvote. */
  public static final int VOTE_DOWN = -1;

  /**
   * The vote type representing a report (an abuse flag). A report is stored as another row in the
   * same table but is independent of the up/down vote: a user may both have a vote and a report on
   * the same reply, which is why the vote type is part of the table's primary key.
   */
  public static final int VOTE_REPORT = 2;

  /** Default constructor of the discussion reply vote. */
  public DiscussionReplyVote() {}

  /**
   * Constructor of the discussion reply vote.
   *
   * @param discussionReplyId - the unique identifier of the discussion reply being voted on
   * @param voterUid - the unique identifier of the user casting the vote
   * @param voteType - the type of the vote ({@link #VOTE_UP} or {@link #VOTE_DOWN})
   */
  public DiscussionReplyVote(long discussionReplyId, long voterUid, int voteType) {
    this.discussionReplyId = discussionReplyId;
    this.voterUid = voterUid;
    this.voteType = voteType;
  }

  /**
   * Gets the unique identifier of the discussion reply being voted on.
   *
   * @return the unique identifier of the discussion reply being voted on
   */
  public long getDiscussionReplyId() {
    return discussionReplyId;
  }

  /**
   * Sets the unique identifier of the discussion reply being voted on.
   *
   * @param discussionReplyId - the unique identifier of the discussion reply being voted on
   */
  public void setDiscussionReplyId(long discussionReplyId) {
    this.discussionReplyId = discussionReplyId;
  }

  /**
   * Gets the unique identifier of the user casting the vote.
   *
   * @return the unique identifier of the user casting the vote
   */
  public long getVoterUid() {
    return voterUid;
  }

  /**
   * Sets the unique identifier of the user casting the vote.
   *
   * @param voterUid - the unique identifier of the user casting the vote
   */
  public void setVoterUid(long voterUid) {
    this.voterUid = voterUid;
  }

  /**
   * Gets the type of the vote ({@link #VOTE_UP} or {@link #VOTE_DOWN}).
   *
   * @return the type of the vote
   */
  public int getVoteType() {
    return voteType;
  }

  /**
   * Sets the type of the vote ({@link #VOTE_UP} or {@link #VOTE_DOWN}).
   *
   * @param voteType - the type of the vote
   */
  public void setVoteType(int voteType) {
    this.voteType = voteType;
  }

  /**
   * Gets the time when the vote was cast.
   *
   * @return the time when the vote was cast
   */
  public Date getVoteTime() {
    return voteTime;
  }

  /**
   * Sets the time when the vote was cast.
   *
   * @param voteTime - the time when the vote was cast
   */
  public void setVoteTime(Date voteTime) {
    this.voteTime = voteTime;
  }

  /* (non-Javadoc)
   * @see java.lang.Object#toString()
   */
  public String toString() {
    return String.format(
        "DiscussionReplyVote [ReplyID=%d, VoterUID=%d, VoteType=%d]",
        new Object[] {discussionReplyId, voterUid, voteType});
  }

  /** The unique identifier of the discussion reply being voted on. */
  private long discussionReplyId;

  /** The unique identifier of the user casting the vote. */
  private long voterUid;

  /** The type of the vote ({@link #VOTE_UP} or {@link #VOTE_DOWN}). */
  private int voteType;

  /** The time when the vote was cast. */
  private Date voteTime;

  /** The unique serialization identifier. */
  private static final long serialVersionUID = 6219531534295113721L;
}
