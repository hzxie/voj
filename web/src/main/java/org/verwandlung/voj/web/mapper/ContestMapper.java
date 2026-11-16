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
import org.verwandlung.voj.web.model.Contest;

import java.util.List;
import java.util.Map;

/**
 * Contest Data Access Object.
 *
 * @author Haozhe Xie
 */
public interface ContestMapper {
  /**
   * [For administrators only] Gets the total number of contests.
   *
   * @param keyword - the keyword of the contest
   * @return the total number of contests
   */
  long getNumberOfContests(@Param("keyword") String keyword);

  /**
   * [For administrators only] Gets the number of contests that are currently live (started but not
   * yet ended). Feeds the dashboard's "live contests" KPI and the sidebar badge.
   *
   * @return the number of live contests
   */
  long getNumberOfLiveContests();

  /**
   * Gets the list of contests.
   *
   * @param keyword - the keyword of the contest
   * @param isPublicOnly - whether to fetch only the published contests
   * @param offset - the offset of the first contest
   * @param limit - the number of contests to fetch
   * @return the expected contest objects
   */
  List<Contest> getContests(
      @Param("keyword") String keyword,
      @Param("isPublicOnly") boolean isPublicOnly,
      @Param("offset") long offset,
      @Param("limit") int limit);

  /**
   * Gets a contest by its unique identifier.
   *
   * @param contestId - the unique identifier of the contest
   * @return the expected contest object
   */
  Contest getContest(long contestId);

  /**
   * Creates a contest.
   *
   * @param contest - the contest object to create
   */
  int createContest(Contest contest);

  /**
   * Updates a contest.
   *
   * @param contest - the contest object to update
   */
  int updateContest(Contest contest);

  /**
   * Deletes a contest.
   *
   * @param contestId - the unique identifier of the contest
   */
  int deleteContest(long contestId);

  /**
   * Gets the problems of a contest, in the order the contest lists them.
   *
   * @param contestId - the unique identifier of the contest
   * @return the unique identifiers of the problems
   */
  List<Long> getProblemIdsOfContest(@Param("contestId") long contestId);

  /**
   * Checks whether a problem is one of the problems of a contest.
   *
   * @param contestId - the unique identifier of the contest
   * @param problemId - the unique identifier of the problem
   * @return whether the problem is one of the contest's
   */
  boolean isProblemOfContest(
      @Param("contestId") long contestId, @Param("problemId") long problemId);

  /**
   * Gets the number of problems of some contests.
   *
   * @param contestIds - the unique identifiers of the contests (not empty)
   * @return for each contest with problems: {@code contestId} and {@code numberOfProblems}
   */
  List<Map<String, Object>> getNumberOfProblemsOfContests(
      @Param("contestIds") List<Long> contestIds);

  /**
   * [For administrators only] Removes every problem from a contest.
   *
   * @param contestId - the unique identifier of the contest
   * @return the number of problems removed
   */
  int deleteProblemsOfContest(@Param("contestId") long contestId);

  /**
   * [For administrators only] Adds problems to a contest, at the positions of the list.
   *
   * @param contestId - the unique identifier of the contest
   * @param problemIds - the unique identifiers of the problems (not empty, without duplicates)
   * @return the number of problems added
   */
  int createProblemsOfContest(
      @Param("contestId") long contestId, @Param("problemIds") List<Long> problemIds);
}
