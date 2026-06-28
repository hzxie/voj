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
import org.verwandlung.voj.web.util.JsonUtils;

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
    // The seeded roster changes over time, so assert the seeded languages are returned rather than
    // a hard-coded count.
    Assertions.assertFalse(languages.isEmpty());
    Assertions.assertNotNull(languageService.getLanguageUsingSlug("text/x-csrc"));
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
    int baseCount = newLanguages.size();
    newLanguages.add(
        new Language("text/x-haskell", "Haskell", "ghc -o {filename}.exe {filename}.hs", "{filename}.exe"));

    Map<String, Object> result = languageService.updateLanguageSettings(newLanguages);
    Assertions.assertTrue((Boolean) result.get("isSuccessful"));
    Assertions.assertEquals(1, ((List<Language>) result.get("languageCreated")).size());
    Assertions.assertEquals(baseCount + 1, languageService.getAllLanguages().size());
  }

  /**
   * Test case: tests the updateLanguageSettings(List) method against a list deserialized from a JSON
   * payload, mirroring the controller flow (the admin editor posts JSON, the action runs it through
   * JsonUtils.toList). Regression guard: compileCommand / runCommand must survive deserialization
   * (they were @JsonIgnore, which stripped them on the way in and NPE'd the validation). Expected:
   * the new language is created and its commands are persisted.
   */
  @Test
  @SuppressWarnings("unchecked")
  public void testUpdateLanguageSettingsFromJsonPayload() {
    String payload =
        "[{\"languageId\":0,\"languageSlug\":\"text/x-haskell\",\"languageName\":\"Haskell\","
            + "\"compileCommand\":\"ghc -o {filename}.exe {filename}.hs\","
            + "\"runCommand\":\"{filename}.exe\",\"enabled\":true,"
            + "\"sourceFilename\":\"Main.hs\",\"timeMultiplier\":1.5,\"memoryMultiplier\":2.0}]";
    List<Language> newLanguages = currentLanguages();
    newLanguages.addAll(JsonUtils.toList(payload, Language.class));

    Map<String, Object> result = languageService.updateLanguageSettings(newLanguages);
    Assertions.assertTrue((Boolean) result.get("isSuccessful"));
    Assertions.assertEquals(1, ((List<Language>) result.get("languageCreated")).size());

    Language created = languageService.getLanguageUsingSlug("text/x-haskell");
    Assertions.assertNotNull(created);
    Assertions.assertEquals("ghc -o {filename}.exe {filename}.hs", created.getCompileCommand());
    Assertions.assertEquals("{filename}.exe", created.getRunCommand());
    Assertions.assertTrue(created.isEnabled());
    Assertions.assertEquals("Main.hs", created.getSourceFilename());
    Assertions.assertEquals(1.5, created.getTimeMultiplier());
    Assertions.assertEquals(2.0, created.getMemoryMultiplier());
  }

  /** Test case: tests the updateLanguageSettings(List) method. Test data: the slug of the new language is empty. Expected: the update fails. */
  @Test
  public void testUpdateLanguageSettingsCreateLanguageWithEmptySlug() {
    List<Language> newLanguages = currentLanguages();
    int baseCount = newLanguages.size();
    newLanguages.add(new Language("", "Haskell", "ghc -o {filename}.exe {filename}.hs", "{filename}.exe"));

    Map<String, Object> result = languageService.updateLanguageSettings(newLanguages);
    Assertions.assertFalse((Boolean) result.get("isSuccessful"));
    Assertions.assertEquals(baseCount, languageService.getAllLanguages().size());
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
    newLanguages.add(new Language("text/x-haskell", "Haskell", "ghc main.hs", "{filename}"));

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
    int baseCount = newLanguages.size();
    // Pascal (language_id = 4) is not used by any submission record or user.
    newLanguages.removeIf(language -> language.getLanguageId() == 4);

    Map<String, Object> result = languageService.updateLanguageSettings(newLanguages);
    Assertions.assertTrue((Boolean) result.get("isSuccessful"));
    Assertions.assertEquals(1, ((List<Language>) result.get("languageDeleted")).size());
    Assertions.assertEquals(baseCount - 1, languageService.getAllLanguages().size());
  }

  /** Test case: tests the updateLanguageSettings(List) method. Test data: delete a language that is in use. Expected: the update fails and the language is not deleted. */
  @Test
  public void testUpdateLanguageSettingsDeleteLanguageInUse() {
    List<Language> newLanguages = currentLanguages();
    int baseCount = newLanguages.size();
    // C++ (language_id = 2) is used by submission records and user preferences, and cannot be deleted.
    newLanguages.removeIf(language -> language.getLanguageId() == 2);

    Map<String, Object> result = languageService.updateLanguageSettings(newLanguages);
    Assertions.assertFalse((Boolean) result.get("isSuccessful"));
    Assertions.assertEquals(baseCount, languageService.getAllLanguages().size());
  }

  /** Returns a mutable copy of all current languages, used to construct the input of updateLanguageSettings. */
  private List<Language> currentLanguages() {
    List<Language> languages = new ArrayList<>();
    for (Language language : languageService.getAllLanguages()) {
      Language copy =
          new Language(
              language.getLanguageId(),
              language.getLanguageSlug(),
              language.getLanguageName(),
              language.getCompileCommand(),
              language.getRunCommand());
      copy.setEnabled(language.isEnabled());
      copy.setSourceFilename(language.getSourceFilename());
      copy.setTimeMultiplier(language.getTimeMultiplier());
      copy.setMemoryMultiplier(language.getMemoryMultiplier());
      languages.add(copy);
    }
    return languages;
  }

  /** The LanguageService object under test. */
  @Autowired private LanguageService languageService;
}
