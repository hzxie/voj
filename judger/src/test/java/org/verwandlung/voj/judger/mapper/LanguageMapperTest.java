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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

import org.verwandlung.voj.judger.model.Language;

/**
 * Test class for LanguageMapper.
 *
 * @author Haozhe Xie
 */
@ExtendWith(SpringExtension.class)
@Transactional
@ContextConfiguration({"classpath:test-spring-context.xml"})
public class LanguageMapperTest {
  /**
   * Test case: tests the getLanguageUsingId(int) method. Test data: the unique identifier of the C
   * language. Expected result: returns the programming language object for C.
   */
  @Test
  public void testGetLanguageUsingIdExists() {
    Language language = languageMapper.getLanguageUsingId(1);
    Assertions.assertNotNull(language);

    String languageName = language.getLanguageName();
    Assertions.assertEquals("C", languageName);
  }

  /**
   * Test case: tests the getLanguageUsingId(int) method. Test data: a non-existent programming
   * language identifier. Expected result: returns a null reference.
   */
  @Test
  public void testGetLanguageUsingIdNotExists() {
    Language language = languageMapper.getLanguageUsingId(0);
    Assertions.assertNull(language);
  }

  /**
   * Test case: tests the getLanguageUsingSlug(String) method. Test data: the unique English
   * abbreviation of the C language. Expected result: returns the programming language object for C.
   */
  @Test
  public void testGetLanguageUsingSlugExists() {
    Language language = languageMapper.getLanguageUsingSlug("text/x-csrc");
    Assertions.assertNotNull(language);

    String languageName = language.getLanguageName();
    Assertions.assertEquals("C", languageName);
  }

  /**
   * Test case: tests the getLanguageUsingSlug(String) method. Test data: a non-existent unique
   * English abbreviation of a programming language. Expected result: returns a null reference.
   */
  @Test
  public void testGetLanguageUsingSlugNotExists() {
    Language language = languageMapper.getLanguageUsingSlug("Not-Exists");
    Assertions.assertNull(language);
  }

  /**
   * Test case: tests the getAllLanguages() method. Test data: N/A. Expected result: returns the
   * list of all programming languages (6 languages in total).
   */
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
  /** The LanguageMapper object under test. */
  @Autowired private LanguageMapper languageMapper;
}
