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

import java.util.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.verwandlung.voj.web.mapper.CheckpointMapper;
import org.verwandlung.voj.web.mapper.ProblemCategoryMapper;
import org.verwandlung.voj.web.mapper.ProblemDifficultyMapper;
import org.verwandlung.voj.web.mapper.ProblemMapper;
import org.verwandlung.voj.web.mapper.ProblemTagMapper;
import org.verwandlung.voj.web.model.*;
import com.fasterxml.jackson.core.type.TypeReference;
import org.verwandlung.voj.web.util.JsonUtils;
import org.verwandlung.voj.web.util.SlugifyUtils;

/**
 * The business logic layer of the Problem class.
 *
 * @author Haozhe Xie
 */
@Service
@Transactional
public class ProblemService {
  /**
   * Gets the starting number of the problems.
   *
   * @return the starting number of the problems
   */
  public long getFirstIndexOfProblems() {
    return problemMapper.getLowerBoundOfProblems();
  }

  /**
   * Gets the ending number of the problems.
   *
   * @param isPublicOnly - whether to filter only public problems
   * @param offset - the starting number of the problem identifier
   * @param limit - the number of problems to load each time
   * @return the ending number of the problems
   */
  public long getLastIndexOfProblems(boolean isPublicOnly, long offset, int limit) {
    return problemMapper.getUpperBoundOfProblemsWithLimit(isPublicOnly, offset, limit);
  }

  /**
   * Gets the detailed information of a problem by its unique identifier.
   *
   * @param problemId - the unique identifier of the problem
   * @return the detailed information of the problem
   */
  public Problem getProblem(long problemId) {
    return problemMapper.getProblem(problemId);
  }

  /**
   * Gets the list of problems.
   *
   * @param offset - the starting number of the problem identifier
   * @param keyword - the keyword
   * @param problemCategorySlug - the alias of the problem category
   * @param problemTagSlug - the alias of the problem tag
   * @param problemDifficultySlug - the alias of the problem difficulty (null/empty = no filter)
   * @param isPublicOnly - whether to filter only public problems
   * @param limit - the number of problems to load each time
   * @return the list of problems (a List<Problem> object)
   */
  public List<Problem> getProblemsUsingFilters(
      long offset,
      String keyword,
      String problemCategorySlug,
      String problemTagSlug,
      String problemDifficultySlug,
      boolean isPublicOnly,
      int limit) {
    ProblemCategory problemCategory =
        problemCategoryMapper.getProblemCategoryUsingCategorySlug(problemCategorySlug);
    ProblemTag problemTag =
        problemTagMapper.getProblemTagUsingTagSlug(SlugifyUtils.getSlug(problemTagSlug));
    int problemCategoryId = 0;
    long problemTagId = 0;
    int problemDifficultyId = resolveProblemDifficultyId(problemDifficultySlug);
    if (offset < 0) {
      offset = 0;
    }
    if (problemCategory != null) {
      problemCategoryId = problemCategory.getProblemCategoryId();
    }
    if (problemTag != null) {
      problemTagId = problemTag.getProblemTagId();
    }
    return problemMapper.getProblemsUsingFilters(
        keyword, problemCategoryId, problemTagId, problemDifficultyId, isPublicOnly, offset, limit);
  }

  /**
   * Gets a page of problems for the public problem set, filtered by a single field. Backs the
   * numbered-pager problem list (the ID/Title/Tag segmented filter in the redesigned page).
   *
   * @param pageNumber - the 1-based page number
   * @param pageSize - the number of problems per page
   * @param field - the filter field ("id", "title" or "tag")
   * @param query - the filter value (may be null/empty for no filtering)
   * @param problemDifficultySlug - the alias of the problem difficulty (null/empty = no filter)
   * @param isPublicOnly - whether to filter only public problems
   * @return the list of problems on the requested page
   */
  public List<Problem> getProblemsByPage(
      int pageNumber,
      int pageSize,
      String field,
      String query,
      String problemDifficultySlug,
      boolean isPublicOnly) {
    long offset = (long) (pageNumber - 1) * pageSize;
    if (offset < 0) {
      offset = 0;
    }
    return problemMapper.getProblemsByPage(
        field,
        query,
        resolveProblemTagId(field, query),
        resolveProblemDifficultyId(problemDifficultySlug),
        isPublicOnly,
        offset,
        pageSize);
  }

  /**
   * Gets the total number of problems matching the public problem-set filters.
   *
   * @param field - the filter field ("id", "title" or "tag")
   * @param query - the filter value (may be null/empty for no filtering)
   * @param problemDifficultySlug - the alias of the problem difficulty (null/empty = no filter)
   * @param isPublicOnly - whether to filter only public problems
   * @return the total number of matching problems
   */
  public long getNumberOfProblemsByPage(
      String field, String query, String problemDifficultySlug, boolean isPublicOnly) {
    return problemMapper.getNumberOfProblemsByPage(
        field,
        query,
        resolveProblemTagId(field, query),
        resolveProblemDifficultyId(problemDifficultySlug),
        isPublicOnly);
  }

