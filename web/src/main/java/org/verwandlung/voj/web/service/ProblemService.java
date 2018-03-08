/* Verwandlung Online Judge - A cross-platform judge online system
 * Copyright (C) 2018 Haozhe Xie <cshzxie@gmail.com>
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
 *
 *
 *                              _ooOoo_  
 *                             o8888888o  
 *                             88" . "88  
 *                             (| -_- |)  
 *                             O\  =  /O  
 *                          ____/`---'\____  
 *                        .'  \\|     |//  `.  
 *                       /  \\|||  :  |||//  \  
 *                      /  _||||| -:- |||||-  \  
 *                      |   | \\\  -  /// |   |  
 *                      | \_|  ''\---/''  |   |  
 *                      \  .-\__  `-`  ___/-. /  
 *                    ___`. .'  /--.--\  `. . __  
 *                 ."" '<  `.___\_<|>_/___.'  >'"".  
 *                | | :  `- \`.;`\ _ /`;.`/ - ` : | |  
 *                \  \ `-.   \_ __\ /__ _/   .-` /  /  
 *           ======`-.____`-.___\_____/___.-`____.-'======  
 *                              `=---=' 
 *
 *                          HERE BE BUDDHA
 *
 */
package org.verwandlung.voj.web.service;

import java.util.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.verwandlung.voj.web.mapper.CheckpointMapper;
import org.verwandlung.voj.web.mapper.ProblemCategoryMapper;
import org.verwandlung.voj.web.mapper.ProblemMapper;
import org.verwandlung.voj.web.mapper.ProblemTagMapper;
import org.verwandlung.voj.web.model.*;
import org.verwandlung.voj.web.util.SlugifyUtils;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

/**
 * 试题类(Problem)的业务逻辑层.
 * @author Haozhe Xie
 */
@Service
@Transactional
public class ProblemService {
	/**
	 * 获取试题的起始编号.
	 * @return 试题的起始编号
	 */
	public long getFirstIndexOfProblems() {
		return problemMapper.getLowerBoundOfProblems();
	}
	
	/**
	 * 获取试题的结束编号.
	 * @param isPublicOnly - 是否只筛选公开试题
	 * @param offset - 试题唯一标识符的起始序号
	 * @param limit - 每次加载试题的数量
	 * @return 试题的结束编号
	 */
	public long getLastIndexOfProblems(boolean isPublicOnly, long offset, int limit) {
		return problemMapper.getUpperBoundOfProblemsWithLimit(isPublicOnly, offset, limit);
	}
	
	/**
	 * 通过试题的唯一标识符获取试题的详细信息.
	 * @param problemId - 试题的唯一标识符
	 * @return 试题的详细信息
	 */
	public Problem getProblem(long problemId) {
		return problemMapper.getProblem(problemId);
	}
	
	/**
	 * 获取试题列表.
	 * @param offset - 试题唯一标识符的起始序号
	 * @param keyword - 关键字
	 * @param problemCategorySlug - 试题分类的别名
	 * @param problemTagSlug - 试题标签的别名
	 * @param isPublicOnly - 是否只筛选公开试题
	 * @param limit - 每次加载试题的数量
	 * @return 试题列表(List<Problem>对象)
	 */
	public List<Problem> getProblemsUsingFilters(long offset, String keyword, String problemCategorySlug,
			String problemTagSlug, boolean isPublicOnly, int limit) {
		ProblemCategory problemCategory = problemCategoryMapper.getProblemCategoryUsingCategorySlug(problemCategorySlug);
		ProblemTag problemTag = problemTagMapper.getProblemTagUsingTagSlug(SlugifyUtils.getSlug(problemTagSlug));
		int problemCategoryId = 0;
		long problemTagId = 0;
		if ( offset < 0 ) {
			offset = 0;
		}
		if ( problemCategory != null ) {
			problemCategoryId = problemCategory.getProblemCategoryId();
		}
		if ( problemTag != null ) {
			problemTagId = problemTag.getProblemTagId();
		}
		return problemMapper.getProblemsUsingFilters(keyword, problemCategoryId, problemTagId, isPublicOnly, offset, limit);
	}
	
