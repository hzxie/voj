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
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

import org.verwandlung.voj.web.model.Language;

/**
 * The test class for LanguageService.
 *
 * @author Haozhe Xie
 */
@ExtendWith(SpringExtension.class)
@Transactional
@ContextConfiguration({"classpath:test-spring-context.xml"})
public class LanguageServiceTest {
  /** Test case: tests the getAllLanguages() method. Test data: N/a. Expected: all supported programming languages. */
  @Test
  public void testGetAllLanguages() {
    List<Language> languages = languageService.getAllLanguages();
    Assertions.assertEquals(6, languages.size());
  }

  /** Test case: tests the getLanguageUsingSlug(String) method. Test data: an existing language slug. Expected: the corresponding programming language object. */
  @Test
  public void testGetLanguageUsingSlugExists() {
    Language language = languageService.getLanguageUsingSlug("text/x-java");
    Assertions.assertNotNull(language);
    Assertions.assertEquals("Java", language.getLanguageName());
  }

  /** Test case: tests the getLanguageUsingSlug(String) method. Test data: a non-existing language slug. Expected: a null reference. */
  @Test
  public void testGetLanguageUsingSlugNotExists() {
    Assertions.assertNull(languageService.getLanguageUsingSlug("text/x-not-exist"));
  }

  /** Test case: tests the updateLanguageSettings(List) method. Test data: add a valid language on top of the existing ones. Expected: the update succeeds and the total number of languages increases by one. */
  @Test
  @SuppressWarnings("unchecked")
  public void testUpdateLanguageSettingsCreateLanguage() {
    List<Language> newLanguages = currentLanguages();
    newLanguages.add(new Language("text/x-go", "Go", "go build {filename}.go", "{filename}"));

    Map<String, Object> result = languageService.updateLanguageSettings(newLanguages);
    Assertions.assertTrue((Boolean) result.get("isSuccessful"));
    Assertions.assertEquals(1, ((List<Language>) result.get("languageCreated")).size());
    Assertions.assertEquals(7, languageService.getAllLanguages().size());
  }

  /** Test case: tests the updateLanguageSettings(List) method. Test data: the slug of the new language is empty. Expected: the update fails. */
  @Test
  public void testUpdateLanguageSettingsCreateLanguageWithEmptySlug() {
    List<Language> newLanguages = currentLanguages();
    newLanguages.add(new Language("", "Go", "go build {filename}.go", "{filename}"));

    Map<String, Object> result = languageService.updateLanguageSettings(newLanguages);
    Assertions.assertFalse((Boolean) result.get("isSuccessful"));
    Assertions.assertEquals(6, languageService.getAllLanguages().size());
  }

  /** Test case: tests the updateLanguageSettings(List) method. Test data: the slug of the new language already exists. Expected: the update fails. */
  @Test
  public void testUpdateLanguageSettingsCreateLanguageWithDuplicatedSlug() {
    List<Language> newLanguages = currentLanguages();
    newLanguages.add(new Language("text/x-java", "Java Clone", "javac {filename}.java", "java -cp {filename}"));

    Map<String, Object> result = languageService.updateLanguageSettings(newLanguages);
    Assertions.assertFalse((Boolean) result.get("isSuccessful"));
  }

  /** Test case: tests the updateLanguageSettings(List) method. Test data: the compile command of the new language lacks the {filename} placeholder. Expected: the update fails. */
  @Test
  public void testUpdateLanguageSettingsCreateLanguageWithIllegalCompileCommand() {
    List<Language> newLanguages = currentLanguages();
    newLanguages.add(new Language("text/x-go", "Go", "go build main.go", "{filename}"));

    Map<String, Object> result = languageService.updateLanguageSettings(newLanguages);
    Assertions.assertFalse((Boolean) result.get("isSuccessful"));
  }

  /** Test case: tests the updateLanguageSettings(List) method. Test data: modify the name of an existing language. Expected: the update succeeds and the language name is persisted. */
  @Test
  @SuppressWarnings("unchecked")
  public void testUpdateLanguageSettingsUpdateLanguage() {
    List<Language> newLanguages = currentLanguages();
    // Modify the name of the C language (language_id = 1).
    for (Language language : newLanguages) {
      if (language.getLanguageId() == 1) {
        language.setLanguageName("C11");
      }
    }

    Map<String, Object> result = languageService.updateLanguageSettings(newLanguages);
    Assertions.assertTrue((Boolean) result.get("isSuccessful"));
    Assertions.assertEquals(1, ((List<Language>) result.get("languageUpdated")).size());
    Assertions.assertEquals("C11", languageService.getLanguageUsingSlug("text/x-csrc").getLanguageName());
  }

  /** Test case: tests the updateLanguageSettings(List) method. Test data: delete a language that is not in use. Expected: the update succeeds and the total number of languages decreases by one. */
  @Test
  @SuppressWarnings("unchecked")
  public void testUpdateLanguageSettingsDeleteUnusedLanguage() {
    List<Language> newLanguages = currentLanguages();
    // Pascal (language_id = 4) is not used by any submission record or user.
    newLanguages.removeIf(language -> language.getLanguageId() == 4);

    Map<String, Object> result = languageService.updateLanguageSettings(newLanguages);
    Assertions.assertTrue((Boolean) result.get("isSuccessful"));
    Assertions.assertEquals(1, ((List<Language>) result.get("languageDeleted")).size());
    Assertions.assertEquals(5, languageService.getAllLanguages().size());
  }

  /** Test case: tests the updateLanguageSettings(List) method. Test data: delete a language that is in use. Expected: the update fails and the language is not deleted. */
  @Test
  public void testUpdateLanguageSettingsDeleteLanguageInUse() {
    List<Language> newLanguages = currentLanguages();
    // C++ (language_id = 2) is used by submission records and user preferences, and cannot be deleted.
    newLanguages.removeIf(language -> language.getLanguageId() == 2);

    Map<String, Object> result = languageService.updateLanguageSettings(newLanguages);
    Assertions.assertFalse((Boolean) result.get("isSuccessful"));
    Assertions.assertEquals(6, languageService.getAllLanguages().size());
  }

  /** Returns a mutable copy of all current languages, used to construct the input of updateLanguageSettings. */
  private List<Language> currentLanguages() {
    List<Language> languages = new ArrayList<>();
    for (Language language : languageService.getAllLanguages()) {
      languages.add(
          new Language(
              language.getLanguageId(),
              language.getLanguageSlug(),
              language.getLanguageName(),
              language.getCompileCommand(),
              language.getRunCommand()));
    }
    return languages;
  }

  /** The LanguageService object under test. */
  @Autowired private LanguageService languageService;
}
