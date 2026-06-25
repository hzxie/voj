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

public class ContestSubmission {
  /** Default constructor of the contest submission. */
  public ContestSubmission() {}

  /**
   * Constructor of the contest submission.
   *
   * @param contest - the contest
   * @param submission - the submission
   */
  public ContestSubmission(Contest contest, Submission submission) {
    this.contest = contest;
    this.submission = submission;
  }

  /**
   * Gets the contest.
   *
   * @return the contest
   */
  public Contest getContest() {
    return contest;
  }

  /**
   * Sets the contest.
   *
   * @param contest - the contest
   */
  public void setContest(Contest contest) {
    this.contest = contest;
  }

  /**
   * Gets the submission.
   *
   * @return the submission
   */
  public Submission getSubmission() {
    return submission;
  }

  /**
   * Sets the submission.
   *
   * @param submission - the submission
   */
  public void setSubmission(Submission submission) {
    this.submission = submission;
  }

  /* (non-Javadoc)
   * @see java.lang.Object#toString()
   */
  public String toString() {
    return String.format(
        "ContestSubmission [Contest={%s}, Submission={%s}]", new Object[] {contest, submission});
  }

  /** The contest. */
  private Contest contest;

  /** The submission. */
  private Submission submission;
}
