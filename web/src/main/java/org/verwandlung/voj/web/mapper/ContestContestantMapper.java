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
import org.verwandlung.voj.web.model.ContestContestant;

import java.util.List;

/**
 * ContestContestant Data Access Object.
 *
 * @author Haozhe Xie
 */
public interface ContestContestantMapper {
  /**
   * Gets the number of contestants of a contest.
   *
   * @param contestId - the unique identifier of the contest
   * @return the number of contestants of the contest
   */
  long getNumberOfContestantsOfContest(long contestId);

  /**
   * Gets the number of contests a user has attended.
   *
   * @param contestantUid - the unique identifier of the contestant
   * @return the number of contests the user has attended
   */
  long getNumberOfContestsUsingContestantUid(@Param("contestantUid") long contestantUid);

  /**
   * Gets the list of contestants of a contest (OI mode).
   *
   * @param contestId - the unique identifier of the contest
   * @param offset - the offset of the first contestant
   * @param limit - the number of contestants to fetch
   * @return the list of contestants of the contest
   */
  List<ContestContestant> getContestantsOfContestForOi(
      @Param("contestId") long contestId, @Param("offset") long offset, @Param("limit") int limit);

  /**
   * Gets the list of contestants of a contest (ACM mode). Here, the Score field of
   * ContestContestant is used to store the number of accepted problems, and the Time field is used
   * to store the number of rejected problems (the number of rejected problems is related to the
   * penalty time).
   *
   * @param contestId - the unique identifier of the contest
   * @param offset - the offset of the first contestant
   * @param limit - the number of contestants to fetch
   * @return the list of contestants of the contest
   */
  List<ContestContestant> getContestantsOfContestForAcm(
      @Param("contestId") long contestId, @Param("offset") long offset, @Param("limit") int limit);

  /**
   * Gets the participation record of a contestant in a contest.
   *
   * @param contestId - the unique identifier of the contest
   * @param contestantUid - the unique identifier of the contestant
   * @return the corresponding participation record of the contestant in the contest
   */
  ContestContestant getContestantOfContest(
      @Param("contestId") long contestId, @Param("contestantUid") long contestantUid);

  /**
   * Creates a participation record (for entering a contest).
   *
   * @param contestContestant - the participation record to create
   */
  int createContestContestant(ContestContestant contestContestant);

  /**
   * Updates a participation record. Used to update the submitted source code when participating in
   * an OI contest.
   *
   * @param contestContestant - the participation record to update
   */
  int updateContestContestant(ContestContestant contestContestant);

  /**
   * Deletes a participation record.
   *
   * @param contestId - the unique identifier of the contest
   * @param contestantUid - the unique identifier of the contestant
   */
  int deleteContestContestant(
      @Param("contestId") long contestId, @Param("contestantUid") long contestantUid);

  /**
   * Deletes all participation records of a contest.
   *
   * @param contestId - the unique identifier of the contest
   * @return the number of participation records deleted
   */
  int deleteContestantsOfContest(@Param("contestId") long contestId);
}
