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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.verwandlung.voj.web.mapper.OptionMapper;
import org.verwandlung.voj.web.model.Option;

/**
 * The system administration service, used to perform the administration functions of the whole
 * system.
 *
 * @author Haozhe Xie
 */
@Service
@Transactional
public class OptionService {
  /**
   * Gets all system options.
   *
   * @return a list containing all system options
   */
  public List<Option> getOptions() {
    return optionMapper.getOptions();
  }

  /**
   * Gets the system options that are loaded automatically.
   *
   * @return a list containing the auto-loaded system options
   */
  public List<Option> getAutoloadOptions() {
    return optionMapper.getAutoloadOptions();
  }

  /**
   * Gets the value of a system option by its name.
   *
   * @param optionName - the name of the system option
   * @return the corresponding Option object
   */
  public Option getOption(String optionName) {
    return optionMapper.getOption(optionName);
  }

  /**
   * Gets the value of a system option as a positive integer.
   *
   * @param optionName - the name of the system option
   * @param defaultValue - the value to fall back to when the option is missing, non-numeric, or
   *     not positive
   * @return the integer value of the option, or the default value
   */
  public int getIntOption(String optionName, int defaultValue) {
    Option option = optionMapper.getOption(optionName);
    if (option == null) {
      return defaultValue;
    }
    try {
      int value = Integer.parseInt(option.getOptionValue().trim());
      return value > 0 ? value : defaultValue;
    } catch (NumberFormatException e) {
      return defaultValue;
    }
  }

  /**
   * Updates the system options.
   *
   * @param websiteName - the website name
   * @param websiteDescription - the website description
   * @param copyright - the website copyright information
   * @param allowUserRegister - whether to allow user registration
   * @param icpNumber - the website ICP registration number
   * @param policeIcpNumber - the police ICP registration number
   * @param googleAnalyticsCode - the Google Analytics code
   * @param offensiveWordSources - the offensive word import sources, one URL per line
   * @return a Map object containing the option update result
   */
  public Map<String, Boolean> updateOptions(
      String websiteName,
      String websiteDescription,
      String copyright,
      boolean allowUserRegister,
      String icpNumber,
      String policeIcpNumber,
      String googleAnalyticsCode,
      String offensiveWordSources) {
    Map<String, Boolean> result = new HashMap<>();
    result.put("isWebsiteNameEmpty", websiteName.isEmpty());
    result.put("isWebisteNameLegal", isWebsiteNameLegal(websiteName));
    result.put("isDescriptionEmpty", websiteDescription.isEmpty());
    result.put("isDescriptionLegal", isWebsiteDescriptionLegal(websiteDescription));
    result.put("isCopyrightEmpty", copyright.isEmpty());
    result.put("isCopyrightLegal", isCopyrightLegal(copyright));
    result.put("isIcpNumberLegal", isIcpNumberLegal(icpNumber));
    result.put("isPoliceIcpNumberLegal", isPoliceIcpNumberLegal(policeIcpNumber));
    result.put("isAnalyticsCodeLegal", isGoogleAnalyticsCodeLegal(googleAnalyticsCode));

    boolean isSuccessful =
        !result.get("isWebsiteNameEmpty")
            && result.get("isWebisteNameLegal")
            && !result.get("isDescriptionEmpty")
            && result.get("isDescriptionLegal")
            && !result.get("isCopyrightEmpty")
            && result.get("isCopyrightLegal")
            && result.get("isIcpNumberLegal")
            && result.get("isPoliceIcpNumberLegal")
            && result.get("isAnalyticsCodeLegal");
    if (isSuccessful) {
      Map<String, String> optionMap = new HashMap<>();
      optionMap.put("websiteName", websiteName);
      optionMap.put("description", websiteDescription);
      optionMap.put("copyright", copyright);
      optionMap.put("googleAnalyticsCode", googleAnalyticsCode);
      optionMap.put("icpNumber", icpNumber);
      optionMap.put("policeIcpNumber", policeIcpNumber);
      optionMap.put("allowUserRegister", allowUserRegister ? "1" : "0");
      optionMap.put("offensiveWordSources", offensiveWordSources);
      updateOptions(optionMap);
    }
    result.put("isSuccessful", isSuccessful);
    return result;
  }

