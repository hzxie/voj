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

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import jakarta.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import org.verwandlung.voj.web.mapper.CheckpointMapper;
import org.verwandlung.voj.web.mapper.ProblemMapper;
import org.verwandlung.voj.web.model.Checkpoint;

/**
 * Serves a problem's checkpoints (test data) to the judgers over HTTP.
 *
 * <p>This is the distribution side of the file-based checkpoint storage: the database remains the
 * source of truth, but instead of every judger reading the {@code longtext} input/output columns on
 * each submission, a judger downloads a problem's checkpoints once (as a single ZIP) and caches them
 * locally, re-fetching only when the problem's {@code checkpoints_version} changes. Routing the data
 * through this endpoint (rather than a shared database read) keeps the storage backend pluggable —
 * the judger only knows how to GET a ZIP, so the source can later move to object storage without
 * touching the judger.
 *
 * <p>The endpoint is guarded by a shared secret ({@code judger.apiToken}) submitted in the {@code
 * X-Judger-Token} header, because the ZIP contains the reference answers and must not be publicly
 * readable. It is a {@code GET}, so Spring Security's CSRF protection does not apply.
 *
 * @author Haozhe Xie
 */
@RestController
@RequestMapping("/api/judger")
public class CheckpointApiController {
  /** The HTTP header carrying the shared secret that authenticates a judger. */
  private static final String TOKEN_HEADER = "X-Judger-Token";

  /** The response header that reports the version of the checkpoints contained in the ZIP. */
  private static final String VERSION_HEADER = "X-Checkpoints-Version";

  /** The name of the manifest entry inside the ZIP that lists each checkpoint's metadata. */
  private static final String MANIFEST_ENTRY = "manifest.properties";

  /**
   * Streams a problem's checkpoints to a judger as a ZIP archive.
   *
   * <p>The archive contains a {@code manifest.properties} describing every checkpoint (id, score and
   * whether an exact match is required) plus, per checkpoint, an {@code input#<id>.txt} and an {@code
   * output#<id>.txt}. The current checkpoints version is reported both in the manifest and in the
   * {@value #VERSION_HEADER} response header.
   *
   * @param problemId - the unique identifier of the problem
   * @param token - the shared secret submitted by the judger
   * @param response - the servlet response the ZIP is written to
   * @throws IOException if the ZIP cannot be written to the response
   */
  @GetMapping("/checkpoints/{problemId}")
  public void getCheckpoints(
      @PathVariable("problemId") long problemId,
      @RequestHeader(value = TOKEN_HEADER, required = false) String token,
      HttpServletResponse response)
      throws IOException {
    if (!isAuthorized(token)) {
      response.sendError(HttpServletResponse.SC_FORBIDDEN);
      return;
    }

    Integer version = problemMapper.getCheckpointsVersion(problemId);
    if (version == null) {
      response.sendError(HttpServletResponse.SC_NOT_FOUND);
      return;
    }
    List<Checkpoint> checkpoints = checkpointMapper.getCheckpointsUsingProblemId(problemId);

    response.setStatus(HttpServletResponse.SC_OK);
    response.setContentType("application/zip");
    response.setHeader(VERSION_HEADER, String.valueOf(version));
    response.setHeader(
        "Content-Disposition", "attachment; filename=checkpoints-" + problemId + ".zip");

    try (ZipOutputStream zipStream = new ZipOutputStream(response.getOutputStream())) {
      writeEntry(zipStream, MANIFEST_ENTRY, buildManifest(version, checkpoints));
      for (Checkpoint checkpoint : checkpoints) {
        int checkpointId = checkpoint.getCheckpointId();
        writeEntry(
            zipStream,
            String.format("input#%d.txt", checkpointId),
            checkpoint.getInput().getBytes(StandardCharsets.UTF_8));
        writeEntry(
            zipStream,
            String.format("output#%d.txt", checkpointId),
            checkpoint.getOutput().getBytes(StandardCharsets.UTF_8));
      }
    }
    LOGGER.info(
        "Served {} checkpoint(s) (version {}) for problem #{}",
        checkpoints.size(),
        version,
        problemId);
  }

  /**
   * Checks the shared secret in constant time. The endpoint is refused outright when no token is
   * configured, so a blank {@code judger.apiToken} cannot accidentally expose the reference answers.
   *
   * @param token - the token submitted by the caller
   * @return whether the caller is authorized
   */
  private boolean isAuthorized(String token) {
    if (apiToken == null || apiToken.isBlank() || token == null) {
      return false;
    }
    return MessageDigest.isEqual(
        apiToken.getBytes(StandardCharsets.UTF_8), token.getBytes(StandardCharsets.UTF_8));
  }

  /**
   * Builds the manifest describing the checkpoints. The judger reads this to learn each checkpoint's
   * id and score without touching the database.
   *
   * @param version - the current checkpoints version
   * @param checkpoints - the problem's checkpoints
   * @return the manifest as UTF-8 bytes
   */
  private byte[] buildManifest(int version, List<Checkpoint> checkpoints) {
    StringBuilder manifest = new StringBuilder();
    manifest.append("version=").append(version).append('\n');
    manifest.append("count=").append(checkpoints.size()).append('\n');
    for (int i = 0; i < checkpoints.size(); ++i) {
      Checkpoint checkpoint = checkpoints.get(i);
      manifest.append("checkpoint.").append(i).append(".id=").append(checkpoint.getCheckpointId());
      manifest.append('\n');
      manifest.append("checkpoint.").append(i).append(".score=").append(checkpoint.getScore());
      manifest.append('\n');
      manifest
          .append("checkpoint.")
          .append(i)
          .append(".exactlyMatch=")
          .append(checkpoint.isExactlyMatch());
      manifest.append('\n');
    }
    return manifest.toString().getBytes(StandardCharsets.UTF_8);
  }

  /**
   * Writes a single entry into the ZIP archive.
   *
   * @param zipStream - the open ZIP output stream
   * @param name - the entry name
   * @param content - the entry content
   * @throws IOException if the entry cannot be written
   */
  private void writeEntry(ZipOutputStream zipStream, String name, byte[] content)
      throws IOException {
    zipStream.putNextEntry(new ZipEntry(name));
    zipStream.write(content);
    zipStream.closeEntry();
  }

  /** The shared secret a judger must present to download checkpoints; empty disables the endpoint. */
  @Value("${judger.apiToken:}")
  private String apiToken;

  /** The autowired ProblemMapper object, used to read a problem's checkpoints version. */
  @Autowired private ProblemMapper problemMapper;

  /** The autowired CheckpointMapper object, used to read a problem's checkpoints. */
  @Autowired private CheckpointMapper checkpointMapper;

  /** The logger. */
  private static final Logger LOGGER = LogManager.getLogger(CheckpointApiController.class);
}
