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
package org.verwandlung.voj.judger.core;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Downloads a problem's checkpoints from the web application over HTTP.
 *
 * <p>Issues a {@code GET} to {@code <judger.web.baseUrl>/api/judger/checkpoints/<problemId>} with the
 * shared secret in the {@code X-Judger-Token} header and unpacks the returned ZIP into the
 * destination directory.
 *
 * @author Haozhe Xie
 */
@Component
public class HttpCheckpointProvider implements CheckpointProvider {
  /** The HTTP header carrying the shared secret that authenticates this judger to the web side. */
  private static final String TOKEN_HEADER = "X-Judger-Token";

  /*
   * (non-Javadoc)
   * @see CheckpointProvider#download(long, File)
   */
  @Override
  public void download(long problemId, File destinationDirectory) throws IOException {
    if (!destinationDirectory.exists() && !destinationDirectory.mkdirs()) {
      throw new IOException(
          "Failed to create the checkpoint directory: " + destinationDirectory);
    }

    URI uri =
        URI.create(String.format("%s/api/judger/checkpoints/%d", normalizedBaseUrl(), problemId));
    HttpRequest request =
        HttpRequest.newBuilder(uri)
            .timeout(Duration.ofSeconds(60))
            .header(TOKEN_HEADER, apiToken == null ? "" : apiToken)
            .GET()
            .build();

    HttpResponse<InputStream> response;
    try {
      response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
      throw new IOException(
          "Interrupted while downloading checkpoints for problem " + problemId, ex);
    }
    if (response.statusCode() != 200) {
      // Drain and close the body so the connection can be reused.
      try (InputStream body = response.body()) {
        body.readAllBytes();
      } catch (IOException ignored) {
        // Nothing actionable; we are already failing.
      }
      throw new IOException(
          String.format(
              "Failed to download checkpoints for problem %d: HTTP %d",
              problemId, response.statusCode()));
    }

    extract(response.body(), destinationDirectory);
    LOGGER.info("Downloaded checkpoints for problem #{} from {}", problemId, uri);
  }

  /**
   * Unpacks the checkpoint ZIP into the destination directory, guarding against entries that would
   * escape it (a "zip slip").
   *
   * @param zipBody - the ZIP response body
   * @param destinationDirectory - the directory entries are written into
   * @throws IOException if an entry cannot be written
   */
  private void extract(InputStream zipBody, File destinationDirectory) throws IOException {
    String destinationPrefix = destinationDirectory.getCanonicalPath() + File.separator;
    try (ZipInputStream zipStream = new ZipInputStream(zipBody)) {
      ZipEntry entry;
      while ((entry = zipStream.getNextEntry()) != null) {
        if (entry.isDirectory()) {
          continue;
        }
        File outputFile = new File(destinationDirectory, entry.getName());
        if (!outputFile.getCanonicalPath().startsWith(destinationPrefix)) {
          throw new IOException(
              "Refusing to extract entry outside the destination directory: " + entry.getName());
        }
        try (OutputStream outputStream = new FileOutputStream(outputFile)) {
          zipStream.transferTo(outputStream);
        }
      }
    }
  }

  /**
   * Returns the configured base URL without a trailing slash, so it can be concatenated with the
   * absolute endpoint path.
   *
   * @return the normalized base URL
   */
  private String normalizedBaseUrl() {
    if (baseUrl.endsWith("/")) {
      return baseUrl.substring(0, baseUrl.length() - 1);
    }
    return baseUrl;
  }

  /** The shared HTTP client. {@link HttpClient} is thread-safe and intended to be reused. */
  private final HttpClient httpClient =
      HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

  /** The base URL of the web application, e.g. {@code http://localhost:8080/voj}. */
  @Value("${judger.web.baseUrl:http://localhost:8080/voj}")
  private String baseUrl;

  /** The shared secret presented to the web side; must match its {@code judger.apiToken}. */
  @Value("${judger.apiToken:}")
  private String apiToken;

  /** The logger. */
  private static final Logger LOGGER = LogManager.getLogger(HttpCheckpointProvider.class);
}