  /**
   * Resolves the tag filter to a tag identifier. Returns 0 when no tag filter applies and -1 (a
   * sentinel that matches no rows) when filtering by a tag that does not exist.
   *
   * @param field - the filter field
   * @param query - the filter value
   * @return the tag identifier to filter by
   */
  private long resolveProblemTagId(String field, String query) {
    if (!"tag".equals(field) || query == null || query.isEmpty()) {
      return 0;
    }
    ProblemTag problemTag = problemTagMapper.getProblemTagUsingTagSlug(SlugifyUtils.getSlug(query));
    return problemTag != null ? problemTag.getProblemTagId() : -1;
  }

  /**
   * Resolves a difficulty filter slug to a difficulty identifier. Returns 0 when no difficulty
   * filter applies and -1 (a sentinel that matches no rows) when filtering by a difficulty that does
   * not exist.
   *
   * @param problemDifficultySlug - the slug of the problem difficulty
   * @return the difficulty identifier to filter by
   */
  private int resolveProblemDifficultyId(String problemDifficultySlug) {
    if (problemDifficultySlug == null || problemDifficultySlug.isEmpty()) {
      return 0;
    }
    ProblemDifficulty problemDifficulty =
        problemDifficultyMapper.getProblemDifficultyUsingSlug(problemDifficultySlug);
    return problemDifficulty != null ? problemDifficulty.getProblemDifficultyId() : -1;
  }

  /**
   * Gets the total number of problems.
   *
   * @param keyword - the keyword
   * @param problemCategorySlug - the alias of the problem category
   * @param problemDifficultySlug - the alias of the problem difficulty (null/empty = no filter)
   * @param isPublicOnly - whether to filter only public problems
   * @return the total number of problems
   */
  public long getNumberOfProblemsUsingFilters(
      String keyword,
      String problemCategorySlug,
      String problemDifficultySlug,
      boolean isPublicOnly) {
    ProblemCategory problemCategory =
        problemCategoryMapper.getProblemCategoryUsingCategorySlug(problemCategorySlug);
    int problemCategoryId = 0;
    if (problemCategory != null) {
      problemCategoryId = problemCategory.getProblemCategoryId();
    }
    int problemDifficultyId = resolveProblemDifficultyId(problemDifficultySlug);
    return problemMapper.getNumberOfProblemsUsingFilters(
        keyword, problemCategoryId, problemDifficultyId, isPublicOnly);
  }

  /**
   * Gets the categories of problems within a range.
   *
   * @param problemIdLowerBound - the lower bound of the problem ID range
   * @param problemIdUpperBound - the upper bound of the problem ID range
   * @return a list containing the problem category information
   */
  public Map<Long, List<ProblemCategory>> getProblemCategoriesOfProblems(
      long problemIdLowerBound, long problemIdUpperBound) {
    List<ProblemCategoryRelationship> problemCategoryRelationships =
        problemCategoryMapper.getProblemCategoriesOfProblems(
            problemIdLowerBound, problemIdUpperBound);

    Map<Long, List<ProblemCategory>> problemCategoriesOfProblems = new HashMap<>();
    for (ProblemCategoryRelationship pcr : problemCategoryRelationships) {
      long problemId = pcr.getProblemId();
      if (!problemCategoriesOfProblems.containsKey(problemId)) {
        problemCategoriesOfProblems.put(problemId, new ArrayList<>());
      }

      List<ProblemCategory> problemCategories = problemCategoriesOfProblems.get(problemId);
      problemCategories.add(
          new ProblemCategory(
              pcr.getProblemCategoryId(),
              pcr.getProblemCategorySlug(),
              pcr.getProblemCategoryName(),
              0));
    }
    return problemCategoriesOfProblems;
  }

  /**
   * Gets the category list of a problem.
   *
   * @param problemId - the unique identifier of the problem
   * @return a list containing the problem categories
   */
  public List<ProblemCategory> getProblemCategoriesUsingProblemId(long problemId) {
    return problemCategoryMapper.getProblemCategoriesUsingProblemId(problemId);
  }

  /**
   * Gets the tags of problems within a range.
   *
   * @param problemIdLowerBound - the lower bound of the problem ID range
   * @param problemIdUpperBound - the upper bound of the problem ID range
   * @return a list containing the problem tag information
   */
  public Map<Long, List<ProblemTag>> getProblemTagsOfProblems(
      long problemIdLowerBound, long problemIdUpperBound) {
    List<ProblemTagRelationship> problemTagRelationships =
        problemTagMapper.getProblemTagsOfProblems(problemIdLowerBound, problemIdUpperBound);

    Map<Long, List<ProblemTag>> problemTagsOfProblems = new HashMap<>();
    for (ProblemTagRelationship ptr : problemTagRelationships) {
      long problemId = ptr.getProblemId();
      if (!problemTagsOfProblems.containsKey(problemId)) {
        problemTagsOfProblems.put(problemId, new ArrayList<>());
      }

      List<ProblemTag> problemTags = problemTagsOfProblems.get(problemId);
      problemTags.add(
          new ProblemTag(ptr.getProblemTagId(), ptr.getProblemTagSlug(), ptr.getProblemTagName()));
    }
    return problemTagsOfProblems;
  }

