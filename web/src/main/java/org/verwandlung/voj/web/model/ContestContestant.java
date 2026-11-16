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

public class ContestContestant implements Comparable {
  /** Default constructor of the contest contestant. */
  public ContestContestant() {}

  /**
   * Constructor of the contest contestant.
   *
   * @param contest - the contest
   * @param contestant - the contestant
   */
  public ContestContestant(Contest contest, User contestant) {
    this.contest = contest;
    this.contestant = contestant;
  }

  /**
   * Gets the contest object.
   *
   * @return the contest object
   */
  public Contest getContest() {
    return contest;
  }

  /**
   * Sets the contest object.
   *
   * @param contest - the contest object
   */
  public void setContest(Contest contest) {
    this.contest = contest;
  }

  /**
   * Gets the contestant object.
   *
   * @return the contestant object
   */
  public User getContestant() {
    return contestant;
  }

  /**
   * Sets the contestant object.
   *
   * @param contestant - the contestant object
   */
  public void setContestant(User contestant) {
    this.contestant = contestant;
  }

  /**
   * Gets the score.
   *
   * @return the score
   */
  public int getScore() {
    return score;
  }

  /**
   * Gets the running time (OI) or penalty time (ACM).
   *
   * @return the running time (OI) or penalty time (ACM)
   */
  public long getTime() {
    return time;
  }

  /**
   * Sets the penalty time (ACM).
   *
   * @param time the penalty time (ACM)
   */
  public void setTime(long time) {
    this.time = time;
  }

  /**
   * Sets the rank in the contest.
   *
   * @param rank - the rank in the contest
   */
  public void setRank(long rank) {
    this.rank = rank;
  }

  /**
   * Gets the rank in the contest.
   *
   * @return the rank in the contest
   */
  public long getRank() {
    return rank;
  }

  /* (non-Javadoc)
   * @see java.lang.Object#compareTo(Object)
   */
  public int compareTo(Object o) {
    if (!(o instanceof ContestContestant)) {
      throw new ClassCastException("A ContestContestant object expected.");
    }

    ContestContestant occ = (ContestContestant) o;
    if (occ.getScore() != this.score) {
      return Integer.compare(occ.getScore(), this.score);
    }
    if (occ.getTime() != this.time) {
      return Long.compare(this.time, occ.getTime());
    }
    // Full tie: order by UID so the comparison stays a consistent total order (the two
    // contestants still share a rank, see ContestService#rankingContestants).
    long uid = this.contestant != null ? this.contestant.getUid() : 0;
    long otherUid = occ.getContestant() != null ? occ.getContestant().getUid() : 0;
    return Long.compare(uid, otherUid);
  }

  /* (non-Javadoc)
   * @see java.lang.Object#toString()
   */
  public String toString() {
    return String.format(
        "ContestContestant [Contest={%s}, Contestant={%s}, Score=%s, Time=%s (ms)]",
        new Object[] {contest, contestant, score, time});
  }

  /** The contest. */
  private Contest contest;

  /** The contestant. */
  private User contestant;

  /** The score. */
  private int score;

  /** The running time in the OI mode. The penalty time in the ACM mode. */
  private long time;

  /** The rank in the contest. */
  private long rank;
}
