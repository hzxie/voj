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
package org.verwandlung.voj.judger.model;

import java.io.Serializable;

/**
 * A problem, as the judger needs it: its limits and the version of its test data. Maps to the
 * voj_problems table in the database.
 *
 * @author Haozhe Xie
 */
public class Problem implements Serializable {
  /** Default constructor of the problem. */
  public Problem() {}

  /**
   * Constructor of the Problem class.
   *
   * @param problemId - the unique identifier of the problem
   * @param timeLimit - the time limit of a run (ms)
   * @param memoryLimit - the memory limit of a run (KB)
   */
  public Problem(long problemId, int timeLimit, int memoryLimit) {
    this.problemId = problemId;
    this.timeLimit = timeLimit;
    this.memoryLimit = memoryLimit;
  }

  /**
   * Gets the unique identifier of the problem.
   *
   * @return the unique identifier of the problem
   */
  public long getProblemId() {
    return problemId;
  }

  /**
   * Sets the unique identifier of the problem.
   *
   * @param problemId - the unique identifier of the problem
   */
  public void setProblemId(long problemId) {
    this.problemId = problemId;
  }

  /**
   * Gets the time limit of a run.
   *
   * @return the time limit (ms)
   */
  public int getTimeLimit() {
    return timeLimit;
  }

  /**
   * Sets the time limit of a run.
   *
   * @param timeLimit - the time limit (ms)
   */
  public void setTimeLimit(int timeLimit) {
    this.timeLimit = timeLimit;
  }

  /**
   * Gets the memory limit of a run.
   *
   * @return the memory limit (KB)
   */
  public int getMemoryLimit() {
    return memoryLimit;
  }

  /**
   * Sets the memory limit of a run.
   *
   * @param memoryLimit - the memory limit (KB)
   */
  public void setMemoryLimit(int memoryLimit) {
    this.memoryLimit = memoryLimit;
  }

  /**
   * Gets the version of the problem's test data.
   *
   * @return the version, bumped by the web application whenever the test data changes
   */
  public int getCheckpointsVersion() {
    return checkpointsVersion;
  }

  /**
   * Sets the version of the problem's test data.
   *
   * @param checkpointsVersion - the version
   */
  public void setCheckpointsVersion(int checkpointsVersion) {
    this.checkpointsVersion = checkpointsVersion;
  }

  /* (non-Javadoc)
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    return String.format(
        "Problem: [ProblemID=%s, TimeLimit=%s, MemoryLimit=%s, CheckpointsVersion=%s]",
        problemId, timeLimit, memoryLimit, checkpointsVersion);
  }

  /** The unique identifier of the problem. */
  private long problemId;

  /** The time limit of a run (ms). */
  private int timeLimit;

  /** The memory limit of a run (KB). */
  private int memoryLimit;

  /** The checkpoints version, bumped by the web side whenever a problem's test data changes. */
  private int checkpointsVersion;

  /** The unique serialization identifier. */
  private static final long serialVersionUID = 4703482016721365341L;
}
