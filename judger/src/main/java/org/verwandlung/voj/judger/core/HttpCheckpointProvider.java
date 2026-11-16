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
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipInputStream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import org.verwandlung.voj.judger.exception.CheckpointsRefusedException;
import org.verwandlung.voj.judger.exception.InvalidCheckpointsException;

/**
 * Downloads a problem's checkpoints from the web application over HTTP.
 *
 * <p>Issues a {@code GET} to {@code <judger.web.baseUrl>/api/judger/checkpoints/<problemId>} with the
 * shared secret in the {@code X-Judger-Token} header and unpacks the returned ZIP into the
 * destination directory. {@link CheckpointStore} owns the local cache (versioning, atomic
 * replacement) and delegates the transfer to this class.
 *
 * @author Haozhe Xie
 */
@Component
public class HttpCheckpointProvider {
  /** The HTTP header carrying the shared secret that authenticates this judger to the web side. */
  private static final String TOKEN_HEADER = "X-Judger-Token";

  /** The entry of the archive that describes the checkpoints; every archive has one. */
  private static final String MANIFEST_FILE = "manifest.properties";

  /**
   * Downloads a problem's checkpoints into the given directory.
   *
   * <p>On return the directory holds a {@code manifest.properties} describing the checkpoints plus,
   * per checkpoint, an {@code input#<id>.txt} and an {@code output#<id>.txt}. {@link
   * CheckpointStore} downloads into a temporary directory and only then swaps it into place, so a
   * failed download never corrupts a usable cache. The whole download, from the request to the
   * last byte, must be over within {@code judger.web.downloadTimeout} seconds.
   *
   * @param problemId - the unique identifier of the problem
   * @param destinationDirectory - the directory the checkpoints are written into
   * @throws InvalidCheckpointsException if the problem has no test data, or its archive is
   *     malformed
   * @throws CheckpointsRefusedException if the web application refuses the download for good
   *     (e.g. HTTP 403, as the judger's API token is not the web application's)
   * @throws IOException if the checkpoints cannot be downloaded for another reason, or not in time
   */
  public void download(long problemId, File destinationDirectory) throws IOException {
    if (!destinationDirectory.exists() && !destinationDirectory.mkdirs()) {
      throw new IOException(
          "Failed to create the checkpoint directory: " + destinationDirectory);
    }

    URI uri =
        URI.create(String.format("%s/api/judger/checkpoints/%d", normalizedBaseUrl(), problemId));
    Duration timeout = Duration.ofSeconds(downloadTimeoutSeconds);
    long deadline = System.nanoTime() + timeout.toNanos();
    // The request timeout covers the wait for the response headers only.
    HttpRequest request =
        HttpRequest.newBuilder(uri)
            .timeout(timeout)
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
    // Closing the body (rather than draining it) also ends the exchange of a failed request.
    try (InputStream body = response.body()) {
      int statusCode = response.statusCode();
      if (statusCode == 404) {
        throw new InvalidCheckpointsException(
            String.format("Problem %d has no test data to download (HTTP 404)", problemId), null);
      }
      if (isRefusal(statusCode)) {
        throw new CheckpointsRefusedException(
            String.format(
                "The web application refused the checkpoints for problem %d: HTTP %d",
                problemId, statusCode),
            statusCode);
      }
      if (statusCode != 200) {
        throw new IOException(
            String.format(
                "Failed to download checkpoints for problem %d: HTTP %d",
                problemId, response.statusCode()));
      }
      extractBefore(deadline, body, destinationDirectory, problemId);
    }
    LOGGER.info("Downloaded checkpoints for problem #{} from {}", problemId, uri);
  }

  /**
   * Checks whether an answer of the web application refuses a download for good: a client error,
   * but for 408 (Request Timeout) and 429 (Too Many Requests), which may be over later. Server
   * errors (5xx), e.g. while the web application restarts behind a proxy, may be over later too.
   *
   * @param statusCode - the HTTP status code of the answer
   * @return whether asking again would get the same answer
   */
  private static boolean isRefusal(int statusCode) {
    return statusCode >= 400 && statusCode < 500 && statusCode != 408 && statusCode != 429;
  }

