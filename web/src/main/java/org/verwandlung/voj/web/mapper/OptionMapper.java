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

import org.verwandlung.voj.web.model.Option;

/**
 * Opton Data Access Object.
 *
 * @author Haozhe Xie
 */
public interface OptionMapper {
  /**
   * Gets all system options.
   *
   * @return a list containing all system options
   */
  List<Option> getOptions();

  /**
   * Gets the system options that are loaded automatically.
   *
   * @return a list containing the auto-loaded system options
   */
  List<Option> getAutoloadOptions();

  /**
   * Gets the value of a system option by its name.
   *
   * @param optionName - the name of the system option
   * @return the corresponding Option object
   */
  Option getOption(@Param("optionName") String optionName);

  /**
   * Updates a system option.
   *
   * @param option - the system option object
   */
  int updateOption(Option option);
}