	/**
	 * 获取试题的总数量.
	 * @param keyword - 关键字
	 * @param problemCategorySlug - 试题分类的别名
	 * @param isPublicOnly - 是否只筛选公开试题
	 * @return 试题的总数量
	 */
	public long getNumberOfProblemsUsingFilters(String keyword, String problemCategorySlug, boolean isPublicOnly) {
		ProblemCategory problemCategory = problemCategoryMapper.getProblemCategoryUsingCategorySlug(problemCategorySlug);
		int problemCategoryId = 0;
		if ( problemCategory != null ) {
			problemCategoryId = problemCategory.getProblemCategoryId();
		}
		return problemMapper.getNumberOfProblemsUsingFilters(keyword, problemCategoryId, isPublicOnly);
	}

	/**
	 * 获取某个区间内各试题的分类.
	 * @param problemIdLowerBound - 试题ID区间的下界
	 * @param problemIdUpperBound - 试题ID区间的上界
	 * @return 包含试题分类信息的列表
	 */
	public Map<Long, List<ProblemCategory>> getProblemCategoriesOfProblems(
			long problemIdLowerBound, long problemIdUpperBound) {
		List<ProblemCategoryRelationship> problemCategoryRelationships =
				problemCategoryMapper.getProblemCategoriesOfProblems(problemIdLowerBound, problemIdUpperBound);

		Map<Long, List<ProblemCategory>> problemCategoriesOfProblems = new HashMap<>();
		for ( ProblemCategoryRelationship pcr : problemCategoryRelationships ) {
			long problemId = pcr.getProblemId();
			if ( !problemCategoriesOfProblems.containsKey(problemId) ) {
				problemCategoriesOfProblems.put(problemId, new ArrayList<>());
			}

			List<ProblemCategory> problemCategories = problemCategoriesOfProblems.get(problemId);
			problemCategories.add(new ProblemCategory(
					pcr.getProblemCategoryId(), pcr.getProblemCategorySlug(),
					pcr.getProblemCategoryName(), 0));
		}
		return problemCategoriesOfProblems;
	}
	
	/**
	 * 获取试题的分类列表.
	 * @param problemId - 试题的唯一标识符.
	 * @return 包含试题分类的列表
	 */
	public List<ProblemCategory> getProblemCategoriesUsingProblemId(long problemId) {
		return problemCategoryMapper.getProblemCategoriesUsingProblemId(problemId);
	}

	/**
	 * 获取某个区间内各试题的标签.
	 * @param problemIdLowerBound - 试题ID区间的下界
	 * @param problemIdUpperBound - 试题ID区间的上界
	 * @return 包含试题标签信息的列表
	 */
	public Map<Long, List<ProblemTag>> getProblemTagsOfProblems(
			long problemIdLowerBound, long problemIdUpperBound) {
		List<ProblemTagRelationship> problemTagRelationships =
				problemTagMapper.getProblemTagsOfProblems(problemIdLowerBound, problemIdUpperBound);

		Map<Long, List<ProblemTag>> problemTagsOfProblems = new HashMap<>();
		for ( ProblemTagRelationship ptr : problemTagRelationships ) {
			long problemId = ptr.getProblemId();
			if ( !problemTagsOfProblems.containsKey(problemId) ) {
				problemTagsOfProblems.put(problemId, new ArrayList<>());
			}

			List<ProblemTag> problemTags = problemTagsOfProblems.get(problemId);
			problemTags.add(new ProblemTag(
					ptr.getProblemTagId(), ptr.getProblemTagSlug(),
					ptr.getProblemTagName()));
		}
		return problemTagsOfProblems;
	}
	
	/**
	 * 获取试题的标签列表.
	 * @param problemId - 试题的唯一标识符.
	 * @return 包含试题标签的列表
	 */
	public List<ProblemTag> getProblemTagsUsingProblemId(long problemId) {
		return problemTagMapper.getProblemTagsUsingProblemId(problemId);
	}

