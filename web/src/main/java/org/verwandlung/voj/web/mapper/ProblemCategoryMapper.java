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
import org.verwandlung.voj.web.model.ProblemCategory;
import org.verwandlung.voj.web.model.ProblemCategoryRelationship;

/**
 * ProblemCategory Data Access Object.
 *
 * @author Haozhe Xie
 */
public interface ProblemCategoryMapper {
  /**
   * Gets all problem categories.
   *
   * @return a list containing all problem categories
   */
  List<ProblemCategory> getProblemCategories();

  /**
   * Gets a problem category object by its unique identifier.
   *
   * @param problemCategoryId - the unique identifier of the problem category
   * @return the expected problem category object, or a null reference
   */
  ProblemCategory getProblemCategoryUsingCategoryId(int problemCategoryId);

  /**
   * Gets the category list of a problem.
   *
   * @param problemId - the unique identifier of the problem
   * @return a list containing the problem categories
   */
  List<ProblemCategory> getProblemCategoriesUsingProblemId(long problemId);

  /**
   * Gets the categories of problems within a range.
   *
   * @param problemIdLowerBound - the lower bound of the problem ID range
   * @param problemIdUpperBound - the upper bound of the problem ID range
   * @return a list containing the problem category information
   */
  List<ProblemCategoryRelationship> getProblemCategoriesOfProblems(
      @Param(value = "problemIdLowerBound") long problemIdLowerBound,
      @Param(value = "problemIdUpperBound") long problemIdUpperBound);

  /**
   * Gets a problem category object by its alias.
   *
   * @param problemCategorySlug - the alias of the problem category
   * @return the expected problem category object, or a null reference
   */
  ProblemCategory getProblemCategoryUsingCategorySlug(String problemCategorySlug);

  /**
   * Creates a problem category object.
   *
   * @param problemCategory - the problem category object to create
   */
  int createProblemCategory(ProblemCategory problemCategory);

  /**
   * Creates the relationship between a problem and a problem category.
   *
   * @param problemId - the unique identifier of the problem
   * @param problemCategory - the problem category object
   */
  int createProblemCategoryRelationship(
      @Param(value = "problemId") long problemId,
      @Param(value = "problemCategory") ProblemCategory problemCategory);

  /**
   * Updates a problem category object.
   *
   * @param problemCategory - the problem category object to update
   */
  int updateProblemCategory(ProblemCategory problemCategory);

  /**
   * Deletes a problem category object.
   *
   * @param problemCategoryId - the unique identifier of the problem category object to delete
   */
  int deleteProblemCategory(int problemCategoryId);

  /**
   * Deletes all category relationships of a problem.
   *
   * @param problemId - the unique identifier of the problem
   */
  int deleteProblemCategoryRelationship(long problemId);
}
