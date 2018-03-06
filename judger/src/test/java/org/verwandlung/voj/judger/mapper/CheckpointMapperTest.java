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
package org.verwandlung.voj.judger.mapper;

import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

import org.verwandlung.voj.judger.model.Checkpoint;

/**
 * CheckpointMapper测试类.
 * 
 * @author Haozhe Xie
 */
@ExtendWith(SpringExtension.class)
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
		Assertions.assertEquals(10, checkpoints.size());
		
		Checkpoint firstCheckpoint = checkpoints.get(0);
		String output = firstCheckpoint.getOutput();
		Assertions.assertEquals("45652\r\n", output);
	}
	
	/**
	 * 测试用例: 测试getCheckpointsUsingProblemId(long)方法
	 * 测试数据: 使用不存在的试题唯一标识符(0)
	 * 预期结果: 返回对应的测试点列表(0个项目)
	 */
	@Test
	public void testGetCheckpointsUsingProblemIdNotExists() {
		List<Checkpoint> checkpoints = checkpointMapper.getCheckpointsUsingProblemId(0);
		Assertions.assertEquals(0, checkpoints.size());
	}
	
	/**
	 * 待测试的CheckpointMapper对象.
	 */
	@Autowired
	private CheckpointMapper checkpointMapper;
}
