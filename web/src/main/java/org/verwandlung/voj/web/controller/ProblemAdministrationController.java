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

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;

import org.verwandlung.voj.web.exception.ResourceNotFoundException;
import org.verwandlung.voj.web.interceptor.AdministrationSection;
import org.verwandlung.voj.web.model.Problem;
import org.verwandlung.voj.web.model.ProblemCategory;
import org.verwandlung.voj.web.model.ProblemDifficulty;
import org.verwandlung.voj.web.model.ProblemTag;
import org.verwandlung.voj.web.service.OptionService;
import org.verwandlung.voj.web.service.ProblemService;
import org.verwandlung.voj.web.util.JsonUtils;
import org.verwandlung.voj.web.util.LogSanitizer;
import org.verwandlung.voj.web.util.Pagination;

/**
 * The problem administration: problems, their test data, categories and tags.
 *
 * @author Haozhe Xie
 */
@Controller
@RequestMapping(value = "/administration")
@AdministrationSection("problems")
public class ProblemAdministrationController {
  /**
   * Loads the problem list page.
   *
   * @param request - the HttpServletRequest object
   * @param response - the HttpServletResponse object
   * @return a ModelAndView object containing the information of the problem list page
   */
  @RequestMapping(value = "/all-problems", method = RequestMethod.GET)
  public ModelAndView allProblemsView(
      @RequestParam(value = "keyword", required = false, defaultValue = "") String keyword,
      @RequestParam(value = "problemCategory", required = false, defaultValue = "")
          String problemCategorySlug,
      @RequestParam(value = "problemTag", required = false, defaultValue = "")
          String problemTagSlug,
      @RequestParam(value = "problemDifficulty", required = false, defaultValue = "")
          String problemDifficultySlug,
      @RequestParam(value = "page", required = false, defaultValue = "1") long pageNumber,
      HttpServletRequest request,
      HttpServletResponse response) {
    final int NUMBER_OF_PROBLEMS_PER_PAGE = optionService.getIntOption("problemsPerPage", 100);
    List<ProblemCategory> problemCategories = problemService.getProblemCategories();
    List<ProblemDifficulty> problemDifficulties = problemService.getProblemDifficulties();
    long totalProblems =
        problemService.getNumberOfProblemsUsingFilters(
            keyword, problemCategorySlug, problemTagSlug, problemDifficultySlug, false);

    long offset = Pagination.getOffset(pageNumber, NUMBER_OF_PROBLEMS_PER_PAGE);

    List<Problem> problems =
        problemService.getProblemsUsingFilters(
            offset,
            keyword,
            problemCategorySlug,
            problemTagSlug,
            problemDifficultySlug,
            false,
            NUMBER_OF_PROBLEMS_PER_PAGE);

    // The page's problems are no longer a contiguous id range, so bound the
    // relationship lookups by the min/max id actually shown.
    Map<Long, List<ProblemCategory>> problemCategoryRelationships = new HashMap<>();
    Map<Long, List<ProblemTag>> problemTagRelationships = new HashMap<>();
    if (!problems.isEmpty()) {
      long minProblemId = problems.get(0).getProblemId();
      long maxProblemId = problems.get(0).getProblemId();
      for (Problem problem : problems) {
        minProblemId = Math.min(minProblemId, problem.getProblemId());
        maxProblemId = Math.max(maxProblemId, problem.getProblemId());
      }
      problemCategoryRelationships =
          problemService.getProblemCategoriesOfProblems(minProblemId, maxProblemId);
      problemTagRelationships =
          problemService.getProblemTagsOfProblems(minProblemId, maxProblemId);
    }

    ModelAndView view = new ModelAndView("pages/administration/all-problems");
    view.addObject("problemCategories", problemCategories);
    view.addObject("selectedProblemCategory", problemCategorySlug);
    view.addObject("selectedProblemTag", problemTagSlug);
    view.addObject("problemDifficulties", problemDifficulties);
    view.addObject("selectedProblemDifficulty", problemDifficultySlug);
    view.addObject("keyword", keyword);
    view.addObject("currentPage", pageNumber);
    view.addObject(
        "totalPages", (long) Math.ceil(totalProblems * 1.0 / NUMBER_OF_PROBLEMS_PER_PAGE));
    view.addObject("problems", problems);
    view.addObject("problemCategoryRelationships", problemCategoryRelationships);
    view.addObject("problemTagRelationships", problemTagRelationships);
    return view;
  }