  /**
   * Gets the tag list of a problem.
   *
   * @param problemId - the unique identifier of the problem
   * @return a list containing the problem tags
   */
  public List<ProblemTag> getProblemTagsUsingProblemId(long problemId) {
    return problemTagMapper.getProblemTagsUsingProblemId(problemId);
  }

  /**
   * Gets the hierarchical list of problem categories.
   *
   * @return a Map<ProblemCategory, List<ProblemCategory>> object containing problem categories and
   *     their inheritance relationships
   */
  public Map<ProblemCategory, List<ProblemCategory>> getProblemCategoriesWithHierarchy() {
    List<ProblemCategory> problemCategories = getProblemCategories();
    Map<Integer, List<ProblemCategory>> problemCategoriesIndexer = new HashMap<>();
    Map<ProblemCategory, List<ProblemCategory>> problemCategoriesHierarchy = new LinkedHashMap<>();

    // Add the top-level problem categories (without a parent) to the list
    for (ProblemCategory pc : problemCategories) {
      if (pc.getParentProblemCategoryId() == 0) {
        List<ProblemCategory> subProblemCategories = new ArrayList<>();
        problemCategoriesHierarchy.put(pc, subProblemCategories);
        problemCategoriesIndexer.put(pc.getProblemCategoryId(), subProblemCategories);
      }
    }
    // Add the remaining problem categories to the list
    for (ProblemCategory pc : problemCategories) {
      int parentProblemCategoryId = pc.getParentProblemCategoryId();
      if (parentProblemCategoryId != 0) {
        List<ProblemCategory> subProblemCategories =
            problemCategoriesIndexer.get(parentProblemCategoryId);
        if (subProblemCategories != null) {
          subProblemCategories.add(pc);
        }
      }
    }
    return problemCategoriesHierarchy;
  }

  /**
   * Gets all problem categories.
   *
   * @return a list containing all problem categories
   */
  public List<ProblemCategory> getProblemCategories() {
    return problemCategoryMapper.getProblemCategories();
  }

  /**
   * [For administrators only] Gets all problem tags.
   *
   * @return a list containing all problem tags
   */
  public List<ProblemTag> getProblemTags() {
    return problemTagMapper.getProblemTags();
  }

  /**
   * [For administrators only] Creates a problem tag.
   *
   * @param problemTagName - the name of the problem tag
   * @return a Map<String, Object> object containing the problem tag creation result
   */
  public Map<String, Object> createProblemTag(String problemTagName) {
    String name = problemTagName == null ? "" : problemTagName.trim();
    String slug = SlugifyUtils.getSlug(name);

    Map<String, Object> result = new HashMap<>();
    result.put("isProblemTagNameEmpty", name.isEmpty());
    result.put("isProblemTagNameLegal", name.length() <= 32 && slug.length() <= 32);
    result.put(
        "isProblemTagSlugExists",
        !slug.isEmpty() && problemTagMapper.getProblemTagUsingTagSlug(slug) != null);

    boolean isSuccessful =
        !(boolean) result.get("isProblemTagNameEmpty")
            && (boolean) result.get("isProblemTagNameLegal")
            && !slug.isEmpty()
            && !(boolean) result.get("isProblemTagSlugExists");
    result.put("isSuccessful", isSuccessful);

    if (isSuccessful) {
      ProblemTag problemTag = new ProblemTag(slug, name);
      problemTagMapper.createProblemTag(problemTag);
      result.put("problemTagId", problemTag.getProblemTagId());
      result.put("problemTagSlug", slug);
      result.put("problemTagName", name);
    }
    return result;
  }

  /**
   * [For administrators only] Deletes a problem tag by its unique identifier. Relationships with
   * problems are removed by the database via {@code ON DELETE CASCADE}.
   *
   * @param problemTagId - the unique identifier of the problem tag
   * @return whether the problem tag was deleted
   */
  public boolean deleteProblemTag(long problemTagId) {
    if (problemTagMapper.getProblemTagUsingTagId(problemTagId) == null) {
      return false;
    }
    problemTagMapper.deleteProblemTagUsingTagId(problemTagId);
    return true;
  }

  /**
   * Gets all the problem difficulty levels (for the administration problem editor).
   *
   * @return a list of all problem difficulty levels
   */
  public List<ProblemDifficulty> getProblemDifficulties() {
    return problemDifficultyMapper.getProblemDifficulties();
  }

