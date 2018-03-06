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

import org.verwandlung.voj.web.model.ProblemTag;
import org.verwandlung.voj.web.model.ProblemTagRelationship;

/**
 * ProblemTagMapper测试类.
 * 
 * @author Haozhe Xie
 */
@ExtendWith(SpringExtension.class)
@Transactional
@ContextConfiguration({"classpath:test-spring-context.xml"})
public class ProblemTagMapperTest {
	/**
	 *  测试用例: 测试getProblemTags()方法
	 *  测试数据: N/a
	 *  预期结果: 全部的试题标签列表
	 */
    @Test
	public void testGetProblemTags() {
		List<ProblemTag> problemTags = problemTagMapper.getProblemTags();
		Assertions.assertEquals(2, problemTags.size());
		
		ProblemTag firstTag = problemTags.get(0);
		String tagSlug = firstTag.getProblemTagSlug();
		Assertions.assertEquals("greedy", tagSlug);
	}
	
	/**
	 * 测试用例: 测试getProblemTagUsingId(int)方法
	 * 测试数据: 默认标签的唯一标识符
	 * 预期结果: 返回默认标签的试题标签对象
	 */
	@Test
	public void testGetProblemTagUsingIdExists() {
		ProblemTag problemTag = problemTagMapper.getProblemTagUsingTagId(1);
		Assertions.assertNotNull(problemTag);
		
		String problemTagName = problemTag.getProblemTagName();
		Assertions.assertEquals("Greedy", problemTagName);
	}
	
	/**
	 * 测试用例: 测试getProblemTagUsingId(int)方法
	 * 测试数据: 不存在的唯一标识符
	 * 预期结果: 空引用
	 */
	@Test
	public void testGetProblemTagUsingIdNotExists() {
		ProblemTag problemTag = problemTagMapper.getProblemTagUsingTagId(0);
		Assertions.assertNull(problemTag);
	}
	
	/**
	 * 测试用例: 测试getProblemTagUsingId(int)方法
	 * 测试数据: 默认标签的唯一标识符
	 * 预期结果: 返回某个试题
	 */
	@Test
	public void testGetProblemTagUsingProblemIdExists() {
		List<ProblemTag> problemTags = problemTagMapper.getProblemTagsUsingProblemId(1001);
		Assertions.assertNotNull(problemTags);
		Assertions.assertEquals(2, problemTags.size());
		
		ProblemTag problemTag = problemTags.get(0);
		String problemTagName = problemTag.getProblemTagName();
		Assertions.assertEquals("Greedy", problemTagName);
	}
	
	/**
	 * 测试用例: 测试getProblemTagUsingId(int)方法
	 * 测试数据: 不存在的唯一标识符
	 * 预期结果: 空试题标签列表
	 */
	@Test
	public void testGetProblemTagUsingProblemIdNotExists() {
		List<ProblemTag> problemTags = problemTagMapper.getProblemTagsUsingProblemId(0);
		Assertions.assertEquals(0, problemTags.size());
	}

	/**
	 * 测试用例: 测试getProblemTagsOfProblems()方法
	 * 测试数据: 试题ID的范围从1000到1002
	 * 预期结果: 返回全部的试题标签信息(总计4条记录)
	 */
	@Test
	public void testGetProblemTagsOfProblemsFrom1000WithLimit3() {
		List<ProblemTagRelationship> problemTagRelationships = problemTagMapper.getProblemTagsOfProblems(1000, 1002);
		Assertions.assertEquals(4, problemTagRelationships.size());

		ProblemTagRelationship ptr = problemTagRelationships.get(1);
		Assertions.assertEquals(1001, ptr.getProblemId());
		Assertions.assertEquals(1, ptr.getProblemTagId());
	}

	/**
	 * 测试用例: 测试getProblemTagsOfProblems()方法
	 * 测试数据: 试题ID的范围从999到1000
	 * 预期结果: 返回全部的试题标签信息(总计2条记录)
	 */
	@Test
	public void testGetProblemTagsOfProblemsFrom999WithLimit2() {
		List<ProblemTagRelationship> problemTagRelationships = problemTagMapper.getProblemTagsOfProblems(999, 1000);
		Assertions.assertEquals(1, problemTagRelationships.size());

		ProblemTagRelationship ptr = problemTagRelationships.get(0);
		Assertions.assertEquals(1000, ptr.getProblemId());
		Assertions.assertEquals(1, ptr.getProblemTagId());
	}

