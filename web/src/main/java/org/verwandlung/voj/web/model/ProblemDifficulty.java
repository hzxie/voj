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
package org.verwandlung.voj.web.model;

import java.io.Serializable;

/**
 * The model of a problem difficulty level. Maps to the voj_problem_difficulty table in the
 * database. Like categories and tags, the difficulty is a normalized lookup (id + slug + name).
 *
 * @author Haozhe Xie
 */
public class ProblemDifficulty implements Serializable {
  /** Default constructor of the problem difficulty. */
  public ProblemDifficulty() {}

  /**
   * Constructor of the problem difficulty.
   *
   * @param problemDifficultyId - the unique identifier of the problem difficulty
   * @param problemDifficultySlug - the alias of the problem difficulty
   * @param problemDifficultyName - the name of the problem difficulty
   */
  public ProblemDifficulty(
      int problemDifficultyId, String problemDifficultySlug, String problemDifficultyName) {
    this.problemDifficultyId = problemDifficultyId;
    this.problemDifficultySlug = problemDifficultySlug;
    this.problemDifficultyName = problemDifficultyName;
  }

  /**
   * Gets the unique identifier of the problem difficulty.
   *
   * @return the unique identifier of the problem difficulty
   */
  public int getProblemDifficultyId() {
    return problemDifficultyId;
  }

  /**
   * Sets the unique identifier of the problem difficulty.
   *
   * @param problemDifficultyId - the unique identifier of the problem difficulty
   */
  public void setProblemDifficultyId(int problemDifficultyId) {
    this.problemDifficultyId = problemDifficultyId;
  }

  /**
   * Gets the alias of the problem difficulty.
   *
   * @return the alias of the problem difficulty
   */
  public String getProblemDifficultySlug() {
    return problemDifficultySlug;
  }

  /**
   * Sets the alias of the problem difficulty.
   *
   * @param problemDifficultySlug - the alias of the problem difficulty
   */
  public void setProblemDifficultySlug(String problemDifficultySlug) {
    this.problemDifficultySlug = problemDifficultySlug;
  }

  /**
   * Gets the name of the problem difficulty.
   *
   * @return the name of the problem difficulty
   */
  public String getProblemDifficultyName() {
    return problemDifficultyName;
  }

  /**
   * Sets the name of the problem difficulty.
   *
   * @param problemDifficultyName - the name of the problem difficulty
   */
  public void setProblemDifficultyName(String problemDifficultyName) {
    this.problemDifficultyName = problemDifficultyName;
  }

  /* (non-Javadoc)
   * @see java.lang.Object#hashCode()
   */
  public int hashCode() {
    return problemDifficultyId;
  }

  /* (non-Javadoc)
   * @see java.lang.Object#equals(java.lang.Object)
   */
  public boolean equals(Object obj) {
    if (obj instanceof ProblemDifficulty) {
      ProblemDifficulty anotherDifficulty = (ProblemDifficulty) obj;
      return anotherDifficulty.getProblemDifficultyId() == problemDifficultyId;
    }
    return false;
  }

  /* (non-Javadoc)
   * @see java.lang.Object#toString()
   */
  public String toString() {
    return String.format(
        "ProblemDifficulty [ID=%d, Slug=%s, Name=%s]",
        new Object[] {problemDifficultyId, problemDifficultySlug, problemDifficultyName});
  }

  /** The unique identifier of the problem difficulty. */
  private int problemDifficultyId;

  /** The alias of the problem difficulty. */
  private String problemDifficultySlug;

  /** The name of the problem difficulty. */
  private String problemDifficultyName;

  /** The unique serialization identifier. */
  private static final long serialVersionUID = 7521837016721365342L;
}
