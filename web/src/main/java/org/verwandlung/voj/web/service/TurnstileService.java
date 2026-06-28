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

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import org.verwandlung.voj.web.mapper.OptionMapper;
import org.verwandlung.voj.web.model.Option;
import org.verwandlung.voj.web.util.JsonUtils;

/**
 * Verifies Cloudflare Turnstile responses for the public, abuse-prone forms (login, registration
 * and password reset). The feature is controlled entirely by the {@code turnstileEnabled}, {@code
 * turnstileSiteKey} and {@code turnstileSecretKey} options configured from the administration
 * console: when it is disabled (the default) every verification trivially passes, so the site keeps
 * working before an administrator provisions Cloudflare credentials.
 *
 * @author Haozhe Xie
 */
@Service
public class TurnstileService {
  /**
   * Checks whether Turnstile verification is currently enabled.
   *
   * @return whether the {@code turnstileEnabled} option is on and a secret key is configured
   */
  public boolean isEnabled() {
    Option enabled = optionMapper.getOption("turnstileEnabled");
    if (enabled == null || !"1".equals(enabled.getOptionValue())) {
      return false;
    }
    Option secretKey = optionMapper.getOption("turnstileSecretKey");
    return secretKey != null && !secretKey.getOptionValue().isEmpty();
  }

  /**
   * Verifies a Turnstile response token against Cloudflare's siteverify endpoint. When the feature
   * is disabled the token is not checked and verification passes; when it is enabled an empty token
   * or any non-successful / unreachable response fails closed.
   *
   * @param token - the {@code cf-turnstile-response} token submitted by the widget
   * @param remoteIp - the client IP address, forwarded to Cloudflare for risk analysis
   * @return whether the request is allowed to proceed
   */
  public boolean verify(String token, String remoteIp) {
    if (!isEnabled()) {
      return true;
    }
    if (token == null || token.isEmpty()) {
      return false;
    }
    String secretKey = optionMapper.getOption("turnstileSecretKey").getOptionValue();
    try {
      String form =
          "secret=" + encode(secretKey) + "&response=" + encode(token);
      if (remoteIp != null && !remoteIp.isEmpty()) {
        form += "&remoteip=" + encode(remoteIp);
      }
      HttpRequest request =
          HttpRequest.newBuilder(URI.create(SITE_VERIFY_URL))
              .timeout(REQUEST_TIMEOUT)
              .header("Content-Type", "application/x-www-form-urlencoded")
              .POST(HttpRequest.BodyPublishers.ofString(form, StandardCharsets.UTF_8))
              .build();
      HttpResponse<String> response =
          httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
      if (response.statusCode() != 200) {
        LOGGER.warn("Turnstile siteverify returned HTTP status " + response.statusCode());
        return false;
      }
      Map<String, Object> result = JsonUtils.toMap(response.body());
      return Boolean.TRUE.equals(result.get("success"));
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
      LOGGER.error("Turnstile verification was interrupted.", ex);
      return false;
    } catch (Exception ex) {
      LOGGER.error("Failed to verify the Turnstile response.", ex);
      return false;
    }
  }

  /** URL-encodes a form value as UTF-8. */
  private static String encode(String value) {
    return URLEncoder.encode(value, StandardCharsets.UTF_8);
  }

  /** Cloudflare's Turnstile server-side verification endpoint. */
  private static final String SITE_VERIFY_URL =
      "https://challenges.cloudflare.com/turnstile/v0/siteverify";

  /** The per-request timeout for the siteverify call. */
  private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(10);

  /** The shared HTTP client used to reach Cloudflare. */
  private final HttpClient httpClient =
      HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

  /** The autowired OptionMapper object, used to resolve the Turnstile configuration. */
  @Autowired private OptionMapper optionMapper;

  /** The log4j2 logger. */
  private static final Logger LOGGER = LogManager.getLogger(TurnstileService.class);
}
