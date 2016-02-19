package org.verwandlung.voj.web.mapper;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import org.verwandlung.voj.web.model.ProblemCategory;

/**
 * ProblemCategoryMapper测试类.
 * 
 * @author Haozhe Xie
 */
@RunWith(SpringJUnit4ClassRunner.class)
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
		Assert.assertNotNull(problemCategories);
		Assert.assertEquals(1, problemCategories.size());
		
		ProblemCategory firstProblemCategory = problemCategories.get(0);
		String problemCategoryName = firstProblemCategory.getProblemCategoryName();
        Assert.assertEquals("Uncategorized", problemCategoryName);
	}
	
    /**
     * 测试用例: 测试getProblemCategoryUsingId(int)方法
     * 测试数据: 默认分类的唯一标识符
     * 预期结果: 返回默认分类的试题分类对象
     */
    @Test
    public void testGetProblemCategoryUsingIdExists() {
        ProblemCategory problemCategory = problemCategoryMapper.getProblemCategoryUsingCategoryId(1);
        Assert.assertNotNull(problemCategory);
        
        String problemCategoryName = problemCategory.getProblemCategoryName();
        Assert.assertEquals("Uncategorized", problemCategoryName);
    }
    
    /**
     * 测试用例: 测试getProblemCategoryUsingId(int)方法
     * 测试数据: 不存在的唯一标识符
     * 预期结果: 空引用
     */
    @Test
    public void testGetProblemCategoryUsingIdNotExists() {
        ProblemCategory problemCategory = problemCategoryMapper.getProblemCategoryUsingCategoryId(0);
        Assert.assertNull(problemCategory);
    }
    
    /**
     * 测试用例: 测试getProblemCategoriesUsingProblemId(long)方法
     * 测试数据: 存在的某个试题的唯一标识符
     * 预期结果: 返回试题的全部试题标签列表
     */
    @Test
    public void testGetProblemTagUsingProblemIdExists() {
    	List<ProblemCategory> problemCategories = problemCategoryMapper.getProblemCategoriesUsingProblemId(1000);
    	Assert.assertEquals(1, problemCategories.size());
    	
    	ProblemCategory problemCategory = problemCategories.get(0);
    	String problemCategorySlug = problemCategory.getProblemCategorySlug();
    	Assert.assertEquals("uncategorized", problemCategorySlug);
    }
    
    /**
     * 测试用例: 测试getProblemCategoriesUsingProblemId(long)方法
     * 测试数据: 不存在试题的唯一标识符
     * 预期结果: 返回空的试题标签列表
     */
    @Test
    public void testGetProblemTagUsingProblemIdNotExists() {
    	List<ProblemCategory> problemCategories = problemCategoryMapper.getProblemCategoriesUsingProblemId(0);
    	Assert.assertEquals(0, problemCategories.size());
    }

    /**
     * 测试用例: 测试getProblemCategoryUsingSlug(String)方法
     * 测试数据: 默认分类的唯一英文缩写
     * 预期结果: 返回默认分类的试题分类对象
     */
    @Test
    public void testGetProblemCategoryUsingSlugExists() {
        ProblemCategory problemCategory = problemCategoryMapper.getProblemCategoryUsingCategorySlug("uncategorized");
        Assert.assertNotNull(problemCategory);
        
        String problemCategoryName = problemCategory.getProblemCategoryName();
        Assert.assertEquals("Uncategorized", problemCategoryName);
    }
    
    /**
     * 测试用例: 测试getProblemCategoryUsingSlug(String)方法
     * 测试数据: 不存在的唯一英文缩写
     * 预期结果: 空引用
     */
    @Test
    public void testGetProblemCategoryUsingSlugNotExists() {
        ProblemCategory problemCategory = problemCategoryMapper.getProblemCategoryUsingCategorySlug("not-exists");
        Assert.assertNull(problemCategory);
    }
    
    /**
     * 测试用例: 测试createProblemCategory(ProblemCategory)方法
     * 测试数据: 使用合法的数据集, 并且数据表中不存在相同的英文缩写
     * 预期结果: 数据插入操作成功完成
     */
    @Test
    public void testCreateProblemCategoryNormally() {
        ProblemCategory problemCategory = new ProblemCategory("new-category", "New Category", 0);
        problemCategoryMapper.createProblemCategory(problemCategory);
    }
    
    /**
     * 测试用例: 测试createProblemCategory(ProblemCategory)方法
     * 测试数据: 使用不合法的数据集(过长的类别名称)
     * 预期结果: 抛出DataIntegrityViolationException异常
     */
    @Test(expected = org.springframework.dao.DataIntegrityViolationException.class)
    public void testCreateProblemCategoryUsingTooLongCategoryName() {
        ProblemCategory problemCategory = new ProblemCategory("new-category", "New Category Very Very Very Long Name", 0);
        problemCategoryMapper.createProblemCategory(problemCategory);
    }
    
    /**
     * 测试用例: 测试createProblemCategory(ProblemCategory)方法
     * 测试数据: 使用合法的数据集, 但数据表中存在相同的英文缩写
     * 预期结果: 抛出DuplicateKeyException异常
     */
    @Test(expected = org.springframework.dao.DuplicateKeyException.class)
    public void testCreateProblemCategoryUsingExistingSlug() {
        ProblemCategory problemCategory = new ProblemCategory("uncategorized", "New Category", 0);
        problemCategoryMapper.createProblemCategory(problemCategory);
    }
    
    /**
     * 测试用例: 测试createProblemCategoryRelationship(long, ProblemCategory)方法
     * 测试数据: 使用存在的试题ID和试题分类对象
     * 预期结果: 对应试题和试题分类的关系被成功创建
     */
    @Test
    public void testCreateProblemCategoryRelationshipNormally() {
        ProblemCategory problemCategory = problemCategoryMapper.getProblemCategoryUsingCategoryId(1);
        problemCategoryMapper.createProblemCategoryRelationship(1002, problemCategory);
    }
    
    /**
     * 测试用例: 测试createProblemCategoryRelationship(long, ProblemCategory)方法
     * 测试数据: 使用存在的不存在试题ID和存在的试题分类对象
     * 预期结果: 抛出DataIntegrityViolationException异常
     */
    @Test(expected = org.springframework.dao.DataIntegrityViolationException.class)
    public void testCreateProblemCategoryRelationshipUsingNotExistingProblemId() {
        ProblemCategory problemCategory = problemCategoryMapper.getProblemCategoryUsingCategoryId(1);
        problemCategoryMapper.createProblemCategoryRelationship(0, problemCategory);
    }
    
    /**
     * 测试用例: 测试createProblemCategoryRelationship(long, ProblemCategory)方法
     * 测试数据: 使用存在的存在的试题ID和不存在的试题分类对象
     * 预期结果: 抛出DataIntegrityViolationException异常
     */
    @Test(expected = org.springframework.dao.DataIntegrityViolationException.class)
    public void testCreateProblemCategoryRelationshipUsingNotExistingProblemCategory() {
        problemCategoryMapper.createProblemCategoryRelationship(1000, null);
    }
    
    /**
     * 测试用例: 测试createProblemCategoryRelationship(long, ProblemCategory)方法
     * 测试数据: 使用存在的存在试题ID和存在的试题分类对象
     * 预期结果: 抛出DuplicateKeyException异常
     */
    @Test(expected = org.springframework.dao.DuplicateKeyException.class)
    public void testCreateProblemCategoryRelationshipUsingExistingProblemIdAndExistingProblemCategory() {
        ProblemCategory problemCategory = problemCategoryMapper.getProblemCategoryUsingCategoryId(1);
        problemCategoryMapper.createProblemCategoryRelationship(1000, problemCategory);
    }
    
    /**
     * 测试用例: 测试updateProblemCategory(ProblemCategory)方法
     * 测试数据: 使用合法的数据集, 且数据库中存在对应的记录
     * 预期结果: 数据更新操作成功完成
     */
    @Test
    public void testUpdateProblemCategoryNormally() {
        ProblemCategory problemCategory = problemCategoryMapper.getProblemCategoryUsingCategorySlug("uncategorized");
        Assert.assertNotNull(problemCategory);

        problemCategory.setProblemCategoryName("New Category Name");
        problemCategoryMapper.updateProblemCategory(problemCategory);

        problemCategory = problemCategoryMapper.getProblemCategoryUsingCategorySlug("uncategorized");
        String newCategoryName = problemCategory.getProblemCategoryName();
        Assert.assertEquals("New Category Name", newCategoryName);
    }
    
    /**
     * 测试用例: 测试updateProblemCategory(ProblemCategory)方法
     * 测试数据: 使用不合法的数据集(过长的类别名称)
     * 预期结果: 抛出DataIntegrityViolationException异常
     */
    @Test(expected = org.springframework.dao.DataIntegrityViolationException.class)
    public void testUpdateProblemCategoryUsingTooLongCategoryName() {
    	ProblemCategory problemCategory = problemCategoryMapper.getProblemCategoryUsingCategorySlug("uncategorized");
        Assert.assertNotNull(problemCategory);

        problemCategory.setProblemCategoryName("New Category Very Very Very Long Name");
        problemCategoryMapper.updateProblemCategory(problemCategory);
    }

    /**
     * 测试用例: 测试deleteProblemCategory(int)方法
     * 测试数据: 存在的试题分类唯一标识符
     * 预期结果: 数据删除操作成功完成
     */
    @Test
    public void testDeleteProblemCategoryExists() {
    	ProblemCategory problemCategory = problemCategoryMapper.getProblemCategoryUsingCategoryId(1);
        Assert.assertNotNull(problemCategory);
        
        problemCategoryMapper.deleteProblemCategory(1);
        problemCategory = problemCategoryMapper.getProblemCategoryUsingCategoryId(1);
        Assert.assertNull(problemCategory);
    }
    
    /**
     * 测试用例: 测试deleteProblemCategory(int)方法
     * 测试数据: 不存在的试题分类唯一标识符
     * 预期结果: 方法正常执行, 未影响数据表中的数据
     */
    @Test
    public void testDeleteProblemCategoryNotExists() {
    	ProblemCategory problemCategory = problemCategoryMapper.getProblemCategoryUsingCategoryId(0);
        Assert.assertNull(problemCategory);
        
        problemCategoryMapper.deleteProblemCategory(0);
    }
    
    /**
     * 待测试的ProblemCategoryMapper对象.
     */
    @Autowired
    private ProblemCategoryMapper problemCategoryMapper;
}
