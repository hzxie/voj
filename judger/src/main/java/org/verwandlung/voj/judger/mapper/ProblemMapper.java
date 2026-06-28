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

import org.apache.ibatis.annotations.Options;
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
   * Gets a problem object by its unique identifier.
   *
   * @param problemId - the unique identifier of the problem
   * @return a problem object
   */
  @Select("SELECT * FROM voj_problems WHERE problem_id = #{problemId}")
  @Options(useCache = true)
  @Results({
    @Result(property = "problemId", column = "problem_id"),
    @Result(property = "isPublic", column = "problem_is_public"),
    @Result(property = "problemName", column = "problem_name"),
    @Result(property = "totalSubmission", column = "total_submission"),
    @Result(property = "acceptedSubmission", column = "accepted_submission"),
    @Result(property = "timeLimit", column = "problem_time_limit"),
    @Result(property = "memoryLimit", column = "problem_memory_limit"),
    @Result(property = "description", column = "problem_description"),
    @Result(property = "inputFormat", column = "problem_input_format"),
    @Result(property = "outputFormat", column = "problem_output_format"),
    @Result(property = "sampleInput", column = "problem_sample_input"),
    @Result(property = "sampleOutput", column = "problem_sample_output"),
    @Result(property = "hint", column = "problem_hint"),
    @Result(property = "checkpointsVersion", column = "checkpoints_version")
  })
  Problem getProblem(@Param("problemId") long problemId);
}