	/**
	 * 获得具有层次关系的试题分类列表.
	 * @return 包含试题分类及其继承关系的Map<ProblemCategory, List<ProblemCategory>>对象
	 */
	public Map<ProblemCategory, List<ProblemCategory>> getProblemCategoriesWithHierarchy() {
		List<ProblemCategory> problemCategories = getProblemCategories();
		Map<Integer, List<ProblemCategory>> problemCategoriesIndexer = new HashMap<>();
		Map<ProblemCategory, List<ProblemCategory>> problemCategoriesHierarchy = new LinkedHashMap<>();

		// 将无父亲的试题分类加入列表
		for ( ProblemCategory pc : problemCategories ) {
			if ( pc.getParentProblemCategoryId() == 0 ) {
				List<ProblemCategory> subProblemCategories = new ArrayList<>();
				problemCategoriesHierarchy.put(pc, subProblemCategories);
				problemCategoriesIndexer.put(pc.getProblemCategoryId(), subProblemCategories);
			}
		}
		// 将其他试题分类加入列表
		for ( ProblemCategory pc : problemCategories ) {
			int parentProblemCategoryId = pc.getParentProblemCategoryId() ;
			if ( parentProblemCategoryId != 0 ) {
				List<ProblemCategory> subProblemCategories = problemCategoriesIndexer.get(parentProblemCategoryId);
				if ( subProblemCategories != null ) {
					subProblemCategories.add(pc);
				}
			}
		}
		return problemCategoriesHierarchy;
	}
	
	/**
	 * 获取全部的试题分类.
	 * @return 包含全部试题分类的列表
	 */
	public List<ProblemCategory> getProblemCategories() {
		return problemCategoryMapper.getProblemCategories();
	}
	
	/**
	 * [此方法仅供管理员使用]
	 * 获取全部试题的总数量.
	 * @return 全部试题的总数量
	 */
	public long getNumberOfProblems() {
		return problemMapper.getNumberOfProblems();
	}
	
	/**
	 * [此方法仅供管理员使用]
	 * 获取系统中全部试题测试点的数量(包括私有试题).
	 * @return 系统中全部试题测试点的数量
	 */
	public long getNumberOfCheckpoints() {
		return checkpointMapper.getNumberOfCheckpoints();
	}
	
	/**
	 * [此方法仅供管理员使用]
	 * 获取某个试题的测试数据集.
	 * @param problemId - 试题的唯一标识符
	 * @return 某个试题的测试数据列表
	 */
	public List<Checkpoint> getCheckpointsUsingProblemId(long problemId) {
		List<Checkpoint> checkpoints = checkpointMapper.getCheckpointsUsingProblemId(problemId);
		return checkpoints;
	}
	
