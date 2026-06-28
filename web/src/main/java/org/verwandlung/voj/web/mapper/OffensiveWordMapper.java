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
package org.verwandlung.voj.web.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Param;

/**
 * The offensive word Data Access Object. The offensive words form the dictionary used to build the
 * {@link org.verwandlung.voj.web.util.OffensiveWordFilter} DFA; they are usually imported in bulk
 * from external word lists rather than maintained by hand.
 *
 * @author Haozhe Xie
 */
public interface OffensiveWordMapper {
  /**
   * Gets all offensive words.
   *
   * @return a list containing all offensive words
   */
  List<String> getOffensiveWords();

  /**
   * Gets the total number of offensive words in the dictionary.
   *
   * @return the total number of offensive words
   */
  long getNumberOfOffensiveWords();

  /** Deletes all offensive words. Used before a fresh bulk import replaces the whole dictionary. */
  void deleteAllOffensiveWords();

  /**
   * Inserts a batch of offensive words in a single statement.
   *
   * @param offensiveWords - the (de-duplicated) offensive words to insert
   * @return the number of rows inserted
   */
  int batchCreateOffensiveWords(@Param("offensiveWords") List<String> offensiveWords);
}