  /**
   * Deletes the selected problems.
   *
   * @param problems - the set of problem IDs, separated by commas (, )
   * @param request - the HttpServletRequest object
   * @return the deletion result of the problems
   */
  @RequestMapping(value = "/deleteProblems.action", method = RequestMethod.POST)
  public @ResponseBody Map<String, Boolean> deleteProblemsAction(
      @RequestParam(value = "problems") String problems, HttpServletRequest request) {
    List<Long> problemList = JsonUtils.toList(problems, Long.class);
    boolean isSuccessful =
        BulkActions.run(
            "delete problems " + problemList, () -> problemService.deleteProblems(problemList));
    if (isSuccessful) {
      LOGGER.info(
          String.format(
              "Problems: %s were deleted by administrator at %s.",
              problemList, LogSanitizer.forLog(request.getRemoteAddr())));
    }
    Map<String, Boolean> result = new HashMap<>(2, 1);
    result.put("isSuccessful", isSuccessful);
    return result;
  }

  /**
   * Loads the create problem page.
   *
   * @param request - the HttpServletRequest object
   * @param response - the HttpServletResponse object
   * @return a ModelAndView object containing the information of the create problem page
   */
  @RequestMapping(value = "/new-problem", method = RequestMethod.GET)
  public ModelAndView newProblemView(HttpServletRequest request, HttpServletResponse response) {
    Map<ProblemCategory, List<ProblemCategory>> problemCategories =
        problemService.getProblemCategoriesWithHierarchy();

    ModelAndView view = new ModelAndView("pages/administration/new-problem");
    view.addObject("problemCategories", problemCategories);
    view.addObject("problemDifficulties", problemService.getProblemDifficulties());
    return view;
  }

  /**
   * Handles the user's request to create a problem.
   *
   * @param problemName - the name of the problem
   * @param timeLimit - the time limit
   * @param memoryLimit - the memory usage limit
   * @param description - the description of the problem
   * @param hint - the hint of the problem
   * @param inputFormat - the input format
   * @param outputFormat - the output format
   * @param inputSample - the input sample
   * @param outputSample - the output sample
   * @param testCases - the test cases (in JSON format)
   * @param problemCategories - the problem categories (in JSON format)
   * @param problemTags - the problem tags (in JSON format)
   * @param status - the publication status of the problem (PUBLISHED / DRAFT / HIDDEN)
   * @param isExactlyMatch - whether the checkpoints are matched exactly
   * @param request - the HttpServletRequest object
   * @return a Map<String, Boolean> object containing the problem creation result
   */
  @RequestMapping(value = "/createProblem.action", method = RequestMethod.POST)
  public @ResponseBody Map<String, Object> createProblemAction(
      @RequestParam(value = "problemName") String problemName,
      @RequestParam(value = "timeLimit") String timeLimit,
      @RequestParam(value = "memoryLimit") String memoryLimit,
      @RequestParam(value = "description") String description,
      @RequestParam(value = "hint") String hint,
      @RequestParam(value = "inputFormat") String inputFormat,
      @RequestParam(value = "outputFormat") String outputFormat,
      @RequestParam(value = "inputSample") String inputSample,
      @RequestParam(value = "outputSample") String outputSample,
      @RequestParam(value = "testCases") String testCases,
      @RequestParam(value = "problemCategories") String problemCategories,
      @RequestParam(value = "problemTags") String problemTags,
      @RequestParam(value = "status", required = false, defaultValue = "PUBLISHED") String status,
      @RequestParam(value = "isExactlyMatch") boolean isExactlyMatch,
      @RequestParam(value = "problemDifficulty", required = false, defaultValue = "") String problemDifficulty,
      HttpServletRequest request) {
    if (timeLimit.isEmpty() || !StringUtils.isNumeric(timeLimit)) {
      timeLimit = "-1";
    }
    if (memoryLimit.isEmpty() || !StringUtils.isNumeric(memoryLimit)) {
      memoryLimit = "-1";
    }
    Map<String, Object> result =
        problemService.createProblem(
            problemName,
            Integer.parseInt(timeLimit),
            Integer.parseInt(memoryLimit),
            description,
            hint,
            inputFormat,
            outputFormat,
            inputSample,
            outputSample,
            testCases,
            problemCategories,
            problemTags,
            status,
            isExactlyMatch,
            problemDifficulty);

    if ((boolean) result.get("isSuccessful")) {
      long problemId = (Long) result.get("problemId");
      String ipAddress = request.getRemoteAddr();

      LOGGER.info(
          String.format(
              "Problem: [ProblemId=%s] was created by administrator at %s.",
              new Object[] {problemId, LogSanitizer.forLog(ipAddress)}));
    }
    return result;
  }

