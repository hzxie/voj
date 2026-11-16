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
   * @param scoring - the scoring rule of the contest (ICPC / IOI)
   */
  public Contest(
      String contestName, String contestNotes, Date startTime, Date endTime, String scoring) {
    this.contestName = contestName;
    this.contestNotes = contestNotes;
    this.startTime = startTime;
    this.endTime = endTime;
    setScoring(scoring);
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
   * Checks whether the contest uses the IOI scoring rule (ranked by score) rather than the ICPC one
   * (ranked by solved problems and penalty time).
   *
   * @return whether the contest uses the IOI scoring rule
   */
  public boolean isIoi() {
    return SCORING_IOI.equals(scoring);
  }

  /* (non-Javadoc)
   * @see java.lang.Object#hashCode()
   */
  @Override
  public int hashCode() {
    return Long.hashCode(contestId);
  }

  /* (non-Javadoc)
   * Two contests are the same contest when they have the same unique identifier.
   * @see java.lang.Object#equals(java.lang.Object)
   */
  @Override
  public boolean equals(Object o) {
    return o instanceof Contest && ((Contest) o).getContestId() == contestId;
  }

  /* (non-Javadoc)
   * @see java.lang.Object#toString()
   */
  public String toString() {
    return String.format(
        "Contest [ID=%d, Name=%s, StartTime=%s, EndTime=%s, Scoring=%s]",
        new Object[] {contestId, contestName, startTime, endTime, scoring});
  }

  /**
   * Gets the publication status of the contest (PUBLISHED / DRAFT / HIDDEN).
   *
   * @return the publication status of the contest
   */
  public String getStatus() {
    return status;
  }

  /**
   * Sets the publication status of the contest (PUBLISHED / DRAFT / HIDDEN).
   *
   * @param status - the publication status of the contest
   */
  public void setStatus(String status) {
    this.status = status;
  }

  /**
   * Gets whether the contest is publicly visible (i.e. its status is PUBLISHED). Derived from
   * {@link #getStatus()}; kept for visibility checks across the codebase.
   *
   * @return whether the contest is publicly visible
   */
  public boolean isPublic() {
    return PublicationStatus.isPublic(status);
  }

  /**
   * Gets the scoring rule of the contest (ICPC / IOI).
   *
   * @return the scoring rule of the contest
   */
  public String getScoring() {
    return scoring;
  }

  /**
   * Sets the scoring rule of the contest. Anything but IOI is ranked in the ICPC style, so it is
   * stored as ICPC.
   *
   * @param scoring - the scoring rule of the contest (ICPC / IOI)
   */
  public void setScoring(String scoring) {
    this.scoring = SCORING_IOI.equals(scoring) ? SCORING_IOI : SCORING_ICPC;
  }

  /**
   * Gets the penalty (in minutes) applied for each wrong submission.
   *
   * @return the penalty in minutes per wrong submission
   */
  public int getPenalty() {
    return penalty;
  }

  /**
   * Sets the penalty (in minutes) applied for each wrong submission.
   *
   * @param penalty - the penalty in minutes per wrong submission
   */
  public void setPenalty(int penalty) {
    this.penalty = penalty;
  }

  /**
   * Gets the scoreboard freeze duration (in minutes) before the contest ends.
   *
   * @return the freeze duration in minutes
   */
  public int getFreeze() {
    return freeze;
  }

  /**
   * Sets the scoreboard freeze duration (in minutes) before the contest ends.
   *
   * @param freeze - the freeze duration in minutes
   */
  public void setFreeze(int freeze) {
    this.freeze = freeze;
  }

  /**
   * Gets whether the contest is open for registration.
   *
   * @return whether the contest is open for registration
   */
  public boolean isOpenRegistration() {
    return openRegistration;
  }

  /**
   * Sets whether the contest is open for registration.
   *
   * @param openRegistration - whether the contest is open for registration
   */
  public void setOpenRegistration(boolean openRegistration) {
    this.openRegistration = openRegistration;
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

  /** The publication status of the contest (PUBLISHED / DRAFT / HIDDEN). */
  private String status = PublicationStatus.PUBLISHED;

  /** The ICPC scoring rule: ranked by solved problems, then penalty time. */
  public static final String SCORING_ICPC = "ICPC";

  /** The IOI scoring rule: ranked by the total score of the last submission to every problem. */
  public static final String SCORING_IOI = "IOI";

  /** The scoring rule of the contest (ICPC / IOI). */
  private String scoring = SCORING_ICPC;

  /** The penalty (in minutes) applied for each wrong submission. */
  private int penalty = 20;

  /** The scoreboard freeze duration (in minutes) before the contest ends. */
  private int freeze = 0;

  /** Whether the contest is open for registration. */
  private boolean openRegistration = true;

  /** The contest status, representing not started, in progress, and finished respectively. */
  public enum CONTEST_STATUS {
    READY,
    LIVE,
    DONE
  }
}
