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

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

import org.verwandlung.voj.web.mapper.ProblemMapper;
import org.verwandlung.voj.web.model.Checkpoint;
import org.verwandlung.voj.web.model.Problem;
import org.verwandlung.voj.web.model.ProblemCategory;
import org.verwandlung.voj.web.model.ProblemTag;

/**
 * ProblemService测试类.
 *
 * <p>种子数据: 4道试题 (1000, 1001, 1003公开, 1002私有); 2个试题分类 (uncategorized默认/不可编辑, dynamic-programming); 2个标签;
 * 试题1000和1001各有10个测试点 (合计20个).
 *
 * @author Haozhe Xie
 */
@ExtendWith(SpringExtension.class)
@Transactional
@ContextConfiguration({"classpath:test-spring-context.xml"})
public class ProblemServiceTest {
  private static final String TEST_CASES = "[{\"input\": \"1 2\", \"output\": \"3\"}]";
  private static final String CATEGORIES = "[\"uncategorized\"]";
  private static final String TAGS = "[\"Greedy\"]";

  /** 测试用例: 测试getFirstIndexOfProblems()方法 测试数据: N/a 预期结果: 返回试题ID的下界 */
  @Test
  public void testGetFirstIndexOfProblems() {
    Assertions.assertEquals(1000, problemService.getFirstIndexOfProblems());
  }

  /** 测试用例: 测试getNumberOfProblems()方法 测试数据: N/a 预期结果: 返回全部试题的数量 */
  @Test
  public void testGetNumberOfProblems() {
    Assertions.assertEquals(4, problemService.getNumberOfProblems());
  }

  /** 测试用例: 测试getProblem(long)方法 测试数据: 存在/不存在的试题标识符 预期结果: 返回对应试题或空引用 */
  @Test
  public void testGetProblem() {
    Assertions.assertNotNull(problemService.getProblem(1000));
    Assertions.assertNull(problemService.getProblem(0));
  }

  /** 测试用例: 测试getProblemsUsingFilters(...)方法 测试数据: 不带过滤条件 预期结果: 返回全部试题 */
  @Test
  public void testGetProblemsUsingFiltersWithoutFilters() {
    List<Problem> problems = problemService.getProblemsUsingFilters(0, "", "", "", false, 100);
    Assertions.assertEquals(4, problems.size());
  }

  /** 测试用例: 测试getProblemsUsingFilters(...)方法 测试数据: 仅公开试题 预期结果: 返回公开试题 */
  @Test
  public void testGetProblemsUsingFiltersPublicOnly() {
    List<Problem> problems = problemService.getProblemsUsingFilters(0, "", "", "", true, 100);
    Assertions.assertEquals(3, problems.size());
  }

  /** 测试用例: 测试getProblemsUsingFilters(...)方法 测试数据: 按分类过滤 预期结果: 仅返回该分类下的试题 */
  @Test
  public void testGetProblemsUsingFiltersByCategory() {
    List<Problem> problems =
        problemService.getProblemsUsingFilters(0, "", "dynamic-programming", "", false, 100);
    // 仅试题1000属于dynamic-programming分类.
    Assertions.assertEquals(1, problems.size());
    Assertions.assertEquals(1000, problems.get(0).getProblemId());
  }

  /** 测试用例: 测试getNumberOfProblemsUsingFilters(...)方法 测试数据: 全部/仅公开 预期结果: 返回对应数量 */
  @Test
  public void testGetNumberOfProblemsUsingFilters() {
    Assertions.assertEquals(4, problemService.getNumberOfProblemsUsingFilters("", "", false));
    Assertions.assertEquals(3, problemService.getNumberOfProblemsUsingFilters("", "", true));
  }

  /** 测试用例: 测试getProblemCategories()方法 测试数据: N/a 预期结果: 返回全部试题分类 */
  @Test
  public void testGetProblemCategories() {
    Assertions.assertEquals(2, problemService.getProblemCategories().size());
  }

  /** 测试用例: 测试getProblemCategoriesWithHierarchy()方法 测试数据: N/a 预期结果: 返回2个顶级分类, 各自子分类为空 */
  @Test
  public void testGetProblemCategoriesWithHierarchy() {
    Map<ProblemCategory, List<ProblemCategory>> hierarchy =
        problemService.getProblemCategoriesWithHierarchy();
    Assertions.assertEquals(2, hierarchy.size());
    for (List<ProblemCategory> subCategories : hierarchy.values()) {
      Assertions.assertTrue(subCategories.isEmpty());
    }
  }

  /** 测试用例: 测试getProblemCategoriesUsingProblemId(long)方法 测试数据: 试题1000 预期结果: 返回该试题的全部分类 */
  @Test
  public void testGetProblemCategoriesUsingProblemId() {
    List<ProblemCategory> categories = problemService.getProblemCategoriesUsingProblemId(1000);
    Assertions.assertEquals(2, categories.size());
  }

