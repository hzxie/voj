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

import org.apache.ibatis.annotations.Param;
import org.verwandlung.voj.web.model.DiscussionReply;

import java.util.List;

/**
 * DiscussionReply Data Access Object.
 *
 * @author Haozhe Xie
 */
public interface DiscussionReplyMapper {
  /**
   * Gets a DiscussionReply object by the unique identifier of the discussion reply.
   *
   * @param discussionReplyId - the unique identifier of the discussion reply
   * @return the expected DiscussionReply object, or a null reference
   */
  DiscussionReply getDiscussionReplyUsingReplyId(
      @Param("discussionReplyId") long discussionReplyId);

  /**
   * Gets all replies of a discussion thread, with pagination.
   *
   * @param discussionThreadId - the unique identifier of the discussion thread
   * @param offset the cursor of the first reply
   * @param limit the number of replies to fetch
   * @param visibleOnly - when {@code true}, replies hidden by an administrator are excluded
   * @return a List object containing the discussion replies
   */
  List<DiscussionReply> getDiscussionRepliesUsingThreadId(
      @Param("discussionThreadId") long discussionThreadId,
      @Param("offset") long offset,
      @Param("limit") int limit,
      @Param("visibleOnly") boolean visibleOnly);

  /**
   * Creates a discussion reply.
   *
   * @param discussionReply - the DiscussionReply object to create
   */
  int createDiscussionReply(DiscussionReply discussionReply);

  /**
   * Updates a discussion reply.
   *
   * @param discussionReply - the DiscussionReply object to update
   */
  int updateDiscussionReply(DiscussionReply discussionReply);

  /**
   * [For administrators only] Updates the visibility of a discussion reply.
   *
   * @param discussionReplyId - the unique identifier of the discussion reply
   * @param visible - whether the discussion reply is visible to ordinary users
   */
  int updateReplyVisibility(
      @Param("discussionReplyId") long discussionReplyId, @Param("visible") boolean visible);

  /**
   * Deletes a discussion reply.
   *
   * @param discussionReplyId - the unique identifier of the reply to delete
   */
  int deleteDiscussionReplyUsingReplyId(long discussionReplyId);
}
