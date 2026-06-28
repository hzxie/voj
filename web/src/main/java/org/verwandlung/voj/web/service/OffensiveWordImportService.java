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
package org.verwandlung.voj.web.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import org.verwandlung.voj.web.util.OffensiveWordFilter;

/**
 * Imports the offensive word dictionary from external word lists.
 *
 * <p>Each configured source is a URL pointing at a plain-text word list (one word per line, blank
 * lines and {@code #} comments ignored), such as the per-category files published by community
 * lexicons. The words from every source are fetched, merged and de-duplicated, then handed to
 * {@link OffensiveWordService} which replaces the dictionary atomically; finally the in-memory
 * {@link OffensiveWordFilter} DFA is rebuilt. Fetching happens outside any database transaction, and
 * an import that yields no words leaves the existing dictionary untouched - so a broken or
 * unreachable source can never wipe the live dictionary.
 *
 * @author Haozhe Xie
 */
@Service
public class OffensiveWordImportService {
  /**
   * Imports the offensive word dictionary from the configured sources.
   *
   * @return a result map describing the overall outcome ({@code isSuccessful}, {@code totalWords},
   *     {@code importedAt}) and a per-source breakdown ({@code sources})
   */
  public Map<String, Object> importFromSources() {
    List<String> urls = parseSourceUrls(offensiveWordService.getOffensiveWordSources());
    Map<String, Object> result = new HashMap<>();
    List<Map<String, Object>> sourceResults = new ArrayList<>();
    result.put("sources", sourceResults);

    if (urls.isEmpty()) {
      result.put("isSuccessful", false);
      result.put("totalWords", 0);
      return result;
    }

    // Merge every source into one de-duplicated set, recording per-source outcomes.
    Set<String> offensiveWords = new LinkedHashSet<>();
    for (String url : urls) {
      Map<String, Object> sourceResult = new HashMap<>();
      sourceResult.put("url", url);
      try {
        int wordsBefore = offensiveWords.size();
        collectOffensiveWords(url, offensiveWords);
        sourceResult.put("isSuccessful", true);
        sourceResult.put("numberOfWords", offensiveWords.size() - wordsBefore);
      } catch (Exception ex) {
        LOGGER.warn("Failed to import offensive words from source: " + url, ex);
        sourceResult.put("isSuccessful", false);
        sourceResult.put("error", ex.getMessage());
      }
      sourceResults.add(sourceResult);
    }

    // Guard: never replace the live dictionary with an empty one (every source failed / was empty).
    if (offensiveWords.isEmpty()) {
      result.put("isSuccessful", false);
      result.put("totalWords", 0);
      return result;
    }

    int numberOfWords = offensiveWordService.replaceAllOffensiveWords(offensiveWords);
    offensiveWordFilter.reload();
    result.put("isSuccessful", true);
    result.put("totalWords", numberOfWords);
    result.put("importedAt", offensiveWordService.getOffensiveWordsImportedAt());
    return result;
  }

  /**
   * Parses the configured sources (one URL per line) into a list of source URLs. Blank lines and
   * {@code #} comments are ignored.
   *
   * @param sources - the configured sources
   * @return the list of source URLs
   */
  private static List<String> parseSourceUrls(String sources) {
    List<String> urls = new ArrayList<>();
    if (sources == null) {
      return urls;
    }
    for (String line : sources.split("\\r?\\n")) {
      String url = line.trim();
      if (!url.isEmpty() && !url.startsWith("#")) {
        urls.add(url);
      }
    }
    return urls;
  }

  /**
   * Fetches one source and adds its words to the given set.
   *
   * @param url - the source URL
   * @param offensiveWords - the set the parsed words are added to
   * @throws IOException if the source cannot be fetched or is not a valid word list
   * @throws InterruptedException if the fetch is interrupted
   */
  private void collectOffensiveWords(String url, Set<String> offensiveWords)
      throws IOException, InterruptedException {
    String body = fetch(url);
    // Some word lists carry a leading UTF-8 byte order mark (U+FEFF) on their first line.
    if (!body.isEmpty() && body.charAt(0) == BYTE_ORDER_MARK) {
      body = body.substring(1);
    }
    for (String line : body.split("\\r?\\n")) {
      String word = line.trim();
      if (!word.isEmpty() && !word.startsWith("#") && word.length() <= MAX_WORD_LENGTH) {
        offensiveWords.add(word);
      }
    }
  }

