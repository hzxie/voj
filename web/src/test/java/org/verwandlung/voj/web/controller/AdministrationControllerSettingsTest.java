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
package org.verwandlung.voj.web.controller;

import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;

import org.verwandlung.voj.web.service.LanguageService;
import org.verwandlung.voj.web.service.OffensiveWordImportService;
import org.verwandlung.voj.web.service.OffensiveWordService;
import org.verwandlung.voj.web.service.OptionService;
import org.verwandlung.voj.web.service.SubmissionService;

/**
 * The test class for AdministrationController - the settings / moderation area.
 *
 * <p>Covers the controller-specific logic: the page-size and non-negative clamping plus
 * boolean-to-"1"/"0" mapping applied before the option store is written, the illegal-contact-email
 * short circuit, and the "keep only the ids that were actually removed" fan-out of the bulk
 * submission endpoints.
 *
 * @author Haozhe Xie
 */
public class AdministrationControllerSettingsTest {
  /** Wires mocked collaborators into a fresh controller before every test. */
  @BeforeEach
  public void setUp() {
    optionService = mock(OptionService.class);
    offensiveWordService = mock(OffensiveWordService.class);
    offensiveWordImportService = mock(OffensiveWordImportService.class);
    languageService = mock(LanguageService.class);
    submissionService = mock(SubmissionService.class);

    controller = new AdministrationController();
    ReflectionTestUtils.setField(controller, "optionService", optionService);
    ReflectionTestUtils.setField(controller, "offensiveWordService", offensiveWordService);
    ReflectionTestUtils.setField(
        controller, "offensiveWordImportService", offensiveWordImportService);
    ReflectionTestUtils.setField(controller, "languageService", languageService);
    ReflectionTestUtils.setField(controller, "submissionService", submissionService);
  }

  /** Test case: tests updateGeneralSettingsAction(...). Test data: an illegal contact email. Expected: the result is marked unsuccessful and the moderation / page-size options are never written. */
  @Test
  public void testUpdateGeneralSettingsRejectsIllegalContactEmail() {
    stubBaseOptionsUpdate(true);
    when(optionService.isContactEmailLegal("not-an-email")).thenReturn(false);

    Map<String, Boolean> result = updateGeneralSettings("not-an-email", 100, 3);

    Assertions.assertEquals(Boolean.FALSE, result.get("isContactEmailLegal"));
    Assertions.assertEquals(Boolean.FALSE, result.get("isSuccessful"));
    verify(optionService, never()).updateOptions(org.mockito.ArgumentMatchers.<Map<String, String>>any());
  }

  /** Test case: tests updateGeneralSettingsAction(...). Test data: an oversized page size and a zero email limit. Expected: the page size is clamped to 1000 and the email daily limit to a minimum of 1 before being written. */
  @Test
  public void testUpdateGeneralSettingsClampsValues() {
    stubBaseOptionsUpdate(true);
    when(optionService.isContactEmailLegal("admin@voj.org")).thenReturn(true);

    updateGeneralSettings("admin@voj.org", 5000, 0);

    @SuppressWarnings({"unchecked", "rawtypes"})
    ArgumentCaptor<Map<String, String>> captor = ArgumentCaptor.forClass((Class) Map.class);
    verify(optionService, times(2)).updateOptions(captor.capture());

    Map<String, String> moderation = mapContaining(captor.getAllValues(), "emailDailyLimit");
    Map<String, String> pageSizes = mapContaining(captor.getAllValues(), "problemsPerPage");
    // emailDailyLimit 0 -> clamped to a minimum of 1; booleans mapped to "1"/"0".
    Assertions.assertEquals("1", moderation.get("emailDailyLimit"));
    Assertions.assertEquals("1", moderation.get("maintenanceMode"));
    // problemsPerPage 5000 -> clamped to the [1, 1000] range.
    Assertions.assertEquals("1000", pageSizes.get("problemsPerPage"));
  }

