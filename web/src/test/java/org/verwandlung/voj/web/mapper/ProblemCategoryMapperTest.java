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
package org.verwandlung.voj.web.mapper;

import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

import org.verwandlung.voj.web.model.ProblemCategory;
import org.verwandlung.voj.web.model.ProblemCategoryRelationship;

/**
 * ProblemCategoryMapper测试类.
 *
 * @author Haozhe Xie
 */
@ExtendWith(SpringExtension.class)
@Transactional
@ContextConfiguration({"classpath:test-spring-context.xml"})
public class ProblemCategoryMapperTest {
	/**
	 * 测试用例: 测试getProblemCategoryUsingId()方法
	 * 测试数据: N/a
	 * 预期结果: 返回默认分类的试题分类对象
	 */
	@Test
	public void testGetProblemCategories() {
		List<ProblemCategory> problemCategories = problemCategoryMapper.getProblemCategories();
		Assertions.assertNotNull(problemCategories);
		Assertions.assertEquals(2, problemCategories.size());

		ProblemCategory firstProblemCategory = problemCategories.get(0);
		String problemCategoryName = firstProblemCategory.getProblemCategoryName();
		Assertions.assertEquals("Uncategorized", problemCategoryName);
	}

	/**
	 * 测试用例: 测试getProblemCategoryUsingId(int)方法
	 * 测试数据: 默认分类的唯一标识符
	 * 预期结果: 返回默认分类的试题分类对象
	 */
	@Test
	public void testGetProblemCategoryUsingIdExists() {
		ProblemCategory problemCategory = problemCategoryMapper.getProblemCategoryUsingCategoryId(1);
		Assertions.assertNotNull(problemCategory);

		String problemCategoryName = problemCategory.getProblemCategoryName();
		Assertions.assertEquals("Uncategorized", problemCategoryName);
	}

	/**
	 * 测试用例: 测试getProblemCategoryUsingId(int)方法
	 * 测试数据: 不存在的唯一标识符
	 * 预期结果: 空引用
	 */
	@Test
	public void testGetProblemCategoryUsingIdNotExists() {
		ProblemCategory problemCategory = problemCategoryMapper.getProblemCategoryUsingCategoryId(0);
		Assertions.assertNull(problemCategory);
	}

	/**
	 * 测试用例: 测试getProblemCategoriesUsingProblemId(long)方法
	 * 测试数据: 存在的某个试题的唯一标识符
	 * 预期结果: 返回试题的全部试题标签列表
	 */
	@Test
	public void testGetProblemCategoriesUsingProblemIdExists() {
		List<ProblemCategory> problemCategories = problemCategoryMapper.getProblemCategoriesUsingProblemId(1000);
		Assertions.assertEquals(2, problemCategories.size());

		ProblemCategory problemCategory = problemCategories.get(0);
		String problemCategorySlug = problemCategory.getProblemCategorySlug();
		Assertions.assertEquals("uncategorized", problemCategorySlug);
	}

	/**
	 * 测试用例: 测试getProblemCategoriesUsingProblemId(long)方法
	 * 测试数据: 不存在试题的唯一标识符
	 * 预期结果: 返回空的试题标签列表
	 */
	@Test
	public void testGetProblemCategoriesUsingProblemIdNotExists() {
		List<ProblemCategory> problemCategories = problemCategoryMapper.getProblemCategoriesUsingProblemId(0);
		Assertions.assertEquals(0, problemCategories.size());
	}

	/**
	 * 测试用例: 测试getProblemCategoriesOfProblems()方法
	 * 测试数据: 试题ID的范围从1000到1002
	 * 预期结果: 返回全部的试题分类信息(总计4条记录)
	 */
	@Test
	public void testGetProblemCategoriesOfProblemsFrom1000WithLimit3() {
		List<ProblemCategoryRelationship> problemCategoryRelationships = problemCategoryMapper.getProblemCategoriesOfProblems(1000, 1002);
		Assertions.assertEquals(4, problemCategoryRelationships.size());

		ProblemCategoryRelationship pcr = problemCategoryRelationships.get(0);
		Assertions.assertEquals(1000, pcr.getProblemId());
		Assertions.assertEquals(1, pcr.getProblemCategoryId());
	}

	/**
	 * 测试用例: 测试getProblemCategoriesOfProblems()方法
	 * 测试数据: 试题ID的范围从999到1000
	 * 预期结果: 返回全部的试题分类信息(总计2条记录)
	 */
	@Test
	public void testGetProblemCategoriesOfProblemsFrom999WithLimit2() {
		List<ProblemCategoryRelationship> problemCategoryRelationships = problemCategoryMapper.getProblemCategoriesOfProblems(999, 1000);
		Assertions.assertEquals(2, problemCategoryRelationships.size());

		ProblemCategoryRelationship pcr = problemCategoryRelationships.get(0);
		Assertions.assertEquals(1000, pcr.getProblemId());
		Assertions.assertEquals(1, pcr.getProblemCategoryId());
	}

