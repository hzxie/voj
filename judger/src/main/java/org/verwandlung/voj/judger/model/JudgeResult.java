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
 * The model of a judge result. Maps to the voj_judge_results table in the database.
 *
 * @author Haozhe Xie
 */
public class JudgeResult implements Serializable {
  /** Default constructor of the judge result. */
  public JudgeResult() {}

  /**
   * Constructor of the judge result.
   *
   * @param judgeResultId - the unique identifier of the judge result
   * @param judgeResultSlug - the unique English abbreviation of the judge result
   * @param judgeResultName - the name of the judge result
   */
  public JudgeResult(int judgeResultId, String judgeResultSlug, String judgeResultName) {
    this.judgeResultId = judgeResultId;
    this.judgeResultSlug = judgeResultSlug;
    this.judgeResultName = judgeResultName;
  }

  /**
   * Gets the unique identifier of the judge result.
   *
   * @return the unique identifier of the judge result
   */
  public int getJudgeResultId() {
    return judgeResultId;
  }

  /**
   * Sets the unique identifier of the judge result.
   *
   * @param judgeResultId - the unique identifier of the judge result
   */
  public void setJudgeResultId(int judgeResultId) {
    this.judgeResultId = judgeResultId;
  }

  /**
   * Gets the unique English abbreviation of the judge result.
   *
   * @return the unique English abbreviation of the judge result
   */
  public String getJudgeResultSlug() {
    return judgeResultSlug;
  }

  /**
   * Sets the unique English abbreviation of the judge result.
   *
   * @param judgeResultSlug - the unique English abbreviation of the judge result
   */
  public void setJudgeResultSlug(String judgeResultSlug) {
    this.judgeResultSlug = judgeResultSlug;
  }

  /**
   * Gets the name of the judge result.
   *
   * @return the name of the judge result
   */
  public String getJudgeResultName() {
    return judgeResultName;
  }

  /**
   * Sets the name of the judge result.
   *
   * @param judgeResultName - the name of the judge result
   */
  public void setJudgeResultName(String judgeResultName) {
    this.judgeResultName = judgeResultName;
  }

  /* (non-Javadoc)
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    return String.format(
        "JudgeResult [Id=%d, Slug=%s, Name=%s]",
        new Object[] {judgeResultId, judgeResultSlug, judgeResultName});
  }

  /** The unique identifier of the judge result. */
  private int judgeResultId;

  /** The unique English abbreviation of the judge result. */
  private String judgeResultSlug;

  /** The name of the judge result. */
  private String judgeResultName;

  /** The unique serialization identifier. */
  private static final long serialVersionUID = -1572920076997918781L;
}