	/**
	 * [此方法仅供管理员使用]
	 * 创建试题.
	 * @param problemName - 试题名称
	 * @param timeLimit - 时间限制
	 * @param memoryLimit - 内存占用限制
	 * @param description - 试题描述
	 * @param hint - 试题提示
	 * @param inputFormat - 输入格式
	 * @param outputFormat - 输出格式
	 * @param inputSample - 输入样例
	 * @param outputSample - 输出样例
	 * @param testCases - 测试用例(JSON 格式)
	 * @param problemCategories - 试题分类(JSON 格式)
	 * @param problemTags - 试题标签((JSON 格式)
	 * @param isPublic - 试题是否公开
	 * @param isExactlyMatch - 测试点是否精确匹配
	 * @return 包含试题创建结果的Map<String, Object>对象
	 */
	public Map<String, Object> createProblem(String problemName, int timeLimit, int memoryLimit, 
			String description, String hint, String inputFormat, String outputFormat, 
			String inputSample, String outputSample, String testCases, String problemCategories, 
			String problemTags, boolean isPublic, boolean isExactlyMatch) {
		Problem problem = new Problem(isPublic, problemName, timeLimit, memoryLimit, 
				description, inputFormat, outputFormat, inputSample, outputSample, hint);
		@SuppressWarnings("unchecked")
		Map<String, Object> result = (Map<String, Object>) getProblemCreationResult(problem);
		
		if ( (boolean) result.get("isSuccessful") ) {
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
	 * 检查试题信息是否合法.
	 * @param problem - 待创建的试题
	 * @return 包含试题创建结果的Map<String, Boolean>对象
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
		
		boolean isSuccessful = !result.get("isProblemNameEmpty")  &&  result.get("isProblemNameLegal") &&
								result.get("isTimeLimitLegal")	&&  result.get("isMemoryLimitLegal") &&
							   !result.get("isDescriptionEmpty")  && !result.get("isInputFormatEmpty") &&
							   !result.get("isOutputFormatEmpty") && !result.get("isInputSampleEmpty") &&
							   !result.get("isOutputSampleEmpty");
		result.put("isSuccessful", isSuccessful);
		return result;
	}
	
	/**
	 * [此方法仅供管理员使用]
	 * 编辑试题.
	 * @param problemId - 试题唯一标识符
	 * @param problemName - 试题名称
	 * @param timeLimit - 时间限制
	 * @param memoryLimit - 内存占用限制
	 * @param description - 试题描述
	 * @param hint - 试题提示
	 * @param inputFormat - 输入格式
	 * @param outputFormat - 输出格式
	 * @param inputSample - 输入样例
	 * @param outputSample - 输出样例
	 * @param testCases - 测试用例(JSON 格式)
	 * @param problemCategories - 试题分类(JSON 格式)
	 * @param problemTags - 试题标签((JSON 格式)
	 * @param isPublic - 试题是否公开
	 * @param isExactlyMatch - 测试点是否精确匹配
	 * @return 包含试题创建结果的Map<String, Object>对象
	 */
	public Map<String, Boolean> editProblem(long problemId, String problemName, int timeLimit, 
			int memoryLimit, String description, String hint, String inputFormat, String outputFormat, 
			String inputSample, String outputSample, String testCases, String problemCategories, 
			String problemTags, boolean isPublic, boolean isExactlyMatch) {
		Problem problem = new Problem(problemId, isPublic, problemName, timeLimit, memoryLimit, 
				description, inputFormat, outputFormat, inputSample, outputSample, hint);
		Map<String, Boolean> result = getProblemEditResult(problem);
		
		if ( result.get("isSuccessful") ) {
			problemMapper.updateProblem(problem);
			
			updateTestCases(problemId, testCases, isExactlyMatch);
			updateProblemCategoryRelationships(problemId, problemCategories);
			updateProblemTags(problemId, problemTags);
		}
		return result;
	}
	
	/**
	 * 检查试题信息是否合法.
	 * @param problem - 待编辑的试题
	 * @return 包含试题编辑结果的Map<String, Boolean>对象
	 */
	private Map<String, Boolean> getProblemEditResult(Problem problem) {
		@SuppressWarnings("unchecked")
		Map<String, Boolean> result = (Map<String, Boolean>) getProblemCreationResult(problem);
		
		long problemId = problem.getProblemId();
		if ( problemMapper.getProblem(problemId) != null ) {
			result.put("isProblemExists", true);
		} else {
			result.put("isProblemExists", false);
			result.put("isSuccessful", false);
		}
		return result;
	}
	
	/**
	 * 创建测试用例.
	 * @param problemId - 试题的唯一标识符
	 * @param testCases - 测试用例 (JSON格式)
	 * @param isExactlyMatch - 是否精确匹配测试用例
	 */
	private void createTestCases(long problemId, String testCases, boolean isExactlyMatch) {
		JSONArray jsonArray = JSON.parseArray(testCases);
		
		for ( int i = 0; i < jsonArray.size(); ++ i ) {
			JSONObject testCase = jsonArray.getJSONObject(i);
			
			int score = 100 / jsonArray.size();
			if ( i == jsonArray.size() - 1 ) {
				score = 100 - score * i;
			}
			String input = testCase.getString("input");
			String output = testCase.getString("output");
			
			Checkpoint checkpoint = new Checkpoint(problemId, i, isExactlyMatch, score, input, output);
			checkpointMapper.createCheckpoint(checkpoint);
		}
	}
	
	/**
	 * 更新测试用例.
	 * 首先删除该试题的全部的测试用例, 然后创建测试用例.
	 * @param problemId - 试题的唯一标识符
	 * @param testCases - 测试用例 (JSON格式)
	 * @param isExactlyMatch - 是否精确匹配测试用例
	 */
	private void updateTestCases(long problemId, String testCases, boolean isExactlyMatch) {
		checkpointMapper.deleteCheckpoint(problemId);
		createTestCases(problemId, testCases, isExactlyMatch);
	}
	
	/**
	 * 创建试题所属分类.
	 * @param problemId - 试题的唯一标识符
	 * @param problemCategories - 试题分类别名的JSON数组
	 */
	private void createProblemCategoryRelationships(long problemId, String problemCategories) {
		JSONArray jsonArray = JSON.parseArray(problemCategories);
		
		if ( jsonArray.size() == 0 ) {
			jsonArray.add("uncategorized");
		}
		for ( int i = 0; i < jsonArray.size(); ++ i ) {
			String problemCategorySlug = jsonArray.getString(i);
			ProblemCategory pc = problemCategoryMapper.getProblemCategoryUsingCategorySlug(problemCategorySlug);
			
			problemCategoryMapper.createProblemCategoryRelationship(problemId, pc);
		}
	}
	
	/**
	 * 更新试题所属分类.
	 * 首先删除该试题的全部分类, 然后重新创建分类关系.
	 * @param problemId - 试题的唯一标识符
	 * @param problemCategories - 试题分类别名的JSON数组
	 */
	private void updateProblemCategoryRelationships(long problemId, String problemCategories) {
		problemCategoryMapper.deleteProblemCategoryRelationship(problemId);
		createProblemCategoryRelationships(problemId, problemCategories);
	}
	
	/**
	 * 创建试题标签.
	 * @param problemId - 试题的唯一标识符
	 * @param problemTags - 试题标签的JSON数组
	 */
	private void createProblemTags(long problemId, String problemTags) {
		Set<String> problemTagSlugs = new HashSet<>();
		JSONArray jsonArray = JSON.parseArray(problemTags);
		
		for ( int i = 0; i < jsonArray.size(); ++ i ) {
			String problemTagName = jsonArray.getString(i);
			String problemTagSlug = SlugifyUtils.getSlug(problemTagName);
			
			ProblemTag pt = problemTagMapper.getProblemTagUsingTagSlug(problemTagSlug);
			if ( pt == null ) {
				pt = new ProblemTag(problemTagSlug, problemTagName);
				problemTagMapper.createProblemTag(pt);
			}
			// Fix Bug: Two tags have different tag name but the same tag slug.
			// Example: Hello World / Hello-World
			if ( !problemTagSlugs.contains(problemTagSlug) ) {
				problemTagMapper.createProblemTagRelationship(problemId, pt);
				problemTagSlugs.add(problemTagSlug);
			}
		}
	}
	
	/**
	 * 更新试题标签.
	 * 首先删除该试题的全部标签, 然后重新创建标签与试题的关系.
	 * @param problemId - 试题的唯一标识符
	 * @param problemTags - 试题标签的JSON数组
	 */
	private void updateProblemTags(long problemId, String problemTags) {
		problemTagMapper.deleteProblemTagRelationship(problemId);
		createProblemTags(problemId, problemTags);
	}
	
	/**
	 * 检查试题名称的合法性.
	 * @param problemName - 试题名称
	 * @return 试题名称是否合法
	 */
	private boolean isProblemNameLegal(String problemName) {
		return problemName.length() <= 128;
	}
	
	/**
	 * [此方法仅供管理员使用]
	 * 删除指定的试题.
	 * @param problemId - 试题的唯一标识符
	 */
	public void deleteProblem(long problemId) {
		problemMapper.deleteProblem(problemId);
	}
	
	/**
	 * [此方法仅供管理员使用]
	 * 创建试题分类.
	 * @param problemCategorySlug - 试题分类的别名
	 * @param problemCategoryName - 试题分类的名称
	 * @param parentProblemCategorySlug - 父级试题分类的别名
	 * @return 包含试题分类创建结果的Map<String, Object>对象
	 */
	public Map<String, Object> createProblemCategory(
			String problemCategorySlug, String problemCategoryName,
			String parentProblemCategorySlug) {
		int parentProblemCategoryId = getProblemCategoryIdUsingSlug(parentProblemCategorySlug);
		ProblemCategory problemCategory = new ProblemCategory(
				problemCategorySlug, problemCategoryName, parentProblemCategoryId);
		@SuppressWarnings("unchecked")
		Map<String, Object> result = (Map<String, Object>) getProblemCategoryCreationResult(problemCategory);
		
		if ( (boolean) result.get("isSuccessful") ) {
			problemCategoryMapper.createProblemCategory(problemCategory);
			
			long problemCategoryId = problemCategory.getProblemCategoryId();
			result.put("problemCategoryId", problemCategoryId);
		}
		return result;
	}
	
	/**
	 * 检查欲创建的试题分类对象各字段的合法性.
	 * @param problemCategory - 欲创建的试题分类对象
	 * @return 包含试题分类对象各字段验证结果的Map<String, Boolean>对象
	 */
	private Map<String, ? extends Object> getProblemCategoryCreationResult(ProblemCategory problemCategory) {
		Map<String, Boolean> result = new HashMap<>(6, 1);
		result.put("isProblemCategorySlugEmpty", problemCategory.getProblemCategorySlug().isEmpty());
		result.put("isProblemCategorySlugLegal", isProblemCategorySlugLegal(problemCategory.getProblemCategorySlug()));
		result.put("isProblemCategorySlugExists", isProblemCategorySlugExists(problemCategory.getProblemCategorySlug()));
		result.put("isProblemCategoryNameEmpty", problemCategory.getProblemCategoryName().isEmpty());
		result.put("isProblemCategoryNameLegal", isProblemCategoryNameLegal(problemCategory.getProblemCategoryName()));
		
		boolean isSuccessful = !result.get("isProblemCategorySlugEmpty") && 
				 result.get("isProblemCategorySlugLegal") && !result.get("isProblemCategorySlugExists") &&
				!result.get("isProblemCategoryNameEmpty") &&  result.get("isProblemCategoryNameLegal");
		result.put("isSuccessful", isSuccessful);
		return result;
	}
	
	/**
	 * [此方法仅供管理员使用]
	 * 编辑试题分类.
	 * @param problemCategoryId - 试题分类的唯一标识符
	 * @param problemCategorySlug - 试题分类的别名
	 * @param problemCategoryName - 试题分类的名称
	 * @param parentProblemCategorySlug - 父级试题分类的别名
	 * @return 包含试题分类编辑结果的Map<String, Boolean>对象
	 */
	public Map<String, Boolean> editProblemCategory(
			int problemCategoryId, String problemCategorySlug, 
			String problemCategoryName, String parentProblemCategorySlug) {
		int parentProblemCategoryId = getProblemCategoryIdUsingSlug(parentProblemCategorySlug);

		ProblemCategory problemCategory = new ProblemCategory(problemCategoryId,
				problemCategorySlug, problemCategoryName, parentProblemCategoryId);

		Map<String, Boolean> result = getProblemCategoryEditResult(problemCategory);

		if ( result.get("isSuccessful") ) {
			problemCategoryMapper.updateProblemCategory(problemCategory);
		}
		return result;
	}

	/**
	 * 获取试题分类的编辑结果.
	 * @param problemCategory - 待编辑的试题分类对象
	 * @return 包含试题分类编辑结果的Map<String, Boolean>对象
	 */
	private Map<String, Boolean> getProblemCategoryEditResult(ProblemCategory problemCategory) {
		Map<String, Boolean> result = new HashMap<>();
		result.put("isProblemCategoryExists", isProblemCategoryExists(problemCategory.getProblemCategoryId()));
		result.put("isProblemCategoryEditable", isProblemCategoryEditable(problemCategory.getProblemCategoryId()));
		result.put("isProblemCategorySlugEmpty", problemCategory.getProblemCategorySlug().isEmpty());
		result.put("isProblemCategorySlugLegal", isProblemCategorySlugLegal(problemCategory.getProblemCategorySlug()));
		result.put("isProblemCategorySlugExists", isProblemCategorySlugExists(problemCategory, problemCategory.getProblemCategorySlug()));
		result.put("isProblemCategoryNameEmpty", problemCategory.getProblemCategoryName().isEmpty());
		result.put("isProblemCategoryNameLegal", isProblemCategoryNameLegal(problemCategory.getProblemCategoryName()));

		boolean isSuccessful = result.get("isProblemCategoryExists") &&
				result.get("isProblemCategoryEditable")   && !result.get("isProblemCategorySlugEmpty")  &&
				result.get("isProblemCategorySlugLegal")  && !result.get("isProblemCategorySlugExists") &&
				!result.get("isProblemCategoryNameEmpty") &&  result.get("isProblemCategoryNameLegal");
		result.put("isSuccessful", isSuccessful);
		return result;
	}

	/**
	 * 检查分类目录是否存在.
	 * @param problemCategoryId - 分类目录的唯一标识符
	 * @return 分类目录是否存在
	 */
	private boolean isProblemCategoryExists(int problemCategoryId) {
		ProblemCategory problemCategory = problemCategoryMapper.getProblemCategoryUsingCategoryId(problemCategoryId);
		return problemCategory != null;
	}

	/**
	 * 检查试题分类是否可编辑.
	 * 试题分类的唯一标识符为1的项目是默认分类, 不可编辑.
	 * @param problemCategoryId - 待编辑的试题分类对象
	 * @return 试题分类是否可编辑
	 */
	private boolean isProblemCategoryEditable(int problemCategoryId) {
		return problemCategoryId != 1;
	}

	/**
	 * 根据试题分类的别名获取试题分类的唯一标识符.
	 * @param problemCategorySlug - 试题分类的别名
	 * @return 试题分类的唯一标识符
	 */
	private int getProblemCategoryIdUsingSlug(String problemCategorySlug) {
		int problemCategoryId = 0;
		if ( !problemCategorySlug.isEmpty() ) {
			ProblemCategory problemCategory = problemCategoryMapper.
					getProblemCategoryUsingCategorySlug(problemCategorySlug);
			
			if ( problemCategory != null ) {
				problemCategoryId = problemCategory.getProblemCategoryId();
			}
		}
		return problemCategoryId;
	}
	
	/**
	 * 检查试题分类的别名的合法性
	 * @param problemCategorySlug - 试题分类的别名
	 * @return 试题分类的别名是否合法
	 */
	private boolean isProblemCategorySlugLegal(String problemCategorySlug) {
		return problemCategorySlug.length() <= 32;
	}
	
	/**
	 * 检查试题分类是否存在(检查Slug是否重复)
	 * @param problemCategorySlug - 试题分类的别名
	 * @return 试题分类是否存在
	 */
	private boolean isProblemCategorySlugExists(String problemCategorySlug) {
		ProblemCategory problemCategory = problemCategoryMapper.
				getProblemCategoryUsingCategorySlug(problemCategorySlug);
		return problemCategory != null;
	}

	/**
	 * 检查试题分类是否存在(检查Slug是否重复)
	 * @param problemCategory - 当前的试题分类对象
	 * @param problemCategorySlug - 试题分类的别名
	 * @return 试题分类是否存在
	 */
	private boolean isProblemCategorySlugExists(ProblemCategory problemCategory, String problemCategorySlug) {
		ProblemCategory anotherProblemCategory = problemCategoryMapper.
				getProblemCategoryUsingCategorySlug(problemCategorySlug);
		return anotherProblemCategory != null &&
				anotherProblemCategory.getProblemCategoryId() != problemCategory.getProblemCategoryId();
	}
	
	/**
	 * 检查试题分类名称的合法性
	 * @param problemCategoryName - 试题分类的名称
	 * @return 试题分类名称是否合法
	 */
	private boolean isProblemCategoryNameLegal(String problemCategoryName) {
		return problemCategoryName.length() <= 32;
	}

	/**
	 * 根据试题分类的唯一标识符删除某个试题分类.
	 * @param problemCategoryId - 分类目录的唯一标识符
	 * @return 试题分类是否被删除
	 */
	public boolean deleteProblemCategory(int problemCategoryId) {
		boolean isProblemCategoryEditable = false;
		if ( isProblemCategoryEditable(problemCategoryId) ) {
			problemCategoryMapper.deleteProblemCategory(problemCategoryId);
			isProblemCategoryEditable = true;
		}
		return isProblemCategoryEditable;
	}

	/**
	 * 自动注入的ProblemMapper对象.
	 */
	@Autowired
	private ProblemMapper problemMapper;
	
	/**
	 * 自动注入的ProblemCategoryMapper对象.
	 */
	@Autowired
	private ProblemCategoryMapper problemCategoryMapper;
	
	/**
	 * 自动注入的ProblemTagMapper对象.
	 */
	@Autowired
	private ProblemTagMapper problemTagMapper;
	
	/**
	 * 自动注入的CheckpointMapper对象.
	 */
	@Autowired
	private CheckpointMapper checkpointMapper;
}
