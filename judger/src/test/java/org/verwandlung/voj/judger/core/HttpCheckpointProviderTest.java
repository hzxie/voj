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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Tests for HttpCheckpointProvider against a stub of the web application's checkpoint endpoint.
 *
 * @author Haozhe Xie
 */
public class HttpCheckpointProviderTest {
  /** Releases the handlers that stall and stops the stub server. */
  @AfterEach
  public void tearDown() {
    release.countDown();
    if (server != null) {
      server.stop(0);
    }
  }

  /**
   * Test case: the web application answers with the checkpoints. Expected result: the archive is
   * unpacked into the destination directory.
   */
  @Test
  public void testDownloadUnpacksTheArchive() throws Exception {
    byte[] archive = archive();
    startServer(
        exchange -> {
          exchange.sendResponseHeaders(200, archive.length);
          try (OutputStream body = exchange.getResponseBody()) {
            body.write(archive);
          }
        });

    provider.download(1000, tempDir.toFile());

    Assertions.assertEquals(
        "version=1\ncount=0\n", Files.readString(tempDir.resolve("manifest.properties")));
  }

  /**
   * Test case: the web application sends the headers and part of the archive, then nothing more
   * (e.g. it hangs while restarting). Expected result: the download fails once its deadline has
   * passed, rather than blocking the judger for good.
   */
  @Test
  public void testStalledBodyTimesOut() throws Exception {
    byte[] archive = archive();
    startServer(
        exchange -> {
          exchange.sendResponseHeaders(200, archive.length);
          OutputStream body = exchange.getResponseBody();
          body.write(archive, 0, archive.length / 2);
          body.flush();
          stall();
        });

    assertFailsWithin(5, "Timed out");
  }

  /**
   * Test case: the web application accepts the request but never answers. Expected result: the
   * download fails once its deadline has passed.
   */
  @Test
  public void testStalledResponseTimesOut() throws Exception {
    startServer(exchange -> stall());

    assertFailsWithin(5, null);
  }

  /**
   * Starts the stub of the web application, on the loopback interface, and points the provider at
   * it with a deadline of one second.
   *
   * @param handler - the handler of the checkpoint endpoint
   */
  private void startServer(HttpHandler handler) throws IOException {
    server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
    server.createContext("/voj/api/judger/checkpoints/", handler);
    server.setExecutor(Executors.newCachedThreadPool());
    server.start();
    ReflectionTestUtils.setField(
        provider,
        "baseUrl",
        "http://localhost:" + server.getAddress().getPort() + "/voj");
    ReflectionTestUtils.setField(provider, "apiToken", "token");
    ReflectionTestUtils.setField(provider, "downloadTimeoutSeconds", 1L);
  }

  /**
   * Asserts that a download fails, and soon.
   *
   * @param seconds - how long the download may take to fail
   * @param message - a part of the expected message, or {@code null}
   */
  private void assertFailsWithin(int seconds, String message) {
    long start = System.nanoTime();
    IOException ex =
        Assertions.assertThrows(IOException.class, () -> provider.download(1000, tempDir.toFile()));
    Assertions.assertTrue(
        System.nanoTime() - start < TimeUnit.SECONDS.toNanos(seconds), "Took too long to fail");
    if (message != null) {
      Assertions.assertTrue(ex.getMessage().contains(message), ex.getMessage());
    }
  }

  /** Blocks a handler of the stub until the test is over. */
  private void stall() {
    try {
      release.await(30, TimeUnit.SECONDS);
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
    }
  }

  /**
   * Builds an archive of checkpoints, as the web application does.
   *
   * @return the archive
   */
  private static byte[] archive() throws IOException {
    ByteArrayOutputStream archive = new ByteArrayOutputStream();
    try (ZipOutputStream zipStream = new ZipOutputStream(archive)) {
      zipStream.putNextEntry(new ZipEntry("manifest.properties"));
      zipStream.write("version=1\ncount=0\n".getBytes(StandardCharsets.UTF_8));
      zipStream.closeEntry();
      // Large (and incompressible) enough not to fit into the half the stalled handler sends.
      byte[] input = new byte[64 * 1024];
      new Random(1).nextBytes(input);
      zipStream.putNextEntry(new ZipEntry("input#1.txt"));
      zipStream.write(input);
      zipStream.closeEntry();
    }
    return archive.toByteArray();
  }

  /** Lets the stalled handlers of the stub finish. */
  private final CountDownLatch release = new CountDownLatch(1);

  /** The HttpCheckpointProvider object under test. */
  private final HttpCheckpointProvider provider = new HttpCheckpointProvider();

  /** The stub of the web application. */
  private HttpServer server;

  /** The destination directory of the downloads. */
  @TempDir Path tempDir;
}
