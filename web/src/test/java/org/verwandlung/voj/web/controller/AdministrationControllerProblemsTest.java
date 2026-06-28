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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import org.verwandlung.voj.web.model.Problem;
import org.verwandlung.voj.web.service.ProblemService;

/**
 * The test class for AdministrationController - the problem-administration area.
 *
 * <p>Covers the controller-specific logic: the non-numeric time/memory-limit sanitization shared by
 * create/edit, the test-data archive parser that pairs {@code NN.in}/{@code NN.out} entries, and the
 * existence-gated checkpoint deletion.
 *
 * @author Haozhe Xie
 */
public class AdministrationControllerProblemsTest {
  /** Wires a mocked ProblemService into a fresh controller before every test. */
  @BeforeEach
  public void setUp() {
    problemService = mock(ProblemService.class);
    controller = new AdministrationController();
    ReflectionTestUtils.setField(controller, "problemService", problemService);
  }

  /** Test case: tests createProblemAction(...). Test data: an empty time limit and a non-numeric memory limit. Expected: both are sanitized to -1 before the service call. */
  @Test
  public void testCreateProblemSanitizesNonNumericLimits() {
    when(problemService.createProblem(
            anyString(), anyInt(), anyInt(), anyString(), anyString(), anyString(), anyString(),
            anyString(), anyString(), anyString(), anyString(), anyString(), anyString(),
            anyBoolean(), anyString()))
        .thenReturn(falseResult());

    controller.createProblemAction(
        "A+B", "", "not-a-number", "d", "h", "if", "of", "is", "os", "[]", "[]", "[]", "PUBLISHED",
        true, "", new MockHttpServletRequest());

    verify(problemService)
        .createProblem(
            eq("A+B"), eq(-1), eq(-1), anyString(), anyString(), anyString(), anyString(),
            anyString(), anyString(), anyString(), anyString(), anyString(), anyString(),
            anyBoolean(), anyString());
  }

  /** Test case: tests createProblemAction(...). Test data: numeric time/memory limits and a successful creation. Expected: the parsed integer limits are passed through. */
  @Test
  public void testCreateProblemPassesNumericLimits() {
    Map<String, Object> result = new HashMap<>();
    result.put("isSuccessful", true);
    result.put("problemId", 1000L);
    when(problemService.createProblem(
            anyString(), anyInt(), anyInt(), anyString(), anyString(), anyString(), anyString(),
            anyString(), anyString(), anyString(), anyString(), anyString(), anyString(),
            anyBoolean(), anyString()))
        .thenReturn(result);

    controller.createProblemAction(
        "A+B", "1000", "256", "d", "h", "if", "of", "is", "os", "[]", "[]", "[]", "PUBLISHED",
        true, "", new MockHttpServletRequest());

    verify(problemService)
        .createProblem(
            eq("A+B"), eq(1000), eq(256), anyString(), anyString(), anyString(), anyString(),
            anyString(), anyString(), anyString(), anyString(), anyString(), anyString(),
            anyBoolean(), anyString());
  }

  /** Test case: tests editProblemAction(...). Test data: non-numeric limits. Expected: both are sanitized to -1 before the service call. */
  @Test
  public void testEditProblemSanitizesNonNumericLimits() {
    Map<String, Boolean> result = new HashMap<>();
    result.put("isSuccessful", false);
    when(problemService.editProblem(
            anyLong(), anyString(), anyInt(), anyInt(), anyString(), anyString(), anyString(),
            anyString(), anyString(), anyString(), anyString(), anyString(), anyString(),
            anyBoolean(), anyString()))
        .thenReturn(result);

    controller.editProblemAction(
        1000L, "A+B", "abc", "", "d", "h", "if", "of", "is", "os", "[]", "[]", "PUBLISHED", false,
        "", new MockHttpServletRequest());

    verify(problemService)
        .editProblem(
            eq(1000L), eq("A+B"), eq(-1), eq(-1), anyString(), anyString(), anyString(),
            anyString(), anyString(), anyString(), anyString(), anyString(), anyString(),
            anyBoolean(), anyString());
  }

  /** Test case: tests uploadProblemTestDataAction(...). Test data: a non-existent problem. Expected: isProblemExists and isSuccessful are false and nothing is parsed. */
  @Test
  public void testUploadTestDataProblemNotFound() {
    when(problemService.getProblem(9999L)).thenReturn(null);
    MockMultipartFile archive = new MockMultipartFile("testDataArchive", "td.zip", "application/zip",
        new byte[] {1, 2, 3});

    Map<String, Object> result =
        controller.uploadProblemTestDataAction(9999L, true, archive, new MockHttpServletRequest());

    Assertions.assertEquals(Boolean.FALSE, result.get("isProblemExists"));
    Assertions.assertEquals(Boolean.FALSE, result.get("isSuccessful"));
    verify(problemService, never()).replaceTestCases(anyLong(), any(), anyBoolean());
  }

