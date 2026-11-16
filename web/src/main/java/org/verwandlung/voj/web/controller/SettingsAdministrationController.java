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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import org.verwandlung.voj.web.interceptor.AdministrationSection;
import org.verwandlung.voj.web.model.Option;
import org.verwandlung.voj.web.service.OffensiveWordImportService;
import org.verwandlung.voj.web.service.OffensiveWordService;
import org.verwandlung.voj.web.service.OptionService;
import org.verwandlung.voj.web.util.MailSender;

/**
 * The general settings of the website, and the offensive word import.
 *
 * @author Haozhe Xie
 */
@Controller
@RequestMapping(value = "/administration")
@AdministrationSection("settings")
public class SettingsAdministrationController {
  /**
   * Loads the general settings page.
   *
   * @param request - the HttpRequest object
   * @param response - the HttpResponse object
   * @return a ModelAndView object containing the information of the general settings page
   */
  @RequestMapping(value = "/general-settings", method = RequestMethod.GET)
  public ModelAndView generalSettingsView(
      HttpServletRequest request, HttpServletResponse response) {
    ModelAndView view = new ModelAndView("pages/administration/general-settings");
    view.addObject("options", getOptions());
    view.addObject("offensiveWordCount", offensiveWordService.getNumberOfOffensiveWords());
    return view;
  }

  /**
   * Gets all the options of the system, returned as key-value pairs.
   *
   * @return the system options as key-value pairs
   */
  private Map<String, String> getOptions() {
    Map<String, String> optionMap = new HashMap<>();
    List<Option> options = optionService.getOptions();

    for (Option option : options) {
      optionMap.put(option.getOptionName(), option.getOptionValue());
    }
    return optionMap;
  }

