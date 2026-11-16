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
package org.verwandlung.voj.judger.mapper;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.Select;

import org.verwandlung.voj.judger.model.Problem;

/**
 * Problem Data Access Object.
 *
 * @author Haozhe Xie
 */
public interface ProblemMapper {
  /**
   * Gets a problem by its unique identifier: what the judger needs of it, its limits and the
   * version of its test data.
   *
   * @param problemId - the unique identifier of the problem
   * @return the problem, or null if there is none
   */
  @Select(
      "SELECT problem_id, problem_time_limit, problem_memory_limit, checkpoints_version"
          + " FROM voj_problems WHERE problem_id = #{problemId}")
  @Results({
    @Result(property = "problemId", column = "problem_id"),
    @Result(property = "timeLimit", column = "problem_time_limit"),
    @Result(property = "memoryLimit", column = "problem_memory_limit"),
    @Result(property = "checkpointsVersion", column = "checkpoints_version")
  })
  Problem getProblem(@Param("problemId") long problemId);
}
