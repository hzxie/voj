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
 * The model of a problem category. Maps to the voj_problem_categories table in the database.
 *
 * @author Haozhe Xie
 */
public class ProblemCategory implements Serializable {
  /** Default constructor of the problem category. */
  public ProblemCategory() {}

  /** Constructor of the problem category. */
  public ProblemCategory(
      String problemCategorySlug, String problemCategoryName, int parentProblemCategoryId) {
    this.problemCategorySlug = problemCategorySlug;
    this.problemCategoryName = problemCategoryName;
    this.parentProblemCategoryId = parentProblemCategoryId;
  }

  /** Constructor of the problem category. */
  public ProblemCategory(
      int problemCategoryId,
      String problemCategorySlug,
      String problemCategoryName,
      int parentProblemCategoryId) {
    this(problemCategorySlug, problemCategoryName, parentProblemCategoryId);
    this.problemCategoryId = problemCategoryId;
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

  /**
   * Gets the unique identifier of the parent problem category.
   *
   * @return the unique identifier of the parent problem category
   */
  public int getParentProblemCategoryId() {
    return parentProblemCategoryId;
  }

  /**
   * Sets the unique identifier of the parent problem category.
   *
   * @param parentProblemCategoryId - the unique identifier of the parent problem category
   */
  public void setParentProblemCategory(int parentProblemCategoryId) {
    this.parentProblemCategoryId = parentProblemCategoryId;
  }

  /* (non-Javadoc)
   * @see java.lang.Object#hashCode()
   */
  public int hashCode() {
    return problemCategoryId;
  }

  /* (non-Javadoc)
   * @see java.lang.Object#equals(java.lang.Object)
   */
  public boolean equals(Object obj) {
    if (obj instanceof ProblemCategory) {
      ProblemCategory anotherCategory = (ProblemCategory) obj;
      return anotherCategory.getProblemCategoryId() == problemCategoryId;
    }
    return false;
  }

  /* (non-Javadoc)
   * @see java.lang.Object#toString()
   */
  public String toString() {
    return String.format(
        "ProblemCategory [ID=%d, Slug=%s, Name=%s, ParentID=%d]",
        new Object[] {
          problemCategoryId, problemCategorySlug, problemCategoryName, parentProblemCategoryId
        });
  }

  /** The unique identifier of the problem category. */
  private int problemCategoryId;

  /** The alias of the problem category. */
  private String problemCategorySlug;

  /** The name of the problem category. */
  private String problemCategoryName;

  /** The unique identifier of the parent problem category. */
  private int parentProblemCategoryId;

  /** The unique serialization identifier. */
  private static final long serialVersionUID = 7093703712360669823L;
}
