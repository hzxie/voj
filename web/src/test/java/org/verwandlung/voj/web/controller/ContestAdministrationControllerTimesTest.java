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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.time.Instant;
import java.util.Date;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;

import org.verwandlung.voj.web.service.ContestService;

/**
 * The test class for ContestAdministrationController - the contest start / end times.
 *
 * <p>The contest editor posts instants (epoch milliseconds) computed in the admin's browser; the
 * controller must read them as instants, independent of the server's time zone.
 *
 * @author Haozhe Xie
 */
public class ContestAdministrationControllerTimesTest {
  /** Wires a mocked ContestService into a fresh controller before every test. */
  @BeforeEach
  public void setUp() {
    contestService = mock(ContestService.class);
    controller = new ContestAdministrationController();
    ReflectionTestUtils.setField(controller, "contestService", contestService);
  }

  /** Test case: tests parseInstant(...). Test data: epoch milliseconds. Expected: that instant. */
  @Test
  public void testParseInstantReadsEpochMilliseconds() {
    Assertions.assertEquals(new Date(1893553440000L), ContestAdministrationController.parseInstant("1893553440000"));
  }

  /** Test case: tests parseInstant(...). Test data: ISO-8601 with an offset. Expected: that instant, whatever the server's zone. */
  @Test
  public void testParseInstantReadsIsoOffsetDateTime() {
    Assertions.assertEquals(
        Date.from(Instant.parse("2030-01-01T19:04:00Z")),
        ContestAdministrationController.parseInstant("2030-01-02T03:04:00+08:00"));
  }

  /** Test case: tests parseInstant(...). Test data: a wall-clock time without an offset, garbage, blank. Expected: null (rejected). */
  @Test
  public void testParseInstantRejectsAmbiguousOrInvalidValues() {
    Assertions.assertNull(ContestAdministrationController.parseInstant("2030-01-02T03:04"));
    Assertions.assertNull(ContestAdministrationController.parseInstant("2030-01-02 03:04:00"));
    Assertions.assertNull(ContestAdministrationController.parseInstant("tomorrow"));
    Assertions.assertNull(ContestAdministrationController.parseInstant(" "));
    Assertions.assertNull(ContestAdministrationController.parseInstant(null));
  }

  /** Test case: tests createContestAction(...). Test data: start and end as epoch ms. Expected: the service gets those instants. */
  @Test
  public void testCreateContestPassesPostedInstants() {
    controller.createContestAction(
        "Contest", "", "1893553440000", "1893571440000", "ICPC", "[]", "PUBLISHED", 20, 0, true,
        new MockHttpServletRequest());

    ArgumentCaptor<Date> start = ArgumentCaptor.forClass(Date.class);
    ArgumentCaptor<Date> end = ArgumentCaptor.forClass(Date.class);
    verify(contestService)
        .createContest(
            anyString(), anyString(), start.capture(), end.capture(), anyString(), anyString(),
            anyString(), anyInt(), anyInt(), anyBoolean());
    Assertions.assertEquals(new Date(1893553440000L), start.getValue());
    Assertions.assertEquals(new Date(1893571440000L), end.getValue());
  }

  /** Test case: tests editContestAction(...). Test data: a wall-clock start without an offset. Expected: the service gets null (and rejects the contest). */
  @Test
  public void testEditContestRejectsWallClockTime() {
    controller.editContestAction(
        1L, "Contest", "", "2030-01-02T03:04", "1893571440000", "ICPC", "[]", "PUBLISHED", 20, 0,
        true, new MockHttpServletRequest());

    verify(contestService)
        .editContest(
            anyLong(), anyString(), anyString(), isNull(), any(Date.class), anyString(),
            anyString(), anyString(), anyInt(), anyInt(), anyBoolean());
  }

  private ContestService contestService;
  private ContestAdministrationController controller;
}
