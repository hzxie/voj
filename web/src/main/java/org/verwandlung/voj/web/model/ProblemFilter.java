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

/**
 * The conditions of a problem search, shared by the public problem set and the administration
 * console. An empty (or null) condition does not filter; a category, tag or difficulty that does not
 * exist matches no problem.
 *
 * @author Haozhe Xie
 */
public class ProblemFilter {
  /**
   * Matches the problems whose name contains a keyword.
   *
   * @param keyword - the keyword
   * @return this filter
   */
  public ProblemFilter withKeyword(String keyword) {
    this.keyword = keyword;
    return this;
  }

  /**
   * Matches the problems whose identifier contains a number (as typed in the problem set).
   *
   * @param problemIdQuery - the digits to look for
   * @return this filter
   */
  public ProblemFilter withProblemIdQuery(String problemIdQuery) {
    this.problemIdQuery = problemIdQuery;
    return this;
  }

  /**
   * Matches the problems of a category.
   *
   * @param problemCategorySlug - the slug of the category
   * @return this filter
   */
  public ProblemFilter withCategory(String problemCategorySlug) {
    this.problemCategorySlug = problemCategorySlug;
    return this;
  }

  /**
   * Matches the problems with a tag.
   *
   * @param problemTag - the slug or the name of the tag
   * @return this filter
   */
  public ProblemFilter withTag(String problemTag) {
    this.problemTag = problemTag;
    return this;
  }

  /**
   * Matches the problems of a difficulty.
   *
   * @param problemDifficultySlug - the slug of the difficulty
   * @return this filter
   */
  public ProblemFilter withDifficulty(String problemDifficultySlug) {
    this.problemDifficultySlug = problemDifficultySlug;
    return this;
  }

  /**
   * Matches the published problems only, and counts their submissions as the public sees them
   * (without the submissions of running contests).
   *
   * @return this filter
   */
  public ProblemFilter publicOnly() {
    this.isPublicOnly = true;
    return this;
  }

  /** @return the keyword of the problem name, or null */
  public String getKeyword() {
    return keyword;
  }

  /** @return the digits of the problem identifier, or null */
  public String getProblemIdQuery() {
    return problemIdQuery;
  }

  /** @return the slug of the category, or null */
  public String getProblemCategorySlug() {
    return problemCategorySlug;
  }

  /** @return the slug or name of the tag, or null */
  public String getProblemTag() {
    return problemTag;
  }

  /** @return the slug of the difficulty, or null */
  public String getProblemDifficultySlug() {
    return problemDifficultySlug;
  }

  /** @return whether only the published problems match */
  public boolean isPublicOnly() {
    return isPublicOnly;
  }

  /** The keyword of the problem name. */
  private String keyword;

  /** The digits of the problem identifier. */
  private String problemIdQuery;

  /** The slug of the category. */
  private String problemCategorySlug;

  /** The slug or name of the tag. */
  private String problemTag;

  /** The slug of the difficulty. */
  private String problemDifficultySlug;

  /** Whether only the published problems match. */
  private boolean isPublicOnly;
}
