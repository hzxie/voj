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

import org.verwandlung.voj.web.model.ProblemDifficulty;

/**
 * Problem Difficulty Data Access Object.
 *
 * @author Haozhe Xie
 */
public interface ProblemDifficultyMapper {
  /**
   * Gets all the problem difficulty levels.
   *
   * @return a list of all problem difficulty levels
   */
  List<ProblemDifficulty> getProblemDifficulties();

  /**
   * Gets a problem difficulty by its alias.
   *
   * @param problemDifficultySlug - the alias of the problem difficulty
   * @return a problem difficulty object, or null if it does not exist
   */
  ProblemDifficulty getProblemDifficultyUsingSlug(
      @Param("problemDifficultySlug") String problemDifficultySlug);
}
