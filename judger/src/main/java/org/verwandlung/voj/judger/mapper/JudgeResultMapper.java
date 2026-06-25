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

import java.util.List;

import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.Select;

import org.verwandlung.voj.judger.model.JudgeResult;

/**
 * JudgeResult Data Access Object.
 *
 * @author Haozhe Xie
 */
public interface JudgeResultMapper {
  /**
   * Gets all judge result objects.
   *
   * @return a list of all judge result objects
   */
  @Select("SELECT * FROM voj_judge_results")
  @Options(useCache = true)
  @Results({
    @Result(property = "judgeResultId", column = "judge_result_id"),
    @Result(property = "judgeResultSlug", column = "judge_result_slug"),
    @Result(property = "judgeResultName", column = "judge_result_name")
  })
  List<JudgeResult> getAllJudgeResults();

  /**
   * Gets a judge result object by its unique English abbreviation.
   *
   * @param judgeResultSlug - the unique English abbreviation of the judge result
   * @return the expected judge result object, or a null reference
   */
  @Select("SELECT * FROM voj_judge_results WHERE judge_result_slug = #{judgeResultSlug}")
  @Options(useCache = true)
  @Results({
    @Result(property = "judgeResultId", column = "judge_result_id"),
    @Result(property = "judgeResultSlug", column = "judge_result_slug"),
    @Result(property = "judgeResultName", column = "judge_result_name")
  })
  JudgeResult getJudgeResultUsingSlug(@Param("judgeResultSlug") String judgeResultSlug);
}
