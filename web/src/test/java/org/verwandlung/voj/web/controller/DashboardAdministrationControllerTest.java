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

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;

import org.verwandlung.voj.web.service.LanguageService;
import org.verwandlung.voj.web.service.SubmissionService;

/**
 * The test class for DashboardAdministrationController - the counts of the dashboard.
 *
 * @author Haozhe Xie
 */
public class DashboardAdministrationControllerTest {
  /**
   * Test case: tests getSubmissionsToday(). Expected: the submissions are counted from midnight
   * (00:00:00.000, not with the milliseconds of the current time) of today, in the application's
   * time zone, up to midnight of tomorrow.
   */
  @Test
  public void testSubmissionsTodayAreCountedFromMidnight() {
    SubmissionService submissionService = mock(SubmissionService.class);
    DashboardAdministrationController controller = new DashboardAdministrationController();
    ReflectionTestUtils.setField(controller, "submissionService", submissionService);

    ZoneId zone = ZoneId.systemDefault();
    LocalDate dayBefore = LocalDate.now(zone);
    ReflectionTestUtils.invokeMethod(controller, "getSubmissionsToday");
    LocalDate dayAfter = LocalDate.now(zone);

    ArgumentCaptor<Date> startTime = ArgumentCaptor.forClass(Date.class);
    ArgumentCaptor<Date> endTime = ArgumentCaptor.forClass(Date.class);
    verify(submissionService)
        .getNumberOfSubmissionsUsingDate(startTime.capture(), endTime.capture());
    // The test may run across midnight: today is then either day.
    LocalDate today =
        startTime.getValue().equals(Date.from(dayAfter.atStartOfDay(zone).toInstant()))
            ? dayAfter
            : dayBefore;
    Assertions.assertEquals(
        Date.from(today.atStartOfDay(zone).toInstant()), startTime.getValue());
    Assertions.assertEquals(
        Date.from(today.plusDays(1).atStartOfDay(zone).toInstant()), endTime.getValue());
  }

  /** Test case: tests updateLanguageSettingsAction(...). Test data: a JSON languages array. Expected: the parsed list is forwarded to LanguageService and its result returned. */
  @Test
  public void testUpdateLanguageSettingsDelegates() {
    LanguageService languageService = mock(LanguageService.class);
    DashboardAdministrationController controller = new DashboardAdministrationController();
    ReflectionTestUtils.setField(controller, "languageService", languageService);
    Map<String, Object> serviceResult = Map.of("isSuccessful", true);
    when(languageService.updateLanguageSettings(org.mockito.ArgumentMatchers.anyList()))
        .thenReturn(serviceResult);

    Map<String, Object> result =
        controller.updateLanguageSettingsAction("[]", new MockHttpServletRequest());

    Assertions.assertSame(serviceResult, result);
  }
}
