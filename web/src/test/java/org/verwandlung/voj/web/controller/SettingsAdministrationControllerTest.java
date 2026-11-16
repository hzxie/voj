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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;

import org.verwandlung.voj.web.service.OffensiveWordImportService;
import org.verwandlung.voj.web.service.OffensiveWordService;
import org.verwandlung.voj.web.service.OptionService;
import org.verwandlung.voj.web.util.MailSender;

/**
 * The test class for SettingsAdministrationController.
 *
 * <p>Covers the controller-specific logic: the page-size and non-negative clamping plus the
 * boolean-to-"1"/"0" mapping applied before the settings are saved, and the offensive word import.
 *
 * @author Haozhe Xie
 */
public class SettingsAdministrationControllerTest {
  /** Wires mocked collaborators into a fresh controller before every test. */
  @BeforeEach
  public void setUp() {
    optionService = mock(OptionService.class);
    offensiveWordService = mock(OffensiveWordService.class);
    offensiveWordImportService = mock(OffensiveWordImportService.class);

    controller = new SettingsAdministrationController();
    ReflectionTestUtils.setField(controller, "optionService", optionService);
    ReflectionTestUtils.setField(controller, "offensiveWordService", offensiveWordService);
    ReflectionTestUtils.setField(
        controller, "offensiveWordImportService", offensiveWordImportService);
  }

  /** Test case: tests updateGeneralSettingsAction(...). Test data: an oversized page size and a zero email limit. Expected: every setting goes to OptionService in one call, with the page size clamped to 1000, the email daily limit to a minimum of 1 and the booleans mapped to "1"/"0". */
  @Test
  public void testUpdateGeneralSettingsClampsValues() {
    Map<String, Boolean> serviceResult = Map.of("isSuccessful", true);
    when(optionService.updateGeneralSettings(org.mockito.ArgumentMatchers.any()))
        .thenReturn(serviceResult);

    Assertions.assertSame(serviceResult, updateGeneralSettings("admin@voj.org", 5000, 0));

    @SuppressWarnings({"unchecked", "rawtypes"})
    ArgumentCaptor<Map<String, String>> captor = ArgumentCaptor.forClass((Class) Map.class);
    verify(optionService).updateGeneralSettings(captor.capture());
    Map<String, String> settings = captor.getValue();
    Assertions.assertEquals("admin@voj.org", settings.get("contactEmail"));
    Assertions.assertEquals("1", settings.get("emailDailyLimit"));
    Assertions.assertEquals("1", settings.get("maintenanceMode"));
    Assertions.assertEquals("1000", settings.get("problemsPerPage"));
  }

  /**
   * Test case: tests updateGeneralSettingsAction(...) on a site that sends no email. Test data:
   * email verification required. Expected: nothing is saved, and the result says that the site
   * sends no email.
   */
  @Test
  public void testUpdateGeneralSettingsRefusesEmailVerificationWithoutMail() {
    MailSender mailSender = mock(MailSender.class);
    when(mailSender.isEnabled()).thenReturn(false);
    ReflectionTestUtils.setField(controller, "mailSender", mailSender);

    Map<String, Boolean> result =
        controller.updateGeneralSettingsAction(
            "VOJ", "desc", "(c)", true, "icp", "police", "ga", "src", "", true, true, true,
            true, true, 0, 5, 1, 3, 100, 100, 100, 50, 50, 50, false, "", "", 3,
            new MockHttpServletRequest());

    Assertions.assertFalse(result.get("isSuccessful"));
    Assertions.assertFalse(result.get("isMailEnabled"));
    verify(optionService, org.mockito.Mockito.never())
        .updateGeneralSettings(org.mockito.ArgumentMatchers.any());
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

  /** Invokes updateGeneralSettingsAction with mostly-fixed arguments, varying the values under test. */
  private Map<String, Boolean> updateGeneralSettings(
      String contactEmail, int problemsPerPage, int emailDailyLimit) {
    return controller.updateGeneralSettingsAction(
        "VOJ", "desc", "(c)", true, "icp", "police", "ga", "src", contactEmail, false, true, true,
        true, true, 0, 5, 1, 3, problemsPerPage, 100, 100, 50, 50, 50, false, "", "",
        emailDailyLimit, new MockHttpServletRequest());
  }

  /** The mocked OptionService. */
  private OptionService optionService;

  /** The mocked OffensiveWordService. */
  private OffensiveWordService offensiveWordService;

  /** The mocked OffensiveWordImportService. */
  private OffensiveWordImportService offensiveWordImportService;

  /** The SettingsAdministrationController object under test. */
  private SettingsAdministrationController controller;
}
