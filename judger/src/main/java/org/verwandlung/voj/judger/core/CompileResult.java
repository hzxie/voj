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
 * The outcome of the compilation of a submission.
 *
 * @param verdict - how the compiler ended ({@link Verdict#INTERNAL_ERROR}: the judger failed)
 * @param log - the compile log shown to the user
 * @author Haozhe Xie
 */
public record CompileResult(Verdict verdict, String log) {
  /**
   * Tells whether the submission compiled.
   *
   * @return whether the compiler exited with 0 within its limits
   */
  public boolean isSuccessful() {
    return verdict == Verdict.NORMAL;
  }
}
