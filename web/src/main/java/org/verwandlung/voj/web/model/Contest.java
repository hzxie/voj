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

import java.util.Date;

public class Contest {
  /** Default constructor of the contest. */
  public Contest() {}

  /**
   * Constructor of the contest.
   *
   * @param contestName - the name of the contest
   * @param contestNotes - the notes of the contest
   * @param startTime - the start time of the contest
   * @param endTime - the end time of the contest
   * @param mode - the mode of the contest
   * @param problems - the problems included in the contest
   */
  public Contest(
      String contestName,
      String contestNotes,
      Date startTime,
      Date endTime,
      String mode,
      String problems) {
    this.contestName = contestName;
    this.contestNotes = contestNotes;
    this.startTime = startTime;
    this.endTime = endTime;
    this.contestMode = mode;
    this.problems = problems;
  }

  /**
   * Constructor of the contest.
   *
   * @param contestId - the unique identifier of the contest
   * @param contestName - the name of the contest
   * @param contestNotes - the notes of the contest
   * @param startTime - the start time of the contest
   * @param endTime - the end time of the contest
   * @param mode - the mode of the contest
   * @param problems - the problems included in the contest
   */
  public Contest(
      long contestId,
      String contestNotes,
      String contestName,
      Date startTime,
      Date endTime,
      String mode,
      String problems) {
    this(contestName, contestNotes, startTime, endTime, mode, problems);
    this.contestId = contestId;
  }

  /**
   * Gets the unique identifier of the contest.
   *
   * @return the unique identifier of the contest
   */
  public long getContestId() {
    return contestId;
  }

  /**
   * Sets the unique identifier of the contest.
   *
   * @param contestId - the unique identifier of the contest
   */
  public void setContestId(long contestId) {
    this.contestId = contestId;
  }

  /**
   * Gets the name of the contest.
   *
   * @return the name of the contest
   */
  public String getContestName() {
    return contestName;
  }

  /**
   * Sets the name of the contest.
   *
   * @param contestName - the name of the contest
   */
  public void setContestName(String contestName) {
    this.contestName = contestName;
  }

  /**
   * Gets the notes of the contest.
   *
   * @return the notes of the contest
   */
  public String getContestNotes() {
    return contestNotes;
  }

  /**
   * Sets the notes of the contest.
   *
   * @param contestNotes - the notes of the contest
   */
  public void setContestNotes(String contestNotes) {
    this.contestNotes = contestNotes;
  }

  /**
   * Gets the start time of the contest.
   *
   * @return the start time of the contest
   */
  public Date getStartTime() {
    return startTime;
  }

  /**
   * Sets the start time of the contest.
   *
   * @param startTime - the start time of the contest
   */
  public void setStartTime(Date startTime) {
    this.startTime = startTime;
  }

  /**
   * Gets the end time of the contest.
   *
   * @return the end time of the contest
   */
  public Date getEndTime() {
    return endTime;
  }

  /**
   * Sets the end time of the contest.
   *
   * @param endTime - the end time of the contest
   */
  public void setEndTime(Date endTime) {
    this.endTime = endTime;
  }

  /**
   * Gets the mode of the contest.
   *
   * @return the mode of the contest
   */
  public String getContestMode() {
    return contestMode;
  }

  /**
   * Sets the mode of the contest.
   *
   * @param contestMode - the mode of the contest
   */
  public void setContestMode(String contestMode) {
    this.contestMode = contestMode;
  }

  /* (non-Javadoc)
   * @see java.lang.Object#hashCode()
   */
  public int hashCode() {
    return (int) (contestId % 37 + contestId % 1001);
  }

  /* (non-Javadoc)
   * @see java.lang.Object#equals(java.lang.Object)
   */
  public boolean equals(Object o) {
    if (!(o instanceof Contest)) {
      return false;
    }
    Contest c = (Contest) o;
    return c.hashCode() == hashCode();
  }

  /* (non-Javadoc)
   * @see java.lang.Object#toString()
   */
  public String toString() {
    return String.format(
        "Contest [ID=%d, Name=%s, StartTime=%s, EndTime=%s, Mode=%s]",
        new Object[] {contestId, contestName, startTime, endTime, contestMode});
  }

  /**
   * Gets the problems included in the contest.
   *
   * @return the problems included in the contest
   */
  public String getProblems() {
    return problems;
  }

  /**
   * Sets the problems included in the contest.
   *
   * @param problems - the problems included in the contest
   */
  public void setProblems(String problems) {
    this.problems = problems;
  }

  /** The unique identifier of the contest. */
  private long contestId;

  /** The name of the contest. */
  private String contestName;

  /** The notes of the contest. */
  private String contestNotes;

  /** The start time of the contest. */
  private Date startTime;

  /** The end time of the contest. */
  private Date endTime;

  /** The mode of the contest. The available modes are: ACM and OI. */
  private String contestMode;

  /** The problems included in the contest (a JSON-formatted string). */
  private String problems;

  /** The contest status, representing not started, in progress, and finished respectively. */
  public enum CONTEST_STATUS {
    READY,
    LIVE,
    DONE
  }
}
