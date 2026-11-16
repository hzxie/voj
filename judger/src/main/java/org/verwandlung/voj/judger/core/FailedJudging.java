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
package org.verwandlung.voj.judger.core;

/**
 * A judging that failed for a known reason (see {@link Dispatcher}). Its message is the reason,
 * shown to the user in the System Error of the submission if it is the last of too many failed
 * judgings.
 *
 * @author Haozhe Xie
 */
final class FailedJudging extends Exception {
  /**
   * A failed judging that the submission may have caused, which counts on it.
   *
   * @param reason - the reason
   * @param cause - the underlying exception, or {@code null}
   * @return the failed judging
   */
  static FailedJudging ofSubmission(String reason, Throwable cause) {
    return new FailedJudging(reason, cause, true);
  }

  /**
   * A failed judging of the judger or its environment, which does not count on the submission.
   *
   * @param reason - the reason
   * @param cause - the underlying exception, or {@code null}
   * @return the failed judging
   */
  static FailedJudging ofJudger(String reason, Throwable cause) {
    return new FailedJudging(reason, cause, false);
  }

  /**
   * Constructor.
   *
   * @param reason - the reason
   * @param cause - the underlying exception, or {@code null}
   * @param countsOnSubmission - whether it counts on the submission
   */
  private FailedJudging(String reason, Throwable cause, boolean countsOnSubmission) {
    super(reason, cause);
    this.countsOnSubmission = countsOnSubmission;
  }

  /**
   * Tells whether the failed judging counts on the submission.
   *
   * @return whether the submission may have caused it
   */
  boolean countsOnSubmission() {
    return countsOnSubmission;
  }

  /** Whether the failed judging counts on the submission. */
  private final boolean countsOnSubmission;

  /** The unique serialization identifier. */
  private static final long serialVersionUID = 1L;
}