  /**
   * Resolves a difficulty alias to a {@link ProblemDifficulty}, falling back to the first
   * (lowest) difficulty when the alias is empty or unknown.
   *
   * @param problemDifficultySlug - the alias of the problem difficulty
   * @return the matching problem difficulty, or the default one
   */
  private ProblemDifficulty getProblemDifficultyOrDefault(String problemDifficultySlug) {
    ProblemDifficulty problemDifficulty = null;
    if (problemDifficultySlug != null && !problemDifficultySlug.isEmpty()) {
      problemDifficulty =
          problemDifficultyMapper.getProblemDifficultyUsingSlug(problemDifficultySlug);
    }
    if (problemDifficulty == null) {
      List<ProblemDifficulty> problemDifficulties = problemDifficultyMapper.getProblemDifficulties();
      if (!problemDifficulties.isEmpty()) {
        problemDifficulty = problemDifficulties.get(0);
      }
    }
    return problemDifficulty;
  }

  /**
   * [For administrators only] Gets the total number of all problems.
   *
   * @return the total number of all problems
   */
  public long getNumberOfProblems() {
    return problemMapper.getNumberOfProblems();
  }

  /**
   * [For administrators only] Gets the total number of all problem checkpoints in the system
   * (including private problems).
   *
   * @return the total number of all problem checkpoints in the system
   */
  public long getNumberOfCheckpoints() {
    return checkpointMapper.getNumberOfCheckpoints();
  }

  /**
   * [For administrators only] Gets the test data set of a problem.
   *
   * @param problemId - the unique identifier of the problem
   * @return the test data list of the problem
   */
  public List<Checkpoint> getCheckpointsUsingProblemId(long problemId) {
    List<Checkpoint> checkpoints = checkpointMapper.getCheckpointsUsingProblemId(problemId);
    return checkpoints;
  }

  /**
   * [For administrators only] Gets lightweight metadata (id + byte size) of a problem's checkpoints,
   * without loading the input/output text. Used by the editor's test-data file list.
   *
   * @param problemId - the unique identifier of the problem
   * @return a list of maps, each holding {@code checkpointId} and {@code size} (bytes)
   */
  public List<Map<String, Object>> getCheckpointMetaUsingProblemId(long problemId) {
    return checkpointMapper.getCheckpointMetaUsingProblemId(problemId);
  }

  /**
   * [For administrators only] Gets the exact-match flag of a problem's checkpoints.
   *
   * @param problemId - the unique identifier of the problem
   * @return the exact-match flag; defaults to {@code true} when the problem has no checkpoints
   */
  public boolean isExactlyMatch(long problemId) {
    Boolean isExactlyMatch = checkpointMapper.getCheckpointExactlyMatch(problemId);
    return isExactlyMatch == null || isExactlyMatch;
  }

  /**
   * [For administrators only] Replaces all test cases of a problem with the given input/output
   * pairs (e.g. parsed from an uploaded archive). Checkpoints are renumbered from 0 and scores are
   * redistributed evenly.
   *
   * @param problemId - the unique identifier of the problem
   * @param testCases - the input/output pairs (each map holds {@code input} and {@code output})
   * @param isExactlyMatch - whether the checkpoints require an exact match
   */
  public void replaceTestCases(
      long problemId, List<Map<String, String>> testCases, boolean isExactlyMatch) {
    checkpointMapper.deleteCheckpoint(problemId);
    createTestCases(problemId, testCases, isExactlyMatch);
  }

  /**
   * [For administrators only] Deletes a single checkpoint of a problem, then renumbers and re-scores
   * the remaining checkpoints so ids stay contiguous and scores keep summing to 100.
   *
   * @param problemId - the unique identifier of the problem
   * @param checkpointId - the id of the checkpoint to delete
   */
  public void deleteCheckpoint(long problemId, int checkpointId) {
    List<Checkpoint> checkpoints = checkpointMapper.getCheckpointsUsingProblemId(problemId);
    boolean isExactlyMatch = checkpoints.isEmpty() || checkpoints.get(0).isExactlyMatch();
    List<Map<String, String>> remaining = new ArrayList<>();
    for (Checkpoint checkpoint : checkpoints) {
      if (checkpoint.getCheckpointId() != checkpointId) {
        Map<String, String> testCase = new HashMap<>();
        testCase.put("input", checkpoint.getInput());
        testCase.put("output", checkpoint.getOutput());
        remaining.add(testCase);
      }
    }
    checkpointMapper.deleteCheckpoint(problemId);
    createTestCases(problemId, remaining, isExactlyMatch);
  }