  /** 测试用例: 测试getProblemCategoriesOfProblems(long, long)方法 测试数据: 试题区间[1000, 1003] 预期结果: 按试题索引返回分类 */
  @Test
  public void testGetProblemCategoriesOfProblems() {
    Map<Long, List<ProblemCategory>> categories =
        problemService.getProblemCategoriesOfProblems(1000, 1003);
    Assertions.assertEquals(2, categories.get(1000L).size());
  }

  /** 测试用例: 测试getProblemTagsUsingProblemId(long)方法 测试数据: 试题1001 预期结果: 返回该试题的全部标签 */
  @Test
  public void testGetProblemTagsUsingProblemId() {
    List<ProblemTag> tags = problemService.getProblemTagsUsingProblemId(1001);
    Assertions.assertEquals(2, tags.size());
  }

  /** 测试用例: 测试getProblemTagsOfProblems(long, long)方法 测试数据: 试题区间[1000, 1003] 预期结果: 按试题索引返回标签 */
  @Test
  public void testGetProblemTagsOfProblems() {
    Map<Long, List<ProblemTag>> tags = problemService.getProblemTagsOfProblems(1000, 1003);
    Assertions.assertEquals(2, tags.get(1001L).size());
  }

  /** 测试用例: 测试getNumberOfCheckpoints()方法 测试数据: N/a 预期结果: 返回全部测试点的数量 */
  @Test
  public void testGetNumberOfCheckpoints() {
    Assertions.assertEquals(20, problemService.getNumberOfCheckpoints());
  }

  /** 测试用例: 测试getCheckpointsUsingProblemId(long)方法 测试数据: 试题1000与试题1002 预期结果: 返回对应测试点列表 */
  @Test
  public void testGetCheckpointsUsingProblemId() {
    List<Checkpoint> checkpoints = problemService.getCheckpointsUsingProblemId(1000);
    Assertions.assertEquals(10, checkpoints.size());
    Assertions.assertTrue(problemService.getCheckpointsUsingProblemId(1002).isEmpty());
  }

  /** 测试用例: 测试createProblem(...)方法 测试数据: 合法的试题数据 预期结果: 创建成功并返回试题标识符 */
  @Test
  public void testCreateProblemSuccessfully() {
    Map<String, Object> result =
        problemService.createProblem(
            "A New Problem", 1000, 65536, "Description", "Hint", "Input Format",
            "Output Format", "1 2", "3", TEST_CASES, CATEGORIES, TAGS, true, true);
    Assertions.assertTrue((Boolean) result.get("isSuccessful"));
    Assertions.assertTrue(result.containsKey("problemId"));
    Assertions.assertEquals(5, problemService.getNumberOfProblems());
  }

  /** 测试用例: 测试createProblem(...)方法 测试数据: 试题名称为空 预期结果: 创建失败 */
  @Test
  public void testCreateProblemWithEmptyName() {
    Map<String, Object> result =
        problemService.createProblem(
            "", 1000, 65536, "Description", "Hint", "Input Format", "Output Format",
            "1 2", "3", TEST_CASES, CATEGORIES, TAGS, true, true);
    Assertions.assertTrue((Boolean) result.get("isProblemNameEmpty"));
    Assertions.assertFalse((Boolean) result.get("isSuccessful"));
  }

  /** 测试用例: 测试createProblem(...)方法 测试数据: 非法的时间限制 预期结果: 创建失败 */
  @Test
  public void testCreateProblemWithIllegalTimeLimit() {
    Map<String, Object> result =
        problemService.createProblem(
            "A New Problem", 0, 65536, "Description", "Hint", "Input Format", "Output Format",
            "1 2", "3", TEST_CASES, CATEGORIES, TAGS, true, true);
    Assertions.assertFalse((Boolean) result.get("isTimeLimitLegal"));
    Assertions.assertFalse((Boolean) result.get("isSuccessful"));
  }

  /** 测试用例: 测试editProblem(...)方法 测试数据: 存在的试题 预期结果: 编辑成功 */
  @Test
  public void testEditProblemSuccessfully() {
    Map<String, Boolean> result =
        problemService.editProblem(
            1000, "A+B Problem (Edited)", 2000, 131072, "New Description", "Hint",
            "Input Format", "Output Format", "1 2", "3", TEST_CASES, CATEGORIES, TAGS, true, true);
    Assertions.assertTrue(result.get("isSuccessful"));
    Assertions.assertEquals("A+B Problem (Edited)", problemService.getProblem(1000).getProblemName());
  }

