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

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;

import org.verwandlung.voj.web.service.SubmissionService;

/**
 * The test class for SubmissionAdministrationController.
 *
 * @author Haozhe Xie
 */
public class SubmissionAdministrationControllerTest {
  /** Wires a mocked SubmissionService into a fresh controller before every test. */
  @BeforeEach
  public void setUp() {
    submissionService = mock(SubmissionService.class);
    controller = new SubmissionAdministrationController();
    ReflectionTestUtils.setField(controller, "submissionService", submissionService);
  }

  /** Test case: tests deleteSubmissionsAction(...). Test data: two ids where only one is deletable. Expected: only the successfully-deleted id is reported back. */
  @Test
  public void testDeleteSubmissionsKeepsOnlyDeletedIds() {
    when(submissionService.deleteSubmissions(List.of(1000L, 1001L))).thenReturn(List.of(1000L));

    Map<String, Object> result =
        controller.deleteSubmissionsAction("[1000, 1001]", new MockHttpServletRequest());

    Assertions.assertEquals(Boolean.TRUE, result.get("isSuccessful"));
    @SuppressWarnings("unchecked")
    List<Long> deleted = (List<Long>) result.get("deletedSubmissions");
    Assertions.assertEquals(List.of(1000L), deleted);
  }

  /** Test case: tests restartSubmissionsAction(...). Test data: a JSON array of two ids. Expected: both are rejudged. */
  @Test
  public void testRestartSubmissionsRejudgesEachId() {
    Map<String, Boolean> result =
        controller.restartSubmissionsAction("[1000, 1001]", new MockHttpServletRequest());

    Assertions.assertEquals(Boolean.TRUE, result.get("isSuccessful"));
    verify(submissionService).rejudgeSubmissions(List.of(1000L, 1001L));
  }

  /** The mocked SubmissionService. */
  private SubmissionService submissionService;

  /** The SubmissionAdministrationController object under test. */
  private SubmissionAdministrationController controller;
}