  /** Test case: tests importOffensiveWordsAction(...). Test data: a sources string. Expected: the sources are saved, the dictionary re-imported and the import result returned. */
  @Test
  public void testImportOffensiveWordsDelegates() {
    Map<String, Object> importResult = Map.of("isSuccessful", true, "totalWords", 42);
    when(offensiveWordImportService.importFromSources()).thenReturn(importResult);

    Map<String, Object> result =
        controller.importOffensiveWordsAction(
            "https://example.org/words.txt", new MockHttpServletRequest());

    Assertions.assertSame(importResult, result);
    verify(offensiveWordService).updateOffensiveWordSources("https://example.org/words.txt");
    verify(offensiveWordImportService).importFromSources();
  }

  /** Test case: tests updateLanguageSettingsAction(...). Test data: a JSON languages array. Expected: the parsed list is forwarded to LanguageService and its result returned. */
  @Test
  public void testUpdateLanguageSettingsDelegates() {
    Map<String, Object> serviceResult = Map.of("isSuccessful", true);
    when(languageService.updateLanguageSettings(org.mockito.ArgumentMatchers.anyList()))
        .thenReturn(serviceResult);

    Map<String, Object> result =
        controller.updateLanguageSettingsAction("[]", new MockHttpServletRequest());

    Assertions.assertSame(serviceResult, result);
  }

  /** Test case: tests deleteSubmissionsAction(...). Test data: two ids where only one is deletable. Expected: only the successfully-deleted id is reported back. */
  @Test
  public void testDeleteSubmissionsKeepsOnlyDeletedIds() {
    when(submissionService.deleteSubmission(1000L)).thenReturn(true);
    when(submissionService.deleteSubmission(1001L)).thenReturn(false);

    Map<String, Object> result =
        controller.deleteSubmissionsAction("[1000, 1001]", new MockHttpServletRequest());

    Assertions.assertEquals(Boolean.TRUE, result.get("isSuccessful"));
    @SuppressWarnings("unchecked")
    List<Long> deleted = (List<Long>) result.get("deletedSubmissions");
    Assertions.assertEquals(List.of(1000L), deleted);
  }

  /** Test case: tests restartSubmissionsAction(...). Test data: a JSON array of two ids. Expected: a rejudge task is created for each. */
  @Test
  public void testRestartSubmissionsCreatesTaskPerId() {
    Map<String, Boolean> result =
        controller.restartSubmissionsAction("[1000, 1001]", new MockHttpServletRequest());

    Assertions.assertEquals(Boolean.TRUE, result.get("isSuccessful"));
    verify(submissionService).createSubmissionTask(1000L);
    verify(submissionService).createSubmissionTask(1001L);
  }

  /** Stubs the base (8-arg) option update used by updateGeneralSettingsAction. */
  private void stubBaseOptionsUpdate(boolean successful) {
    Map<String, Boolean> base = new HashMap<>();
    base.put("isSuccessful", successful);
    when(optionService.updateOptions(
            anyString(), anyString(), anyString(), anyBoolean(), anyString(), anyString(),
            anyString(), anyString()))
        .thenReturn(base);
  }

  /** Invokes updateGeneralSettingsAction with mostly-fixed arguments, varying the values under test. */
  private Map<String, Boolean> updateGeneralSettings(
      String contactEmail, int problemsPerPage, int emailDailyLimit) {
    return controller.updateGeneralSettingsAction(
        "VOJ", "desc", "(c)", true, "icp", "police", "ga", "src", contactEmail, false, true, true,
        true, true, 0, 5, 1, 3, problemsPerPage, 100, 100, 50, 50, 50, false, "", "",
        emailDailyLimit, new MockHttpServletRequest());
  }

  /** Returns the first captured map that contains the given key. */
  private static Map<String, String> mapContaining(List<Map<String, String>> maps, String key) {
    for (Map<String, String> map : maps) {
      if (map.containsKey(key)) {
        return map;
      }
    }
    return new HashMap<>();
  }

  /** The mocked OptionService. */
  private OptionService optionService;

  /** The mocked OffensiveWordService. */
  private OffensiveWordService offensiveWordService;

  /** The mocked OffensiveWordImportService. */
  private OffensiveWordImportService offensiveWordImportService;

  /** The mocked LanguageService. */
  private LanguageService languageService;

  /** The mocked SubmissionService. */
  private SubmissionService submissionService;

  /** The AdministrationController object under test. */
  private AdministrationController controller;
}