	/**
	 * 测试用例: 测试getProblemCategoriesOfProblems()方法
	 * 测试数据: 试题ID的范围从999到999
	 * 预期结果: 返回全部的试题分类信息(总计0条记录)
	 */
	@Test
	public void testGetProblemCategoriesOfProblemsFrom999WithLimit1() {
		List<ProblemCategoryRelationship> problemCategoryRelationships = problemCategoryMapper.getProblemCategoriesOfProblems(999, 999);
		Assertions.assertEquals(0, problemCategoryRelationships.size());
	}

	/**
	 * 测试用例: 测试getProblemCategoryUsingSlug(String)方法
	 * 测试数据: 默认分类的别名
	 * 预期结果: 返回默认分类的试题分类对象
	 */
	@Test
	public void testGetProblemCategoryUsingSlugExists() {
		ProblemCategory problemCategory = problemCategoryMapper.getProblemCategoryUsingCategorySlug("uncategorized");
		Assertions.assertNotNull(problemCategory);

		String problemCategoryName = problemCategory.getProblemCategoryName();
		Assertions.assertEquals("Uncategorized", problemCategoryName);
	}

	/**
	 * 测试用例: 测试getProblemCategoryUsingSlug(String)方法
	 * 测试数据: 不存在的别名
	 * 预期结果: 空引用
	 */
	@Test
	public void testGetProblemCategoryUsingSlugNotExists() {
		ProblemCategory problemCategory = problemCategoryMapper.getProblemCategoryUsingCategorySlug("not-exists");
		Assertions.assertNull(problemCategory);
	}

	/**
	 * 测试用例: 测试createProblemCategory(ProblemCategory)方法
	 * 测试数据: 使用合法的数据集, 并且数据表中不存在相同的英文缩写
	 * 预期结果: 数据插入操作成功完成
	 */
	@Test
	public void testCreateProblemCategoryNormally() {
		ProblemCategory problemCategory = new ProblemCategory("new-category", "New Category", 0);
		int numberOfRowsAffected = problemCategoryMapper.createProblemCategory(problemCategory);
		Assertions.assertEquals(1, numberOfRowsAffected);
	}

	/**
	 * 测试用例: 测试createProblemCategory(ProblemCategory)方法
	 * 测试数据: 使用不合法的数据集(过长的类别名称)
	 * 预期结果: 抛出DataIntegrityViolationException异常
	 */
	@Test
	public void testCreateProblemCategoryUsingTooLongCategoryName() {
		ProblemCategory problemCategory = new ProblemCategory("new-category", "New Category Very Very Very Long Name", 0);
		Executable e = () -> {
			problemCategoryMapper.createProblemCategory(problemCategory);
		};
		Assertions.assertThrows(org.springframework.dao.DataIntegrityViolationException.class, e);
	}

	/**
	 * 测试用例: 测试createProblemCategory(ProblemCategory)方法
	 * 测试数据: 使用合法的数据集, 但数据表中存在相同的英文缩写
	 * 预期结果: 抛出DuplicateKeyException异常
	 */
	@Test
	public void testCreateProblemCategoryUsingExistingSlug() {
		ProblemCategory problemCategory = new ProblemCategory("uncategorized", "New Category", 0);
		Executable e = () -> {
			problemCategoryMapper.createProblemCategory(problemCategory);
		};
		Assertions.assertThrows(org.springframework.dao.DuplicateKeyException.class, e);
	}

	/**
	 * 测试用例: 测试createProblemCategoryRelationship(long, ProblemCategory)方法
	 * 测试数据: 使用存在的试题ID和试题分类对象
	 * 预期结果: 对应试题和试题分类的关系被成功创建
	 */
	@Test
	public void testCreateProblemCategoryRelationshipNormally() {
		ProblemCategory problemCategory = problemCategoryMapper.getProblemCategoryUsingCategoryId(2);
		int numberOfRowsAffected = problemCategoryMapper.createProblemCategoryRelationship(1002, problemCategory);
		Assertions.assertEquals(1, numberOfRowsAffected);
	}

	/**
	 * 测试用例: 测试createProblemCategoryRelationship(long, ProblemCategory)方法
	 * 测试数据: 使用存在的不存在试题ID和存在的试题分类对象
	 * 预期结果: 抛出DataIntegrityViolationException异常
	 */
	@Test
	public void testCreateProblemCategoryRelationshipUsingNotExistingProblemId() {
		ProblemCategory problemCategory = problemCategoryMapper.getProblemCategoryUsingCategoryId(1);
		Executable e = () -> {
			problemCategoryMapper.createProblemCategoryRelationship(0, problemCategory);
		};
		Assertions.assertThrows(org.springframework.dao.DataIntegrityViolationException.class, e);
	}

