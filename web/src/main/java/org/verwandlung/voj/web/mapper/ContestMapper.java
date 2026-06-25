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
   * Gets the list of contests.
   *
   * @param keyword - the keyword of the contest
   * @param offset - the offset of the first contest
   * @param limit - the number of contests to fetch
   * @return the expected contest objects
   */
  List<Contest> getContests(
      @Param("keyword") String keyword, @Param("offset") long offset, @Param("limit") int limit);

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
}
