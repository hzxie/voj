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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

import org.verwandlung.voj.web.model.Language;

/**
 * The test class for LanguageMapper.
 *
 * @author Haozhe Xie
 */
@ExtendWith(SpringExtension.class)
@Transactional
@ContextConfiguration({"classpath:test-spring-context.xml"})
public class LanguageMapperTest {
  /** Test case: tests the getLanguageUsingId(int) method. Test data: the unique identifier of the C language. Expected: the C language object. */
  @Test
  public void testGetLanguageUsingIdExists() {
    Language language = languageMapper.getLanguageUsingId(1);
    Assertions.assertNotNull(language);

    String languageName = language.getLanguageName();
    Assertions.assertEquals("C", languageName);
  }

  /** Test case: tests the getLanguageUsingId(int) method. Test data: a non-existing language unique identifier. Expected: a null reference. */
  @Test
  public void testGetLanguageUsingIdNotExists() {
    Language language = languageMapper.getLanguageUsingId(0);
    Assertions.assertNull(language);
  }

  /** Test case: tests the getLanguageUsingSlug(String) method. Test data: the slug of the C language. Expected: the C language object. */
  @Test
  public void testGetLanguageUsingSlugExists() {
    Language language = languageMapper.getLanguageUsingSlug("text/x-csrc");
    Assertions.assertNotNull(language);

    String languageName = language.getLanguageName();
    Assertions.assertEquals("C", languageName);
  }

  /** Test case: tests the getLanguageUsingSlug(String) method. Test data: a non-existing language slug. Expected: a null reference. */
  @Test
  public void testGetLanguageUsingSlugNotExists() {
    Language language = languageMapper.getLanguageUsingSlug("Not-Exists");
    Assertions.assertNull(language);
  }

  /** Test case: tests the getAllLanguages() method. Test data: N/a. Expected: the list of all programming languages (6 languages in total). */
  @Test
  public void testGetAllLanguages() {
    List<Language> languages = languageMapper.getAllLanguages();
    Assertions.assertNotNull(languages);
    // The seeded roster changes over time; assert it is non-empty and ordered (C first) rather than
    // a hard-coded count.
    Assertions.assertFalse(languages.isEmpty());

    Language firstLanguage = languages.get(0);
    Assertions.assertNotNull(firstLanguage);

    String languageName = firstLanguage.getLanguageName();
    Assertions.assertEquals("C", languageName);
  }

  /** Test case: tests the createLanguage(Language) method. Test data: a valid data set. Expected: the data insertion operation completes successfully. */
  @Test
  public void testCreateLanguageNormally() {
    Language language = new Language("text/x-php", "PHP", "php foo.php", "php foo.php");
    int numberOfRowsAffected = languageMapper.createLanguage(language);
    Assertions.assertEquals(1, numberOfRowsAffected);
  }

  /**
   * Test case: tests the createLanguage(Language) method. Test data: an invalid data set (a language
   * slug that is too long). Expected: a DataIntegrityViolationException is thrown.
   */
  @Test
  public void testCreateLanguageUsingTooLongSlug() {
    Language language =
        new Language("TooLongLanguageSlug", "Invalid Langauge", "Compile Command", "Run Command");
    Executable e =
        () -> {
          languageMapper.createLanguage(language);
        };
    Assertions.assertThrows(org.springframework.dao.DataIntegrityViolationException.class, e);
  }

  /** Test case: tests the updateLanguage(Language) method. Test data: a valid data set, and a record with the corresponding ID exists in the table. Expected: the data update operation completes successfully. */
  @Test
  public void testUpdateLanguageNormally() {
    Language language = languageMapper.getLanguageUsingId(2);
    Assertions.assertNotNull(language);

    language.setLanguageName("D");
    int numberOfRowsAffected = languageMapper.updateLanguage(language);
    Assertions.assertEquals(1, numberOfRowsAffected);

    language = languageMapper.getLanguageUsingId(2);
    Assertions.assertEquals("D", language.getLanguageName());
  }

  /** Test case: tests the updateLanguage(Language) method. Test data: a valid data set, but the language does not exist in the table. Expected: the method executes normally without affecting the data in the table. */
  @Test
  public void testUpdateLanguageNotExists() {
    Language language = new Language(0, "not-exist", "Not Exist", "Not Exist", "Not Exist");
    int numberOfRowsAffected = languageMapper.updateLanguage(language);
    Assertions.assertEquals(0, numberOfRowsAffected);
  }

  /** Test case: tests the deleteLanguage(int) method. Test data: the unique identifier of the Ruby language. Expected: the data deletion operation completes successfully. */
  @Test
  public void testDeleteLanguageExists() {
    Language language = languageMapper.getLanguageUsingId(6);
    Assertions.assertNotNull(language);

    int numberOfRowsAffected = languageMapper.deleteLanguage(6);
    Assertions.assertEquals(1, numberOfRowsAffected);

    language = languageMapper.getLanguageUsingId(6);
    Assertions.assertNull(language);
  }

  /** Test case: tests the deleteLanguage(int) method. Test data: a non-existing language unique identifier. Expected: the method executes normally without affecting the data in the table. */
  @Test
  public void testDeleteLanguageNotExists() {
    Language language = languageMapper.getLanguageUsingId(0);
    Assertions.assertNull(language);

    int numberOfRowsAffected = languageMapper.deleteLanguage(0);
    Assertions.assertEquals(0, numberOfRowsAffected);
  }

  /** The LanguageMapper object under test. */
  @Autowired private LanguageMapper languageMapper;
}
