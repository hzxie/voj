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
 * The model of a problem tag relationship. Maps to the voj_problem_tag_relationships table in the
 * database.
 *
 * @author Haozhe Xie
 */
public class ProblemTagRelationship implements Serializable {
  /** Default constructor of the problem tag relationship. */
  public ProblemTagRelationship() {}

  /**
   * Constructor of the problem tag relationship.
   *
   * @param problemId - the unique identifier of the problem
   * @param problemTagId - the unique identifier of the problem tag
   * @param problemTagSlug - the alias of the problem tag
   * @param problemTagName - the name of the problem tag
   */
  public ProblemTagRelationship(
      long problemId, long problemTagId, String problemTagSlug, String problemTagName) {
    this.problemId = problemId;
    this.problemTagId = problemTagId;
    this.problemTagSlug = problemTagSlug;
    this.problemTagName = problemTagName;
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
   * Gets the unique identifier of the problem tag.
   *
   * @return the unique identifier of the problem tag
   */
  public long getProblemTagId() {
    return problemTagId;
  }

  /**
   * Sets the unique identifier of the problem tag.
   *
   * @param problemTagId - the unique identifier of the problem tag
   */
  public void setProblemTagId(long problemTagId) {
    this.problemTagId = problemTagId;
  }

  /**
   * Gets the alias of the problem tag.
   *
   * @return the alias of the problem tag
   */
  public String getProblemTagSlug() {
    return problemTagSlug;
  }

  /**
   * Sets the alias of the problem tag.
   *
   * @param problemTagSlug - the alias of the problem tag
   */
  public void setProblemTagSlug(String problemTagSlug) {
    this.problemTagSlug = problemTagSlug;
  }

  /**
   * Gets the name of the problem tag.
   *
   * @return the name of the problem tag
   */
  public String getProblemTagName() {
    return problemTagName;
  }

  /**
   * Sets the name of the problem tag.
   *
   * @param problemTagName - the name of the problem tag
   */
  public void setProblemTagName(String problemTagName) {
    this.problemTagName = problemTagName;
  }

  /* (non-Javadoc)
   * @see java.lang.Object#hashCode()
   */
  public int hashCode() {
    return (int) problemId + (int) problemTagId;
  }

  /* (non-Javadoc)
   * @see java.lang.Object#equals(java.lang.Object)
   */
  public boolean equals(Object obj) {
    if (obj instanceof ProblemTagRelationship) {
      ProblemTagRelationship anotherTagRelationship = (ProblemTagRelationship) obj;
      return anotherTagRelationship.getProblemId() == problemId
          && anotherTagRelationship.getProblemTagId() == problemTagId;
    }
    return false;
  }

  /* (non-Javadoc)
   * @see java.lang.Object#toString()
   */
  public String toString() {
    return String.format(
        "ProblemTagRelationship [ProblemID=%d, TagID=%d, Slug=%s, Name=%s]",
        new Object[] {problemId, problemTagId, problemTagSlug, problemTagName});
  }

  /** The unique identifier of the problem. */
  private long problemId;

  /** The unique identifier of the problem tag. */
  private long problemTagId;

  /** The alias of the problem tag. */
  private String problemTagSlug;

  /** The name of the problem tag. */
  private String problemTagName;

  /** The unique serialization identifier. */
  private static final long serialVersionUID = -1636820984815816993L;
}