  /**
   * Loads the edit problem page.
   *
   * @param problemId - the unique identifier of the problem
   * @param request - the HttpServletRequest object
   * @param response - the HttpServletResponse object
   * @return a ModelAndView object containing the information of the edit problem page
   */
  @RequestMapping(value = "/edit-problem/{problemId}", method = RequestMethod.GET)
  public ModelAndView editProblemsView(
      @PathVariable(value = "problemId") long problemId,
      HttpServletRequest request,
      HttpServletResponse response) {
    Problem problem = problemService.getProblem(problemId);

    if (problem == null) {
      throw new ResourceNotFoundException();
    }
    List<Map<String, Object>> checkpointMeta =
        problemService.getCheckpointMetaUsingProblemId(problemId);
    List<ProblemCategory> selectedProblemCategories =
        problemService.getProblemCategoriesUsingProblemId(problemId);
    Map<ProblemCategory, List<ProblemCategory>> problemCategories =
        problemService.getProblemCategoriesWithHierarchy();
    List<ProblemTag> problemTags = problemService.getProblemTagsUsingProblemId(problemId);

    ModelAndView view = new ModelAndView("pages/administration/edit-problem");
    view.addObject("problem", problem);
    view.addObject("checkpointMeta", checkpointMeta);
    view.addObject("currentExactlyMatch", problemService.isExactlyMatch(problemId));
    view.addObject("problemCategories", problemCategories);
    view.addObject("selectedProblemCategories", selectedProblemCategories);
    view.addObject("problemTags", problemTags);
    view.addObject("problemDifficulties", problemService.getProblemDifficulties());
    return view;
  }

  /**
   * Handles the user's request to edit a problem.
   *
   * @param problemName - the name of the problem
   * @param timeLimit - the time limit
   * @param memoryLimit - the memory usage limit
   * @param description - the description of the problem
   * @param hint - the hint of the problem
   * @param inputFormat - the input format
   * @param outputFormat - the output format
   * @param inputSample - the input sample
   * @param outputSample - the output sample
   * @param problemCategories - the problem categories (in JSON format)
   * @param problemTags - the problem tags (in JSON format)
   * @param status - the publication status of the problem (PUBLISHED / DRAFT / HIDDEN)
   * @param isExactlyMatch - whether the checkpoints are matched exactly
   * @param request - the HttpServletRequest object
   * @return a Map<String, Boolean> object containing the problem edit result
   */
  @RequestMapping(value = "/editProblem.action", method = RequestMethod.POST)
  public @ResponseBody Map<String, Boolean> editProblemAction(
      @RequestParam(value = "problemId") long problemId,
      @RequestParam(value = "problemName") String problemName,
      @RequestParam(value = "timeLimit") String timeLimit,
      @RequestParam(value = "memoryLimit") String memoryLimit,
      @RequestParam(value = "description") String description,
      @RequestParam(value = "hint") String hint,
      @RequestParam(value = "inputFormat") String inputFormat,
      @RequestParam(value = "outputFormat") String outputFormat,
      @RequestParam(value = "inputSample") String inputSample,
      @RequestParam(value = "outputSample") String outputSample,
      @RequestParam(value = "problemCategories") String problemCategories,
      @RequestParam(value = "problemTags") String problemTags,
      @RequestParam(value = "status", required = false, defaultValue = "PUBLISHED") String status,
      @RequestParam(value = "isExactlyMatch") boolean isExactlyMatch,
      @RequestParam(value = "problemDifficulty", required = false, defaultValue = "") String problemDifficulty,
      HttpServletRequest request) {
    if (timeLimit.isEmpty() || !StringUtils.isNumeric(timeLimit)) {
      timeLimit = "-1";
    }
    if (memoryLimit.isEmpty() || !StringUtils.isNumeric(memoryLimit)) {
      memoryLimit = "-1";
    }
    Map<String, Boolean> result =
        problemService.editProblem(
            problemId,
            problemName,
            Integer.parseInt(timeLimit),
            Integer.parseInt(memoryLimit),
            description,
            hint,
            inputFormat,
            outputFormat,
            inputSample,
            outputSample,
            problemCategories,
            problemTags,
            status,
            isExactlyMatch,
            problemDifficulty);

    if (result.get("isSuccessful")) {
      String ipAddress = request.getRemoteAddr();

      LOGGER.info(
          String.format(
              "Problem: [ProblemId=%s] was edited by administrator at %s.",
              new Object[] {problemId, LogSanitizer.forLog(ipAddress)}));
    }
    return result;
  }

