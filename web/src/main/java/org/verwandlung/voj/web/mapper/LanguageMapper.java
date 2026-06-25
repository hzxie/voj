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

import org.verwandlung.voj.web.model.Language;

/**
 * Language Data Access Object.
 *
 * @author Haozhe Xie
 */
public interface LanguageMapper {
  /**
   * Gets a programming language object by its unique identifier.
   *
   * @param languageId - the unique identifier of the programming language
   * @return the expected programming language object, or a null reference
   */
  Language getLanguageUsingId(@Param("languageId") int languageId);

  /**
   * Gets a programming language object by its alias.
   *
   * @param languageSlug - the alias of the programming language
   * @return the expected programming language object, or a null reference
   */
  Language getLanguageUsingSlug(@Param("languageSlug") String languageSlug);

  /**
   * Gets the supported programming languages.
   *
   * @return the list of programming languages (a List<Language> object)
   */
  List<Language> getAllLanguages();

  /**
   * Adds a programming language object.
   *
   * @param language - the programming language object to add
   */
  int createLanguage(Language language);

  /**
   * Updates a programming language object.
   *
   * @param language - the programming language object to update
   */
  int updateLanguage(Language language);

  /**
   * Deletes a programming language object.
   *
   * @param languageId - the unique identifier of the programming language
   */
  int deleteLanguage(@Param("languageId") int languageId);
}
