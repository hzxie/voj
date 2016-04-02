package org.verwandlung.voj.web.mapper;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import org.verwandlung.voj.web.model.ProblemTag;
import org.verwandlung.voj.web.model.ProblemTagRelationship;

/**
 * ProblemTagMapper测试类.
 * 
 * @author Haozhe Xie
 */
@RunWith(SpringJUnit4ClassRunner.class)
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
		Assert.assertEquals(2, problemTags.size());
		
		ProblemTag firstTag = problemTags.get(0);
		String tagSlug = firstTag.getProblemTagSlug();
		Assert.assertEquals("greedy", tagSlug);
	}
	
	/**
	 * 测试用例: 测试getProblemTagUsingId(int)方法
	 * 测试数据: 默认标签的唯一标识符
	 * 预期结果: 返回默认标签的试题标签对象
	 */
	@Test
	public void testGetProblemTagUsingIdExists() {
		ProblemTag problemTag = problemTagMapper.getProblemTagUsingTagId(1);
		Assert.assertNotNull(problemTag);
		
		String problemTagName = problemTag.getProblemTagName();
		Assert.assertEquals("Greedy", problemTagName);
	}
	
	/**
	 * 测试用例: 测试getProblemTagUsingId(int)方法
	 * 测试数据: 不存在的唯一标识符
	 * 预期结果: 空引用
	 */
	@Test
	public void testGetProblemTagUsingIdNotExists() {
		ProblemTag problemTag = problemTagMapper.getProblemTagUsingTagId(0);
		Assert.assertNull(problemTag);
	}
	
	/**
	 * 测试用例: 测试getProblemTagUsingId(int)方法
	 * 测试数据: 默认标签的唯一标识符
	 * 预期结果: 返回某个试题
	 */
	@Test
	public void testGetProblemTagUsingProblemIdExists() {
		List<ProblemTag> problemTags = problemTagMapper.getProblemTagsUsingProblemId(1001);
		Assert.assertNotNull(problemTags);
		Assert.assertEquals(2, problemTags.size());
		
		ProblemTag problemTag = problemTags.get(0);
		String problemTagName = problemTag.getProblemTagName();
		Assert.assertEquals("Greedy", problemTagName);
	}
	
	/**
	 * 测试用例: 测试getProblemTagUsingId(int)方法
	 * 测试数据: 不存在的唯一标识符
	 * 预期结果: 空试题标签列表
	 */
	@Test
	public void testGetProblemTagUsingProblemIdNotExists() {
		List<ProblemTag> problemTags = problemTagMapper.getProblemTagsUsingProblemId(0);
		Assert.assertEquals(0, problemTags.size());
	}

	/**
	 * 测试用例: 测试getProblemTagsOfProblems()方法
	 * 测试数据: 试题ID的范围从1000到1002
	 * 预期结果: 返回全部的试题标签信息(总计4条记录)
	 */
	@Test
	public void testGetProblemTagsOfProblemsFrom1000WithLimit3() {
		List<ProblemTagRelationship> problemTagRelationships = problemTagMapper.getProblemTagsOfProblems(1000, 1002);
		Assert.assertEquals(4, problemTagRelationships.size());

		ProblemTagRelationship ptr = problemTagRelationships.get(1);
		Assert.assertEquals(1001, ptr.getProblemId());
		Assert.assertEquals(1, ptr.getProblemTagId());
	}

	/**
	 * 测试用例: 测试getProblemTagsOfProblems()方法
	 * 测试数据: 试题ID的范围从999到1000
	 * 预期结果: 返回全部的试题标签信息(总计2条记录)
	 */
	@Test
	public void testGetProblemTagsOfProblemsFrom999WithLimit2() {
		List<ProblemTagRelationship> problemTagRelationships = problemTagMapper.getProblemTagsOfProblems(999, 1000);
		Assert.assertEquals(1, problemTagRelationships.size());

		ProblemTagRelationship ptr = problemTagRelationships.get(0);
		Assert.assertEquals(1000, ptr.getProblemId());
		Assert.assertEquals(1, ptr.getProblemTagId());
	}

	/**
	 * 测试用例: 测试getProblemTagsOfProblems()方法
	 * 测试数据: 试题ID的范围从999到999
	 * 预期结果: 返回全部的试题标签信息(总计0条记录)
	 */
	@Test
	public void testGetProblemTagsOfProblemsFrom999WithLimit1() {
		List<ProblemTagRelationship> problemTagRelationships = problemTagMapper.getProblemTagsOfProblems(999, 999);
		Assert.assertEquals(0, problemTagRelationships.size());
	}

	/**
	 * 测试用例: 测试getProblemTagUsingSlug(String)方法
	 * 测试数据: 默认标签的别名
	 * 预期结果: 返回默认标签的试题标签对象
	 */
	@Test
	public void testGetProblemTagUsingTagSlugExists() {
		ProblemTag problemTag = problemTagMapper.getProblemTagUsingTagSlug("greedy");
		Assert.assertNotNull(problemTag);
		
		String problemTagName = problemTag.getProblemTagName();
		Assert.assertEquals("Greedy", problemTagName);
	}
	
	/**
	 * 测试用例: 测试getProblemTagUsingSlug(String)方法
	 * 测试数据: 不存在的别名
	 * 预期结果: 空引用
	 */
	@Test
	public void testGetProblemTagUsingTagSlugNotExists() {
		ProblemTag problemTag = problemTagMapper.getProblemTagUsingTagSlug("not-exists");
		Assert.assertNull(problemTag);
	}

    @Test
    public void testGetProblemTagsOfProblems() {
        List<ProblemTagRelationship> problemTagRelationships = problemTagMapper.getProblemTagsOfProblems(1000, 1010);
        System.out.println(problemTagRelationships);
    }
	
	/**
	 * 测试用例: createProblemTag(ProblemTag)方法
	 * 测试数据: 使用合法的数据集, 并且数据表中不存在相同的英文缩写
	 * 预期结果: 数据插入操作成功完成
	 */
	@Test
	public void testCreateProblemTagNormally() {
		ProblemTag problemTag = new ProblemTag("new- tag", "New Tag");
		problemTagMapper.createProblemTag(problemTag);
	}
	
	/**
	 * 测试用例: createProblemTag(ProblemTag)方法
	 * 测试数据: 使用不合法的数据集(过长的类别名称)
	 * 预期结果: 抛出DataIntegrityViolationException异常
	 */
	@Test(expected = org.springframework.dao.DataIntegrityViolationException.class)
	public void testCreateProblemTagUsingTooLongTagName() {
		ProblemTag problemTag = new ProblemTag("new-tag", "New Tag Very Very Very Very Long Name");
		problemTagMapper.createProblemTag(problemTag);
	}
	
	/**
	 * 测试用例: createProblemTag(ProblemTag)方法
	 * 测试数据: 使用合法的数据集, 但数据表中存在相同的英文缩写
	 * 预期结果: 抛出DuplicateKeyException异常
	 */
	@Test(expected = org.springframework.dao.DuplicateKeyException.class)
	public void testCreateProblemTagUsingExistingSlug() {
		ProblemTag problemTag = new ProblemTag("greedy", "New Tag");
		problemTagMapper.createProblemTag(problemTag);
	}
	
	/**
	 * 测试用例: updateProblemTag(ProblemTag)方法
	 * 测试数据: 使用合法的数据集, 且数据库中存在对应的记录
	 * 预期结果: 数据更新操作成功完成
	 */
	@Test
	public void testUpdateProblemTagNormally() {
		ProblemTag problemTag = problemTagMapper.getProblemTagUsingTagSlug("greedy");
		Assert.assertNotNull(problemTag);

		problemTag.setProblemTagName("New Tag Name");
		problemTagMapper.updateProblemTag(problemTag);

		problemTag = problemTagMapper.getProblemTagUsingTagSlug("greedy");
		String newTagName = problemTag.getProblemTagName();
		Assert.assertEquals("New Tag Name", newTagName);
	}
	
	/**
	 * 测试用例: updateProblemTag(ProblemTag)方法
	 * 测试数据: 使用不合法的数据集(过长的类别名称)
	 * 预期结果: 抛出DataIntegrityViolationException异常
	 */
	@Test(expected = org.springframework.dao.DataIntegrityViolationException.class)
	public void testUpdateProblemTagUsingTooLongTagName() {
		ProblemTag problemTag = problemTagMapper.getProblemTagUsingTagSlug("greedy");
		Assert.assertNotNull(problemTag);

		problemTag.setProblemTagName("New Tag Very Very Very Very Long Name");
		problemTagMapper.updateProblemTag(problemTag);
	}

	/**
	 * 测试用例: deleteProblemTag(int)方法
	 * 测试数据: 存在的试题标签唯一标识符
	 * 预期结果: 数据删除操作成功完成
	 */
	@Test
	public void testDeleteProblemTagUsingTagIdExists() {
		ProblemTag problemTag = problemTagMapper.getProblemTagUsingTagId(1);
		Assert.assertNotNull(problemTag);
		
		problemTagMapper.deleteProblemTagUsingTagId(1);
		problemTag = problemTagMapper.getProblemTagUsingTagId(1);
		Assert.assertNull(problemTag);
	}
	
	/**
	 * 测试用例: deleteProblemTag(int)方法
	 * 测试数据: 不存在的试题标签唯一标识符
	 * 预期结果: 方法正常执行, 未影响数据表中的数据
	 */
	@Test
	public void testDeleteProblemTagUsingTagIdNotExists() {
		ProblemTag problemTag = problemTagMapper.getProblemTagUsingTagId(0);
		Assert.assertNull(problemTag);
		
		problemTagMapper.getProblemTagUsingTagId(0);
	}
	
	/**
	 * 待测试的ProblemTagMapper对象.
	 */
	@Autowired
	private ProblemTagMapper problemTagMapper;
}