	/**
	 * 测试用例: 测试getProblemTagsOfProblems()方法
	 * 测试数据: 试题ID的范围从999到999
	 * 预期结果: 返回全部的试题标签信息(总计0条记录)
	 */
	@Test
	public void testGetProblemTagsOfProblemsFrom999WithLimit1() {
		List<ProblemTagRelationship> problemTagRelationships = problemTagMapper.getProblemTagsOfProblems(999, 999);
		Assertions.assertEquals(0, problemTagRelationships.size());
	}

	/**
	 * 测试用例: 测试getProblemTagUsingSlug(String)方法
	 * 测试数据: 默认标签的别名
	 * 预期结果: 返回默认标签的试题标签对象
	 */
	@Test
	public void testGetProblemTagUsingTagSlugExists() {
		ProblemTag problemTag = problemTagMapper.getProblemTagUsingTagSlug("greedy");
		Assertions.assertNotNull(problemTag);
		
		String problemTagName = problemTag.getProblemTagName();
		Assertions.assertEquals("Greedy", problemTagName);
	}
	
	/**
	 * 测试用例: 测试getProblemTagUsingSlug(String)方法
	 * 测试数据: 不存在的别名
	 * 预期结果: 空引用
	 */
	@Test
	public void testGetProblemTagUsingTagSlugNotExists() {
		ProblemTag problemTag = problemTagMapper.getProblemTagUsingTagSlug("not-exists");
		Assertions.assertNull(problemTag);
	}

    /**
     * 测试用例: 测试getProblemTagsOfProblems(long, long)方法
	 * 测试数据: 试题编号1000-1010
	 * 预期结果: 对应试题的试题标签
     */
    @Test
    public void testGetProblemTagsOfProblems() {
        List<ProblemTagRelationship> problemTagRelationships = problemTagMapper.getProblemTagsOfProblems(1000, 1010);
        Assertions.assertEquals(5, problemTagRelationships.size());
        
        ProblemTagRelationship ptr = problemTagRelationships.get(0);
        Assertions.assertEquals(ptr.getProblemId(), 1000);
        Assertions.assertEquals(ptr.getProblemTagId(), 1);
    }
	
	/**
	 * 测试用例: 测试createProblemTag(ProblemTag)方法
	 * 测试数据: 使用合法的数据集, 并且数据表中不存在相同的英文缩写
	 * 预期结果: 数据插入操作成功完成
	 */
	@Test
	public void testCreateProblemTagNormally() {
		ProblemTag problemTag = new ProblemTag("new- tag", "New Tag");
		int numberOfRowsAffected = problemTagMapper.createProblemTag(problemTag);
		Assertions.assertEquals(1, numberOfRowsAffected);
	}
	
	/**
	 * 测试用例: 测试createProblemTag(ProblemTag)方法
	 * 测试数据: 使用不合法的数据集(过长的类别名称)
	 * 预期结果: 抛出DataIntegrityViolationException异常
	 */
	@Test
	public void testCreateProblemTagUsingTooLongTagName() {
		ProblemTag problemTag = new ProblemTag("new-tag", "New Tag Very Very Very Very Long Name");
		Executable e = () -> {
			problemTagMapper.createProblemTag(problemTag);
		};
		Assertions.assertThrows(org.springframework.dao.DataIntegrityViolationException.class, e);
		
	}
	
	/**
	 * 测试用例: 测试createProblemTag(ProblemTag)方法
	 * 测试数据: 使用合法的数据集, 但数据表中存在相同的英文缩写
	 * 预期结果: 抛出DuplicateKeyException异常
	 */
	@Test
	public void testCreateProblemTagUsingExistingSlug() {
		ProblemTag problemTag = new ProblemTag("greedy", "New Tag");
		Executable e = () -> {
			problemTagMapper.createProblemTag(problemTag);
		};
		Assertions.assertThrows(org.springframework.dao.DuplicateKeyException.class, e);
	}
	
