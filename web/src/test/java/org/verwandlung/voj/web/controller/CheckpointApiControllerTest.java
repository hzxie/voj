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
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import org.verwandlung.voj.web.mapper.CheckpointMapper;
import org.verwandlung.voj.web.mapper.ProblemMapper;
import org.verwandlung.voj.web.model.Checkpoint;

/**
 * The test class for CheckpointApiController.
 *
 * @author Haozhe Xie
 */
public class CheckpointApiControllerTest {
  /** The shared secret the controller is configured with for the tests. */
  private static final String API_TOKEN = "s3cr3t-judger-token";

  /** Wires mocked mappers and a known API token into a fresh controller before every test. */
  @BeforeEach
  public void setUp() {
    problemMapper = mock(ProblemMapper.class);
    checkpointMapper = mock(CheckpointMapper.class);

    CheckpointApiController controller = new CheckpointApiController();
    ReflectionTestUtils.setField(controller, "problemMapper", problemMapper);
    ReflectionTestUtils.setField(controller, "checkpointMapper", checkpointMapper);
    ReflectionTestUtils.setField(controller, "apiToken", API_TOKEN);
    mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
  }

  /** Test case: tests GET /api/judger/checkpoints/{problemId}. Test data: a request without the token header. Expected: HTTP 403 Forbidden. */
  @Test
  public void testGetCheckpointsWithoutToken() throws Exception {
    mockMvc.perform(get("/api/judger/checkpoints/1000")).andExpect(status().isForbidden());
  }

  /** Test case: tests GET /api/judger/checkpoints/{problemId}. Test data: a request with the wrong token. Expected: HTTP 403 Forbidden. */
  @Test
  public void testGetCheckpointsWithWrongToken() throws Exception {
    mockMvc
        .perform(get("/api/judger/checkpoints/1000").header("X-Judger-Token", "wrong-token"))
        .andExpect(status().isForbidden());
  }

  /** Test case: tests GET /api/judger/checkpoints/{problemId}. Test data: a valid token but a problem that does not exist. Expected: HTTP 404 Not Found. */
  @Test
  public void testGetCheckpointsForUnknownProblem() throws Exception {
    when(problemMapper.getCheckpointsVersion(404L)).thenReturn(null);

    mockMvc
        .perform(get("/api/judger/checkpoints/404").header("X-Judger-Token", API_TOKEN))
        .andExpect(status().isNotFound());
  }

  /** Test case: tests GET /api/judger/checkpoints/{problemId}. Test data: a valid token and a problem with two checkpoints. Expected: HTTP 200 with a ZIP carrying the version header, a manifest, and an input/output entry per checkpoint. */
  @Test
  public void testGetCheckpointsReturnsZip() throws Exception {
    when(problemMapper.getCheckpointsVersion(1000L)).thenReturn(7);
    List<Checkpoint> checkpoints =
        Arrays.asList(
            new Checkpoint(1000, 0, true, 50, "1 2", "3"),
            new Checkpoint(1000, 1, true, 50, "4 5", "9"));
    when(checkpointMapper.getCheckpointsUsingProblemId(1000L)).thenReturn(checkpoints);

    MvcResult result =
        mockMvc
            .perform(get("/api/judger/checkpoints/1000").header("X-Judger-Token", API_TOKEN))
            .andExpect(status().isOk())
            .andExpect(header().string("X-Checkpoints-Version", "7"))
            .andExpect(header().string("Content-Type", "application/zip"))
            .andReturn();

    Map<String, String> entries = readZip(result.getResponse().getContentAsByteArray());
    Assertions.assertTrue(entries.containsKey("manifest.properties"));
    Assertions.assertTrue(entries.get("manifest.properties").contains("version=7"));
    Assertions.assertTrue(entries.get("manifest.properties").contains("count=2"));
    Assertions.assertEquals("1 2", entries.get("input#0.txt"));
    Assertions.assertEquals("3", entries.get("output#0.txt"));
    Assertions.assertEquals("4 5", entries.get("input#1.txt"));
    Assertions.assertEquals("9", entries.get("output#1.txt"));
  }

  /** Reads every entry of a ZIP archive into a name-to-content map. */
  private Map<String, String> readZip(byte[] zipBytes) throws Exception {
    Map<String, String> entries = new HashMap<>();
    try (ZipInputStream zipStream = new ZipInputStream(new ByteArrayInputStream(zipBytes))) {
      ZipEntry entry;
      while ((entry = zipStream.getNextEntry()) != null) {
        List<Byte> buffer = new ArrayList<>();
        int read;
        byte[] chunk = new byte[256];
        while ((read = zipStream.read(chunk)) != -1) {
          for (int i = 0; i < read; ++i) {
            buffer.add(chunk[i]);
          }
        }
        byte[] content = new byte[buffer.size()];
        for (int i = 0; i < content.length; ++i) {
          content[i] = buffer.get(i);
        }
        entries.put(entry.getName(), new String(content, StandardCharsets.UTF_8));
      }
    }
    return entries;
  }

  /** The mocked ProblemMapper resolving the checkpoints version. */
  private ProblemMapper problemMapper;

  /** The mocked CheckpointMapper resolving a problem's checkpoints. */
  private CheckpointMapper checkpointMapper;

  /** The MockMvc driving the controller under test. */
  private MockMvc mockMvc;
}