  /**
   * Fetches the body of an http(s) URL, capped at {@link #MAX_RESPONSE_BYTES} bytes.
   *
   * @param url - the URL to fetch
   * @return the response body decoded as UTF-8
   * @throws IOException if the URL is not http(s), the response is not 200, the response is not a
   *     plain-text word list, or it is too large
   * @throws InterruptedException if the fetch is interrupted
   */
  private String fetch(String url) throws IOException, InterruptedException {
    URI uri = URI.create(url);
    String scheme = uri.getScheme();
    if (scheme == null
        || !(scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https"))) {
      throw new IOException("Only http(s) sources are allowed.");
    }

    HttpRequest request = HttpRequest.newBuilder(uri).timeout(REQUEST_TIMEOUT).GET().build();
    HttpResponse<InputStream> response =
        httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
    if (response.statusCode() != 200) {
      throw new IOException("Unexpected HTTP status: " + response.statusCode());
    }
    // Reject anything that is not a plain-text word list: a URL that points at a web page (e.g. a
    // repository's file-view page rather than the raw file, or a soft-404 / login / error page that
    // still answers 200) would otherwise have every line of its HTML stored as an offensive word.
    String contentType = response.headers().firstValue("Content-Type").orElse("");
    if (!isPlainTextContentType(contentType)) {
      throw new IOException(
          "Source is not a plain-text word list (Content-Type: " + contentType + ").");
    }
    try (InputStream in = response.body()) {
      return readBoundedUtf8(in);
    }
  }

  /**
   * Decides whether a response Content-Type denotes a plain-text word list. Textual types are
   * accepted (apart from HTML), as is {@code application/octet-stream} - the type {@code
   * raw.githubusercontent.com} and many static hosts serve raw files as - and an absent Content-Type,
   * which some static hosts omit for {@code .txt} files.
   *
   * @param contentType - the raw Content-Type response header, possibly empty
   * @return whether the response should be parsed as a word list
   */
  private static boolean isPlainTextContentType(String contentType) {
    String type = contentType.toLowerCase(Locale.ROOT).trim();
    // Drop any ";charset=..." parameter.
    int semicolon = type.indexOf(';');
    if (semicolon >= 0) {
      type = type.substring(0, semicolon).trim();
    }
    if (type.isEmpty() || type.equals("application/octet-stream")) {
      return true;
    }
    return type.startsWith("text/") && !type.equals("text/html");
  }

  /**
   * Reads an input stream into a UTF-8 string, rejecting bodies larger than {@link
   * #MAX_RESPONSE_BYTES} bytes so that a malicious or runaway source cannot exhaust memory.
   *
   * @param in - the input stream
   * @return the stream contents decoded as UTF-8
   * @throws IOException if the stream exceeds the size cap
   */
  private static String readBoundedUtf8(InputStream in) throws IOException {
    ByteArrayOutputStream buffer = new ByteArrayOutputStream();
    byte[] chunk = new byte[8192];
    int total = 0;
    int read;
    while ((read = in.read(chunk)) != -1) {
      total += read;
      if (total > MAX_RESPONSE_BYTES) {
        throw new IOException("Source exceeds the maximum allowed size.");
      }
      buffer.write(chunk, 0, read);
    }
    return buffer.toString(StandardCharsets.UTF_8);
  }

  /** The Unicode byte order mark, stripped from the start of a fetched word list when present. */
  private static final char BYTE_ORDER_MARK = (char) 0xFEFF;

  /** The longest line accepted as a single offensive word (longer lines are skipped). */
  private static final int MAX_WORD_LENGTH = 128;

  /** The maximum size of a single source response, in bytes (16 MiB). */
  private static final int MAX_RESPONSE_BYTES = 16 * 1024 * 1024;

  /** The per-request read timeout. */
  private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(20);

  /** The shared HTTP client used to fetch the sources. */
  private final HttpClient httpClient =
      HttpClient.newBuilder()
          .connectTimeout(Duration.ofSeconds(10))
          .followRedirects(HttpClient.Redirect.NORMAL)
          .build();

  /** The log4j2 logger. */
  private static final Logger LOGGER = LogManager.getLogger(OffensiveWordImportService.class);

  /** The autowired OffensiveWordService object. */
  @Autowired private OffensiveWordService offensiveWordService;

  /** The autowired OffensiveWordFilter object. */
  @Autowired private OffensiveWordFilter offensiveWordFilter;
}
