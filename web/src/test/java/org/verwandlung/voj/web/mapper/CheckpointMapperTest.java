package org.verwandlung.voj.web.mapper;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import org.verwandlung.voj.web.model.Checkpoint;

/**
 * CheckpointMapper测试类.
 * 
 * @author Haozhe Xie
 */
@RunWith(SpringJUnit4ClassRunner.class)
@Transactional
@ContextConfiguration({"classpath:test-spring-context.xml"})
public class CheckpointMapperTest {
	/**
	 * 测试用例: 测试getCheckpointsUsingProblemId(long)方法
	 * 测试数据: 使用存在的试题唯一标识符(1000)
	 * 预期结果: 返回对应的测试点列表(10个项目)
	 */
	@Test
	public void testGetCheckpointsUsingProblemIdExists() {
		List<Checkpoint> checkpoints = checkpointMapper.getCheckpointsUsingProblemId(1000);
		Assert.assertEquals(10, checkpoints.size());
		
		Checkpoint firstCheckpoint = checkpoints.get(0);
		String output = firstCheckpoint.getOutput();
		Assert.assertEquals("45652\r\n", output);
	}
	
	/**
	 * 测试用例: 测试getCheckpointsUsingProblemId(long)方法
	 * 测试数据: 使用不存在的试题唯一标识符(0)
	 * 预期结果: 返回对应的测试点列表(0个项目)
	 */
	@Test
	public void testGetCheckpointsUsingProblemIdNotExists() {
		List<Checkpoint> checkpoints = checkpointMapper.getCheckpointsUsingProblemId(0);
		Assert.assertEquals(0, checkpoints.size());
	}
	
	/**
	 * 测试用例: 测试createCheckpoint(Checkpoint)方法
	 * 测试数据: 使用正常的数据集创建测试点
	 * 预期结果: 测试点被成功创建
	 */
	@Test
	public void testCreateCheckpointNormally() {
		 Checkpoint checkpoint = new Checkpoint(1000, 100, false, 10, "input", "output");
		 checkpointMapper.createCheckpoint(checkpoint);
	}
	
	/**
	 * 测试用例: 测试createCheckpoint(Checkpoint)方法
	 * 测试数据: 使用重复的主键创建测试点
	 * 预期结果: 抛出DuplicateKeyException异常
	 */
	@Test(expected = org.springframework.dao.DuplicateKeyException.class)
	public void testCreateCheckpointUsingExistingProblemIdAndCheckpointId() {
		 Checkpoint checkpoint = new Checkpoint(1000, 0, false, 10, "input", "output");
		 checkpointMapper.createCheckpoint(checkpoint);
	}
	
	/**
	 * 测试用例: 测试createCheckpoint(Checkpoint)方法
	 * 测试数据: 使用不存在的试题ID创建测试点
	 * 预期结果: 抛出DataIntegrityViolationException异常
	 */
	@Test(expected = org.springframework.dao.DataIntegrityViolationException.class)
	public void testCreateCheckpointUsingNotExistingProblemId() {
		 Checkpoint checkpoint = new Checkpoint(0, 100, false, 10, "input", "output");
		 checkpointMapper.createCheckpoint(checkpoint);
	}
	
	/**
	 * 测试用例: 测试deleteCheckpoint(long)方法
	 * 测试数据: 使用存在的试题ID删除测试点
	 * 预期结果: 指定试题的全部测试点被删除
	 */
	@Test
	public void testDeleteCheckpointUsingExistsingProblemId() {
		List<Checkpoint> checkpoints = checkpointMapper.getCheckpointsUsingProblemId(1000);
		Assert.assertEquals(10, checkpoints.size());
		
		checkpointMapper.deleteCheckpoint(1000);
		checkpoints = checkpointMapper.getCheckpointsUsingProblemId(1000);
		Assert.assertEquals(0, checkpoints.size());
	}
	
	/**
	 * 待测试的CheckpointMapper对象.
	 */
	@Autowired
	private CheckpointMapper checkpointMapper;
}