  /**
   * Handles uploading a test-data archive (.zip of NN.in / NN.out pairs) for a problem. The parsed
   * pairs replace all existing checkpoints.
   *
   * @param problemId - the unique identifier of the problem
   * @param isExactlyMatch - whether the new checkpoints require an exact match
   * @param testDataArchive - the uploaded .zip archive
   * @param request - the HttpServletRequest object
   * @return a Map containing the upload result and the refreshed checkpoint metadata
   */
  @RequestMapping(value = "/uploadProblemTestData.action", method = RequestMethod.POST)
  public @ResponseBody Map<String, Object> uploadProblemTestDataAction(
      @RequestParam(value = "problemId") long problemId,
      @RequestParam(value = "isExactlyMatch", required = false, defaultValue = "false") boolean isExactlyMatch,
      @RequestParam(value = "testDataArchive", required = false) MultipartFile testDataArchive,
      HttpServletRequest request) {
    Map<String, Object> result = new HashMap<>();
    boolean problemExists = problemService.getProblem(problemId) != null;
    boolean archiveEmpty = testDataArchive == null || testDataArchive.isEmpty();
    result.put("isProblemExists", problemExists);
    result.put("isArchiveEmpty", archiveEmpty);
    result.put("isArchiveLegal", true);
    result.put("isSuccessful", false);

    if (!problemExists || archiveEmpty) {
      return result;
    }
    List<Map<String, String>> testCases;
    try {
      testCases = problemService.parseTestDataArchive(testDataArchive.getInputStream());
    } catch (IOException e) {
      LOGGER.warn(String.format("Failed to parse test-data archive for [ProblemId=%s].", problemId), e);
      result.put("isArchiveLegal", false);
      return result;
    }
    if (testCases.isEmpty()) {
      result.put("isArchiveLegal", false);
      return result;
    }
    problemService.replaceTestCases(problemId, testCases, isExactlyMatch);
    result.put("isSuccessful", true);
    result.put("checkpoints", problemService.getCheckpointMetaUsingProblemId(problemId));

    LOGGER.info(
        String.format(
            "Problem: [ProblemId=%s] test data (%s checkpoints) was uploaded by administrator at %s.",
            new Object[] {
              problemId,
              testCases.size(),
              LogSanitizer.forLog(request.getRemoteAddr())
            }));
    return result;
  }