  /**
   * [For administrators only] Creates a problem.
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
   * @param testCases - the test cases (JSON format)
   * @param problemCategories - the problem categories (JSON format)
   * @param problemTags - the problem tags (JSON format)
   * @param status - the publication status of the problem (PUBLISHED / DRAFT / HIDDEN)
   * @param isExactlyMatch - whether the checkpoints require an exact match
   * @return a Map<String, Object> object containing the problem creation result
   */
  public Map<String, Object> createProblem(
      String problemName,
      int timeLimit,
      int memoryLimit,
      String description,
      String hint,
      String inputFormat,
      String outputFormat,
      String inputSample,
      String outputSample,
      String testCases,
      String problemCategories,
      String problemTags,
      String status,
      boolean isExactlyMatch,
      String problemDifficulty) {
    Problem problem =
        new Problem(
            PublicationStatus.normalize(status),
            problemName,
            timeLimit,
            memoryLimit,
            description,
            inputFormat,
            outputFormat,
            inputSample,
            outputSample,
            hint);
    problem.setProblemDifficulty(getProblemDifficultyOrDefault(problemDifficulty));
    @SuppressWarnings("unchecked")
    Map<String, Object> result = (Map<String, Object>) getProblemCreationResult(problem);

    if ((boolean) result.get("isSuccessful")) {
      problemMapper.createProblem(problem);

      long problemId = problem.getProblemId();
      createTestCases(problemId, testCases, isExactlyMatch);
      createProblemCategoryRelationships(problemId, problemCategories);
      createProblemTags(problemId, problemTags);

      result.put("problemId", problemId);
    }
    return result;
  }

  /**
   * Checks whether the problem information is legal.
   *
   * @param problem - the problem to create
   * @return a Map<String, Boolean> object containing the problem creation result
   */
  private Map<String, ? extends Object> getProblemCreationResult(Problem problem) {
    Map<String, Boolean> result = new HashMap<>();
    result.put("isProblemNameEmpty", problem.getProblemName().isEmpty());
    result.put("isProblemNameLegal", isProblemNameLegal(problem.getProblemName()));
    result.put("isTimeLimitLegal", problem.getTimeLimit() > 0);
    result.put("isMemoryLimitLegal", problem.getMemoryLimit() > 0);
    result.put("isDescriptionEmpty", problem.getDescription().isEmpty());
    result.put("isInputFormatEmpty", problem.getInputFormat().isEmpty());
    result.put("isOutputFormatEmpty", problem.getOutputFormat().isEmpty());
    result.put("isInputSampleEmpty", problem.getSampleInput().isEmpty());
    result.put("isOutputSampleEmpty", problem.getSampleOutput().isEmpty());

    boolean isSuccessful =
        !result.get("isProblemNameEmpty")
            && result.get("isProblemNameLegal")
            && result.get("isTimeLimitLegal")
            && result.get("isMemoryLimitLegal")
            && !result.get("isDescriptionEmpty")
            && !result.get("isInputFormatEmpty")
            && !result.get("isOutputFormatEmpty")
            && !result.get("isInputSampleEmpty")
            && !result.get("isOutputSampleEmpty");
    result.put("isSuccessful", isSuccessful);
    return result;
  }

  /**
   * [For administrators only] Edits a problem.
   *
   * @param problemId - the unique identifier of the problem
   * @param problemName - the name of the problem
   * @param timeLimit - the time limit
   * @param memoryLimit - the memory usage limit
   * @param description - the description of the problem
   * @param hint - the hint of the problem
   * @param inputFormat - the input format
   * @param outputFormat - the output format
   * @param inputSample - the input sample
   * @param outputSample - the output sample
   * @param problemCategories - the problem categories (JSON format)
   * @param problemTags - the problem tags (JSON format)
   * @param status - the publication status of the problem (PUBLISHED / DRAFT / HIDDEN)
   * @param isExactlyMatch - whether the checkpoints require an exact match
   * @return a Map<String, Object> object containing the problem editing result
   */
  public Map<String, Boolean> editProblem(
      long problemId,
      String problemName,
      int timeLimit,
      int memoryLimit,
      String description,
      String hint,
      String inputFormat,
      String outputFormat,
      String inputSample,
      String outputSample,
      String problemCategories,
      String problemTags,
      String status,
      boolean isExactlyMatch,
      String problemDifficulty) {
    Problem problem =
        new Problem(
            problemId,
            PublicationStatus.normalize(status),
            problemName,
            timeLimit,
            memoryLimit,
            description,
            inputFormat,
            outputFormat,
            inputSample,
            outputSample,
            hint);
    problem.setProblemDifficulty(getProblemDifficultyOrDefault(problemDifficulty));
    Map<String, Boolean> result = getProblemEditResult(problem);

    if (result.get("isSuccessful")) {
      problemMapper.updateProblem(problem);

      // Test data is managed separately (archive upload / per-checkpoint delete); editing only
      // re-applies the exact-match flag to the existing checkpoints.
      checkpointMapper.updateCheckpointsExactlyMatch(problemId, isExactlyMatch);
      updateProblemCategoryRelationships(problemId, problemCategories);
      updateProblemTags(problemId, problemTags);
    }
    return result;
  }

