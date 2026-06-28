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
import org.verwandlung.voj.web.mapper.SubmissionMapper;
import org.verwandlung.voj.web.mapper.UserMapper;
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
   * Gets a programming language object by its alias.
   *
   * @param languageSlug - the alias of the programming language
   * @return the programming language object
   */
  public Language getLanguageUsingSlug(String languageSlug) {
    return languageMapper.getLanguageUsingSlug(languageSlug);
  }

  /**
   * Updates the programming language options.
   *
   * @param languages - the array containing the programming language settings
   * @return the update result of the programming language options
   */
  public Map<String, Object> updateLanguageSettings(List<Language> languages) {
    List<Language> previousLanguages = languageMapper.getAllLanguages();
    Map<String, Object> result = new HashMap<>();
    Map<String, Object> validationResult =
        getUpdateLanguageSettingsResult(previousLanguages, languages);
    boolean isSuccessful = (Boolean) validationResult.get("isSuccessful");

    if (isSuccessful) {
      Map<String, List<Language>> languageChanges =
          getLanguageChanges(previousLanguages, languages);

      createLanguages(languageChanges.get("languageCreated"));
      updateLanguages(languageChanges.get("languageUpdated"));
      deleteLanguages(languageChanges.get("languageDeleted"));
      result.putAll(languageChanges);
    } else {
      result.putAll(validationResult);
    }
    result.put("isSuccessful", isSuccessful);
    return result;
  }

  /**
   * Checks the correctness of the programming language settings.
   *
   * @param previousLanguages - the list of language settings before the update
   * @param newLanguages - the list of language settings after the update
   * @return the validation result of the programming language settings
   */
  private Map<String, Object> getUpdateLanguageSettingsResult(
      List<Language> previousLanguages, List<Language> newLanguages) {
    boolean isSuccessful = true;
    Map<String, Object> result = new HashMap<>();

    for (Language language : previousLanguages) {
      Map<String, Boolean> languageResult = new HashMap<>(4, 1);
      languageResult.put("isLanguageDeleted", isLanguageDeleted(newLanguages, language));
      languageResult.put("isLangaugeInUse", isLanguageInUse(language));

      boolean isLanguageSuccessful =
          !languageResult.get("isLanguageDeleted") || !languageResult.get("isLangaugeInUse");
      languageResult.put("isSuccessful", isLanguageSuccessful);
      result.put(language.getLanguageName(), languageResult);

      isSuccessful &= isLanguageSuccessful;
    }
    for (Language language : newLanguages) {
      Map<String, Boolean> languageResult = new HashMap<>(10, 1);
      languageResult.put("isLanguageSlugEmpty", language.getLanguageSlug().isEmpty());
      languageResult.put("isLanguageSlugLegal", isLanguageSlugLegal(language.getLanguageSlug()));
      languageResult.put(
          "isLanguageSlugExists",
          isLanguageSlugExists(language.getLanguageSlug(), language.getLanguageId()));
      languageResult.put("isLanguageNameEmpty", language.getLanguageName().isEmpty());
      languageResult.put("isLanguageNameLegal", isLanguageNameLegal(language.getLanguageName()));
      languageResult.put("isCompileCommandEmpty", language.getCompileCommand().isEmpty());
      languageResult.put(
          "isCompileCommandLegal", isCompileCommandLegal(language.getCompileCommand()));
      languageResult.put("isRunCommandEmpty", language.getRunCommand().isEmpty());
      languageResult.put("isRunCommandLegal", isRunCommandLegal(language.getRunCommand()));

      boolean isLanguageSuccessful =
          !languageResult.get("isLanguageSlugEmpty")
              && languageResult.get("isLanguageSlugLegal")
              && !languageResult.get("isLanguageSlugExists")
              && !languageResult.get("isLanguageNameEmpty")
              && languageResult.get("isLanguageNameLegal")
              && !languageResult.get("isCompileCommandEmpty")
              && languageResult.get("isCompileCommandLegal")
              && !languageResult.get("isRunCommandEmpty")
              && languageResult.get("isRunCommandLegal");
      languageResult.put("isSuccessful", isLanguageSuccessful);
      result.put(language.getLanguageName(), languageResult);

      isSuccessful &= isLanguageSuccessful;
    }
    result.put("isSuccessful", isSuccessful);
    return result;
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
   * Checks whether a programming language alias is legal. Rule: a legal programming language alias
   * should not exceed 16 characters.
   *
   * @param languageSlug - the alias of the programming language
   * @return whether the programming language alias is legal
   */
  private boolean isLanguageSlugLegal(String languageSlug) {
    return languageSlug.length() <= 16;
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
   * Checks whether a programming language name is legal. Rule: a legal programming language name
   * should not exceed 16 characters.
   *
   * @param languageName - the name of the programming language
   * @return whether the programming language name is legal
   */
  private boolean isLanguageNameLegal(String languageName) {
    return languageName.length() <= 16;
  }

  /**
   * Checks whether a programming language's compile command is legal. Rule: the compile command
   * should not exceed 128 characters and should contain the {filename} field.
   *
   * @param compileCommand - the compile command of the programming language
   * @return whether the programming language's compile command is legal
   */
  private boolean isCompileCommandLegal(String compileCommand) {
    return compileCommand.length() <= 128 && compileCommand.matches(".*\\{filename\\}.*");
  }

  /**
   * Checks whether a programming language's run command is legal. Rule: the run command should not
   * exceed 128 characters and should contain the {filename} field.
   *
   * @param runCommand - the run command of the programming language
   * @return whether the programming language's run command is legal
   */
  private boolean isRunCommandLegal(String runCommand) {
    return runCommand.length() <= 128 && runCommand.matches(".*\\{filename\\}.*");
  }

  /**
   * Checks whether other records reference this language; if so, it cannot be deleted due to the
   * database foreign key integrity constraints.
   *
   * @param language - the programming language to check
   * @return whether there are submissions referencing this language
   */
  private boolean isLanguageInUse(Language language) {
    if (language == null) {
      return false;
    }

    int languageId = language.getLanguageId();
    return userMapper.getNumberOfUsersUsingLanguage(languageId) != 0
        || submissionMapper.getNumberOfSubmissionsUsingLanguage(languageId) != 0;
  }

  /** The autowired LanguageMapper object. */
  @Autowired private LanguageMapper languageMapper;

  /** The autowired SubmissionMapper object, used to query submissions associated with a language. */
  @Autowired private SubmissionMapper submissionMapper;

  /**
   * The autowired UserMapper object, used to query users whose preferred language is associated with
   * a language.
   */
  @Autowired private UserMapper userMapper;
}
