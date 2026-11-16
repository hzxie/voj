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
 * The claim of a judging on its submission, as the steps of the judging see it: renewed in the
 * background while it runs (see {@link Dispatcher}), until it is found superseded.
 *
 * @author Haozhe Xie
 */
@FunctionalInterface
interface Lease {
  /**
   * Stops the judging if it was superseded.
   *
   * @throws Superseded if the judging was superseded
   */
  void check() throws Superseded;
}