  /**
   * Checks whether the problem information is legal.
   *
   * @param problem - the problem to edit
   * @return a Map<String, Boolean> object containing the problem editing result
   */
  private Map<String, Boolean> getProblemEditResult(Problem problem) {
    @SuppressWarnings("unchecked")
    Map<String, Boolean> result = (Map<String, Boolean>) getProblemCreationResult(problem);

    long problemId = problem.getProblemId();
    if (problemMapper.getProblem(problemId) != null) {
      result.put("isProblemExists", true);
    } else {
      result.put("isProblemExists", false);
      result.put("isSuccessful", false);
    }
    return result;
  }

  /**
   * Creates the test cases.
   *
   * @param problemId - the unique identifier of the problem
   * @param testCases - the test cases (JSON format)
   * @param isExactlyMatch - whether the test cases require an exact match
   */
  private void createTestCases(long problemId, String testCases, boolean isExactlyMatch) {
    List<Map<String, String>> testCaseList =
        JsonUtils.toObject(testCases, new TypeReference<List<Map<String, String>>>() {});
    createTestCases(problemId, testCaseList, isExactlyMatch);
  }

  /**
   * Creates the test cases from parsed input/output pairs, numbering checkpoints from 0 and
   * distributing 100 points evenly across them.
   *
   * @param problemId - the unique identifier of the problem
   * @param testCaseList - the input/output pairs (each map holds {@code input} and {@code output})
   * @param isExactlyMatch - whether the test cases require an exact match
   */
  private void createTestCases(
      long problemId, List<Map<String, String>> testCaseList, boolean isExactlyMatch) {
    for (int i = 0; i < testCaseList.size(); ++i) {
      Map<String, String> testCase = testCaseList.get(i);

      int score = 100 / testCaseList.size();
      if (i == testCaseList.size() - 1) {
        score = 100 - score * i;
      }
      String input = testCase.get("input");
      String output = testCase.get("output");

      Checkpoint checkpoint = new Checkpoint(problemId, i, isExactlyMatch, score, input, output);
      checkpointMapper.createCheckpoint(checkpoint);
    }
    // Invalidate any judger's cached copy of this problem's test data. Runs in the same transaction
    // as the checkpoint writes (both create and edit funnel through here), so the bump is rolled back
    // together with the data if the surrounding operation fails.
    problemMapper.bumpCheckpointsVersion(problemId);
  }

  /**
   * Creates the categories a problem belongs to.
   *
   * @param problemId - the unique identifier of the problem
   * @param problemCategories - a JSON array of problem category aliases
   */
  private void createProblemCategoryRelationships(long problemId, String problemCategories) {
    List<String> categorySlugs = JsonUtils.toList(problemCategories, String.class);

    if (categorySlugs.isEmpty()) {
      categorySlugs.add("uncategorized");
    }
    for (String problemCategorySlug : categorySlugs) {
      ProblemCategory pc =
          problemCategoryMapper.getProblemCategoryUsingCategorySlug(problemCategorySlug);

      problemCategoryMapper.createProblemCategoryRelationship(problemId, pc);
    }
  }

  /**
   * Updates the categories a problem belongs to. First deletes all categories of the problem, then
   * recreates the category relationships.
   *
   * @param problemId - the unique identifier of the problem
   * @param problemCategories - a JSON array of problem category aliases
   */
  private void updateProblemCategoryRelationships(long problemId, String problemCategories) {
    problemCategoryMapper.deleteProblemCategoryRelationship(problemId);
    createProblemCategoryRelationships(problemId, problemCategories);
  }

  /**
   * Creates the problem tags.
   *
   * @param problemId - the unique identifier of the problem
   * @param problemTags - a JSON array of problem tags
   */
  private void createProblemTags(long problemId, String problemTags) {
    Set<String> problemTagSlugs = new HashSet<>();
    List<String> problemTagNames = JsonUtils.toList(problemTags, String.class);

    for (String problemTagName : problemTagNames) {
      String problemTagSlug = SlugifyUtils.getSlug(problemTagName);

      ProblemTag pt = problemTagMapper.getProblemTagUsingTagSlug(problemTagSlug);
      if (pt == null) {
        pt = new ProblemTag(problemTagSlug, problemTagName);
        problemTagMapper.createProblemTag(pt);
      }
      // Fix Bug: Two tags have different tag name but the same tag slug.
      // Example: Hello World / Hello-World
      if (!problemTagSlugs.contains(problemTagSlug)) {
        problemTagMapper.createProblemTagRelationship(problemId, pt);
        problemTagSlugs.add(problemTagSlug);
      }
    }
  }

  /**
   * Updates the problem tags. First deletes all tags of the problem, then recreates the
   * tag-to-problem relationships.
   *
   * @param problemId - the unique identifier of the problem
   * @param problemTags - a JSON array of problem tags
   */
  private void updateProblemTags(long problemId, String problemTags) {
    problemTagMapper.deleteProblemTagRelationship(problemId);
    createProblemTags(problemId, problemTags);
  }

