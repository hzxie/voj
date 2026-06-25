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
package org.verwandlung.voj.judger.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.Select;

import org.verwandlung.voj.judger.model.Language;

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
  @Select("SELECT * FROM voj_languages WHERE language_id = #{languageId}")
  @Options(useCache = true)
  @Results({
    @Result(property = "languageId", column = "language_id"),
    @Result(property = "languageSlug", column = "language_slug"),
    @Result(property = "languageName", column = "language_name"),
    @Result(property = "compileCommand", column = "language_compile_command"),
    @Result(property = "runCommand", column = "language_run_command"),
  })
  Language getLanguageUsingId(@Param("languageId") int languageId);

  /**
   * Gets a programming language object by its unique English abbreviation.
   *
   * @param languageSlug - the unique English abbreviation of the programming language
   * @return the expected programming language object, or a null reference
   */
  @Select("SELECT * FROM voj_languages WHERE language_slug = #{languageSlug}")
  @Options(useCache = true)
  @Results({
    @Result(property = "languageId", column = "language_id"),
    @Result(property = "languageSlug", column = "language_slug"),
    @Result(property = "languageName", column = "language_name"),
    @Result(property = "compileCommand", column = "language_compile_command"),
    @Result(property = "runCommand", column = "language_run_command"),
  })
  Language getLanguageUsingSlug(@Param("languageSlug") String languageSlug);

  /**
   * Gets the supported programming languages.
   *
   * @return the list of programming languages (a List<Language> object)
   */
  @Select("SELECT * FROM voj_languages")
  @Options(useCache = true)
  @Results({
    @Result(property = "languageId", column = "language_id"),
    @Result(property = "languageSlug", column = "language_slug"),
    @Result(property = "languageName", column = "language_name"),
    @Result(property = "compileCommand", column = "language_compile_command"),
    @Result(property = "runCommand", column = "language_run_command"),
  })
  List<Language> getAllLanguages();
}