  /**
   * Updates the general settings of the website, all of them or (when one is illegal) none.
   *
   * @param websiteName - the name of the website
   * @param websiteDescription - the description of the website
   * @param copyright - the copyright information of the website
   * @param allowUserRegister - whether user registration is allowed
   * @param icpNumber - the ICP filing number of the website
   * @param policeIcpNumber - the public security filing number
   * @param googleAnalyticsCode - the Google Analytics code
   * @param offensiveWordSources - the offensive word import sources, one URL per line
   * @param request - the HttpServletRequest object
   * @return the update result of the general settings of the website; {@code isMailEnabled} is
   *     false when email verification is to be required on a site that sends no email
   */
  @RequestMapping(value = "/updateGeneralSettings.action", method = RequestMethod.POST)
  public @ResponseBody Map<String, Boolean> updateGeneralSettingsAction(
      @RequestParam(value = "websiteName") String websiteName,
      @RequestParam(value = "websiteDescription") String websiteDescription,
      @RequestParam(value = "copyright") String copyright,
      @RequestParam(value = "allowUserRegister") boolean allowUserRegister,
      @RequestParam(value = "icpNumber") String icpNumber,
      @RequestParam(value = "policeIcpNumber") String policeIcpNumber,
      @RequestParam(value = "googleAnalyticsCode") String googleAnalyticsCode,
      @RequestParam(value = "offensiveWordSources") String offensiveWordSources,
      @RequestParam(value = "contactEmail", defaultValue = "") String contactEmail,
      @RequestParam(value = "requireEmailVerification", defaultValue = "false")
          boolean requireEmailVerification,
      @RequestParam(value = "publicSubmissions", defaultValue = "true") boolean publicSubmissions,
      @RequestParam(value = "maintenanceMode", defaultValue = "false") boolean maintenanceMode,
      @RequestParam(value = "autoHideOnReports", defaultValue = "true") boolean autoHideOnReports,
      @RequestParam(value = "autoCensorOffensiveWords", defaultValue = "true")
          boolean autoCensorOffensiveWords,
      @RequestParam(value = "newUserPostDelay", defaultValue = "0") int newUserPostDelay,
      @RequestParam(value = "discussionReportHideThreshold", defaultValue = "5")
          int discussionReportHideThreshold,
      @RequestParam(value = "discussionMinSolvedToVote", defaultValue = "1")
          int discussionMinSolvedToVote,
      @RequestParam(value = "discussionMinSolvedToReport", defaultValue = "3")
          int discussionMinSolvedToReport,
      @RequestParam(value = "problemsPerPage", defaultValue = "100") int problemsPerPage,
      @RequestParam(value = "usersPerPage", defaultValue = "100") int usersPerPage,
      @RequestParam(value = "submissionsPerPage", defaultValue = "100") int submissionsPerPage,
      @RequestParam(value = "contestsPerPage", defaultValue = "50") int contestsPerPage,
      @RequestParam(value = "bulletinsPerPage", defaultValue = "50") int bulletinsPerPage,
      @RequestParam(value = "discussionsPerPage", defaultValue = "50") int discussionsPerPage,
      @RequestParam(value = "turnstileEnabled", defaultValue = "false") boolean turnstileEnabled,
      @RequestParam(value = "turnstileSiteKey", defaultValue = "") String turnstileSiteKey,
      @RequestParam(value = "turnstileSecretKey", defaultValue = "") String turnstileSecretKey,
      @RequestParam(value = "emailDailyLimit", defaultValue = "3") int emailDailyLimit,
      HttpServletRequest request) {
    // Nobody could verify an address on a site that sends no email.
    if (requireEmailVerification && !mailSender.isEnabled()) {
      Map<String, Boolean> result = new HashMap<>(2, 1);
      result.put("isMailEnabled", false);
      result.put("isSuccessful", false);
      return result;
    }
    Map<String, String> settings = new HashMap<>();
    settings.put("websiteName", websiteName);
    settings.put("description", websiteDescription);
    settings.put("copyright", copyright);
    settings.put("allowUserRegister", allowUserRegister ? "1" : "0");
    settings.put("icpNumber", icpNumber);
    settings.put("policeIcpNumber", policeIcpNumber);
    settings.put("googleAnalyticsCode", googleAnalyticsCode);
    settings.put("offensiveWordSources", offensiveWordSources);
    settings.put("contactEmail", contactEmail);
    settings.put("requireEmailVerification", requireEmailVerification ? "1" : "0");
    settings.put("publicSubmissions", publicSubmissions ? "1" : "0");
    settings.put("maintenanceMode", maintenanceMode ? "1" : "0");
    settings.put("autoHideOnReports", autoHideOnReports ? "1" : "0");
    settings.put("autoCensorOffensiveWords", autoCensorOffensiveWords ? "1" : "0");
    settings.put("newUserPostDelay", String.valueOf(clampNonNegative(newUserPostDelay)));
    settings.put(
        "discussionReportHideThreshold",
        String.valueOf(clampNonNegative(discussionReportHideThreshold)));
    settings.put(
        "discussionMinSolvedToVote", String.valueOf(clampNonNegative(discussionMinSolvedToVote)));
    settings.put(
        "discussionMinSolvedToReport", String.valueOf(clampNonNegative(discussionMinSolvedToReport)));
    settings.put("turnstileEnabled", turnstileEnabled ? "1" : "0");
    settings.put("turnstileSiteKey", turnstileSiteKey.trim());
    settings.put("turnstileSecretKey", turnstileSecretKey.trim());
    settings.put("emailDailyLimit", String.valueOf(Math.max(1, emailDailyLimit)));
    settings.put("problemsPerPage", String.valueOf(clampPageSize(problemsPerPage)));
    settings.put("usersPerPage", String.valueOf(clampPageSize(usersPerPage)));
    settings.put("submissionsPerPage", String.valueOf(clampPageSize(submissionsPerPage)));
    settings.put("contestsPerPage", String.valueOf(clampPageSize(contestsPerPage)));
    settings.put("bulletinsPerPage", String.valueOf(clampPageSize(bulletinsPerPage)));
    settings.put("discussionsPerPage", String.valueOf(clampPageSize(discussionsPerPage)));
    Map<String, Boolean> result = optionService.updateGeneralSettings(settings);

    return result;
  }

  /** Clamps a page-size value to a sane range of [1, 1000]. */
  private int clampPageSize(int pageSize) {
    if (pageSize < 1) {
      return 1;
    }
    return Math.min(pageSize, 1000);
  }

  /** Clamps a value to be non-negative. */
  private int clampNonNegative(int value) {
    return Math.max(value, 0);
  }

  /**
   * Saves the offensive word import sources and re-imports the offensive word dictionary from them.
   * The whole dictionary is replaced with the freshly fetched words; if every source fails the
   * existing dictionary is left untouched.
   *
   * @param offensiveWordSources - the offensive word import sources, one URL per line
   * @param request - the HttpServletRequest object
   * @return a result map describing the import outcome and a per-source breakdown
   */
  @RequestMapping(value = "/importOffensiveWords.action", method = RequestMethod.POST)
  public @ResponseBody Map<String, Object> importOffensiveWordsAction(
      @RequestParam(value = "offensiveWordSources") String offensiveWordSources,
      HttpServletRequest request) {
    offensiveWordService.updateOffensiveWordSources(offensiveWordSources);
    return offensiveWordImportService.importFromSources();
  }

  /** The autowired OptionService object. Used for getting the settings options of the system. */
  @Autowired private OptionService optionService;

  /** The autowired MailSender object. Used for telling whether the site sends email. */
  @Autowired private MailSender mailSender;

  /** The autowired OffensiveWordService object. Used for managing the offensive word dictionary. */
  @Autowired private OffensiveWordService offensiveWordService;

  /** The autowired OffensiveWordImportService object. Used for importing offensive words by URL. */
  @Autowired private OffensiveWordImportService offensiveWordImportService;
}
