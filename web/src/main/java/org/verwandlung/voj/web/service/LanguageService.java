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
package org.verwandlung.voj.web.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.verwandlung.voj.web.mapper.LanguageMapper;
import org.verwandlung.voj.web.model.Language;

/**
 * The business logic layer of the Language class.
 *
 * @author Haozhe Xie
 */
@Service
@Transactional
public class LanguageService {
  /**
   * Gets the supported programming languages.
   *
   * @return the list of programming languages (a List<Language> object)
   */
  public List<Language> getAllLanguages() {
    return languageMapper.getAllLanguages();
  }

  /**
   * Gets the enabled programming languages, i.e. the ones offered to users when submitting. The
   * administration pages keep using {@link #getAllLanguages()} so disabled languages stay editable.
   *
   * @return the list of enabled programming languages
   */
  public List<Language> getEnabledLanguages() {
    return languageMapper.getEnabledLanguages();
  }

  /**
   * Updates the programming language options: the languages are replaced by the given ones (a
   * language without an identifier is created, one left out is deleted), unless one of them fails a
   * check.
   *
   * @param languages - the array containing the programming language settings
   * @return {@code isSuccessful}, and either the changes ({@code languageCreated}, {@code
   *     languageUpdated} and {@code languageDeleted}) or the {@code errors} (see {@link
   *     #getLanguageSettingErrors})
   */
  public Map<String, Object> updateLanguageSettings(List<Language> languages) {
    List<Language> previousLanguages = languageMapper.getAllLanguages();
    List<Map<String, String>> errors = getLanguageSettingErrors(previousLanguages, languages);
    Map<String, Object> result = new HashMap<>();
    if (errors.isEmpty()) {
      Map<String, List<Language>> languageChanges =
          getLanguageChanges(previousLanguages, languages);

      createLanguages(languageChanges.get("languageCreated"));
      updateLanguages(languageChanges.get("languageUpdated"));
      deleteLanguages(languageChanges.get("languageDeleted"));
      result.putAll(languageChanges);
    } else {
      result.put("errors", errors);
    }
    result.put("isSuccessful", errors.isEmpty());
    return result;
  }

  /**
   * Checks the programming language settings. A language that would be deleted must not be in use
   * (which only those are asked); every language that would remain needs a slug of 1 to 16
   * characters that no other language has, a name of 1 to 16 characters, compile and run
   * commands of at most 128 characters with the {@code {filename}} placeholder, and a source file
   * name (see {@link #isSourceFilenameLegal}).
   *
   * @param previousLanguages - the list of language settings before the update
   * @param newLanguages - the list of language settings after the update
   * @return the failed checks, each with the name of its language ({@code languageName}) and the
   *     check ({@code error}): {@code languageInUse}, {@code languageSlugIllegal}, {@code
   *     languageSlugExists}, {@code languageNameIllegal}, {@code compileCommandIllegal}, {@code
   *     runCommandIllegal} or {@code sourceFilenameIllegal}
   */
  private List<Map<String, String>> getLanguageSettingErrors(
      List<Language> previousLanguages, List<Language> newLanguages) {
    List<Map<String, String>> errors = new ArrayList<>();
    for (Language language : previousLanguages) {
      if (isLanguageDeleted(newLanguages, language)
          && languageMapper.isLanguageInUse(language.getLanguageId())) {
        errors.add(getLanguageSettingError(language, "languageInUse"));
      }
    }
    for (Language language : newLanguages) {
      if (!isLanguageSlugLegal(language.getLanguageSlug())) {
        errors.add(getLanguageSettingError(language, "languageSlugIllegal"));
      } else if (isLanguageSlugExists(language.getLanguageSlug(), language.getLanguageId())) {
        errors.add(getLanguageSettingError(language, "languageSlugExists"));
      }
      if (!isLanguageNameLegal(language.getLanguageName())) {
        errors.add(getLanguageSettingError(language, "languageNameIllegal"));
      }
      if (!isCompileCommandLegal(language.getCompileCommand())) {
        errors.add(getLanguageSettingError(language, "compileCommandIllegal"));
      }
      if (!isRunCommandLegal(language.getRunCommand())) {
        errors.add(getLanguageSettingError(language, "runCommandIllegal"));
      }
      if (!isSourceFilenameLegal(language.getSourceFilename())) {
        errors.add(getLanguageSettingError(language, "sourceFilenameIllegal"));
      }
    }
    return errors;
  }

  /**
   * Describes a failed check of the programming language settings.
   *
   * @param language - the programming language that fails it
   * @param error - the check
   * @return the name of the language ({@code languageName}) and the check ({@code error})
   */
  private static Map<String, String> getLanguageSettingError(Language language, String error) {
    return Map.of("languageName", language.getLanguageName(), "error", error);
  }

  /**
   * Checks the changes in the programming language settings.
   *
   * @param previousLanguages - the programming language settings before the update
   * @param newLanguages - the programming language settings after the update
   * @return a Map<String, List<Language>> object containing the changes in the language settings
   */
  private Map<String, List<Language>> getLanguageChanges(
      List<Language> previousLanguages, List<Language> newLanguages) {
    List<Language> languageCreated = new ArrayList<>();
    List<Language> languageUpdated = new ArrayList<>();
    List<Language> languageDeleted = new ArrayList<>();

    for (Language language : newLanguages) {
      int languageId = language.getLanguageId();
      if (languageId == 0) {
        languageCreated.add(language);
      } else if (isLanguageUpdated(previousLanguages, language)) {
        languageUpdated.add(language);
      }
    }
    for (Language pLanguage : previousLanguages) {
      int languageId = pLanguage.getLanguageId();
      if (languageId != 0 && isLanguageDeleted(newLanguages, pLanguage)) {
        languageDeleted.add(pLanguage);
      }
    }
    Map<String, List<Language>> languageChanges = new HashMap<String, List<Language>>();
    languageChanges.put("languageCreated", languageCreated);
    languageChanges.put("languageUpdated", languageUpdated);
    languageChanges.put("languageDeleted", languageDeleted);
    return languageChanges;
  }