	/**
	 * 测试用例: 测试createProblemCategoryRelationship(long, ProblemCategory)方法
	 * 测试数据: 使用存在的存在的试题ID和不存在的试题分类对象
	 * 预期结果: 抛出DataIntegrityViolationException异常
	 */
	@Test
	public void testCreateProblemCategoryRelationshipUsingNotExistingProblemCategory() {
		Executable e = () -> {
			problemCategoryMapper.createProblemCategoryRelationship(1000, null);
		};
		Assertions.assertThrows(org.springframework.dao.DataIntegrityViolationException.class, e);
	}

	/**
	 * 测试用例: 测试createProblemCategoryRelationship(long, ProblemCategory)方法
	 * 测试数据: 使用存在的存在试题ID和存在的试题分类对象
	 * 预期结果: 抛出DuplicateKeyException异常
	 */
	@Test
	public void testCreateProblemCategoryRelationshipUsingExistingProblemIdAndExistingProblemCategory() {
		ProblemCategory problemCategory = problemCategoryMapper.getProblemCategoryUsingCategoryId(1);
		Executable e = () -> {
			problemCategoryMapper.createProblemCategoryRelationship(1000, problemCategory);
		};
		Assertions.assertThrows(org.springframework.dao.DuplicateKeyException.class, e);
	}

	/**
	 * 测试用例: 测试updateProblemCategory(ProblemCategory)方法
	 * 测试数据: 使用合法的数据集, 且数据库中存在对应的记录
	 * 预期结果: 数据更新操作成功完成
	 */
	@Test
	public void testUpdateProblemCategoryNormally() {
		ProblemCategory problemCategory = problemCategoryMapper.getProblemCategoryUsingCategorySlug("uncategorized");
		Assertions.assertNotNull(problemCategory);

		problemCategory.setProblemCategoryName("New Category Name");
		int numberOfRowsAffected = problemCategoryMapper.updateProblemCategory(problemCategory);
		Assertions.assertEquals(1, numberOfRowsAffected);

		problemCategory = problemCategoryMapper.getProblemCategoryUsingCategorySlug("uncategorized");
		String newCategoryName = problemCategory.getProblemCategoryName();
		Assertions.assertEquals("New Category Name", newCategoryName);
	}

	/**
	 * 测试用例: 测试updateProblemCategory(ProblemCategory)方法
	 * 测试数据: 使用不合法的数据集(过长的类别名称)
	 * 预期结果: 抛出DataIntegrityViolationException异常
	 */
	@Test
	public void testUpdateProblemCategoryUsingTooLongCategoryName() {
		ProblemCategory problemCategory = problemCategoryMapper.getProblemCategoryUsingCategorySlug("uncategorized");
		Assertions.assertNotNull(problemCategory);

		problemCategory.setProblemCategoryName("New Category Very Very Very Long Name");
		Executable e = () -> {
			problemCategoryMapper.updateProblemCategory(problemCategory);
		};
		Assertions.assertThrows(org.springframework.dao.DataIntegrityViolationException.class, e);
	}

	/**
	 * 测试用例: 测试deleteProblemCategory(int)方法
	 * 测试数据: 存在的试题分类唯一标识符
	 * 预期结果: 数据删除操作成功完成
	 */
	@Test
	public void testDeleteProblemCategoryExists() {
		ProblemCategory problemCategory = problemCategoryMapper.getProblemCategoryUsingCategoryId(1);
		Assertions.assertNotNull(problemCategory);

		int numberOfRowsAffected = problemCategoryMapper.deleteProblemCategory(1);
		Assertions.assertEquals(1, numberOfRowsAffected);

		problemCategory = problemCategoryMapper.getProblemCategoryUsingCategoryId(1);
		Assertions.assertNull(problemCategory);
	}

	/**
	 * 测试用例: 测试deleteProblemCategory(int)方法
	 * 测试数据: 不存在的试题分类唯一标识符
	 * 预期结果: 方法正常执行, 未影响数据表中的数据
	 */
	@Test
	public void testDeleteProblemCategoryNotExists() {
		ProblemCategory problemCategory = problemCategoryMapper.getProblemCategoryUsingCategoryId(0);
		Assertions.assertNull(problemCategory);

		int numberOfRowsAffected = problemCategoryMapper.deleteProblemCategory(0);
		Assertions.assertEquals(0, numberOfRowsAffected);
	}

	/**
	 * 待测试的ProblemCategoryMapper对象.
	 */
	@Autowired
	private ProblemCategoryMapper problemCategoryMapper;
}