  /** Test case: tests uploadProblemTestDataAction(...). Test data: an absent (empty) archive. Expected: isArchiveEmpty is true and the upload is rejected. */
  @Test
  public void testUploadTestDataEmptyArchive() {
    when(problemService.getProblem(1000L)).thenReturn(new Problem());
    MockMultipartFile archive =
        new MockMultipartFile("testDataArchive", "td.zip", "application/zip", new byte[0]);

    Map<String, Object> result =
        controller.uploadProblemTestDataAction(1000L, true, archive, new MockHttpServletRequest());

    Assertions.assertEquals(Boolean.TRUE, result.get("isArchiveEmpty"));
    Assertions.assertEquals(Boolean.FALSE, result.get("isSuccessful"));
    verify(problemService, never()).replaceTestCases(anyLong(), any(), anyBoolean());
  }

  /** Test case: tests uploadProblemTestDataAction(...). Test data: a zip with two paired and one unmatched checkpoint. Expected: only the two pairs are extracted, in order, and replace the test cases. */
  @Test
  public void testUploadTestDataPairsCheckpoints() throws IOException {
    when(problemService.getProblem(1000L)).thenReturn(new Problem());
    byte[] zip =
        zip(
            entry("1.in", "1 2"),
            entry("1.out", "3"),
            entry("2.in", "4 5"),
            entry("2.ans", "9"),
            entry("3.in", "lonely input")); // no matching output -> ignored
    MockMultipartFile archive =
        new MockMultipartFile("testDataArchive", "td.zip", "application/zip", zip);

    Map<String, Object> result =
        controller.uploadProblemTestDataAction(1000L, false, archive, new MockHttpServletRequest());

    Assertions.assertEquals(Boolean.TRUE, result.get("isSuccessful"));

    @SuppressWarnings("unchecked")
    ArgumentCaptor<List<Map<String, String>>> captor = ArgumentCaptor.forClass(List.class);
    verify(problemService).replaceTestCases(eq(1000L), captor.capture(), eq(false));
    List<Map<String, String>> testCases = captor.getValue();
    Assertions.assertEquals(2, testCases.size());
    Assertions.assertEquals("1 2", testCases.get(0).get("input"));
    Assertions.assertEquals("3", testCases.get(0).get("output"));
    Assertions.assertEquals("4 5", testCases.get(1).get("input"));
    Assertions.assertEquals("9", testCases.get(1).get("output"));
  }

  /** Test case: tests deleteProblemCheckpointAction(...). Test data: an existing problem. Expected: the checkpoint is deleted and isSuccessful is true. */
  @Test
  public void testDeleteCheckpointExisting() {
    when(problemService.getProblem(1000L)).thenReturn(new Problem());

    Map<String, Object> result = controller.deleteProblemCheckpointAction(1000L, 2);

    Assertions.assertEquals(Boolean.TRUE, result.get("isSuccessful"));
    verify(problemService).deleteCheckpoint(1000L, 2);
  }

  /** Test case: tests deleteProblemCheckpointAction(...). Test data: a non-existent problem. Expected: isSuccessful is false and no checkpoint is deleted. */
  @Test
  public void testDeleteCheckpointMissingProblem() {
    when(problemService.getProblem(9999L)).thenReturn(null);

    Map<String, Object> result = controller.deleteProblemCheckpointAction(9999L, 2);

    Assertions.assertEquals(Boolean.FALSE, result.get("isProblemExists"));
    Assertions.assertEquals(Boolean.FALSE, result.get("isSuccessful"));
    verify(problemService, never()).deleteCheckpoint(anyLong(), anyInt());
  }

  /** Test case: tests deleteProblemsAction(...). Test data: a JSON array of two problem ids. Expected: each problem is deleted and isSuccessful is true. */
  @Test
  public void testDeleteProblemsActionDeletesEach() {
    Map<String, Boolean> result =
        controller.deleteProblemsAction("[1000, 1001]", new MockHttpServletRequest());

    Assertions.assertEquals(Boolean.TRUE, result.get("isSuccessful"));
    verify(problemService).deleteProblem(1000L);
    verify(problemService).deleteProblem(1001L);
  }

  /** A minimal failure result for the create/edit stubs that should not enter the success branch. */
  private static Map<String, Object> falseResult() {
    Map<String, Object> result = new HashMap<>();
    result.put("isSuccessful", false);
    return result;
  }

  /** A single (name, content) zip entry. */
  private static Map.Entry<String, String> entry(String name, String content) {
    return Map.entry(name, content);
  }

  /** Builds an in-memory zip archive from the given (name, content) entries. */
  @SafeVarargs
  private static byte[] zip(Map.Entry<String, String>... entries) throws IOException {
    ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    try (ZipOutputStream zipStream = new ZipOutputStream(bytes)) {
      for (Map.Entry<String, String> entry : entries) {
        zipStream.putNextEntry(new ZipEntry(entry.getKey()));
        zipStream.write(entry.getValue().getBytes(java.nio.charset.StandardCharsets.UTF_8));
        zipStream.closeEntry();
      }
    }
    return bytes.toByteArray();
  }

  /** The mocked ProblemService. */
  private ProblemService problemService;

  /** The AdministrationController object under test. */
  private AdministrationController controller;
}
