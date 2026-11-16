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
 * The verdict of a submission on one checkpoint.
 *
 * @param checkpointId - the identifier of the checkpoint
 * @param judgeResult - the slug of the judge result (e.g. {@code AC} or {@code TLE})
 * @param usedTime - the CPU time the run used (ms)
 * @param usedMemory - the memory the run used (KB)
 * @param score - the score it earned: the checkpoint's if Accepted, else 0
 * @author Haozhe Xie
 */
public record CheckpointResult(
    int checkpointId, String judgeResult, int usedTime, int usedMemory, int score) {
  /** The slug of the judge result of an Accepted checkpoint. */
  public static final String ACCEPTED = "AC";

  /**
   * Tells whether the submission passed the checkpoint.
   *
   * @return whether the judge result is Accepted
   */
  public boolean isAccepted() {
    return ACCEPTED.equals(judgeResult);
  }
}
