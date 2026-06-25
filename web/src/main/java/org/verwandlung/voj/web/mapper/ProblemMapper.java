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

import org.apache.ibatis.annotations.Param;

import org.verwandlung.voj.web.model.Problem;

/**
 * Problem Data Access Object.
 *
 * @author Haozhe Xie
 */
public interface ProblemMapper {
  /**
   * [For administrators only] Gets the total number of all problems.
   *
   * @return the total number of all problems
   */
  long getNumberOfProblems();

  /**
   * Gets the total number of problems matching the filter conditions.
   *
   * @param keyword - the keyword
   * @param problemCategoryId - the unique identifier of the problem category
   * @param isPublicOnly - whether to filter only public problems
   * @return the total number of problems matching the filter conditions
   */
  long getNumberOfProblemsUsingFilters(
      @Param("keyword") String keyword,
      @Param("problemCategoryId") int problemCategoryId,
      @Param("isPublicOnly") boolean isPublicOnly);

  /**
   * Gets the ID of the first problem.
   *
   * @return the ID of the first problem
   */
  long getLowerBoundOfProblems();

  /**
   * Gets the ID of the last problem.
   *
   * @return the ID of the last problem
   */
  long getUpperBoundOfProblems();

  /**
   * Gets the ID of the last problem within a problem range.
   *
   * @param isPublicOnly - whether to filter only public problems
   * @param offset - the starting number of the problem identifier
   * @param limit - the number of problems to fetch
   * @return the ID of the last problem within the problem range
   */
  long getUpperBoundOfProblemsWithLimit(
      @Param("isPublicOnly") boolean isPublicOnly,
      @Param("problemId") long offset,
      @Param("limit") int limit);

  /**
   * Gets a problem object by its unique identifier.
   *
   * @param problemId - the unique identifier of the problem
   * @return a problem object
   */
  Problem getProblem(@Param("problemId") long problemId);

  /**
   * Gets all problems within a range by problem identifier and keyword.
   *
   * @param keyword - the keyword
   * @param problemCategoryId - the unique identifier of the problem category
   * @param problemTagId - the unique identifier of the problem tag
   * @param isPublicOnly - whether to filter only public problems
   * @param offset - the starting number of the problem identifier
   * @param limit - the number of problems to fetch
   * @return the matching problems within the range
   */
  List<Problem> getProblemsUsingFilters(
      @Param("keyword") String keyword,
      @Param("problemCategoryId") int problemCategoryId,
      @Param("problemTagId") long problemTagId,
      @Param("isPublicOnly") boolean isPublicOnly,
      @Param("problemId") long offset,
      @Param("limit") int limit);

  /**
   * Creates a new problem object.
   *
   * @param problem - the problem object
   * @return whether the operation completed successfully
   */
  int createProblem(Problem problem);

  /**
   * Updates problem information.
   *
   * @param problem - the problem object
   * @return whether the operation completed successfully
   */
  int updateProblem(Problem problem);

  /**
   * Deletes a problem object by its unique identifier.
   *
   * @param problemId - the unique identifier of the problem
   * @return whether the operation completed successfully
   */
  int deleteProblem(@Param("problemId") long problemId);
}
