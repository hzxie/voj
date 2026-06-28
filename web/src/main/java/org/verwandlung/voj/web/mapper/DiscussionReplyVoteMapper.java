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

import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Param;
import org.verwandlung.voj.web.model.DiscussionReplyVote;

/**
 * DiscussionReplyVote Data Access Object. Each vote is one row keyed by (discussion reply, voter),
 * so the "one vote per user per reply" rule is enforced by the database rather than in application
 * code.
 *
 * @author Haozhe Xie
 */
public interface DiscussionReplyVoteMapper {
  /**
   * Gets the number of votes of a given type on a discussion reply.
   *
   * @param discussionReplyId - the unique identifier of the discussion reply
   * @param voteType - the type of the vote ({@code DiscussionReplyVote.VOTE_UP} or {@code
   *     DiscussionReplyVote.VOTE_DOWN})
   * @return the number of votes of the given type on the discussion reply
   */
  int getNumberOfVotes(
      @Param("discussionReplyId") long discussionReplyId, @Param("voteType") int voteType);

  /**
   * Gets the type of the up/down vote a user has cast on a discussion reply. Report rows are ignored,
   * so this returns only {@code VOTE_UP}, {@code VOTE_DOWN} or {@code null}.
   *
   * @param discussionReplyId - the unique identifier of the discussion reply
   * @param voterUid - the unique identifier of the voting user
   * @return the up/down vote type the user has cast, or a {@code null} reference if none
   */
  Integer getVoteType(
      @Param("discussionReplyId") long discussionReplyId, @Param("voterUid") long voterUid);

  /**
   * Gets the total number of reports across all replies of a discussion thread. Used to decide
   * whether a thread should be hidden.
   *
   * @param discussionThreadId - the unique identifier of the discussion thread
   * @return the total number of reports on the thread's replies
   */
  int getNumberOfReportsOfThread(@Param("discussionThreadId") long discussionThreadId);

  /**
   * [For administrators only] Gets the most-reported discussion replies, feeding the dashboard's
   * moderation queue and the discussion admin. Each row carries the reply id, its thread id, a
   * content snippet, the report count and the time of the latest report.
   *
   * @param limit - the maximum number of rows to return
   * @return an ordered list of reported-reply rows (most reports first)
   */
  List<Map<String, Object>> getReportedReplies(@Param("limit") int limit);

  /**
   * [For administrators only] Gets the number of distinct replies that have at least one report.
   * Feeds the moderation badge.
   *
   * @return the number of reported replies
   */
  int getNumberOfReportedReplies();

  /**
   * [For administrators only] Gets the reporters of a discussion reply, feeding the moderation
   * review page. Each row carries the reporter's username and the time the report was filed, most
   * recent first.
   *
   * @param discussionReplyId - the unique identifier of the reported reply
   * @return an ordered list of reporter rows (most recent first)
   */
  List<Map<String, Object>> getReportersOfReply(@Param("discussionReplyId") long discussionReplyId);

  /**
   * [For administrators only] Gets the reporters across every reply of a discussion thread, feeding
   * the thread moderation page's report list. Each row carries the reporter's username and the time
   * the report was filed, most recent first.
   *
   * @param discussionThreadId - the unique identifier of the discussion thread
   * @return an ordered list of reporter rows (most recent first)
   */
  List<Map<String, Object>> getReportersOfThread(
      @Param("discussionThreadId") long discussionThreadId);

  /**
   * [For administrators only] Dismisses every report on a discussion reply, clearing it from the
   * moderation queue while keeping the reply itself intact.
   *
   * @param discussionReplyId - the unique identifier of the reply whose reports are dismissed
   * @return the number of report rows removed
   */
  int dismissReportsOfReply(@Param("discussionReplyId") long discussionReplyId);

  /**
   * Casts a vote on a discussion reply. If the user has already voted on the reply, the existing vote
   * is overwritten so a user can switch between an upvote and a downvote with a single statement.
   *
   * @param discussionReplyVote - the vote to cast
   * @return the number of rows affected
   */
  int castVote(DiscussionReplyVote discussionReplyVote);

  /**
   * Withdraws a user's vote of a given type from a discussion reply. The vote type is matched so that
   * cancelling an upvote never removes a downvote and vice versa.
   *
   * @param discussionReplyId - the unique identifier of the discussion reply
   * @param voterUid - the unique identifier of the voting user
   * @param voteType - the type of the vote to withdraw
   * @return the number of rows affected
   */
  int withdrawVote(
      @Param("discussionReplyId") long discussionReplyId,
      @Param("voterUid") long voterUid,
      @Param("voteType") int voteType);
}
