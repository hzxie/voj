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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import org.verwandlung.voj.web.mapper.OptionMapper;
import org.verwandlung.voj.web.model.Option;
import org.verwandlung.voj.web.util.InputValidator;

/**
 * Reads and writes the system options (voj_options). Every setting is read through this service,
 * with the meaning of a missing or malformed value stated by the method each caller picks.
 *
 * <p>Reads outside a transaction (the interceptors, page rendering, controllers) are served from a
 * cache of the committed options, reloaded when it is older than {@link #CACHE_TTL_MILLIS} (which
 * bounds how long another web instance's change takes to show up) and dropped when an update
 * commits. Reads inside a transaction go to the database, so they see that transaction's own
 * changes and never put uncommitted values into the cache.
 *
 * @author Haozhe Xie
 */
@Service
public class OptionService {
  /**
   * Gets all system options.
   *
   * @return a list containing all system options
   */
  public List<Option> getOptions() {
    return new ArrayList<>(getCurrentOptions().values());
  }

  /**
   * Gets the system options that are loaded automatically (exposed to every page).
   *
   * @return a list containing the auto-loaded system options
   */
  public List<Option> getAutoloadOptions() {
    List<Option> options = new ArrayList<>();
    for (Option option : getCurrentOptions().values()) {
      if (option.isAutoload()) {
        options.add(option);
      }
    }
    return options;
  }

  /**
   * Gets the value of an option.
   *
   * @param optionName - the name of the option
   * @param defaultValue - the value returned when the option does not exist
   * @return the value of the option, or the default value
   */
  public String getStringOption(String optionName, String defaultValue) {
    Option option = getCurrentOptions().get(optionName);
    return option == null || option.getOptionValue() == null
        ? defaultValue
        : option.getOptionValue();
  }

  /**
   * Gets a switch: {@code "1"} means on, any other value off.
   *
   * @param optionName - the name of the option
   * @param defaultValue - the value returned when the option does not exist
   * @return whether the switch is on
   */
  public boolean isOptionEnabled(String optionName, boolean defaultValue) {
    String value = getStringOption(optionName, null);
    return value == null ? defaultValue : "1".equals(value.trim());
  }

  /**
   * Gets a positive integer, such as a page size or a limit.
   *
   * @param optionName - the name of the option
   * @param defaultValue - the value returned when the option is missing, not a number or not
   *     positive
   * @return the value of the option, or the default value
   */
  public int getIntOption(String optionName, int defaultValue) {
    Integer value = parseInt(optionName);
    return value != null && value > 0 ? value : defaultValue;
  }

  /**
   * Gets a non-negative integer, for settings where 0 turns the feature off (thresholds, delays).
   *
   * @param optionName - the name of the option
   * @param defaultValue - the value returned when the option is missing, not a number or negative
   * @return the value of the option, or the default value
   */
  public int getNonNegativeIntOption(String optionName, int defaultValue) {
    Integer value = parseInt(optionName);
    return value != null && value >= 0 ? value : defaultValue;
  }

  /**
   * Parses the value of an option as an integer.
   *
   * @param optionName - the name of the option
   * @return the integer, or null when the option is missing or not a number
   */
  private Integer parseInt(String optionName) {
    String value = getStringOption(optionName, null);
    if (value == null) {
      return null;
    }
    try {
      return Integer.parseInt(value.trim());
    } catch (NumberFormatException e) {
      return null;
    }
  }

  /**
   * Validates and saves the settings of the general settings page, all in one transaction: nothing
   * is saved unless every value is legal.
   *
   * @param settings - the option names and values to save (only existing options are written)
   * @return a Map object containing the validation flags and {@code isSuccessful}
   */
  @Transactional
  public Map<String, Boolean> updateGeneralSettings(Map<String, String> settings) {
    String websiteName = settings.getOrDefault("websiteName", "");
    String description = settings.getOrDefault("description", "");
    String copyright = settings.getOrDefault("copyright", "");
    String icpNumber = settings.getOrDefault("icpNumber", "");
    String policeIcpNumber = settings.getOrDefault("policeIcpNumber", "");
    String googleAnalyticsCode = settings.getOrDefault("googleAnalyticsCode", "");
    String contactEmail = settings.getOrDefault("contactEmail", "");

    Map<String, Boolean> result = new HashMap<>();
    result.put("isWebsiteNameEmpty", websiteName.isEmpty());
    result.put("isWebisteNameLegal", websiteName.length() <= 32);
    result.put("isDescriptionEmpty", description.isEmpty());
    result.put("isDescriptionLegal", description.length() <= 128);
    result.put("isCopyrightEmpty", copyright.isEmpty());
    result.put("isCopyrightLegal", copyright.length() <= 128);
    result.put("isIcpNumberLegal", isIcpNumberLegal(icpNumber));
    result.put("isPoliceIcpNumberLegal", isPoliceIcpNumberLegal(policeIcpNumber));
    result.put("isAnalyticsCodeLegal", isGoogleAnalyticsCodeLegal(googleAnalyticsCode));
    result.put("isContactEmailLegal", contactEmail.isEmpty() || InputValidator.isEmail(contactEmail));

    boolean isSuccessful =
        !result.get("isWebsiteNameEmpty")
            && result.get("isWebisteNameLegal")
            && !result.get("isDescriptionEmpty")
            && result.get("isDescriptionLegal")
            && !result.get("isCopyrightEmpty")
            && result.get("isCopyrightLegal")
            && result.get("isIcpNumberLegal")
            && result.get("isPoliceIcpNumberLegal")
            && result.get("isAnalyticsCodeLegal")
            && result.get("isContactEmailLegal");
    if (isSuccessful) {
      updateOptions(settings);
    }
    result.put("isSuccessful", isSuccessful);
    return result;
  }

