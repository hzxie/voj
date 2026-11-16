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
 * A judging that was superseded: the submission was rejudged, or claimed by another judging as the
 * lease of the judging had expired. It stops without recording or sending anything more, and is
 * neither an outcome nor a failure of its judger.
 *
 * @author Haozhe Xie
 */
public final class Superseded extends Exception {
  /** Constructor. */
  public Superseded() {
    super(null, null, false, false);
  }

  /** The unique serialization identifier. */
  private static final long serialVersionUID = 1L;
}