  /**
   * Checks whether the problem name is legal.
   *
   * @param problemName - the name of the problem
   * @return whether the problem name is legal
   */
  private boolean isProblemNameLegal(String problemName) {
    return problemName.length() <= 128;
  }

  /**
   * [For administrators only] Deletes the specified problem.
   *
   * @param problemId - the unique identifier of the problem
   */
  public void deleteProblem(long problemId) {
    problemMapper.deleteProblem(problemId);
  }

  /**
   * [For administrators only] Creates a problem category.
   *
   * @param problemCategorySlug - the alias of the problem category
   * @param problemCategoryName - the name of the problem category
   * @param parentProblemCategorySlug - the alias of the parent problem category
   * @return a Map<String, Object> object containing the problem category creation result
   */
  public Map<String, Object> createProblemCategory(
      String problemCategorySlug, String problemCategoryName, String parentProblemCategorySlug) {
    int parentProblemCategoryId = getProblemCategoryIdUsingSlug(parentProblemCategorySlug);
    ProblemCategory problemCategory =
        new ProblemCategory(problemCategorySlug, problemCategoryName, parentProblemCategoryId);
    @SuppressWarnings("unchecked")
    Map<String, Object> result =
        (Map<String, Object>) getProblemCategoryCreationResult(problemCategory);

    if ((boolean) result.get("isSuccessful")) {
      problemCategoryMapper.createProblemCategory(problemCategory);

      long problemCategoryId = problemCategory.getProblemCategoryId();
      result.put("problemCategoryId", problemCategoryId);
    }
    return result;
  }

  /**
   * Checks the legality of each field of the problem category object to be created.
   *
   * @param problemCategory - the problem category object to be created
   * @return a Map<String, Boolean> object containing the validation result of each field of the
   *     problem category object
   */
  private Map<String, ? extends Object> getProblemCategoryCreationResult(
      ProblemCategory problemCategory) {
    Map<String, Boolean> result = new HashMap<>(6, 1);
    result.put("isProblemCategorySlugEmpty", problemCategory.getProblemCategorySlug().isEmpty());
    result.put(
        "isProblemCategorySlugLegal",
        isProblemCategorySlugLegal(problemCategory.getProblemCategorySlug()));
    result.put(
        "isProblemCategorySlugExists",
        isProblemCategorySlugExists(problemCategory.getProblemCategorySlug()));
    result.put("isProblemCategoryNameEmpty", problemCategory.getProblemCategoryName().isEmpty());
    result.put(
        "isProblemCategoryNameLegal",
        isProblemCategoryNameLegal(problemCategory.getProblemCategoryName()));

    boolean isSuccessful =
        !result.get("isProblemCategorySlugEmpty")
            && result.get("isProblemCategorySlugLegal")
            && !result.get("isProblemCategorySlugExists")
            && !result.get("isProblemCategoryNameEmpty")
            && result.get("isProblemCategoryNameLegal");
    result.put("isSuccessful", isSuccessful);
    return result;
  }

  /**
   * [For administrators only] Edits a problem category.
   *
   * @param problemCategoryId - the unique identifier of the problem category
   * @param problemCategorySlug - the alias of the problem category
   * @param problemCategoryName - the name of the problem category
   * @param parentProblemCategorySlug - the alias of the parent problem category
   * @return a Map<String, Boolean> object containing the problem category editing result
   */
  public Map<String, Boolean> editProblemCategory(
      int problemCategoryId,
      String problemCategorySlug,
      String problemCategoryName,
      String parentProblemCategorySlug) {
    int parentProblemCategoryId = getProblemCategoryIdUsingSlug(parentProblemCategorySlug);

    ProblemCategory problemCategory =
        new ProblemCategory(
            problemCategoryId, problemCategorySlug, problemCategoryName, parentProblemCategoryId);

    Map<String, Boolean> result = getProblemCategoryEditResult(problemCategory);

    if (result.get("isSuccessful")) {
      problemCategoryMapper.updateProblemCategory(problemCategory);
    }
    return result;
  }

  /**
   * Gets the editing result of a problem category.
   *
   * @param problemCategory - the problem category object to edit
   * @return a Map<String, Boolean> object containing the problem category editing result
   */
  private Map<String, Boolean> getProblemCategoryEditResult(ProblemCategory problemCategory) {
    Map<String, Boolean> result = new HashMap<>();
    result.put(
        "isProblemCategoryExists", isProblemCategoryExists(problemCategory.getProblemCategoryId()));
    result.put(
        "isProblemCategoryEditable",
        isProblemCategoryEditable(problemCategory.getProblemCategoryId()));
    result.put("isProblemCategorySlugEmpty", problemCategory.getProblemCategorySlug().isEmpty());
    result.put(
        "isProblemCategorySlugLegal",
        isProblemCategorySlugLegal(problemCategory.getProblemCategorySlug()));
    result.put(
        "isProblemCategorySlugExists",
        isProblemCategorySlugExists(problemCategory, problemCategory.getProblemCategorySlug()));
    result.put("isProblemCategoryNameEmpty", problemCategory.getProblemCategoryName().isEmpty());
    result.put(
        "isProblemCategoryNameLegal",
        isProblemCategoryNameLegal(problemCategory.getProblemCategoryName()));

    boolean isSuccessful =
        result.get("isProblemCategoryExists")
            && result.get("isProblemCategoryEditable")
            && !result.get("isProblemCategorySlugEmpty")
            && result.get("isProblemCategorySlugLegal")
            && !result.get("isProblemCategorySlugExists")
            && !result.get("isProblemCategoryNameEmpty")
            && result.get("isProblemCategoryNameLegal");
    result.put("isSuccessful", isSuccessful);
    return result;
  }

