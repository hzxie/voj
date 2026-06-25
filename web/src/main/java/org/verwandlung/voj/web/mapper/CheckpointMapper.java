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

import org.verwandlung.voj.web.model.Checkpoint;

/**
 * Checkpoint Data Access Object.
 *
 * @author Haozhe Xie
 */
public interface CheckpointMapper {
  /**
   * [For administrators only] Gets the total number of problem checkpoints in the system.
   *
   * @return the total number of problem checkpoints in the system
   */
  long getNumberOfCheckpoints();

  /**
   * Gets all checkpoints of a problem.
   *
   * @param problemId - the unique identifier of the problem
   * @return all checkpoints of the problem
   */
  List<Checkpoint> getCheckpointsUsingProblemId(@Param("problemId") long problemId);

  /**
   * [For administrators only] Creates a checkpoint.
   *
   * @param checkpoint - the checkpoint
   */
  int createCheckpoint(Checkpoint checkpoint);

  /**
   * [For administrators only] Deletes all checkpoints of a problem.
   *
   * @param problemId - the unique identifier of the problem
   */
  int deleteCheckpoint(long problemId);
}
