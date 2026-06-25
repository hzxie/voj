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
 * The model of a checkpoint. Maps to the voj_problem_checkpoints table in the database.
 *
 * @author Haozhe Xie
 */
public class Checkpoint implements Serializable {
  /** Default constructor of the checkpoint. */
  public Checkpoint() {}

  /**
   * Constructor of the checkpoint.
   *
   * @param problemId - the unique identifier of the problem
   * @param checkpointId - the unique identifier of the checkpoint
   * @param isExactlyMatch - whether the checkpoint requires an exact match
   * @param score - the score of the checkpoint
   * @param input - the standard input of the checkpoint
   * @param output - the standard output of the checkpoint
   */
  public Checkpoint(
      long problemId,
      int checkpointId,
      boolean isExactlyMatch,
      int score,
      String input,
      String output) {
    this.problemId = problemId;
    this.checkpointId = checkpointId;
    this.isExactlyMatch = isExactlyMatch;
    this.score = score;
    this.input = input;
    this.output = output;
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
   * Gets the unique identifier of the checkpoint.
   *
   * @return the unique identifier of the checkpoint
   */
  public int getCheckpointId() {
    return checkpointId;
  }

  /**
   * Sets the unique identifier of the checkpoint.
   *
   * @param checkpointId - the unique identifier of the checkpoint
   */
  public void setCheckpointId(int checkpointId) {
    this.checkpointId = checkpointId;
  }

  /**
   * Gets whether the checkpoint requires an exact match.
   *
   * @return whether the checkpoint requires an exact match
   */
  public boolean isExactlyMatch() {
    return isExactlyMatch;
  }

  /**
   * Sets whether the checkpoint requires an exact match.
   *
   * @param isExactlyMatch - whether the checkpoint requires an exact match
   */
  public void setExactlyMatch(boolean isExactlyMatch) {
    this.isExactlyMatch = isExactlyMatch;
  }

  /**
   * Gets the score of the checkpoint.
   *
   * @return the score of the checkpoint
   */
  public int getScore() {
    return score;
  }

  /**
   * Sets the score of the checkpoint.
   *
   * @param score - the score of the checkpoint
   */
  public void setScore(int score) {
    this.score = score;
  }

  /**
   * Gets the input of the checkpoint.
   *
   * @return the input of the checkpoint
   */
  public String getInput() {
    return input;
  }

  /**
   * Sets the input of the checkpoint.
   *
   * @param input - the input of the checkpoint
   */
  public void setInput(String input) {
    this.input = input;
  }

  /**
   * Gets the output of the checkpoint.
   *
   * @return the output of the checkpoint
   */
  public String getOutput() {
    return output;
  }

  /**
   * Sets the output of the checkpoint.
   *
   * @param output - the output of the checkpoint
   */
  public void setOutput(String output) {
    this.output = output;
  }

  /* (non-Javadoc)
   * @see java.lang.Object#toString()
   */
  public String toString() {
    return String.format(
        "CheckPoint [ProblemId=%d, CheckpointId=%d, Score=%d]",
        new Object[] {problemId, checkpointId, score});
  }

  /** The unique identifier of the problem. */
  private long problemId;

  /** The unique identifier of the checkpoint. */
  private int checkpointId;

  /** Whether the checkpoint requires an exact match. */
  private boolean isExactlyMatch;

  /** The score of the checkpoint. */
  private int score;

  /** The standard input of the checkpoint. */
  private String input;

  /** The standard output of the checkpoint. */
  private String output;

  /** The unique serialization identifier. */
  private static final long serialVersionUID = -8129670699537187948L;
}