  /**
   * Updates the system options.
   *
   * @param optionMap - the key-value pairs containing the system options
   */
  public void updateOptions(Map<String, String> optionMap) {
    for (Entry<String, String> e : optionMap.entrySet()) {
      String optionName = e.getKey();
      String optionValue = e.getValue();

      Option option = optionMapper.getOption(optionName);
      if (option == null) {
        continue;
      }
      option.setOptionValue(optionValue);
      optionMapper.updateOption(option);
    }
  }

  /**
   * Checks whether the website name is legal. Rule: a legal website name should not exceed 32
   * characters.
   *
   * @param websiteName - the website name
   * @return whether the website name is legal
   */
  private boolean isWebsiteNameLegal(String websiteName) {
    return websiteName.length() <= 32;
  }

  /**
   * Checks whether the website description is legal. Rule: a legal website description should not
   * exceed 128 characters.
   *
   * @param websiteDescription - the website description
   * @return whether the website description is legal
   */
  private boolean isWebsiteDescriptionLegal(String websiteDescription) {
    return websiteDescription.length() <= 128;
  }

  /**
   * Checks whether the website copyright information is legal. Rule: legal copyright information
   * should not exceed 128 characters.
   *
   * @param copyright - the website copyright information
   * @return whether the copyright information is legal
   */
  private boolean isCopyrightLegal(String copyright) {
    return copyright.length() <= 128;
  }

  /**
   * Checks whether the website ICP registration number is legal. Rule: a legal ICP registration
   * number is a Chinese ICP filing number consisting of a province character, the "ICP" filing
   * prefix and an 8-digit number.
   *
   * @param icpNumber - the website ICP registration number
   * @return whether the website ICP registration number is legal
   */
  private boolean isIcpNumberLegal(String icpNumber) {
    boolean isIcpNumberEmpty = icpNumber.isEmpty();
    boolean isIcpNumberLegal = icpNumber.matches("^.ICP备[0-9]{8}号$");
    return isIcpNumberEmpty || isIcpNumberLegal;
  }

  /**
   * Checks whether the police ICP registration number is legal. Rule: a legal police ICP
   * registration number is a Chinese public-security filing number consisting of a province
   * character, the public-security filing prefix and a 14-digit number.
   *
   * @param policeIcpNumber - the police ICP registration number
   * @return whether the police ICP registration number is legal
   */
  private boolean isPoliceIcpNumberLegal(String policeIcpNumber) {
    boolean isIcpNumberEmpty = policeIcpNumber.isEmpty();
    boolean isIcpNumberLegal = policeIcpNumber.matches("^.公网安备[\\s]*[0-9]{14}号$");
    return isIcpNumberEmpty || isIcpNumberLegal;
  }

  /**
   * Checks whether the contact email address is legal. Rule: a legal contact email is either empty
   * or a syntactically valid email address.
   *
   * @param contactEmail - the contact email address
   * @return whether the contact email address is legal
   */
  public boolean isContactEmailLegal(String contactEmail) {
    return contactEmail.isEmpty()
        || contactEmail.matches("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
  }

  /**
   * Checks whether the Google Analytics code is legal. Rule: a legal Google Analytics code should be
   * wrapped in a &lt;script&gt; tag.
   *
   * @param googleAnalyticsCode - the Google Analytics code
   * @return whether the Google Analytics code is legal
   */
  private boolean isGoogleAnalyticsCodeLegal(String googleAnalyticsCode) {
    boolean isAnalyticsCodeEmpty = googleAnalyticsCode.isEmpty();
    Pattern p = Pattern.compile("<script.*>.*</script>", Pattern.DOTALL);
    boolean isAnalyticsCodeLegal = p.matcher(googleAnalyticsCode).matches();
    return isAnalyticsCodeEmpty || isAnalyticsCodeLegal;
  }

  /** The autowired OptionMapper object. */
  @Autowired private OptionMapper optionMapper;
}
