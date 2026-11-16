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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import org.verwandlung.voj.web.model.Option;

/**
 * The test class for OptionService.
 *
 * @author Haozhe Xie
 */
@ExtendWith(SpringExtension.class)
@Transactional
@ContextConfiguration({"classpath:test-spring-context.xml"})
public class OptionServiceTest {
  /** Test case: tests the getOptions() method. Test data: N/a. Expected: all system options. */
  @Test
  public void testGetOptions() {
    List<Option> options = optionService.getOptions();
    Assertions.assertEquals(31, options.size());
  }

  /** Test case: tests the getAutoloadOptions() method. Test data: N/a. Expected: only the auto-loaded system options. */
  @Test
  public void testGetAutoloadOptions() {
    List<Option> options = optionService.getAutoloadOptions();
    Assertions.assertEquals(9, options.size());
  }

  /** Test case: tests getStringOption(String, String). Test data: an existing and a missing option. Expected: the value, and the default for the missing one. */
  @Test
  public void testGetStringOption() {
    Assertions.assertEquals("Verwandlung Online Judge", optionService.getStringOption("websiteName", ""));
    Assertions.assertEquals("fallback", optionService.getStringOption("notExistOption", "fallback"));
  }

  /** Test case: tests isOptionEnabled(String, boolean). Test data: a switch set to "1" and "0", and a missing option. Expected: on, off, and the default. */
  @Test
  public void testIsOptionEnabled() {
    Assertions.assertTrue(optionService.isOptionEnabled("allowUserRegister", false));
    Assertions.assertFalse(optionService.isOptionEnabled("maintenanceMode", true));
    Assertions.assertTrue(optionService.isOptionEnabled("notExistOption", true));
  }

  /** Test case: tests getIntOption and getNonNegativeIntOption. Test data: 0, a negative, a non-numeric and a missing value. Expected: getIntOption falls back for 0, getNonNegativeIntOption keeps it; both fall back for the rest. */
  @Test
  public void testIntOptions() {
    optionService.updateOptions(Map.of("newUserPostDelay", "0", "problemsPerPage", "0"));
    Assertions.assertEquals(0, optionService.getNonNegativeIntOption("newUserPostDelay", 7));
    Assertions.assertEquals(100, optionService.getIntOption("problemsPerPage", 100));

    optionService.updateOptions(Map.of("newUserPostDelay", "-1", "problemsPerPage", "abc"));
    Assertions.assertEquals(7, optionService.getNonNegativeIntOption("newUserPostDelay", 7));
    Assertions.assertEquals(100, optionService.getIntOption("problemsPerPage", 100));
    Assertions.assertEquals(9, optionService.getIntOption("notExistOption", 9));
  }

  /** Test case: tests the cache, outside a transaction. Test data: an option changed behind the service's back, then through it. Expected: the cached value is served until an update through the service commits. */
  @Test
  @Transactional(propagation = Propagation.NOT_SUPPORTED)
  public void testCachedOptionsFollowCommittedUpdates() {
    JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
    try {
      Assertions.assertEquals(
          "Verwandlung Online Judge", optionService.getStringOption("websiteName", ""));
      jdbcTemplate.update(
          "UPDATE voj_options SET option_value = 'Behind' WHERE option_name = 'websiteName'");
      Assertions.assertEquals(
          "Verwandlung Online Judge", optionService.getStringOption("websiteName", ""));

      optionService.updateOptions(Map.of("websiteName", "Through"));
      Assertions.assertEquals("Through", optionService.getStringOption("websiteName", ""));
    } finally {
      optionService.updateOptions(Map.of("websiteName", "Verwandlung Online Judge"));
    }
  }

  /** Test case: tests reads inside a transaction. Test data: an option changed within the test transaction. Expected: the change is read back at once. */
  @Test
  public void testTransactionReadsItsOwnChanges() {
    new JdbcTemplate(dataSource)
        .update("UPDATE voj_options SET option_value = 'Mine' WHERE option_name = 'websiteName'");
    Assertions.assertEquals("Mine", optionService.getStringOption("websiteName", ""));
  }

  /** Test case: tests the updateOptions(Map) method. Test data: valid key-value pairs. Expected: the system options are updated. */
  @Test
  public void testUpdateOptionsUsingMap() {
    Map<String, String> optionMap = new HashMap<>();
    optionMap.put("websiteName", "New OJ Platform");
    optionService.updateOptions(optionMap);

    Assertions.assertEquals("New OJ Platform", optionService.getStringOption("websiteName", ""));
  }

  /** Test case: tests the updateGeneralSettings(Map) method. Test data: all valid system options. Expected: the update succeeds and the website name is persisted. */
  @Test
  public void testUpdateOptionsWithLegalArguments() {
    Map<String, Boolean> result =
        optionService.updateGeneralSettings(
            settings(
                "websiteName", "Verwandlung OJ",
                "description", "An online judge platform",
                "copyright", "Copyright (C) Haozhe Xie",
                "allowUserRegister", "1",
                "icpNumber", "浙ICP备15017174号",
                "policeIcpNumber", "浙公网安备33010202000766号",
                "googleAnalyticsCode", "<script>console.log('ga');</script>",
                "offensiveWordSources", "shit,fuck"));
    Assertions.assertTrue(result.get("isSuccessful"));
    Assertions.assertEquals("Verwandlung OJ", optionService.getStringOption("websiteName", ""));
  }

