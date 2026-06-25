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
 * The model of a problem category relationship. Maps to the voj_problem_category_relationships
 * table in the database.
 *
 * @author Haozhe Xie
 */
public class ProblemCategoryRelationship implements Serializable {
  /** Default constructor of the problem category relationship. */
  public ProblemCategoryRelationship() {}

  /**
   * Constructor of the problem category relationship.
   *
   * @param problemId - the unique identifier of the problem
   * @param problemCategoryId - the unique identifier of the problem category
   * @param problemCategorySlug - the alias of the problem category
   * @param problemCategoryName - the name of the problem category
   */
  public ProblemCategoryRelationship(
      long problemId,
      int problemCategoryId,
      String problemCategorySlug,
      String problemCategoryName) {
    this.problemId = problemId;
    this.problemCategoryId = problemCategoryId;
    this.problemCategorySlug = problemCategorySlug;
    this.problemCategoryName = problemCategoryName;
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
   * Gets the unique identifier of the problem category.
   *
   * @return the unique identifier of the problem category
   */
  public int getProblemCategoryId() {
    return problemCategoryId;
  }

  /**
   * Sets the unique identifier of the problem category.
   *
   * @param problemCategoryId - the unique identifier of the problem category
   */
  public void setProblemCategoryId(int problemCategoryId) {
    this.problemCategoryId = problemCategoryId;
  }

  /**
   * Gets the alias of the problem category.
   *
   * @return the alias of the problem category
   */
  public String getProblemCategorySlug() {
    return problemCategorySlug;
  }

  /**
   * Sets the alias of the problem category.
   *
   * @param problemCategorySlug - the alias of the problem category
   */
  public void setProblemCategorySlug(String problemCategorySlug) {
    this.problemCategorySlug = problemCategorySlug;
  }

  /**
   * Gets the name of the problem category.
   *
   * @return the name of the problem category
   */
  public String getProblemCategoryName() {
    return problemCategoryName;
  }

  /**
   * Sets the name of the problem category.
   *
   * @param problemCategoryName - the name of the problem category
   */
  public void setProblemCategoryName(String problemCategoryName) {
    this.problemCategoryName = problemCategoryName;
  }

  /* (non-Javadoc)
   * @see java.lang.Object#hashCode()
   */
  public int hashCode() {
    return (int) problemId + (int) problemCategoryId;
  }

  /* (non-Javadoc)
   * @see java.lang.Object#equals(java.lang.Object)
   */
  public boolean equals(Object obj) {
    if (obj instanceof ProblemCategoryRelationship) {
      ProblemCategoryRelationship anotherCategoryRelationship = (ProblemCategoryRelationship) obj;
      return anotherCategoryRelationship.getProblemId() == problemId
          && anotherCategoryRelationship.getProblemCategoryId() == problemCategoryId;
    }
    return false;
  }

  /* (non-Javadoc)
   * @see java.lang.Object#toString()
   */
  public String toString() {
    return String.format(
        "ProblemCategoryRelationship [ProblemID=%d, CategoryID=%d, Slug=%s, Name=%s]",
        new Object[] {problemId, problemCategoryId, problemCategorySlug, problemCategoryName});
  }

  /** The unique identifier of the problem. */
  private long problemId;

  /** The unique identifier of the problem category. */
  private int problemCategoryId;

  /** The alias of the problem category. */
  private String problemCategorySlug;

  /** The name of the problem category. */
  private String problemCategoryName;

  /** The unique serialization identifier. */
  private static final long serialVersionUID = 5108958918621777517L;
}