  /**
   * Updates the values of existing options; names of options that do not exist are ignored. The
   * cache is dropped once the change commits.
   *
   * @param optionMap - the option names and their new values
   */
  @Transactional
  public void updateOptions(Map<String, String> optionMap) {
    Map<String, Option> options = getCurrentOptions();
    for (Map.Entry<String, String> e : optionMap.entrySet()) {
      Option current = options.get(e.getKey());
      if (current != null) {
        optionMapper.updateOption(
            new Option(
                current.getOptionId(), current.getOptionName(), e.getValue(), current.isAutoload()));
      }
    }
    if (TransactionSynchronizationManager.isSynchronizationActive()) {
      TransactionSynchronizationManager.registerSynchronization(
          new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
              invalidateCache();
            }
          });
    } else {
      invalidateCache();
    }
  }

  /**
   * Gets the options as the caller sees them: from the database inside a transaction, otherwise
   * from the cache of the committed options.
   *
   * @return the options, keyed by name
   */
  private Map<String, Option> getCurrentOptions() {
    if (TransactionSynchronizationManager.isActualTransactionActive()) {
      return toMap(optionMapper.getOptions());
    }
    CachedOptions cached = cache;
    if (cached != null && System.currentTimeMillis() - cached.loadedAt < CACHE_TTL_MILLIS) {
      return cached.options;
    }
    long generation = cacheGeneration.get();
    Map<String, Option> options = toMap(optionMapper.getOptions());
    synchronized (this) {
      // An update that committed while loading may not be in what was read.
      if (generation == cacheGeneration.get()) {
        cache = new CachedOptions(options, System.currentTimeMillis());
      }
    }
    return options;
  }

  /**
   * Indexes options by name.
   *
   * @param options - the options
   * @return an unmodifiable map from the option names to the options
   */
  private static Map<String, Option> toMap(List<Option> options) {
    Map<String, Option> map = new LinkedHashMap<>();
    for (Option option : options) {
      map.put(option.getOptionName(), option);
    }
    return Collections.unmodifiableMap(map);
  }

  /** Drops the cache, so the next read loads the committed options. */
  private synchronized void invalidateCache() {
    cacheGeneration.incrementAndGet();
    cache = null;
  }

  /**
   * Checks whether the website ICP registration number is legal: empty, or a Chinese ICP filing
   * number (a province character, the "ICP" filing prefix and an 8-digit number).
   *
   * @param icpNumber - the website ICP registration number
   * @return whether the website ICP registration number is legal
   */
  private boolean isIcpNumberLegal(String icpNumber) {
    return icpNumber.isEmpty() || icpNumber.matches("^.ICP备[0-9]{8}号$");
  }

  /**
   * Checks whether the police ICP registration number is legal: empty, or a Chinese public-security
   * filing number (a province character, the filing prefix and a 14-digit number).
   *
   * @param policeIcpNumber - the police ICP registration number
   * @return whether the police ICP registration number is legal
   */
  private boolean isPoliceIcpNumberLegal(String policeIcpNumber) {
    return policeIcpNumber.isEmpty() || policeIcpNumber.matches("^.公网安备[\\s]*[0-9]{14}号$");
  }

  /**
   * Checks whether the Google Analytics code is legal: empty, or wrapped in a &lt;script&gt; tag.
   *
   * @param googleAnalyticsCode - the Google Analytics code
   * @return whether the Google Analytics code is legal
   */
  private boolean isGoogleAnalyticsCodeLegal(String googleAnalyticsCode) {
    String code = googleAnalyticsCode.trim();
    return code.isEmpty() || SCRIPT_TAG.matcher(code).matches();
  }

  /** The committed options and when they were loaded. */
  private static final class CachedOptions {
    CachedOptions(Map<String, Option> options, long loadedAt) {
      this.options = options;
      this.loadedAt = loadedAt;
    }

    final Map<String, Option> options;

    final long loadedAt;
  }

  /** How long the cached options are used before they are loaded again. */
  private static final long CACHE_TTL_MILLIS = TimeUnit.MINUTES.toMillis(1);

  /** The syntax of the Google Analytics code. */
  private static final Pattern SCRIPT_TAG = Pattern.compile("<script[^>]*>.*</script>", Pattern.DOTALL);

  /** The cached committed options, or null when they must be loaded. */
  private volatile CachedOptions cache;

  /** Incremented whenever the cache is dropped, so a load that raced an update is not cached. */
  private final AtomicLong cacheGeneration = new AtomicLong();

  /** The autowired OptionMapper object. */
  @Autowired private OptionMapper optionMapper;
}
