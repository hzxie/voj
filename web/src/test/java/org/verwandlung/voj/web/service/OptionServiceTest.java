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
    Assertions.assertEquals(12, options.size());
  }

  /** Test case: tests the getAutoloadOptions() method. Test data: N/a. Expected: only the auto-loaded system options. */
  @Test
  public void testGetAutoloadOptions() {
    List<Option> options = optionService.getAutoloadOptions();
    Assertions.assertEquals(6, options.size());
  }

  /** Test case: tests the getOption(String) method. Test data: an existing option name. Expected: the corresponding Option object. */
  @Test
  public void testGetOptionExists() {
    Option option = optionService.getOption("websiteName");
    Assertions.assertNotNull(option);
    Assertions.assertEquals(1, option.getOptionId());
  }

  /** Test case: tests the getOption(String) method. Test data: a non-existing option name. Expected: a null reference. */
  @Test
  public void testGetOptionNotExists() {
    Assertions.assertNull(optionService.getOption("notExistOption"));
  }

  /** Test case: tests the updateOptions(Map) method. Test data: valid key-value pairs. Expected: the system options are updated. */
  @Test
  public void testUpdateOptionsUsingMap() {
    Map<String, String> optionMap = new HashMap<>();
    optionMap.put("websiteName", "New OJ Platform");
    optionService.updateOptions(optionMap);

    Assertions.assertEquals("New OJ Platform", optionService.getOption("websiteName").getOptionValue());
  }

  /** Test case: tests the updateOptions(...) method. Test data: all valid system options. Expected: the update succeeds and the website name is persisted. */
  @Test
  public void testUpdateOptionsWithLegalArguments() {
    Map<String, Boolean> result =
        optionService.updateOptions(
            "Verwandlung OJ",
            "An online judge platform",
            "Copyright (C) Haozhe Xie",
            true,
            "浙ICP备15017174号",
            "浙公网安备33010202000766号",
            "<script>console.log('ga');</script>",
            "shit,fuck");
    Assertions.assertTrue(result.get("isSuccessful"));
    Assertions.assertEquals("Verwandlung OJ", optionService.getOption("websiteName").getOptionValue());
  }

  /** Test case: tests the updateOptions(...) method. Test data: an empty website name. Expected: the update fails (isWebsiteNameEmpty). */
  @Test
  public void testUpdateOptionsWithEmptyWebsiteName() {
    Map<String, Boolean> result =
        optionService.updateOptions(
            "", "Description", "Copyright", true, "", "", "", "");
    Assertions.assertTrue(result.get("isWebsiteNameEmpty"));
    Assertions.assertFalse(result.get("isSuccessful"));
  }

  /** Test case: tests the updateOptions(...) method. Test data: a website name that is too long (over 32 characters). Expected: the update fails (isWebisteNameLegal is false). */
  @Test
  public void testUpdateOptionsWithTooLongWebsiteName() {
    String tooLongName = "x".repeat(33);
    Map<String, Boolean> result =
        optionService.updateOptions(
            tooLongName, "Description", "Copyright", true, "", "", "", "");
    Assertions.assertFalse(result.get("isWebisteNameLegal"));
    Assertions.assertFalse(result.get("isSuccessful"));
  }

  /** Test case: tests the updateOptions(...) method. Test data: a website description that is too long (over 128 characters). Expected: the update fails (isDescriptionLegal is false). */
  @Test
  public void testUpdateOptionsWithTooLongDescription() {
    String tooLongDescription = "x".repeat(129);
    Map<String, Boolean> result =
        optionService.updateOptions(
            "OJ", tooLongDescription, "Copyright", true, "", "", "", "");
    Assertions.assertFalse(result.get("isDescriptionLegal"));
    Assertions.assertFalse(result.get("isSuccessful"));
  }

  /** Test case: tests the updateOptions(...) method. Test data: an illegal ICP filing number. Expected: the update fails (isIcpNumberLegal is false). */
  @Test
  public void testUpdateOptionsWithIllegalIcpNumber() {
    Map<String, Boolean> result =
        optionService.updateOptions(
            "OJ", "Description", "Copyright", true, "Illegal ICP", "", "", "");
    Assertions.assertFalse(result.get("isIcpNumberLegal"));
    Assertions.assertFalse(result.get("isSuccessful"));
  }

  /** Test case: tests the updateOptions(...) method. Test data: an illegal Google Analytics code. Expected: the update fails (isAnalyticsCodeLegal is false). */
  @Test
  public void testUpdateOptionsWithIllegalAnalyticsCode() {
    Map<String, Boolean> result =
        optionService.updateOptions(
            "OJ", "Description", "Copyright", true, "", "", "not-a-script-tag", "");
    Assertions.assertFalse(result.get("isAnalyticsCodeLegal"));
    Assertions.assertFalse(result.get("isSuccessful"));
  }

  /** The OptionService object under test. */
  @Autowired private OptionService optionService;
}