  /**
   * Unpacks the response body, which must be read completely before the deadline. A read of the
   * body blocks for as long as the web application sends nothing, and the judger judges nothing
   * else meanwhile (while its heartbeat keeps saying it is online), so a watchdog closes the body
   * at the deadline, which makes a blocked read fail.
   *
   * @param deadline - the deadline, in {@link System#nanoTime()} terms
   * @param body - the response body
   * @param destinationDirectory - the directory entries are written into
   * @param problemId - the unique identifier of the problem (for the error message)
   * @throws IOException if the body cannot be unpacked, or not in time
   */
  private void extractBefore(
      long deadline, InputStream body, File destinationDirectory, long problemId)
      throws IOException {
    AtomicBoolean expired = new AtomicBoolean();
    ScheduledFuture<?> watchdog =
        WATCHDOG.schedule(
            () -> {
              expired.set(true);
              try {
                body.close();
              } catch (IOException ignored) {
                // The download fails either way.
              }
            },
            deadline - System.nanoTime(),
            TimeUnit.NANOSECONDS);
    try {
      extract(body, destinationDirectory);
    } catch (IOException ex) {
      if (expired.get()) {
        throw new IOException(
            String.format(
                "Timed out after %d s while downloading checkpoints for problem %d",
                downloadTimeoutSeconds, problemId),
            ex);
      }
      throw ex;
    } finally {
      watchdog.cancel(false);
    }
  }

  /**
   * Unpacks the checkpoint ZIP into the destination directory, guarding against entries that would
   * escape it (a "zip slip").
   *
   * @param zipBody - the ZIP response body
   * @param destinationDirectory - the directory entries are written into
   * @throws InvalidCheckpointsException if the archive is malformed
   * @throws IOException if the body cannot be read or an entry cannot be written
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
          throw new InvalidCheckpointsException(
              "Refusing to extract entry outside the destination directory: " + entry.getName(),
              null);
        }
        try (OutputStream outputStream = new FileOutputStream(outputFile)) {
          zipStream.transferTo(outputStream);
        }
      }
    } catch (ZipException ex) {
      throw new InvalidCheckpointsException("The checkpoint archive is malformed", ex);
    }
    // Something that is no ZIP at all (e.g. an error page of a proxy) has no entries.
    if (!new File(destinationDirectory, MANIFEST_FILE).isFile()) {
      throw new InvalidCheckpointsException(
          "The checkpoint archive has no " + MANIFEST_FILE, null);
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

  /** Closes the body of a download that is not over by its deadline. */
  private static final ScheduledThreadPoolExecutor WATCHDOG = newWatchdog();

  /**
   * Creates the watchdog of the downloads: a single daemon thread, which forgets a download as
   * soon as it is over.
   *
   * @return the watchdog
   */
  private static ScheduledThreadPoolExecutor newWatchdog() {
    ScheduledThreadPoolExecutor watchdog =
        new ScheduledThreadPoolExecutor(
            1,
            runnable -> {
              Thread thread = new Thread(runnable, "checkpoint-download-watchdog");
              thread.setDaemon(true);
              return thread;
            });
    watchdog.setRemoveOnCancelPolicy(true);
    return watchdog;
  }

  /** The shared HTTP client. {@link HttpClient} is thread-safe and intended to be reused. */
  private final HttpClient httpClient =
      HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

  /** The base URL of the web application, e.g. {@code http://localhost:8080/voj}. */
  @Value("${judger.web.baseUrl:http://localhost:8080/voj}")
  private String baseUrl;

  /** How long (seconds) a download may take as a whole, from the request to the last byte. */
  @Value("${judger.web.downloadTimeout:300}")
  private long downloadTimeoutSeconds = 300;

  /** The shared secret presented to the web side; must match its {@code judger.apiToken}. */
  @Value("${judger.apiToken:}")
  private String apiToken;

  /** The logger. */
  private static final Logger LOGGER = LogManager.getLogger(HttpCheckpointProvider.class);
}