  /**
   * Checks whether a programming language has been updated.
   *
   * @param previousLanguages - the list of programming languages before the update
   * @param language - the programming language object to check
   * @return whether the programming language has been updated
   */
  private boolean isLanguageUpdated(List<Language> previousLanguages, Language language) {
    Language previousLanguage = null;
    for (Language pLanguage : previousLanguages) {
      if (pLanguage.getLanguageId() == language.getLanguageId()) {
        previousLanguage = pLanguage;
        break;
      }
    }

    if (previousLanguage == null) {
      return false;
    }
    return !previousLanguage.getLanguageSlug().equals(language.getLanguageSlug())
        || !previousLanguage.getLanguageName().equals(language.getLanguageName())
        || !previousLanguage.getCompileCommand().equals(language.getCompileCommand())
        || !previousLanguage.getRunCommand().equals(language.getRunCommand())
        || previousLanguage.isEnabled() != language.isEnabled()
        || !previousLanguage.getSourceFilename().equals(language.getSourceFilename())
        || previousLanguage.getTimeMultiplier() != language.getTimeMultiplier()
        || previousLanguage.getMemoryMultiplier() != language.getMemoryMultiplier();
  }

  /**
   * Checks whether a programming language has been deleted from the list.
   *
   * @param newLanguages - the list of programming languages after the update
   * @param language - the programming language object to check
   * @return whether the programming language has been deleted from the list
   */
  private boolean isLanguageDeleted(List<Language> newLanguages, Language language) {
    int languageId = language.getLanguageId();
    for (Language newLanguage : newLanguages) {
      if (newLanguage.getLanguageId() == languageId) {
        return false;
      }
    }
    return true;
  }

  /**
   * Creates programming languages.
   *
   * @param languages - the programming languages to create
   */
  private void createLanguages(List<Language> languages) {
    if (languages == null || languages.isEmpty()) {
      return;
    }
    for (Language language : languages) {
      languageMapper.createLanguage(language);
    }
  }

  /**
   * Updates programming languages.
   *
   * @param languages - the programming languages to update
   */
  private void updateLanguages(List<Language> languages) {
    if (languages == null || languages.isEmpty()) {
      return;
    }
    for (Language language : languages) {
      languageMapper.updateLanguage(language);
    }
  }

  /**
   * Deletes programming languages.
   *
   * @param languages - the programming languages to delete
   */
  private void deleteLanguages(List<Language> languages) {
    if (languages == null || languages.isEmpty()) {
      return;
    }
    for (Language language : languages) {
      int languageId = language.getLanguageId();
      languageMapper.deleteLanguage(languageId);
    }
  }

  /**
   * Checks whether a programming language alias is legal: 1 to 16 characters.
   *
   * @param languageSlug - the alias of the programming language
   * @return whether the programming language alias is legal
   */
  private boolean isLanguageSlugLegal(String languageSlug) {
    return !languageSlug.isEmpty() && languageSlug.length() <= 16;
  }

  /**
   * Checks whether a programming language alias already exists.
   *
   * @param languageSlug - the alias of the programming language
   * @param languageId - the unique identifier of the programming language
   * @return whether the programming language alias already exists
   */
  private boolean isLanguageSlugExists(String languageSlug, int languageId) {
    Language expectedLanguage = languageMapper.getLanguageUsingSlug(languageSlug);

    return expectedLanguage != null && expectedLanguage.getLanguageId() != languageId;
  }

  /**
   * Checks whether a programming language name is legal: 1 to 16 characters.
   *
   * @param languageName - the name of the programming language
   * @return whether the programming language name is legal
   */
  private boolean isLanguageNameLegal(String languageName) {
    return !languageName.isEmpty() && languageName.length() <= 16;
  }

  /**
   * Checks whether a programming language's compile command is legal. Rule: the compile command
   * should not exceed 128 characters and should contain the {filename} field.
   *
   * @param compileCommand - the compile command of the programming language
   * @return whether the programming language's compile command is legal
   */
  private boolean isCompileCommandLegal(String compileCommand) {
    return compileCommand.length() <= 128 && compileCommand.contains("{filename}");
  }

  /**
   * Checks whether a programming language's run command is legal. Rule: the run command should not
   * exceed 128 characters and should contain the {filename} field.
   *
   * @param runCommand - the run command of the programming language
   * @return whether the programming language's run command is legal
   */
  private boolean isRunCommandLegal(String runCommand) {
    return runCommand.length() <= 128 && runCommand.contains("{filename}");
  }

  /**
   * Checks whether the name of a language's source file is legal: the judger saves a submission
   * under it (and {@code {filename}} stands for it without its extension), so it is a plain file
   * name of at most 32 characters.
   *
   * @param sourceFilename - the name of the source file
   * @return whether the name is legal
   */
  private boolean isSourceFilenameLegal(String sourceFilename) {
    return sourceFilename != null
        && !sourceFilename.isBlank()
        && sourceFilename.length() <= 32
        && !sourceFilename.equals(".")
        && !sourceFilename.equals("..")
        && !sourceFilename.contains("/")
        && !sourceFilename.contains("\\");
  }

  /** The autowired LanguageMapper object. */
  @Autowired private LanguageMapper languageMapper;
}