  /**
   * Handles deleting a single checkpoint of a problem. The remaining checkpoints are renumbered and
   * re-scored by the service layer.
   *
   * @param problemId - the unique identifier of the problem
   * @param checkpointId - the id of the checkpoint to delete
   * @return a Map containing the result and the refreshed checkpoint metadata
   */
  @RequestMapping(value = "/deleteProblemCheckpoint.action", method = RequestMethod.POST)
  public @ResponseBody Map<String, Object> deleteProblemCheckpointAction(
      @RequestParam(value = "problemId") long problemId,
      @RequestParam(value = "checkpointId") int checkpointId) {
    Map<String, Object> result = new HashMap<>();
    boolean problemExists = problemService.getProblem(problemId) != null;
    if (problemExists) {
      problemService.deleteCheckpoint(problemId, checkpointId);
    }
    result.put("isProblemExists", problemExists);
    result.put("isSuccessful", problemExists);
    result.put("checkpoints", problemService.getCheckpointMetaUsingProblemId(problemId));
    return result;
  }

  /**
   * Loads the problem categories page.
   *
   * @param request - the HttpServletRequest object
   * @param response - the HttpServletResponse object
   * @return a ModelAndView object containing the information of the problem categories page
   */
  @RequestMapping(value = "/problem-categories", method = RequestMethod.GET)
  public ModelAndView problemCategoriesView(
      HttpServletRequest request, HttpServletResponse response) {
    List<ProblemCategory> problemCategories = problemService.getProblemCategories();

    Map<Integer, Long> problemCategoryProblems = new HashMap<>();
    for (ProblemCategory problemCategory : problemCategories) {
      long numberOfProblems =
          problemService.getNumberOfProblemsUsingFilters(
              "", problemCategory.getProblemCategorySlug(), "", false);
      problemCategoryProblems.put(problemCategory.getProblemCategoryId(), numberOfProblems);
    }

    List<ProblemTag> problemTags = problemService.getProblemTags();

    ModelAndView view = new ModelAndView("pages/administration/problem-categories");
    view.addObject("problemCategories", problemCategories);
    view.addObject("problemCategoryProblems", problemCategoryProblems);
    view.addObject("problemTags", problemTags);
    return view;
  }

  /**
   * Creates a problem category.
   *
   * @param problemCategorySlug - the slug of the problem category
   * @param problemCategoryName - the name of the problem category
   * @param parentProblemCategorySlug - the slug of the parent problem category
   * @param request - the HttpServletRequest object
   * @return a Map<String, Object> object containing the problem category creation result
   */
  @RequestMapping(value = "/createProblemCategory.action", method = RequestMethod.POST)
  public @ResponseBody Map<String, Object> createProblemCategoryAction(
      @RequestParam(value = "problemCategorySlug") String problemCategorySlug,
      @RequestParam(value = "problemCategoryName") String problemCategoryName,
      @RequestParam(value = "parentProblemCategory") String parentProblemCategorySlug,
      HttpServletRequest request) {
    Map<String, Object> result =
        problemService.createProblemCategory(
            problemCategorySlug, problemCategoryName, parentProblemCategorySlug);

    if ((boolean) result.get("isSuccessful")) {
      long problemCategoryId = (Long) result.get("problemCategoryId");
      String ipAddress = request.getRemoteAddr();

      LOGGER.info(
          String.format(
              "ProblemCategory: [ProblemCategoryId=%s] was created by administrator at %s.",
              new Object[] {problemCategoryId, LogSanitizer.forLog(ipAddress)}));
    }
    return result;
  }

  /**
   * Edits a problem category.
   *
   * @param problemCategoryId - the unique identifier of the problem category
   * @param problemCategorySlug - the slug of the problem category
   * @param problemCategoryName - the name of the problem category
   * @param parentProblemCategorySlug - the slug of the parent problem category
   * @param request - the HttpServletRequest object
   * @return a Map<String, Boolean> object containing the problem category edit result
   */
  @RequestMapping(value = "/editProblemCategory.action", method = RequestMethod.POST)
  public @ResponseBody Map<String, Boolean> editProblemCategoryAction(
      @RequestParam(value = "problemCategoryId") String problemCategoryId,
      @RequestParam(value = "problemCategorySlug") String problemCategorySlug,
      @RequestParam(value = "problemCategoryName") String problemCategoryName,
      @RequestParam(value = "parentProblemCategory") String parentProblemCategorySlug,
      HttpServletRequest request) {
    Map<String, Boolean> result =
        problemService.editProblemCategory(
            Integer.parseInt(problemCategoryId),
            problemCategorySlug,
            problemCategoryName,
            parentProblemCategorySlug);

    if (result.get("isSuccessful")) {
      String ipAddress = request.getRemoteAddr();

      LOGGER.info(
          String.format(
              "ProblemCategory: [ProblemCategoryId=%s] was edited by administrator at %s.",
              new Object[] {
                LogSanitizer.forLog(problemCategoryId), LogSanitizer.forLog(ipAddress)
              }));
    }
    return result;
  }