  /** 测试用例: 测试editProblem(...)方法 测试数据: 不存在的试题 预期结果: 编辑失败 */
  @Test
  public void testEditProblemNotExists() {
    Map<String, Boolean> result =
        problemService.editProblem(
            99999, "Nonexistent", 1000, 65536, "Description", "Hint", "Input Format",
            "Output Format", "1 2", "3", TEST_CASES, CATEGORIES, TAGS, true, true);
    Assertions.assertFalse(result.get("isProblemExists"));
    Assertions.assertFalse(result.get("isSuccessful"));
  }

  /** 测试用例: 测试deleteProblem(long)方法 测试数据: 无外键引用的试题 预期结果: 试题被删除 */
  @Test
  public void testDeleteProblem() {
    // 直接通过Mapper插入一道没有任何关联数据的试题, 以避免外键约束.
    Problem problem =
        new Problem(
            true, "Throwaway Problem", 1000, 65536, "Description", "Input Format",
            "Output Format", "1 2", "3", "Hint");
    problemMapper.createProblem(problem);
    long problemId = problem.getProblemId();

    problemService.deleteProblem(problemId);
    Assertions.assertNull(problemService.getProblem(problemId));
  }

  /** 测试用例: 测试createProblemCategory(...)方法 测试数据: 合法的分类数据 预期结果: 创建成功并返回分类标识符 */
  @Test
  public void testCreateProblemCategorySuccessfully() {
    Map<String, Object> result =
        problemService.createProblemCategory("graph-theory", "Graph Theory", "");
    Assertions.assertTrue((Boolean) result.get("isSuccessful"));
    Assertions.assertTrue(result.containsKey("problemCategoryId"));
    Assertions.assertEquals(3, problemService.getProblemCategories().size());
  }

  /** 测试用例: 测试createProblemCategory(...)方法 测试数据: 分类别名已存在 预期结果: 创建失败 */
  @Test
  public void testCreateProblemCategoryWithDuplicatedSlug() {
    Map<String, Object> result =
        problemService.createProblemCategory("uncategorized", "Duplicated", "");
    Assertions.assertTrue((Boolean) result.get("isProblemCategorySlugExists"));
    Assertions.assertFalse((Boolean) result.get("isSuccessful"));
  }

  /** 测试用例: 测试createProblemCategory(...)方法 测试数据: 分类别名为空 预期结果: 创建失败 */
  @Test
  public void testCreateProblemCategoryWithEmptySlug() {
    Map<String, Object> result = problemService.createProblemCategory("", "Empty Slug", "");
    Assertions.assertTrue((Boolean) result.get("isProblemCategorySlugEmpty"));
    Assertions.assertFalse((Boolean) result.get("isSuccessful"));
  }

  /** 测试用例: 测试editProblemCategory(...)方法 测试数据: 可编辑的分类 预期结果: 编辑成功 */
  @Test
  public void testEditProblemCategorySuccessfully() {
    Map<String, Boolean> result =
        problemService.editProblemCategory(2, "dynamic-programming", "DP (Updated)", "");
    Assertions.assertTrue(result.get("isSuccessful"));
  }

  /** 测试用例: 测试editProblemCategory(...)方法 测试数据: 默认分类 (ID为1, 不可编辑) 预期结果: 编辑失败 */
  @Test
  public void testEditProblemCategoryNotEditable() {
    Map<String, Boolean> result =
        problemService.editProblemCategory(1, "uncategorized", "Uncategorized", "");
    Assertions.assertFalse(result.get("isProblemCategoryEditable"));
    Assertions.assertFalse(result.get("isSuccessful"));
  }

  /** 测试用例: 测试editProblemCategory(...)方法 测试数据: 不存在的分类 预期结果: 编辑失败 */
  @Test
  public void testEditProblemCategoryNotExists() {
    Map<String, Boolean> result = problemService.editProblemCategory(9999, "slug", "Name", "");
    Assertions.assertFalse(result.get("isProblemCategoryExists"));
    Assertions.assertFalse(result.get("isSuccessful"));
  }

  /** 测试用例: 测试deleteProblemCategory(int)方法 测试数据: 默认分类 (ID为1) 预期结果: 删除失败 */
  @Test
  public void testDeleteProblemCategoryNotEditable() {
    Assertions.assertFalse(problemService.deleteProblemCategory(1));
  }

  /** 测试用例: 测试deleteProblemCategory(int)方法 测试数据: 可编辑且无关联的分类 预期结果: 删除成功 */
  @Test
  public void testDeleteProblemCategorySuccessfully() {
    @SuppressWarnings("unchecked")
    Map<String, Object> created =
        problemService.createProblemCategory("graph-theory", "Graph Theory", "");
    int problemCategoryId = (int) (long) (Long) created.get("problemCategoryId");
    Assertions.assertTrue(problemService.deleteProblemCategory(problemCategoryId));
  }

  /** 待测试的ProblemService对象. */
  @Autowired private ProblemService problemService;

  /** 用于在测试事务内构造无关联试题数据的Mapper. */
  @Autowired private ProblemMapper problemMapper;
}
