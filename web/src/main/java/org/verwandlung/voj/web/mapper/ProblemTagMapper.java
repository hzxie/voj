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
import org.verwandlung.voj.web.model.ProblemTag;
import org.verwandlung.voj.web.model.ProblemTagRelationship;

/**
 * ProblemTag Data Access Object.
 *
 * @author Haozhe Xie
 */
public interface ProblemTagMapper {
  /**
   * Gets all problem tags.
   *
   * @return a list containing the problem tags
   */
  List<ProblemTag> getProblemTags();

  /**
   * Gets a problem tag object by its unique identifier.
   *
   * @param problemTagId - the unique identifier of the problem tag
   * @return the expected problem tag object, or a null reference
   */
  ProblemTag getProblemTagUsingTagId(long problemTagId);

  /**
   * Gets the list of problem tag objects by the unique identifier of a problem.
   *
   * @param problemId - the unique identifier of the problem
   * @return the expected list of problem tag objects
   */
  List<ProblemTag> getProblemTagsUsingProblemId(long problemId);

  /**
   * Gets the tags of problems within a range.
   *
   * @param problemIdLowerBound - the lower bound of the problem ID range
   * @param problemIdUpperBound - the upper bound of the problem ID range
   * @return a list containing the problem tag information
   */
  List<ProblemTagRelationship> getProblemTagsOfProblems(
      @Param(value = "problemIdLowerBound") long problemIdLowerBound,
      @Param(value = "problemIdUpperBound") long problemIdUpperBound);

  /**
   * Gets a problem tag object by its alias.
   *
   * @param problemTagSlug - the alias of the problem tag
   * @return the expected problem tag object, or a null reference
   */
  ProblemTag getProblemTagUsingTagSlug(String problemTagSlug);

  /**
   * Creates a problem tag object.
   *
   * @param problemTag - the problem tag object to create
   */
  int createProblemTag(ProblemTag problemTag);

  /**
   * Creates the relationship between a problem and a problem tag.
   *
   * @param problemId - the unique identifier of the problem
   * @param problemTag - the problem tag object
   */
  int createProblemTagRelationship(
      @Param(value = "problemId") long problemId,
      @Param(value = "problemTag") ProblemTag problemTag);

  /**
   * Updates a problem tag object.
   *
   * @param problemTag - the problem tag object to update
   */
  int updateProblemTag(ProblemTag problemTag);

  /**
   * Deletes a problem tag object.
   *
   * @param problemTagId - the unique identifier of the problem tag object to delete
   */
  int deleteProblemTagUsingTagId(long problemTagId);

  /**
   * Deletes all tags of a problem.
   *
   * @param problemId - the unique identifier of the problem
   */
  int deleteProblemTagRelationship(long problemId);
}
