package org.verwandlung.voj.web.mapper;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import org.verwandlung.voj.web.model.ProblemTag;

/**
 * ProblemTagMapper测试类.
 * 
 * @author Xie Haozhe
 */
@RunWith(SpringJUnit4ClassRunner.class)
@Transactional
@ContextConfiguration({"classpath:test-spring-context.xml"})
public class ProblemTagMapperTest {
    /**
     * 测试用例: 测试getProblemTagUsingId(int)方法
     * 测试数据: 默认标签的唯一标识符
     * 预期结果: 返回默认标签的试题标签对象
     */
    @Test
    public void testGetProblemTagUsingIdExists() {
        ProblemTag problemTag = problemTagMapper.getProblemTagUsingId(1);
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
        ProblemTag problemTag = problemTagMapper.getProblemTagUsingId(0);
        Assert.assertNull(problemTag);
    }

    /**
     * 测试用例: 测试getProblemTagUsingSlug(String)方法
     * 测试数据: 默认标签的唯一英文缩写
     * 预期结果: 返回默认标签的试题标签对象
     */
    @Test
    public void testGetProblemTagUsingSlugExists() {
        ProblemTag problemTag = problemTagMapper.getProblemTagUsingSlug("greedy");
        Assert.assertNotNull(problemTag);
        
        String problemTagName = problemTag.getProblemTagName();
        Assert.assertEquals("Greedy", problemTagName);
    }
    
    /**
     * 测试用例: 测试getProblemTagUsingSlug(String)方法
     * 测试数据: 不存在的唯一英文缩写
     * 预期结果: 空引用
     */
    @Test
    public void testGetProblemTagUsingSlugNotExists() {
        ProblemTag problemTag = problemTagMapper.getProblemTagUsingSlug("not-exists");
        Assert.assertNull(problemTag);
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
        ProblemTag problemTag = problemTagMapper.getProblemTagUsingSlug("greedy");
        Assert.assertNotNull(problemTag);

        problemTag.setProblemTagName("New Tag Name");
        problemTagMapper.updateProblemTag(problemTag);

        problemTag = problemTagMapper.getProblemTagUsingSlug("greedy");
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
    	ProblemTag problemTag = problemTagMapper.getProblemTagUsingSlug("greedy");
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
    public void testDeleteProblemTagExists() {
    	ProblemTag problemTag = problemTagMapper.getProblemTagUsingId(1);
        Assert.assertNotNull(problemTag);
        
        problemTagMapper.deleteProblemTag(1);
        problemTag = problemTagMapper.getProblemTagUsingId(1);
        Assert.assertNull(problemTag);
    }
    
    /**
     * 测试用例: deleteProblemTag(int)方法
     * 测试数据: 不存在的试题标签唯一标识符
     * 预期结果: 方法正常执行, 未影响数据表中的数据
     */
    @Test
    public void testDeleteProblemTagNotExists() {
    	ProblemTag problemTag = problemTagMapper.getProblemTagUsingId(0);
        Assert.assertNull(problemTag);
        
        problemTagMapper.deleteProblemTag(0);
    }
    
    /**
     * 待测试的ProblemTagMapper对象.
     */
    @Autowired
    private ProblemTagMapper problemTagMapper;
}