	/**
	 * 测试用例: 测试updateProblemTag(ProblemTag)方法
	 * 测试数据: 使用合法的数据集, 且数据库中存在对应的记录
	 * 预期结果: 数据更新操作成功完成
	 */
	@Test
	public void testUpdateProblemTagNormally() {
		ProblemTag problemTag = problemTagMapper.getProblemTagUsingTagSlug("greedy");
		Assertions.assertNotNull(problemTag);

		problemTag.setProblemTagName("New Tag Name");
		int numberOfRowsAffected = problemTagMapper.updateProblemTag(problemTag);
		Assertions.assertEquals(1, numberOfRowsAffected);

		problemTag = problemTagMapper.getProblemTagUsingTagSlug("greedy");
		String newTagName = problemTag.getProblemTagName();
		Assertions.assertEquals("New Tag Name", newTagName);
	}
	
	/**
	 * 测试用例: 测试updateProblemTag(ProblemTag)方法
	 * 测试数据: 使用不合法的数据集(过长的类别名称)
	 * 预期结果: 抛出DataIntegrityViolationException异常
	 */
	@Test
	public void testUpdateProblemTagUsingTooLongTagName() {
		ProblemTag problemTag = problemTagMapper.getProblemTagUsingTagSlug("greedy");
		Assertions.assertNotNull(problemTag);

		problemTag.setProblemTagName("New Tag Very Very Very Very Long Name");
		Executable e = () -> {
			problemTagMapper.updateProblemTag(problemTag);
		};
		Assertions.assertThrows(org.springframework.dao.DataIntegrityViolationException.class, e);	
	}

	/**
	 * 测试用例: 测试deleteProblemTag(int)方法
	 * 测试数据: 存在的试题标签唯一标识符
	 * 预期结果: 数据删除操作成功完成
	 */
	@Test
	public void testDeleteProblemTagUsingTagIdExists() {
		ProblemTag problemTag = problemTagMapper.getProblemTagUsingTagId(1);
		Assertions.assertNotNull(problemTag);

		int numberOfRowsAffected = problemTagMapper.deleteProblemTagUsingTagId(1);
		Assertions.assertEquals(1, numberOfRowsAffected);

		problemTag = problemTagMapper.getProblemTagUsingTagId(1);
		Assertions.assertNull(problemTag);
	}

	/**
	 * 测试用例: 测试deleteProblemTag(int)方法
	 * 测试数据: 不存在的试题标签唯一标识符
	 * 预期结果: 方法正常执行, 未影响数据表中的数据
	 */
	@Test
	public void testDeleteProblemTagUsingTagIdNotExists() {
		ProblemTag problemTag = problemTagMapper.getProblemTagUsingTagId(0);
		Assertions.assertNull(problemTag);

		int numberOfRowsAffected = problemTagMapper.deleteProblemTagUsingTagId(0);
		Assertions.assertEquals(0, numberOfRowsAffected);
	}

	/**
	 * 测试用例: 测试deleteProblemTagRelationship(int)方法
	 * 测试数据: 存在的试题标签唯一标识符
	 * 预期结果: 数据删除操作成功完成
	 */
	@Test
	public void testDeleteProblemTagUsingProblemIdExists() {
		List<ProblemTag> problemTags = problemTagMapper.getProblemTagsUsingProblemId(1001);
		Assertions.assertEquals(2, problemTags.size());

		int numberOfRowsAffected = problemTagMapper.deleteProblemTagRelationship(1001);
		Assertions.assertEquals(2, numberOfRowsAffected);
	}

	/**
	 * 测试用例: 测试deleteProblemTagRelationship(int)方法
	 * 测试数据: 不存在的试题标签唯一标识符
	 * 预期结果: 方法正常执行, 未影响数据表中的数据
	 */
	@Test
	public void testDeleteProblemTagUsingProblemIdNotExists() {
		List<ProblemTag> problemTags = problemTagMapper.getProblemTagsUsingProblemId(0);
		Assertions.assertEquals(0, problemTags.size());

		int numberOfRowsAffected = problemTagMapper.deleteProblemTagRelationship(0);
		Assertions.assertEquals(0, numberOfRowsAffected);
	}
	
	/**
	 * 待测试的ProblemTagMapper对象.
	 */
	@Autowired
	private ProblemTagMapper problemTagMapper;
}