  /**
   * Checks whether a problem category exists.
   *
   * @param problemCategoryId - the unique identifier of the problem category
   * @return whether the problem category exists
   */
  private boolean isProblemCategoryExists(int problemCategoryId) {
    ProblemCategory problemCategory =
        problemCategoryMapper.getProblemCategoryUsingCategoryId(problemCategoryId);
    return problemCategory != null;
  }

  /**
   * Checks whether a problem category is editable. The category with the unique identifier 1 is the
   * default category and is not editable.
   *
   * @param problemCategoryId - the unique identifier of the problem category to edit
   * @return whether the problem category is editable
   */
  private boolean isProblemCategoryEditable(int problemCategoryId) {
    return problemCategoryId != 1;
  }

  /**
   * Gets the unique identifier of a problem category by its alias.
   *
   * @param problemCategorySlug - the alias of the problem category
   * @return the unique identifier of the problem category
   */
  private int getProblemCategoryIdUsingSlug(String problemCategorySlug) {
    int problemCategoryId = 0;
    if (!problemCategorySlug.isEmpty()) {
      ProblemCategory problemCategory =
          problemCategoryMapper.getProblemCategoryUsingCategorySlug(problemCategorySlug);

      if (problemCategory != null) {
        problemCategoryId = problemCategory.getProblemCategoryId();
      }
    }
    return problemCategoryId;
  }

  /**
   * Checks the legality of a problem category alias.
   *
   * @param problemCategorySlug - the alias of the problem category
   * @return whether the problem category alias is legal
   */
  private boolean isProblemCategorySlugLegal(String problemCategorySlug) {
    return problemCategorySlug.length() <= 32;
  }

  /**
   * Checks whether a problem category exists (checks for a duplicate slug).
   *
   * @param problemCategorySlug - the alias of the problem category
   * @return whether the problem category exists
   */
  private boolean isProblemCategorySlugExists(String problemCategorySlug) {
    ProblemCategory problemCategory =
        problemCategoryMapper.getProblemCategoryUsingCategorySlug(problemCategorySlug);
    return problemCategory != null;
  }

  /**
   * Checks whether a problem category exists (checks for a duplicate slug).
   *
   * @param problemCategory - the current problem category object
   * @param problemCategorySlug - the alias of the problem category
   * @return whether the problem category exists
   */
  private boolean isProblemCategorySlugExists(
      ProblemCategory problemCategory, String problemCategorySlug) {
    ProblemCategory anotherProblemCategory =
        problemCategoryMapper.getProblemCategoryUsingCategorySlug(problemCategorySlug);
    return anotherProblemCategory != null
        && anotherProblemCategory.getProblemCategoryId() != problemCategory.getProblemCategoryId();
  }

  /**
   * Checks the legality of a problem category name.
   *
   * @param problemCategoryName - the name of the problem category
   * @return whether the problem category name is legal
   */
  private boolean isProblemCategoryNameLegal(String problemCategoryName) {
    return problemCategoryName.length() <= 32;
  }

  /**
   * Deletes a problem category by its unique identifier.
   *
   * @param problemCategoryId - the unique identifier of the problem category
   * @return whether the problem category was deleted
   */
  public boolean deleteProblemCategory(int problemCategoryId) {
    boolean isProblemCategoryEditable = false;
    if (isProblemCategoryEditable(problemCategoryId)) {
      problemCategoryMapper.deleteProblemCategory(problemCategoryId);
      isProblemCategoryEditable = true;
    }
    return isProblemCategoryEditable;
  }

  /** The autowired ProblemMapper object. */
  @Autowired private ProblemMapper problemMapper;

  /** The autowired ProblemCategoryMapper object. */
  @Autowired private ProblemCategoryMapper problemCategoryMapper;

  /** The autowired ProblemDifficultyMapper object. */
  @Autowired private ProblemDifficultyMapper problemDifficultyMapper;

  /** The autowired ProblemTagMapper object. */
  @Autowired private ProblemTagMapper problemTagMapper;

  /** The autowired CheckpointMapper object. */
  @Autowired private CheckpointMapper checkpointMapper;
}
