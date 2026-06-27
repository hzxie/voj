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
import org.verwandlung.voj.web.model.DiscussionThread;

import java.util.List;

/**
 * DiscussionThread Data Access Object.
 *
 * @author Haozhe Xie
 */
public interface DiscussionThreadMapper {
  /**
   * Gets all discussion threads.
   *
   * @param problemId - the unique identifier of the problem
   * @param discussionTopicId - the unique identifier of the discussion topic
   * @param offset - the starting ID of the discussion threads
   * @param limit - the number of threads to fetch
   * @param reportHideThreshold - threads whose total report count reaches this value are hidden; a
   *     value of {@code 0} or below disables hiding
   * @return a list containing discussion thread objects
   */
  List<DiscussionThread> getDiscussionThreads(
      @Param("problemId") long problemId,
      @Param("discussionTopicId") int discussionTopicId,
      @Param("offset") long offset,
      @Param("limit") int limit,
      @Param("reportHideThreshold") int reportHideThreshold);

  /**
   * Gets the solution discussion of a problem. The solution discussion is created automatically
   * when the problem is created, i.e. it is the first discussion related to that problem.
   *
   * @param problemId - the unique identifier of the problem
   * @return the DiscussionThread object of the problem's solution discussion
   */
  DiscussionThread getSolutionThreadOfProblem(@Param("problemId") long problemId);

  /**
   * Gets a discussion thread object by the unique identifier of the discussion thread.
   *
   * @param discussionThreadId - the unique identifier of the discussion thread
   * @return the corresponding discussion thread object
   */
  DiscussionThread getDiscussionThreadUsingThreadId(
      @Param("discussionThreadId") long discussionThreadId);

  /**
   * Creates a discussion thread.
   *
   * @param discussionThread - the discussion thread to create
   */
  int createDiscussionThread(DiscussionThread discussionThread);

  /**
   * Updates a discussion thread.
   *
   * @param discussionThread - the discussion thread to update
   */
  int updateDiscussionThread(DiscussionThread discussionThread);

  /**
   * Deletes a discussion thread.
   *
   * @param discussionThreadId - the unique identifier of the discussion thread to delete
   */
  int deleteDiscussionThreadUsingThreadId(long discussionThreadId);
}
