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

import org.verwandlung.voj.web.model.JudgeResult;

/**
 * JudgeResult Data Access Object.
 *
 * @author Haozhe Xie
 */
public interface JudgeResultMapper {
  /**
   * Gets a judge result object by its unique identifier.
   *
   * @param judgeResultId - the unique identifier of the judge result
   * @return the expected judge result object, or a null reference
   */
  JudgeResult getJudgeResultUsingId(@Param("judgeResultId") int judgeResultId);

  /**
   * Gets a judge result object by its alias.
   *
   * @param judgeResultSlug - the alias of the judge result
   * @return the expected judge result object, or a null reference
   */
  JudgeResult getJudgeResultUsingSlug(@Param("judgeResultSlug") String judgeResultSlug);
}