  /** Test case: tests the updateGeneralSettings(Map) method. Test data: an empty website name. Expected: the update fails (isWebsiteNameEmpty). */
  @Test
  public void testUpdateOptionsWithEmptyWebsiteName() {
    Map<String, Boolean> result =
        optionService.updateGeneralSettings(
            settings("websiteName", "", "description", "Description", "copyright", "Copyright", "allowUserRegister", "1", "icpNumber", "", "policeIcpNumber", "", "googleAnalyticsCode", "", "offensiveWordSources", ""));
    Assertions.assertTrue(result.get("isWebsiteNameEmpty"));
    Assertions.assertFalse(result.get("isSuccessful"));
  }

  /** Test case: tests the updateGeneralSettings(Map) method. Test data: a website name that is too long (over 32 characters). Expected: the update fails (isWebisteNameLegal is false). */
  @Test
  public void testUpdateOptionsWithTooLongWebsiteName() {
    String tooLongName = "x".repeat(33);
    Map<String, Boolean> result =
        optionService.updateGeneralSettings(
            settings("websiteName", tooLongName, "description", "Description", "copyright", "Copyright", "allowUserRegister", "1", "icpNumber", "", "policeIcpNumber", "", "googleAnalyticsCode", "", "offensiveWordSources", ""));
    Assertions.assertFalse(result.get("isWebisteNameLegal"));
    Assertions.assertFalse(result.get("isSuccessful"));
  }

  /** Test case: tests the updateGeneralSettings(Map) method. Test data: a website description that is too long (over 128 characters). Expected: the update fails (isDescriptionLegal is false). */
  @Test
  public void testUpdateOptionsWithTooLongDescription() {
    String tooLongDescription = "x".repeat(129);
    Map<String, Boolean> result =
        optionService.updateGeneralSettings(
            settings("websiteName", "OJ", "description", tooLongDescription, "copyright", "Copyright", "allowUserRegister", "1", "icpNumber", "", "policeIcpNumber", "", "googleAnalyticsCode", "", "offensiveWordSources", ""));
    Assertions.assertFalse(result.get("isDescriptionLegal"));
    Assertions.assertFalse(result.get("isSuccessful"));
  }

  /** Test case: tests the updateGeneralSettings(Map) method. Test data: an illegal ICP filing number. Expected: the update fails (isIcpNumberLegal is false). */
  @Test
  public void testUpdateOptionsWithIllegalIcpNumber() {
    Map<String, Boolean> result =
        optionService.updateGeneralSettings(
            settings("websiteName", "OJ", "description", "Description", "copyright", "Copyright", "allowUserRegister", "1", "icpNumber", "Illegal ICP", "policeIcpNumber", "", "googleAnalyticsCode", "", "offensiveWordSources", ""));
    Assertions.assertFalse(result.get("isIcpNumberLegal"));
    Assertions.assertFalse(result.get("isSuccessful"));
  }

  /** Test case: tests the updateGeneralSettings(Map) method. Test data: an illegal Google Analytics code. Expected: the update fails (isAnalyticsCodeLegal is false). */
  @Test
  public void testUpdateOptionsWithIllegalAnalyticsCode() {
    Map<String, Boolean> result =
        optionService.updateGeneralSettings(
            settings("websiteName", "OJ", "description", "Description", "copyright", "Copyright", "allowUserRegister", "1", "icpNumber", "", "policeIcpNumber", "", "googleAnalyticsCode", "not-a-script-tag", "offensiveWordSources", ""));
    Assertions.assertFalse(result.get("isAnalyticsCodeLegal"));
    Assertions.assertFalse(result.get("isSuccessful"));
  }

  /** Test case: tests updateGeneralSettings(Map). Test data: legal site texts with an illegal contact email, and a page size. Expected: isContactEmailLegal is false and nothing is saved, not even the legal values. */
  @Test
  public void testUpdateGeneralSettingsSavesNothingWhenOneValueIsIllegal() {
    Map<String, String> settings =
        settings("websiteName", "Changed", "description", "D", "copyright", "C",
            "contactEmail", "not-an-email", "problemsPerPage", "7");
    Map<String, Boolean> result = optionService.updateGeneralSettings(settings);
    Assertions.assertFalse(result.get("isContactEmailLegal"));
    Assertions.assertFalse(result.get("isSuccessful"));
    Assertions.assertEquals("Verwandlung Online Judge", optionService.getStringOption("websiteName", ""));
    Assertions.assertEquals(100, optionService.getIntOption("problemsPerPage", 100));

    settings.put("contactEmail", "admin+oj@verwandlung.org");
    Assertions.assertTrue(optionService.updateGeneralSettings(settings).get("isSuccessful"));
    Assertions.assertEquals(7, optionService.getIntOption("problemsPerPage", 100));
  }

  /** Builds a settings map from alternating names and values. */
  private static Map<String, String> settings(String... namesAndValues) {
    Map<String, String> settings = new HashMap<>();
    for (int i = 0; i < namesAndValues.length; i += 2) {
      settings.put(namesAndValues[i], namesAndValues[i + 1]);
    }
    return settings;
  }

  /** The OptionService object under test. */
  @Autowired private OptionService optionService;

  /** The data source, used to change an option behind the service's back. */
  @Autowired private javax.sql.DataSource dataSource;
}