  /**
   * Deletes problem categories.
   *
   * @param problemCategories - the set of unique identifiers of the problem categories
   * @param request - the HttpServletRequest object
   * @return a Map<String, Boolean> object containing the problem category deletion result
   */
  @RequestMapping(value = "/deleteProblemCategories.action", method = RequestMethod.POST)
  public @ResponseBody Map<String, Object> deleteProblemCategoryAction(
      @RequestParam(value = "problemCategories") String problemCategories,
      HttpServletRequest request) {
    Map<String, Object> result = new HashMap<>(3, 1);
    List<Integer> problemCategoryList = JsonUtils.toList(problemCategories, Integer.class);
    List<Integer> deletedProblemCategories = new ArrayList<>();

    for (int problemCategoryId : problemCategoryList) {
      if (problemService.deleteProblemCategory(problemCategoryId)) {
        deletedProblemCategories.add(problemCategoryId);
      }
      String ipAddress = request.getRemoteAddr();
      LOGGER.info(
          String.format(
              "ProblemCategory: [ProblemCategoryId=%s] was deleted by administrator at %s.",
              new Object[] {problemCategoryId, LogSanitizer.forLog(ipAddress)}));
    }
    result.put("isSuccessful", true);
    result.put("deletedProblemCategories", deletedProblemCategories);
    return result;
  }

  /**
   * Creates a problem tag.
   *
   * @param problemTagName - the name of the problem tag
   * @param request - the HttpServletRequest object
   * @return a Map<String, Object> object containing the problem tag creation result
   */
  @RequestMapping(value = "/createProblemTag.action", method = RequestMethod.POST)
  public @ResponseBody Map<String, Object> createProblemTagAction(
      @RequestParam(value = "problemTagName") String problemTagName, HttpServletRequest request) {
    Map<String, Object> result = problemService.createProblemTag(problemTagName);

    if ((boolean) result.get("isSuccessful")) {
      long problemTagId = (Long) result.get("problemTagId");
      String ipAddress = request.getRemoteAddr();

      LOGGER.info(
          String.format(
              "ProblemTag: [ProblemTagId=%s] was created by administrator at %s.",
              new Object[] {problemTagId, LogSanitizer.forLog(ipAddress)}));
    }
    return result;
  }

  /**
   * Deletes a problem tag.
   *
   * @param problemTagId - the unique identifier of the problem tag
   * @param request - the HttpServletRequest object
   * @return a Map<String, Boolean> object containing the problem tag deletion result
   */
  @RequestMapping(value = "/deleteProblemTag.action", method = RequestMethod.POST)
  public @ResponseBody Map<String, Boolean> deleteProblemTagAction(
      @RequestParam(value = "problemTagId") long problemTagId, HttpServletRequest request) {
    Map<String, Boolean> result = new HashMap<>(2, 1);
    boolean isSuccessful = problemService.deleteProblemTag(problemTagId);

    if (isSuccessful) {
      String ipAddress = request.getRemoteAddr();
      LOGGER.info(
          String.format(
              "ProblemTag: [ProblemTagId=%s] was deleted by administrator at %s.",
              new Object[] {problemTagId, LogSanitizer.forLog(ipAddress)}));
    }
    result.put("isSuccessful", isSuccessful);
    return result;
  }

  /** The autowired ProblemService object. Used for getting problem record information. */
  @Autowired private ProblemService problemService;

  /** The autowired OptionService object. Used for getting the settings options of the system. */
  @Autowired private OptionService optionService;

  /** The logger. */
  private static final Logger LOGGER = LogManager.getLogger(ProblemAdministrationController.class);
}
